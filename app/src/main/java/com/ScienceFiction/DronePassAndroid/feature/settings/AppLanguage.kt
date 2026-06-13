package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R
import java.util.Locale

/**
 * 앱 언어 선택 enum — iOS `AppLanguage` 정합.
 *
 * 앱 자체 저장소와 Android runtime locale 에 함께 적용한다.
 * iOS UserDefaults `AppleLanguages` 와 동등한 사용자 선택값이다.
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
            val primary = tag.substringBefore('-').lowercase(Locale.ROOT)
            return if (primary == Korean.tag) Korean else English
        }
    }
}

internal fun initialAppLanguageForSystemTag(systemLanguageTag: String?): AppLanguage {
    return AppLanguage.fromTag(systemLanguageTag)
}
