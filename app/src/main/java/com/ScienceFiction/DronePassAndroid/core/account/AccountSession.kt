package com.ScienceFiction.DronePassAndroid.core.account

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** 저장하는 주인 상태. 앱을 지우면 함께 지워지는 DataStore 에 둔다. */
internal object AccountSessionKeys {
    val OWNER = stringPreferencesKey("account_owner")
    val PENDING_IMPORT_UID = stringPreferencesKey("account_pending_import_uid")
    val JOURNAL = stringPreferencesKey("account_journal")
    val EPOCH = longPreferencesKey("account_epoch")
}

internal data class AccountSessionState(
    val owner: LocalDataOwner?,
    val pendingImportUid: String?,
    val journal: AccountJournal?,
    val epoch: Long,
)

internal fun accountSessionState(preferences: Preferences) = AccountSessionState(
    owner = decodeLocalDataOwner(preferences[AccountSessionKeys.OWNER]),
    pendingImportUid = preferences[AccountSessionKeys.PENDING_IMPORT_UID],
    journal = decodeAccountJournal(preferences[AccountSessionKeys.JOURNAL]),
    epoch = preferences[AccountSessionKeys.EPOCH] ?: 0L,
)

internal fun MutablePreferences.writeAccountOwner(owner: LocalDataOwner) {
    this[AccountSessionKeys.OWNER] = owner.encode()
}

internal fun MutablePreferences.writeAccountJournal(journal: AccountJournal?) {
    if (journal == null) remove(AccountSessionKeys.JOURNAL) else this[AccountSessionKeys.JOURNAL] = journal.encode()
}

internal fun MutablePreferences.writePendingImportUid(uid: String?) {
    if (uid == null) remove(AccountSessionKeys.PENDING_IMPORT_UID) else this[AccountSessionKeys.PENDING_IMPORT_UID] = uid
}

/** 주인이 바뀔 때마다 1 올린다. 예전 작업이 새 주인의 저장소에 반영되지 않게 한다. */
internal fun MutablePreferences.bumpAccountEpoch() {
    this[AccountSessionKeys.EPOCH] = (this[AccountSessionKeys.EPOCH] ?: 0L) + 1L
}

internal enum class SyncTicketPurpose {
    /** 관문이 열려 있을 때의 일반 동기화. */
    SYNC,

    /** 로그아웃 ① 업로드. 관문이 닫힌 채로 AccountSessionFlows 만 발급한다. */
    LOGOUT_FLUSH,

    /** 가져오기 서버 읽기·업로드. 관문이 닫힌 채로 AccountSessionFlows 만 발급한다. */
    IMPORT,
}

/**
 * 계정 데이터 작업 하나의 허가. 작업을 시작할 때 한 번 받아 끝날 때까지 쓰고, uid 를 다시 읽지 않는다.
 * [AccountSession] 만 만든다(생성 위치는 계약 테스트가 고정한다). 관문이 바뀌면 [generation] 이 달라져 무효가 된다.
 */
class SyncTicket internal constructor(
    val uid: String,
    internal val generation: Long,
    internal val purpose: SyncTicketPurpose,
)

/** 받아들인 PERMISSION_DENIED 하나. [generation] 은 이 거절로 관문을 닫은 뒤의 세대다. */
internal data class PermissionDenial(val uid: String, val generation: Long)

/** 관문이 바뀌어 더 이상 쓸 수 없는 티켓. 재시도하지 않고 작업을 끝낸다. */
class StaleSyncTicketException : IllegalStateException("Sync ticket is no longer valid")

internal fun isPermissionDeniedCode(code: String?): Boolean = code == "PERMISSION_DENIED"

/** 쓰기가 보안 규칙에서 거부됐다(예: 다른 기기에서 탈퇴가 진행 중·완료된 계정의 탈퇴 잠금). */
internal fun Throwable.isPermissionDenied(): Boolean =
    isPermissionDeniedCode((this as? FirebaseFirestoreException)?.code?.name)

/**
 * 기기 데이터 주인과 동기화 관문(3.6.0 재설계). 화면에 묶이지 않는 앱 범위 싱글턴이다.
 *
 * - 관문은 주인이 로그인 계정과 같고 대기 상태(가져오기 확인·로그아웃·탈퇴 journal)가 없을 때만 열린다.
 *   판단과 흐름은 [AccountSessionFlows] 가 하고, 앱 시작 직후 판단이 끝나기 전에는 닫혀 있다.
 * - `users/{uid}/…` 의 도형·스케치·드론·metadata 읽기·쓰기·리스너는 모두 [SyncTicket] 을 거친다.
 * - 네트워크를 기다린 뒤 기기 저장소에 반영할 때는 [commit] 안에서 티켓을 다시 확인한다.
 *   관문을 바꾸는 쪽도 같은 잠금을 잡으므로, 확인과 반영 사이에 기기 데이터가 지워지는 일은 없다.
 */
