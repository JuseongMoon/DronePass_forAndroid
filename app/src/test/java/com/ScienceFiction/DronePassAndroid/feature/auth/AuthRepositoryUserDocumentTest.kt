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
    fun `provider 계정 복구는 도형 드론 스케치와 동기화 메타데이터를 함께 이전한다`() {
        assertEquals(
            listOf("shapes", "drones", "sketches", "metadata"),
            AUTH_PROVIDER_RECOVERY_COLLECTIONS,
        )
    }

    @Test
    fun `provider 계정 복구는 기기별 FCM devices 컬렉션을 이전하지 않는다`() {
        assertFalse(AUTH_PROVIDER_RECOVERY_COLLECTIONS.contains("devices"))
    }

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
    fun `Apple 계정 복구는 저장된 UID 와 현재 UID 가 다를 때만 시도한다`() {
        assertFalse(
            shouldAttemptAppleAccountRecovery(
                savedFirebaseUid = null,
                currentFirebaseUid = "current",
            )
        )
        assertFalse(
            shouldAttemptAppleAccountRecovery(
                savedFirebaseUid = "current",
                currentFirebaseUid = "current",
            )
        )
        assertTrue(
            shouldAttemptAppleAccountRecovery(
                savedFirebaseUid = "old",
                currentFirebaseUid = "current",
            )
        )
    }

    @Test
    fun `provider 계정 복구도 저장된 UID 와 현재 UID 가 다를 때만 시도한다`() {
        assertFalse(
            shouldAttemptProviderAccountRecovery(
                savedFirebaseUid = null,
                currentFirebaseUid = "current",
            )
        )
        assertFalse(
            shouldAttemptProviderAccountRecovery(
                savedFirebaseUid = "current",
                currentFirebaseUid = "current",
            )
        )
        assertTrue(
            shouldAttemptProviderAccountRecovery(
                savedFirebaseUid = "old",
                currentFirebaseUid = "current",
            )
        )
    }

    @Test
    fun `Apple 계정 복구는 현재 로그인한 Apple User ID 가 Firestore 값과 직접 같아야 한다`() {
        assertTrue(
            isSameRecoveredAppleAccount(
                oldAppleUserId = "apple-1",
                savedAppleUserId = null,
                currentAppleUserId = "apple-1",
            )
        )
        assertFalse(
            isSameRecoveredAppleAccount(
                oldAppleUserId = "apple-1",
                savedAppleUserId = "apple-1",
                currentAppleUserId = "apple-2",
            )
        )
        assertFalse(
            isSameRecoveredAppleAccount(
                oldAppleUserId = "apple-1",
                savedAppleUserId = "apple-2",
                currentAppleUserId = "apple-2",
            )
        )
    }

    @Test
    fun `Apple 계정 복구는 iOS처럼 Firestore provider User ID 만 신뢰한다`() {
        assertFalse(
            isSameRecoveredAppleAccount(
                oldAppleUserId = null,
                savedAppleUserId = "apple-1",
                currentAppleUserId = "apple-2",
            )
        )
        assertFalse(
            isSameRecoveredAppleAccount(
                oldAppleUserId = null,
                savedAppleUserId = null,
                currentAppleUserId = "apple-1",
            )
        )
    }

    @Test
    fun `provider 계정 복구는 iOS처럼 Firestore provider User ID 만 신뢰한다`() {
        assertTrue(
            isSameRecoveredProviderAccount(
                oldProviderUserId = "google-1",
                currentProviderUserId = "google-1",
            )
        )
        assertFalse(
            isSameRecoveredProviderAccount(
                oldProviderUserId = "google-1",
                currentProviderUserId = "google-2",
            )
        )
        assertFalse(
            isSameRecoveredProviderAccount(
                oldProviderUserId = null,
                currentProviderUserId = "google-1",
            )
        )
    }

    @Test
    fun `provider 계정 판별은 UID가 같거나 처음 로그인인 경우 로컬 데이터를 유지한다`() {
        assertEquals(
            ProviderAccountResolution.KEEP_LOCAL_DATA,
            resolveProviderAccountResolution(
                savedFirebaseUid = null,
                currentFirebaseUid = "current",
                oldAccountExists = false,
                oldProviderUserId = null,
                currentProviderUserId = "google-1",
            ),
        )
        assertEquals(
            ProviderAccountResolution.KEEP_LOCAL_DATA,
            resolveProviderAccountResolution(
                savedFirebaseUid = "current",
                currentFirebaseUid = "current",
                oldAccountExists = true,
                oldProviderUserId = "google-1",
                currentProviderUserId = "google-1",
            ),
        )
    }

    @Test
    fun `provider 계정 판별은 저장 UID 계정의 Firestore 식별자가 같을 때만 마이그레이션한다`() {
        assertEquals(
            ProviderAccountResolution.MIGRATE_ACCOUNT,
            resolveProviderAccountResolution(
                savedFirebaseUid = "old",
                currentFirebaseUid = "current",
                oldAccountExists = true,
                oldProviderUserId = "google-1",
                currentProviderUserId = "google-1",
            ),
        )
    }

    @Test
    fun `provider 계정 판별은 이전 계정이 없거나 식별자가 다르면 계정 전환으로 처리한다`() {
        assertEquals(
            ProviderAccountResolution.SWITCH_ACCOUNT,
            resolveProviderAccountResolution(
                savedFirebaseUid = "old",
                currentFirebaseUid = "current",
                oldAccountExists = false,
                oldProviderUserId = null,
                currentProviderUserId = "google-1",
            ),
        )
        assertEquals(
            ProviderAccountResolution.SWITCH_ACCOUNT,
            resolveProviderAccountResolution(
                savedFirebaseUid = "old",
                currentFirebaseUid = "current",
                oldAccountExists = true,
                oldProviderUserId = "google-1",
                currentProviderUserId = "google-2",
            ),
        )
        assertEquals(
            ProviderAccountResolution.SWITCH_ACCOUNT,
            resolveProviderAccountResolution(
                savedFirebaseUid = "old",
                currentFirebaseUid = "current",
                oldAccountExists = true,
                oldProviderUserId = null,
                currentProviderUserId = "google-1",
            ),
        )
    }

    @Test
    fun `마이그레이션된 사용자 문서는 iOS처럼 최신 UID 와 이전 UID 메타데이터를 남긴다`() {
        val data = buildMigratedUserDocumentData(
            oldUserData = mapOf(
                "id" to "old-uid",
                "email" to "old@example.com",
                "createdAt" to Timestamp(Date(1_600_000_000_000L)),
            ),
            toUserId = "new-uid",
            fromUserId = "old-uid",
            appleUserId = "apple-1",
            nowMillis = 1_700_000_000_000L,
        )

        assertEquals("new-uid", data["id"])
        assertEquals("old@example.com", data["email"])
        assertEquals("apple-1", data["appleUserID"])
        assertEquals("old-uid", data["migratedFrom"])
        assertEquals(1_700_000_000_000L, (data["lastLogin"] as Timestamp).toDate().time)
        assertEquals(1_700_000_000_000L, (data["migratedAt"] as Timestamp).toDate().time)
    }

    @Test
    fun `마이그레이션된 Google 사용자 문서는 googleUserID 와 이전 UID 메타데이터를 남긴다`() {
        val data = buildMigratedProviderUserDocumentData(
            oldUserData = mapOf(
                "id" to "old-uid",
                "email" to "old@example.com",
                "createdAt" to Timestamp(Date(1_600_000_000_000L)),
            ),
            toUserId = "new-uid",
            fromUserId = "old-uid",
            providerUserFieldName = "googleUserID",
            providerUserId = "google-1",
            nowMillis = 1_700_000_000_000L,
        )

        assertEquals("new-uid", data["id"])
        assertEquals("old@example.com", data["email"])
        assertEquals("google-1", data["googleUserID"])
        assertFalse(data.containsKey("appleUserID"))
        assertEquals("old-uid", data["migratedFrom"])
        assertEquals(1_700_000_000_000L, (data["lastLogin"] as Timestamp).toDate().time)
        assertEquals(1_700_000_000_000L, (data["migratedAt"] as Timestamp).toDate().time)
    }

    @Test
    fun `이전 사용자 문서는 iOS처럼 마이그레이션 완료 플래그를 남긴다`() {
        val patch = buildMigratedOldUserPatch(
            toUserId = "new-uid",
            nowMillis = 1_700_000_000_000L,
        )

        assertEquals(true, patch["migrated"])
        assertEquals("new-uid", patch["migratedTo"])
        assertEquals(1_700_000_000_000L, (patch["migratedAt"] as Timestamp).toDate().time)
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
                "providerUserFieldName = GOOGLE_USER_ID_FIELD",
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
                "providerUserFieldName = APPLE_USER_ID_FIELD",
                "provider = AuthLoginProvider.APPLE",
                "providerUserId = appleUserId",
            ),
        )
    }

    @Test
    fun `로그인 성공 확정은 iOS AuthManager처럼 provider 별 복구 키와 사용자 문서를 갱신한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthRepository.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "internal suspend fun finalizeSuccessfulSignIn",
                "encryptedPrefsHelper.saveFirebaseUid(result.user.uid)",
                "AuthLoginProvider.APPLE ->",
                "encryptedPrefsHelper.saveAppleUserId(it)",
                "ensureUserDocumentSafely(user = result.user, appleUserId = result.providerUserId)",
                "AuthLoginProvider.GOOGLE ->",
                "encryptedPrefsHelper.saveGoogleUserId(it)",
                "ensureUserDocumentSafely(user = result.user, googleUserId = result.providerUserId)",
            ),
        )
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
