package com.ScienceFiction.DronePassAndroid.core.account

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.buildAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.encodeAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.recordShapeRealtimeSyncSuccess
import com.ScienceFiction.DronePassAndroid.core.data.sync.recordSketchRealtimeSyncSuccess
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionPreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionState
import com.ScienceFiction.DronePassAndroid.feature.profile.ProfilePreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditPreferenceKeys
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
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

/** 계정 동기화 중 사용자에게 알릴 일. */
internal sealed interface AccountSessionEvent {
    data object SyncFailed : AccountSessionEvent
}

/**
 * 기기 데이터 주인 판단과 그에 따른 흐름(3.6.0 재설계). 앱 시작 시 [start] 로 시작하고, 로그인 상태가
 * 바뀔 때마다 다시 판단한다. 화면이나 ViewModel 에 묶이지 않으므로 FCM·부팅으로 프로세스가 시작돼도 동작한다.
 *
 * 판단은 [decideAccountSession](공유 fixture 로 고정)이 하고, 여기서는 그 결과대로 관문을 열고 닫는다.
 */
@Singleton
class AccountSessionFlows @Inject constructor(
    private val session: AccountSession,
    private val auth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefsHelper: EncryptedPrefsHelper,
    private val shapeDao: ShapeDao,
    private val sketchDao: SketchDao,
    private val droneDao: DroneDao,
    private val shapeRepository: ShapeRepository,
    private val sketchRepository: SketchRepository,
    private val droneRepository: DroneRepository,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val droneSelectionState: DroneSelectionState,
) {
    companion object {
        private const val TAG = "AccountSessionFlows"

        /** journal 처리·이전 뒤 다시 판단하는 횟수 상한(무한 반복 방지). */
        private const val MAX_EVALUATION_STEPS = 6
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val evaluationMutex = Mutex()
    private var started = false

    private val _events = MutableSharedFlow<AccountSessionEvent>(extraBufferCapacity = 4)
    internal val events: SharedFlow<AccountSessionEvent> = _events.asSharedFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { requestEvaluation() }

    /** 앱 시작 시 한 번. 판단이 끝나기 전까지 관문은 닫혀 있다. */
    fun start() {
        if (started) return
        started = true
        scope.launch { session.permissionDenied.collect { uid -> onPermissionDenied(uid) } }
        // 등록하면 바로 한 번 불리므로 첫 판단도 이 리스너가 한다.
        auth.addAuthStateListener(authStateListener)
    }

    fun requestEvaluation() {
        scope.launch { evaluate() }
    }

    private suspend fun evaluate() = evaluationMutex.withLock {
        repeat(MAX_EVALUATION_STEPS) {
            if (!applyDecision()) return@withLock
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
                false
            }
            AccountSessionAction.OPEN_GATE -> {
                if (authUid != null && session.openUid.value != authUid) startAccountSync(authUid, selectAllDrones = false)
                false
            }
            AccountSessionAction.ADOPT_EMPTY -> {
                if (authUid != null) adoptEmpty(authUid)
                false
            }
            AccountSessionAction.EVALUATE_IMPORT -> {
                closeGateAndStopSync()
                if (authUid != null && state.pendingImportUid != authUid) {
                    session.updateState { writePendingImportUid(authUid) }
                }
                false
            }
            AccountSessionAction.CONFIRM_REPLACE_OTHER_ACCOUNT_DATA,
            AccountSessionAction.SESSION_LOST,
            AccountSessionAction.CHECK_DELETED_ACCOUNT -> {
                closeGateAndStopSync()
                false
            }
            AccountSessionAction.ABORT_LOGOUT -> {
                session.updateState { writeAccountJournal(null) }
                true
            }
            AccountSessionAction.FINISH_LOGOUT_WIPE,
            AccountSessionAction.FINISH_DELETION_WIPE -> {
                closeGateAndStopSync()
                false
            }
        }
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

    /** 기기에 옮길 데이터가 없다: 남은 것(손대지 않은 기본 드론 등)을 지우고 계정 데이터를 내려받는다. */
    private suspend fun adoptEmpty(authUid: String) {
        wipeDeviceAccountData()
        session.updateState {
            writeAccountOwner(LocalDataOwner.Account(authUid))
            writePendingImportUid(null)
            bumpAccountEpoch()
        }
        startAccountSync(authUid, selectAllDrones = true)
    }

    /** 관문을 열고 리스너와 첫 전체 동기화를 시작한다. 동기화는 판단 잠금 밖에서 돈다. */
    private suspend fun startAccountSync(uid: String, selectAllDrones: Boolean) {
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
    }

    /**
     * 쓰기가 PERMISSION_DENIED 로 거부됐다(다른 기기에서 탈퇴가 진행 중이거나 끝난 계정). 관문은 이미 닫혔고
     * 재시도하지 않는다. 기기 데이터는 지우지 않고 계정이 아직 있는지만 확인한다. 계정이 없으면 로그아웃해
     * "세션만 사라진 경우"로 다룬다. 계정이 있으면 이 프로세스에서는 관문을 다시 열지 않는다(같은 오류 반복 방지).
     */
    private suspend fun onPermissionDenied(uid: String) {
        realtimeSyncManager.stopListening()
        val user = auth.currentUser?.takeIf { it.uid == uid } ?: return
        try {
            user.reload().await()
            Log.w(TAG, "쓰기가 거부됐지만 계정은 남아 있습니다. 다음 실행까지 동기화를 멈춥니다.")
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.w(TAG, "계정이 삭제되어 로그아웃합니다.")
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "계정 확인 실패", e)
        }
    }
}
