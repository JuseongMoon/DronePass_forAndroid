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
    fun `realtime listener retries silently but manual sync propagates failures`() {
        assertEquals(false, shouldRethrowRealtimeSyncFailure(manualRequest = false))
        assertEquals(true, shouldRethrowRealtimeSyncFailure(manualRequest = true))
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
