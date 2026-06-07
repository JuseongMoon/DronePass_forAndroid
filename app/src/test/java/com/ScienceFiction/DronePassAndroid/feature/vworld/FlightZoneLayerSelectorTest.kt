package com.ScienceFiction.DronePassAndroid.feature.vworld

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlightZoneLayerSelectorTest {

    @Test
    fun `비행구역 선택 시트는 iOS처럼 표시 이름 가나다순으로 정렬한다`() {
        val sortedLayers = sortedFlightZoneLayersForSelector { it.displayName }

        assertEquals(
            listOf(
                FlightZoneLayer.ALERT,
                FlightZoneLayer.LANDING_FIELD,
                FlightZoneLayer.CONTROL_ZONE,
                FlightZoneLayer.NATIONAL_PARK,
                FlightZoneLayer.CULTURAL_HERITAGE,
                FlightZoneLayer.PROHIBITED,
                FlightZoneLayer.ATZ,
                FlightZoneLayer.RESTRICTED,
                FlightZoneLayer.PRIOR_CONSULTATION,
                FlightZoneLayer.DANGER,
                FlightZoneLayer.TEMPORARY_PROHIBITED,
                FlightZoneLayer.OBSTACLE,
                FlightZoneLayer.ULTRALIGHT,
            ),
            sortedLayers,
        )
    }

    @Test
    fun `법적 안내 배너는 iOS처럼 상단 여백과 파란 테두리를 사용한다`() {
        assertEquals(Color(0x1A007AFF), FlightZoneLayerSelectorLegalNoticeBackgroundColor)
        assertEquals(16.dp, FlightZoneLayerSelectorLegalNoticeTopPadding)
        assertEquals(8.dp, FlightZoneLayerSelectorLegalNoticeBottomPadding)
        assertEquals(1.dp, FlightZoneLayerSelectorLegalNoticeBorderWidth)
        assertEquals(Color(0x4D007AFF), FlightZoneLayerSelectorLegalNoticeBorderColor)
    }

    @Test
    fun `레이어 행 separator는 iOS처럼 마지막 행 뒤에도 표시한다`() {
        assertEquals(60.dp, FlightZoneLayerSelectorDividerLeadingPadding)
        assertTrue(shouldShowFlightZoneLayerDividerAfterItem(index = 0, lastIndex = 2))
        assertTrue(shouldShowFlightZoneLayerDividerAfterItem(index = 2, lastIndex = 2))
        assertFalse(shouldShowFlightZoneLayerDividerAfterItem(index = 0, lastIndex = -1))
    }
}
