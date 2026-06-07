package com.ScienceFiction.DronePassAndroid.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionState
import com.ScienceFiction.DronePassAndroid.feature.profile.ProfilePreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.profile.storedCloudBackupEnabled
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.ScienceFiction.DronePassAndroid.R
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
    START_REALTIME_AND_SYNC,
}

internal enum class AuthProviderSignInAction {
    START,
    IGNORE,
}

data class AccountSwitchConfirmationRequest(
    val localDataCount: Int,
)

private data class PendingAccountSwitchLogin(
    val result: AuthSignInResult,
    val providerName: String,
    val selectAllDronesAfterSync: Boolean,
)

internal fun resolveAuthProviderSignInAction(authState: AuthState): AuthProviderSignInAction {
    return when (authState) {
        AuthState.LoggedOut,
        is AuthState.Error -> AuthProviderSignInAction.START
        AuthState.Loading,
        is AuthState.LoggedIn -> AuthProviderSignInAction.IGNORE
    }
}

internal fun shouldSuppressGoogleSignInFailure(exception: Throwable): Boolean {
    return exception is GetCredentialCancellationException
}

internal fun shouldResetLocalDataForAccountChange(action: AuthAccountChangeAction): Boolean {
    return action == AuthAccountChangeAction.RESET_LOCAL_DATA
}

internal fun shouldRequestAccountSwitchConfirmation(localDataCount: Int): Boolean {
    return localDataCount > 0
}

internal fun resolveForegroundCloudSyncAction(
    isLoggedIn: Boolean,
    cloudBackupEnabled: Boolean,
    realtimeSyncEnabled: Boolean,
): ForegroundCloudSyncAction {
    return if (isLoggedIn && cloudBackupEnabled && !realtimeSyncEnabled) {
        ForegroundCloudSyncAction.START_REALTIME_AND_SYNC
    } else {
        ForegroundCloudSyncAction.NO_OP
    }
}

