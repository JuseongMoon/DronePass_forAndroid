package com.ScienceFiction.DronePassAndroid.feature.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 설정에서 연 날씨·KP 시트는 ViewModel 이 Activity 범위라, 닫아도 자동 갱신이 백그라운드에서 계속 돌았다
 * (날씨 3분·KP 5분마다 위치 읽기와 서버 호출). 시트가 컴포지션에서 빠질 때 자동 갱신을 멈춘다.
 */
class SettingsForecastSheetRefreshTest {

    @Test
    fun `settings forecast sheets stop auto refresh when they close`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val screen = File(root, "com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreen.kt").readText()
        val kp = screen.substringAfter("if (showKpForecastSheet) {").substringBefore("DronePassModalBottomSheet(")
        assertTrue(kp, kp.contains("onDispose { kpViewModel.stopAutoRefresh() }"))
        val weather = screen.substringAfter("if (showWeatherSheet) {").substringBefore("DronePassModalBottomSheet(")
        assertTrue(weather, weather.contains("onDispose { weatherViewModel.stopAutoRefresh() }"))
    }
}
