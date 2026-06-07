package com.ScienceFiction.DronePassAndroid.feature.profile

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnonymizedDeletionDataTest {

    @Test
    fun `익명 사용자 통계는 iOS AnalyticsDataGenerator 와 같은 주요 필드를 만든다`() {
        val shapes = listOf(
            shape(id = "shape-1", shapeType = ShapeType.CIRCLE),
            shape(id = "shape-2", shapeType = ShapeType.POLYGON),
            shape(id = "shape-3", shapeType = ShapeType.POLYGON),
        )
        val drones = listOf(
            drone(id = "drone-active", deletedAt = null),
            drone(id = "drone-deleted", deletedAt = 1_700_000_010_000L),
        )

        val data = buildAnonymizedUserData(
            shapes = shapes,
            drones = drones,
            cloudSyncEnabled = true,
            deletedAtMillis = 1_700_000_000_000L,
            accountCreatedAtMillis = 1_600_000_000_000L,
        )

        assertEquals(3, data["totalShapesCreated"])
        assertEquals(1, data["totalDronesUsed"])
        assertEquals(true, data["cloudSyncEnabled"])
        assertEquals("Android", data["devicePlatform"])
        assertEquals(mapOf("circle" to 1, "polygon" to 2), data["shapeTypeDistribution"])
        assertEquals(1_700_000_000_000L, (data["deletedAt"] as Timestamp).toDate().time)
        assertEquals(1_600_000_000_000L, (data["accountCreatedAt"] as Timestamp).toDate().time)
    }

    @Test
    fun `도형 익명 데이터는 iOS 와 같은 필드명과 raw shapeType 을 사용한다`() {
        val data = shapeToAnonymizedData(
            shape(
                id = "shape-1",
                shapeType = ShapeType.POLYLINE,
                deletedAt = 1_700_000_030_000L,
            ).copy(
                address = null,
                memo = null,
                radius = null,
                droneId = "drone-1",
                polylineCoordinates = listOf(Coordinate(37.1, 127.1), Coordinate(37.2, 127.2)),
            )
        )

        assertEquals("shape-1", data["id"])
        assertEquals("polyline", data["shapeType"])
        assertEquals("", data["address"])
        assertEquals("", data["memo"])
        assertEquals("drone-1", data["droneId"])
        assertFalse(data.containsKey("height"))
        assertFalse(data.containsKey("radius"))
        assertTrue(data["polylineCoordinates"] is List<*>)
    }

    @Test
    fun `드론 익명 데이터는 삭제 상태를 포함한다`() {
        val data = droneToAnonymizedData(drone(id = "drone-1", deletedAt = 1_700_000_020_000L))

        assertEquals("drone-1", data["id"])
        assertEquals(true, data["isDeleted"])
        assertEquals(1_700_000_020_000L, (data["deletedAt"] as Timestamp).toDate().time)
    }

    private fun shape(
        id: String,
        shapeType: ShapeType,
        deletedAt: Long? = null,
    ): ShapeModel {
        return ShapeModel(
            id = id,
            title = "Shape $id",
            shapeType = shapeType,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 100.0,
            createdAt = 1_700_000_000_000L,
            flightStartDate = 1_700_000_001_000L,
            updatedAt = 1_700_000_002_000L,
            deletedAt = deletedAt,
        )
    }

    private fun drone(id: String, deletedAt: Long?): DroneModel {
        return DroneModel(
            id = id,
            name = "Drone $id",
            color = "#007AFF",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_002_000L,
            deletedAt = deletedAt,
        )
    }
}
