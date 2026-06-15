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
    fun `Firestore 파싱은 iOS처럼 잘못된 색상 문자열을 기본색으로 살린다`() {
        val shape = shapeFromFirestoreData(validDocument() + ("color" to "blue"))

        requireNotNull(shape)
        assertEquals("#007AFF", shape.color)
    }

    @Test
    fun `Firestore 파싱은 iOS처럼 빈 제목 문자열을 보존한다`() {
        val emptyTitle = shapeFromFirestoreData(validDocument() + ("title" to ""))
        val whitespaceTitle = shapeFromFirestoreData(validDocument() + ("title" to "   "))

        assertEquals("", requireNotNull(emptyTitle).title)
        assertEquals("   ", requireNotNull(whitespaceTitle).title)
    }

    @Test
    fun `Firestore 파싱은 Firebase 계약을 어긴 좌표 범위를 invalid 로 본다`() {
        val invalidBaseCoordinate = validDocument() + (
            "baseCoordinate" to mapOf(
                "latitude" to 91.0,
                "longitude" to 127.0,
            )
        )
        val nonFiniteBaseCoordinate = validDocument() + (
            "baseCoordinate" to mapOf(
                "latitude" to Double.NaN,
                "longitude" to 127.0,
            )
        )

        assertNull(shapeFromFirestoreData(invalidBaseCoordinate))
        assertNull(shapeFromFirestoreData(nonFiniteBaseCoordinate))
    }

    @Test
    fun `Firestore 파싱은 필수 좌표 타입 불일치와 non-finite 숫자를 invalid 로 본다`() {
        val integerBaseCoordinate = validDocument() + (
            "baseCoordinate" to mapOf(
                "latitude" to 37,
                "longitude" to 127.0,
            )
        )
        val nonFiniteHeight = validDocument() + ("height" to Double.NaN)
        val integerRectangleCoordinate = validDocument() + mapOf(
            "shapeType" to "rectangle",
            "secondCoordinate" to mapOf("latitude" to 37.0, "longitude" to 127),
        )

        assertNull(shapeFromFirestoreData(integerBaseCoordinate))
        assertNull(shapeFromFirestoreData(nonFiniteHeight))
        assertNull(shapeFromFirestoreData(integerRectangleCoordinate))
    }

    @Test
    fun `Firestore 파싱은 iOS처럼 optional 정수 숫자를 Double 로 읽는다`() {
        val shape = shapeFromFirestoreData(
            validDocument() + mapOf(
                "radius" to 100,
                "height" to 120L,
            ),
        )

        requireNotNull(shape)
        assertEquals(100.0, shape.radius ?: 0.0, 0.0)
        assertEquals(120.0, shape.height ?: 0.0, 0.0)
    }

    @Test
    fun `Firestore 파싱은 optional 숫자 문자열을 누락값으로 본다`() {
        val shape = shapeFromFirestoreData(
            validDocument() + mapOf(
                "radius" to "100",
                "height" to "120",
            ),
        )

        requireNotNull(shape)
        assertNull(shape.radius)
        assertNull(shape.height)
    }

    @Test
    fun `Firestore 파싱은 Firebase 계약을 어긴 원형 반경을 invalid 로 본다`() {
        assertNull(shapeFromFirestoreData(validDocument() + ("radius" to 0.0)))
        assertNull(shapeFromFirestoreData(validDocument() + ("radius" to Double.POSITIVE_INFINITY)))
        assertNull(shapeFromFirestoreData(validDocument() + ("radius" to 50_000.1)))
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
    fun `표준 flightStartDate 가 있으면 레거시 startedAt 보다 우선한다`() {
        val document = validDocument()
            .plus("flightStartDate" to Timestamp(Date(1_700_000_222_000L)))
            .plus("startedAt" to Timestamp(Date(1_700_000_111_000L)))

        val shape = shapeFromFirestoreData(document)

        requireNotNull(shape)
        assertEquals(1_700_000_222_000L, shape.flightStartDate)
    }

    @Test
    fun `Firestore 파싱은 시작일 Long 과 문자열을 invalid 로 본다`() {
        assertNull(shapeFromFirestoreData(validDocument() + ("flightStartDate" to 1_700_000_000_000L)))
        assertNull(shapeFromFirestoreData(validDocument() + ("flightStartDate" to "2026-06-14T00:00:00Z")))
        assertNull(
            shapeFromFirestoreData(
                validDocument()
                    .minus("flightStartDate")
                    .plus("startedAt" to 1_700_000_000_000L),
            ),
        )
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

    @Test
    fun `Firestore 파싱은 polygon polyline 손상 좌표 원소만 iOS처럼 제외한다`() {
        val polygonWithOneBrokenPoint = validDocument() + mapOf(
            "shapeType" to "polygon",
            "polygonCoordinates" to listOf(
                mapOf("latitude" to 37.0, "longitude" to 127.0),
                mapOf("latitude" to "37.1", "longitude" to 127.1),
                mapOf("latitude" to 37.2, "longitude" to 127.2),
                mapOf("latitude" to 37.3, "longitude" to 127.3),
            ),
        )
        val polylineWithOneBrokenPoint = validDocument() + mapOf(
            "shapeType" to "polyline",
            "polylineCoordinates" to listOf(
                mapOf("latitude" to 37.0, "longitude" to 127.0),
                mapOf("latitude" to "37.1", "longitude" to 127.1),
                mapOf("latitude" to 37.2, "longitude" to 127.2),
            ),
        )

        val polygon = shapeFromFirestoreData(polygonWithOneBrokenPoint)
        val polyline = shapeFromFirestoreData(polylineWithOneBrokenPoint)

        assertEquals(3, requireNotNull(polygon).polygonCoordinates?.size)
        assertEquals(2, requireNotNull(polyline).polylineCoordinates?.size)
    }

    @Test
    fun `Firestore 파싱은 배열 좌표 값이 Firebase 범위를 벗어나면 invalid 로 본다`() {
        val polygonWithOutOfRangePoint = validDocument() + mapOf(
            "shapeType" to "polygon",
            "polygonCoordinates" to listOf(
                mapOf("latitude" to 37.0, "longitude" to 127.0),
                mapOf("latitude" to 37.1, "longitude" to 127.1),
                mapOf("latitude" to 91.0, "longitude" to 127.2),
            ),
        )
        val polylineWithNonFinitePoint = validDocument() + mapOf(
            "shapeType" to "polyline",
            "polylineCoordinates" to listOf(
                mapOf("latitude" to 37.0, "longitude" to 127.0),
                mapOf("latitude" to 37.1, "longitude" to Double.NaN),
            ),
        )

        assertNull(shapeFromFirestoreData(polygonWithOutOfRangePoint))
        assertNull(shapeFromFirestoreData(polylineWithNonFinitePoint))
    }

    @Test
    fun `Firestore 파싱은 타입별 필수 geometry 누락과 복구 불가능한 좌표를 invalid 로 본다`() {
        val rectangleMissingSecond = validDocument() + ("shapeType" to "rectangle")
        val polygonMissingCoordinates = validDocument() + ("shapeType" to "polygon")
        val polygonBelowMinimum = validDocument() + mapOf(
            "shapeType" to "polygon",
            "polygonCoordinates" to listOf(
                mapOf("latitude" to 37.0, "longitude" to 127.0),
                mapOf("latitude" to 37.1, "longitude" to 127.1),
            ),
        )
        val polylineMissingCoordinates = validDocument() + ("shapeType" to "polyline")
        val polylineInvalidCoordinate = validDocument() + mapOf(
            "shapeType" to "polyline",
            "polylineCoordinates" to listOf(
                mapOf("latitude" to 37.0, "longitude" to 127.0),
                mapOf("latitude" to "37.1", "longitude" to 127.1),
            ),
        )
        val rectangleOutOfRangeCoordinate = validDocument() + mapOf(
            "shapeType" to "rectangle",
            "secondCoordinate" to mapOf("latitude" to 37.0, "longitude" to 181.0),
        )

        assertNull(shapeFromFirestoreData(rectangleMissingSecond))
        assertNull(shapeFromFirestoreData(polygonMissingCoordinates))
        assertNull(shapeFromFirestoreData(polygonBelowMinimum))
        assertNull(shapeFromFirestoreData(polylineMissingCoordinates))
        assertNull(shapeFromFirestoreData(polylineInvalidCoordinate))
        assertNull(shapeFromFirestoreData(rectangleOutOfRangeCoordinate))
    }

    @Test
    fun `Firestore 문서 파싱은 문서 ID 와 id 필드가 다르면 invalid 이다`() {
        assertEquals(
            "00000000-0000-0000-0000-000000000001",
            shapeFromFirestoreDocument(
                documentId = "00000000-0000-0000-0000-000000000001",
                data = validDocument(),
            )?.id,
        )
        assertNull(
            shapeFromFirestoreDocument(
                documentId = "00000000-0000-0000-0000-000000000099",
                data = validDocument(),
            ),
        )
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
