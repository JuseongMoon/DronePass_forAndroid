package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneSyncMergeTest {

    @Test
    fun `첫 동기화의 서버 누락 로컬 드론은 iOS 업로드 대상처럼 유지한다`() {
        val local = drone(id = "local-only", updatedAt = 20L)

        val result = mergeDronesForFullSync(
            localDrones = listOf(local),
            serverDrones = emptyList(),
            lastSyncTime = null,
        )

        assertEquals(listOf("local-only"), result.merged.map { it.id })
        assertEquals(listOf("local-only"), result.toUpload.map { it.id })
    }

    @Test
    fun `이전 동기화 이후 서버에서 사라진 로컬 드론은 원격 삭제로 보고 되살리지 않는다`() {
        val local = drone(id = "ios-deleted", updatedAt = 20L)

        val result = mergeDronesForFullSync(
            localDrones = listOf(local),
            serverDrones = emptyList(),
            lastSyncTime = 30L,
        )

        assertEquals(emptyList<DroneModel>(), result.merged)
        assertEquals(emptyList<DroneModel>(), result.toUpload)
    }

    @Test
    fun `서버에만 있는 드론은 유지하고 업로드하지 않는다`() {
        val server = drone(id = "server-only", updatedAt = 20L)

        val result = mergeDronesForFullSync(
            localDrones = emptyList(),
            serverDrones = listOf(server),
            lastSyncTime = 30L,
        )

        assertEquals(listOf("server-only"), result.merged.map { it.id })
        assertEquals(emptyList<DroneModel>(), result.toUpload)
    }

    @Test
    fun `마지막 동기화 이후 수정된 로컬 드론은 서버에 없어도 업로드한다`() {
        val local = drone(id = "offline-created", updatedAt = 40L)

        val result = mergeDronesForFullSync(
            localDrones = listOf(local),
            serverDrones = emptyList(),
            lastSyncTime = 30L,
        )

        assertEquals(listOf("offline-created"), result.merged.map { it.id })
        assertEquals(listOf("offline-created"), result.toUpload.map { it.id })
    }

    @Test
    fun `서버에 있는 드론은 기존 LWW 처럼 더 최신 값을 채택한다`() {
        val localWinner = drone(id = "local-winner", updatedAt = 40L, name = "Local")
        val localWinnerServer = drone(id = "local-winner", updatedAt = 30L, name = "Old server")
        val serverWinnerLocal = drone(id = "server-winner", updatedAt = 10L, name = "Old local")
        val serverWinner = drone(id = "server-winner", updatedAt = 20L, name = "Server")

        val result = mergeDronesForFullSync(
            localDrones = listOf(localWinner, serverWinnerLocal),
            serverDrones = listOf(localWinnerServer, serverWinner),
            lastSyncTime = 30L,
        )

        assertEquals(
            mapOf(
                "local-winner" to "Local",
                "server-winner" to "Server",
            ),
            result.merged.associate { it.id to it.name },
        )
        assertEquals(listOf("local-winner"), result.toUpload.map { it.id })
    }

    @Test
    fun `서버에 없는 로컬 드론 유지 여부는 마지막 동기화 시각으로 판단한다`() {
        assertTrue(shouldKeepLocalDroneMissingOnServer(localDroneUpdatedAt = 40L, lastSyncTime = 30L))
        assertTrue(shouldKeepLocalDroneMissingOnServer(localDroneUpdatedAt = 40L, lastSyncTime = null))
        assertFalse(shouldKeepLocalDroneMissingOnServer(localDroneUpdatedAt = 30L, lastSyncTime = 30L))
    }

    private fun drone(
        id: String,
        updatedAt: Long,
        name: String = id,
    ): DroneModel {
        return DroneModel(
            id = id,
            name = name,
            color = "#007AFF",
            createdAt = 1L,
            updatedAt = updatedAt,
        )
    }
}
