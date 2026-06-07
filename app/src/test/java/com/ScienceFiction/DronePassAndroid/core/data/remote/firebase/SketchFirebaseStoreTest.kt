package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
