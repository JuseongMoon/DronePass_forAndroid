package com.ScienceFiction.DronePassAndroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppIdentityContractTest {

    @Test
    fun applicationIdKeepsSharedFirebasePackageName() {
        assertEquals("com.ScienceFiction.DronePassAndroid", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun debugBuildDoesNotUseApplicationIdSuffix() {
        assertFalse(BuildConfig.APPLICATION_ID.endsWith(".debug"))
    }
}
