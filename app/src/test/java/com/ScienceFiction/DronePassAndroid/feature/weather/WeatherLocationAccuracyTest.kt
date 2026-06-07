package com.ScienceFiction.DronePassAndroid.feature.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherLocationAccuracyTest {

    @Test
    fun `accuracy at or below 50 meters is treated as GPS like iOS`() {
        assertTrue(resolveWeatherLocationAccuracy(50f).isUsingGps)
        assertTrue(resolveWeatherLocationAccuracy(12.5f).isUsingGps)
    }

    @Test
    fun `accuracy above 50 meters shows non GPS location notice`() {
        val state = resolveWeatherLocationAccuracy(51f)

        assertEquals(51.0, state.accuracyMeters!!, 0.0)
        assertFalse(state.isUsingGps)
    }

    @Test
    fun `missing or invalid accuracy does not show location notice`() {
        assertNull(resolveWeatherLocationAccuracy(null).accuracyMeters)
        assertFalse(resolveWeatherLocationAccuracy(null).isUsingGps)

        assertNull(resolveWeatherLocationAccuracy(Float.NaN).accuracyMeters)
        assertFalse(resolveWeatherLocationAccuracy(Float.NaN).isUsingGps)

        assertNull(resolveWeatherLocationAccuracy(-1f).accuracyMeters)
        assertFalse(resolveWeatherLocationAccuracy(-1f).isUsingGps)
    }
}
