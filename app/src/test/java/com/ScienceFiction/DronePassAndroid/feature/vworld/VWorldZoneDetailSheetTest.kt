package com.ScienceFiction.DronePassAndroid.feature.vworld

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.NotamStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VWorldZoneDetailSheetTest {

    @Test
    fun `detail rows use iOS defaultMinListRowHeight 44`() {
        assertEquals(44.dp, VWorldZoneDetailRowMinHeight)
    }

    @Test
    fun `detail row dividers use thin iOS List separator`() {
        assertEquals(0.5.dp, VWorldZoneDetailRowDividerThickness)
    }

    @Test
    fun `zone type marker uses iOS filled circle without border`() {
        assertEquals(12.dp, VWorldZoneDetailZoneTypeMarkerSize)
        assertEquals(0.dp, VWorldZoneDetailZoneTypeMarkerBorderWidth)
    }

    @Test
    fun `dynamic sheet heights match iOS VWorldZoneDetail detents by layer`() {
        assertEquals(680.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.CULTURAL_HERITAGE))
        assertEquals(550.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.TEMPORARY_PROHIBITED))
        assertEquals(480.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.PRIOR_CONSULTATION))
        assertEquals(480.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.NATIONAL_PARK))
        assertEquals(340.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.PROHIBITED))
        assertEquals(340.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.RESTRICTED))
        assertEquals(340.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.ALERT))
        assertEquals(340.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.DANGER))
        assertEquals(280.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.CONTROL_ZONE))
        assertEquals(280.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.ULTRALIGHT))
        assertEquals(230.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.LANDING_FIELD))
        assertEquals(230.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.OBSTACLE))
        assertEquals(230.dp, resolveVWorldZoneDetailSheetMinHeight(FlightZoneLayer.ATZ))
    }

    @Test
    fun `notam status colors match iOS detail sheet`() {
        assertEquals(Color(0xFFFF3B30), resolveNotamStatusValueColor(NotamStatus.ACTIVE))
        assertEquals(Color(0xFF007AFF), resolveNotamStatusValueColor(NotamStatus.SCHEDULED))
        assertEquals(Color.Gray, resolveNotamStatusValueColor(NotamStatus.EXPIRED))
        assertNull(resolveNotamStatusValueColor(NotamStatus.UNKNOWN))
    }

    @Test
    fun `notam status display names use iOS localization keys`() {
        assertEquals(R.string.zone_detail_notam_status_scheduled, NotamStatus.SCHEDULED.displayNameRes)
        assertEquals(R.string.zone_detail_notam_status_active, NotamStatus.ACTIVE.displayNameRes)
        assertEquals(R.string.zone_detail_notam_status_expired, NotamStatus.EXPIRED.displayNameRes)
        assertEquals(R.string.zone_detail_notam_status_unknown, NotamStatus.UNKNOWN.displayNameRes)
    }

    @Test
    fun `altitude row is shown when either upper or lower value exists like iOS altitudeInfo`() {
        assertTrue(shouldShowAltitudeRow(upper = "무제한", lower = "지면 (0m)"))
        assertTrue(shouldShowAltitudeRow(upper = "무제한", lower = null))
        assertTrue(shouldShowAltitudeRow(upper = null, lower = "지면 (0m)"))
        assertFalse(shouldShowAltitudeRow(upper = null, lower = null))
    }

    @Test
    fun `public contact lookup uses zoneCode before fallback name like iOS`() {
        val zone = DroneZoneFeature(
            id = "zone.1",
            layer = FlightZoneLayer.PROHIBITED,
            polygons = emptyList(),
            zoneCode = "RK P73A",
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "다른 표시명"
        )

        assertEquals("RK P73A", resolvePublicContactLookupName(zone))
    }

    @Test
    fun `public contact lookup falls back to layer display name when zoneCode is missing like iOS`() {
        val zone = DroneZoneFeature(
            id = "zone.2",
            layer = FlightZoneLayer.ATZ,
            polygons = emptyList(),
            zoneCode = null,
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "VWorld 개별 구역명"
        )

        assertEquals("비행장교통구역", resolvePublicContactLookupName(zone))
    }

    @Test
    fun `phone row uses safe dial intent with sanitized tel uri`() {
        assertEquals("tel:0312909221", buildVWorldPhoneDialUriString("031-290 9221"))
    }
}
