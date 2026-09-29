package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.core.ui.IosWeatherSymbols
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCodeMapperTest {

    @Test
    fun `WeatherKit condition codes parse case-insensitively and unknown codes become null`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromAppleCode("Clear"))
        assertEquals(WeatherCondition.HEAVY_RAIN, WeatherCondition.fromAppleCode("heavyRain"))
        assertEquals(WeatherCondition.SCATTERED_THUNDERSTORMS, WeatherCondition.fromAppleCode("ScatteredThunderstorms"))
        assertNull(WeatherCondition.fromAppleCode("SomethingNew"))
        assertNull(WeatherCondition.fromAppleCode(null))
    }

    @Test
    fun `every WeatherKit condition has its own localized description`() {
        val descriptions = WeatherCondition.entries.map(WeatherCodeMapper::conditionDescriptionRes)
        assertEquals(WeatherCondition.entries.size, descriptions.toSet().size)
        assertEquals(R.string.weather_condition_clear, WeatherCodeMapper.conditionDescriptionRes(WeatherCondition.CLEAR))
        assertEquals(R.string.weather_unknown, WeatherCodeMapper.conditionDescriptionRes(null))
    }

    @Test
    fun `snowfall label follows iOS isSnowing conditions`() {
        assertTrue(WeatherCodeMapper.isSnowing(WeatherCondition.SNOW))
        assertTrue(WeatherCodeMapper.isSnowing(WeatherCondition.HEAVY_SNOW))
        assertTrue(WeatherCodeMapper.isSnowing(WeatherCondition.BLOWING_SNOW))
        assertTrue(WeatherCodeMapper.isSnowing(WeatherCondition.FLURRIES))
        assertFalse(WeatherCodeMapper.isSnowing(WeatherCondition.SLEET))
        assertFalse(WeatherCodeMapper.isSnowing(null))
    }

    @Test
    fun `precipitation icon matches iOS by requiring actual precipitation for rain icon`() {
        assertEquals(Icons.Default.Cloud.name, WeatherCodeMapper.conditionIcon(WeatherCondition.RAIN, 0.0).name)
        assertEquals(IosWeatherSymbols.CloudRain.name, WeatherCodeMapper.conditionIcon(WeatherCondition.RAIN, 0.1).name)
        assertEquals(IosWeatherSymbols.CloudSnow.name, WeatherCodeMapper.conditionIcon(WeatherCondition.SNOW, 0.1).name)
    }

    @Test
    fun `precipitation icon keeps iOS clear and storm no precipitation fallbacks`() {
        assertEquals(Icons.Default.WbSunny.name, WeatherCodeMapper.conditionIcon(WeatherCondition.CLEAR, 0.0).name)
        assertEquals(Icons.Default.FlashOn.name, WeatherCodeMapper.conditionIcon(WeatherCondition.THUNDERSTORMS, 0.0).name)
        assertEquals(Icons.Default.Cloud.name, WeatherCodeMapper.conditionIcon(null, null).name)
    }
}
