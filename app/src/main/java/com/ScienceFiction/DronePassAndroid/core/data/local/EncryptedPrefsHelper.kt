package com.ScienceFiction.DronePassAndroid.core.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptedSharedPreferences를 사용한 보안 저장소
 * Firebase UID 등 민감한 정보를 암호화하여 저장한다.
 *
 * 손상 시 폴백 정책:
 * - KeyStore 손상(앱 재설치/클라우드 복원/일부 OEM 단말)으로 인한
 *   [GeneralSecurityException] 또는 [IOException] 발생 시 prefs 파일을 삭제하고
 *   재생성을 시도한다. 두 번째 시도도 실패하면 평문 SharedPreferences로 폴백하여
 *   앱이 크래시 없이 동작하도록 한다.
 *
 * [createEncryptedPrefs] 정적 헬퍼는 [com.ScienceFiction.DronePassAndroid.service.FcmService]
 * 등 Hilt 주입이 어려운 컴포넌트에서도 사용 가능하다.
 */
@Singleton
class EncryptedPrefsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EncryptedPrefsHelper"

        const val PREFS_FILE_NAME = "dronepass_encrypted_prefs"

        /** KeyStore 손상으로 EncryptedPrefs 사용 불가 시 사용할 평문 폴백 파일명 */
        const val FALLBACK_PREFS_FILE_NAME = "dronepass_prefs_fallback"

        private const val KEY_FIREBASE_UID = "firebase_uid"
        private const val KEY_APPLE_USER_ID = "apple_user_id"
        private const val KEY_GOOGLE_USER_ID = "google_user_id"

        /**
         * 암호화된 SharedPreferences를 생성한다. KeyStore 손상 시 1회 재생성 시도 후
         * 그래도 실패하면 평문 SharedPreferences로 폴백한다.
         *
         * 호출자는 반환된 SharedPreferences에 비밀번호 등 최고 등급 비밀은 저장하지
         * 말아야 한다(폴백 시 평문 저장 가능성). UID/디바이스ID 같은 식별자는 OK.
         */
        fun createEncryptedPrefs(context: Context): SharedPreferences {
            return try {
                buildEncryptedPrefs(context)
            } catch (e: GeneralSecurityException) {
                Log.w(TAG, "EncryptedPrefs KeyStore 손상 감지, 재생성 시도", e)
                recoverOrFallback(context)
            } catch (e: IOException) {
                Log.w(TAG, "EncryptedPrefs IO 오류, 재생성 시도", e)
                recoverOrFallback(context)
            }
        }

        private fun buildEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        private fun recoverOrFallback(context: Context): SharedPreferences {
            // prefs 파일 삭제 후 재시도
            runCatching { context.deleteSharedPreferences(PREFS_FILE_NAME) }
                .onFailure { Log.e(TAG, "Prefs 파일 삭제 실패", it) }
            return runCatching { buildEncryptedPrefs(context) }
                .getOrElse { e ->
                    Log.e(TAG, "EncryptedPrefs 재생성도 실패, 평문 SharedPreferences로 폴백", e)
                    context.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
                }
        }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedPrefs(context)
    }

    /**
     * Firebase UID를 암호화하여 저장
     */
    fun saveFirebaseUid(uid: String) {
        sharedPreferences.edit {
            putString(KEY_FIREBASE_UID, uid)
        }
    }

    fun saveAppleUserId(appleUserId: String) {
        sharedPreferences.edit {
            putString(KEY_APPLE_USER_ID, appleUserId)
        }
    }

    fun saveGoogleUserId(googleUserId: String) {
        sharedPreferences.edit {
            putString(KEY_GOOGLE_USER_ID, googleUserId)
        }
    }

    /**
     * 저장된 Firebase UID를 불러오기
     * @return 저장된 UID 또는 null
     */
    fun loadFirebaseUid(): String? {
        return sharedPreferences.getString(KEY_FIREBASE_UID, null)
    }

    fun loadAppleUserId(): String? {
        return sharedPreferences.getString(KEY_APPLE_USER_ID, null)
    }

    fun loadGoogleUserId(): String? {
        return sharedPreferences.getString(KEY_GOOGLE_USER_ID, null)
    }

    /**
     * 모든 암호화된 데이터 삭제
     * 계정 삭제 시 호출. 로그아웃 시에는 계정 복구용 UID/provider User ID를 유지한다.
     */
    fun clearAll() {
        sharedPreferences.edit {
            clear()
        }
    }
}
