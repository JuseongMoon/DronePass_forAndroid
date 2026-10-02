package com.ScienceFiction.DronePassAndroid.feature.profile

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.account.AccountSessionFlows
import com.ScienceFiction.DronePassAndroid.core.account.LogoutResult
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncState
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
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
import kotlinx.coroutines.launch
import javax.inject.Inject

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

/**
 * 동기화 상태 라벨(iOS `realtimeCloudSyncStatusText` 정합). 클라우드 동기화 토글은 없다: 로그인하면 동기화 관문이 열려
 * 있는 동안 항상 동기화한다. 가져오기 확인을 기다리는 중이면 "가져오기 확인 필요".
 */
internal fun resolveProfileSyncStatus(
    syncing: Boolean,
    loggedIn: Boolean,
    importPending: Boolean,
    realtimeEnabled: Boolean,
): ProfileSyncStatus = when {
    syncing -> ProfileSyncStatus.Syncing
    !loggedIn -> ProfileSyncStatus.LoginRequired
    importPending -> ProfileSyncStatus.ImportPending
    realtimeEnabled -> ProfileSyncStatus.Active
    else -> ProfileSyncStatus.Waiting
}

/**
 * iOS `ProfileView` 정합 ViewModel.
 * 동기화(클라우드 백업/실시간 sync) + 약관/정책 + 계정 관리(로그아웃·탈퇴) 흐름을 담당.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val firebaseAuth: FirebaseAuth,
    private val shapeRepository: ShapeRepository,
    private val sketchRepository: SketchRepository,
    private val droneRepository: DroneRepository,
    val subscriptionManager: com.ScienceFiction.DronePassAndroid.subscription.SubscriptionManager,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val accountSessionFlows: AccountSessionFlows,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    /** 가져오기 확인을 기다리는 중인지(기기 데이터 주인, 3.6.0). */
    val isImportPending: StateFlow<Boolean> = accountSessionFlows.importPending
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

    /** 로그아웃 ① 업로드가 서버에 닿지 못했다: "인터넷 연결 없음" 경고를 띄운다. */
    private val _logoutOfflineConfirmation = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutOfflineConfirmation: SharedFlow<Unit> = _logoutOfflineConfirmation.asSharedFlow()

    /** 로그아웃 실패(로그인 상태 유지). */
    private val _logoutFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutFailed: SharedFlow<Unit> = _logoutFailed.asSharedFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        _isLoggedIn.value = user != null
        _profileEmail.value = user?.email
        _profileLoginProvider.value = resolveProfileLoginProvider(
            user?.providerData?.map { it.providerId }.orEmpty(),
        )
        _joinDateMillis.value = normalizeProfileJoinDateMillis(user?.metadata?.creationTimestamp)
    }

    /** 동기화 상태 라벨. 규칙은 [resolveProfileSyncStatus]. */
    val syncStatus: StateFlow<ProfileSyncStatus> = combine(
        isSyncing,
        isLoggedIn,
        isImportPending,
        realtimeSyncManager.isRealtimeSyncEnabled,
    ) { syncing, loggedIn, importPending, realtimeEnabled ->
        resolveProfileSyncStatus(syncing, loggedIn, importPending, realtimeEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileSyncStatus.Waiting)

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
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
     * 로그아웃(기기 데이터 주인, 3.6.0): 계정에 없는 변경만 올린 뒤 로그아웃하고 이 기기의 계정 데이터를 지운다.
     * 앱 범위에서 끝까지 진행하므로 화면을 닫아도 중간에 끊기지 않는다. 서버에 닿지 못하면 경고를 띄우고 멈춘다.
     *
     * @param proceedWithoutUpload 경고에서 [로그아웃]을 고른 경우 true.
     */
    fun signOut(proceedWithoutUpload: Boolean = false, onComplete: () -> Unit = {}) {
        if (_isAccountActionInProgress.value) return
        _isAccountActionInProgress.value = true
        viewModelScope.launch {
            val result = accountSessionFlows.logout(proceedWithoutUpload)
            _isAccountActionInProgress.value = false
            when (result) {
                LogoutResult.DONE -> onComplete()
                LogoutResult.NEEDS_OFFLINE_CONFIRMATION -> _logoutOfflineConfirmation.tryEmit(Unit)
                LogoutResult.FAILED -> _logoutFailed.tryEmit(Unit)
            }
        }
    }

    /**
     * 계정 삭제 (iOS `ProfileView.deleteAccount()` 정합 — 2단계 확인 후 호출). 서버 처리가 끝나면 이 기기의 계정 데이터도 지운다.
     */
    fun deleteAccount(activity: Activity, onResult: (Boolean, String) -> Unit) {
        if (_isAccountActionInProgress.value) return
        _isAccountActionInProgress.value = true
        viewModelScope.launch {
            accountSessionFlows.deleteAccount(activity).fold(
                onSuccess = {
                    _isAccountActionInProgress.value = false
                    onResult(true, appContext.getString(R.string.profile_delete_account_success))
                },
                onFailure = { exception ->
                    Log.e(TAG, "서버측 회원 탈퇴 실패", exception)
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
