package com.ScienceFiction.DronePassAndroid.feature.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class SunEventTickerTest {

    @Test
    fun `sun event remaining time refreshes every minute like iOS SettingManager`() {
        assertEquals(60_000L, SunEventRefreshIntervalMs)
    }
}
