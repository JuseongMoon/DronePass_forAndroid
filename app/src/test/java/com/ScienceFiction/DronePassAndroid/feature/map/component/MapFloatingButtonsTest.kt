package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class MapFloatingButtonsTest {

    @Test
    fun `phone paddings match iOS MainFloatingButtonView`() {
        val paddings = resolveMapFloatingButtonPaddings(isTablet = false)

        assertEquals(12.dp, paddings.edge)
        assertEquals(100.dp, paddings.flightZoneBottom)
        assertEquals(170.dp, paddings.statusGroupBottom)
        assertEquals(90.dp, paddings.createShapeBottom)
    }

    @Test
    fun `tablet edge padding matches iOS non-phone value`() {
        val paddings = resolveMapFloatingButtonPaddings(isTablet = true)

        assertEquals(20.dp, paddings.edge)
        assertEquals(100.dp, paddings.flightZoneBottom)
        assertEquals(170.dp, paddings.statusGroupBottom)
        assertEquals(90.dp, paddings.createShapeBottom)
    }

    @Test
    fun `shape and sketch FAB icon color matches iOS accentColor`() {
        assertEquals(0xFF007AFF.toInt(), MapFloatingAccentColor.toArgb())
    }

    @Test
    fun `shape and sketch FAB sizes match iOS MainFloatingButtonView`() {
        assertEquals(60.dp, MapCreateShapeButtonSize)
        assertEquals(28.dp, MapCreateShapeIconSize)
        assertEquals(6.dp, MapCreateShapeButtonShadowElevation)
        assertEquals(45.dp, MapSketchButtonSize)
        assertEquals(21.dp, MapSketchIconSize)
        assertEquals(4.dp, MapSketchButtonShadowElevation)
    }

    @Test
    fun `sketch FAB icon matches iOS pencil tip drawing action`() {
        assertEquals(Icons.Default.Draw, MapSketchButtonIcon)
    }

    @Test
    fun `right status group spacing matches iOS VStack spacing`() {
        assertEquals(8.dp, MapStatusGroupSpacing)
    }

    @Test
    fun `KP button visual tokens match iOS kpIndexButton`() {
        assertEquals(12.dp, MapKpButtonHorizontalPadding)
        assertEquals(8.dp, MapKpButtonVerticalPadding)
        assertEquals(12.dp, MapKpButtonCornerRadius)
        assertEquals(4.dp, MapKpButtonShadowElevation)
        assertEquals(4.dp, MapKpButtonTextSpacing)
        assertEquals(16.sp, MapKpButtonTextSize)
    }

    @Test
    fun `KP value text matches iOS currentKPString formatting`() {
        assertEquals("5.0", formatMapKpValue(5.0))
        assertEquals("5.3", formatMapKpValue(5.25))
        assertEquals("-", formatMapKpValue(null))
    }

    @Test
    fun `flight zone FAB visual tokens match iOS FlightZoneLayerSelector`() {
        assertEquals(50.dp, FlightZoneFabSize)
        assertEquals(12.dp, FlightZoneFabCornerRadius)
        assertEquals(4.dp, FlightZoneFabShadowElevation)
        assertEquals(22.dp, FlightZoneFabIconSize)
        assertEquals(10f, FlightZoneFabBadgeFontSize.value, 0f)
        assertEquals(4.dp, FlightZoneFabVerticalSpacing)
    }

    @Test
    fun `flight zone FAB count change pulse matches iOS scale feedback`() {
        assertEquals(1.0f, FlightZoneFabIdleScale, 0f)
        assertEquals(1.1f, FlightZoneFabPulseScale, 0f)
        assertEquals(200L, FlightZoneFabPulseResetDelayMillis)
        assertEquals(0.6f, FlightZoneFabPulseDampingRatio, 0f)
    }

    @Test
    fun `flight zone FAB open action uses light haptic feedback like iOS`() {
        assertEquals(HapticFeedbackType.TextHandleMove, FlightZoneFabOpenHapticType)
    }

    @Test
    fun `flight zone FAB inactive state matches iOS map icon without badge`() {
        val state = resolveFlightZoneFabUiState(count = 0)

        assertEquals(false, state.active)
        assertEquals(FlightZoneFabIconStyle.OUTLINED_MAP, state.iconStyle)
        assertEquals(null, state.badgeText)
    }

    @Test
    fun `flight zone FAB active state matches iOS filled map icon and count badge`() {
        val state = resolveFlightZoneFabUiState(count = 3)

        assertEquals(true, state.active)
        assertEquals(FlightZoneFabIconStyle.FILLED_MAP, state.iconStyle)
        assertEquals("3", state.badgeText)
        assertEquals(0xFF007AFF.toInt(), state.tint.toArgb())
    }
}
