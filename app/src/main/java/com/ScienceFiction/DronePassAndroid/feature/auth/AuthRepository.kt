package com.ScienceFiction.DronePassAndroid.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Auth 래퍼 Repository
 * Google Sign-In(Credential Manager API)과 Firebase 인증을 통합 관리
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val encryptedPrefsHelper: EncryptedPrefsHelper
) {
    companion object {
        /**
         * Google OAuth 웹 클라이언트 ID
         * local.properties의 WEB_CLIENT_ID 값이 BuildConfig를 통해 주입됨
         */
        private val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID
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
