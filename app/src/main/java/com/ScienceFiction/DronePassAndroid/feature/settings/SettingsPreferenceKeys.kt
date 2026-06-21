package com.ScienceFiction.DronePassAndroid.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

internal object SettingsPreferenceKeys {
    val KOREA_FEATURES_ENABLED = booleanPreferencesKey("KoreaFeaturesEnabled")
    val HIDE_EXPIRED_SHAPES = booleanPreferencesKey("hideExpiredShapesEnabled")
    val HIDE_NOT_STARTED_SHAPES = booleanPreferencesKey("hideNotStartedShapesEnabled")
    val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keepScreenAwakeEnabled")

    val LEGACY_KOREA_FEATURES_ENABLED = booleanPreferencesKey("korea_features_enabled")
    val LEGACY_HIDE_EXPIRED_SHAPES = booleanPreferencesKey("hide_expired_shapes")
    val LEGACY_HIDE_NOT_STARTED_SHAPES = booleanPreferencesKey("hide_not_started_shapes")
    val LEGACY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
}

internal fun storedKoreaFeaturesEnabled(preferences: Preferences): Boolean? {
    return preferences[SettingsPreferenceKeys.KOREA_FEATURES_ENABLED]
        ?: preferences[SettingsPreferenceKeys.LEGACY_KOREA_FEATURES_ENABLED]
}

internal fun storedHideExpiredShapes(preferences: Preferences): Boolean {
    return preferences[SettingsPreferenceKeys.HIDE_EXPIRED_SHAPES]
        ?: preferences[SettingsPreferenceKeys.LEGACY_HIDE_EXPIRED_SHAPES]
        ?: false
}

internal fun storedHideNotStartedShapes(preferences: Preferences): Boolean {
    return preferences[SettingsPreferenceKeys.HIDE_NOT_STARTED_SHAPES]
        ?: preferences[SettingsPreferenceKeys.LEGACY_HIDE_NOT_STARTED_SHAPES]
        ?: false
}

internal fun storedKeepScreenAwake(preferences: Preferences): Boolean {
    return preferences[SettingsPreferenceKeys.KEEP_SCREEN_AWAKE]
        ?: preferences[SettingsPreferenceKeys.LEGACY_KEEP_SCREEN_AWAKE]
        ?: false
}

internal fun resolveCurrentAppLanguage(context: Context): AppLanguage {
    return resolvePersistedOrInitialAppLanguage(context)
}

internal fun defaultKoreaFeaturesEnabled(language: AppLanguage): Boolean {
    return language == AppLanguage.Korean
}

internal fun defaultKoreaFeaturesEnabled(context: Context): Boolean {
    return defaultKoreaFeaturesEnabled(resolveCurrentAppLanguage(context))
}

internal fun resolveKoreaFeaturesEnabled(
    storedValue: Boolean?,
    language: AppLanguage,
): Boolean {
    return storedValue ?: defaultKoreaFeaturesEnabled(language)
}
