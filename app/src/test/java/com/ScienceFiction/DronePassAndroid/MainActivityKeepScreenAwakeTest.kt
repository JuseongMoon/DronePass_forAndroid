package com.ScienceFiction.DronePassAndroid

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun `알림 탭 shapeId 는 iOS처럼 앱 시작 지도 포커스로 사용하지 않는다`() {
        assertNull(resolveNotificationLaunchFocusShapeId(notificationShapeId = "shape-1"))
        assertNull(resolveNotificationLaunchFocusShapeId(notificationShapeId = null))
    }

    @Test
    fun `알림 탭 Intent 는 iOS처럼 지도 포커스와 팝업 데이터를 분리해 MainScreen 에 전달한다`() {
        val source = mainActivitySource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "initialFocusShapeId.value = resolveNotificationLaunchFocusShapeId(",
                "notificationShapeId = extractNotificationShapeId(intent)",
                "notificationForPopup.value = extractForegroundNotification(intent)",
                "MainScreen(",
                "initialFocusShapeId = initialFocusShapeId.value",
                "initialForegroundNotification = notificationForPopup.value",
            ),
        )
    }

    @Test
    fun `새 알림 Intent 도 iOS처럼 지도 포커스와 팝업 데이터를 같은 순서로 갱신한다`() {
        val source = mainActivitySource()
        val onNewIntentBlock = source.substringAfter("override fun onNewIntent(intent: Intent)")
            .substringBefore("override fun onStart()")

        assertAppearsInOrder(
            source = onNewIntentBlock,
            tokens = listOf(
                "setIntent(intent)",
                "initialFocusShapeId.value = resolveNotificationLaunchFocusShapeId(",
                "notificationShapeId = extractNotificationShapeId(intent)",
                "notificationForPopup.value = extractForegroundNotification(intent)",
            ),
        )
    }

    private fun mainActivitySource(): String {
        return resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/MainActivity.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/MainActivity.kt",
        ).readText()
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in MainActivity.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
