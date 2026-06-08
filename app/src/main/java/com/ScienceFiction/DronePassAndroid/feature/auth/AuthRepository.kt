package com.ScienceFiction.DronePassAndroid.feature.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val APPLE_USER_ID_FIELD = "appleUserID"
private const val GOOGLE_USER_ID_FIELD = "googleUserID"

internal enum class AuthAccountChangeAction {
    KEEP_LOCAL_DATA,
    RESET_LOCAL_DATA,
}

internal data class AuthSignInResult(
    val user: FirebaseUser,
    val accountChangeAction: AuthAccountChangeAction = AuthAccountChangeAction.KEEP_LOCAL_DATA,
    val provider: AuthLoginProvider,
    val providerUserId: String?,
)

internal enum class AuthLoginProvider {
    APPLE,
    GOOGLE,
}

internal enum class ProviderAccountResolution {
    KEEP_LOCAL_DATA,
    MIGRATE_ACCOUNT,
    SWITCH_ACCOUNT,
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
    email: String?,
    appleUserId: String?,
    googleUserId: String?,
    nowMillis: Long,
): Map<String, Any?> = buildMap {
    put("lastLogin", Timestamp(Date(nowMillis)))
    if (email != null) put("email", email)
    if (appleUserId != null) put(APPLE_USER_ID_FIELD, appleUserId)
    if (googleUserId != null) put(GOOGLE_USER_ID_FIELD, googleUserId)
}

internal fun shouldAttemptProviderAccountRecovery(
    savedFirebaseUid: String?,
    currentFirebaseUid: String,
): Boolean {
    return savedFirebaseUid != null && savedFirebaseUid != currentFirebaseUid
}

internal fun shouldAttemptAppleAccountRecovery(
    savedFirebaseUid: String?,
    currentFirebaseUid: String,
): Boolean {
    return shouldAttemptProviderAccountRecovery(savedFirebaseUid, currentFirebaseUid)
}

internal fun isSameRecoveredProviderAccount(
    oldProviderUserId: String?,
    currentProviderUserId: String,
): Boolean {
    return oldProviderUserId == currentProviderUserId
}

@Suppress("UNUSED_PARAMETER")
internal fun isSameRecoveredAppleAccount(
    oldAppleUserId: String?,
    savedAppleUserId: String?,
    currentAppleUserId: String,
): Boolean {
    return isSameRecoveredProviderAccount(
        oldProviderUserId = oldAppleUserId,
        currentProviderUserId = currentAppleUserId,
    )
}

internal fun resolveProviderAccountResolution(
    savedFirebaseUid: String?,
    currentFirebaseUid: String,
    oldAccountExists: Boolean,
    oldProviderUserId: String?,
    currentProviderUserId: String?,
): ProviderAccountResolution {
    if (!shouldAttemptProviderAccountRecovery(savedFirebaseUid, currentFirebaseUid)) {
        return ProviderAccountResolution.KEEP_LOCAL_DATA
    }
    if (!oldAccountExists) {
        return ProviderAccountResolution.SWITCH_ACCOUNT
    }
    if (currentProviderUserId == null || oldProviderUserId == null) {
        return ProviderAccountResolution.SWITCH_ACCOUNT
    }
    return if (isSameRecoveredProviderAccount(oldProviderUserId, currentProviderUserId)) {
        ProviderAccountResolution.MIGRATE_ACCOUNT
    } else {
        ProviderAccountResolution.SWITCH_ACCOUNT
    }
}

internal fun buildMigratedProviderUserDocumentData(
    oldUserData: Map<String, Any>,
    toUserId: String,
    fromUserId: String,
    providerUserFieldName: String,
    providerUserId: String,
    nowMillis: Long,
): Map<String, Any> = oldUserData.toMutableMap().apply {
    put("id", toUserId)
    put(providerUserFieldName, providerUserId)
    put("lastLogin", Timestamp(Date(nowMillis)))
    put("migratedFrom", fromUserId)
    put("migratedAt", Timestamp(Date(nowMillis)))
}

internal fun buildMigratedUserDocumentData(
    oldUserData: Map<String, Any>,
    toUserId: String,
    fromUserId: String,
    appleUserId: String,
    nowMillis: Long,
): Map<String, Any> = buildMigratedProviderUserDocumentData(
    oldUserData = oldUserData,
    toUserId = toUserId,
    fromUserId = fromUserId,
    providerUserFieldName = APPLE_USER_ID_FIELD,
    providerUserId = appleUserId,
    nowMillis = nowMillis,
)

