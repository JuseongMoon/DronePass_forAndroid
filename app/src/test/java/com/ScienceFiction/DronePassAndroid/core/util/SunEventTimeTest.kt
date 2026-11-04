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
    fun afterSunsetWithoutTomorrowSunriseShowsPlaceholderAtNightLikeIos() {
        val event = nextSunEvent(
            sunriseIsoList = listOf("2026-06-04T05:10"),
            sunsetIsoList = listOf("2026-06-04T19:40"),
            now = LocalDateTime.parse("2026-06-04T20:30"),
        )

        // D-54: 밤인데 내일 일출을 모르면 밤(일출 아이콘)으로 두고 시간은 보여 주지 않는다.
        assertFalse(event.isNextSunset)
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

    // 11/4 결정(iOS 와 같은 규칙): 초 단위로 비교하고, 일몰과 같은 시각이면 밤, 표시는 분 내림.
    private val sunrises = listOf("2026-11-04T07:03:10", "2026-11-05T07:04:12")
    private val sunsets = listOf("2026-11-04T17:32:30", "2026-11-05T17:31:33")

    @Test
    fun remainingTimeIsComparedInSecondsAndFlooredToMinutes() {
        val event = nextSunEvent(sunrises, sunsets, LocalDateTime.parse("2026-11-04T17:28:00"))

        assertTrue(event.isNextSunset)
        assertEquals("00:04", event.timeUntilFormatted) // 4분 30초 → 4분
    }

    @Test
    fun oneSecondBeforeSunsetIsStillDay() {
        val event = nextSunEvent(sunrises, sunsets, LocalDateTime.parse("2026-11-04T17:32:29"))

        assertTrue(event.isNextSunset)
        assertEquals("00:00", event.timeUntilFormatted)
    }

    @Test
    fun theSunsetSecondItselfIsNight() {
        val event = nextSunEvent(sunrises, sunsets, LocalDateTime.parse("2026-11-04T17:32:30"))

        assertFalse(event.isNextSunset)
        assertEquals("13:31", event.timeUntilFormatted) // 내일 07:04:12 까지 13시간 31분 42초
    }

    @Test
    fun theSunriseSecondItselfIsDay() {
        val event = nextSunEvent(sunrises, sunsets, LocalDateTime.parse("2026-11-04T07:03:10"))

        assertTrue(event.isNextSunset)
        assertEquals("10:29", event.timeUntilFormatted)
    }
}
