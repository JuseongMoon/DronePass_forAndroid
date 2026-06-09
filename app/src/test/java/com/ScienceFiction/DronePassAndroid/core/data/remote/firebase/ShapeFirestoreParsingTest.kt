package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class ShapeFirestoreParsingTest {

    @Test
    fun `iOS ShapeFirebaseStore 와 같이 필수 필드가 모두 있으면 파싱한다`() {
        val shape = shapeFromFirestoreData(validDocument())

        requireNotNull(shape)
        assertEquals("00000000-0000-0000-0000-000000000001", shape.id)
        assertEquals("Shape", shape.title)
        assertEquals(ShapeType.CIRCLE, shape.shapeType)
        assertEquals("#007AFF", shape.color)
        assertEquals(1_700_000_000_000L, shape.flightStartDate)
    }

    @Test
    fun `iOS ShapeFirebaseStore 와 같이 필수 필드 누락은 invalid 이다`() {
        assertNull(shapeFromFirestoreData(validDocument() - "id"))
        assertNull(shapeFromFirestoreData(validDocument() - "title"))
        assertNull(shapeFromFirestoreData(validDocument() - "color"))
        assertNull(shapeFromFirestoreData(validDocument() - "shapeType"))
        assertNull(shapeFromFirestoreData(validDocument() - "baseCoordinate"))
        assertNull(shapeFromFirestoreData(validDocument() - "flightStartDate"))
    }

    @Test
    fun `iOS ShapeFirebaseStore 와 같이 UUID 로 파싱되지 않는 id 는 invalid 이다`() {
        assertNull(shapeFromFirestoreData(validDocument() + ("id" to "not-a-uuid")))
    }

    @Test
    fun `startedAt 은 iOS 레거시 fallback 으로 flightStartDate 를 대체한다`() {
        val document = validDocument()
            .minus("flightStartDate")
            .plus("startedAt" to Timestamp(Date(1_700_000_123_000L)))

        val shape = shapeFromFirestoreData(document)

        requireNotNull(shape)
        assertEquals(1_700_000_123_000L, shape.flightStartDate)
    }

    @Test
    fun `expireDate 는 iOS 레거시 fallback 으로 flightEndDate 를 대체한다`() {
        val document = validDocument()
            .plus("expireDate" to Timestamp(Date(1_700_000_456_000L)))

        val shape = shapeFromFirestoreData(document)

        requireNotNull(shape)
        assertEquals(1_700_000_456_000L, shape.flightEndDate)
    }

    @Test
    fun `Firestore 파싱은 Android 레거시 enum name 을 허용하지만 unknown 은 invalid 이다`() {
        val legacy = shapeFromFirestoreData(validDocument() + ("shapeType" to "CIRCLE"))
        val mixedCase = shapeFromFirestoreData(validDocument() + ("shapeType" to "Circle"))
        val unknown = shapeFromFirestoreData(validDocument() + ("shapeType" to "unknown"))

        requireNotNull(legacy)
        assertEquals(ShapeType.CIRCLE, legacy.shapeType)
        requireNotNull(mixedCase)
        assertEquals(ShapeType.CIRCLE, mixedCase.shapeType)
        assertNull(unknown)
    }

    @Test
    fun `Firestore 파싱은 iOS처럼 shapeType 에 맞는 지오메트리 필드만 유지한다`() {
        val staleGeometry = mapOf(
            "radius" to 150.0,
            "secondCoordinate" to mapOf("latitude" to 37.1, "longitude" to 127.1),
            "polygonCoordinates" to listOf(
                mapOf("latitude" to 37.0, "longitude" to 127.0),
                mapOf("latitude" to 37.1, "longitude" to 127.1),
                mapOf("latitude" to 37.2, "longitude" to 127.2),
            ),
            "polylineCoordinates" to listOf(
                mapOf("latitude" to 37.3, "longitude" to 127.3),
                mapOf("latitude" to 37.4, "longitude" to 127.4),
            ),
        )

        val circle = shapeFromFirestoreData(validDocument() + staleGeometry + ("shapeType" to "circle"))
        val rectangle = shapeFromFirestoreData(validDocument() + staleGeometry + ("shapeType" to "rectangle"))
        val polygon = shapeFromFirestoreData(validDocument() + staleGeometry + ("shapeType" to "polygon"))
        val polyline = shapeFromFirestoreData(validDocument() + staleGeometry + ("shapeType" to "polyline"))

        val circleShape = requireNotNull(circle)
        assertEquals(150.0, circleShape.radius ?: 0.0, 0.0)
        assertNull(circleShape.secondCoordinate)
        assertNull(circleShape.polygonCoordinates)
        assertNull(circleShape.polylineCoordinates)

        val rectangleShape = requireNotNull(rectangle)
        assertNull(rectangleShape.radius)
        assertEquals(37.1, rectangleShape.secondCoordinate?.latitude ?: 0.0, 0.0)
        assertNull(rectangleShape.polygonCoordinates)
        assertNull(rectangleShape.polylineCoordinates)

        val polygonShape = requireNotNull(polygon)
        assertNull(polygonShape.radius)
        assertNull(polygonShape.secondCoordinate)
        assertEquals(3, polygonShape.polygonCoordinates?.size)
        assertNull(polygonShape.polylineCoordinates)

        val polylineShape = requireNotNull(polyline)
        assertNull(polylineShape.radius)
        assertNull(polylineShape.secondCoordinate)
        assertNull(polylineShape.polygonCoordinates)
        assertEquals(2, polylineShape.polylineCoordinates?.size)
    }

    private fun validDocument(): Map<String, Any?> {
        return mapOf(
            "id" to "00000000-0000-0000-0000-000000000001",
            "title" to "Shape",
            "shapeType" to "circle",
            "baseCoordinate" to mapOf(
                "latitude" to 37.0,
                "longitude" to 127.0,
            ),
            "radius" to 100.0,
            "color" to "#007AFF",
            "flightStartDate" to Timestamp(Date(1_700_000_000_000L)),
        )
    }
}
