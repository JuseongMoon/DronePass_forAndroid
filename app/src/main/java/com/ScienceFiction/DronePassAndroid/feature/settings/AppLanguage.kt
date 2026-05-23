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
         * "ko-KR", "ko" → Korean / "en-US", "en" → English.
         * 매치되지 않으면 Korean (기본값).
         */
        fun fromTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank()) return Korean
            val primary = tag.substringBefore('-').lowercase()
            return entries.firstOrNull { it.tag == primary } ?: Korean
        }
    }
}
