package com.ScienceFiction.DronePassAndroid.core.account

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.DroneFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.ShapeFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.SketchFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.buildAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.encodeAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.recordShapeRealtimeSyncSuccess
import com.ScienceFiction.DronePassAndroid.core.data.sync.recordSketchRealtimeSyncSuccess
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionPreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionState
import com.ScienceFiction.DronePassAndroid.feature.profile.AccountDeletionService
import com.ScienceFiction.DronePassAndroid.feature.profile.AccountReloadStatus
import com.ScienceFiction.DronePassAndroid.feature.profile.ProfilePreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.profile.reloadAccountStatus
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditPreferenceKeys
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/** 기기 계정 데이터를 지울 때 함께 지우는 DataStore 키(동기화·선택·백업 표시). 위치 동의·화면 설정 등 기기 항목은 남긴다. */
internal val DEVICE_ACCOUNT_DATA_PREFERENCE_KEYS: List<Preferences.Key<*>> = listOf(
    SyncPreferenceKeys.LAST_SYNC_TIME,
    SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME,
    SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME,
    SyncPreferenceKeys.SYNCED_SHAPE_BASELINE,
    SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME,
    SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME,
    ProfilePreferenceKeys.LAST_BACKUP_TIME,
    ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME,
    ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED,
    ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED,
    DroneSelectionPreferenceKeys.SELECTED_DRONE_ID,
    DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS,
    DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS,
    ShapeEditPreferenceKeys.LAST_SELECTED_DRONE_ID,
    ShapeEditPreferenceKeys.LEGACY_LAST_SELECTED_DRONE_ID,
)

/** 로그아웃 ① 업로드·대기 쓰기 확인의 제한 시간. 넘기면 오프라인으로 보고 사용자에게 묻는다. */
internal const val LOGOUT_FLUSH_TIMEOUT_MS = 10_000L

private const val ACCOUNT_SESSION_PREFS = "account_session"
private const val KEY_NEEDS_FIRESTORE_CLEAR = "needs_firestore_clear"

/**
 * 로그아웃·탈퇴 ⑥: 다음 실행 때 Firestore 를 처음 쓰기 전에 오프라인 캐시와 대기 중인 쓰기를 지운다(이전 계정의
 * 옛 쓰기가 나중에 재전송되지 않게). Application.onCreate 에서 동기로 읽어야 하므로 SharedPreferences 에 둔다.
 */
fun clearFirestorePersistenceIfNeeded(context: Context) {
    val prefs = context.getSharedPreferences(ACCOUNT_SESSION_PREFS, Context.MODE_PRIVATE)
    if (!prefs.getBoolean(KEY_NEEDS_FIRESTORE_CLEAR, false)) return
    FirebaseFirestore.getInstance().clearPersistence()
        .addOnSuccessListener { prefs.edit().remove(KEY_NEEDS_FIRESTORE_CLEAR).apply() }
        .addOnFailureListener { Log.w("AccountSessionFlows", "Firestore 캐시 정리 실패(다음 실행에 다시 시도)", it) }
}

/** 계정 동기화 중 사용자에게 알릴 일. */
internal sealed interface AccountSessionEvent {
    data object SyncFailed : AccountSessionEvent
}

/** 앱 최상위에 띄우는 기기 데이터 확인창. 대기 상태는 저장되므로 앱을 다시 켜거나 돌아오면 다시 묻는다. */
sealed interface AccountPrompt {
    data object None : AccountPrompt

    /** 로그인 전 데이터를 가져올지. [가져오기] / [삭제(개수로 2차 확인)] / [취소=로그아웃, 데이터 유지]. */
    data class Import(
        val uid: String,
        val kind: ImportPromptKind,
        val shapeCount: Int,
        val sketchCount: Int,
        val droneCount: Int,
        val deviceItemCount: Int,
    ) : AccountPrompt

    /** 서버를 읽지 못해 후보를 세지 못했다. 대기 상태를 유지하고 다시 시도한다. */
    data class ImportCheckFailed(val uid: String) : AccountPrompt

    /** 주인이 다른 계정인데 다른 계정으로 로그인했다. [지우고 계속] / [취소=로그아웃]. */
    data class ReplaceOtherAccountData(val uid: String) : AccountPrompt

    /** journal 없이 세션만 사라졌다. 데이터는 남아 있다. [다시 로그인] / [이 기기 데이터 삭제]. */
    data object SessionLost : AccountPrompt

    /** 다른 기기(또는 웹)에서 탈퇴가 확인되어 이 기기 데이터를 지웠다. */
    data object AccountDeletedElsewhere : AccountPrompt
}

