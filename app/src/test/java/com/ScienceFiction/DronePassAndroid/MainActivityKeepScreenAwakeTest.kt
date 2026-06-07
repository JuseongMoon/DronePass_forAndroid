package com.ScienceFiction.DronePassAndroid

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityKeepScreenAwakeTest {

    @Test
    fun `화면 항상 켜기는 iOS isIdleTimerDisabled처럼 켜면 window flag를 추가한다`() {
        assertEquals(
            KeepScreenAwakeFlagUpdate.ADD,
            resolveKeepScreenAwakeFlagUpdate(keepScreenAwake = true),
        )
    }

    @Test
    fun `화면 항상 켜기는 iOS isIdleTimerDisabled처럼 끄면 window flag를 제거한다`() {
        assertEquals(
            KeepScreenAwakeFlagUpdate.CLEAR,
            resolveKeepScreenAwakeFlagUpdate(keepScreenAwake = false),
        )
    }

    @Test
    fun `앱 시작 알림 권한은 iOS requestNotificationPermission처럼 Android 13 이상에서 아직 허용되지 않았을 때 요청한다`() {
        assertTrue(
            shouldRequestLaunchNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = false,
                alreadyRequested = false,
            ),
        )
    }

    @Test
    fun `앱 시작 알림 권한은 Android 13 미만 이미 허용 이미 요청 상태에서는 다시 요청하지 않는다`() {
        assertFalse(
            shouldRequestLaunchNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU - 1,
                permissionGranted = false,
                alreadyRequested = false,
            ),
        )
        assertFalse(
            shouldRequestLaunchNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = true,
                alreadyRequested = false,
            ),
        )
        assertFalse(
            shouldRequestLaunchNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionGranted = false,
                alreadyRequested = true,
            ),
        )
    }
}
