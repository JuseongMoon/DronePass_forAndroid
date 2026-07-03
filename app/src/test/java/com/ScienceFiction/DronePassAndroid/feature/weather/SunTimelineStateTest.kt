package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SunTimelineStateTest {

    private val sunriseTimes = listOf(
        "2026-06-04T05:10",
        "2026-06-05T05:05",
    )
    private val sunsetTimes = listOf(
        "2026-06-04T19:40",
        "2026-06-05T19:41",
    )

    @Test
    fun `daytime timeline uses same day sunrise and sunset`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T12:00"),
        )

        assertTrue(state.isDaytime)
        assertEquals(LocalDateTime.parse("2026-06-04T05:10"), state.startDateTime)
        assertEquals(LocalDateTime.parse("2026-06-04T19:40"), state.endDateTime)
        assertTrue(state.nextEvent.isNextSunset)
    }

    @Test
    fun `after sunset timeline ends at tomorrow sunrise like iOS`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T20:30"),
        )

        assertFalse(state.isDaytime)
        assertEquals(LocalDateTime.parse("2026-06-04T19:40"), state.startDateTime)
        assertEquals(LocalDateTime.parse("2026-06-05T05:05"), state.endDateTime)
        assertFalse(state.nextEvent.isNextSunset)
        assertEquals("08:35", state.nextEvent.timeUntilFormatted)
    }

    @Test
    fun `before sunrise timeline approximates previous sunset like iOS`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T04:50"),
        )

        assertFalse(state.isDaytime)
        assertEquals(LocalDateTime.parse("2026-06-03T19:40"), state.startDateTime)
        assertEquals(LocalDateTime.parse("2026-06-04T05:10"), state.endDateTime)
        assertFalse(state.nextEvent.isNextSunset)
        assertEquals("00:20", state.nextEvent.timeUntilFormatted)
    }

    @Test
    fun `remaining time parser matches iOS detail card formatting input`() {
        assertEquals(SunTimelineRemainingTime(hours = 7, minutes = 40), parseSunTimelineRemainingTime("07:40"))
        assertEquals(SunTimelineRemainingTime(hours = 0, minutes = 20), parseSunTimelineRemainingTime("00:20"))
        assertEquals(SunTimelineRemainingTime(hours = 0, minutes = 0), parseSunTimelineRemainingTime("00:00"))
        assertEquals(null, parseSunTimelineRemainingTime("--:--"))
        assertEquals(null, parseSunTimelineRemainingTime("07"))
    }

    @Test
    fun `missing sun data keeps iOS sunrise sunset card placeholder instead of removing card`() {
        val now = LocalDateTime.parse("2026-06-04T12:00")
        val state = resolveSunTimelineState(
            sunriseIsoList = emptyList(),
            sunsetIsoList = emptyList(),
            now = now,
        )

        assertTrue(state.isPlaceholder)
        assertTrue(state.isDaytime)
        assertEquals(now, state.startDateTime)
        assertEquals(now.plusHours(1), state.endDateTime)
        assertTrue(state.nextEvent.isNextSunset)
        assertEquals("--:--", state.nextEvent.timeUntilFormatted)
        assertEquals(0f, calculateSunTimelineProgress(state, now), 0f)
        assertEquals(null, resolveSunTimelineMarker(state))
    }

    @Test
    fun `night marker is midnight within the active night interval`() {
        val state = resolveSunTimelineState(
            sunriseIsoList = sunriseTimes,
            sunsetIsoList = sunsetTimes,
            now = LocalDateTime.parse("2026-06-04T20:30"),
        )

        val marker = resolveSunTimelineMarker(state)!!

        assertEquals(R.string.weather_midnight, marker.labelRes)
        assertTrue(marker.progress in 0f..1f)
    }

    @Test
    fun `sun timeline endpoint icons keep iOS sunrise and sunset semantics`() {
        assertEquals(SunTimelineEndpointIcon.Sunrise, resolveSunTimelineEndpointIcon(isSunrise = true))
        assertEquals(SunTimelineEndpointIcon.Sunset, resolveSunTimelineEndpointIcon(isSunrise = false))
        assertEquals(
            "Filled.WbSunny",
            sunTimelineEndpointImageVector(SunTimelineEndpointIcon.Sunrise).name,
        )
        assertEquals(
            "Filled.WbTwilight",
            sunTimelineEndpointImageVector(SunTimelineEndpointIcon.Sunset).name,
        )
    }

    @Test
    fun `sun timeline tokens match iOS sunriseSunsetCard`() {
        assertEquals(20.sp, IosSunTimelineTitleFontSize)
        assertEquals(FontWeight.SemiBold, IosSunTimelineTitleFontWeight)
        assertEquals(16.dp, IosSunTimelineInnerSpacing)
        assertEquals(60.dp, IosSunTimelineRowHeight)
        assertEquals(8.dp, IosSunTimelineRowSpacing)
        assertEquals(56.dp, IosSunTimelineSideSlotWidth)
        assertEquals(4.dp, IosSunTimelineSideIconTimeSpacing)
        assertEquals(24.dp, IosSunTimelineSideIconSize)
        assertEquals(12.sp, IosSunTimelineSideTimeFontSize)
        assertEquals(2.dp, IosSunTimelineProgressLineHeight)
        assertEquals(2.dp, IosSunTimelineMarkerSpacing)
        assertEquals(8.dp, IosSunTimelineMarkerDiamondSize)
        assertEquals(1.dp, IosSunTimelineMarkerLineWidth)
        assertEquals(20.dp, IosSunTimelineMarkerLineHeight)
        assertEquals(11.sp, IosSunTimelineMarkerLabelFontSize)
        assertEquals(36.dp, IosSunTimelineCurrentBadgeSize)
        assertEquals(16.dp, IosSunTimelineCurrentBadgeIconSize)
        assertEquals(8.dp, IosSunTimelineCurrentBadgeShadowElevation)
        assertEquals(6.dp, IosSunTimelineRemainingSpacing)
        assertEquals(14.dp, IosSunTimelineRemainingIconSize)
        assertEquals(15.sp, IosSunTimelineRemainingFontSize)
        assertEquals(FontWeight.Normal, IosSunTimelineRegularFontWeight)
        assertEquals(FontWeight.SemiBold, IosSunTimelineSemiboldFontWeight)
    }
}
