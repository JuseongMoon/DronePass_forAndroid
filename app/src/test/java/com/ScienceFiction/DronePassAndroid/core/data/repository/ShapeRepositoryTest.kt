package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `첫 동기화의 서버 누락 로컬 도형은 업로드 대상으로 유지한다`() {
        val local = shapeModel(id = "local-only", updatedAt = 20L)

        val result = mergeShapesForFullSync(
            localShapes = listOf(local),
            serverShapes = emptyList(),
            lastSyncTime = null,
        )

        assertEquals(listOf("local-only"), result.merged.map { it.id })
        assertEquals(listOf("local-only"), result.toUpload.map { it.id })
    }

    @Test
    fun `이전 동기화 이후 서버에서 사라진 로컬 도형은 원격 삭제로 보고 되살리지 않는다`() {
        val local = shapeModel(id = "ios-deleted", updatedAt = 20L)

        val result = mergeShapesForFullSync(
            localShapes = listOf(local),
            serverShapes = emptyList(),
            lastSyncTime = 30L,
        )

        assertEquals(emptyList<ShapeModel>(), result.merged)
        assertEquals(emptyList<ShapeModel>(), result.toUpload)
    }

    @Test
    fun `서버에만 있는 도형은 유지하고 업로드하지 않는다`() {
        val server = shapeModel(id = "server-only", updatedAt = 20L)

        val result = mergeShapesForFullSync(
            localShapes = emptyList(),
            serverShapes = listOf(server),
            lastSyncTime = 30L,
        )

        assertEquals(listOf("server-only"), result.merged.map { it.id })
        assertEquals(emptyList<ShapeModel>(), result.toUpload)
    }

    @Test
    fun `마지막 동기화 이후 수정된 로컬 도형은 서버에 없어도 업로드한다`() {
        val local = shapeModel(id = "offline-created", updatedAt = 40L)

        val result = mergeShapesForFullSync(
            localShapes = listOf(local),
            serverShapes = emptyList(),
            lastSyncTime = 30L,
        )

        assertEquals(listOf("offline-created"), result.merged.map { it.id })
        assertEquals(listOf("offline-created"), result.toUpload.map { it.id })
    }

    @Test
    fun `서버에 있는 도형은 기존 LWW처럼 더 최신 값을 채택한다`() {
        val localWinner = shapeModel(id = "local-winner", updatedAt = 40L, title = "Local")
        val localWinnerServer = shapeModel(id = "local-winner", updatedAt = 30L, title = "Old server")
        val serverWinnerLocal = shapeModel(id = "server-winner", updatedAt = 10L, title = "Old local")
        val serverWinner = shapeModel(id = "server-winner", updatedAt = 20L, title = "Server")

        val result = mergeShapesForFullSync(
            localShapes = listOf(localWinner, serverWinnerLocal),
            serverShapes = listOf(localWinnerServer, serverWinner),
            lastSyncTime = 30L,
        )

        assertEquals(
            mapOf(
                "local-winner" to "Local",
                "server-winner" to "Server",
            ),
            result.merged.associate { it.id to it.title },
        )
        assertEquals(listOf("local-winner"), result.toUpload.map { it.id })
    }

    @Test
    fun `서버에 없는 로컬 도형 유지 여부는 마지막 동기화 시각으로 판단한다`() {
        assertTrue(shouldKeepLocalShapeMissingOnServer(localShapeUpdatedAt = 40L, lastSyncTime = 30L))
        assertTrue(shouldKeepLocalShapeMissingOnServer(localShapeUpdatedAt = 40L, lastSyncTime = null))
        assertFalse(shouldKeepLocalShapeMissingOnServer(localShapeUpdatedAt = 30L, lastSyncTime = 30L))
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

    private fun shapeModel(
        id: String,
        updatedAt: Long,
        title: String = id,
    ): ShapeModel {
        return ShapeModel(
            id = id,
            title = title,
            radius = 100.0,
            color = "#007AFF",
            createdAt = 1L,
            flightStartDate = 2L,
            flightEndDate = 3L,
            updatedAt = updatedAt,
        )
    }
}
