package com.ScienceFiction.DronePassAndroid.core.data

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferenceKeysTest {

    @Test
    fun `알림 설정 키 이름은 iOS UserDefaults 이름과 동일하게 유지한다`() {
        assertEquals("sunriseAlarmEnabled", NotificationPreferenceKeys.SUNRISE_ALARM_ENABLED.name)
        assertEquals("sunsetAlarmEnabled", NotificationPreferenceKeys.SUNSET_ALARM_ENABLED.name)
        assertEquals("endDateAlarmEnabled", NotificationPreferenceKeys.END_DATE_ALARM_ENABLED.name)
        assertEquals(
            "launchNotificationPermissionRequested",
            NotificationPreferenceKeys.LAUNCH_NOTIFICATION_PERMISSION_REQUESTED.name,
        )
    }

    @Test
    fun `알림 설정 키는 기존 Android snake case 값을 fallback 으로 읽는다`() {
        val preferences = preferencesOf(
            NotificationPreferenceKeys.LEGACY_SUNRISE_ALARM_ENABLED to true,
            NotificationPreferenceKeys.LEGACY_SUNSET_ALARM_ENABLED to true,
            NotificationPreferenceKeys.LEGACY_END_DATE_ALARM_ENABLED to true,
        )

        assertTrue(storedSunriseAlarmEnabled(preferences))
        assertTrue(storedSunsetAlarmEnabled(preferences))
        assertTrue(storedEndDateAlarmEnabled(preferences))
    }

    @Test
    fun `알림 설정 키는 iOS primary 값이 있으면 legacy 값보다 우선한다`() {
        val preferences = preferencesOf(
            NotificationPreferenceKeys.SUNRISE_ALARM_ENABLED to false,
            NotificationPreferenceKeys.SUNSET_ALARM_ENABLED to false,
            NotificationPreferenceKeys.END_DATE_ALARM_ENABLED to false,
            NotificationPreferenceKeys.LEGACY_SUNRISE_ALARM_ENABLED to true,
            NotificationPreferenceKeys.LEGACY_SUNSET_ALARM_ENABLED to true,
            NotificationPreferenceKeys.LEGACY_END_DATE_ALARM_ENABLED to true,
        )

        assertFalse(storedSunriseAlarmEnabled(preferences))
        assertFalse(storedSunsetAlarmEnabled(preferences))
        assertFalse(storedEndDateAlarmEnabled(preferences))
    }

    @Test
    fun `앱 시작 알림 권한 요청 기록은 없으면 false 저장값이 있으면 true 이다`() {
        assertFalse(storedLaunchNotificationPermissionRequested(preferencesOf()))
        assertTrue(
            storedLaunchNotificationPermissionRequested(
                preferencesOf(NotificationPreferenceKeys.LAUNCH_NOTIFICATION_PERMISSION_REQUESTED to true),
            ),
        )
    }
}
