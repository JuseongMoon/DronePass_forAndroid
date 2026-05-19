package com.ScienceFiction.DronePassAndroid.feature.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.service.FcmService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 인증 화면의 ViewModel
 * Google Sign-In 및 로그인 상태 관리
 * 로그인 성공 시 Firebase 양방향 동기화 자동 실행 및 실시간 동기화 리스너 시작
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    private val sketchRepository: SketchRepository,
    private val realtimeSyncManager: RealtimeSyncManager,
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
            performFullSync()
            startRealtimeSync()
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
     * Google Sign-In 실행
     * Credential Manager를 통해 Google 계정 선택 후 Firebase 인증
     * 로그인 성공 시 Firebase 양방향 동기화 자동 실행 및 실시간 리스너 시작
     *
     * @param context Activity Context (Credential Manager에 필요)
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            authRepository.signInWithGoogle(context).fold(
                onSuccess = { user ->
                    _authState.value = AuthState.LoggedIn(user)
                    analyticsLogger.logLogin("google")
                    // 로그인 성공 후 Firebase 양방향 동기화
                    performFullSync()
                    // 실시간 동기화 리스너 시작
                    startRealtimeSync()
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.localizedMessage ?: "Google 로그인 중 오류가 발생했습니다."
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

    /**
     * 모든 Repository에 대해 Firebase 양방향 동기화 실행
     * 로그인 성공 시 및 앱 시작 시(이미 로그인 상태) 호출
     */
    private fun performFullSync() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Firebase 양방향 동기화 시작")
                shapeRepository.performFullSync()
                droneRepository.performFullSync()
                sketchRepository.performFullSync()
                Log.d(TAG, "Firebase 양방향 동기화 완료")
            } catch (e: Exception) {
                Log.e(TAG, "Firebase 동기화 실패", e)
                _syncMessage.tryEmit("데이터 동기화에 실패했습니다. 네트워크 상태를 확인해주세요.")
            }
        }
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
}
