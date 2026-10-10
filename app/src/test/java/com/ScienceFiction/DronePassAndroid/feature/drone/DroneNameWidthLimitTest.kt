package com.ScienceFiction.DronePassAndroid.feature.drone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneNameWidthLimitTest {

    @Test
    fun `이름 폭이 iOS 제한 안이면 입력을 허용한다`() {
        val accepted = shouldAcceptDroneNameChange(
            oldName = "Drone",
            newName = "Drone 1",
            maxWidthPx = 210f,
            measureTextWidth = { it.length * 20f },
        )

        assertTrue(accepted)
    }

    @Test
    fun `이름 폭이 iOS 제한을 넘으면 입력을 거부한다`() {
        val accepted = shouldAcceptDroneNameChange(
            oldName = "Drone",
            newName = "Very long drone name",
            maxWidthPx = 210f,
            measureTextWidth = { it.length * 20f },
        )

        assertFalse(accepted)
    }

    @Test
    fun `이미 제한을 넘은 이름은 줄이는 것만 허용한다`() {
        // 다른 기기에서 받은 긴 이름이나 글꼴 크기 변경으로 넘은 이름을 지우지도 못해 이름 칸이 잠겼다.
        assertTrue(
            shouldAcceptDroneNameChange(
                oldName = "Very very long drone name",
                newName = "Very long drone name",
                maxWidthPx = 210f,
                measureTextWidth = { it.length * 20f },
            ),
        )
        assertFalse(
            shouldAcceptDroneNameChange(
                oldName = "Very very long drone name",
                newName = "Very very long drone name!",
                maxWidthPx = 210f,
                measureTextWidth = { it.length * 20f },
            ),
        )
    }

    @Test
    fun `드론 편집 시트 입력은 열려 있는 동안 추천 값이 바뀌어도 유지된다`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { java.io.File(java.io.File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val sheet = java.io.File(root, "com/ScienceFiction/DronePassAndroid/feature/drone/DroneEditSheet.kt").readText()
        assertTrue(sheet.contains("var name by remember(drone?.id) {"))
        assertTrue(sheet.contains("var selectedColor by remember(drone?.id) {"))
    }
}
