package com.ScienceFiction.DronePassAndroid.feature.profile

import com.ScienceFiction.DronePassAndroid.core.di.FIREBASE_FUNCTIONS_REGION
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionServiceTest {

    @Test
    fun `회원 탈퇴 callable 계약은 UID 없는 빈 요청과 deleted 응답을 사용한다`() {
        assertEquals("asia-northeast3", FIREBASE_FUNCTIONS_REGION)
        assertEquals("deleteDronePassAccount", ACCOUNT_DELETION_FUNCTION_NAME)
        assertTrue(accountDeletionCallablePayload().isEmpty())
        assertTrue(isAccountDeletionSuccessResponse(mapOf("status" to "deleted")))
        assertFalse(isAccountDeletionSuccessResponse(mapOf("status" to "pending")))
        assertFalse(isAccountDeletionSuccessResponse(emptyMap<String, Any>()))
        assertFalse(isAccountDeletionSuccessResponse("deleted"))
        assertFalse(isAccountDeletionSuccessResponse(null))
    }

    @Test
    fun `Apple provider 계정만 탈퇴 전에 토큰 철회 대상이다`() {
        assertTrue(hasAppleLoginProvider(listOf("firebase", "apple.com")))
        assertFalse(hasAppleLoginProvider(listOf("firebase", "google.com")))
        assertFalse(hasAppleLoginProvider(emptyList()))
    }

    @Test
    fun `회원 탈퇴 서비스는 Apple 재인증과 토큰 철회 후 서버 함수를 호출한다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/AccountDeletionService.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/AccountDeletionService.kt",
        ).readText()

        val requestSource = source.substring(
            source.indexOf("override suspend fun deleteCurrentAccount"),
            source.indexOf("private suspend fun revokeAppleAccessToken"),
        )
        assertSourceOrder(
            requestSource,
            listOf(
                "revokeAppleAccessToken(activity)",
                "getHttpsCallable(ACCOUNT_DELETION_FUNCTION_NAME)",
                ".call(accountDeletionCallablePayload())",
            ),
        )
        val revocationSource = source.substring(source.indexOf("private suspend fun revokeAppleAccessToken"))
        assertSourceOrder(
            revocationSource,
            listOf(
                "startActivityForReauthenticateWithProvider",
                "revokeAccessToken",
            ),
        )
        assertFalse(source.contains("collection(\"users\")"))
        assertFalse(source.contains("analytics/deleted_users"))
        assertFalse(source.contains("currentUser?.delete"))
    }

    @Test
    fun `프로필 탈퇴 흐름은 서버 성공 후에만 로컬 인증 상태를 정리하고 fallback 삭제를 하지 않는다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileViewModel.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileViewModel.kt",
        ).readText()
        val deletionSource = source.substring(source.indexOf("fun deleteAccount(activity: Activity"))

        assertSourceOrder(
            deletionSource,
            listOf(
                "accountDeletionService.deleteCurrentAccount(activity).fold(",
                "onSuccess = {",
                "authRepository.completeAccountDeletionLocally()",
                "onResult(true",
                "onFailure = { exception ->",
                "onResult(",
                "false,",
            ),
        )
        assertFalse(deletionSource.contains("deleteFirestoreUserData"))
        assertFalse(deletionSource.contains("saveAnonymizedStats"))
        assertFalse(deletionSource.contains("authRepository.deleteAccount"))
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Project file not found. Tried: ${candidates.joinToString()}")
    }

    private fun assertSourceOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        tokens.forEach { token ->
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue("Missing token after $previousIndex: $token", index >= 0)
            previousIndex = index
        }
    }
}