@Singleton
class AccountSession internal constructor(
    private val dataStore: DataStore<Preferences>,
    /** 지금 로그인한 계정 uid. 앱에서는 FirebaseAuth, 테스트에서는 고정값. */
    private val currentUid: () -> String?,
) {
    @Inject
    constructor(dataStore: DataStore<Preferences>, auth: FirebaseAuth) : this(dataStore, { auth.currentUser?.uid })

    private val generation = AtomicLong(0L)
    private val commitMutex = Mutex()

    private val _openUid = MutableStateFlow<String?>(null)

    /** 관문이 열린 계정 uid. 닫혀 있으면 null. */
    val openUid: StateFlow<String?> = _openUid.asStateFlow()

    /** 이 프로세스에서 쓰기가 PERMISSION_DENIED 로 거부된 계정. 다시 판단하기 전까지 관문을 열지 않는다. */
    @Volatile
    private var deniedUid: String? = null

    private val _permissionDenied = MutableSharedFlow<PermissionDenial>(extraBufferCapacity = 1)
    internal val permissionDenied: SharedFlow<PermissionDenial> = _permissionDenied.asSharedFlow()

    /** 일반 동기화 티켓. 관문이 닫혀 있으면 null 이다. */
    fun syncTicket(): SyncTicket? {
        val uid = _openUid.value ?: return null
        if (currentUid() != uid || deniedUid == uid) return null
        return SyncTicket(uid, generation.get(), SyncTicketPurpose.SYNC)
    }

    fun isValid(ticket: SyncTicket): Boolean {
        if (ticket.generation != generation.get()) return false
        if (currentUid() != ticket.uid || deniedUid == ticket.uid) return false
        return ticket.purpose != SyncTicketPurpose.SYNC || _openUid.value == ticket.uid
    }

    fun requireValid(ticket: SyncTicket) {
        if (!isValid(ticket)) throw StaleSyncTicketException()
    }

    /** 티켓이 아직 유효할 때만 [block] 으로 기기 저장소에 반영한다. 무효면 [StaleSyncTicketException]. */
    suspend fun <T> commit(ticket: SyncTicket, block: suspend () -> T): T = commitMutex.withLock {
        requireValid(ticket)
        block()
    }

    /**
     * Firestore 요청 하나를 티켓으로 감싼다. 요청 전에 티켓을 확인하고, PERMISSION_DENIED 면 관문을 닫는다
     * (다른 기기에서 탈퇴가 진행 중이거나 끝난 계정 — 재시도하지 않는다).
     */
    suspend fun <T> remote(ticket: SyncTicket, request: suspend (uid: String) -> T): T {
        requireValid(ticket)
        try {
            return request(ticket.uid)
        } catch (e: Exception) {
            if (e.isPermissionDenied()) reportPermissionDenied(ticket)
            throw e
        }
    }

    /**
     * [remote] 와 같지만 PERMISSION_DENIED 로 관문을 닫지 않는다. 태그 저장소 전용이다: 운영 보안 규칙(tags)이
     * 앱보다 늦게 배포돼도 도형 동기화까지 멈추지 않게 태그 동기화만 실패한다 (iOS TagFirebaseStore 정합).
     */
    suspend fun <T> remoteIgnoringDenial(ticket: SyncTicket, request: suspend (uid: String) -> T): T {
        requireValid(ticket)
        return request(ticket.uid)
    }

    /**
     * 지금 세대의 티켓이 받은 거절만 받아들인다(L-10). 관문이 닫혔다가 같은 계정으로 다시 열린 뒤에 도착한 옛 요청의
     * 거절은 새 세션을 막지 않는다. 로그아웃 ① 업로드·가져오기의 흐름 티켓도 발급 뒤 관문이 바뀌지 않았으면 그대로
     * 받아들인다(탈퇴 잠금 감지).
     */
    internal fun reportPermissionDenied(ticket: SyncTicket) {
        if (deniedUid == ticket.uid || currentUid() != ticket.uid) return
        // 리스너 콜백에서도 불려 commitMutex 를 잡지 못한다. 세대 확인과 올리기를 한 번에 해 같은 거절도 한 번만 받는다.
        if (!generation.compareAndSet(ticket.generation, ticket.generation + 1)) return
        deniedUid = ticket.uid
        _openUid.value = null
        _permissionDenied.tryEmit(PermissionDenial(ticket.uid, ticket.generation + 1))
    }

    /** 이 거절로 닫은 관문이 아직 그대로인지. 그사이 다시 열렸으면 false. */
    internal fun isCurrent(denial: PermissionDenial): Boolean = denial.generation == generation.get()

    internal fun clearPermissionDenied() {
        deniedUid = null
    }

    /** [AccountSessionFlows] 의 로그아웃 ① 업로드와 가져오기에서만 쓴다. 관문이 닫혀 있어도 그 흐름 동안만 유효하다. */
    internal fun issueFlowTicket(uid: String, purpose: SyncTicketPurpose): SyncTicket {
        require(purpose != SyncTicketPurpose.SYNC)
        return SyncTicket(uid, generation.get(), purpose)
    }

    internal suspend fun openGate(uid: String) = commitMutex.withLock {
        generation.incrementAndGet()
        _openUid.value = uid
    }

    /** 관문을 닫는다. 진행 중인 [commit] 이 끝난 뒤에 닫히고, 그 뒤로는 이전 티켓이 모두 무효다. */
    internal suspend fun closeGate() = commitMutex.withLock {
        generation.incrementAndGet()
        _openUid.value = null
    }

    internal suspend fun state(): AccountSessionState = accountSessionState(dataStore.data.first())

    internal suspend fun updateState(transform: MutablePreferences.() -> Unit) {
        dataStore.edit { it.transform() }
    }
}
