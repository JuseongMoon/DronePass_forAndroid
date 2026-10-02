package com.ScienceFiction.DronePassAndroid.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 107 실기기 버그: 다른 기기 탈퇴를 감지해 기기 데이터 흐름이 signOut 했는데 설정 첫 줄이 "내 프로필"로 남았다.
 * 로그인 상태를 보여 주는 화면은 앱 범위의 로그인 사용자 변화를 따라가야 한다.
 */
class AuthStateFollowTest {

    @Test
    fun `a sign out from anywhere makes the screen state logged out`() {
        // AccountSessionFlows 의 signOut 이나 SDK 의 세션 만료로 사용자가 null 이 되면
        assertEquals(AuthState.LoggedOut, authStateFor(null))
        // 로그인 화면이 아닌 곳(설정)은 언제나 따라가고, 로그인 화면도 진행 중이 아니면 따라간다.
        listOf(AuthState.LoggedOut, AuthState.Error("x")).forEach { current ->
            assertTrue(shouldFollowAuthChange(current, keepWhileSigningIn = true))
        }
        assertTrue(shouldFollowAuthChange(AuthState.Loading, keepWhileSigningIn = false))
        // 로그인 진행 중에는 로그인 후처리(사용자 문서·푸시 토큰)가 끝나야 LoggedIn 이 된다.
        assertFalse(shouldFollowAuthChange(AuthState.Loading, keepWhileSigningIn = true))
    }

    @Test
    fun `the signed in user flow is fed by the firebase auth listener for the whole app`() {
        val repository = source("feature/auth/AuthRepository.kt")
        assertTrue(repository.contains("firebaseAuth.addAuthStateListener { auth -> _signedInUser.value = auth.currentUser }"))
        assertTrue(repository.contains("val signedInUser: StateFlow<FirebaseUser?>"))
    }

    @Test
    fun `auth view model follows sign outs done outside the login screen`() {
        val viewModel = source("feature/auth/AuthViewModel.kt").substringAfter("init {").substringBefore("fun checkAuthState()")
        assertOrder(
            viewModel,
            listOf(
                "authRepository.signedInUser.collect { user ->",
                "shouldFollowAuthChange(_authState.value, keepWhileSigningIn = true)",
                "_authState.value = authStateFor(user)",
            ),
        )
    }

    @Test
    fun `settings first row becomes sign in when the session is lost and the profile sheet closes`() {
        val viewModel = source("feature/settings/SettingsViewModel.kt").substringAfter("init {").substringBefore("\n    }\n")
        assertTrue(viewModel.contains("authRepository.signedInUser.collect { user -> _authState.value = authStateFor(user) }"))

        val screen = source("feature/settings/SettingsScreen.kt")
        assertOrder(
            screen,
            listOf(
                "val authState by settingsViewModel.authState.collectAsStateWithLifecycle()",
                "val isLoggedIn = authState is AuthState.LoggedIn",
                "LaunchedEffect(isLoggedIn) {",
                "if (!isLoggedIn) showProfileSheet = false",
                "if (isLoggedIn) showProfileSheet = true else showLoginSheet = true",
            ),
        )
    }

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }

    private fun assertOrder(text: String, tokens: List<String>) {
        var previous = -1
        tokens.forEach { token ->
            val index = text.indexOf(token, previous + 1)
            assertTrue("$token should appear after index $previous", index > previous)
            previous = index
        }
    }
}
