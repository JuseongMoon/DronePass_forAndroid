package com.ScienceFiction.DronePassAndroid.feature.weather

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class SunEventTickerTest {

    @Test
    fun `remaining time is shown in whole minutes`() {
        assertEquals(60_000L, SunEventRefreshIntervalMs)
    }

    @Test
    fun `sun event now uses weather response utc offset when available`() {
        val instantMillis = LocalDateTime.parse("2026-01-01T00:00")
            .atOffset(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals(
            LocalDateTime.parse("2026-01-01T09:00"),
            resolveSunEventNow(
                utcOffsetSeconds = 9 * 60 * 60,
                currentTimeMillis = instantMillis,
            ),
        )
    }

    private val sunrises = listOf("2026-11-04T07:03:10", "2026-11-05T07:04:12")
    private val sunsets = listOf("2026-11-04T17:32:30")

    @Test
    fun `next refresh lands right when the floored remaining minutes change`() {
        // 일몰까지 4분 30초: 4분으로 보이다가 30초 뒤(+1ms) 3분으로 바뀐다.
        assertEquals(30_001L, millisUntilSunDisplayChange(LocalDateTime.parse("2026-11-04T17:28:00"), sunrises, sunsets))
        // 일몰까지 정확히 4분: 바로 다음 순간 3분 59초가 되어 3분으로 바뀐다.
        assertEquals(1L, millisUntilSunDisplayChange(LocalDateTime.parse("2026-11-04T17:28:30"), sunrises, sunsets))
        // 일몰 1초 전: 일몰 순간(낮→밤)에 바로 갱신한다.
        assertEquals(1_001L, millisUntilSunDisplayChange(LocalDateTime.parse("2026-11-04T17:32:29"), sunrises, sunsets))
    }

    @Test
    fun `without a future sun event the refresh falls back to one minute`() {
        assertEquals(
            SunEventRefreshIntervalMs,
            millisUntilSunDisplayChange(LocalDateTime.parse("2026-11-05T08:00:00"), sunrises, sunsets),
        )
    }
}
