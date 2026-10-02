package com.ScienceFiction.DronePassAndroid.feature.auth

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Date

class AuthRepositoryUserDocumentTest {

    @Test
    fun `새 사용자 문서는 iOS AuthManager 와 같은 루트 필드를 만든다`() {
        val data = buildNewUserDocumentData(
            userId = "uid-1",
            email = "user@example.com",
            appleUserId = "apple-user-1",
            googleUserId = null,
            nowMillis = 1_700_000_000_000L,
        )

        assertEquals("uid-1", data["id"])
        assertEquals("user@example.com", data["email"])
        assertEquals("apple-user-1", data["appleUserID"])
        assertEquals(1_700_000_000_000L, (data["createdAt"] as Timestamp).toDate().time)
        assertEquals(1_700_000_000_000L, (data["lastLogin"] as Timestamp).toDate().time)
    }

    @Test
    fun `Google 로그인 사용자 문서에는 Apple User ID 를 넣지 않는다`() {
        val data = buildNewUserDocumentData(
            userId = "uid-1",
            email = "user@example.com",
            appleUserId = null,
            googleUserId = "google-user-1",
            nowMillis = 1_700_000_000_000L,
        )

        assertFalse(data.containsKey("appleUserID"))
        assertEquals("google-user-1", data["googleUserID"])
    }

    @Test
    fun `새 Google 사용자 문서는 iOS AuthManager처럼 googleUserID 를 저장한다`() {
        val data = buildNewUserDocumentData(
            userId = "uid-1",
            email = "user@example.com",
            appleUserId = null,
            googleUserId = "google-user-1",
            nowMillis = 1_700_000_000_000L,
        )

        assertEquals("google-user-1", data["googleUserID"])
    }

    @Test
    fun `기존 사용자 patch 는 createdAt 을 덮어쓰지 않고 현재 Apple User ID 를 반영한다`() {
        val patch = buildExistingUserDocumentPatch(
            appleUserId = "current-apple-user",
            googleUserId = null,
            nowMillis = 1_700_000_000_000L,
        )

        assertFalse(patch.containsKey("createdAt"))
        assertFalse(patch.containsKey("email"))
        assertEquals("current-apple-user", patch["appleUserID"])
        assertTrue(patch["lastLogin"] is Timestamp)
    }

    @Test
    fun `기존 Google 사용자 patch 는 iOS처럼 email 을 덮지 않고 현재 googleUserID 만 반영한다`() {
        val patch = buildExistingUserDocumentPatch(
            appleUserId = null,
            googleUserId = "current-google-user",
            nowMillis = 1_700_000_000_000L,
        )

        assertFalse(patch.containsKey("createdAt"))
        assertFalse(patch.containsKey("email"))
        assertEquals("current-google-user", patch["googleUserID"])
        assertFalse(patch.containsKey("appleUserID"))
        assertTrue(patch["lastLogin"] is Timestamp)
    }

    @Test
    fun `Google 로그인은 WEB_CLIENT_ID 가 설정된 경우에만 시작한다`() {
        assertFalse(isGoogleWebClientIdConfigured(""))
        assertFalse(isGoogleWebClientIdConfigured("   "))
        assertFalse(isGoogleWebClientIdConfigured("YOUR_FIREBASE_WEB_CLIENT_ID"))
        assertFalse(isGoogleWebClientIdConfigured("YOUR_WEB_CLIENT_ID"))
        assertFalse(isGoogleWebClientIdConfigured("web-client-id"))
        assertTrue(isGoogleWebClientIdConfigured("web-client-id.apps.googleusercontent.com"))
        assertTrue(isGoogleWebClientIdConfigured("  web-client-id.apps.googleusercontent.com  "))
    }

    @Test
    fun `Google 로그인 저장소 흐름은 iOS GoogleLoginManager 후처리와 같은 provider 문서 필드를 사용한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "internal suspend fun signInWithGoogle",
                "isGoogleWebClientIdConfigured(WEB_CLIENT_ID)",
                "GetGoogleIdOption.Builder()",
                ".setServerClientId(WEB_CLIENT_ID)",
                "GoogleIdTokenCredential.createFrom(credential.data)",
                "GoogleAuthProvider.getCredential(idToken, null)",
                "firebaseAuth.signInWithCredential(firebaseCredential).await()",
                "val googleUserId = user.googleProviderUserId()",
                "provider = AuthLoginProvider.GOOGLE",
                "providerUserId = googleUserId",
            ),
        )
    }

    @Test
    fun `Apple 로그인 저장소 흐름은 iOS AppleLoginManager 와 같은 Firebase apple provider 를 사용한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "internal suspend fun signInWithApple",
                "OAuthProvider.newBuilder(APPLE_PROVIDER_ID)",
                "setScopes(listOf(\"email\", \"name\"))",
                "val pending = firebaseAuth.pendingAuthResult",
                "firebaseAuth.startActivityForSignInWithProvider(activity, provider).await()",
                "val appleUserId = user.appleProviderUserId()",
                "provider = AuthLoginProvider.APPLE",
                "providerUserId = appleUserId",
            ),
        )
    }

    @Test
    fun `로그인 성공 확정은 provider 별 사용자 문서만 갱신하고 마지막 로그인 uid 를 저장하지 않는다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "internal suspend fun finalizeSuccessfulSignIn",
                "AuthLoginProvider.APPLE ->",
                "ensureUserDocumentSafely(user = result.user, appleUserId = result.providerUserId)",
                "AuthLoginProvider.GOOGLE ->",
                "ensureUserDocumentSafely(user = result.user, googleUserId = result.providerUserId)",
            ),
        )
        // 기기 데이터 주인(3.6.0): 계정 판단에 "마지막 로그인 uid"를 쓰지 않고, 다른 uid 로 데이터를 복사하지 않는다.
        listOf("saveFirebaseUid", "saveAppleUserId", "saveGoogleUserId", "loadFirebaseUid", "migratedFrom", ".batch()")
            .forEach { token -> assertFalse(token, source.contains(token)) }
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex",
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
