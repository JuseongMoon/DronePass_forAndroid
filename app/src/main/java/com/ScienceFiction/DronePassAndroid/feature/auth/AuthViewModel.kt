package com.ScienceFiction.DronePassAndroid.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.account.AccountSession
import com.ScienceFiction.DronePassAndroid.core.account.AccountSessionEvent
import com.ScienceFiction.DronePassAndroid.core.account.AccountSessionFlows
import com.ScienceFiction.DronePassAndroid.core.account.StaleSyncTicketException
import com.ScienceFiction.DronePassAndroid.core.account.SyncTicket
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.buildAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.encodeAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.recordShapeRealtimeSyncSuccess
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.ScienceFiction.DronePassAndroid.R
import com.google.firebase.auth.FirebaseAuthWebException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class ForegroundCloudSyncAction {
    NO_OP,
    REQUEST_USER_CONFIRMATION,
}

internal enum class ForegroundCloudSyncDomain {
    Shape,
}

internal sealed interface ForegroundSyncDialogState {
    data object Loading : ForegroundSyncDialogState
    data object Complete : ForegroundSyncDialogState
    data class Error(val message: String) : ForegroundSyncDialogState
}

internal enum class AuthProviderSignInAction {
    START,
    IGNORE,
}

private sealed interface FullSyncResult {
    data object Success : FullSyncResult
    data class Failure(val message: String) : FullSyncResult
}

internal fun resolveAuthProviderSignInAction(authState: AuthState): AuthProviderSignInAction {
    return when (authState) {
        AuthState.LoggedOut,
        is AuthState.Error -> AuthProviderSignInAction.START
        AuthState.Loading,
        is AuthState.LoggedIn -> AuthProviderSignInAction.IGNORE
    }
}

internal fun shouldSuppressGoogleSignInFailure(exception: Throwable): Boolean {
    return exception is GetCredentialCancellationException ||
        exception is NoCredentialException
}

internal fun isAppleSignInCancellationErrorCode(errorCode: String?): Boolean {
    return errorCode in setOf(
        "ERROR_WEB_CONTEXT_CANCELED",
        "ERROR_WEB_CONTEXT_CANCELLED",
    )
}

internal fun shouldSuppressAppleSignInFailure(exception: Throwable): Boolean {
    if (exception !is FirebaseAuthWebException) return false
    return isAppleSignInCancellationErrorCode(exception.errorCode)
}

/**
 * 앱 복귀 시 리스너가 꺼져 있으면 변경 확인을 묻는다. 동기화 관문이 닫혀 있으면(비로그인·가져오기 확인 대기·
 * 로그아웃 중 등) 이 대체 경로도 열리지 않는다.
 */
internal fun resolveForegroundCloudSyncAction(
    syncGateOpen: Boolean,
    realtimeSyncEnabled: Boolean,
): ForegroundCloudSyncAction {
    return if (syncGateOpen && !realtimeSyncEnabled) {
        ForegroundCloudSyncAction.REQUEST_USER_CONFIRMATION
    } else {
        ForegroundCloudSyncAction.NO_OP
    }
}

internal fun foregroundCloudSyncDomains(): List<ForegroundCloudSyncDomain> {
    return listOf(ForegroundCloudSyncDomain.Shape)
}

internal const val ForegroundRemoteChangeCheckThrottleMs = 30_000L

internal fun shouldCheckForegroundRemoteChanges(
    hasCheckedForChanges: Boolean,
    isSyncing: Boolean,
    nowMillis: Long,
    lastCheckTimeMillis: Long?,
): Boolean {
    if (hasCheckedForChanges || isSyncing) return false
    return lastCheckTimeMillis == null ||
        nowMillis - lastCheckTimeMillis >= ForegroundRemoteChangeCheckThrottleMs
}

