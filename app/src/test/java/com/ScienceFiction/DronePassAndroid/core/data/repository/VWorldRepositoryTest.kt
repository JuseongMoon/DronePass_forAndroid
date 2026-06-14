package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VWorldRepositoryTest {

    @Test
    fun `parsed zone code uses only iOS layer specific fields`() {
        assertEquals(
            "RK P73A",
            resolveParsedVWorldZoneCode(
                layer = FlightZoneLayer.PROHIBITED,
                featureId = "lt_c_aisprhc.1",
                properties = mapOf(
                    "prh_lbl_1" to "RK P73A",
                    "code" to "generic-code",
                ),
            ),
        )
        assertNull(
            resolveParsedVWorldZoneCode(
                layer = FlightZoneLayer.ATZ,
                featureId = "lt_c_aisatzc.1",
                properties = mapOf(
                    "code" to "generic-code",
                    "zoneCode" to "generic-zone-code",
                ),
            ),
        )
    }
}
