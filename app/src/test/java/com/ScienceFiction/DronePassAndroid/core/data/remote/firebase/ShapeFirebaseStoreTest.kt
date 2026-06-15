package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeFirebaseStoreTest {

    @Test
    fun `좌표 컴포넌트는 iOS ShapeFirebaseStore 와 동일하게 소수 6자리로 반올림한다`() {
        assertEquals(37.123457, roundCoordinateComponent(37.1234567), 0.0)
        assertEquals(126.987654, roundCoordinateComponent(126.9876544), 0.0)
        assertEquals(-73.123457, roundCoordinateComponent(-73.1234567), 0.0)
    }

    @Test
    fun `Firestore 좌표 map 은 iOS 와 같은 latitude longitude 키와 6자리 반올림을 사용한다`() {
        val map = coordinateToFirestoreMap(Coordinate(37.1234567, 126.9876544))

        assertEquals(37.123457, map["latitude"])
        assertEquals(126.987654, map["longitude"])
    }

    @Test
    fun `Shape Firestore 쓰기는 iOS 계약처럼 소문자 shapeType Timestamp Double 좌표 map을 사용한다`() {
        val shape = ShapeModel(
            id = "00000000-0000-0000-0000-000000000001",
            title = "신규 도형22",
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.1234567, 126.9876544),
            radius = 120.0,
            color = "#007aff",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            flightStartDate = 1_700_000_456_000L,
            flightEndDate = 1_700_000_789_000L,
            deletedAt = 1_700_000_999_000L,
        )

        val data = shapeToFirestoreDocumentData(shape)
        val baseCoordinate = data["baseCoordinate"] as Map<*, *>

        assertEquals(shape.id, data["id"])
        assertEquals("circle", data["shapeType"])
        assertFalse(data["shapeType"] == ShapeType.CIRCLE.name)
        assertEquals("#007AFF", data["color"])
        assertEquals("", data["memo"])
        assertEquals("", data["address"])
        assertEquals(37.123457, baseCoordinate["latitude"])
        assertEquals(126.987654, baseCoordinate["longitude"])
        assertTrue(baseCoordinate["latitude"] is Double)
        assertTrue(baseCoordinate["longitude"] is Double)
        assertTrue(data["createdAt"] is Timestamp)
        assertTrue(data["updatedAt"] is Timestamp)
        assertTrue(data["flightStartDate"] is Timestamp)
        assertTrue(data["flightEndDate"] is Timestamp)
        assertTrue(data["deletedAt"] is Timestamp)
        assertFalse(data.containsKey("startedAt"))
        assertFalse(data.containsKey("expireDate"))
    }

    @Test
    fun `Shape Firestore 쓰기는 polygon polyline 좌표도 GeoPoint 대신 Double map 배열로 저장한다`() {
        val polygonShape = ShapeModel(
            id = "00000000-0000-0000-0000-000000000002",
            title = "Polygon",
            shapeType = ShapeType.POLYGON,
            baseCoordinate = Coordinate(37.0, 127.0),
            polygonCoordinates = listOf(
                Coordinate(37.1234567, 127.1234567),
                Coordinate(37.2234567, 127.2234567),
                Coordinate(37.3234567, 127.3234567),
            ),
            color = "#007AFF",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            flightStartDate = 1_700_000_456_000L,
        )
        val polylineShape = ShapeModel(
            id = "00000000-0000-0000-0000-000000000003",
            title = "Polyline",
            shapeType = ShapeType.POLYLINE,
            baseCoordinate = Coordinate(37.0, 127.0),
            polylineCoordinates = listOf(
                Coordinate(36.1234567, 126.1234567),
                Coordinate(36.2234567, 126.2234567),
            ),
            color = "#007AFF",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            flightStartDate = 1_700_000_456_000L,
        )

        val polygonData = shapeToFirestoreDocumentData(polygonShape)
        val polygonPoints = polygonData["polygonCoordinates"] as List<*>
        val firstPolygonPoint = polygonPoints.first() as Map<*, *>
        val polylineData = shapeToFirestoreDocumentData(polylineShape)
        val polylinePoints = polylineData["polylineCoordinates"] as List<*>
        val firstPolylinePoint = polylinePoints.first() as Map<*, *>

        assertEquals("polygon", polygonData["shapeType"])
        assertEquals(37.123457, firstPolygonPoint["latitude"])
        assertEquals(127.123457, firstPolygonPoint["longitude"])
        assertTrue(firstPolygonPoint["latitude"] is Double)
        assertTrue(firstPolygonPoint["longitude"] is Double)
        assertEquals("polyline", polylineData["shapeType"])
        assertEquals(36.123457, firstPolylinePoint["latitude"])
        assertEquals(126.123457, firstPolylinePoint["longitude"])
        assertTrue(firstPolylinePoint["latitude"] is Double)
        assertTrue(firstPolylinePoint["longitude"] is Double)
    }

    @Test
    fun `Shape Firestore 쓰기는 rectangle secondCoordinate 도 Double map 으로 저장한다`() {
        val shape = ShapeModel(
            id = "00000000-0000-0000-0000-000000000004",
            title = "Rectangle",
            shapeType = ShapeType.RECTANGLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            secondCoordinate = Coordinate(37.1234567, 127.1234567),
            color = "#007AFF",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            flightStartDate = 1_700_000_456_000L,
        )

        val data = shapeToFirestoreDocumentData(shape)
        val secondCoordinate = data["secondCoordinate"] as Map<*, *>

        assertEquals("rectangle", data["shapeType"])
        assertEquals(37.123457, secondCoordinate["latitude"])
        assertEquals(127.123457, secondCoordinate["longitude"])
        assertTrue(secondCoordinate["latitude"] is Double)
        assertTrue(secondCoordinate["longitude"] is Double)
    }

    @Test
    fun `Shape Firestore 문서 데이터는 현재 shapeType 에 맞지 않는 geometry 필드를 쓰지 않는다`() {
        val circle = ShapeModel(
            id = "00000000-0000-0000-0000-000000000005",
            title = "Circle",
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 120.0,
            secondCoordinate = Coordinate(37.1, 127.1),
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.1, 127.1),
                Coordinate(37.2, 127.2),
            ),
            polylineCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.1, 127.1),
            ),
            color = "#007AFF",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            flightStartDate = 1_700_000_456_000L,
        )
        val rectangle = circle.copy(
            id = "00000000-0000-0000-0000-000000000006",
            title = "Rectangle",
            shapeType = ShapeType.RECTANGLE,
        )

        val circleData = shapeToFirestoreDocumentData(circle)
        val rectangleData = shapeToFirestoreDocumentData(rectangle)

        assertEquals(120.0, circleData["radius"])
        assertFalse(circleData.containsKey("secondCoordinate"))
        assertFalse(circleData.containsKey("polygonCoordinates"))
        assertFalse(circleData.containsKey("polylineCoordinates"))
        assertFalse(rectangleData.containsKey("radius"))
        assertTrue(rectangleData["secondCoordinate"] is Map<*, *>)
        assertFalse(rectangleData.containsKey("polygonCoordinates"))
        assertFalse(rectangleData.containsKey("polylineCoordinates"))
    }

    @Test
    fun `Shape Firestore merge 쓰기는 누락 optional 필드를 delete sentinel 로 정리한다`() {
        val shape = ShapeModel(
            id = "00000000-0000-0000-0000-000000000007",
            title = "Circle",
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 120.0,
            color = "#007AFF",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            flightStartDate = 1_700_000_456_000L,
        )

        val data = shapeToFirestoreMergeData(shape)

        assertEquals(120.0, data["radius"])
        assertTrue(data["secondCoordinate"] is FieldValue)
        assertTrue(data["polygonCoordinates"] is FieldValue)
        assertTrue(data["polylineCoordinates"] is FieldValue)
        assertTrue(data["height"] is FieldValue)
        assertTrue(data["droneId"] is FieldValue)
        assertTrue(data["flightEndDate"] is FieldValue)
        assertTrue(data["deletedAt"] is FieldValue)
    }
}
