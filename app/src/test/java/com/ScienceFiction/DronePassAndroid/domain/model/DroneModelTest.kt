package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DroneModelTest {

    @Test
    fun `기본 드론 이름은 iOS처럼 호출자가 현지화 문자열을 주입할 수 있다`() {
        val drone = DroneModel.createDefault(defaultName = "My Drone")

        assertEquals("My Drone", drone.name)
        assertEquals(PaletteColor.BLUE.hex, drone.color)
    }

    @Test
    fun `기본 드론 fallback 이름은 iOS 첫 번째 기본명과 같다`() {
        assertEquals("내 드론", DroneModel.createDefault().name)
    }

    @Test
    fun `신규 드론 편의 생성자는 iOS처럼 회색을 건너뛴다`() {
        assertEquals(PaletteColor.PINK.hex, DroneModel.createNew(8).color)
        assertEquals(PaletteColor.RED.hex, DroneModel.createNew(9).color)
    }
}
