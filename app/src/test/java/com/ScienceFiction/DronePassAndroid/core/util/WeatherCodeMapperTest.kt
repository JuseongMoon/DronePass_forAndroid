package com.ScienceFiction.DronePassAndroid.core.util

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
}
