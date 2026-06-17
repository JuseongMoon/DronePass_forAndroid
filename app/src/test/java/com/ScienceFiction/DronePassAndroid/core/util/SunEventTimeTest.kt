package com.ScienceFiction.DronePassAndroid.core.util

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SunEventTimeTest {

    @Test
    fun afterSunsetUsesTomorrowSunriseFromDailyValues() {
        val event = nextSunEvent(
            sunriseIsoList = listOf("2026-06-04T05:10", "2026-06-05T05:05"),
            sunsetIsoList = listOf("2026-06-04T19:40", "2026-06-05T19:41"),
            now = LocalDateTime.parse("2026-06-04T20:30"),
        )

        assertFalse(event.isNextSunset)
        assertEquals("08:35", event.timeUntilFormatted)
    }

    @Test
    fun afterSunsetWithoutTomorrowSunriseFallsBackToPlaceholderLikeIos() {
        val event = nextSunEvent(
            sunriseIsoList = listOf("2026-06-04T05:10"),
            sunsetIsoList = listOf("2026-06-04T19:40"),
            now = LocalDateTime.parse("2026-06-04T20:30"),
        )

        assertTrue(event.isNextSunset)
        assertEquals("--:--", event.timeUntilFormatted)
    }

    @Test
    fun daytimeUsesSameDaySunset() {
        val event = nextSunEvent(
            sunriseIsoList = listOf("2026-06-04T05:10", "2026-06-05T05:05"),
            sunsetIsoList = listOf("2026-06-04T19:40", "2026-06-05T19:41"),
            now = LocalDateTime.parse("2026-06-04T12:00"),
        )

        assertTrue(event.isNextSunset)
        assertEquals("07:40", event.timeUntilFormatted)
    }

    @Test
    fun beforeSunriseUsesSameDaySunrise() {
        val event = nextSunEvent(
            sunriseIsoList = listOf("2026-06-04T05:10", "2026-06-05T05:05"),
            sunsetIsoList = listOf("2026-06-04T19:40", "2026-06-05T19:41"),
            now = LocalDateTime.parse("2026-06-04T04:50"),
        )

        assertFalse(event.isNextSunset)
        assertEquals("00:20", event.timeUntilFormatted)
    }

    @Test
    fun invalidDataFallsBackToPlaceholder() {
        val event = nextSunEvent(
            sunriseIsoList = listOf("not-a-time"),
            sunsetIsoList = emptyList(),
            now = LocalDateTime.parse("2026-06-04T12:00"),
        )

        assertTrue(event.isNextSunset)
        assertEquals("--:--", event.timeUntilFormatted)
    }
}
