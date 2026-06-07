package com.ScienceFiction.DronePassAndroid.core.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

internal object NotificationPreferenceKeys {
    val SUNRISE_ALARM_ENABLED = booleanPreferencesKey("sunriseAlarmEnabled")
    val SUNSET_ALARM_ENABLED = booleanPreferencesKey("sunsetAlarmEnabled")
    val END_DATE_ALARM_ENABLED = booleanPreferencesKey("endDateAlarmEnabled")
    val LAUNCH_NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("launchNotificationPermissionRequested")

    val LEGACY_SUNRISE_ALARM_ENABLED = booleanPreferencesKey("sunrise_alarm_enabled")
    val LEGACY_SUNSET_ALARM_ENABLED = booleanPreferencesKey("sunset_alarm_enabled")
    val LEGACY_END_DATE_ALARM_ENABLED = booleanPreferencesKey("end_date_alarm_enabled")
}

internal fun storedSunriseAlarmEnabled(preferences: Preferences): Boolean {
    return preferences[NotificationPreferenceKeys.SUNRISE_ALARM_ENABLED]
        ?: preferences[NotificationPreferenceKeys.LEGACY_SUNRISE_ALARM_ENABLED]
        ?: false
}

internal fun storedSunsetAlarmEnabled(preferences: Preferences): Boolean {
    return preferences[NotificationPreferenceKeys.SUNSET_ALARM_ENABLED]
        ?: preferences[NotificationPreferenceKeys.LEGACY_SUNSET_ALARM_ENABLED]
        ?: false
}

internal fun storedEndDateAlarmEnabled(preferences: Preferences): Boolean {
    return preferences[NotificationPreferenceKeys.END_DATE_ALARM_ENABLED]
        ?: preferences[NotificationPreferenceKeys.LEGACY_END_DATE_ALARM_ENABLED]
        ?: false
}

internal fun storedLaunchNotificationPermissionRequested(preferences: Preferences): Boolean {
    return preferences[NotificationPreferenceKeys.LAUNCH_NOTIFICATION_PERMISSION_REQUESTED] ?: false
}
