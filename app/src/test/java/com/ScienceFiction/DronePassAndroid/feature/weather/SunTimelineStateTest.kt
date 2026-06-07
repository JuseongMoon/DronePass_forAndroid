package com.ScienceFiction.DronePassAndroid.feature.weather

import com.ScienceFiction.DronePassAndroid.R
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SunTimelineStateTest {

    private val sunriseTimes = listOf(
        "2026-06-04T05:10",
        "2026-06-05T05:05",
    )
    private val sunsetTimes = listOf(
        "2026-06-04T19:40",
        "2026-06-05T19:41",
    )

    @Test
    fun `daytime timeline uses same day sunrise and sunset`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T12:00"),
        )!!

        assertTrue(state.isDaytime)
        assertEquals(LocalDateTime.parse("2026-06-04T05:10"), state.startDateTime)
        assertEquals(LocalDateTime.parse("2026-06-04T19:40"), state.endDateTime)
        assertTrue(state.nextEvent.isNextSunset)
    }

    @Test
    fun `after sunset timeline ends at tomorrow sunrise like iOS`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T20:30"),
        )!!

        assertFalse(state.isDaytime)
        assertEquals(LocalDateTime.parse("2026-06-04T19:40"), state.startDateTime)
        assertEquals(LocalDateTime.parse("2026-06-05T05:05"), state.endDateTime)
        assertFalse(state.nextEvent.isNextSunset)
        assertEquals("08:35", state.nextEvent.timeUntilFormatted)
    }

    @Test
    fun `before sunrise timeline approximates previous sunset like iOS`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T04:50"),
        )!!

        assertFalse(state.isDaytime)
        assertEquals(LocalDateTime.parse("2026-06-03T19:40"), state.startDateTime)
        assertEquals(LocalDateTime.parse("2026-06-04T05:10"), state.endDateTime)
        assertFalse(state.nextEvent.isNextSunset)
        assertEquals("00:20", state.nextEvent.timeUntilFormatted)
    }

    @Test
    fun `night marker is midnight within the active night interval`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T20:30"),
        )!!

        val marker = resolveSunTimelineMarker(state)!!

        assertEquals(R.string.weather_midnight, marker.labelRes)
        assertTrue(marker.progress in 0f..1f)
    }
}
