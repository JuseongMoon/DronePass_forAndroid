package com.ScienceFiction.DronePassAndroid.feature.auth

import com.google.firebase.auth.FirebaseUser

/**
 * 인증 상태를 나타내는 sealed class
 * ViewModel에서 UI로 전달되는 상태 모델
 */
sealed class AuthState {
    /** 인증 상태 확인 중 (로딩) */
    data object Loading : AuthState()

    /** 로그인 완료 상태 */
    data class LoggedIn(val user: FirebaseUser) : AuthState()

    /** 로그아웃 상태 */
    data object LoggedOut : AuthState()

    /** 인증 오류 발생 */
    data class Error(val message: String) : AuthState()
}
