package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

internal object ProfilePreferenceKeys {
    val CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloudBackupEnabled")
    val LAST_BACKUP_TIME = longPreferencesKey("lastBackupTime")

    val LEGACY_CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
    val LEGACY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
}

internal fun storedCloudBackupEnabled(preferences: Preferences): Boolean {
    return preferences[ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED]
        ?: preferences[ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED]
        ?: false
}

internal fun storedLastBackupTime(preferences: Preferences): Long? {
    return preferences[ProfilePreferenceKeys.LAST_BACKUP_TIME]
        ?: preferences[ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME]
}
