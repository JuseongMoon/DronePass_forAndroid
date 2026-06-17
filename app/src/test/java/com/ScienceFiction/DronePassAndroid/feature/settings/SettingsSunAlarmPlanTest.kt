package com.ScienceFiction.DronePassAndroid.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSunAlarmPlanTest {

    @Test
    fun `일출 알림 ON 재예약은 iOS처럼 이미 켜진 일몰 알림도 함께 갱신한다`() {
        assertEquals(
            SunAlarmRescheduleTargets(sunrise = true, sunset = true),
            enabledSunAlarmTargetsForReschedule(
                sunriseAlarmEnabled = true,
                sunsetAlarmEnabled = true,
            ),
        )
    }

    @Test
    fun `일몰 알림 ON 재예약은 iOS처럼 이미 켜진 일출 알림도 함께 갱신한다`() {
        assertEquals(
            SunAlarmRescheduleTargets(sunrise = true, sunset = true),
            enabledSunAlarmTargetsForReschedule(
                sunriseAlarmEnabled = true,
                sunsetAlarmEnabled = true,
            ),
        )
    }

    @Test
    fun `일출 일몰 알림이 모두 꺼져 있으면 재예약하지 않는다`() {
        val targets = enabledSunAlarmTargetsForReschedule(
            sunriseAlarmEnabled = false,
            sunsetAlarmEnabled = false,
        )

        assertFalse(targets.sunrise)
        assertFalse(targets.sunset)
        assertFalse(targets.hasAnyEnabled)
    }

    @Test
    fun `켜진 일출 일몰 알림만 재예약 대상으로 남긴다`() {
        val sunriseOnly = enabledSunAlarmTargetsForReschedule(
            sunriseAlarmEnabled = true,
            sunsetAlarmEnabled = false,
        )
        val sunsetOnly = enabledSunAlarmTargetsForReschedule(
            sunriseAlarmEnabled = false,
            sunsetAlarmEnabled = true,
        )

        assertTrue(sunriseOnly.sunrise)
        assertFalse(sunriseOnly.sunset)
        assertTrue(sunriseOnly.hasAnyEnabled)
        assertFalse(sunsetOnly.sunrise)
        assertTrue(sunsetOnly.sunset)
        assertTrue(sunsetOnly.hasAnyEnabled)
    }
}
