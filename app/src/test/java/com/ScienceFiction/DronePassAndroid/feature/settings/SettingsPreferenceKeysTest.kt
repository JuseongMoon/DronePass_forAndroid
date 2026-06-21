package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.datastore.preferences.core.preferencesOf
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferenceKeysTest {

    @Test
    fun `설정 키 이름은 iOS UserDefaults 이름과 동일하게 유지한다`() {
        assertEquals("KoreaFeaturesEnabled", SettingsPreferenceKeys.KOREA_FEATURES_ENABLED.name)
        assertEquals("hideExpiredShapesEnabled", SettingsPreferenceKeys.HIDE_EXPIRED_SHAPES.name)
        assertEquals("hideNotStartedShapesEnabled", SettingsPreferenceKeys.HIDE_NOT_STARTED_SHAPES.name)
        assertEquals("keepScreenAwakeEnabled", SettingsPreferenceKeys.KEEP_SCREEN_AWAKE.name)
    }

    @Test
    fun `설정 키는 기존 Android snake case 값을 fallback 으로 읽는다`() {
        val preferences = preferencesOf(
            SettingsPreferenceKeys.LEGACY_KOREA_FEATURES_ENABLED to false,
            SettingsPreferenceKeys.LEGACY_HIDE_EXPIRED_SHAPES to true,
            SettingsPreferenceKeys.LEGACY_HIDE_NOT_STARTED_SHAPES to true,
            SettingsPreferenceKeys.LEGACY_KEEP_SCREEN_AWAKE to true,
        )

        assertFalse(storedKoreaFeaturesEnabled(preferences)!!)
        assertTrue(storedHideExpiredShapes(preferences))
        assertTrue(storedHideNotStartedShapes(preferences))
        assertTrue(storedKeepScreenAwake(preferences))
    }

    @Test
    fun `설정 키는 iOS primary 값이 있으면 legacy 값보다 우선한다`() {
        val preferences = preferencesOf(
            SettingsPreferenceKeys.KOREA_FEATURES_ENABLED to true,
            SettingsPreferenceKeys.LEGACY_KOREA_FEATURES_ENABLED to false,
            SettingsPreferenceKeys.HIDE_EXPIRED_SHAPES to false,
            SettingsPreferenceKeys.LEGACY_HIDE_EXPIRED_SHAPES to true,
            SettingsPreferenceKeys.HIDE_NOT_STARTED_SHAPES to false,
            SettingsPreferenceKeys.LEGACY_HIDE_NOT_STARTED_SHAPES to true,
            SettingsPreferenceKeys.KEEP_SCREEN_AWAKE to false,
            SettingsPreferenceKeys.LEGACY_KEEP_SCREEN_AWAKE to true,
        )

        assertTrue(storedKoreaFeaturesEnabled(preferences)!!)
        assertFalse(storedHideExpiredShapes(preferences))
        assertFalse(storedHideNotStartedShapes(preferences))
        assertFalse(storedKeepScreenAwake(preferences))
    }

    @Test
    fun `한국 특화 기능 기본값은 한국어면 켜진다`() {
        assertTrue(defaultKoreaFeaturesEnabled(AppLanguage.Korean))
    }

    @Test
    fun `한국 특화 기능 기본값은 영어면 꺼진다`() {
        assertFalse(defaultKoreaFeaturesEnabled(AppLanguage.English))
    }

    @Test
    fun `앱 언어 기본값은 iOS처럼 한국어가 아닌 시스템 언어면 영어다`() {
        assertEquals(AppLanguage.Korean, AppLanguage.fromTag("ko-KR"))
        assertEquals(AppLanguage.English, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.English, AppLanguage.fromTag("ja-JP"))
        assertEquals(AppLanguage.English, AppLanguage.fromTag(null))
    }

    @Test
    fun `한국 특화 기능은 저장값이 있으면 언어 기본값보다 저장값을 우선한다`() {
        assertFalse(
            resolveKoreaFeaturesEnabled(
                storedValue = false,
                language = AppLanguage.Korean,
            )
        )
        assertTrue(
            resolveKoreaFeaturesEnabled(
                storedValue = true,
                language = AppLanguage.English,
            )
        )
    }

    @Test
    fun `한국 특화 기능은 저장값이 없을 때만 언어 기본값을 사용한다`() {
        assertTrue(
            resolveKoreaFeaturesEnabled(
                storedValue = null,
                language = AppLanguage.Korean,
            )
        )
        assertFalse(
            resolveKoreaFeaturesEnabled(
                storedValue = null,
                language = AppLanguage.English,
            )
        )
    }

    @Test
    fun `한국 특화 기능 초기 저장값은 런타임 Locale 이 아닌 앱 언어 저장값을 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsViewModel.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsViewModel.kt",
        ).readText()

        assertTrue(source.contains("legacyValue ?: defaultKoreaFeaturesEnabled(appContext)"))
        assertFalse(source.contains("legacyValue ?: defaultKoreaFeaturesEnabled()"))
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
