package com.ScienceFiction.DronePassAndroid.feature.map

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapScreenLayersTest {

    @Test
    fun `드론 드롭다운 상단 패딩은 iOS처럼 iPad 에서 30dp 이다`() {
        assertEquals(
            30.dp,
            resolveDroneDropdownTopPadding(
                screenHeight = 1_024.dp,
                isTablet = true,
            ),
        )
    }

    @Test
    fun `드론 드롭다운 상단 패딩은 iOS처럼 폰 높이별 값을 사용한다`() {
        assertEquals(60.dp, resolveDroneDropdownTopPadding(screenHeight = 901.dp, isTablet = false))
        assertEquals(50.dp, resolveDroneDropdownTopPadding(screenHeight = 851.dp, isTablet = false))
        assertEquals(40.dp, resolveDroneDropdownTopPadding(screenHeight = 850.dp, isTablet = false))
        assertEquals(40.dp, resolveDroneDropdownTopPadding(screenHeight = 800.dp, isTablet = false))
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
    fun `상단 드론 드롭다운은 iOS처럼 지도 준비 전에도 표시한다`() {
        assertEquals(true, shouldShowMapDroneDropdown(isSketchMode = false))
        assertEquals(false, shouldShowMapDroneDropdown(isSketchMode = true))
    }

    @Test
    fun `지도 생명주기는 iOS처럼 KP와 날씨 자동 갱신을 함께 관리한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))",
                "kpViewModel.startAutoRefresh()",
                "weatherViewModel.startAutoRefresh()",
                "Lifecycle.Event.ON_START",
                "kpViewModel.startAutoRefresh()",
                "weatherViewModel.startAutoRefresh()",
                "Lifecycle.Event.ON_STOP",
                "kpViewModel.stopAutoRefresh()",
                "weatherViewModel.stopAutoRefresh()",
                "onDispose",
                "kpViewModel.stopAutoRefresh()",
                "weatherViewModel.stopAutoRefresh()",
            ),
        )
    }

    @Test
    fun `스케치 모드에서는 iOS처럼 비행구역 오버레이도 렌더하지 않는다`() {
        assertEquals(
            false,
            shouldRenderFlightZoneOverlays(
                mapReady = true,
                isSketchMode = true,
                koreaFeaturesEnabled = true,
            ),
        )
        assertEquals(
            true,
            shouldRenderFlightZoneOverlays(
                mapReady = true,
                isSketchMode = false,
                koreaFeaturesEnabled = true,
            ),
        )
        assertEquals(
            false,
            shouldRenderFlightZoneOverlays(
                mapReady = false,
                isSketchMode = false,
                koreaFeaturesEnabled = true,
            ),
        )
    }

    @Test
    fun `한국 특화 기능이 꺼져 있으면 iOS처럼 비행구역 오버레이를 렌더하지 않는다`() {
        assertEquals(
            false,
            shouldRenderFlightZoneOverlays(
                mapReady = true,
                isSketchMode = false,
                koreaFeaturesEnabled = false,
            ),
        )
    }

    @Test
    fun `한국 특화 기능이 꺼져 있으면 VWorld 시트를 렌더하지 않는다`() {
        assertEquals(
            false,
            shouldShowFlightZoneLayerSelector(
                koreaFeaturesEnabled = false,
                showLayerSelector = true,
            ),
        )
        assertEquals(
            false,
            shouldShowFlightZoneDetail(
                koreaFeaturesEnabled = false,
                showZoneDetail = true,
            ),
        )
        assertEquals(
            true,
            shouldShowFlightZoneLayerSelector(
                koreaFeaturesEnabled = true,
                showLayerSelector = true,
            ),
        )
        assertEquals(
            true,
            shouldShowFlightZoneDetail(
                koreaFeaturesEnabled = true,
                showZoneDetail = true,
            ),
        )
    }

    @Test
    fun `스케치 오버레이는 iOS처럼 지도 bounds 와 겹치는 스케치만 렌더한다`() {
        val bounds = MapViewportBounds(
            southWestLatitude = 37.0,
            southWestLongitude = 126.0,
            northEastLatitude = 38.0,
            northEastLongitude = 127.0,
        )
        val inside = sketch(
            id = "inside",
            points = listOf(
                Coordinate(37.2, 126.2),
                Coordinate(37.4, 126.4),
            ),
        )
        val overlapping = sketch(
            id = "overlap",
            points = listOf(
                Coordinate(36.9, 126.5),
                Coordinate(37.1, 126.6),
            ),
        )
        val outside = sketch(
            id = "outside",
            points = listOf(
                Coordinate(38.2, 127.2),
                Coordinate(38.3, 127.3),
            ),
        )
        val empty = sketch(id = "empty", points = emptyList())

        assertEquals(true, isSketchInMapBounds(inside, bounds))
        assertEquals(true, isSketchInMapBounds(overlapping, bounds))
        assertEquals(false, isSketchInMapBounds(outside, bounds))
        assertEquals(false, isSketchInMapBounds(empty, bounds))
        assertEquals(
            listOf(inside, overlapping),
            visibleSketchesForMapBounds(listOf(inside, overlapping, outside, empty), bounds),
        )
    }

    @Test
    fun `스케치 오버레이 bounds 가 아직 없으면 초기 표시를 위해 기존 목록을 유지한다`() {
        val sketches = listOf(
            sketch(
                id = "pending-bounds",
                points = listOf(
                    Coordinate(37.2, 126.2),
                    Coordinate(37.4, 126.4),
                ),
            ),
        )

        assertEquals(sketches, visibleSketchesForMapBounds(sketches, bounds = null))
    }

    @Test
    fun `새 도형 편집 시트는 기존 선택 도형이 있어도 iOS처럼 신규 좌표로 진입한다`() {
        val selectedShape = ShapeModel(
            id = "selected-shape",
            title = "Selected",
            baseCoordinate = Coordinate(37.0, 127.0),
        )

        val editingShape = resolveShapeEditSheetShape(
            selectedShape = selectedShape,
            newShapeCoordinate = Coordinate(37.5, 127.5),
            isDuplicateMode = false,
        )

        assertNull(editingShape)
        assertTrue(
            shouldFocusShapeAfterMapEditSave(
                editingShape = editingShape,
                isDuplicateMode = false,
            )
        )
    }

    @Test
    fun `기존 도형 편집과 복제 시트는 선택 도형을 유지한다`() {
        val selectedShape = ShapeModel(
            id = "selected-shape",
            title = "Selected",
            baseCoordinate = Coordinate(37.0, 127.0),
        )

        assertEquals(
            selectedShape,
            resolveShapeEditSheetShape(
                selectedShape = selectedShape,
                newShapeCoordinate = null,
                isDuplicateMode = false,
            ),
        )
        assertEquals(
            selectedShape,
            resolveShapeEditSheetShape(
                selectedShape = selectedShape,
                newShapeCoordinate = Coordinate(37.5, 127.5),
                isDuplicateMode = true,
            ),
        )
        assertTrue(
            shouldFocusShapeAfterMapEditSave(
                editingShape = selectedShape,
                isDuplicateMode = true,
            )
        )
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
                koreaFeaturesEnabled = true,
            ),
        )
        assertEquals(
            selectedLayers,
            visibleFlightZoneLayersForRender(
                visibleLayers = selectedLayers,
                mapReady = true,
                isSketchMode = false,
                koreaFeaturesEnabled = true,
            ),
        )
    }

    @Test
    fun `한국 특화 기능이 꺼져 있으면 선택된 레이어도 렌더 대상으로 복원하지 않는다`() {
        val selectedLayers = setOf(
            FlightZoneLayer.PROHIBITED,
            FlightZoneLayer.CONTROL_ZONE,
        )

        assertEquals(
            emptySet<FlightZoneLayer>(),
            visibleFlightZoneLayersForRender(
                visibleLayers = selectedLayers,
                mapReady = true,
                isSketchMode = false,
                koreaFeaturesEnabled = false,
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

    @Test
    fun `한국 특화 기능이 꺼져 있으면 새 레이어 요청도 비운다`() {
        val requestedLayers = setOf(
            FlightZoneLayer.PROHIBITED,
            FlightZoneLayer.CONTROL_ZONE,
        )

        assertEquals(
            emptySet<FlightZoneLayer>(),
            resolveRequestedFlightZoneLayers(
                koreaFeaturesEnabled = false,
                requestedLayers = requestedLayers,
            ),
        )
    }

    @Test
    fun `한국 특화 기능이 켜져 있으면 새 레이어 요청을 유지한다`() {
        val requestedLayers = setOf(
            FlightZoneLayer.PROHIBITED,
            FlightZoneLayer.CONTROL_ZONE,
        )

        assertEquals(
            requestedLayers,
            resolveRequestedFlightZoneLayers(
                koreaFeaturesEnabled = true,
                requestedLayers = requestedLayers,
            ),
        )
    }

    @Test
    fun `비행구역 로드 결과는 iOS처럼 현재 표시 중인 레이어일 때만 반영한다`() {
        assertEquals(
            true,
            shouldApplyFlightZoneLayerResult(
                layer = FlightZoneLayer.PROHIBITED,
                currentVisibleLayers = setOf(FlightZoneLayer.PROHIBITED),
            ),
        )
        assertEquals(
            false,
            shouldApplyFlightZoneLayerResult(
                layer = FlightZoneLayer.CONTROL_ZONE,
                currentVisibleLayers = setOf(FlightZoneLayer.PROHIBITED),
            ),
        )
    }

    @Test
    fun `비행구역 표시 수는 iOS overlayCount처럼 렌더 가능한 폴리곤 오버레이 수를 센다`() {
        val outerOne = listOf(37.0 to 126.0, 37.0 to 127.0, 38.0 to 127.0)
        val outerTwo = listOf(35.0 to 128.0, 35.0 to 129.0, 36.0 to 129.0)
        val hole = listOf(35.2 to 128.2, 35.2 to 128.4, 35.4 to 128.4)
        val invalidOuter = listOf(34.0 to 127.0, 34.1 to 127.1)
        val multiPolygonZone = DroneZoneFeature(
            id = "multi-polygon-zone",
            layer = FlightZoneLayer.PROHIBITED,
            polygons = listOf(outerOne, outerTwo, invalidOuter),
            zoneCode = "RK TEST",
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "테스트 구역",
            polygonRings = listOf(
                listOf(outerOne),
                listOf(outerTwo, hole),
                listOf(invalidOuter),
            )
        )

        assertEquals(
            2,
            displayedFlightZoneOverlayCount(
                mapOf(FlightZoneLayer.PROHIBITED to listOf(multiPolygonZone))
            )
        )
    }

    private fun sketch(
        id: String,
        points: List<Coordinate>,
    ): SketchModel {
        return SketchModel(
            id = id,
            points = points,
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in MapScreen.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
