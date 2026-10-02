package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

internal object ProfilePreferenceKeys {
    /** 예전 클라우드 동기화 토글. 3.6.0 부터 토글이 없어 읽지 않고, 기기 계정 데이터를 지울 때 함께 지운다. */
    val CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloudBackupEnabled")
    val LAST_BACKUP_TIME = longPreferencesKey("lastBackupTime")

    val LEGACY_CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
    val LEGACY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
}

internal fun storedLastBackupTime(preferences: Preferences): Long? {
    return preferences[ProfilePreferenceKeys.LAST_BACKUP_TIME]
        ?: preferences[ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME]
}
