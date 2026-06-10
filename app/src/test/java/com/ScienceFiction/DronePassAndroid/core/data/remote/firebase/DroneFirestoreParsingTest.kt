package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class DroneFirestoreParsingTest {

    @Test
    fun `Drone Firestore 쓰기는 iOS 계약처럼 날짜를 Timestamp로 저장한다`() {
        val drone = DroneModel(
            id = "00000000-0000-0000-0000-000000000001",
            name = "Drone",
            color = "#007aff",
            serialNumber = "SN-1",
            takeoffWeight = "249g",
            size = "140x140x55mm",
            memo = "memo",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_123_000L,
            deletedAt = 1_700_000_456_000L,
        )

        val data = droneToFirestoreDocumentData(drone)

        assertEquals("00000000-0000-0000-0000-000000000001", data["id"])
        assertEquals("#007AFF", data["color"])
        assertTrue(data["createdAt"] is Timestamp)
        assertTrue(data["updatedAt"] is Timestamp)
        assertTrue(data["deletedAt"] is Timestamp)
    }

    @Test
    fun `iOS DroneFirebaseStore 와 같이 필수 필드가 모두 있으면 파싱한다`() {
        val drone = droneFromFirestoreData(validDocument())

        requireNotNull(drone)
        assertEquals("00000000-0000-0000-0000-000000000001", drone.id)
        assertEquals("Drone", drone.name)
        assertEquals("#007AFF", drone.color)
        assertEquals("SN-1", drone.serialNumber)
        assertEquals(1_700_000_000_000L, drone.createdAt)
        assertEquals(1_700_000_123_000L, drone.updatedAt)
    }

    @Test
    fun `iOS DroneFirebaseStore 와 같이 name color createdAt updatedAt 누락은 invalid 이다`() {
        assertNull(droneFromFirestoreData(validDocument() - "id"))
        assertNull(droneFromFirestoreData(validDocument() - "name"))
        assertNull(droneFromFirestoreData(validDocument() - "color"))
        assertNull(droneFromFirestoreData(validDocument() - "createdAt"))
        assertNull(droneFromFirestoreData(validDocument() - "updatedAt"))
    }

    @Test
    fun `iOS DroneFirebaseStore 와 같이 UUID 로 파싱되지 않는 id 는 invalid 이다`() {
        assertNull(droneFromFirestoreData(validDocument() + ("id" to "not-a-uuid")))
    }

    @Test
    fun `Firestore 파싱은 Firebase 계약을 어긴 이름과 색상을 invalid 로 본다`() {
        assertNull(droneFromFirestoreData(validDocument() + ("name" to "   ")))
        assertNull(droneFromFirestoreData(validDocument() + ("color" to "blue")))
        assertNull(droneFromFirestoreData(validDocument() + ("color" to "#007AFFCC")))
    }

    @Test
    fun `선택 필드가 누락되어도 iOS처럼 null 로 파싱한다`() {
        val drone = droneFromFirestoreData(
            validDocument()
                .minus("serialNumber")
                .minus("takeoffWeight")
                .minus("size")
                .minus("memo")
        )

        requireNotNull(drone)
        assertNull(drone.serialNumber)
        assertNull(drone.takeoffWeight)
        assertNull(drone.size)
        assertNull(drone.memo)
    }

    @Test
    fun `Firestore 문서 파싱은 문서 ID 와 id 필드가 다르면 invalid 이다`() {
        assertEquals(
            "00000000-0000-0000-0000-000000000001",
            droneFromFirestoreDocument(
                documentId = "00000000-0000-0000-0000-000000000001",
                data = validDocument(),
            )?.id,
        )
        assertNull(
            droneFromFirestoreDocument(
                documentId = "00000000-0000-0000-0000-000000000099",
                data = validDocument(),
            ),
        )
    }

    private fun validDocument(): Map<String, Any?> {
        return mapOf(
            "id" to "00000000-0000-0000-0000-000000000001",
            "name" to "Drone",
            "color" to "#007AFF",
            "serialNumber" to "SN-1",
            "takeoffWeight" to "249g",
            "size" to "140x140x55mm",
            "memo" to "memo",
            "createdAt" to Timestamp(Date(1_700_000_000_000L)),
            "updatedAt" to Timestamp(Date(1_700_000_123_000L)),
        )
    }
}
