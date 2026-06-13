package com.ScienceFiction.DronePassAndroid.feature.settings

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionRequestTest {

    @Test
    fun `POST_NOTIFICATIONS 권한은 Android 13 미만에서 항상 허용으로 취급한다`() {
        assertTrue(
            resolveNotificationPermissionGranted(
                sdkInt = Build.VERSION_CODES.TIRAMISU - 1,
                permissionGranted = false,
            ),
        )
    }

    @Test
    fun `POST_NOTIFICATIONS 권한은 Android 13 이상에서 실제 권한 상태를 따른다`() {
        assertFalse(
            resolveNotificationPermissionGranted(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = false,
            ),
        )
        assertTrue(
            resolveNotificationPermissionGranted(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = true,
            ),
        )
    }

    @Test
    fun `정확한 알람 권한은 Android 12 미만에서 항상 허용으로 취급한다`() {
        assertTrue(
            resolveExactAlarmPermissionGranted(
                sdkInt = Build.VERSION_CODES.S - 1,
                canScheduleExactAlarms = false,
            ),
        )
    }

    @Test
    fun `정확한 알람 권한은 Android 12 이상에서 AlarmManager 상태를 따른다`() {
        assertFalse(
            resolveExactAlarmPermissionGranted(
                sdkInt = Build.VERSION_CODES.S,
                canScheduleExactAlarms = null,
            ),
        )
        assertFalse(
            resolveExactAlarmPermissionGranted(
                sdkInt = Build.VERSION_CODES.S,
                canScheduleExactAlarms = false,
            ),
        )
        assertTrue(
            resolveExactAlarmPermissionGranted(
                sdkInt = Build.VERSION_CODES.S,
                canScheduleExactAlarms = true,
            ),
        )
    }

    @Test
    fun `알림 권한 안내 카드는 둘 중 하나라도 부족하면 표시한다`() {
        assertFalse(
            shouldRenderNotificationPermissionRequest(
                notificationPermissionGranted = true,
                exactAlarmGranted = true,
            ),
        )
        assertTrue(
            shouldRenderNotificationPermissionRequest(
                notificationPermissionGranted = false,
                exactAlarmGranted = true,
            ),
        )
        assertTrue(
            shouldRenderNotificationPermissionRequest(
                notificationPermissionGranted = true,
                exactAlarmGranted = false,
            ),
        )
    }

    @Test
    fun `정확한 알람 설정 이동은 Android 12 이상에서만 사용한다`() {
        assertFalse(shouldOpenExactAlarmSettings(Build.VERSION_CODES.S - 1))
        assertTrue(shouldOpenExactAlarmSettings(Build.VERSION_CODES.S))
    }
}
