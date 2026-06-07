package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeValidationTest {

    @Test
    fun `iOS 와 같이 빈 제목은 유효하지 않다`() {
        val shape = validCircle().copy(title = "   ")

        assertFalse(shape.isValidForLocalPersistence())
    }

    @Test
    fun `iOS 와 같이 좌표 범위와 finite 값을 검증한다`() {
        assertFalse(validCircle().copy(baseCoordinate = Coordinate(91.0, 127.0)).isValidForLocalPersistence())
        assertFalse(validCircle().copy(baseCoordinate = Coordinate(37.0, Double.NaN)).isValidForLocalPersistence())
        assertTrue(validCircle().copy(baseCoordinate = Coordinate(37.0, 127.0)).isValidForLocalPersistence())
    }

    @Test
    fun `원형 반경은 없으면 통과하지만 0 이하와 Firebase 50km 초과는 거부한다`() {
        assertTrue(validCircle().copy(radius = null).isValidForLocalPersistence())
        assertFalse(validCircle().copy(radius = 0.0).isValidForLocalPersistence())
        assertTrue(validCircle().copy(radius = 60_000.0).isValidForLocalPersistence())
        assertFalse(validCircle().copy(radius = 60_000.0).isValidForFirebasePersistence())
    }

    @Test
    fun `polygon polyline 좌표 개수는 iOS Firebase 규칙과 같다`() {
        val polygon = validCircle().copy(
            shapeType = ShapeType.POLYGON,
            polygonCoordinates = listOf(Coordinate(37.0, 127.0), Coordinate(37.1, 127.1)),
        )
        val polyline = validCircle().copy(
            shapeType = ShapeType.POLYLINE,
            polylineCoordinates = listOf(Coordinate(37.0, 127.0)),
        )

        assertFalse(polygon.isValidForFirebasePersistence())
        assertFalse(polyline.isValidForFirebasePersistence())
    }

    @Test
    fun `Firebase batch 는 중복 ID 를 거부한다`() {
        val shape = validCircle()

        assertFalse(validateFirebaseShapeBatch(listOf(shape, shape.copy(title = "Duplicate"))).isValid)
    }

    private fun validCircle(): ShapeModel {
        return ShapeModel(
            id = "shape-1",
            title = "Shape",
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 100.0,
        )
    }
}
