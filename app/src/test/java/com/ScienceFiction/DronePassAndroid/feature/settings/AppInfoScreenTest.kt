package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppInfoScreenTest {

    @Test
    fun `앱 버전 행은 iOS처럼 버전명과 빌드 번호를 함께 표시한다`() {
        val value = appInfoVersionValue("1.2.3", 45)

        assertEquals("1.2.3 (45)", value)
    }

    @Test
    fun `빌드 번호 행은 iOS처럼 빌드 번호만 표시한다`() {
        assertEquals("45", appInfoBuildNumberValue(45))
    }

    @Test
    fun `앱 정보 feature row 치수는 iOS AppInfoView 를 따른다`() {
        assertEquals(60.dp, AppInfoIntroIconSize)
        assertEquals(36.dp, AppInfoIntroSymbolSize)
        assertEquals(12.dp, AppInfoIntroSpacing)
        assertEquals(32.dp, AppInfoFeatureIconSize)
        assertEquals(12.dp, AppInfoFeatureHorizontalSpacing)
        assertEquals(4.dp, AppInfoFeatureTitleDescriptionSpacing)
        assertEquals(4.dp, AppInfoFeatureVerticalPadding)
        assertEquals(16.dp, AppInfoFeatureHorizontalPadding)
    }

    @Test
    fun `KP 지수 기능 아이콘은 iOS antenna radiowaves 의미와 맞는 안테나 아이콘을 사용한다`() {
        assertEquals("Filled.SettingsInputAntenna", AppInfoKpIndexIcon.name)
    }

    @Test
    fun `일출일몰 기능 아이콘은 iOS sunrise fill 의미와 맞는 수평선 해 아이콘을 사용한다`() {
        assertEquals("Filled.WbTwilight", AppInfoSunriseSunsetIcon.name)
    }

    @Test
    fun `다중 드론 기능 아이콘은 iOS paperplane circle fill 의미와 맞는 종이비행기 아이콘을 사용한다`() {
        assertEquals("AutoMirrored.Filled.Send", AppInfoMultiDroneIcon.name)
    }

    @Test
    fun `실시간 날씨 기능 아이콘은 iOS cloud sun fill 의미와 맞는 구름 아이콘을 사용한다`() {
        assertEquals("Filled.WbCloudy", AppInfoWeatherIcon.name)
    }

    @Test
    fun `도형 관리 기능 아이콘은 iOS circle circle fill 의미와 맞는 원형 아이콘을 사용한다`() {
        assertEquals("Filled.RadioButtonChecked", AppInfoShapeManagementIcon.name)
    }
}
