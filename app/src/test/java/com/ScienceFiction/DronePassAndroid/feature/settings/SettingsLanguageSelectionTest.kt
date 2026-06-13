package com.ScienceFiction.DronePassAndroid.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLanguageSelectionTest {

    @Test
    fun `다른 언어를 선택하면 iOS처럼 즉시 적용하고 재시작 안내를 표시한다`() {
        val action = resolveLanguageSelectionAction(
            selectedLanguage = AppLanguage.English,
            currentLanguage = AppLanguage.Korean,
        )

        assertEquals(AppLanguage.English, action.languageToApply)
        assertTrue(action.showRestartAlert)
    }

    @Test
    fun `현재 언어를 다시 선택하면 아무 동작도 하지 않는다`() {
        val action = resolveLanguageSelectionAction(
            selectedLanguage = AppLanguage.Korean,
            currentLanguage = AppLanguage.Korean,
        )

        assertNull(action.languageToApply)
        assertFalse(action.showRestartAlert)
    }

    @Test
    fun `첫 실행 앱 언어는 iOS처럼 시스템 언어가 한국어면 한국어다`() {
        assertEquals(AppLanguage.Korean, initialAppLanguageForSystemTag("ko"))
        assertEquals(AppLanguage.Korean, initialAppLanguageForSystemTag("ko-KR"))
    }

    @Test
    fun `첫 실행 앱 언어는 iOS처럼 한국어가 아닌 시스템 언어면 영어다`() {
        assertEquals(AppLanguage.English, initialAppLanguageForSystemTag("en"))
        assertEquals(AppLanguage.English, initialAppLanguageForSystemTag("ja-JP"))
        assertEquals(AppLanguage.English, initialAppLanguageForSystemTag(null))
    }

    @Test
    fun `저장된 앱 언어가 있으면 시스템 언어보다 우선한다`() {
        assertEquals(
            AppLanguage.English,
            resolveAppLanguageForStoredOrSystemTag(
                storedLanguageTag = "en",
                systemLanguageTag = "ko-KR",
            ),
        )
        assertEquals(
            AppLanguage.Korean,
            resolveAppLanguageForStoredOrSystemTag(
                storedLanguageTag = "ko",
                systemLanguageTag = "en-US",
            ),
        )
    }

    @Test
    fun `저장된 앱 언어가 없으면 첫 실행 시스템 언어 정책을 사용한다`() {
        assertEquals(
            AppLanguage.Korean,
            resolveAppLanguageForStoredOrSystemTag(
                storedLanguageTag = null,
                systemLanguageTag = "ko-KR",
            ),
        )
        assertEquals(
            AppLanguage.English,
            resolveAppLanguageForStoredOrSystemTag(
                storedLanguageTag = "",
                systemLanguageTag = "ja-JP",
            ),
        )
    }
}