internal fun buildMigratedOldUserPatch(
    toUserId: String,
    nowMillis: Long,
): Map<String, Any> = mapOf(
    "migrated" to true,
    "migratedTo" to toUserId,
    "migratedAt" to Timestamp(Date(nowMillis)),
)

internal fun isGoogleWebClientIdConfigured(webClientId: String): Boolean =
    webClientId.isNotBlank()

/**
 * Firebase Auth 래퍼 Repository.
 *
 * 지원 provider:
 *  - Google Sign-In: Credential Manager API + GoogleAuthProvider
 *  - Apple Sign-In: Firebase OAuthProvider("apple.com") (Chrome Custom Tabs 자동, 별도 SDK 불필요)
 *
 * iOS DronePass 사용자가 동일 provider 계정으로 로그인했는데 Firebase UID 가 달라지는
 * 복구 케이스는 iOS AuthManager처럼 이전 UID와 provider User ID를 비교해 Firestore
 * 데이터를 새 UID로 옮긴다.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val encryptedPrefsHelper: EncryptedPrefsHelper,
    private val firestore: FirebaseFirestore
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
        private const val FIRESTORE_BATCH_LIMIT = 450
        private val PROVIDER_RECOVERY_COLLECTIONS = listOf("shapes", "drones", "sketches", "metadata")
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
            val accountChangeAction = recoverProviderAccountIfNeeded(
                currentFirebaseUid = user.uid,
                currentProviderUserId = googleUserId,
                providerUserFieldName = GOOGLE_USER_ID_FIELD,
                providerDisplayName = "Google",
            )

            Result.success(
                AuthSignInResult(
                    user = user,
                    accountChangeAction = accountChangeAction,
                    provider = AuthLoginProvider.GOOGLE,
                    providerUserId = googleUserId,
                )
            )
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
            val accountChangeAction = recoverProviderAccountIfNeeded(
                currentFirebaseUid = user.uid,
                currentProviderUserId = appleUserId,
                providerUserFieldName = APPLE_USER_ID_FIELD,
                providerDisplayName = "Apple",
            )

            Result.success(
                AuthSignInResult(
                    user = user,
                    accountChangeAction = accountChangeAction,
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
     * 로그아웃
     * Firebase Auth에서 로그아웃한다. 로컬 복구 키는 iOS Keychain 동작처럼 유지한다.
     */
    fun signOut() {
        firebaseAuth.signOut()
        // iOS AuthManager.signout 과 동일하게 복구용 UID/Apple User ID 는 유지한다.
    }

    /**
     * 로그인 후처리 확정 단계.
     *
     * 계정 전환 시에는 UI 확인 전까지 복구용 UID/provider ID 를 덮어쓰면 안 된다.
     * iOS AuthManager 도 계정 전환 확인 이후 Keychain 을 갱신하므로 Android 도
     * ViewModel 이 확인 절차를 마친 뒤 이 메서드로 저장소와 사용자 문서를 갱신한다.
     */
    internal suspend fun finalizeSuccessfulSignIn(result: AuthSignInResult) {
        encryptedPrefsHelper.saveFirebaseUid(result.user.uid)
        when (result.provider) {
            AuthLoginProvider.APPLE -> {
                result.providerUserId?.let { encryptedPrefsHelper.saveAppleUserId(it) }
                ensureUserDocumentSafely(user = result.user, appleUserId = result.providerUserId)
            }
            AuthLoginProvider.GOOGLE -> {
                result.providerUserId?.let { encryptedPrefsHelper.saveGoogleUserId(it) }
                ensureUserDocumentSafely(user = result.user, googleUserId = result.providerUserId)
            }
        }
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
                ?: return Result.failure(Exception())

            user.delete().await()
            encryptedPrefsHelper.clearAll()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
                email = user.email,
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

    private suspend fun recoverProviderAccountIfNeeded(
        currentFirebaseUid: String,
        currentProviderUserId: String?,
        providerUserFieldName: String,
        providerDisplayName: String,
    ): AuthAccountChangeAction {
        val savedFirebaseUid = encryptedPrefsHelper.loadFirebaseUid()
        if (!shouldAttemptProviderAccountRecovery(savedFirebaseUid, currentFirebaseUid)) {
            return AuthAccountChangeAction.KEEP_LOCAL_DATA
        }

        val oldUserId = savedFirebaseUid ?: return AuthAccountChangeAction.KEEP_LOCAL_DATA
        val oldUserRef = firestore.collection(USERS_COLLECTION).document(oldUserId)
        val oldUserSnapshot = runCatching { oldUserRef.get().await() }
            .getOrElse { error ->
                Log.w(TAG, "이전 $providerDisplayName 계정 조회 실패: userId=$oldUserId", error)
                return AuthAccountChangeAction.RESET_LOCAL_DATA
            }

        val oldProviderUserId = oldUserSnapshot.getString(providerUserFieldName)
        val resolution = resolveProviderAccountResolution(
            savedFirebaseUid = savedFirebaseUid,
            currentFirebaseUid = currentFirebaseUid,
            oldAccountExists = oldUserSnapshot.exists(),
            oldProviderUserId = oldProviderUserId,
            currentProviderUserId = currentProviderUserId,
        )

        if (resolution == ProviderAccountResolution.SWITCH_ACCOUNT) {
            Log.w(TAG, "$providerDisplayName 계정 전환 감지: 이전 provider User ID 와 현재 로그인 정보가 다릅니다.")
            return AuthAccountChangeAction.RESET_LOCAL_DATA
        }
        if (resolution == ProviderAccountResolution.KEEP_LOCAL_DATA) {
            return AuthAccountChangeAction.KEEP_LOCAL_DATA
        }
        val providerUserId = currentProviderUserId ?: return AuthAccountChangeAction.RESET_LOCAL_DATA

        migrateProviderUserData(
            oldUserSnapshot = oldUserSnapshot,
            fromUserId = oldUserId,
            toUserId = currentFirebaseUid,
            providerUserFieldName = providerUserFieldName,
            providerUserId = providerUserId,
            providerDisplayName = providerDisplayName,
        )
        return AuthAccountChangeAction.KEEP_LOCAL_DATA
    }

    private suspend fun migrateProviderUserData(
        oldUserSnapshot: DocumentSnapshot,
        fromUserId: String,
        toUserId: String,
        providerUserFieldName: String,
        providerUserId: String,
        providerDisplayName: String,
    ) {
        val oldUserData = oldUserSnapshot.data ?: return
        val now = System.currentTimeMillis()
        val users = firestore.collection(USERS_COLLECTION)
        val newUserRef = users.document(toUserId)

        runCatching {
            newUserRef.set(
                buildMigratedProviderUserDocumentData(
                    oldUserData = oldUserData,
                    toUserId = toUserId,
                    fromUserId = fromUserId,
                    providerUserFieldName = providerUserFieldName,
                    providerUserId = providerUserId,
                    nowMillis = now,
                )
            ).await()

            PROVIDER_RECOVERY_COLLECTIONS.forEach { collectionName ->
                migrateUserSubcollection(
                    fromUserId = fromUserId,
                    toUserId = toUserId,
                    collectionName = collectionName,
                )
            }

            users.document(fromUserId)
                .set(buildMigratedOldUserPatch(toUserId = toUserId, nowMillis = now), SetOptions.merge())
                .await()
        }.onSuccess {
            Log.d(TAG, "$providerDisplayName 계정 데이터 복구 완료: $fromUserId -> $toUserId")
        }.onFailure { error ->
            Log.w(TAG, "$providerDisplayName 계정 데이터 복구 실패: $fromUserId -> $toUserId", error)
        }
    }

    private suspend fun migrateUserSubcollection(
        fromUserId: String,
        toUserId: String,
        collectionName: String,
    ) {
        val users = firestore.collection(USERS_COLLECTION)
        val snapshot = users.document(fromUserId)
            .collection(collectionName)
            .get()
            .await()
        if (snapshot.isEmpty) return

        val targetCollection = users.document(toUserId).collection(collectionName)
        snapshot.documents.chunked(FIRESTORE_BATCH_LIMIT).forEach { documents ->
            val batch = firestore.batch()
            documents.forEach { document ->
                batch.set(targetCollection.document(document.id), document.data.orEmpty())
            }
            batch.commit().await()
        }
    }
}
