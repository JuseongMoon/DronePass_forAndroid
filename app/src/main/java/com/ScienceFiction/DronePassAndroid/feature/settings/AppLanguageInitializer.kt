package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * iOS `SettingManager.initializeAppLanguage` 정합.
 *
 * 사용자가 앱 언어를 아직 고르지 않은 첫 실행에서는 시스템 언어가 한국어면 `ko`,
 * 그 외 언어면 `en`으로 고정한다. 이렇게 해야 `values` 기본 리소스가 한국어인
 * Android에서도 일본어 등 미지원 시스템 언어가 iOS처럼 영어 UI로 보인다.
 */
fun initializeAppLanguageIfUnset() {
    if (!AppCompatDelegate.getApplicationLocales().isEmpty) return

    val initialLanguage = initialAppLanguageForSystemTag(Locale.getDefault().language)
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(initialLanguage.tag),
    )
}
