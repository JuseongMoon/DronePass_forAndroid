package com.ScienceFiction.DronePassAndroid.feature.profile

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences.Key
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.account.AccountSession
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncState
import com.ScienceFiction.DronePassAndroid.core.data.sync.buildAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.encodeAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthRepository
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthSignOutStep
import com.ScienceFiction.DronePassAndroid.feature.auth.authSignOutSteps
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

internal val ACCOUNT_DELETION_SYNC_PREFERENCE_KEYS_TO_CLEAR: List<Key<*>> = listOf(
    SyncPreferenceKeys.LAST_SYNC_TIME,
    SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME,
    SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME,
    SyncPreferenceKeys.SYNCED_SHAPE_BASELINE,
    SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME,
    SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME,
)

internal fun shouldNotifyProfileSyncResultForCloudToggle(
    enabled: Boolean,
    isLoggedIn: Boolean,
): Boolean {
    return enabled && isLoggedIn
}

internal fun shouldAcceptProfileCloudBackupToggle(isSyncing: Boolean): Boolean = !isSyncing

internal const val ProfileCloudBackupRestartScheduleDelayMs = 100L

internal fun profileErrorDescription(localizedMessage: String?, fallback: String): String {
    return localizedMessage ?: fallback
}

internal fun accountDeletionRequiresProviderAuthentication(exception: Throwable): Boolean {
    val reason = (exception as? AccountDeletionException)?.reason
    return reason == AccountDeletionFailureReason.NOT_AUTHENTICATED ||
        reason == AccountDeletionFailureReason.APPLE_REAUTHENTICATION_FAILED ||
        reason == AccountDeletionFailureReason.APPLE_ACCESS_TOKEN_MISSING ||
        reason == AccountDeletionFailureReason.APPLE_TOKEN_REVOCATION_FAILED
}

internal fun shouldRestoreRealtimeSyncAfterAccountDeletionFailure(
    wasCloudBackupEnabled: Boolean,
    previousUserId: String?,
    isStillLoggedIn: Boolean,
): Boolean {
    return wasCloudBackupEnabled && previousUserId != null && isStillLoggedIn
}

internal fun normalizeProfileJoinDateMillis(timestamp: Long?): Long? =
    timestamp?.takeIf { it > 0L }

enum class ProfileLoginProvider {
    APPLE,
    GOOGLE,
    UNKNOWN,
}

internal fun resolveProfileLoginProvider(providerIds: List<String>): ProfileLoginProvider {
    return when {
        "apple.com" in providerIds -> ProfileLoginProvider.APPLE
        "google.com" in providerIds -> ProfileLoginProvider.GOOGLE
        else -> ProfileLoginProvider.UNKNOWN
    }
}

internal fun countExpiredProfileShapes(
    shapes: List<ShapeModel>,
    now: Long = System.currentTimeMillis(),
): Int {
    return shapes.count { shape ->
        shape.flightEndDate?.let { endDate -> endDate <= now } == true
    }
}

internal fun profileSyncSuccessCount(activeLocalShapesBeforeSync: List<ShapeModel>): Int {
    return activeLocalShapesBeforeSync.size
}

internal fun shouldStartProfileCloudSync(
    manualSyncing: Boolean,
    realtimeSyncState: SyncState,
): Boolean {
    return !isProfileSyncInProgress(
        manualSyncing = manualSyncing,
        realtimeSyncState = realtimeSyncState,
    )
}

internal fun isProfileSyncInProgress(
    manualSyncing: Boolean,
    realtimeSyncState: SyncState,
): Boolean {
    return manualSyncing || realtimeSyncState is SyncState.Syncing
}

internal fun shouldRestartProfileRealtimeSyncAfterToggle(
    enabled: Boolean,
    isLoggedIn: Boolean,
): Boolean {
    return enabled && isLoggedIn
}

internal fun buildProfileSyncedShapeBaseline(shapes: List<ShapeModel>): Map<String, Long> {
    return buildAccountSwitchShapeBaseline(shapes)
}

