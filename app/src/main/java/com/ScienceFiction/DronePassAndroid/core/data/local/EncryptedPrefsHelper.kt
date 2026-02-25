package com.ScienceFiction.DronePassAndroid.core.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptedSharedPreferences를 사용한 보안 저장소
 * Firebase UID 등 민감한 정보를 암호화하여 저장
 */
@Singleton
class EncryptedPrefsHelper @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_FILE_NAME = "dronepass_encrypted_prefs"
        private const val KEY_FIREBASE_UID = "firebase_uid"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Firebase UID를 암호화하여 저장
     */
    fun saveFirebaseUid(uid: String) {
        sharedPreferences.edit().putString(KEY_FIREBASE_UID, uid).apply()
    }

    /**
     * 저장된 Firebase UID를 불러오기
     * @return 저장된 UID 또는 null
     */
    fun loadFirebaseUid(): String? {
        return sharedPreferences.getString(KEY_FIREBASE_UID, null)
    }

    /**
     * 모든 암호화된 데이터 삭제
     * 로그아웃 또는 계정 삭제 시 호출
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
