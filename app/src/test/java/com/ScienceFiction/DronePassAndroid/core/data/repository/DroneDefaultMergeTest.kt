package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 로그인 병합에서 기기가 만든 손대지 않은 기본 드론 정리(사용자 승인 2026-10-02). */
class DroneDefaultMergeTest {

    private fun defaultDrone(id: String, createdAt: Long = 1_000L, name: String = "내 드론") = DroneModel(
        id = id,
        name = name,
        color = "#007AFF",
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    fun `untouched default drone needs default name, color, empty fields and no edits`() {
        assertTrue(isUntouchedDefaultDrone(defaultDrone("a")))
        assertTrue(isUntouchedDefaultDrone(defaultDrone("a", name = "My Drone")))
        assertTrue(isUntouchedDefaultDrone(defaultDrone("a").copy(color = "#007aff", memo = "")))
        assertTrue(isUntouchedDefaultDrone(defaultDrone("a").copy(updatedAt = 2_000L)))

        assertFalse(isUntouchedDefaultDrone(defaultDrone("a").copy(updatedAt = 2_001L)))
        assertFalse(isUntouchedDefaultDrone(defaultDrone("a").copy(name = "드론 2")))
        assertFalse(isUntouchedDefaultDrone(defaultDrone("a").copy(color = "#FF9500")))
        assertFalse(isUntouchedDefaultDrone(defaultDrone("a").copy(serialNumber = "SN1")))
        assertFalse(isUntouchedDefaultDrone(defaultDrone("a").copy(memo = "메모")))
        assertFalse(isUntouchedDefaultDrone(defaultDrone("a").copy(deletedAt = 3_000L)))
    }

    @Test
    fun `local default drone moves its shapes to the account default drone and is dropped`() {
        val local = defaultDrone("local", createdAt = 5_000L)
        val serverLater = defaultDrone("server-b", createdAt = 2_000L)
        val serverEarlier = defaultDrone("server-a", createdAt = 1_000L)

        val result = mergeDronesForFullSync(
            localDrones = listOf(local),
            serverDrones = listOf(serverLater, serverEarlier),
            localShapeDroneIds = listOf("local", "local", null),
        )

        assertEquals(setOf("server-a", "server-b"), result.merged.map { it.id }.toSet())
        assertTrue(result.toUpload.isEmpty())
        assertEquals(setOf("local"), result.discardedLocalDroneIds)
        // 서버 기본 드론이 여럿이면 createdAt 이 가장 이른 것
        assertEquals(mapOf("local" to "server-a"), result.reassignedShapeDroneIds)
        assertEquals(mapOf("local" to "server-a"), result.selectionReplacements)
    }

    @Test
    fun `ties on createdAt pick the smallest id`() {
        val result = mergeDronesForFullSync(
            localDrones = listOf(defaultDrone("local")),
            serverDrones = listOf(defaultDrone("z-server"), defaultDrone("a-server")),
            localShapeDroneIds = listOf("local"),
        )

        assertEquals(mapOf("local" to "a-server"), result.reassignedShapeDroneIds)
    }

    @Test
    fun `without a server default drone only an unreferenced local default drone is dropped`() {
        val named = DroneModel(id = "server-named", name = "매빅 3", color = "#FF9500", createdAt = 1_000L, updatedAt = 9_000L)

        val unreferenced = mergeDronesForFullSync(listOf(defaultDrone("local")), listOf(named), emptyList())
        assertEquals(setOf("local"), unreferenced.discardedLocalDroneIds)
        assertTrue(unreferenced.reassignedShapeDroneIds.isEmpty())
        assertEquals(mapOf("local" to "server-named"), unreferenced.selectionReplacements)

        val referenced = mergeDronesForFullSync(listOf(defaultDrone("local")), listOf(named), listOf("local"))
        assertTrue(referenced.discardedLocalDroneIds.isEmpty())
        assertEquals(listOf("local"), referenced.toUpload.map { it.id })
    }

    @Test
    fun `without any live server drone the local default drone is uploaded as before`() {
        val deletedServer = defaultDrone("server").copy(deletedAt = 9_000L, updatedAt = 9_000L)

        val result = mergeDronesForFullSync(listOf(defaultDrone("local")), listOf(deletedServer), listOf("local"))

        assertTrue(result.discardedLocalDroneIds.isEmpty())
        assertEquals(listOf("local"), result.toUpload.map { it.id })
    }

    @Test
    fun `edited local drones and drones already on the server are never dropped`() {
        val editedLocal = defaultDrone("local").copy(updatedAt = 9_000L)
        val alsoOnServer = defaultDrone("shared")

        val result = mergeDronesForFullSync(
            localDrones = listOf(editedLocal, alsoOnServer),
            serverDrones = listOf(defaultDrone("server"), alsoOnServer),
            localShapeDroneIds = emptyList(),
        )

        assertTrue(result.discardedLocalDroneIds.isEmpty())
        assertEquals(setOf("local", "shared", "server"), result.merged.map { it.id }.toSet())
    }

    @Test
    fun `selection follows the replacement drone`() {
        val state = DroneSelectionState()
        state.onPersistedSelectionLoaded(
            com.ScienceFiction.DronePassAndroid.feature.drone.StoredDroneSelection(
                selectedDroneId = "local",
                selectedDroneIds = setOf("local", "other"),
            ),
        )

        state.replaceDrones(mapOf("local" to "server-a"))

        assertEquals(setOf("server-a", "other"), state.selectedDroneIds.value)
        assertEquals("server-a", state.selectedDroneId.value)
    }
}
