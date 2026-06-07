package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Query

class VWorldApiTest {

    @Test
    fun `getFeatures uses iOS WFS typename query parameter`() {
        val queryNames = VWorldApi::class.java.methods
            .single { it.name == "getFeatures" }
            .parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Query>() }
            .map { it.value }

        assertTrue(queryNames.contains("typename"))
        assertFalse(queryNames.contains("typeName"))
    }
}