enum class LogoutResult {
    DONE,

    /** 서버에 닿지 못해 일부 변경이 계정에 저장되지 않았을 수 있다. 사용자가 [로그아웃]을 고르면 다시 부른다. */
    NEEDS_OFFLINE_CONFIRMATION,

    /** signOut 실패. 로그인 상태를 유지한다. */
    FAILED,
}

/** 가져오기 확인창에 보여 줄 개수와 후보. */
private data class ImportEvaluation(val candidates: ImportCandidates, val deviceItemCount: Int)

/**
 * 기기 데이터 주인 판단과 그에 따른 흐름(3.6.0 재설계). 앱 시작 시 [start] 로 시작하고, 로그인 상태가
 * 바뀔 때마다 다시 판단한다. 화면이나 ViewModel 에 묶이지 않으므로 FCM·부팅으로 프로세스가 시작돼도 동작하고,
 * 로그아웃·탈퇴는 화면이 닫혀도 끝까지 진행한다(앱 범위 코루틴).
 *
 * 판단은 [decideAccountSession](공유 fixture 로 고정)이 하고, 여기서는 그 결과대로 관문을 열고 닫는다. 판단과
 * 흐름은 모두 [evaluationMutex] 안에서 한 번에 하나씩 돈다. 관문이 닫힌 채로 서버를 읽고 쓰는 곳은 로그아웃 ① 업로드와
 * 가져오기(후보 읽기·업로드)뿐이며, 그곳만 [AccountSession.issueFlowTicket] 을 쓴다.
 */
