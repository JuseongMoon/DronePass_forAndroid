package com.ScienceFiction.DronePassAndroid.feature.auth

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LoginScreenContractTest {

    @Test
    fun `로그인 provider 버튼 순서는 iOS LoginView처럼 Apple 다음 Google 이다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/LoginScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/LoginScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "stringResource(R.string.login_apple)",
                "stringResource(R.string.login_google)",
                "LoginTermsNotice(",
            ),
        )
    }

    @Test
    fun `로그인 provider 액션은 iOS처럼 Apple과 Google 모두 실제 로그인 경로에 연결된다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/LoginScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/LoginScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "viewModel.signInWithApple(activity)",
                "viewModel.signInWithGoogle(context)",
            ),
        )
    }

    @Test
    fun `로그인 약관 문구 순서는 iOS LoginView 와 동일하다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/LoginScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/LoginScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.login_terms_intro",
                "R.string.login_terms_service",
                "R.string.login_terms_privacy",
                "R.string.login_terms_middle",
                "R.string.login_terms_agree",
            ),
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in LoginScreen.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
