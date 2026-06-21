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
            shape(id = "shape-deleted", shapeType = ShapeType.CIRCLE, deletedAt = 1_700_000_020_000L),
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
    fun `탈퇴 익명화 도형 목록은 iOS ShapeFileStore처럼 삭제 도형을 제외한다`() {
        val shapes = listOf(
            shape(id = "active", shapeType = ShapeType.CIRCLE),
            shape(id = "deleted", shapeType = ShapeType.POLYGON, deletedAt = 1_700_000_020_000L),
        )

        assertEquals(listOf("active"), shapesForAnonymizedDeletion(shapes).map { it.id })
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
    fun `도형 익명 데이터는 iOS AnalyticsDataGenerator 의 선택 필드를 모두 보존한다`() {
        val data = shapeToAnonymizedData(
            shape(
                id = "shape-full",
                shapeType = ShapeType.POLYGON,
                deletedAt = 1_700_000_030_000L,
            ).copy(
                memo = "memo",
                address = "address",
                flightEndDate = 1_700_000_040_000L,
                droneId = "drone-full",
                radius = 120.0,
                secondCoordinate = Coordinate(37.3, 127.3),
                polygonCoordinates = listOf(Coordinate(37.4, 127.4), Coordinate(37.5, 127.5)),
                polylineCoordinates = listOf(Coordinate(37.6, 127.6), Coordinate(37.7, 127.7)),
            ),
        )

        assertEquals("shape-full", data["id"])
        assertEquals("Shape shape-full", data["title"])
        assertEquals("polygon", data["shapeType"])
        assertEquals(mapOf("latitude" to 37.0, "longitude" to 127.0), data["baseCoordinate"])
        assertEquals("memo", data["memo"])
        assertEquals("address", data["address"])
        assertEquals("#007AFF", data["color"])
        assertEquals("drone-full", data["droneId"])
        assertEquals(120.0, data["radius"])
        assertEquals(mapOf("latitude" to 37.3, "longitude" to 127.3), data["secondCoordinate"])
        assertEquals(
            listOf(
                mapOf("latitude" to 37.4, "longitude" to 127.4),
                mapOf("latitude" to 37.5, "longitude" to 127.5),
            ),
            data["polygonCoordinates"],
        )
        assertEquals(
            listOf(
                mapOf("latitude" to 37.6, "longitude" to 127.6),
                mapOf("latitude" to 37.7, "longitude" to 127.7),
            ),
            data["polylineCoordinates"],
        )
        assertEquals(1_700_000_000_000L, timestampMillis(data, "createdAt"))
        assertEquals(1_700_000_001_000L, timestampMillis(data, "flightStartDate"))
        assertEquals(1_700_000_002_000L, timestampMillis(data, "updatedAt"))
        assertEquals(1_700_000_030_000L, timestampMillis(data, "deletedAt"))
        assertEquals(1_700_000_040_000L, timestampMillis(data, "flightEndDate"))
        assertFalse(data.containsKey("height"))
    }

    @Test
    fun `드론 익명 데이터는 삭제 상태를 포함한다`() {
        val data = droneToAnonymizedData(drone(id = "drone-1", deletedAt = 1_700_000_020_000L))

        assertEquals("drone-1", data["id"])
        assertEquals(true, data["isDeleted"])
        assertEquals(1_700_000_020_000L, (data["deletedAt"] as Timestamp).toDate().time)
    }

    @Test
    fun `드론 익명 데이터는 iOS AnalyticsDataGenerator 와 같은 필수 필드만 쓴다`() {
        val data = droneToAnonymizedData(drone(id = "drone-active", deletedAt = null))

        assertEquals("drone-active", data["id"])
        assertEquals("Drone drone-active", data["name"])
        assertEquals("#007AFF", data["color"])
        assertEquals(false, data["isDeleted"])
        assertEquals(1_700_000_000_000L, timestampMillis(data, "createdAt"))
        assertEquals(1_700_000_002_000L, timestampMillis(data, "updatedAt"))
        assertFalse(data.containsKey("deletedAt"))
        assertFalse(data.containsKey("serialNumber"))
        assertFalse(data.containsKey("takeoffWeight"))
        assertFalse(data.containsKey("size"))
        assertFalse(data.containsKey("memo"))
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

    private fun timestampMillis(data: Map<String, Any>, key: String): Long {
        return (data[key] as Timestamp).toDate().time
    }
}