@Singleton
class AccountSessionFlows @Inject constructor(
    private val session: AccountSession,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefsHelper: EncryptedPrefsHelper,
    private val shapeDao: ShapeDao,
    private val sketchDao: SketchDao,
    private val droneDao: DroneDao,
    private val shapeStore: ShapeFirebaseStore,
    private val sketchStore: SketchFirebaseStore,
    private val droneStore: DroneFirebaseStore,
    private val shapeRepository: ShapeRepository,
    private val sketchRepository: SketchRepository,
    private val droneRepository: DroneRepository,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val droneSelectionState: DroneSelectionState,
    private val accountDeletionService: AccountDeletionService,
    private val analyticsLogger: AnalyticsLogger,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "AccountSessionFlows"

        /** journal 처리·이전 뒤 다시 판단하는 횟수 상한(무한 반복 방지). */
        private const val MAX_EVALUATION_STEPS = 6
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val evaluationMutex = Mutex()
    private var started = false

    /** 이 프로세스에서 계정 삭제를 확인한 uid(fixture 의 accountDeletedUid). 탈퇴 정리를 마치면 지운다. */
    @Volatile
    private var accountDeletedUid: String? = null

    /** 세션 끊김 안내를 이 프로세스에서 한 번 닫았으면 다시 띄우지 않는다(로그인 상태가 바뀌면 다시 띄운다). */
    @Volatile
    private var sessionLostAcknowledged = false

    private val _prompt = MutableStateFlow<AccountPrompt>(AccountPrompt.None)
    val prompt: StateFlow<AccountPrompt> = _prompt.asStateFlow()

    /** 가져오기 확인을 기다리는 중인지. 프로필에 "가져오기 확인 필요"를 표시하고 수동 백업을 숨긴다. */
    val importPending: Flow<Boolean> = dataStore.data.map { preferences ->
        accountSessionState(preferences).pendingImportUid != null
    }

    private val _events = MutableSharedFlow<AccountSessionEvent>(extraBufferCapacity = 4)
    internal val events: SharedFlow<AccountSessionEvent> = _events.asSharedFlow()

    private var lastAuthUid: String? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != lastAuthUid) sessionLostAcknowledged = false
        lastAuthUid = uid
        requestEvaluation()
    }

    /** 앱 시작 시 한 번. 판단이 끝나기 전까지 관문은 닫혀 있다. */
    fun start() {
        if (started) return
        started = true
        scope.launch { session.permissionDenied.collect { uid -> onPermissionDenied(uid) } }
        // 등록하면 바로 한 번 불리므로 첫 판단도 이 리스너가 한다.
        auth.addAuthStateListener(authStateListener)
    }

    fun requestEvaluation() {
        scope.launch { evaluationMutex.withLock { evaluateLocked() } }
    }

    /** 앱으로 돌아왔을 때: 가져오기 확인을 기다리는 중이면 다시 묻는다(서버 읽기 재시도 포함). */
    fun onForeground() {
        scope.launch {
            if (session.state().pendingImportUid != null || _prompt.value is AccountPrompt.ImportCheckFailed) {
                evaluationMutex.withLock { evaluateLocked() }
            }
        }
    }

    // region 판단

    private suspend fun evaluateLocked() {
        repeat(MAX_EVALUATION_STEPS) {
            if (!applyDecision()) return
        }
        Log.w(TAG, "판단이 끝나지 않아 관문을 닫아 둡니다.")
        closeGateAndStopSync()
    }

    /** 판단 하나를 처리한다. 다시 판단해야 하면 true. */
    private suspend fun applyDecision(): Boolean {
        val state = session.state()
        val authUid = auth.currentUser?.uid
        val decision = decideAccountSession(
            AccountSessionInput(
                owner = state.owner,
                authUid = authUid,
                journal = state.journal,
                pendingImportUid = state.pendingImportUid,
                deviceHasData = deviceHasData(),
                accountDeletedUid = accountDeletedUid,
            ),
        )
        Log.d(TAG, "판단: ${decision.action}")
        return when (decision.action) {
            AccountSessionAction.DEFER_UNTIL_PROTECTED_DATA -> {
                closeGateAndStopSync()
                false
            }
            AccountSessionAction.RUN_MIGRATION -> {
                runMigration(authUid)
                true
            }
            AccountSessionAction.GUEST_IDLE -> {
                closeGateAndStopSync()
                if (state.pendingImportUid != null) session.updateState { writePendingImportUid(null) }
                clearPromptUnless<AccountPrompt.AccountDeletedElsewhere>()
                false
            }
            AccountSessionAction.OPEN_GATE -> {
                clearPromptUnless<AccountPrompt.AccountDeletedElsewhere>()
                if (authUid != null && session.openUid.value != authUid) startAccountSync(authUid, selectAllDrones = false)
                false
            }
            AccountSessionAction.ADOPT_EMPTY -> {
                if (authUid != null) adoptEmpty(authUid)
                false
            }
            AccountSessionAction.EVALUATE_IMPORT -> {
                if (authUid != null) evaluateImport(authUid, requireNotNull(decision.promptKind), state.pendingImportUid)
                false
            }
            AccountSessionAction.CONFIRM_REPLACE_OTHER_ACCOUNT_DATA -> {
                closeGateAndStopSync()
                if (authUid != null) _prompt.value = AccountPrompt.ReplaceOtherAccountData(authUid)
                false
            }
            AccountSessionAction.SESSION_LOST -> {
                closeGateAndStopSync()
                if (!sessionLostAcknowledged) _prompt.value = AccountPrompt.SessionLost
                false
            }
            AccountSessionAction.ABORT_LOGOUT -> {
                session.updateState { writeAccountJournal(null) }
                true
            }
            AccountSessionAction.FINISH_LOGOUT_WIPE -> {
                finishLogoutWipe()
                true
            }
            AccountSessionAction.CHECK_DELETED_ACCOUNT -> checkDeletedAccount()
            AccountSessionAction.FINISH_DELETION_WIPE -> {
                val deletedElsewhere = accountDeletedUid != null && state.journal == null
                finishDeletionWipe()
                if (deletedElsewhere) _prompt.value = AccountPrompt.AccountDeletedElsewhere
                true
            }
        }
    }

    private inline fun <reified T : AccountPrompt> clearPromptUnless() {
        if (_prompt.value !is T) _prompt.value = AccountPrompt.None
    }

    private suspend fun deviceHasData(): Boolean = deviceHasAccountData(
        shapes = shapeDao.getAllShapesOnce().map { it.toDomain() },
        sketches = sketchDao.getAllSketchesOnce().map { it.toDomain() },
        drones = droneDao.getAllDronesOnce().map { it.toDomain() },
    )

    /** 3.5.x(또는 106 이하)에서 업데이트된 기기: 예전 "마지막 로그인 uid"로 주인을 한 번 정하고 그 키를 지운다. */
    private suspend fun runMigration(authUid: String?) {
        val savedUid = runCatching { encryptedPrefsHelper.loadFirebaseUid() }.getOrNull()
        val owner = decideMigrationOwner(authUid = authUid, savedUid = savedUid)
        session.updateState { writeAccountOwner(owner) }
        runCatching { encryptedPrefsHelper.clearAccountKeys() }
            .onFailure { Log.w(TAG, "계정 키 정리 실패", it) }
        Log.d(TAG, "기기 데이터 주인 이전 완료: ${owner::class.simpleName}")
    }

    // endregion

    // region 관문·동기화

    /** 기기에 옮길 데이터가 없다(또는 지우기로 했다): 기기 계정 데이터를 지우고 계정 데이터를 내려받는다. */
    private suspend fun adoptEmpty(authUid: String) {
        wipeDeviceAccountData()
        session.updateState {
            writeAccountOwner(LocalDataOwner.Account(authUid))
            writePendingImportUid(null)
            bumpAccountEpoch()
        }
        _prompt.value = AccountPrompt.None
        startAccountSync(authUid, selectAllDrones = true)
    }

    /** 관문을 열고 리스너와 첫 전체 동기화를 시작한다. 동기화는 판단 잠금 밖에서 돈다. */
    private suspend fun startAccountSync(uid: String, selectAllDrones: Boolean) {
        session.clearPermissionDenied()
        session.openGate(uid)
        val ticket = session.syncTicket() ?: return
        realtimeSyncManager.startListening(ticket)
        scope.launch { runInitialSync(ticket, selectAllDrones) }
    }

    /** 드론 → 도형 → 기본 드론 → 스케치 순(도형이 드론 id 를 가리키므로 드론을 먼저). */
    private suspend fun runInitialSync(ticket: SyncTicket, selectAllDrones: Boolean) {
        try {
            droneRepository.performFullSync(ticket)
            shapeRepository.performFullSync(ticket)
            // 계정에 드론이 하나도 없을 때만 기본 드론을 만든다(관문이 열려 있으므로 계정에도 올라간다).
            session.requireValid(ticket)
            droneRepository.ensureDefaultDroneIfNeeded()
            session.commit(ticket) {
                val baseline = encodeAccountSwitchShapeBaseline(
                    buildAccountSwitchShapeBaseline(shapeRepository.getAllShapes().first()),
                )
                dataStore.edit { it.recordShapeRealtimeSyncSuccess(System.currentTimeMillis(), baseline) }
                if (selectAllDrones) {
                    droneSelectionState.selectAllDrones(droneRepository.getActiveDrones().first())
                }
            }
            sketchRepository.performFullSync(ticket)
            session.commit(ticket) {
                dataStore.edit { it.recordSketchRealtimeSyncSuccess(System.currentTimeMillis()) }
            }
            Log.d(TAG, "계정 첫 동기화 완료")
        } catch (e: StaleSyncTicketException) {
            Log.d(TAG, "관문이 바뀌어 첫 동기화를 멈춥니다.")
        } catch (e: Exception) {
            if (e.isPermissionDenied()) return
            Log.e(TAG, "계정 첫 동기화 실패", e)
            _events.tryEmit(AccountSessionEvent.SyncFailed)
        }
    }

    private suspend fun closeGateAndStopSync() {
        if (session.openUid.value == null && !realtimeSyncManager.isRealtimeSyncEnabled.value) return
        session.closeGate()
        realtimeSyncManager.stopListening()
    }

    /**
     * 기기 계정 데이터를 지운다(서버에는 손대지 않는다). 관문을 먼저 닫고 진행 중인 동기화가 끝나기를 기다린 뒤 지우므로,
     * 그 뒤에 끝나는 네트워크 작업이 빈 저장소에 예전 계정 데이터를 다시 쓰지 못한다.
     */
    private suspend fun wipeDeviceAccountData() {
        session.closeGate()
        realtimeSyncManager.stopListeningAndWait()
        sketchRepository.cancelAllPendingSyncsAndWait()
        shapeRepository.deleteAllShapes()
        droneRepository.deleteAllDrones()
        sketchRepository.deleteAllSketchesLocally()
        droneSelectionState.resetForAccountSwitch()
        realtimeSyncManager.resetSyncTrackingForAccountSwitch()
        dataStore.edit { preferences ->
            DEVICE_ACCOUNT_DATA_PREFERENCE_KEYS.forEach { preferences.remove(it) }
        }
        runCatching { encryptedPrefsHelper.clearAccountKeys() }
            .onFailure { Log.w(TAG, "계정 키 정리 실패", it) }
        // 이미 표시된 알림(도형 종료·푸시)도 이전 계정 것이므로 지운다.
        runCatching { NotificationManagerCompat.from(context).cancelAll() }
    }

    private fun markNeedsFirestoreClear() {
        context.getSharedPreferences(ACCOUNT_SESSION_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NEEDS_FIRESTORE_CLEAR, true).commit()
    }

    /** 로그아웃·탈퇴 ④~⑦: 진행 중인 동기화 취소·삭제, 다음 실행 Firestore 캐시 정리, 주인을 비로그인으로. */
    private suspend fun finishLogoutWipe() {
        val state = session.state()
        val journal = state.journal
        if (journal is AccountJournal.Logout && journal.step != LogoutStep.WIPING) {
            session.updateState { writeAccountJournal(AccountJournal.Logout(journal.uid, LogoutStep.WIPING)) }
        }
        wipeDeviceAccountData()
        markNeedsFirestoreClear()
        session.updateState {
            writeAccountOwner(LocalDataOwner.Guest)
            writeAccountJournal(null)
            writePendingImportUid(null)
            bumpAccountEpoch()
        }
        runCatching { droneRepository.ensureDefaultDroneIfNeeded() }
            .onFailure { Log.w(TAG, "기본 드론 생성 실패", it) }
    }

    // endregion

    // region 가져오기

    private suspend fun evaluateImport(authUid: String, kind: ImportPromptKind, pendingImportUid: String?) {
        closeGateAndStopSync()
        if (pendingImportUid != authUid) session.updateState { writePendingImportUid(authUid) }
        val current = _prompt.value
        if (current is AccountPrompt.Import && current.uid == authUid) return
        val evaluation = try {
            readImportCandidates(authUid)
        } catch (e: Exception) {
            Log.w(TAG, "가져오기 후보를 세지 못해 대기합니다.", e)
            _prompt.value = AccountPrompt.ImportCheckFailed(authUid)
            return
        }
        if (evaluation.candidates.count == 0) {
            adoptEmpty(authUid)
            return
        }
        _prompt.value = AccountPrompt.Import(
            uid = authUid,
            kind = kind,
            shapeCount = evaluation.candidates.shapes.size,
            sketchCount = evaluation.candidates.sketches.size,
            droneCount = evaluation.candidates.drones.size,
            deviceItemCount = evaluation.deviceItemCount,
        )
    }

    /** 서버를 [Source.SERVER] 로 읽어(캐시로 오판하지 않게, 삭제 표시 포함) 가져오기 후보를 센다. */
    private suspend fun readImportCandidates(uid: String): ImportEvaluation {
        val ticket = session.issueFlowTicket(uid, SyncTicketPurpose.IMPORT)
        return computeCandidatesFromServer(ticket)
    }

    private suspend fun computeCandidatesFromServer(ticket: SyncTicket): ImportEvaluation {
        val serverDrones = droneStore.loadAllDronesIncludingDeleted(ticket, Source.SERVER).getOrThrow()
        val serverShapes = shapeStore.loadAllShapesIncludingDeleted(ticket, Source.SERVER).getOrThrow()
        val serverSketches = sketchStore.loadAllSketchesIncludingDeleted(ticket, Source.SERVER).getOrThrow()
        val localShapes = shapeDao.getAllShapesOnce().map { it.toDomain() }
        val localSketches = sketchDao.getAllSketchesOnce().map { it.toDomain() }
        val localDrones = droneDao.getAllDronesOnce().map { it.toDomain() }
        val candidates = computeImportCandidates(
            localShapes = localShapes,
            localSketches = localSketches,
            localDrones = localDrones,
            serverShapes = serverShapes,
            serverSketches = serverSketches,
            serverDrones = serverDrones,
            nowMillis = System.currentTimeMillis(),
        )
        val deviceItemCount = localShapes.count { it.deletedAt == null } +
            localSketches.count { it.deletedAt == null } +
            localDrones.count { it.deletedAt == null }
        return ImportEvaluation(candidates, deviceItemCount)
    }

    /** [가져오기]: 후보만 올린 뒤 기기 계정 데이터를 정리하고 계정 데이터를 내려받는다. 같은 응답이 두 번 와도 한 번만 처리한다. */
    fun chooseImport(uid: String) {
        scope.launch {
            evaluationMutex.withLock {
                if (!isAskingImport(uid)) return@withLock
                try {
                    uploadImportCandidates(uid)
                } catch (e: Exception) {
                    Log.w(TAG, "가져오기 업로드 실패", e)
                    _prompt.value = AccountPrompt.ImportCheckFailed(uid)
                    return@withLock
                }
                // 올리지 않은 항목(서버가 더 새롭거나 기기에서 지운 것)은 계정에 반영하지 않으므로, 기기를 비우고 계정 기준으로 다시 받는다.
                adoptEmpty(uid)
            }
        }
    }

    private suspend fun uploadImportCandidates(uid: String) {
        val ticket = session.issueFlowTicket(uid, SyncTicketPurpose.IMPORT)
        val candidates = computeCandidatesFromServer(ticket).candidates
        uploadAndMark(ticket, candidates.drones, candidates.shapes, candidates.sketches)
        Log.d(TAG, "가져오기 업로드 완료: 드론=${candidates.drones.size}, 도형=${candidates.shapes.size}, 스케치=${candidates.sketches.size}")
    }

    /** [삭제](2차 확인 뒤): 기기 데이터를 지우고 계정 데이터를 내려받는다. */
    fun chooseDeleteDeviceDataForImport(uid: String) {
        scope.launch {
            evaluationMutex.withLock {
                if (!isAskingImport(uid)) return@withLock
                adoptEmpty(uid)
            }
        }
    }

    /** [취소] 또는 창 닫기: 로그아웃한다. 기기 데이터와 주인은 그대로 두고 가져오기 대기를 지운다. */
    fun cancelImport(uid: String) {
        scope.launch {
            evaluationMutex.withLock {
                if (!isAskingImport(uid)) return@withLock
                session.updateState { writePendingImportUid(null) }
                _prompt.value = AccountPrompt.None
                signOutKeepingDeviceData()
            }
        }
    }

    /** 서버를 다시 읽어 후보를 센다. */
    fun retryImportCheck() = requestEvaluation()

    private suspend fun isAskingImport(uid: String): Boolean {
        val prompt = _prompt.value
        val asking = (prompt is AccountPrompt.Import && prompt.uid == uid) ||
            (prompt is AccountPrompt.ImportCheckFailed && prompt.uid == uid)
        return asking && auth.currentUser?.uid == uid && session.state().pendingImportUid == uid
    }

    // endregion

    // region 다른 계정·세션 끊김

    /** 다른 계정 데이터를 지우고 지금 계정으로 전환한다. */
    fun confirmReplaceOtherAccountData(uid: String) {
        scope.launch {
            evaluationMutex.withLock {
                val prompt = _prompt.value
                if (prompt !is AccountPrompt.ReplaceOtherAccountData || prompt.uid != uid || auth.currentUser?.uid != uid) {
                    return@withLock
                }
                adoptEmpty(uid)
            }
        }
    }

    /** 취소 또는 창 닫기: 로그아웃한다(이전 계정 데이터와 주인은 그대로). */
    fun cancelReplaceOtherAccountData(uid: String) {
        scope.launch {
            evaluationMutex.withLock {
                val prompt = _prompt.value
                if (prompt !is AccountPrompt.ReplaceOtherAccountData || prompt.uid != uid) return@withLock
                _prompt.value = AccountPrompt.None
                signOutKeepingDeviceData()
            }
        }
    }

    /** [다시 로그인] 또는 창 닫기: 안내만 닫는다. 같은 계정으로 다시 로그인하면 이어서 동기화한다. */
    fun acknowledgeSessionLost() {
        sessionLostAcknowledged = true
        if (_prompt.value == AccountPrompt.SessionLost) _prompt.value = AccountPrompt.None
    }

    /** [이 기기 데이터 삭제](2차 확인 뒤): 세션이 사라진 계정의 기기 데이터를 지우고 비로그인으로 쓴다. */
    fun deleteDeviceDataAfterSessionLost() {
        scope.launch {
            evaluationMutex.withLock {
                if (_prompt.value != AccountPrompt.SessionLost || auth.currentUser != null) return@withLock
                if (session.state().owner !is LocalDataOwner.Account) return@withLock
                _prompt.value = AccountPrompt.None
                wipeDeviceAccountData()
                markNeedsFirestoreClear()
                session.updateState {
                    writeAccountOwner(LocalDataOwner.Guest)
                    writeAccountJournal(null)
                    writePendingImportUid(null)
                    bumpAccountEpoch()
                }
                runCatching { droneRepository.ensureDefaultDroneIfNeeded() }
                evaluateLocked()
            }
        }
    }

    fun dismissAccountDeletedNotice() {
        if (_prompt.value == AccountPrompt.AccountDeletedElsewhere) _prompt.value = AccountPrompt.None
    }

    private suspend fun signOutKeepingDeviceData() {
        closeGateAndStopSync()
        runCatching { FcmService.deactivateTokenAndWait(context) }
            .onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패", it) }
        runCatching { auth.signOut() }
            .onFailure { Log.w(TAG, "로그아웃 실패", it) }
    }

    // endregion

    // region 로그아웃

    /**
     * 로그아웃. 앱 범위에서 실행하므로 화면이 닫혀도 끝까지 간다. 단계마다 journal 을 남겨 중간에 앱이 멈춰도 다음 실행에서 이어 간다.
     * ① 서버와 비교해 기기에만 있거나 더 새로운 항목만 올리고 대기 쓰기를 기다린다(10초, 토글과 무관)
     * ② 푸시 비활성화 ③ signOut(실패하면 중단) ④~⑦ 동기화 취소·기기 데이터 삭제·Firestore 캐시 정리 예약·주인을 비로그인으로.
     *
     * @param proceedWithoutUpload 오프라인 경고에서 [로그아웃]을 고른 경우 true. ①이 실패해도 계속한다.
     */
    suspend fun logout(proceedWithoutUpload: Boolean): LogoutResult =
        scope.async { evaluationMutex.withLock { logoutLocked(proceedWithoutUpload) } }.await()

    private suspend fun logoutLocked(proceedWithoutUpload: Boolean): LogoutResult {
        val uid = auth.currentUser?.uid ?: return LogoutResult.DONE
        if (session.state().owner != LocalDataOwner.Account(uid)) {
            // 기기 데이터가 이 계정 것이 아니다(가져오기·다른 계정 확인 대기 중): [취소]와 같이 로그아웃만 하고 데이터는 남긴다.
            session.updateState { writePendingImportUid(null) }
            _prompt.value = AccountPrompt.None
            signOutKeepingDeviceData()
            analyticsLogger.logLogout()
            return if (auth.currentUser == null) LogoutResult.DONE else LogoutResult.FAILED
        }
        session.updateState { writeAccountJournal(AccountJournal.Logout(uid, LogoutStep.FLUSHING)) }
        session.closeGate()
        realtimeSyncManager.stopListeningAndWait()
        sketchRepository.cancelAllPendingSyncsAndWait()

        // ① 서버에 닿지 못하면(10초) 사용자에게 묻는다. 취소하면 로그인 상태와 데이터를 그대로 둔다.
        val flushed = runCatching { withTimeout(LOGOUT_FLUSH_TIMEOUT_MS) { flushBeforeLogout(uid) } }
            .onFailure { Log.w(TAG, "로그아웃 전 업로드 실패", it) }
            .isSuccess
        if (!flushed && !proceedWithoutUpload) {
            session.updateState { writeAccountJournal(null) }
            evaluateLocked()
            return LogoutResult.NEEDS_OFFLINE_CONFIRMATION
        }

        // ② 푸시 비활성화(로그인 상태여야 기기 문서를 쓸 수 있다).
        session.updateState { writeAccountJournal(AccountJournal.Logout(uid, LogoutStep.SIGNING_OUT)) }
        runCatching { FcmService.deactivateTokenAndWait(context) }
            .onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패", it) }

        // ③ signOut. 실패하면 로그인 상태를 유지한다.
        val signedOut = runCatching { auth.signOut() }.isSuccess && auth.currentUser == null
        if (!signedOut) {
            session.updateState { writeAccountJournal(null) }
            evaluateLocked()
            return LogoutResult.FAILED
        }

        // ④~⑦
        finishLogoutWipe()
        _prompt.value = AccountPrompt.None
        analyticsLogger.logLogout()
        return LogoutResult.DONE
    }

    /** ① 서버를 [Source.SERVER] 로 읽어 기기에만 있거나 더 새로운 항목만 올리고, 대기 중인 쓰기가 서버에 닿을 때까지 기다린다. */
    private suspend fun flushBeforeLogout(uid: String) {
        val ticket = session.issueFlowTicket(uid, SyncTicketPurpose.LOGOUT_FLUSH)
        val serverDrones = droneStore.loadAllDronesIncludingDeleted(ticket, Source.SERVER).getOrThrow()
        val serverShapes = shapeStore.loadAllShapesIncludingDeleted(ticket, Source.SERVER).getOrThrow()
        val serverSketches = sketchStore.loadAllSketchesIncludingDeleted(ticket, Source.SERVER).getOrThrow()
        val drones = logoutFlushItems(droneDao.getAllDronesOnce().map { it.toDomain() }, serverDrones, DroneModel::id, DroneModel::updatedAt, DroneModel::deletedAt)
        val shapes = logoutFlushItems(shapeDao.getAllShapesOnce().map { it.toDomain() }, serverShapes, ShapeModel::id, ShapeModel::updatedAt, ShapeModel::deletedAt)
        val sketches = logoutFlushItems(sketchDao.getAllSketchesOnce().map { it.toDomain() }, serverSketches, SketchModel::id, SketchModel::updatedAt, SketchModel::deletedAt)
        uploadAndMark(ticket, drones, shapes, sketches)
        firestore.waitForPendingWrites().await()
        Log.d(TAG, "로그아웃 전 업로드 완료: 드론=${drones.size}, 도형=${shapes.size}, 스케치=${sketches.size}")
    }

    /** 드론 → 도형 → 스케치 순으로 올리고 다른 기기가 받도록 metadata 를 갱신한다. */
    private suspend fun uploadAndMark(
        ticket: SyncTicket,
        drones: List<DroneModel>,
        shapes: List<ShapeModel>,
        sketches: List<SketchModel>,
    ) {
        if (drones.isNotEmpty()) droneStore.saveDrones(ticket, drones)
        if (shapes.isNotEmpty()) shapeStore.saveShapes(ticket, shapes)
        if (drones.isNotEmpty() || shapes.isNotEmpty()) shapeStore.updateServerMetadata(ticket)
        if (sketches.isNotEmpty()) {
            sketchStore.saveSketches(ticket, sketches)
            sketchStore.updateServerMetadata(ticket)
        }
    }

    // endregion

    // region 탈퇴

    /**
     * 탈퇴. 서버를 부르기 전에 탈퇴 journal 을 남기고, 서버 처리가 성공하면 업로드 없이 ④~⑦ + signOut 을 한다.
     * 실패하면 journal 을 지우고 다시 판단한다(관문이 다시 열린다).
     */
    suspend fun deleteAccount(activity: Activity): Result<Unit> =
        scope.async { evaluationMutex.withLock { deleteAccountLocked(activity) } }.await()

    private suspend fun deleteAccountLocked(activity: Activity): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return accountDeletionService.deleteCurrentAccount(activity)
        session.updateState { writeAccountJournal(AccountJournal.Deleting(uid)) }
        session.closeGate()
        realtimeSyncManager.stopListeningAndWait()
        val result = accountDeletionService.deleteCurrentAccount(activity)
        if (result.isFailure) {
            session.updateState { writeAccountJournal(null) }
            evaluateLocked()
            return result
        }
        finishDeletionWipe()
        evaluateLocked()
        return result
    }

    /** 탈퇴 journal 이 있고 세션이 있다: 계정이 남았는지 확인한다. 없으면 정리, 있으면 journal 만 지운다. */
    private suspend fun checkDeletedAccount(): Boolean {
        closeGateAndStopSync()
        val user = auth.currentUser ?: return true
        return when (runCatching { reloadAccountStatus(user) }.getOrNull()) {
            AccountReloadStatus.DELETED -> {
                finishDeletionWipe()
                true
            }
            AccountReloadStatus.EXISTS -> {
                session.updateState { writeAccountJournal(null) }
                true
            }
            // 확인하지 못했다(오프라인 등): 관문을 닫은 채 다음 판단에서 다시 확인한다.
            null -> false
        }
    }

    /** 탈퇴 후 정리(업로드 없음): 로그아웃 ④~⑦ + signOut. */
    private suspend fun finishDeletionWipe() {
        if (auth.currentUser != null) runCatching { auth.signOut() }
        finishLogoutWipe()
        accountDeletedUid = null
    }

    /**
     * 쓰기가 PERMISSION_DENIED 로 거부됐다(다른 기기에서 탈퇴가 진행 중이거나 끝난 계정). 관문은 이미 닫혔고 재시도하지 않는다.
     * 계정이 지워졌는지 확인해, 지워졌으면 이 기기 데이터도 지우고 알린다. 남아 있거나(탈퇴 진행 중 등) 확인하지 못하면
     * 데이터를 지우지 않고, 이 프로세스에서는 관문을 다시 열지 않는다(같은 오류 반복 방지).
     */
    private suspend fun onPermissionDenied(uid: String) {
        realtimeSyncManager.stopListening()
        val user = auth.currentUser?.takeIf { it.uid == uid } ?: return
        when (runCatching { reloadAccountStatus(user) }.getOrNull()) {
            AccountReloadStatus.DELETED -> {
                Log.w(TAG, "다른 기기에서 탈퇴가 확인되어 이 기기 데이터를 지웁니다.")
                accountDeletedUid = uid
                requestEvaluation()
            }
            AccountReloadStatus.EXISTS -> Log.w(TAG, "쓰기가 거부됐지만 계정은 남아 있습니다. 다음 실행까지 동기화를 멈춥니다.")
            null -> Log.w(TAG, "계정 확인 실패. 다음 실행까지 동기화를 멈춥니다.")
        }
    }

    // endregion
}