/**
 * iOS `ProfileView` 정합 ViewModel.
 * 동기화(클라우드 백업/실시간 sync) + 약관/정책 + 계정 관리(로그아웃·탈퇴) 흐름을 담당.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val shapeRepository: ShapeRepository,
    private val sketchRepository: SketchRepository,
    private val droneRepository: DroneRepository,
    private val accountDeletionService: AccountDeletionService,
    val subscriptionManager: com.ScienceFiction.DronePassAndroid.subscription.SubscriptionManager,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val accountSession: AccountSession,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    /** 실시간 클라우드 동기화 활성화 (iOS `isCloudBackupEnabled` 정합). */
    val isCloudBackupEnabled: StateFlow<Boolean> = dataStore.data
        .map(::storedCloudBackupEnabled)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 마지막 백업/동기화 시각 (UI 표시용 — DataStore 영속). */
    val lastBackupTime: StateFlow<Long?> = dataStore.data
        .map(::storedLastBackupTime)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** RealtimeSyncManager 의 마지막 실시간 동기화 시각 (메모리 상태 — UI 표시용). */
    val lastRealtimeSyncTime: StateFlow<Long?> = realtimeSyncManager.lastSyncTime

    /** 로그인 여부 — Firebase AuthStateListener 로 자동 갱신. */
    private val _isLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _profileEmail = MutableStateFlow(firebaseAuth.currentUser?.email)
    val profileEmail: StateFlow<String?> = _profileEmail.asStateFlow()

    private val _profileLoginProvider = MutableStateFlow(
        resolveProfileLoginProvider(
            firebaseAuth.currentUser?.providerData?.map { it.providerId }.orEmpty(),
        ),
    )
    val profileLoginProvider: StateFlow<ProfileLoginProvider> = _profileLoginProvider.asStateFlow()

    /** iOS ProfileView 가입일 행과 동일하게 Firebase Auth 생성 시각을 표시한다. */
    private val _joinDateMillis = MutableStateFlow(
        normalizeProfileJoinDateMillis(firebaseAuth.currentUser?.metadata?.creationTimestamp),
    )
    val joinDateMillis: StateFlow<Long?> = _joinDateMillis.asStateFlow()

    val activeShapeCount: StateFlow<Int> = shapeRepository.getActiveShapes()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiredShapeCount: StateFlow<Int> = shapeRepository.getActiveShapes()
        .map { shapes -> countExpiredProfileShapes(shapes) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeSketchCount: StateFlow<Int> = sketchRepository.getActiveSketches()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeDroneCount: StateFlow<Int> = droneRepository.getActiveDrones()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 진행 중 동기화 (수동 백업·토글 ON 시 활성). */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = combine(
        _isSyncing,
        realtimeSyncManager.syncState,
    ) { manualSyncing, syncState ->
        isProfileSyncInProgress(
            manualSyncing = manualSyncing,
            realtimeSyncState = syncState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 로그아웃·탈퇴 진행 중 (UI 버튼 비활성화용). */
    private val _isAccountActionInProgress = MutableStateFlow(false)
    val isAccountActionInProgress: StateFlow<Boolean> = _isAccountActionInProgress.asStateFlow()

    /** 1회성 동기화 결과 알림 (iOS alert 정합). */
    private val _syncResultMessage = MutableSharedFlow<SyncResult>()
    val syncResultMessage: SharedFlow<SyncResult> = _syncResultMessage.asSharedFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        _isLoggedIn.value = user != null
        _profileEmail.value = user?.email
        _profileLoginProvider.value = resolveProfileLoginProvider(
            user?.providerData?.map { it.providerId }.orEmpty(),
        )
        _joinDateMillis.value = normalizeProfileJoinDateMillis(user?.metadata?.creationTimestamp)
    }

    /**
     * 5종 동기화 상태 (iOS `realtimeCloudSyncStatusText` 정합).
     * isSyncing → Syncing
     * !isLoggedIn → LoginRequired
     * !cloudBackupEnabled → Disabled
     * isRealtimeSyncEnabled → Active
     * else → Waiting
     */
    val syncStatus: StateFlow<ProfileSyncStatus> = combine(
        isSyncing,
        isLoggedIn,
        isCloudBackupEnabled,
        realtimeSyncManager.isRealtimeSyncEnabled,
    ) { syncing, loggedIn, cloudEnabled, realtimeEnabled ->
        when {
            syncing -> ProfileSyncStatus.Syncing
            !loggedIn -> ProfileSyncStatus.LoginRequired
            !cloudEnabled -> ProfileSyncStatus.Disabled
            realtimeEnabled -> ProfileSyncStatus.Active
            else -> ProfileSyncStatus.Waiting
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileSyncStatus.Disabled)

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    /**
     * 실시간 클라우드 동기화 토글 (iOS `isCloudBackupEnabled` onChange 정합).
     * ON 전이 시 즉시 백업 + 실시간 리스너 재시작.
     */
    fun setCloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val syncInProgress = isProfileSyncInProgress(
                manualSyncing = _isSyncing.value,
                realtimeSyncState = realtimeSyncManager.syncState.value,
            )
            if (!shouldAcceptProfileCloudBackupToggle(syncInProgress)) return@launch

            dataStore.edit {
                it[ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED] = enabled
                it.remove(ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED)
            }
            if (enabled && firebaseAuth.currentUser != null) {
                viewModelScope.launch {
                    syncToCloudInternal(
                        notifyResult = shouldNotifyProfileSyncResultForCloudToggle(
                            enabled = enabled,
                            isLoggedIn = true,
                        ),
                    )
                }
                delay(ProfileCloudBackupRestartScheduleDelayMs)
                if (
                    shouldRestartProfileRealtimeSyncAfterToggle(
                        enabled = storedCloudBackupEnabled(dataStore.data.first()),
                        isLoggedIn = firebaseAuth.currentUser != null,
                    )
                ) {
                    realtimeSyncManager.resetAndRestartRealtimeSync()
                }
            } else if (!enabled) {
                runCatching { realtimeSyncManager.stopListening() }
            }
        }
    }

    /**
     * 수동 백업 (iOS `syncToCloud` 정합). 결과는 syncResultMessage 로 1회 emit.
     */
    fun syncToCloud() {
        viewModelScope.launch {
            syncToCloudInternal(notifyResult = true)
        }
    }

    private suspend fun syncToCloudInternal(notifyResult: Boolean) {
        if (!shouldStartProfileCloudSync(_isSyncing.value, realtimeSyncManager.syncState.value)) return
        _isSyncing.value = true
        try {
            val activeLocalShapesBeforeSync = shapeRepository.getActiveShapes().first()
            val syncedShapeCount = profileSyncSuccessCount(activeLocalShapesBeforeSync)
            realtimeSyncManager.forceSyncNow()
            val now = System.currentTimeMillis()
            dataStore.edit {
                it[ProfilePreferenceKeys.LAST_BACKUP_TIME] = now
                it.remove(ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME)
            }
            if (notifyResult) {
                _syncResultMessage.emit(SyncResult.Success(syncedShapeCount))
            }
        } catch (e: Exception) {
            Log.e(TAG, "수동 백업 실패", e)
            if (notifyResult) {
                _syncResultMessage.emit(
                    SyncResult.Failure(
                        profileErrorDescription(
                            localizedMessage = e.localizedMessage,
                            fallback = appContext.getString(R.string.common_unknown_error),
                        ),
                    ),
                )
            }
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * 로그아웃 (iOS `ProfileView.logout()` 정합).
     */
    fun signOut(onComplete: () -> Unit = {}) {
        if (_isAccountActionInProgress.value) return
        _isAccountActionInProgress.value = true
        viewModelScope.launch {
            // 1) 로그아웃 직전 로컬 데이터를 Firebase 로 동기화 (iOS ProfileView.logout 정합).
            if (firebaseAuth.currentUser != null) {
                runCatching {
                    realtimeSyncManager.forceSyncNow()
                    saveProfileSyncedShapeBaseline()
                }
                    .onFailure { Log.w(TAG, "로그아웃 전 동기화 실패", it) }
            }
            authSignOutSteps().forEach { step ->
                when (step) {
                    AuthSignOutStep.DEACTIVATE_FCM_TOKEN -> {
                        runCatching { FcmService.deactivateTokenAndWait(appContext) }
                            .onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패", it) }
                    }
                    AuthSignOutStep.STOP_REALTIME_SYNC -> {
                        runCatching { realtimeSyncManager.stopListening() }
                            .onFailure { Log.w(TAG, "리스너 중단 실패", it) }
                    }
                    AuthSignOutStep.SIGN_OUT -> {
                        authRepository.signOut()
                    }
                }
            }
            _isAccountActionInProgress.value = false
            onComplete()
        }
    }

    private suspend fun saveProfileSyncedShapeBaseline() {
        val baseline = buildProfileSyncedShapeBaseline(shapeRepository.getAllShapes().first())
        dataStore.edit { preferences ->
            preferences[SyncPreferenceKeys.SYNCED_SHAPE_BASELINE] =
                encodeAccountSwitchShapeBaseline(baseline)
        }
    }

    /**
     * 계정 삭제 (iOS `ProfileView.deleteAccount()` 정합 — 2단계 확인 후 호출).
     */
    fun deleteAccount(activity: Activity, onResult: (Boolean, String) -> Unit) {
        if (_isAccountActionInProgress.value) return
        _isAccountActionInProgress.value = true
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid
            val wasCloudBackupEnabled = storedCloudBackupEnabled(dataStore.data.first())
            runCatching { realtimeSyncManager.stopListening() }
                .onFailure { Log.w(TAG, "탈퇴 전 리스너 중단 실패", it) }

            accountDeletionService.deleteCurrentAccount(activity).fold(
                onSuccess = {
                    runCatching {
                        dataStore.edit {
                            it[ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED] = false
                            it.remove(ProfilePreferenceKeys.LAST_BACKUP_TIME)
                            it.remove(ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED)
                            it.remove(ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME)
                            ACCOUNT_DELETION_SYNC_PREFERENCE_KEYS_TO_CLEAR.forEach { key ->
                                it.remove(key)
                            }
                        }
                    }.onFailure { Log.w(TAG, "탈퇴 후 동기화 설정 정리 실패", it) }
                    runCatching { authRepository.completeAccountDeletionLocally() }
                        .onFailure { Log.w(TAG, "탈퇴 후 로컬 인증 정리 실패", it) }
                    _isAccountActionInProgress.value = false
                    onResult(true, appContext.getString(R.string.profile_delete_account_success))
                },
                onFailure = { exception ->
                    Log.e(TAG, "서버측 회원 탈퇴 실패", exception)
                    if (
                        shouldRestoreRealtimeSyncAfterAccountDeletionFailure(
                            wasCloudBackupEnabled = wasCloudBackupEnabled,
                            previousUserId = userId,
                            isStillLoggedIn = firebaseAuth.currentUser != null,
                        )
                    ) {
                        accountSession.syncTicket()?.let { ticket ->
                            runCatching { realtimeSyncManager.startListening(ticket) }
                                .onFailure { Log.w(TAG, "탈퇴 실패 후 리스너 복구 실패", it) }
                        }
                    }
                    _isAccountActionInProgress.value = false
                    onResult(
                        false,
                        if (accountDeletionRequiresProviderAuthentication(exception)) {
                            appContext.getString(R.string.profile_delete_account_requires_recent_login)
                        } else {
                            appContext.getString(R.string.profile_delete_account_error)
                        },
                    )
                },
            )
        }
    }

    /** 동기화 결과 — Toast/SnackBar 용 1회성 메시지. */
    sealed class SyncResult {
        data class Success(val shapeCount: Int) : SyncResult()
        data class Failure(val message: String) : SyncResult()
    }
}
