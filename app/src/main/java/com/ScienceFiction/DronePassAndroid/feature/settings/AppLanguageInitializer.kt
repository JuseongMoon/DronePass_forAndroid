package com.ScienceFiction.DronePassAndroid.feature.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

private const val APP_LANGUAGE_PREFS = "dronepass_app_language"
private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"

/**
 * iOS `SettingManager.initializeAppLanguage` 정합.
 *
 * 저장된 앱 언어, Android per-app language, 런타임 리소스 locale 을 같은 값으로 맞춘다.
 *
 * 사용자가 앱 언어를 아직 고르지 않은 첫 실행에서는 시스템 언어가 한국어면 `ko`,
 * 그 외 언어면 `en`으로 고정한다. 이렇게 해야 `values` 기본 리소스가 한국어인
 * Android에서도 일본어 등 미지원 시스템 언어가 iOS처럼 영어 UI로 보인다.
 */
fun initializeAppLanguage(context: Context) {
    val language = resolvePersistedOrInitialAppLanguage(context)
    persistAppLanguage(context, language)
    applyAppLanguageToRuntime(context, language)
}

internal fun localizedAppLanguageContext(base: Context): Context {
    val language = resolvePersistedOrInitialAppLanguage(base)
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)

    val configuration = Configuration(base.resources.configuration)
    configuration.setLocale(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocales(LocaleList(locale))
    }
    return base.createConfigurationContext(configuration)
}

internal fun resolvePersistedOrInitialAppLanguage(context: Context): AppLanguage {
    return resolveAppLanguageForStoredOrSystemTag(
        storedLanguageTag = selectedRuntimeAppLanguageTag(context) ?: storedAppLanguageTag(context),
        systemLanguageTag = Locale.getDefault().language,
    )
}

internal fun resolveAppLanguageForStoredOrSystemTag(
    storedLanguageTag: String?,
    systemLanguageTag: String?,
): AppLanguage {
    return storedLanguageTag
        ?.takeIf { it.isNotBlank() }
        ?.let(AppLanguage::fromTag)
        ?: initialAppLanguageForSystemTag(systemLanguageTag)
}

internal fun storedAppLanguageTag(context: Context): String? {
    return context
        .getSharedPreferences(APP_LANGUAGE_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_APP_LANGUAGE_TAG, null)
}

private fun selectedRuntimeAppLanguageTag(context: Context): String? {
    val platformTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context
            .getSystemService(LocaleManager::class.java)
            ?.applicationLocales
            ?.takeIf { !it.isEmpty }
            ?.get(0)
            ?.toLanguageTag()
    } else {
        null
    }
    if (!platformTag.isNullOrBlank()) return platformTag

    return AppCompatDelegate.getApplicationLocales()
        .takeIf { !it.isEmpty }
        ?.get(0)
        ?.toLanguageTag()
}

internal fun persistAppLanguage(context: Context, language: AppLanguage) {
    context
        .getSharedPreferences(APP_LANGUAGE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_APP_LANGUAGE_TAG, language.tag)
        .apply()
}

internal fun applyAppLanguageToRuntime(context: Context, language: AppLanguage) {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context
            .getSystemService(LocaleManager::class.java)
            ?.applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(language.tag),
    )
}
