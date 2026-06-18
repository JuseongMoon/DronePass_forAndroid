package com.ScienceFiction.DronePassAndroid.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import com.ScienceFiction.DronePassAndroid.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeMapperTest {

    @Test
    fun `weather code descriptions use localized string resources instead of fixed Korean text`() {
        assertEquals(R.string.weather_condition_clear, WeatherCodeMapper.weatherCodeToDescriptionRes(0))
        assertEquals(R.string.weather_condition_partly_cloudy, WeatherCodeMapper.weatherCodeToDescriptionRes(2))
        assertEquals(R.string.weather_condition_fog, WeatherCodeMapper.weatherCodeToDescriptionRes(45))
        assertEquals(R.string.weather_condition_rain, WeatherCodeMapper.weatherCodeToDescriptionRes(61))
        assertEquals(R.string.weather_condition_snow, WeatherCodeMapper.weatherCodeToDescriptionRes(71))
        assertEquals(R.string.weather_condition_snow_showers, WeatherCodeMapper.weatherCodeToDescriptionRes(86))
        assertEquals(R.string.weather_condition_thunderstorm_hail, WeatherCodeMapper.weatherCodeToDescriptionRes(99))
        assertEquals(R.string.weather_unknown, WeatherCodeMapper.weatherCodeToDescriptionRes(Int.MIN_VALUE))
    }

    @Test
    fun `precipitation icon matches iOS by requiring actual precipitation for rain icon`() {
        assertEquals(
            Icons.Default.Cloud.name,
            WeatherCodeMapper.weatherCodeToIosPrecipitationIcon(code = 61, precipitation = 0.0).name,
        )
        assertEquals(
            Icons.Default.Umbrella.name,
            WeatherCodeMapper.weatherCodeToIosPrecipitationIcon(code = 61, precipitation = 0.1).name,
        )
    }

    @Test
    fun `precipitation icon keeps iOS clear and storm no precipitation fallbacks`() {
        assertEquals(
            Icons.Default.WbSunny.name,
            WeatherCodeMapper.weatherCodeToIosPrecipitationIcon(code = 0, precipitation = 0.0).name,
        )
        assertEquals(
            Icons.Default.FlashOn.name,
            WeatherCodeMapper.weatherCodeToIosPrecipitationIcon(code = 95, precipitation = 0.0).name,
        )
        assertEquals(
            Icons.Default.Cloud.name,
            WeatherCodeMapper.weatherCodeToIosPrecipitationIcon(code = null, precipitation = null).name,
        )
    }
}
