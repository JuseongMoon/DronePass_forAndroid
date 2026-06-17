package com.ScienceFiction.DronePassAndroid.core.data.sync

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RealtimeSyncManagerTest {

    @Test
    fun `manual force sync covers all realtime data domains`() {
        assertEquals(
            listOf(
                RealtimeForceSyncDomain.ShapeDrone,
                RealtimeForceSyncDomain.Sketch,
            ),
            realtimeForceSyncDomains(),
        )
    }

    @Test
    fun `shape drone realtime sync success clears local modification markers like iOS`() {
        assertEquals(
            listOf(
                SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME,
                SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME,
            ),
            SHAPE_REALTIME_SYNC_SUCCESS_KEYS_TO_CLEAR,
        )
    }

    @Test
    fun `sketch realtime sync success clears local modification marker like iOS`() {
        assertEquals(
            listOf(SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME),
            SKETCH_REALTIME_SYNC_SUCCESS_KEYS_TO_CLEAR,
        )
    }

    @Test
    fun `shape drone sync success records sync time and clears dirty markers`() {
        val preferences = mutablePreferencesOf(
            SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME to 100L,
            SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME to 200L,
        )

        preferences.recordShapeRealtimeSyncSuccess(300L)

        assertEquals(300L, preferences[SyncPreferenceKeys.LAST_SYNC_TIME])
        assertNull(preferences[SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME])
        assertNull(preferences[SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME])
    }

    @Test
    fun `shape drone sync success can update account switch baseline`() {
        val preferences = mutablePreferencesOf(
            SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME to 100L,
            SyncPreferenceKeys.SYNCED_SHAPE_BASELINE to "{\"old\":1}",
        )

        preferences.recordShapeRealtimeSyncSuccess(
            syncTimeMillis = 300L,
            syncedShapeBaseline = "{\"new\":2}",
        )

        assertEquals(300L, preferences[SyncPreferenceKeys.LAST_SYNC_TIME])
        assertEquals("{\"new\":2}", preferences[SyncPreferenceKeys.SYNCED_SHAPE_BASELINE])
        assertNull(preferences[SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME])
    }

    @Test
    fun `account switch baseline stores only active shape updated times`() {
        val baseline = buildAccountSwitchShapeBaseline(
            listOf(
                ShapeModel(id = "active", updatedAt = 100L, deletedAt = null),
                ShapeModel(id = "deleted", updatedAt = 200L, deletedAt = 300L),
            ),
        )

        assertEquals(mapOf("active" to 100L), baseline)
    }

    @Test
    fun `sketch sync success records sync time and clears dirty marker`() {
        val preferences = mutablePreferencesOf(
            SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME to 100L,
        )

        preferences.recordSketchRealtimeSyncSuccess(300L)

        assertEquals(300L, preferences[SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME])
        assertNull(preferences[SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME])
    }

    @Test
    fun `realtime listener retries silently but manual sync propagates failures`() {
        assertEquals(false, shouldRethrowRealtimeSyncFailure(manualRequest = false))
        assertEquals(true, shouldRethrowRealtimeSyncFailure(manualRequest = true))
    }

    @Test
    fun `realtime listener schedules only server changes newer than last sync like iOS`() {
        assertEquals(
            true,
            shouldScheduleRealtimeSync(
                serverLastModified = 300L,
                lastSyncTime = 200L,
                lastLocalModificationTime = null,
            ),
        )
        assertEquals(
            false,
            shouldScheduleRealtimeSync(
                serverLastModified = 200L,
                lastSyncTime = 200L,
                lastLocalModificationTime = null,
            ),
        )
    }

    @Test
    fun `realtime listener treats missing last sync as iOS distant past baseline`() {
        assertEquals(
            true,
            shouldScheduleRealtimeSync(
                serverLastModified = 1L,
                lastSyncTime = null,
                lastLocalModificationTime = null,
            ),
        )
        assertEquals(
            false,
            shouldScheduleRealtimeSync(
                serverLastModified = 1L,
                lastSyncTime = null,
                lastLocalModificationTime = 1L,
            ),
        )
        assertEquals(
            true,
            hasForegroundShapeMetadataChange(
                serverLastModified = 1L,
                lastSyncTime = null,
                lastLocalModificationTime = null,
            ),
        )
    }

    @Test
    fun `foreground change prompt requires a remote timestamp`() {
        assertEquals(
            false,
            hasRealtimeRemoteChanges(
                serverLastModified = null,
                lastSyncTime = 200L,
                lastLocalModificationTime = null,
            ),
        )
    }

    @Test
    fun `foreground change prompt appears only for remote changes newer than sync baseline`() {
        assertEquals(
            true,
            hasRealtimeRemoteChanges(
                serverLastModified = 300L,
                lastSyncTime = 200L,
                lastLocalModificationTime = null,
            ),
        )
        assertEquals(
            false,
            hasRealtimeRemoteChanges(
                serverLastModified = 200L,
                lastSyncTime = 200L,
                lastLocalModificationTime = null,
            ),
        )
    }

    @Test
    fun `foreground change prompt follows iOS ChangeDetectionManager shape metadata check`() {
        assertEquals(
            true,
            hasForegroundShapeMetadataChange(
                serverLastModified = 300L,
                lastSyncTime = 200L,
                lastLocalModificationTime = null,
            ),
        )
        assertEquals(
            false,
            hasForegroundShapeMetadataChange(
                serverLastModified = null,
                lastSyncTime = 200L,
                lastLocalModificationTime = null,
            ),
        )
        assertEquals(
            false,
            hasForegroundShapeMetadataChange(
                serverLastModified = 200L,
                lastSyncTime = 100L,
                lastLocalModificationTime = 200L,
            ),
        )
    }

    @Test
    fun `foreground change prompt ignores remote timestamps already covered by local edits`() {
        assertEquals(
            false,
            hasRealtimeRemoteChanges(
                serverLastModified = 200L,
                lastSyncTime = 100L,
                lastLocalModificationTime = 200L,
            ),
        )
        assertEquals(
            true,
            hasRealtimeRemoteChanges(
                serverLastModified = 300L,
                lastSyncTime = 100L,
                lastLocalModificationTime = 200L,
            ),
        )
    }

    @Test
    fun `realtime listener skips server timestamps covered by local modification like iOS`() {
        assertEquals(
            false,
            shouldScheduleRealtimeSync(
                serverLastModified = 200L,
                lastSyncTime = 100L,
                lastLocalModificationTime = 200L,
            ),
        )
        assertEquals(
            true,
            shouldScheduleRealtimeSync(
                serverLastModified = 300L,
                lastSyncTime = 100L,
                lastLocalModificationTime = 200L,
            ),
        )
    }

    @Test
    fun `drone collection listener schedules server snapshots because iOS does not update metadata`() {
        assertEquals(true, shouldScheduleDroneCollectionSync(hasPendingWrites = false))
        assertEquals(false, shouldScheduleDroneCollectionSync(hasPendingWrites = true))
    }

    @Test
    fun `realtime sync restart keeps active listener user first`() {
        assertEquals(
            "listening-user",
            resolveRealtimeSyncRestartUserId(
                currentListeningUserId = "listening-user",
                currentAuthUserId = "auth-user",
            ),
        )
    }

    @Test
    fun `realtime sync restart starts from auth user when listener is currently stopped`() {
        assertEquals(
            "auth-user",
            resolveRealtimeSyncRestartUserId(
                currentListeningUserId = null,
                currentAuthUserId = "auth-user",
            ),
        )
    }

    @Test
    fun `realtime sync restart is skipped when no user is available`() {
        assertEquals(
            null,
            resolveRealtimeSyncRestartUserId(
                currentListeningUserId = null,
                currentAuthUserId = null,
            ),
        )
    }

    @Test
    fun `realtime sync restart delay matches iOS half second timing`() {
        assertEquals(500L, RealtimeSyncRestartDelayMs)
    }
}
