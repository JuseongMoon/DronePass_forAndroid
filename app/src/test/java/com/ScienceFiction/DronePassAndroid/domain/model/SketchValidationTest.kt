package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SketchValidationTest {

    @Test
    fun `스케치 Firebase 저장은 iOS처럼 UUID id 만 허용한다`() {
        assertTrue(validSketch().isValidForFirebasePersistence())
        assertFalse(validSketch().copy(id = "not-a-uuid").isValidForFirebasePersistence())
        assertFalse(validSketch().copy(id = "").isValidForFirebasePersistence())
    }

    @Test
    fun `스케치 Firebase 저장은 iOS처럼 좌표 범위와 finite 값을 검증한다`() {
        assertFalse(
            validSketch().copy(
                points = listOf(Coordinate(latitude = 91.0, longitude = 127.0)),
            ).isValidForFirebasePersistence(),
        )
        assertFalse(
            validSketch().copy(
                points = listOf(Coordinate(latitude = 37.0, longitude = Double.NaN)),
            ).isValidForFirebasePersistence(),
        )
        assertTrue(
            validSketch().copy(
                points = listOf(Coordinate(latitude = 37.0, longitude = 127.0)),
            ).isValidForFirebasePersistence(),
        )
    }

    @Test
    fun `스케치 Firebase 저장은 iOS 계약처럼 RRGGBB hex 색상만 허용한다`() {
        assertTrue(validSketch().copy(color = "#FF0000").isValidForFirebasePersistence())
        assertTrue(validSketch().copy(color = "#ff0000").isValidForFirebasePersistence())
        assertFalse(validSketch().copy(color = "red").isValidForFirebasePersistence())
        assertFalse(validSketch().copy(color = "#FF000080").isValidForFirebasePersistence())
        assertFalse(validSketch().copy(color = "FF0000").isValidForFirebasePersistence())
    }

    @Test
    fun `스케치 Firebase 저장은 iOS처럼 strokeWidth 0 초과 50 이하만 허용한다`() {
        assertFalse(validSketch().copy(strokeWidth = 0.0).isValidForFirebasePersistence())
        assertFalse(validSketch().copy(strokeWidth = -1.0).isValidForFirebasePersistence())
        assertFalse(validSketch().copy(strokeWidth = 50.1).isValidForFirebasePersistence())
        assertFalse(validSketch().copy(strokeWidth = Double.POSITIVE_INFINITY).isValidForFirebasePersistence())
        assertTrue(validSketch().copy(strokeWidth = 50.0).isValidForFirebasePersistence())
    }

    @Test
    fun `스케치 Firebase batch 는 중복 ID 를 거부한다`() {
        val sketch = validSketch()

        assertFalse(validateFirebaseSketchBatch(listOf(sketch, sketch.copy(color = "#00FF00"))).isValid)
    }

    private fun validSketch(): SketchModel {
        return SketchModel(
            id = "00000000-0000-0000-0000-000000000001",
            points = listOf(
                Coordinate(latitude = 37.0, longitude = 127.0),
                Coordinate(latitude = 37.1, longitude = 127.1),
            ),
            color = "#FF0000",
            strokeWidth = 4.0,
            opacity = 1.0,
        )
    }
}
