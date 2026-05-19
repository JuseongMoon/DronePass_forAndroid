package com.ScienceFiction.DronePassAndroid.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Auth 래퍼 Repository.
 *
 * 지원 provider:
 *  - Google Sign-In: Credential Manager API + GoogleAuthProvider
 *  - Apple Sign-In: Firebase OAuthProvider("apple.com") (Chrome Custom Tabs 자동, 별도 SDK 불필요)
 *
 * iOS DronePass 의 Apple Sign-In 사용자는 동일 Apple ID 로 Android Apple Sign-In 시 동일
 * Firebase UID 가 발급되어 `users/{uid}` Firestore 데이터가 자동으로 호환된다 (Account Linking 불필요).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val encryptedPrefsHelper: EncryptedPrefsHelper
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
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
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
                ?: return Result.failure(Exception("Firebase 인증에 성공했지만 사용자 정보를 가져올 수 없습니다."))

            // UID를 암호화된 저장소에 캐싱
            encryptedPrefsHelper.saveFirebaseUid(user.uid)

            Result.success(user)
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
    suspend fun signInWithApple(activity: Activity): Result<FirebaseUser> {
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
                ?: return Result.failure(Exception("Apple 인증에 성공했지만 사용자 정보를 가져올 수 없습니다."))

            // UID 캐시 (Google 흐름과 동일)
            encryptedPrefsHelper.saveFirebaseUid(user.uid)

            Result.success(user)
        } catch (e: Exception) {
            Log.w(TAG, "Apple Sign-In 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 로그아웃
     * Firebase Auth에서 로그아웃하고 로컬 캐시된 UID 삭제
     */
    fun signOut() {
        firebaseAuth.signOut()
        encryptedPrefsHelper.clearAll()
    }

    /**
     * 계정 삭제
     * Firebase Auth에서 현재 사용자 계정을 완전히 삭제
     *
     * @return 성공 시 Unit, 실패 시 에러 메시지를 포함한 Result
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            user.delete().await()
            encryptedPrefsHelper.clearAll()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
