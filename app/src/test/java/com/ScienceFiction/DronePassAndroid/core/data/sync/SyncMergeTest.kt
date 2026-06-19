package com.ScienceFiction.DronePassAndroid.core.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    @Test
    fun `local only item is kept and uploaded`() {
        val local = syncItem(id = "local-only", updatedAt = 10L, value = "local")

        val result = merge(listOf(local), emptyList())

        assertEquals(mapOf("local-only" to "local"), result.merged.valuesById())
        assertEquals(listOf("local-only"), result.toUpload.map { it.id })
    }

    @Test
    fun `server only item is kept without upload`() {
        val server = syncItem(id = "server-only", updatedAt = 10L, value = "server")

        val result = merge(emptyList(), listOf(server))

        assertEquals(mapOf("server-only" to "server"), result.merged.valuesById())
        assertEquals(emptyList<SyncItem>(), result.toUpload)
    }

    @Test
    fun `newer local item wins and is uploaded`() {
        val local = syncItem(id = "shared", updatedAt = 20L, value = "local")
        val server = syncItem(id = "shared", updatedAt = 10L, value = "server")

        val result = merge(listOf(local), listOf(server))

        assertEquals(mapOf("shared" to "local"), result.merged.valuesById())
        assertEquals(listOf(local), result.toUpload)
    }

    @Test
    fun `newer server item wins without upload`() {
        val local = syncItem(id = "shared", updatedAt = 10L, value = "local")
        val server = syncItem(id = "shared", updatedAt = 20L, value = "server")

        val result = merge(listOf(local), listOf(server))

        assertEquals(mapOf("shared" to "server"), result.merged.valuesById())
        assertEquals(emptyList<SyncItem>(), result.toUpload)
    }

    @Test
    fun `same timestamp prefers server like iOS LWW tie handling`() {
        val local = syncItem(id = "shared", updatedAt = 10L, value = "local")
        val server = syncItem(id = "shared", updatedAt = 10L, value = "server")

        val result = merge(listOf(local), listOf(server))

        assertEquals(mapOf("shared" to "server"), result.merged.valuesById())
        assertEquals(emptyList<SyncItem>(), result.toUpload)
    }

    @Test
    fun `mixed merge keeps union and uploads only local winners`() {
        val localOnly = syncItem(id = "local-only", updatedAt = 10L, value = "local-only")
        val localWinner = syncItem(id = "local-winner", updatedAt = 30L, value = "local-winner")
        val serverWinnerLocal = syncItem(id = "server-winner", updatedAt = 10L, value = "old-local")
        val serverWinner = syncItem(id = "server-winner", updatedAt = 20L, value = "server-winner")
        val localWinnerServer = syncItem(id = "local-winner", updatedAt = 20L, value = "old-server")
        val serverOnly = syncItem(id = "server-only", updatedAt = 10L, value = "server-only")

        val result = merge(
            local = listOf(localOnly, localWinner, serverWinnerLocal),
            server = listOf(serverWinner, localWinnerServer, serverOnly),
        )

        assertEquals(
            mapOf(
                "server-winner" to "server-winner",
                "local-winner" to "local-winner",
                "server-only" to "server-only",
                "local-only" to "local-only",
            ),
            result.merged.valuesById(),
        )
        assertEquals(listOf("local-only", "local-winner"), result.toUpload.map { it.id }.sorted())
    }

    @Test
    fun `newer local soft delete wins and is uploaded`() {
        val local = syncItem(id = "deleted", updatedAt = 20L, value = "local-delete", deleted = true)
        val server = syncItem(id = "deleted", updatedAt = 10L, value = "server-active", deleted = false)

        val result = merge(listOf(local), listOf(server))

        assertEquals(true, result.merged.single().deleted)
        assertEquals(listOf(local), result.toUpload)
    }

    @Test
    fun `newer server soft delete wins without upload`() {
        val local = syncItem(id = "deleted", updatedAt = 10L, value = "local-active", deleted = false)
        val server = syncItem(id = "deleted", updatedAt = 20L, value = "server-delete", deleted = true)

        val result = merge(listOf(local), listOf(server))

        assertEquals(true, result.merged.single().deleted)
        assertEquals(emptyList<SyncItem>(), result.toUpload)
    }

    @Test
    fun `filterServerNewer applies server only newer and tie items`() {
        val local = listOf(
            syncItem(id = "server-older", updatedAt = 20L, value = "local-new"),
            syncItem(id = "server-newer", updatedAt = 10L, value = "local-old"),
            syncItem(id = "same-time", updatedAt = 10L, value = "local"),
        )
        val server = listOf(
            syncItem(id = "server-older", updatedAt = 10L, value = "server-old"),
            syncItem(id = "server-newer", updatedAt = 20L, value = "server-new"),
            syncItem(id = "same-time", updatedAt = 10L, value = "server"),
            syncItem(id = "server-only", updatedAt = 10L, value = "server-only"),
        )

        val toApply = filterServerNewer(
            local = local,
            server = server,
            idOf = { it.id },
            updatedAtOf = { it.updatedAt },
        )

        assertEquals(
            mapOf(
                "server-newer" to "server-new",
                "same-time" to "server",
                "server-only" to "server-only",
            ),
            toApply.valuesById(),
        )
    }

    @Test
    fun `filterServerNewer skips empty server data`() {
        val toApply = filterServerNewer(
            local = listOf(syncItem(id = "local", updatedAt = 10L, value = "local")),
            server = emptyList(),
            idOf = { it.id },
            updatedAtOf = { it.updatedAt },
        )

        assertEquals(emptyList<SyncItem>(), toApply)
    }

    @Test
    fun `one hundred iOS server items are applied without loss`() {
        val server = (0 until 100).map { index ->
            syncItem(
                id = "shape-$index",
                updatedAt = 1_700_000_000_000L + index,
                value = "server-shape-$index",
            )
        }

        val result = merge(local = emptyList(), server = server)
        val toApply = filterServerNewer(
            local = emptyList(),
            server = server,
            idOf = { it.id },
            updatedAtOf = { it.updatedAt },
        )

        assertEquals(100, result.merged.size)
        assertEquals(100, toApply.size)
        assertEquals(server.map { it.id }.toSet(), result.merged.map { it.id }.toSet())
        assertEquals(server.map { it.id }.toSet(), toApply.map { it.id }.toSet())
        assertEquals(emptyList<SyncItem>(), result.toUpload)
    }

    @Test
    fun `full sync metadata is updated only when local winners are uploaded`() {
        assertTrue(shouldUpdateServerMetadataAfterFullSync(uploadedItemCount = 1))
        assertFalse(shouldUpdateServerMetadataAfterFullSync(uploadedItemCount = 0))
    }

    private fun merge(
        local: List<SyncItem>,
        server: List<SyncItem>,
    ): SyncMergeResult<SyncItem> {
        return mergeLWW(
            local = local,
            server = server,
            idOf = { it.id },
            updatedAtOf = { it.updatedAt },
        )
    }

    private fun syncItem(
        id: String,
        updatedAt: Long,
        value: String,
        deleted: Boolean = false,
    ) = SyncItem(
        id = id,
        updatedAt = updatedAt,
        value = value,
        deleted = deleted,
    )

    private fun List<SyncItem>.valuesById(): Map<String, String> =
        associate { item -> item.id to item.value }

    private data class SyncItem(
        val id: String,
        val updatedAt: Long,
        val value: String,
        val deleted: Boolean,
    )
}
