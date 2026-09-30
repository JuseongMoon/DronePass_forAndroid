package com.ScienceFiction.DronePassAndroid.feature.weather

import com.ScienceFiction.DronePassAndroid.core.ui.IosWeatherSymbols
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import org.junit.Assert.assertEquals
import org.junit.Test
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition

class WeatherOverlayCardTest {

    @Test
    fun `next sunset uses iOS sunset icon role`() {
        assertEquals(
            SunEventOverlayIcon.Sunset,
            resolveSunEventOverlayIcon(isNextSunset = true),
        )
    }

    @Test
    fun `next sunrise uses iOS sunrise icon role`() {
        assertEquals(
            SunEventOverlayIcon.Sunrise,
            resolveSunEventOverlayIcon(isNextSunset = false),
        )
    }

    @Test
    fun `sun event overlay icons use iOS sunrise and sunset symbol resources`() {
        assertEquals(IosSunriseSymbolName, iosSunEventSymbolName(isSunrise = true))
        assertEquals(IosSunsetSymbolName, iosSunEventSymbolName(isSunrise = false))
        assertEquals(R.drawable.ic_sunrise_fill_ios_like, iosSunEventDrawableRes(isSunrise = true))
        assertEquals(R.drawable.ic_sunset_fill_ios_like, iosSunEventDrawableRes(isSunrise = false))
    }

    @Test
    fun `weather icon falls back to iOS cloud when current weather is unavailable`() {
        assertEquals(
            IosWeatherSymbols.Cloud.name,
            resolveWeatherOverlayWeatherIcon(condition = null, precipitation = null).name,
        )
    }

    @Test
    fun `weather icon uses iOS precipitation aware weather code when current weather is available`() {
        assertEquals(
            IosWeatherSymbols.CloudRain.name,
            resolveWeatherOverlayWeatherIcon(condition = WeatherCondition.RAIN, precipitation = 0.1).name,
        )
    }

    @Test
    fun `weather overlay visual tokens match iOS weatherWindIconButton`() {
        assertEquals(12.dp, WeatherOverlayCardHorizontalPadding)
        assertEquals(8.dp, WeatherOverlayCardVerticalPadding)
        assertEquals(12.dp, WeatherOverlayCardCornerRadius)
        assertEquals(4.dp, WeatherOverlayCardShadowElevation)
        assertEquals(8.dp, WeatherOverlayCardGroupSpacing)
        assertEquals(8.dp, WeatherOverlayCardRowSpacing)
        assertEquals(24.dp, WeatherOverlayCardIconSize) // SF 21pt semibold 글리프 크기
        assertEquals(16.sp, WeatherOverlayCardTextSize)
    }

    @Test
    fun `wind arrow rotation uses iOS wind direction plus 180 degrees`() {
        assertEquals(225f, weatherOverlayWindRotationDegrees(windDirection = 45.0), 0f)
        assertEquals(180f, weatherOverlayWindRotationDegrees(windDirection = null), 0f)
    }

    @Test
    fun `temperature text matches iOS rounded degree display and nil fallback`() {
        assertEquals("23°", weatherOverlayTemperatureText(temperature = 22.6))
        assertEquals("-", weatherOverlayTemperatureText(temperature = null))
    }
}
