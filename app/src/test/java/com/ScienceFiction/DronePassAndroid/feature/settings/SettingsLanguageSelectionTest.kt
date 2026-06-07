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
}
