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
    fun `기존 이름이 길어도 새 이름 폭이 iOS 제한을 넘으면 거부한다`() {
        val accepted = shouldAcceptDroneNameChange(
            oldName = "Very very long drone name",
            newName = "Very long drone name",
            maxWidthPx = 210f,
            measureTextWidth = { it.length * 20f },
        )

        assertFalse(accepted)
    }
}
