package com.ScienceFiction.DronePassAndroid.feature.vworld

import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import org.junit.Assert.assertEquals
import org.junit.Test

class FlightZoneOverlayManagerTest {

    @Test
    fun `displayed zones are unique by layer and feature id like iOS loaded zones`() {
        val multiPolygonZone = droneZone(id = "zone.1", layer = FlightZoneLayer.NATIONAL_PARK)
        val sameZoneSecondOverlay = multiPolygonZone.copy()
        val sameIdDifferentLayer = droneZone(id = "zone.1", layer = FlightZoneLayer.CONTROL_ZONE)

        val displayedZones = deduplicateDisplayedFlightZones(
            listOf(multiPolygonZone, sameZoneSecondOverlay, sameIdDifferentLayer),
        )

        assertEquals(
            listOf(
                FlightZoneLayer.NATIONAL_PARK to "zone.1",
                FlightZoneLayer.CONTROL_ZONE to "zone.1",
            ),
            displayedZones.map { zone -> zone.layer to zone.id },
        )
    }

    private fun droneZone(
        id: String,
        layer: FlightZoneLayer
    ): DroneZoneFeature = DroneZoneFeature(
        id = id,
        layer = layer,
        polygons = listOf(
            listOf(
                37.0 to 126.0,
                37.0 to 126.01,
                37.01 to 126.0,
            ),
        ),
        zoneCode = id,
        upperAltitude = null,
        lowerAltitude = null,
        zoneName = null,
    )
}
