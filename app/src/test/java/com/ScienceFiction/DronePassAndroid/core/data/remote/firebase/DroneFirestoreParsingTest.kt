package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class DroneFirestoreParsingTest {

    @Test
    fun `iOS DroneFirebaseStore 와 같이 필수 필드가 모두 있으면 파싱한다`() {
        val drone = droneFromFirestoreData(validDocument())

        requireNotNull(drone)
        assertEquals("drone-1", drone.id)
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

    private fun validDocument(): Map<String, Any?> {
        return mapOf(
            "id" to "drone-1",
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
