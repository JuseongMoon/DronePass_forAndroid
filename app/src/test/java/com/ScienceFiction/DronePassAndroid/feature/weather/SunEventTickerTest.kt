package com.ScienceFiction.DronePassAndroid.feature.weather

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class SunEventTickerTest {

    @Test
    fun `sun event remaining time refreshes every minute like iOS SettingManager`() {
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
}
