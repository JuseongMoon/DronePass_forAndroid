package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShapeRepositoryTest {

    @Test
    fun `드론 재할당은 iOS처럼 도형 색상을 보존하고 droneId만 변경한다`() {
        val original = ShapeEntity(
            id = "shape-1",
            title = "Area",
            shapeType = "circle",
            baseLatitude = 37.0,
            baseLongitude = 127.0,
            address = null,
            radius = 100.0,
            secondLatitude = null,
            secondLongitude = null,
            polygonCoordinates = null,
            polylineCoordinates = null,
            height = null,
            memo = null,
            color = "#FF3B30",
            droneId = "drone-a",
            createdAt = 1L,
            deletedAt = null,
            flightStartDate = 2L,
            flightEndDate = 3L,
            updatedAt = 4L,
        )

        val reassigned = reassignShapeEntityToDrone(
            shape = original,
            toDroneId = "drone-b",
            updatedAt = 10L,
        )

        assertEquals("drone-b", reassigned.droneId)
        assertEquals("#FF3B30", reassigned.color)
        assertEquals(10L, reassigned.updatedAt)
    }

    @Test
    fun `레거시 도형 연결은 iOS처럼 첫 드론 ID를 채우고 색상은 보존한다`() {
        val original = ShapeEntity(
            id = "shape-legacy",
            title = "Legacy Area",
            shapeType = "circle",
            baseLatitude = 37.0,
            baseLongitude = 127.0,
            address = null,
            radius = 100.0,
            secondLatitude = null,
            secondLongitude = null,
            polygonCoordinates = null,
            polylineCoordinates = null,
            height = null,
            memo = null,
            color = "#34C759",
            droneId = null,
            createdAt = 1L,
            deletedAt = null,
            flightStartDate = 2L,
            flightEndDate = 3L,
            updatedAt = 4L,
        )

        val connected = connectLegacyShapeEntityToDrone(
            shape = original,
            firstDroneId = "drone-first",
            updatedAt = 12L,
        )

        assertEquals("drone-first", connected.droneId)
        assertEquals("#34C759", connected.color)
        assertNull(connected.deletedAt)
        assertEquals(12L, connected.updatedAt)
    }

    @Test
    fun `만료 도형 삭제는 활성 도형 중 종료일이 지난 항목만 같은 시각으로 소프트 삭제한다`() {
        val now = 10_000L
        val expired = shapeEntity(
            id = "expired",
            flightEndDate = now - 1,
        )
        val active = shapeEntity(
            id = "active",
            flightEndDate = now + 1,
        )
        val deletedExpired = shapeEntity(
            id = "deleted-expired",
            deletedAt = now - 5_000,
            flightEndDate = now - 1,
        )
        val noEndDate = shapeEntity(
            id = "no-end",
            flightEndDate = null,
        )

        val deletedShapes = softDeleteExpiredShapeEntities(
            shapes = listOf(expired, active, deletedExpired, noEndDate),
            now = now,
        )

        assertEquals(listOf("expired"), deletedShapes.map { it.id })
        assertEquals(now, deletedShapes.single().deletedAt)
        assertEquals(now, deletedShapes.single().updatedAt)
        assertEquals(expired.color, deletedShapes.single().color)
        assertEquals(expired.droneId, deletedShapes.single().droneId)
    }

    private fun shapeEntity(
        id: String,
        deletedAt: Long? = null,
        flightEndDate: Long? = 3L,
    ): ShapeEntity {
        return ShapeEntity(
            id = id,
            title = "Area",
            shapeType = "circle",
            baseLatitude = 37.0,
            baseLongitude = 127.0,
            address = null,
            radius = 100.0,
            secondLatitude = null,
            secondLongitude = null,
            polygonCoordinates = null,
            polylineCoordinates = null,
            height = null,
            memo = null,
            color = "#FF3B30",
            droneId = "drone-a",
            createdAt = 1L,
            deletedAt = deletedAt,
            flightStartDate = 2L,
            flightEndDate = flightEndDate,
            updatedAt = 4L,
        )
    }
}
