package com.ScienceFiction.DronePassAndroid.core.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first

internal object CloudSyncPreferenceKeys {
    val CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloudBackupEnabled")
    val LEGACY_CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
}

internal fun storedCloudSyncEnabled(preferences: Preferences): Boolean {
    return preferences[CloudSyncPreferenceKeys.CLOUD_BACKUP_ENABLED]
        ?: preferences[CloudSyncPreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED]
        ?: false
}

internal fun shouldRunImmediateCloudSync(
    isLoggedIn: Boolean,
    cloudSyncEnabled: Boolean,
): Boolean = isLoggedIn && cloudSyncEnabled

internal suspend fun DataStore<Preferences>.isCloudSyncEnabled(): Boolean =
    storedCloudSyncEnabled(data.first())
