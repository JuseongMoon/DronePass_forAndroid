package com.ScienceFiction.DronePassAndroid.feature.drone

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneNextColorTest {

    @Test
    fun `사용하지 않은 색상이 있으면 iOS처럼 회색을 제외한 첫 후보를 추천한다`() {
        val color = suggestNextDroneColor(
            drones = listOf(
                DroneModel(color = PaletteColor.RED.hex),
                DroneModel(color = PaletteColor.ORANGE.hex),
            ),
        )

        assertEquals(PaletteColor.YELLOW, color)
    }

    @Test
    fun `모든 비회색 색상이 사용되면 iOS처럼 회색을 제외한 후보에서 고른다`() {
        val usedDrones = PaletteColor.entries
            .filter { it != PaletteColor.GRAY }
            .map { DroneModel(color = it.hex) }

        val color = suggestNextDroneColor(
            drones = usedDrones,
            randomIndex = { 8 },
        )

        assertEquals(PaletteColor.PINK, color)
        assertTrue(color != PaletteColor.GRAY)
    }
}
