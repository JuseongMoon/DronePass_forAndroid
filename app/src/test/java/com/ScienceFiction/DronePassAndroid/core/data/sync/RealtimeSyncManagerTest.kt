package com.ScienceFiction.DronePassAndroid.core.data.sync

import org.junit.Assert.assertEquals
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
    fun `shape realtime sync success clears local modification marker like iOS`() {
        assertEquals(
            listOf(SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME),
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
}
