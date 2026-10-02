package com.ScienceFiction.DronePassAndroid.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.ScienceFiction.DronePassAndroid.core.analytics.UserActivityTracker
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val APPLE_USER_ID_FIELD = "appleUserID"
private const val GOOGLE_USER_ID_FIELD = "googleUserID"

internal data class AuthSignInResult(
    val user: FirebaseUser,
    val provider: AuthLoginProvider,
    val providerUserId: String?,
)

internal enum class AuthLoginProvider {
    APPLE,
    GOOGLE,
}

internal fun buildNewUserDocumentData(
    userId: String,
    email: String?,
    appleUserId: String?,
    googleUserId: String?,
    nowMillis: Long,
): Map<String, Any?> = buildMap {
    put("id", userId)
    put("email", email)
    put("createdAt", Timestamp(Date(nowMillis)))
    put("lastLogin", Timestamp(Date(nowMillis)))
    if (appleUserId != null) put(APPLE_USER_ID_FIELD, appleUserId)
    if (googleUserId != null) put(GOOGLE_USER_ID_FIELD, googleUserId)
}

internal fun buildExistingUserDocumentPatch(
    appleUserId: String?,
    googleUserId: String?,
    nowMillis: Long,
): Map<String, Any?> = buildMap {
    put("lastLogin", Timestamp(Date(nowMillis)))
    if (appleUserId != null) put(APPLE_USER_ID_FIELD, appleUserId)
    if (googleUserId != null) put(GOOGLE_USER_ID_FIELD, googleUserId)
}

internal fun isGoogleWebClientIdConfigured(webClientId: String): Boolean {
    val trimmed = webClientId.trim()
    return trimmed.isNotEmpty() &&
        trimmed != "YOUR_FIREBASE_WEB_CLIENT_ID" &&
        trimmed != "YOUR_WEB_CLIENT_ID" &&
        trimmed.endsWith(".apps.googleusercontent.com")
}

