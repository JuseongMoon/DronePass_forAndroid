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

/** Firebase 로그인 사용자를 화면 상태로 바꾼다. */
internal fun authStateFor(user: FirebaseUser?): AuthState =
    if (user != null) AuthState.LoggedIn(user) else AuthState.LoggedOut

/**
 * 로그인 상태가 앱 어디서든 바뀌면(기기 데이터 흐름의 로그아웃·탈퇴 정리, SDK 의 세션 만료 등) 화면도 따라간다.
 * 다만 로그인 화면은 로그인을 진행하는 동안(Loading) 후처리가 끝날 때까지 기다린다.
 */
internal fun shouldFollowAuthChange(current: AuthState, keepWhileSigningIn: Boolean): Boolean =
    !(keepWhileSigningIn && current is AuthState.Loading)
