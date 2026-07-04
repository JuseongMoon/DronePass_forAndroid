package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsScreenContractTest {

    @Test
    fun `설정 로그인 시트는 iOS SettingView처럼 로그인 없이 시작 버튼을 숨긴다`() {
        assertFalse(SettingsLoginSheetShowSkipLogin)
    }

    @Test
    fun `설정 목록 순서는 iOS SettingView 섹션 순서를 유지한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.settings_section_my_info",
                "R.string.settings_profile_my",
                "R.string.settings_profile_login",
                "R.string.settings_drone_manage",
                "R.string.settings_section_flight_environment",
                "R.string.settings_kp_index_current",
                "R.string.settings_weather_current",
                "R.string.settings_section_notifications",
                "R.string.settings_end_date_alarm",
                "R.string.settings_sunrise_alarm",
                "R.string.settings_sunset_alarm",
                "R.string.settings_section_map_display",
                "R.string.settings_keep_screen_awake",
                "R.string.settings_hide_not_started",
                "R.string.settings_hide_expired",
                "R.string.settings_delete_expired_shapes",
                "R.string.settings_section_app_info",
                "R.string.settings_language",
                "R.string.settings_korea_features",
                "R.string.settings_app_intro",
                "R.string.settings_patch_notes",
            ),
        )
    }

    @Test
    fun `설정 행 chevron 은 iOS caption 크기를 따른다`() {
        assertEquals(12.dp, SettingsItemChevronSize)
    }

    @Test
    fun `비행 환경 행은 iOS처럼 KP 날씨 모두 chevron 버튼으로 연다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.settings_section_flight_environment",
                "R.string.settings_kp_index_current",
                "onClick = { showKpForecastSheet = true }",
                "showArrow = true",
                "R.string.settings_weather_current",
                "onClick = { showWeatherSheet = true }",
                "showArrow = true",
            ),
        )
    }

    @Test
    fun `설정 진입 KP 요약은 iOS처럼 강제 갱신한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsViewModel.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsViewModel.kt",
        ).readText()

        assertTrue(source.contains("kpIndexRepository.getCurrentKp(forceRefresh = true)"))
    }

    @Test
    fun `설정 전역 색상 선택은 iOS 현재 SettingView처럼 노출하지 않는다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
        ).readText()

        assertFalse(source.contains("ColorPicker"))
        assertFalse(source.contains("showColorPicker"))
        assertFalse(source.contains("palette_color"))
    }

    @Test
    fun `설정 날짜 시간 선택 시트는 iOS 현재 SettingView처럼 노출하지 않는다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
        ).readText()

        assertFalse(source.contains("DateTimeSelectionView"))
        assertFalse(source.contains("DatePicker"))
        assertFalse(source.contains("TimePicker"))
    }

    @Test
    fun `Android 알림 권한 카드는 iOS 알림 섹션 순서를 깨지 않는다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.settings_section_notifications",
                "NotificationPermissionRequest()",
                "R.string.settings_end_date_alarm",
                "R.string.settings_sunrise_alarm",
                "R.string.settings_sunset_alarm",
                "R.string.settings_section_map_display",
            ),
        )
    }

    @Test
    fun `한국 특화 기능 토글은 iOS처럼 상태 변경 후 ON OFF 안내를 띄운다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.settings_korea_features",
                "settingsViewModel.toggleKoreaFeatures(newValue)",
                "koreaFeaturesAlertOn = newValue",
                "if (isOn) R.string.settings_korea_features_on_title",
                "else R.string.settings_korea_features_off_title",
                "if (isOn) R.string.settings_korea_features_on_message",
                "else R.string.settings_korea_features_off_message",
            ),
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in SettingsScreen.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
