package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

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
    fun `weather icon falls back to iOS cloud when current weather is unavailable`() {
        assertEquals(
            WeatherOverlayWeatherIcon.DefaultCloud,
            resolveWeatherOverlayWeatherIcon(weatherCode = null),
        )
    }

    @Test
    fun `weather icon uses weather code when current weather is available`() {
        assertEquals(
            WeatherOverlayWeatherIcon.WeatherCode,
            resolveWeatherOverlayWeatherIcon(weatherCode = 0),
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
        assertEquals(21.dp, WeatherOverlayCardIconSize)
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
