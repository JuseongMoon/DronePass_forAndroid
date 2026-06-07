package com.ScienceFiction.DronePassAndroid.feature.map

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import org.junit.Assert.assertEquals
import org.junit.Test

class MapScreenLayersTest {

    @Test
    fun `드론 드롭다운 상단 패딩은 iOS처럼 iPad 에서 30dp 이다`() {
        assertEquals(
            30.dp,
            resolveDroneDropdownTopPadding(
                screenHeightDp = 1_024,
                isTablet = true,
            ),
        )
    }

    @Test
    fun `드론 드롭다운 상단 패딩은 iOS처럼 폰 높이별 값을 사용한다`() {
        assertEquals(60.dp, resolveDroneDropdownTopPadding(screenHeightDp = 901, isTablet = false))
        assertEquals(50.dp, resolveDroneDropdownTopPadding(screenHeightDp = 851, isTablet = false))
        assertEquals(40.dp, resolveDroneDropdownTopPadding(screenHeightDp = 850, isTablet = false))
        assertEquals(40.dp, resolveDroneDropdownTopPadding(screenHeightDp = 800, isTablet = false))
    }

    @Test
    fun `스케치 툴바 하단 간격은 iOS처럼 폰 15dp 태블릿 20dp 이다`() {
        assertEquals(15.dp, resolveSketchToolbarBottomPadding(isTablet = false))
        assertEquals(20.dp, resolveSketchToolbarBottomPadding(isTablet = true))
    }

    @Test
    fun `스케치 모드에서는 iOS처럼 기존 도형 오버레이를 렌더하지 않는다`() {
        assertEquals(false, shouldRenderShapeOverlays(mapReady = true, isSketchMode = true))
        assertEquals(true, shouldRenderShapeOverlays(mapReady = true, isSketchMode = false))
        assertEquals(false, shouldRenderShapeOverlays(mapReady = false, isSketchMode = false))
    }

    @Test
    fun `스케치 모드에서는 iOS처럼 비행구역 오버레이도 렌더하지 않는다`() {
        assertEquals(false, shouldRenderFlightZoneOverlays(mapReady = true, isSketchMode = true))
        assertEquals(true, shouldRenderFlightZoneOverlays(mapReady = true, isSketchMode = false))
        assertEquals(false, shouldRenderFlightZoneOverlays(mapReady = false, isSketchMode = false))
    }

    @Test
    fun `스케치 모드 종료 후에는 iOS처럼 기존 비행구역 선택 상태를 렌더 대상으로 복원한다`() {
        val selectedLayers = setOf(
            FlightZoneLayer.PROHIBITED,
            FlightZoneLayer.CONTROL_ZONE,
        )

        assertEquals(
            emptySet<FlightZoneLayer>(),
            visibleFlightZoneLayersForRender(
                visibleLayers = selectedLayers,
                mapReady = true,
                isSketchMode = true,
            ),
        )
        assertEquals(
            selectedLayers,
            visibleFlightZoneLayersForRender(
                visibleLayers = selectedLayers,
                mapReady = true,
                isSketchMode = false,
            ),
        )
    }

    @Test
    fun `한국 특화 기능 전환처럼 레이어 선택이 비면 비행구역 캐시도 비운다`() {
        val cachedZones = mapOf(
            FlightZoneLayer.PROHIBITED to listOf("prohibited"),
            FlightZoneLayer.CONTROL_ZONE to listOf("control"),
        )

        assertEquals(
            emptyMap<FlightZoneLayer, List<String>>(),
            filterFlightZoneCacheForVisibleLayers(
                current = cachedZones,
                visibleLayers = emptySet(),
            ),
        )
    }

    @Test
    fun `레이어 일부를 해제하면 iOS처럼 숨겨진 비행구역 캐시를 제거한다`() {
        val cachedZones = mapOf(
            FlightZoneLayer.PROHIBITED to listOf("prohibited"),
            FlightZoneLayer.CONTROL_ZONE to listOf("control"),
        )

        assertEquals(
            mapOf(FlightZoneLayer.PROHIBITED to listOf("prohibited")),
            filterFlightZoneCacheForVisibleLayers(
                current = cachedZones,
                visibleLayers = setOf(FlightZoneLayer.PROHIBITED),
            ),
        )
    }

    @Test
    fun `한국 특화 기능이 꺼져 있으면 저장된 비행구역 레이어를 복원하지 않는다`() {
        val storedLayerIds = setOf(
            FlightZoneLayer.PROHIBITED.typeName,
            FlightZoneLayer.CONTROL_ZONE.typeName,
        )

        assertEquals(
            emptySet<FlightZoneLayer>(),
            resolveInitialVisibleFlightZoneLayers(
                koreaFeaturesEnabled = false,
                storedLayerIds = storedLayerIds,
            ),
        )
    }

    @Test
    fun `한국 특화 기능이 켜져 있으면 iOS처럼 저장된 비행구역 레이어를 복원한다`() {
        val storedLayerIds = setOf(
            FlightZoneLayer.PROHIBITED.typeName,
            FlightZoneLayer.CONTROL_ZONE.name,
            "unknown-layer",
        )

        assertEquals(
            setOf(FlightZoneLayer.PROHIBITED, FlightZoneLayer.CONTROL_ZONE),
            resolveInitialVisibleFlightZoneLayers(
                koreaFeaturesEnabled = true,
                storedLayerIds = storedLayerIds,
            ),
        )
    }
}