/**
 * Firebase Auth 래퍼 Repository.
 *
 * 지원 provider:
 *  - Google Sign-In: Credential Manager API + GoogleAuthProvider
 *  - Apple Sign-In: Firebase OAuthProvider("apple.com") (Chrome Custom Tabs 자동, 별도 SDK 불필요)
 *
 * 다른 uid 로의 데이터 복사(uid 마이그레이션)는 하지 않는다. 기기 데이터를 계정으로 올릴지는
 * AccountSessionFlows 가 로그인 뒤에 판단한다(기기 데이터 주인, 3.6.0).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val encryptedPrefsHelper: EncryptedPrefsHelper,
    private val firestore: FirebaseFirestore,
    private val userActivityTracker: UserActivityTracker,
) {
    companion object {
        private const val TAG = "AuthRepository"

        /**
         * Google OAuth 웹 클라이언트 ID
         * local.properties의 WEB_CLIENT_ID 값이 BuildConfig를 통해 주입됨
         */
        private val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID

        /** Firebase OAuthProvider 의 Apple 식별자 */
        private const val APPLE_PROVIDER_ID = "apple.com"
        private const val USERS_COLLECTION = "users"
    }

    /** 현재 로그인된 Firebase 사용자 */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /** 로그인 여부 확인 */
    val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    /**
     * Google Sign-In을 통한 Firebase 인증
     * Credential Manager API를 사용하여 Google 계정 선택 후 Firebase에 인증
     *
     * @param context Activity 또는 Fragment의 Context (Credential Manager에 필요)
     * @return 성공 시 FirebaseUser, 실패 시 에러 메시지를 포함한 Result
     */
    internal suspend fun signInWithGoogle(context: Context): Result<AuthSignInResult> {
        return try {
            if (!isGoogleWebClientIdConfigured(WEB_CLIENT_ID)) {
                return Result.failure(IllegalStateException(context.getString(R.string.login_google_config_missing)))
            }

            // Credential Manager 인스턴스 생성
            val credentialManager = CredentialManager.create(context)

            // Google ID 옵션 설정
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build()

            // 자격 증명 요청 빌드
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Google 계정 선택 다이얼로그 표시 및 결과 수신
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            // GoogleIdTokenCredential에서 ID 토큰 추출
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            // Firebase Auth에 Google 자격 증명으로 로그인
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user
                ?: return Result.failure(Exception())
            val googleUserId = user.googleProviderUserId()

            Result.success(
                AuthSignInResult(
                    user = user,
                    provider = AuthLoginProvider.GOOGLE,
                    providerUserId = googleUserId,
                )
            )
        } catch (e: NoCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Apple — Firebase OAuthProvider("apple.com") 사용.
     *
     * Firebase SDK 가 Chrome Custom Tabs 를 자동으로 띄워 Apple OAuth flow 를 처리한다.
     * 별도의 Apple SDK 추가 불필요. 결과로 받은 Firebase UID 는 **iOS native Apple Sign-In**
     * 으로 받은 UID 와 동일하므로 (같은 Apple ID 기준) Firestore `users/{uid}` 데이터가 자동
     * 호환된다.
     *
     * 호출 흐름:
     *  1. [firebaseAuth.pendingAuthResult] 가 있으면 (회전/백그라운드 후 복귀) 그것을 await
     *  2. 없으면 [startActivityForSignInWithProvider] 로 Custom Tabs 띄움
     *
     * 사전 조건 (Firebase Console):
     *  - Authentication > Sign-in method > Apple provider 활성화
     *  - Apple Developer Service ID + Team ID + Key ID + Private Key 등록
     *  - Authorized domains 에 `dronepass-91564.firebaseapp.com` 포함
     *
     * @param activity 현재 Activity (Custom Tabs intent launch 에 필요)
     * @return 성공 시 FirebaseUser, 실패 시 [Result.failure]
     */
    internal suspend fun signInWithApple(activity: Activity): Result<AuthSignInResult> {
        return try {
            // Apple OAuthProvider 빌더 — email/name scope 요청. Apple 은 첫 로그인 시에만 name 을 반환.
            val provider = OAuthProvider.newBuilder(APPLE_PROVIDER_ID).apply {
                setScopes(listOf("email", "name"))
                // locale 은 지정하지 않으면 디바이스 시스템 언어 사용 (ko, en 등 자동)
            }.build()

            // 회전/백그라운드 진입 후 복귀 시 pendingAuthResult 로 이어받기.
            // 그렇지 않으면 startActivityForSignInWithProvider 로 새 flow 시작.
            val pending = firebaseAuth.pendingAuthResult
            val authResult = if (pending != null) {
                pending.await()
            } else {
                firebaseAuth.startActivityForSignInWithProvider(activity, provider).await()
            }

            val user = authResult.user
                ?: return Result.failure(Exception())

            val appleUserId = user.appleProviderUserId()

            Result.success(
                AuthSignInResult(
                    user = user,
                    provider = AuthLoginProvider.APPLE,
                    providerUserId = appleUserId,
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Apple Sign-In 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 로그아웃. Firebase Auth 에서만 로그아웃한다.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * 로그인 후처리: 사용자 루트 문서와 활동 시각(계정 메타데이터, 동기화 관문 밖)을 기록한다.
     */
    internal suspend fun finalizeSuccessfulSignIn(result: AuthSignInResult) {
        when (result.provider) {
            AuthLoginProvider.APPLE ->
                ensureUserDocumentSafely(user = result.user, appleUserId = result.providerUserId)
            AuthLoginProvider.GOOGLE ->
                ensureUserDocumentSafely(user = result.user, googleUserId = result.providerUserId)
        }
        userActivityTracker.recordIfNeeded()
    }

    /**
     * 서버측 회원 탈퇴 완료 후 로컬 인증 상태만 정리한다.
     *
     * 사용자 Firestore 데이터와 Firebase Auth 계정 삭제는 Cloud Function이 원자적인
     * 순서로 처리한다. 로컬 도형/드론/스케치는 제품 정책에 따라 유지한다.
     */
    fun completeAccountDeletionLocally() {
        encryptedPrefsHelper.clearAll()
        firebaseAuth.signOut()
    }

    /**
     * iOS AuthManager.uploadUserData 정합.
     *
     * 로그인한 사용자의 `users/{uid}` 루트 문서를 보장한다. Shape/Drone/Sketch 하위 컬렉션만
     * 생성하면 프로필/분석/계정 삭제 흐름에서 iOS가 기대하는 사용자 문서가 비어 있을 수 있다.
     */
    private suspend fun ensureUserDocumentSafely(
        user: FirebaseUser,
        appleUserId: String? = null,
        googleUserId: String? = null,
    ) {
        runCatching {
            ensureUserDocument(
                user = user,
                appleUserId = appleUserId,
                googleUserId = googleUserId,
            )
        }
            .onFailure { error ->
                Log.w(TAG, "사용자 루트 문서 보장 실패: userId=${user.uid}", error)
            }
    }

    private suspend fun ensureUserDocument(
        user: FirebaseUser,
        appleUserId: String? = null,
        googleUserId: String? = null,
    ) {
        val userRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = userRef.get().await()
        val now = System.currentTimeMillis()

        if (snapshot.exists()) {
            val patch = buildExistingUserDocumentPatch(
                appleUserId = appleUserId,
                googleUserId = googleUserId,
                nowMillis = now,
            )
            userRef.set(patch, SetOptions.merge()).await()
        } else {
            val data = buildNewUserDocumentData(
                userId = user.uid,
                email = user.email,
                appleUserId = appleUserId,
                googleUserId = googleUserId,
                nowMillis = now,
            )
            userRef.set(data, SetOptions.merge()).await()
        }
    }

    private fun FirebaseUser.appleProviderUserId(): String? {
        return providerData.firstOrNull { info -> info.providerId == APPLE_PROVIDER_ID }?.uid
    }

    private fun FirebaseUser.googleProviderUserId(): String? {
        return providerData.firstOrNull { info -> info.providerId == GoogleAuthProvider.PROVIDER_ID }?.uid
    }
}
