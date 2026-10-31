package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.account.logoutShapesToUpload
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeRepositoryTest {

    @Test
    fun `드론 재할당은 droneId와 함께 색을 옮겨 간 드론 색으로 바꾼다`() {
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
            toDroneColor = "#34C759",
            updatedAt = 10L,
        )

        assertEquals("drone-b", reassigned.droneId)
        assertEquals("#34C759", reassigned.color)
        assertEquals(10L, reassigned.updatedAt)
        // 드론 색을 모르면 도형 색을 그대로 둔다.
        assertEquals("#FF3B30", reassignShapeEntityToDrone(original, "drone-b", null, 10L).color)
    }

    @Test
    fun `레거시 도형 연결은 첫 드론 ID를 채우고 색을 그 드론 색으로 바꾼다`() {
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
            firstDroneColor = "#FF9500",
            updatedAt = 12L,
        )

        assertEquals("drone-first", connected.droneId)
        assertEquals("#FF9500", connected.color)
        assertNull(connected.deletedAt)
        assertEquals(12L, connected.updatedAt)
    }

    @Test
    fun `도형 색 맞춤은 살아 있는 드론을 가리키는 활성 도형만 드론 색으로 바꾸고 updatedAt을 올린다`() {
        val drones = listOf(
            DroneModel(id = "red", name = "Red", color = "#FF3B30", createdAt = 1L, updatedAt = 1L),
            DroneModel(id = "blue", name = "Blue", color = "#007AFF", createdAt = 1L, updatedAt = 1L),
            DroneModel(id = "gone", name = "Gone", color = "#34C759", createdAt = 1L, updatedAt = 1L, deletedAt = 2L),
        )
        val shapes = listOf(
            ShapeModel(id = "mismatch", color = "#007AFF", droneId = "red", updatedAt = 5L),
            ShapeModel(id = "case-only", color = "#007aff", droneId = "blue", updatedAt = 5L),
            ShapeModel(id = "deleted-shape", color = "#007AFF", droneId = "red", updatedAt = 5L, deletedAt = 6L),
            ShapeModel(id = "deleted-drone", color = "#007AFF", droneId = "gone", updatedAt = 5L),
            ShapeModel(id = "no-drone", color = "#FF9500", droneId = null, updatedAt = 5L),
            ShapeModel(id = "unknown-drone", color = "#FF9500", droneId = "missing", updatedAt = 5L),
        )

        val recolored = shapesRecoloredToDrones(shapes, drones, nowMillis = 100L)

        assertEquals(listOf("mismatch"), recolored.map { it.id })
        assertEquals("#FF3B30", recolored.single().color)
        assertEquals(100L, recolored.single().updatedAt)
    }

    @Test
    fun `도형 색 맞춤은 드론이 없으면 아무것도 바꾸지 않는다`() {
        val shapes = listOf(ShapeModel(id = "s", color = "#007AFF", droneId = "red", updatedAt = 5L))

        assertTrue(shapesRecoloredToDrones(shapes, emptyList(), nowMillis = 100L).isEmpty())
    }

    // region D-50 레드팀: 색 맞춤은 서버 값과 병합한 뒤 (iOS TagRedTeamFixTests 정합)

    private val colorDrones = listOf(DroneModel(id = "D1", name = "Drone", color = "#007AFF", createdAt = 1L, updatedAt = 1L))

    private fun colorShape(id: String, title: String, color: String, tagId: String?, updatedAt: Long, deletedAt: Long? = null) =
        ShapeModel(id = id, title = title, color = color, droneId = "D1", tagId = tagId, createdAt = 1L, updatedAt = updatedAt, deletedAt = deletedAt)

    @Test
    fun `관문을 열 때 색 맞춤은 서버의 태그와 제목을 남기고 색만 고친다`() {
        val staleLocal = colorShape("S1", "옛 제목", "#FF3B30", tagId = null, updatedAt = 1_000L)
        val server = colorShape("S1", "새 제목", "#FF3B30", tagId = "T1", updatedAt = 2_000L)

        assertEquals(listOf("S1"), shapeIdsWithDroneColorMismatch(listOf(staleLocal), colorDrones))
        val result = reconcileShapeColorsWithServer(listOf(staleLocal), listOf(server), colorDrones, nowMillis = 9_000L)
        val upload = result.toUpload.single()
        assertEquals("T1", upload.tagId)
        assertEquals("새 제목", upload.title)
        assertEquals("#007AFF", upload.color)
        assertEquals(9_000L, upload.updatedAt)
        assertEquals("기기 사본도 서버 값으로 바뀐다", listOf(upload), result.localWrites)
    }

    @Test
    fun `서버 색이 이미 맞으면 서버 값만 기기에 반영하고 올리지 않는다`() {
        val staleLocal = colorShape("S1", "옛 제목", "#FF3B30", tagId = null, updatedAt = 1_000L)
        val server = colorShape("S1", "새 제목", "#007AFF", tagId = "T1", updatedAt = 2_000L)
        val result = reconcileShapeColorsWithServer(listOf(staleLocal), listOf(server), colorDrones, nowMillis = 9_000L)
        assertTrue(result.toUpload.isEmpty())
        assertEquals(listOf(server), result.localWrites)
    }

    @Test
    fun `기기에서 더 새로 고친 도형은 기기 값에 색을 맞춘다`() {
        val newerLocal = colorShape("S1", "기기에서 고침", "#FF3B30", tagId = "T2", updatedAt = 3_000L)
        val server = colorShape("S1", "서버", "#FF3B30", tagId = "T1", updatedAt = 2_000L)
        val upload = reconcileShapeColorsWithServer(listOf(newerLocal), listOf(server), colorDrones, nowMillis = 9_000L).toUpload.single()
        assertEquals("기기에서 고침", upload.title)
        assertEquals("T2", upload.tagId)
        assertEquals("#007AFF", upload.color)
    }

    @Test
    fun `서버를 읽지 못하면 색만 바꾸고 updatedAt 은 그대로 둔다`() {
        val staleLocal = colorShape("S1", "옛 제목", "#FF3B30", tagId = null, updatedAt = 1_000L)
        val recolored = shapesRecoloredLocallyOnly(listOf(staleLocal), colorDrones).single()
        assertEquals("#007AFF", recolored.color)
        assertEquals(1_000L, recolored.updatedAt)
    }

    @Test
    fun `로그아웃 전 업로드는 기기의 옛 사본을 색이 달라도 올리지 않는다`() {
        val staleLocal = colorShape("S1", "옛 제목", "#FF3B30", tagId = null, updatedAt = 1_000L)
        val server = colorShape("S1", "새 제목", "#007AFF", tagId = "T1", updatedAt = 2_000L)
        assertTrue(logoutShapesToUpload(listOf(staleLocal), listOf(server), colorDrones, nowMillis = 9_000L).isEmpty())
    }

    @Test
    fun `로그아웃 전 업로드는 서버 색이 다르면 서버 값에 색을 고쳐 올린다`() {
        val staleLocal = colorShape("S1", "옛 제목", "#FF3B30", tagId = null, updatedAt = 1_000L)
        val server = colorShape("S1", "새 제목", "#FF3B30", tagId = "T1", updatedAt = 2_000L)
        val upload = logoutShapesToUpload(listOf(staleLocal), listOf(server), colorDrones, nowMillis = 9_000L).single()
        assertEquals("T1", upload.tagId)
        assertEquals("새 제목", upload.title)
        assertEquals("#007AFF", upload.color)
        assertEquals(9_000L, upload.updatedAt)
    }

    @Test
    fun `로그아웃 전 업로드는 더 새로운 도형과 기기에만 있는 도형을 올리고 서버에 없는 삭제 표시는 올리지 않는다`() {
        val newerLocal = colorShape("N", "기기에서 고침", "#FF3B30", tagId = "T2", updatedAt = 3_000L)
        val server = colorShape("N", "서버", "#FF3B30", tagId = "T1", updatedAt = 2_000L)
        val localOnly = colorShape("L", "기기에만", "#007AFF", tagId = null, updatedAt = 1_000L)
        val deletedOnly = colorShape("X", "지움", "#007AFF", tagId = null, updatedAt = 1_000L, deletedAt = 1_000L)

        val upload = logoutShapesToUpload(listOf(newerLocal, localOnly, deletedOnly), listOf(server), colorDrones, nowMillis = 9_000L)
            .associateBy { it.id }
        assertEquals(setOf("N", "L"), upload.keys)
        assertEquals("기기에서 고침", upload.getValue("N").title)
        assertEquals("T2", upload.getValue("N").tagId)
        assertEquals("#007AFF", upload.getValue("N").color)
        assertEquals("기기에만", upload.getValue("L").title)
    }

    // endregion

    @Test
    fun `만료 도형 삭제는 종료일이 현재 이하인 활성 항목을 같은 시각으로 소프트 삭제한다`() {
        val now = 10_000L
        val expired = shapeEntity(
            id = "expired",
            flightEndDate = now - 1,
        )
        val active = shapeEntity(
            id = "active",
            flightEndDate = now + 1,
        )
        val exactlyNow = shapeEntity(
            id = "exactly-now",
            flightEndDate = now,
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
            shapes = listOf(expired, active, exactlyNow, deletedExpired, noEndDate),
            now = now,
        )

        assertEquals(listOf("expired", "exactly-now"), deletedShapes.map { it.id })
        assertEquals(listOf(now, now), deletedShapes.map { it.deletedAt })
        assertEquals(listOf(now, now), deletedShapes.map { it.updatedAt })
        assertEquals(expired.color, deletedShapes.first().color)
        assertEquals(expired.droneId, deletedShapes.first().droneId)
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
