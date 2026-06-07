package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R

/**
 * 앱 언어 선택 enum — iOS `AppLanguage` 정합.
 *
 * AppCompatDelegate.setApplicationLocales 로 적용 (Per-app language).
 * iOS UserDefaults `AppleLanguages` 와 동등.
 */
enum class AppLanguage(val tag: String, @StringRes val displayNameRes: Int) {
    Korean("ko", R.string.settings_language_korean),
    English("en", R.string.settings_language_english);

    companion object {
        /**
         * IETF BCP 47 형식의 언어 태그를 [AppLanguage] 로 변환.
         * iOS `SettingManager.initializeAppLanguage` 와 동일하게
         * "ko-KR", "ko" → Korean / 그 외 시스템 언어 → English.
         */
        fun fromTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank()) return English
            val primary = tag.substringBefore('-').lowercase()
            return if (primary == Korean.tag) Korean else English
        }
    }
}
