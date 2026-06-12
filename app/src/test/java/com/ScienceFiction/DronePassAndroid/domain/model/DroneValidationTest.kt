package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneValidationTest {

    @Test
    fun `드론 Firebase 저장은 iOS 계약처럼 UUID id 만 허용한다`() {
        assertTrue(validDrone().isValidForFirebasePersistence())
        assertFalse(validDrone().copy(id = "drone-1").isValidForFirebasePersistence())
        assertFalse(validDrone().copy(id = "").isValidForFirebasePersistence())
    }

    @Test
    fun `드론 Firebase 저장은 빈 이름을 거부한다`() {
        assertFalse(validDrone().copy(name = "").isValidForFirebasePersistence())
        assertFalse(validDrone().copy(name = "   ").isValidForFirebasePersistence())
    }

    @Test
    fun `드론 Firebase 읽기는 iOS 파서처럼 빈 이름 문자열을 허용한다`() {
        assertTrue(validDrone().copy(name = "").isValidForFirebaseRead())
        assertTrue(validDrone().copy(name = "   ").isValidForFirebaseRead())
        assertFalse(validDrone().copy(id = "not-a-uuid").isValidForFirebaseRead())
    }

    @Test
    fun `드론 Firebase 저장은 iOS 계약처럼 RRGGBB hex 색상만 허용한다`() {
        assertTrue(validDrone().copy(color = "#007AFF").isValidForFirebasePersistence())
        assertTrue(validDrone().copy(color = "#007aff").isValidForFirebasePersistence())
        assertFalse(validDrone().copy(color = "blue").isValidForFirebasePersistence())
        assertFalse(validDrone().copy(color = "#007AFFCC").isValidForFirebasePersistence())
        assertFalse(validDrone().copy(color = "007AFF").isValidForFirebasePersistence())
    }

    @Test
    fun `드론 Firebase batch 는 중복 ID 를 거부한다`() {
        val drone = validDrone()

        assertFalse(validateFirebaseDroneBatch(listOf(drone, drone.copy(name = "Duplicate"))).isValid)
    }

    private fun validDrone(): DroneModel {
        return DroneModel(
            id = "00000000-0000-0000-0000-000000000001",
            name = "Drone",
            color = "#007AFF",
        )
    }
}