/**
 * 인증 화면의 ViewModel.
 *
 * 지원 로그인:
 *  - Google Sign-In (Credential Manager)
 *  - Apple Sign-In (Firebase OAuthProvider — iOS Apple Sign-In 사용자 데이터 자동 호환)
 *
 * 로그인 성공 시 Firebase 양방향 동기화 자동 실행 및 실시간 동기화 리스너 시작.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    private val sketchRepository: SketchRepository,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val droneSelectionState: DroneSelectionState,
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

    private val _accountSwitchConfirmation = MutableStateFlow<AccountSwitchConfirmationRequest?>(null)
    val accountSwitchConfirmation: StateFlow<AccountSwitchConfirmationRequest?> =
        _accountSwitchConfirmation.asStateFlow()

    private var pendingAccountSwitchLogin: PendingAccountSwitchLogin? = null

    init {
        // 1. 로그인 상태를 _authState 에 동기 반영 (Loading → LoggedIn/LoggedOut)
        val currentUser = authRepository.currentUser
        _authState.value = if (currentUser != null) {
            AuthState.LoggedIn(currentUser)
        } else {
            AuthState.LoggedOut
        }

        // 2. 이미 로그인 상태이면 Firebase 양방향 동기화 + 실시간 리스너 시작.
        //    LoginScreen 의 LaunchedEffect 가 LoggedIn 전이를 받기 전에 동기화가
        //    시작되어 화면이 잠시 깜빡이는 일이 없도록 1번을 먼저 수행한다.
        if (currentUser != null) {
            activateCloudSyncAfterLogin(selectAllDronesAfterSync = false)
            requestFcmToken()
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
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            val cloudBackupEnabled = storedCloudBackupEnabled(dataStore.data.first())
            val action = resolveForegroundCloudSyncAction(
                isLoggedIn = true,
                cloudBackupEnabled = cloudBackupEnabled,
                realtimeSyncEnabled = realtimeSyncManager.isRealtimeSyncEnabled.value,
            )
            if (action == ForegroundCloudSyncAction.START_REALTIME_AND_SYNC) {
                Log.d(TAG, "포그라운드 복귀: 실시간 동기화 리스너 복구 userId=${user.uid}")
                realtimeSyncManager.startListening(user.uid)
                performFullSync(selectAllDronesAfterSync = false)
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
                onSuccess = { result ->
                    handleSuccessfulProviderLogin(
                        result = result,
                        providerName = "google",
                        selectAllDronesAfterSync = true,
                    )
                },
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
                onSuccess = { result ->
                    handleSuccessfulProviderLogin(
                        result = result,
                        providerName = "apple",
                        selectAllDronesAfterSync = true,
                    )
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.localizedMessage ?: appContext.getString(R.string.login_apple_error)
                    )
                }
            )
        }
    }

    /**
     * 로그아웃
     * FCM 토큰 비활성화 -> 실시간 동기화 리스너 중단 -> Firebase Auth 로그아웃
     */
    fun signOut() {
        // FCM 토큰 비활성화 (로그아웃 전에 userId가 필요하므로 먼저 호출)
        FcmService.deactivateToken(appContext)
        Log.d(TAG, "FCM 토큰 비활성화 요청")

        // 실시간 동기화 리스너 중단
        realtimeSyncManager.stopListening()
        Log.d(TAG, "실시간 동기화 리스너 중단")

        authRepository.signOut()
        analyticsLogger.logLogout()
        _authState.value = AuthState.LoggedOut
    }

    fun confirmAccountSwitch() {
        val pending = pendingAccountSwitchLogin ?: return
        pendingAccountSwitchLogin = null
        _accountSwitchConfirmation.value = null
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            completeSuccessfulProviderLogin(
                result = pending.result,
                providerName = pending.providerName,
                selectAllDronesAfterSync = pending.selectAllDronesAfterSync,
                prepareAccountSwitchBeforeNavigation = true,
            )
        }
    }

    fun cancelAccountSwitch() {
        pendingAccountSwitchLogin = null
        _accountSwitchConfirmation.value = null
        authRepository.signOut()
        _authState.value = AuthState.LoggedOut
    }

    /**
     * iOS 로그인 성공 흐름 정합:
     * 클라우드 백업 설정을 켠 뒤 실시간 리스너와 Firebase 양방향 동기화를 시작한다.
     */
    private fun activateCloudSyncAfterLogin(
        selectAllDronesAfterSync: Boolean,
        accountChangeAction: AuthAccountChangeAction = AuthAccountChangeAction.KEEP_LOCAL_DATA,
    ) {
        viewModelScope.launch {
            prepareCloudSyncAfterLogin(accountChangeAction)
            performFullSync(selectAllDronesAfterSync)
        }
    }

    private suspend fun handleSuccessfulProviderLogin(
        result: AuthSignInResult,
        providerName: String,
        selectAllDronesAfterSync: Boolean,
    ) {
        if (shouldResetLocalDataForAccountChange(result.accountChangeAction)) {
            val localDataCount = countLocalDataForAccountSwitch()
            if (shouldRequestAccountSwitchConfirmation(localDataCount)) {
                pendingAccountSwitchLogin = PendingAccountSwitchLogin(
                    result = result,
                    providerName = providerName,
                    selectAllDronesAfterSync = selectAllDronesAfterSync,
                )
                _accountSwitchConfirmation.value = AccountSwitchConfirmationRequest(
                    localDataCount = localDataCount,
                )
                return
            }
        }

        completeSuccessfulProviderLogin(
            result = result,
            providerName = providerName,
            selectAllDronesAfterSync = selectAllDronesAfterSync,
            prepareAccountSwitchBeforeNavigation = false,
        )
    }

    private suspend fun completeSuccessfulProviderLogin(
        result: AuthSignInResult,
        providerName: String,
        selectAllDronesAfterSync: Boolean,
        prepareAccountSwitchBeforeNavigation: Boolean,
    ) {
        authRepository.finalizeSuccessfulSignIn(result)
        if (prepareAccountSwitchBeforeNavigation) {
            prepareCloudSyncAfterLogin(result.accountChangeAction)
            _authState.value = AuthState.LoggedIn(result.user)
            viewModelScope.launch {
                performFullSync(selectAllDronesAfterSync)
            }
        } else {
            _authState.value = AuthState.LoggedIn(result.user)
            activateCloudSyncAfterLogin(
                selectAllDronesAfterSync = selectAllDronesAfterSync,
                accountChangeAction = result.accountChangeAction,
            )
        }
        analyticsLogger.logLogin(providerName)
        requestFcmToken()
    }

    private suspend fun prepareCloudSyncAfterLogin(accountChangeAction: AuthAccountChangeAction) {
        enableCloudBackupForLogin()
        if (shouldResetLocalDataForAccountChange(accountChangeAction)) {
            resetLocalDataForAccountSwitch()
        }
        startRealtimeSync()
    }

    private suspend fun enableCloudBackupForLogin() {
        runCatching {
            dataStore.edit { preferences ->
                preferences[ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED] = true
                preferences.remove(ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED)
            }
        }.onFailure { error ->
            Log.e(TAG, "클라우드 백업 설정 저장 실패", error)
        }
    }

    private suspend fun resetLocalDataForAccountSwitch() {
        Log.d(TAG, "계정 전환 감지: 로컬 데이터 초기화 시작")
        realtimeSyncManager.stopListening()
        realtimeSyncManager.resetSyncTrackingForAccountSwitch()
        shapeRepository.deleteAllShapes()
        droneRepository.deleteAllDrones()
        sketchRepository.deleteAllSketchesLocally()
        droneSelectionState.resetForAccountSwitch()
        dataStore.edit { preferences ->
            preferences.remove(ProfilePreferenceKeys.LAST_BACKUP_TIME)
            preferences.remove(ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME)
        }
        Log.d(TAG, "계정 전환 감지: 로컬 데이터 초기화 완료")
    }

    private suspend fun countLocalDataForAccountSwitch(): Int {
        val shapes = shapeRepository.getAllShapes().first().size
        val drones = droneRepository.getAllDrones().first().size
        val sketches = sketchRepository.getAllSketches().first().size
        return shapes + drones + sketches
    }

    /**
     * 모든 Repository에 대해 Firebase 양방향 동기화 실행
     * 로그인 성공 시 및 앱 시작 시(이미 로그인 상태) 호출
     */
    private suspend fun performFullSync(selectAllDronesAfterSync: Boolean) {
        try {
            Log.d(TAG, "Firebase 양방향 동기화 시작")
            shapeRepository.performFullSync()
            droneRepository.performFullSync()
            if (selectAllDronesAfterSync) {
                selectAllActiveDrones()
            }
            sketchRepository.performFullSync()
            Log.d(TAG, "Firebase 양방향 동기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 동기화 실패", e)
            _syncMessage.tryEmit(appContext.getString(R.string.login_sync_failed))
        }
    }

    private suspend fun selectAllActiveDrones() {
        val activeDrones = droneRepository.getActiveDrones().first()
        droneSelectionState.selectAllDrones(activeDrones)
        Log.d(TAG, "로그인 후 활성 드론 전체 선택: count=${activeDrones.size}")
    }

    /**
     * 실시간 동기화 리스너 시작
     * 로그인된 사용자의 userId로 Firestore SnapshotListener를 등록
     */
    private fun startRealtimeSync() {
        val userId = authRepository.currentUser?.uid ?: run {
            Log.d(TAG, "실시간 동기화 시작 실패: userId를 가져올 수 없습니다.")
            return
        }

        Log.d(TAG, "실시간 동기화 리스너 시작: userId=$userId")
        realtimeSyncManager.startListening(userId)
    }

    private fun requestFcmToken() {
        FcmService.requestAndSaveToken(appContext)
        Log.d(TAG, "FCM 토큰 요청")
    }
}
