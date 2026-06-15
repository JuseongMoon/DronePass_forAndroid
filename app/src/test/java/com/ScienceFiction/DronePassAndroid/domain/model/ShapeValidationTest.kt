package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeValidationTest {

    @Test
    fun `도형 Firebase 저장은 iOS처럼 UUID id 만 허용한다`() {
        assertTrue(validCircle().isValidForFirebasePersistence())
        assertFalse(validCircle().copy(id = "not-a-uuid").isValidForFirebasePersistence())
        assertFalse(validCircle().copy(id = "").isValidForFirebasePersistence())
    }

    @Test
    fun `로컬 도형 저장은 빈 제목을 거부한다`() {
        val shape = validCircle().copy(title = "")

        assertFalse(shape.isValidForLocalPersistence())
    }

    @Test
    fun `로컬 도형 저장은 편집 UI 입력처럼 공백 제목을 보존한다`() {
        val shape = validCircle().copy(title = "   ")

        assertTrue(shape.isValidForLocalPersistence())
    }

    @Test
    fun `도형 Firebase 저장은 iOS Firebase 검증처럼 공백 제목을 거부한다`() {
        val shape = validCircle().copy(title = "   ")

        assertFalse(shape.isValidForFirebasePersistence())
    }

    @Test
    fun `도형 Firebase 읽기는 iOS 파서처럼 빈 제목 문자열을 허용한다`() {
        assertTrue(validCircle().copy(title = "").isValidForFirebaseRead())
        assertTrue(validCircle().copy(title = "   ").isValidForFirebaseRead())
        assertFalse(validCircle().copy(id = "not-a-uuid").isValidForFirebaseRead())
    }

    @Test
    fun `iOS 와 같이 좌표 범위와 finite 값을 검증한다`() {
        assertFalse(validCircle().copy(baseCoordinate = Coordinate(91.0, 127.0)).isValidForLocalPersistence())
        assertFalse(validCircle().copy(baseCoordinate = Coordinate(37.0, Double.NaN)).isValidForLocalPersistence())
        assertTrue(validCircle().copy(baseCoordinate = Coordinate(37.0, 127.0)).isValidForLocalPersistence())
    }

    @Test
    fun `도형 Firebase 저장은 iOS 계약처럼 RRGGBB hex 색상만 허용한다`() {
        assertTrue(validCircle().copy(color = "#007AFF").isValidForFirebasePersistence())
        assertTrue(validCircle().copy(color = "#007aff").isValidForFirebasePersistence())
        assertFalse(validCircle().copy(color = "blue").isValidForFirebasePersistence())
        assertFalse(validCircle().copy(color = "#007AFFCC").isValidForFirebasePersistence())
        assertFalse(validCircle().copy(color = "007AFF").isValidForFirebasePersistence())
    }

    @Test
    fun `도형 Firebase 저장은 고도 값이 finite 일 때만 허용한다`() {
        assertTrue(validCircle().copy(height = 120.0).isValidForFirebasePersistence())
        assertFalse(validCircle().copy(height = Double.NaN).isValidForFirebasePersistence())
        assertFalse(validCircle().copy(height = Double.POSITIVE_INFINITY).isValidForFirebasePersistence())
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
    fun `도형 Firebase 저장은 타입별 필수 geometry 를 요구한다`() {
        val validRectangle = validCircle().copy(
            shapeType = ShapeType.RECTANGLE,
            radius = null,
            secondCoordinate = Coordinate(37.1, 127.1),
        )
        val validPolygon = validCircle().copy(
            shapeType = ShapeType.POLYGON,
            radius = null,
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.1, 127.1),
                Coordinate(37.2, 127.2),
            ),
        )
        val validPolyline = validCircle().copy(
            shapeType = ShapeType.POLYLINE,
            radius = null,
            polylineCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.1, 127.1),
            ),
        )

        assertFalse(validCircle().copy(shapeType = ShapeType.RECTANGLE, radius = null).isValidForFirebasePersistence())
        assertFalse(validCircle().copy(shapeType = ShapeType.POLYGON, radius = null).isValidForFirebasePersistence())
        assertFalse(validCircle().copy(shapeType = ShapeType.POLYLINE, radius = null).isValidForFirebasePersistence())
        assertTrue(validRectangle.isValidForFirebasePersistence())
        assertTrue(validPolygon.isValidForFirebasePersistence())
        assertTrue(validPolyline.isValidForFirebasePersistence())
    }

    @Test
    fun `도형 Firebase 쓰기는 실제 면적이나 길이가 없는 geometry 를 거부한다`() {
        val degenerateRectangle = validCircle().copy(
            shapeType = ShapeType.RECTANGLE,
            radius = null,
            secondCoordinate = Coordinate(37.0, 127.1),
        )
        val degeneratePolygon = validCircle().copy(
            shapeType = ShapeType.POLYGON,
            radius = null,
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.0),
            ),
        )
        val degeneratePolyline = validCircle().copy(
            shapeType = ShapeType.POLYLINE,
            radius = null,
            polylineCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.0),
            ),
        )

        assertFalse(degenerateRectangle.isValidForFirebasePersistence())
        assertFalse(degeneratePolygon.isValidForFirebasePersistence())
        assertFalse(degeneratePolyline.isValidForFirebasePersistence())
    }

    @Test
    fun `도형 Firebase 읽기는 iOS처럼 빈 제목과 레거시 퇴화 geometry 를 허용한다`() {
        val blankTitle = validCircle().copy(title = "")
        val degeneratePolygon = validCircle().copy(
            shapeType = ShapeType.POLYGON,
            radius = null,
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.0),
            ),
        )

        assertTrue(blankTitle.isValidForFirebaseRead())
        assertTrue(degeneratePolygon.isValidForFirebaseRead())
    }

    @Test
    fun `Firebase write batch 는 중복 ID 를 거부한다`() {
        val shape = validCircle()

        assertFalse(validateFirebaseShapeBatch(listOf(shape, shape.copy(title = "Duplicate"))).isValid)
    }

    @Test
    fun `Firebase read batch 는 읽기 검증을 사용하고 중복 ID 를 거부한다`() {
        val shape = validCircle()

        assertTrue(validateFirebaseShapeReadBatch(listOf(shape.copy(title = ""))).isValid)
        assertFalse(validateFirebaseShapeReadBatch(listOf(shape, shape.copy(title = ""))).isValid)
    }

    private fun validCircle(): ShapeModel {
        return ShapeModel(
            id = "00000000-0000-0000-0000-000000000001",
            title = "Shape",
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 100.0,
        )
    }
}
