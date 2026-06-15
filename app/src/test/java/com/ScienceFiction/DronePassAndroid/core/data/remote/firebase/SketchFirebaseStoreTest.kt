package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SketchFirebaseStoreTest {

    @Test
    fun `스케치 좌표는 iOS처럼 Firestore 저장 시 소수 6자리로 반올림한다`() {
        assertEquals(37.123457, roundSketchCoordinateForFirestore(37.123456789), 0.0)
        assertEquals(126.987654, roundSketchCoordinateForFirestore(126.987654321), 0.0)
    }

    @Test
    fun `스케치 투명도는 iOS처럼 Firestore 저장 시 소수 2자리로 반올림한다`() {
        assertEquals(0.33, roundSketchOpacityForFirestore(0.3333), 0.0)
        assertEquals(0.68, roundSketchOpacityForFirestore(0.675), 0.0)
    }

    @Test
    fun `Sketch Firestore 쓰기는 iOS 계약처럼 Timestamp와 Double 좌표 map 배열을 사용한다`() {
        val sketch = SketchModel(
            id = "00000000-0000-0000-0000-000000000001",
            points = listOf(Coordinate(37.1234567, 126.9876544)),
            color = "#123abc",
            strokeWidth = 5.0,
            opacity = 0.3333,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            deletedAt = 1_700_000_456_000L,
        )

        val data = sketchToFirestoreDocumentData(sketch)
        val points = data["points"] as List<*>
        val point = points.first() as Map<*, *>

        assertEquals(37.123457, point["latitude"])
        assertEquals(126.987654, point["longitude"])
        assertTrue(point["latitude"] is Double)
        assertTrue(point["longitude"] is Double)
        assertEquals("#123ABC", data["color"])
        assertEquals(0.33, data["opacity"])
        assertTrue(data["createdAt"] is Timestamp)
        assertTrue(data["updatedAt"] is Timestamp)
        assertTrue(data["deletedAt"] is Timestamp)
    }

    @Test
    fun `Sketch Firestore 문서 데이터는 null deletedAt 을 쓰지 않고 merge 쓰기는 delete sentinel 로 정리한다`() {
        val sketch = SketchModel(
            id = "00000000-0000-0000-0000-000000000002",
            points = listOf(Coordinate(37.0, 127.0)),
            color = "#FF0000",
            strokeWidth = 3.0,
            opacity = 1.0,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            deletedAt = null,
        )

        val documentData = sketchToFirestoreDocumentData(sketch)
        val mergeData = sketchToFirestoreMergeData(sketch)

        assertFalse(documentData.containsKey("deletedAt"))
        assertTrue(mergeData["deletedAt"] is FieldValue)
    }

    @Test
    fun `iOS SketchFirebaseStore 와 같이 UUID id 가 있으면 파싱한다`() {
        val sketch = sketchFromFirestoreData(validDocument())

        requireNotNull(sketch)
        assertEquals("00000000-0000-0000-0000-000000000001", sketch.id)
        assertEquals(listOf(Coordinate(37.0, 127.0)), sketch.points)
        assertEquals("#123456", sketch.color)
        assertEquals(5.0, sketch.strokeWidth, 0.0)
        assertEquals(0.5, sketch.opacity, 0.0)
        assertEquals(1_700_000_000_000L, sketch.createdAt)
        assertEquals(1_700_000_123_000L, sketch.updatedAt)
    }

    @Test
    fun `iOS SketchFirebaseStore 와 같이 UUID 로 파싱되지 않는 id 는 invalid 이다`() {
        assertNull(sketchFromFirestoreData(validDocument() + ("id" to "not-a-uuid")))
        assertNull(sketchFromFirestoreData(validDocument() - "id"))
    }

    @Test
    fun `스케치 updatedAt 누락은 iOS처럼 createdAt 으로 fallback 한다`() {
        val sketch = sketchFromFirestoreData(validDocument() - "updatedAt")

        requireNotNull(sketch)
        assertEquals(1_700_000_000_000L, sketch.createdAt)
        assertEquals(1_700_000_000_000L, sketch.updatedAt)
    }

    @Test
    fun `스케치 createdAt 누락은 iOS처럼 현재 시각으로 fallback 한다`() {
        val fallbackNow = 1_800_000_000_000L
        val sketch = sketchFromFirestoreData(
            data = validDocument() - "createdAt",
            nowMillis = fallbackNow,
        )

        requireNotNull(sketch)
        assertEquals(fallbackNow, sketch.createdAt)
        assertEquals(1_700_000_123_000L, sketch.updatedAt)
    }

    @Test
    fun `스케치 createdAt updatedAt 이 모두 누락되면 iOS처럼 둘 다 현재 시각으로 fallback 한다`() {
        val fallbackNow = 1_800_000_000_000L
        val sketch = sketchFromFirestoreData(
            data = validDocument() - "createdAt" - "updatedAt",
            nowMillis = fallbackNow,
        )

        requireNotNull(sketch)
        assertEquals(fallbackNow, sketch.createdAt)
        assertEquals(fallbackNow, sketch.updatedAt)
    }

    @Test
    fun `스케치 선택 필드 누락은 iOS 기본값으로 파싱한다`() {
        val sketch = sketchFromFirestoreData(
            validDocument()
                .minus("points")
                .minus("color")
                .minus("strokeWidth")
                .minus("opacity")
        )

        requireNotNull(sketch)
        assertEquals(emptyList<Coordinate>(), sketch.points)
        assertEquals("#FF0000", sketch.color)
        assertEquals(3.0, sketch.strokeWidth, 0.0)
        assertEquals(1.0, sketch.opacity, 0.0)
    }

    @Test
    fun `스케치 color 문자열이 Firebase 계약을 어기면 iOS처럼 기본 빨강으로 살린다`() {
        assertEquals("#FF0000", sketchFromFirestoreData(validDocument() + ("color" to "red"))?.color)
        assertEquals("#FF0000", sketchFromFirestoreData(validDocument() + ("color" to "#FF000080"))?.color)
    }

    @Test
    fun `스케치 숫자 선택 필드는 iOS처럼 정수 숫자도 Double 로 읽는다`() {
        val sketch = sketchFromFirestoreData(
            validDocument() + mapOf(
                "strokeWidth" to 5,
                "opacity" to 1L,
            ),
        )

        requireNotNull(sketch)
        assertEquals(5.0, sketch.strokeWidth, 0.0)
        assertEquals(1.0, sketch.opacity, 0.0)
    }

    @Test
    fun `스케치 숫자 선택 필드 문자열은 iOS처럼 기본값으로 파싱한다`() {
        val sketch = sketchFromFirestoreData(
            validDocument() + mapOf(
                "strokeWidth" to "5",
                "opacity" to "1.0",
            ),
        )

        requireNotNull(sketch)
        assertEquals(3.0, sketch.strokeWidth, 0.0)
        assertEquals(1.0, sketch.opacity, 0.0)
    }

    @Test
    fun `스케치 숫자 선택 필드가 Firebase 저장 검증 범위를 어기면 invalid 이다`() {
        assertNull(sketchFromFirestoreData(validDocument() + ("strokeWidth" to 0.0)))
        assertNull(sketchFromFirestoreData(validDocument() + ("strokeWidth" to 50.1)))
        assertNull(sketchFromFirestoreData(validDocument() + ("opacity" to -0.1)))
        assertNull(sketchFromFirestoreData(validDocument() + ("opacity" to Double.NaN)))
    }

    @Test
    fun `스케치 points 필드가 배열이 아니면 iOS처럼 빈 배열로 파싱한다`() {
        val sketch = sketchFromFirestoreData(
            validDocument() + ("points" to "not-a-list"),
        )

        requireNotNull(sketch)
        assertEquals(emptyList<Coordinate>(), sketch.points)
    }

    @Test
    fun `스케치 points 배열의 손상된 원소는 iOS처럼 해당 좌표만 제외한다`() {
        val incompletePoint = listOf(
            mapOf("latitude" to 37.0, "longitude" to 127.0),
            mapOf("latitude" to 37.1),
        )

        val sketch = sketchFromFirestoreData(
            validDocument() + ("points" to incompletePoint),
        )

        requireNotNull(sketch)
        assertEquals(listOf(Coordinate(37.0, 127.0)), sketch.points)
    }

    @Test
    fun `스케치 points 좌표 값이 Android 검증을 통과하지 못하면 해당 좌표만 제외한다`() {
        val outOfRangePoint = listOf(mapOf("latitude" to 91.0, "longitude" to 127.0))
        val nonFinitePoint = listOf(mapOf("latitude" to 37.0, "longitude" to Double.NaN))
        val integerPoint = listOf(mapOf("latitude" to 37, "longitude" to 127.0))

        assertEquals(
            emptyList<Coordinate>(),
            sketchFromFirestoreData(validDocument() + ("points" to outOfRangePoint))?.points,
        )
        assertEquals(
            emptyList<Coordinate>(),
            sketchFromFirestoreData(validDocument() + ("points" to nonFinitePoint))?.points,
        )
        assertEquals(
            emptyList<Coordinate>(),
            sketchFromFirestoreData(validDocument() + ("points" to integerPoint))?.points,
        )
    }

    @Test
    fun `Firestore 문서 파싱은 문서 ID 와 id 필드가 다르면 invalid 이다`() {
        assertEquals(
            "00000000-0000-0000-0000-000000000001",
            sketchFromFirestoreDocument(
                documentId = "00000000-0000-0000-0000-000000000001",
                data = validDocument(),
            )?.id,
        )
        assertNull(
            sketchFromFirestoreDocument(
                documentId = "00000000-0000-0000-0000-000000000099",
                data = validDocument(),
            ),
        )
    }

    private fun validDocument(): Map<String, Any?> {
        return mapOf(
            "id" to "00000000-0000-0000-0000-000000000001",
            "points" to listOf(
                mapOf(
                    "latitude" to 37.0,
                    "longitude" to 127.0,
                )
            ),
            "color" to "#123456",
            "strokeWidth" to 5.0,
            "opacity" to 0.5,
            "createdAt" to Timestamp(Date(1_700_000_000_000L)),
            "updatedAt" to Timestamp(Date(1_700_000_123_000L)),
        )
    }
}