/**
 * 인증 화면의 ViewModel.
 *
 * 지원 로그인:
 *  - Google Sign-In (Credential Manager)
 *  - Apple Sign-In (Firebase OAuthProvider — iOS Apple Sign-In 사용자 데이터 자동 호환)
 *
 * 로그인 뒤 계정 데이터 동기화는 [AccountSessionFlows] 가 로그인 상태 변화를 보고 판단한다(기기 데이터 주인 관문).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val shapeRepository: ShapeRepository,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val accountSession: AccountSession,
    private val accountSessionFlows: AccountSessionFlows,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val appContext: Context,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * 동기화 상태 메시지 (1회성 이벤트).
     *
     * StateFlow + consume nullable 패턴 대신 SharedFlow 로 변경하여 이벤트 의미를 명확화한다.
     * - replay = 0 : 늦은 collector 는 이전 이벤트를 받지 않음 (한 번 보여진 메시지는 끝).
     * - extraBufferCapacity = 1 : collect 가 시작되기 전 emit 도 1개 버퍼링 가능.
     */
    private val _syncMessage = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val syncMessage: SharedFlow<String> = _syncMessage.asSharedFlow()

    private val _foregroundSyncConfirmation = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val foregroundSyncConfirmation: SharedFlow<Unit> = _foregroundSyncConfirmation.asSharedFlow()

    private val _foregroundSyncDialogState = MutableSharedFlow<ForegroundSyncDialogState>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    internal val foregroundSyncDialogState: SharedFlow<ForegroundSyncDialogState> =
        _foregroundSyncDialogState.asSharedFlow()

    private var hasCheckedForegroundRemoteChanges = false
    private var lastForegroundRemoteChangeCheckTimeMillis: Long? = null
    private var isForegroundSyncing = false

    init {
        // 로그인 상태를 _authState 에 동기 반영 (Loading → LoggedIn/LoggedOut).
        // 계정 데이터 동기화는 AccountSessionFlows 가 앱 시작 시 판단한다.
        val currentUser = authRepository.currentUser
        _authState.value = if (currentUser != null) {
            AuthState.LoggedIn(currentUser)
        } else {
            AuthState.LoggedOut
        }
        if (currentUser != null) {
            requestFcmToken()
        }
        viewModelScope.launch {
            accountSessionFlows.events.collect { event ->
                when (event) {
                    AccountSessionEvent.SyncFailed ->
                        _syncMessage.tryEmit(appContext.getString(R.string.login_sync_failed))
                }
            }
        }
    }

    /**
     * 앱 시작 시 또는 상태 확인이 필요할 때 현재 로그인 상태를 확인
     */
    fun checkAuthState() {
        val currentUser = authRepository.currentUser
        _authState.value = if (currentUser != null) {
            AuthState.LoggedIn(currentUser)
        } else {
            AuthState.LoggedOut
        }
    }

    /**
     * iOS `applicationDidBecomeActive` 의 fallback 정합.
     * 앱 복귀 시 실시간 동기화 리스너가 꺼져 있으면 변경사항 확인/동기화 경로를 복구한다.
     */
    fun ensureCloudSyncActiveOnForeground() {
        viewModelScope.launch {
            val action = resolveForegroundCloudSyncAction(
                syncGateOpen = accountSession.syncTicket() != null,
                realtimeSyncEnabled = realtimeSyncManager.isRealtimeSyncEnabled.value,
            )
            if (action == ForegroundCloudSyncAction.REQUEST_USER_CONFIRMATION) {
                val nowMillis = System.currentTimeMillis()
                if (!shouldCheckForegroundRemoteChanges(
                        hasCheckedForChanges = hasCheckedForegroundRemoteChanges,
                        isSyncing = isForegroundSyncing,
                        nowMillis = nowMillis,
                        lastCheckTimeMillis = lastForegroundRemoteChangeCheckTimeMillis,
                    )
                ) {
                    return@launch
                }
                lastForegroundRemoteChangeCheckTimeMillis = nowMillis
                val remoteChangesResult = runCatching {
                    realtimeSyncManager.hasForegroundShapeRemoteChanges()
                }
                    .onFailure { Log.w(TAG, "포그라운드 원격 변경 확인 실패", it) }
                if (remoteChangesResult.isSuccess) {
                    hasCheckedForegroundRemoteChanges = true
                }
                val hasRemoteChanges = remoteChangesResult.getOrDefault(false)
                if (hasRemoteChanges) {
                    _foregroundSyncConfirmation.tryEmit(Unit)
                }
            }
        }
    }

    fun resetForegroundSyncCheckStatus() {
        hasCheckedForegroundRemoteChanges = false
        lastForegroundRemoteChangeCheckTimeMillis = null
    }

    fun confirmForegroundCloudSync() {
        viewModelScope.launch {
            val ticket = accountSession.syncTicket() ?: return@launch
            val action = resolveForegroundCloudSyncAction(
                syncGateOpen = true,
                realtimeSyncEnabled = realtimeSyncManager.isRealtimeSyncEnabled.value,
            )
            if (action == ForegroundCloudSyncAction.REQUEST_USER_CONFIRMATION) {
                if (isForegroundSyncing) return@launch
                Log.d(TAG, "포그라운드 복귀: 사용자 확인 후 실시간 동기화 리스너 복구")
                isForegroundSyncing = true
                try {
                    realtimeSyncManager.startListening(ticket)
                    _foregroundSyncDialogState.tryEmit(ForegroundSyncDialogState.Loading)
                    when (val result = performForegroundCloudSync(ticket)) {
                        FullSyncResult.Success ->
                            _foregroundSyncDialogState.tryEmit(ForegroundSyncDialogState.Complete)
                        is FullSyncResult.Failure ->
                            _foregroundSyncDialogState.tryEmit(
                                ForegroundSyncDialogState.Error(result.message),
                            )
                    }
                } finally {
                    isForegroundSyncing = false
                }
            }
        }
    }

    /**
     * Google Sign-In 실행
     * Credential Manager를 통해 Google 계정 선택 후 Firebase 인증
     * 로그인 성공 시 Firebase 양방향 동기화 자동 실행 및 실시간 리스너 시작
     *
     * @param context Activity Context (Credential Manager에 필요)
     */
    fun signInWithGoogle(context: Context) {
        if (resolveAuthProviderSignInAction(_authState.value) == AuthProviderSignInAction.IGNORE) {
            return
        }
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.signInWithGoogle(context).fold(
                onSuccess = { result -> completeSuccessfulProviderLogin(result, providerName = "google") },
                onFailure = { exception ->
                    _authState.value = if (shouldSuppressGoogleSignInFailure(exception)) {
                        AuthState.LoggedOut
                    } else {
                        AuthState.Error(
                            exception.localizedMessage ?: appContext.getString(R.string.login_google_error)
                        )
                    }
                }
            )
        }
    }

    /**
     * Sign in with Apple 실행.
     *
     * Firebase OAuthProvider("apple.com") 가 Chrome Custom Tabs 를 띄워 Apple OAuth flow 처리.
     * 별도 SDK 불필요. 결과 UID 는 iOS native Apple Sign-In 의 UID 와 동일 (같은 Apple ID).
     *
     * @param activity Activity (Custom Tabs intent launch 에 필요 — Context 불가)
     */
    fun signInWithApple(activity: Activity) {
        if (resolveAuthProviderSignInAction(_authState.value) == AuthProviderSignInAction.IGNORE) {
            return
        }
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            authRepository.signInWithApple(activity).fold(
                onSuccess = { result -> completeSuccessfulProviderLogin(result, providerName = "apple") },
                onFailure = { exception ->
                    _authState.value = if (shouldSuppressAppleSignInFailure(exception)) {
                        AuthState.LoggedOut
                    } else {
                        AuthState.Error(
                            exception.localizedMessage ?: appContext.getString(R.string.login_apple_error)
                        )
                    }
                }
            )
        }
    }

    /**
     * 로그인 후처리. 사용자 문서·활동 시각(계정 메타데이터, 관문 밖)과 푸시 토큰만 다룬다.
     * 기기 데이터를 계정으로 올릴지·내려받을지는 [AccountSessionFlows] 가 로그인 상태 변화를 보고 판단한다.
     */
    private suspend fun completeSuccessfulProviderLogin(result: AuthSignInResult, providerName: String) {
        authRepository.finalizeSuccessfulSignIn(result)
        _authState.value = AuthState.LoggedIn(result.user)
        analyticsLogger.logLogin(providerName)
        requestFcmToken()
    }

    private suspend fun saveSyncedShapeBaseline(ticket: SyncTicket) {
        val activeShapeUpdatedAtById = buildAccountSwitchShapeBaseline(
            shapeRepository.getAllShapes().first(),
        )
        accountSession.commit(ticket) {
            dataStore.edit { preferences ->
                preferences[SyncPreferenceKeys.SYNCED_SHAPE_BASELINE] =
                    encodeAccountSwitchShapeBaseline(activeShapeUpdatedAtById)
            }
        }
    }

    /**
     * iOS `ChangeDetectionManager.performSync()` 정합.
     *
     * 포그라운드 복귀 fallback 프롬프트는 Shape 변경 감지/도형 정보 최신화 문구를
     * 표시하므로, 확인 버튼도 Shape 동기화 결과만 완료/실패 다이얼로그에 반영한다.
     */
    private suspend fun performForegroundCloudSync(ticket: SyncTicket): FullSyncResult {
        return try {
            foregroundCloudSyncDomains().forEach { domain ->
                when (domain) {
                    ForegroundCloudSyncDomain.Shape -> {
                        shapeRepository.performFullSync(ticket)
                        saveSyncedShapeBaseline(ticket)
                        val shapeSyncTime = System.currentTimeMillis()
                        accountSession.commit(ticket) {
                            dataStore.edit { preferences ->
                                preferences.recordShapeRealtimeSyncSuccess(shapeSyncTime)
                            }
                        }
                    }
                }
            }
            FullSyncResult.Success
        } catch (e: StaleSyncTicketException) {
            FullSyncResult.Failure(appContext.getString(R.string.login_sync_failed))
        } catch (e: Exception) {
            Log.e(TAG, "포그라운드 Shape 동기화 실패", e)
            _syncMessage.tryEmit(appContext.getString(R.string.login_sync_failed))
            FullSyncResult.Failure(
                e.localizedMessage ?: appContext.getString(R.string.common_unknown_error),
            )
        }
    }

    private fun requestFcmToken() {
        FcmService.requestAndSaveToken(appContext)
        Log.d(TAG, "FCM 토큰 요청")
    }
}
