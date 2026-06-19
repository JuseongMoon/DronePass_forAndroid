package com.ScienceFiction.DronePassAndroid.feature.map

import android.annotation.SuppressLint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.core.ui.currentWindowSizeDp
import com.ScienceFiction.DronePassAndroid.core.data.local.findVWorldContact
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.feature.kp.KpForecastContent
import com.ScienceFiction.DronePassAndroid.feature.kp.KpSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.kp.KpViewModel
import com.ScienceFiction.DronePassAndroid.feature.map.component.DroneSelectionDropdown
import com.ScienceFiction.DronePassAndroid.feature.map.component.MapFloatingButtons
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.ShapeOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.settings.KpInfoGuideSheet
import com.ScienceFiction.DronePassAndroid.feature.settings.WeatherInfoGuideSheet
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeDetailSheet
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditScreen
import com.ScienceFiction.DronePassAndroid.feature.shape.resolveShapeEditDefaultColor
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchToolbar
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchViewModel
import com.ScienceFiction.DronePassAndroid.feature.vworld.FlightZoneLayerSelector
import com.ScienceFiction.DronePassAndroid.feature.vworld.FlightZoneOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.vworld.VWorldZoneDetailSheet
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastContent
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherInfoTopic
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherViewModel
import com.naver.maps.map.NaverMap
import com.naver.maps.map.MapView

/**
 * MapScreen 의 자식 Composable 4종.
 *
 * MapScreen 본체가 25+ collectAsStateWithLifecycle 로 한 함수에 묶여 어떤 Flow 가 emit 해도
 * 전체 함수가 재컴포지션되던 구조를 영역별로 분리한다. 각 자식은 자신이 사용하는 ViewModel
 * Flow 만 collect 하므로 Compose 의 recomposition 범위가 자식 단위로 좁혀진다.
 *
 * 4개 자식:
 *  - [MapOverlayEffects]      : LaunchedEffect 묶음 (오버레이/스케치 갱신, 화면 출력 없음)
 *  - [MapFloatingControls]    : 드론 드롭다운 + FAB + KP/Weather 카드 + Snackbar
 *  - [MapSketchInput]         : 스케치 입력 인터셉터 + 툴바
 *  - [MapBottomSheets]        : 5개 모달 시트 (ShapeDetail/Edit/LayerSelector/ZoneDetail/Kp/Weather)
 *
 * 부모(MapScreen)는 NaverMap factory + lifecycle + permissions + 위치 추적 + 카메라 이벤트만 담당.
 */

// ──────────────────────────────────────────────
// 1. MapOverlayEffects — 오버레이 갱신 LaunchedEffect 묶음
// ──────────────────────────────────────────────

/**
 * 도형/스케치/비행구역 오버레이 갱신 effect 들을 한 곳에 모은다. 화면 출력 없음.
 * mapReady 와 NaverMap 이 준비된 시점에 각 ViewModel 의 state 를 구독해 OverlayManager 에 반영.
 */
@Composable
internal fun MapOverlayEffects(
    naverMap: NaverMap?,
    mapReady: Boolean,
    overlayManager: ShapeOverlayManager,
    sketchOverlayManager: SketchOverlayManager,
    flightZoneOverlayManager: FlightZoneOverlayManager,
    viewModel: MapViewModel,
    sketchViewModel: SketchViewModel,
) {
    // 화면에 직접 그리지 않는 영역에서만 collect — recomposition 범위 격리
    val filteredShapes by viewModel.filteredShapes.collectAsStateWithLifecycle()
    val selectedShapeId by viewModel.selectedShapeId.collectAsStateWithLifecycle()
    val highlightedDroneIds by viewModel.highlightedDroneIds.collectAsStateWithLifecycle()
    val flightZones by viewModel.flightZones.collectAsStateWithLifecycle()
    val visibleLayers by viewModel.visibleLayers.collectAsStateWithLifecycle()
    val currentMapBounds by viewModel.currentMapBounds.collectAsStateWithLifecycle()
    val koreaFeaturesEnabled by viewModel.koreaFeaturesEnabled.collectAsStateWithLifecycle()

    val isSketchMode by sketchViewModel.isSketchMode.collectAsStateWithLifecycle()
    val activeSketches by sketchViewModel.activeSketches.collectAsStateWithLifecycle()
    val currentDrawingPoints by sketchViewModel.currentDrawingPoints.collectAsStateWithLifecycle()
    val currentColor by sketchViewModel.currentColor.collectAsStateWithLifecycle()
    val currentStrokeWidth by sketchViewModel.currentStrokeWidth.collectAsStateWithLifecycle()
    val currentOpacity by sketchViewModel.currentOpacity.collectAsStateWithLifecycle()

    // iOS `ClearMapOverlays` 정합: 로그아웃/탈퇴 시 로컬 도형은 유지하고 하이라이트만 제거한다.
    LaunchedEffect(mapReady) {
        viewModel.clearMapHighlightEvent.collect {
            if (mapReady) {
                overlayManager.setHighlight(null, emptyList())
            }
        }
    }

    // 비행구역 오버레이 갱신.
    // visibleLayers 변경에 따른 fetch 는 MapViewModel.flightZoneLoadCollector 가
    // (visibleLayers, currentMapBounds) combine 으로 처리한다. 여기서는 fetch 결과인
    // flightZones 의 변경만 오버레이에 반영한다.
    LaunchedEffect(flightZones, mapReady, visibleLayers, isSketchMode, koreaFeaturesEnabled) {
        if (
            !shouldRenderFlightZoneOverlays(
                mapReady = mapReady,
                isSketchMode = isSketchMode,
                koreaFeaturesEnabled = koreaFeaturesEnabled,
            )
        ) {
            if (mapReady) {
                flightZoneOverlayManager.clearAllOverlays()
            }
            return@LaunchedEffect
        }
        if (visibleLayers.isEmpty()) {
            flightZoneOverlayManager.clearAllOverlays()
            return@LaunchedEffect
        }
        hiddenFlightZoneLayers(visibleLayers).forEach { layer ->
            flightZoneOverlayManager.removeLayerOverlays(layer)
        }
        flightZones.filterKeys { it in visibleLayers }.forEach { (layer, zones) ->
            flightZoneOverlayManager.setZones(layer, zones)
        }
    }

    // 도형 오버레이 갱신 (만료/미시작 필터 적용된 filteredShapes 사용)
    LaunchedEffect(filteredShapes, highlightedDroneIds, mapReady, isSketchMode) {
        if (!shouldRenderShapeOverlays(mapReady = mapReady, isSketchMode = isSketchMode)) {
            if (mapReady && isSketchMode) {
                overlayManager.clearOverlays()
            }
            return@LaunchedEffect
        }
        overlayManager.updateOverlays(filteredShapes, highlightedDroneIds)
    }

    // 선택 하이라이트
    LaunchedEffect(selectedShapeId, filteredShapes, mapReady, isSketchMode) {
        if (!shouldRenderShapeOverlays(mapReady = mapReady, isSketchMode = isSketchMode)) {
            if (mapReady && isSketchMode) {
                overlayManager.clearOverlays()
            }
            return@LaunchedEffect
        }
        overlayManager.setHighlight(selectedShapeId, filteredShapes)
    }

    // 저장된 스케치 오버레이
    LaunchedEffect(activeSketches, mapReady, currentMapBounds) {
        if (mapReady) {
            sketchOverlayManager.updateOverlays(
                visibleSketchesForMapBounds(
                    sketches = activeSketches,
                    bounds = currentMapBounds,
                )
            )
        }
    }

    // 스케치 프리뷰 — 좌표 변경 / 스타일 변경 effect 분리 (Phase 2.4 B-H4)
    LaunchedEffect(currentDrawingPoints) {
        if (currentDrawingPoints.isNotEmpty()) {
            sketchOverlayManager.updatePreviewOverlay(
                points = currentDrawingPoints,
                color = currentColor,
                strokeWidth = currentStrokeWidth,
                opacity = currentOpacity,
            )
        } else {
            sketchOverlayManager.clearPreviewOverlay()
        }
    }
    LaunchedEffect(currentColor, currentStrokeWidth, currentOpacity) {
        if (currentDrawingPoints.isNotEmpty()) {
            sketchOverlayManager.updatePreviewOverlay(
                points = currentDrawingPoints,
                color = currentColor,
                strokeWidth = currentStrokeWidth,
                opacity = currentOpacity,
            )
        }
    }

    // 스케치 모드에서도 iOS처럼 2손가락 지도 조작은 유지한다.
    // 1손가락 입력 차단/처리는 MapSketchInput 의 MapView 터치 리스너에서 담당한다.
}

// ──────────────────────────────────────────────
// 2. MapFloatingControls — 드론 드롭다운 + FAB + Snackbar
// ──────────────────────────────────────────────

/**
 * 지도 위 컨트롤 (스케치 모드가 아닐 때만 표시).
 * mapReady 만 외부에서 받고 나머지는 자체 collect.
 */
@Composable
internal fun MapFloatingControls(
    mapReady: Boolean,
    naverMap: NaverMap?,
    viewModel: MapViewModel,
    sketchViewModel: SketchViewModel,
    kpViewModel: KpViewModel,
    weatherViewModel: WeatherViewModel,
    onShowKpForecast: () -> Unit,
    onShowWeather: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSketchMode by sketchViewModel.isSketchMode.collectAsStateWithLifecycle()
    val activeDrones by viewModel.activeDrones.collectAsStateWithLifecycle()
    val selectedDroneIds by viewModel.selectedDroneIds.collectAsStateWithLifecycle()
    val highlightedDroneIds by viewModel.highlightedDroneIds.collectAsStateWithLifecycle()
    val visibleLayerCount by viewModel.visibleLayerCount.collectAsStateWithLifecycle()
    val koreaFeaturesEnabled by viewModel.koreaFeaturesEnabled.collectAsStateWithLifecycle()
    val currentKp by kpViewModel.currentKp.collectAsStateWithLifecycle()
    val kpLevel by kpViewModel.kpLevel.collectAsStateWithLifecycle()
    val weatherData by weatherViewModel.weatherData.collectAsStateWithLifecycle()
    val windowSize = currentWindowSizeDp()
    val isTabletLayout = windowSize.width >= MapTabletBreakpointDp.dp
    val droneDropdownTopPadding = resolveDroneDropdownTopPadding(
        screenHeight = windowSize.height,
        isTablet = isTabletLayout,
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 드론 선택 드롭다운 (상단 우측)
        // iOS `droneDropdownView`: .padding(.top, safeAreaInsets.top + dropdownTopPadding)
        // dropdownTopPadding 은 화면 크기별 40~60dp. 안드로이드는 statusBarsPadding 으로
        // 상태바 영역을 보호한 뒤, iOS iPhone 12/13/14/15 기준값(40dp) 을 추가 오프셋으로 사용한다.
        if (shouldShowMapDroneDropdown(isSketchMode = isSketchMode)) {
            DroneSelectionDropdown(
                activeDrones = activeDrones,
                selectedDroneIds = selectedDroneIds,
                highlightedDroneIds = highlightedDroneIds,
                onToggleSelection = { viewModel.toggleDroneSelection(it) },
                onToggleHighlight = { viewModel.toggleDroneHighlight(it) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = droneDropdownTopPadding, end = 16.dp),
            )
        }

        // 플로팅 버튼 (좌측 비행구역 FAB + 우측 도형/스케치/KP/날씨)
        if (!isSketchMode) {
            MapFloatingButtons(
                onCreateShape = {
                    val target = naverMap?.cameraPosition?.target
                    val center = resolveCreateShapeCoordinateFromMapCenter(
                        latitude = target?.latitude,
                        longitude = target?.longitude,
                    )
                    viewModel.onCreateShapeRequested(center)
                },
                onEnterSketchMode = { sketchViewModel.enterSketchMode() },
                onShowFlightZoneLayers = { viewModel.toggleLayerSelector() },
                flightZoneVisibleLayerCount = visibleLayerCount,
                koreaFeaturesEnabled = koreaFeaturesEnabled,
                currentKpValue = currentKp?.kp,
                kpLevelColor = Color(kpLevel.color.toInt()),
                onShowKpForecast = onShowKpForecast,
                currentWeather = weatherData?.current,
                sunrise = weatherData?.sunrise,
                sunset = weatherData?.sunset,
                sunriseTimes = weatherData?.sunriseTimes.orEmpty(),
                sunsetTimes = weatherData?.sunsetTimes.orEmpty(),
                weatherUtcOffsetSeconds = weatherData?.utcOffsetSeconds,
                onWeatherClick = onShowWeather,
                isTabletLayout = isTabletLayout,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun hiddenFlightZoneLayers(
    visibleLayers: Set<FlightZoneLayer>
): List<FlightZoneLayer> = FlightZoneLayer.entries.filterNot { it in visibleLayers }

internal const val MapCreateShapeFallbackLatitude = 37.5665
internal const val MapCreateShapeFallbackLongitude = 126.9780

internal fun resolveCreateShapeCoordinateFromMapCenter(
    latitude: Double?,
    longitude: Double?,
): Coordinate {
    return if (latitude != null && longitude != null) {
        Coordinate(latitude, longitude)
    } else {
        Coordinate(MapCreateShapeFallbackLatitude, MapCreateShapeFallbackLongitude)
    }
}

internal fun resolveShapeEditSheetShape(
    selectedShape: ShapeModel?,
    newShapeCoordinate: Coordinate?,
    isDuplicateMode: Boolean,
): ShapeModel? {
    if (!isDuplicateMode && newShapeCoordinate != null) return null
    return selectedShape
}

internal fun shouldFocusShapeAfterMapEditSave(
    editingShape: ShapeModel?,
    isDuplicateMode: Boolean,
): Boolean = editingShape == null || isDuplicateMode

internal fun visibleSketchesForMapBounds(
    sketches: List<SketchModel>,
    bounds: MapViewportBounds?,
): List<SketchModel> {
    if (bounds == null) return sketches
    return sketches.filter { sketch -> isSketchInMapBounds(sketch, bounds) }
}

internal fun isSketchInMapBounds(
    sketch: SketchModel,
    bounds: MapViewportBounds,
): Boolean {
    if (sketch.points.isEmpty()) return false

    val minLatitude = sketch.points.minOf { it.latitude }
    val maxLatitude = sketch.points.maxOf { it.latitude }
    val minLongitude = sketch.points.minOf { it.longitude }
    val maxLongitude = sketch.points.maxOf { it.longitude }

    return !(maxLatitude < bounds.southWestLatitude ||
        minLatitude > bounds.northEastLatitude ||
        maxLongitude < bounds.southWestLongitude ||
        minLongitude > bounds.northEastLongitude)
}

internal fun shouldRenderShapeOverlays(
    mapReady: Boolean,
    isSketchMode: Boolean,
): Boolean = mapReady && !isSketchMode

internal fun shouldRenderFlightZoneOverlays(
    mapReady: Boolean,
    isSketchMode: Boolean,
    koreaFeaturesEnabled: Boolean = true,
): Boolean = mapReady && !isSketchMode && koreaFeaturesEnabled

internal fun shouldShowMapDroneDropdown(
    isSketchMode: Boolean,
): Boolean = !isSketchMode

internal fun shouldShowFlightZoneLayerSelector(
    koreaFeaturesEnabled: Boolean,
    showLayerSelector: Boolean,
): Boolean = koreaFeaturesEnabled && showLayerSelector

internal fun shouldShowFlightZoneDetail(
    koreaFeaturesEnabled: Boolean,
    showZoneDetail: Boolean,
): Boolean = koreaFeaturesEnabled && showZoneDetail

internal fun visibleFlightZoneLayersForRender(
    visibleLayers: Set<FlightZoneLayer>,
    mapReady: Boolean,
    isSketchMode: Boolean,
    koreaFeaturesEnabled: Boolean = true,
): Set<FlightZoneLayer> {
    return if (
        shouldRenderFlightZoneOverlays(
            mapReady = mapReady,
            isSketchMode = isSketchMode,
            koreaFeaturesEnabled = koreaFeaturesEnabled,
        )
    ) {
        visibleLayers
    } else {
        emptySet()
    }
}

internal fun displayedFlightZoneOverlayCount(
    flightZones: Map<FlightZoneLayer, List<DroneZoneFeature>>
): Int = flightZones.values.sumOf { zones ->
    zones.sumOf { zone ->
        zone.polygonRings.count { polygonRings ->
            polygonRings.firstOrNull()?.size?.let { it >= 3 } == true
        }
    }
}

internal fun resolveDroneDropdownTopPadding(
    screenHeight: Dp,
    isTablet: Boolean,
): Dp {
    if (isTablet) return 30.dp

    return when {
        screenHeight > 900.dp -> 60.dp
        screenHeight > 850.dp -> 50.dp
        else -> 40.dp
    }
}

internal fun resolveSketchToolbarBottomPadding(isTablet: Boolean): Dp {
    return if (isTablet) 20.dp else 15.dp
}

private const val MapTabletBreakpointDp = 600

// ──────────────────────────────────────────────
// 3. MapSketchInput — 스케치 입력 인터셉터 + 툴바
// ──────────────────────────────────────────────

/**
 * 스케치 모드 진입 시에만 표시되는 터치 인터셉터 + 하단 툴바.
 * isSketchMode = false 면 nothing-rendered (caller 가 if 분기로 호출하지 않아도 안전).
 * 이때 MapView 는 지도 클릭 대상이 아니라 펜/지우개 입력 surface 로 동작하므로
 * View.performClick() 으로 지도 클릭 semantics 를 섞지 않는다.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun MapSketchInput(
    mapView: MapView,
    naverMap: NaverMap?,
    sketchViewModel: SketchViewModel,
    modifier: Modifier = Modifier,
) {
    val isSketchMode by sketchViewModel.isSketchMode.collectAsStateWithLifecycle()
    val isEraserMode by sketchViewModel.isEraserMode.collectAsStateWithLifecycle()
    val currentColor by sketchViewModel.currentColor.collectAsStateWithLifecycle()
    val currentStrokeWidth by sketchViewModel.currentStrokeWidth.collectAsStateWithLifecycle()
    val currentOpacity by sketchViewModel.currentOpacity.collectAsStateWithLifecycle()
    val canUndo by sketchViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by sketchViewModel.canRedo.collectAsStateWithLifecycle()
    val activeSketches by sketchViewModel.activeSketches.collectAsStateWithLifecycle()
    val windowSize = currentWindowSizeDp()
    val isTabletLayout = windowSize.width >= MapTabletBreakpointDp.dp

    if (!isSketchMode) return

    DisposableEffect(mapView, naverMap, isSketchMode, isEraserMode) {
        val map = naverMap
        if (map == null) {
            onDispose { mapView.setOnTouchListener(null) }
        } else {
            var isSketchTouchActive = false
            val listener = View.OnTouchListener { _, event ->
                val decision = resolveSketchTouchEvent(
                    eventType = event.toSketchTouchEventType(),
                    pointerCount = event.pointerCount,
                    isSketchTouchActive = isSketchTouchActive,
                    isEraserMode = isEraserMode,
                )

                when (decision.action) {
                    SketchTouchAction.StartDrawing -> {
                        sketchViewModel.startDrawing(event.toMapCoordinate(map))
                    }
                    SketchTouchAction.ContinueDrawing -> {
                        sketchViewModel.continueDrawing(event.toMapCoordinate(map))
                    }
                    SketchTouchAction.FinishDrawing -> {
                        sketchViewModel.finishDrawing()
                    }
                    SketchTouchAction.CancelDrawing -> {
                        sketchViewModel.cancelDrawing()
                    }
                    SketchTouchAction.DeleteAtPoint -> {
                        sketchViewModel.deleteSketchAtPoint(event.toMapCoordinate(map))
                    }
                    null -> Unit
                }

                isSketchTouchActive = decision.nextIsSketchTouchActive
                decision.consume
            }
            mapView.setOnTouchListener(listener)

            onDispose {
                if (isSketchTouchActive && !isEraserMode) {
                    sketchViewModel.cancelDrawing()
                }
                mapView.setOnTouchListener(null)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 하단 스케치 툴바
        SketchToolbar(
            currentColor = currentColor,
            currentStrokeWidth = currentStrokeWidth,
            currentOpacity = currentOpacity,
            isEraserMode = isEraserMode,
            canUndo = canUndo,
            canRedo = canRedo,
            sketchCount = activeSketches.size,
            onColorChanged = { sketchViewModel.setColor(it) },
            onStrokeWidthChanged = { sketchViewModel.setStrokeWidth(it) },
            onOpacityChanged = { sketchViewModel.setOpacity(it) },
            onToggleEraser = { sketchViewModel.toggleEraserMode() },
            onUndo = { sketchViewModel.undo() },
            onRedo = { sketchViewModel.redo() },
            onDeleteAll = { sketchViewModel.deleteAllSketches() },
            onDone = { sketchViewModel.exitSketchMode() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = resolveSketchToolbarBottomPadding(isTablet = isTabletLayout)),
        )
    }
}

private fun MotionEvent.toMapCoordinate(map: NaverMap): Coordinate {
    val latLng = map.projection.fromScreenLocation(PointF(x, y))
    return Coordinate.fromLatLng(latLng)
}

internal enum class SketchTouchEventType {
    Down,
    Move,
    Up,
    Cancel,
    Other,
}

internal enum class SketchTouchAction {
    StartDrawing,
    ContinueDrawing,
    FinishDrawing,
    CancelDrawing,
    DeleteAtPoint,
}

internal data class SketchTouchDecision(
    val consume: Boolean,
    val nextIsSketchTouchActive: Boolean,
    val action: SketchTouchAction?,
)

private fun MotionEvent.toSketchTouchEventType(): SketchTouchEventType {
    return when (actionMasked) {
        MotionEvent.ACTION_DOWN -> SketchTouchEventType.Down
        MotionEvent.ACTION_MOVE -> SketchTouchEventType.Move
        MotionEvent.ACTION_UP -> SketchTouchEventType.Up
        MotionEvent.ACTION_CANCEL -> SketchTouchEventType.Cancel
        else -> SketchTouchEventType.Other
    }
}

internal fun resolveSketchTouchEvent(
    eventType: SketchTouchEventType,
    pointerCount: Int,
    isSketchTouchActive: Boolean,
    isEraserMode: Boolean,
): SketchTouchDecision {
    if (pointerCount != 1) {
        return SketchTouchDecision(
            consume = false,
            nextIsSketchTouchActive = false,
            action = if (isSketchTouchActive && !isEraserMode) {
                SketchTouchAction.CancelDrawing
            } else {
                null
            },
        )
    }

    return when (eventType) {
        SketchTouchEventType.Down -> SketchTouchDecision(
            consume = true,
            nextIsSketchTouchActive = true,
            action = if (isEraserMode) {
                SketchTouchAction.DeleteAtPoint
            } else {
                SketchTouchAction.StartDrawing
            },
        )
        SketchTouchEventType.Move -> {
            if (!isSketchTouchActive) {
                SketchTouchDecision(
                    consume = false,
                    nextIsSketchTouchActive = false,
                    action = null,
                )
            } else {
                SketchTouchDecision(
                    consume = true,
                    nextIsSketchTouchActive = true,
                    action = if (isEraserMode) {
                        SketchTouchAction.DeleteAtPoint
                    } else {
                        SketchTouchAction.ContinueDrawing
                    },
                )
            }
        }
        SketchTouchEventType.Up -> SketchTouchDecision(
            consume = isSketchTouchActive,
            nextIsSketchTouchActive = false,
            action = if (isSketchTouchActive && !isEraserMode) {
                SketchTouchAction.FinishDrawing
            } else {
                null
            },
        )
        SketchTouchEventType.Cancel -> SketchTouchDecision(
            consume = isSketchTouchActive,
            nextIsSketchTouchActive = false,
            action = if (isSketchTouchActive && !isEraserMode) {
                SketchTouchAction.CancelDrawing
            } else {
                null
            },
        )
        SketchTouchEventType.Other -> SketchTouchDecision(
            consume = isSketchTouchActive,
            nextIsSketchTouchActive = isSketchTouchActive,
            action = null,
        )
    }
}

// ──────────────────────────────────────────────
// 4. MapBottomSheets — 5개 모달 시트 묶음
// ──────────────────────────────────────────────

/**
 * 6개 BottomSheet (ShapeDetail/ShapeEdit/LayerSelector/ZoneDetail/Kp/Weather) 를 한 곳에서 관리.
 * showKpSheet / showWeatherSheet 는 부모에서 보유 (FloatingButtons 와 공유).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapBottomSheets(
    showKpSheet: Boolean,
    onDismissKpSheet: () -> Unit,
    showWeatherSheet: Boolean,
    onDismissWeatherSheet: () -> Unit,
    viewModel: MapViewModel,
    flightZoneOverlayManager: FlightZoneOverlayManager,
    kpViewModel: KpViewModel,
    weatherViewModel: WeatherViewModel,
) {
    val selectedShape by viewModel.selectedShape.collectAsStateWithLifecycle()
    val activeShapes by viewModel.activeShapes.collectAsStateWithLifecycle()
    val showShapeDetail by viewModel.showShapeDetail.collectAsStateWithLifecycle()
    val showShapeEdit by viewModel.showShapeEdit.collectAsStateWithLifecycle()
    val isDuplicateMode by viewModel.isDuplicateMode.collectAsStateWithLifecycle()
    val newShapeCoordinate by viewModel.newShapeCoordinate.collectAsStateWithLifecycle()
    val activeDrones by viewModel.activeDrones.collectAsStateWithLifecycle()
    val primarySelectedDroneId by viewModel.primarySelectedDroneId.collectAsStateWithLifecycle()
    val shapeEditDefaults by viewModel.shapeEditDefaults.collectAsStateWithLifecycle()
    val reverseGeocodedAddress by viewModel.reverseGeocodedAddress.collectAsStateWithLifecycle()
    val visibleLayers by viewModel.visibleLayers.collectAsStateWithLifecycle()
    val flightZones by viewModel.flightZones.collectAsStateWithLifecycle()
    val showLayerSelector by viewModel.showLayerSelector.collectAsStateWithLifecycle()
    val showZoneDetail by viewModel.showZoneDetail.collectAsStateWithLifecycle()
    val koreaFeaturesEnabled by viewModel.koreaFeaturesEnabled.collectAsStateWithLifecycle()
    val selectedZone by viewModel.selectedZone.collectAsStateWithLifecycle()
    val vWorldContacts by viewModel.vWorldContacts.collectAsStateWithLifecycle()
    var showKpInfoSheet by remember { mutableStateOf(false) }
    var showWeatherInfoSheet by remember { mutableStateOf(false) }
    var selectedWeatherInfoTopic by remember { mutableStateOf<WeatherInfoTopic?>(null) }

    // 도형 상세
    if (showShapeDetail) {
        selectedShape?.let { shape ->
            ShapeDetailSheet(
                shape = shape,
                onEdit = {
                    viewModel.onEditShapeRequested(
                        shape = shape,
                        returnToDetailAfterSave = true,
                    )
                },
                onDelete = { viewModel.deleteShape(shape) },
                onDismiss = { viewModel.dismissShapeDetail() },
                onDuplicate = {
                    viewModel.onDuplicateRequested(
                        shape = shape,
                        returnToDetailAfterDismiss = true,
                    )
                },
                drone = viewModel.getDroneById(shape.droneId),
                activeDrones = activeDrones,
                koreaFeaturesEnabled = koreaFeaturesEnabled,
            )
        }
    }

    // 도형 생성/편집
    if (showShapeEdit) {
        val editingShape = resolveShapeEditSheetShape(
            selectedShape = selectedShape,
            newShapeCoordinate = newShapeCoordinate,
            isDuplicateMode = isDuplicateMode,
        )
        ShapeEditScreen(
            shape = editingShape,
            initialCoordinate = newShapeCoordinate,
            drones = activeDrones,
            editDefaults = shapeEditDefaults,
            fallbackSelectedDroneId = primarySelectedDroneId,
            defaultShapeColor = resolveShapeEditDefaultColor(activeShapes),
            reverseGeocodedAddress = reverseGeocodedAddress,
            geocodingApi = viewModel.naverGeocodingApi,
            isDuplicateMode = isDuplicateMode,
            onPersistEditDefaults = { viewModel.saveShapeEditDefaults(it) },
            onDateOnlyModeChanged = { viewModel.setShapeEditDateOnlyMode(it) },
            onSave = { updatedShape, originalShapeAtEditStart, onSaveFailed ->
                viewModel.saveShape(
                    shape = updatedShape,
                    isDuplicate = isDuplicateMode,
                    focusAfterSave = shouldFocusShapeAfterMapEditSave(
                        editingShape = editingShape,
                        isDuplicateMode = isDuplicateMode,
                    ),
                    originalShapeAtEditStart = originalShapeAtEditStart,
                    onFailure = onSaveFailed,
                )
            },
            onDismiss = { viewModel.dismissShapeEdit() },
        )
    }

    // 비행구역 레이어 선택
    if (
        shouldShowFlightZoneLayerSelector(
            koreaFeaturesEnabled = koreaFeaturesEnabled,
            showLayerSelector = showLayerSelector,
        )
    ) {
        FlightZoneLayerSelector(
            visibleLayers = visibleLayers,
            displayedZoneCount = displayedFlightZoneOverlayCount(flightZones),
            onToggleLayer = { viewModel.toggleLayer(it) },
            onShowAll = { viewModel.showAllLayers() },
            onHideAll = { viewModel.hideAllLayers() },
            onDismiss = { viewModel.dismissLayerSelector() },
        )
    }

    // 비행구역 상세
    if (
        shouldShowFlightZoneDetail(
            koreaFeaturesEnabled = koreaFeaturesEnabled,
            showZoneDetail = showZoneDetail,
        )
    ) {
        selectedZone?.let { zone ->
            VWorldZoneDetailSheet(
                zone = zone,
                findContact = { name -> findVWorldContact(vWorldContacts, name) },
                onDismiss = {
                    flightZoneOverlayManager.clearSelection()
                    viewModel.dismissZoneDetail()
                },
            )
        }
    }

    // KP 지수 — iOS KPForecastView 정합 (헤더 + 본문)
    if (showKpSheet) {
        val kpIsLoading by kpViewModel.isLoading.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = onDismissKpSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                KpSheetHeader(
                    isLoading = kpIsLoading,
                    onRefresh = { kpViewModel.loadKpData() },
                    onInfo = { showKpInfoSheet = true },
                )
                KpForecastContent(
                    viewModel = kpViewModel,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }

    // 날씨 — iOS WeatherForecastView 정합 (헤더 + 본문)
    if (showWeatherSheet) {
        val weatherIsLoading by weatherViewModel.isLoading.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = onDismissWeatherSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                WeatherSheetHeader(
                    isLoading = weatherIsLoading,
                    onRefresh = { weatherViewModel.refreshWeather() },
                    onInfo = {
                        selectedWeatherInfoTopic = null
                        showWeatherInfoSheet = true
                    },
                )
                WeatherForecastContent(
                    viewModel = weatherViewModel,
                    modifier = Modifier.padding(bottom = 16.dp),
                    onWeatherInfoRequested = { topic ->
                        selectedWeatherInfoTopic = topic
                        showWeatherInfoSheet = true
                    },
                )
            }
        }
    }

    // KP 정보 가이드 — iOS KPForecastView info.circle sheet 정합.
    if (showKpInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showKpInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            KpInfoGuideSheet(onDismiss = { showKpInfoSheet = false })
        }
    }

    // 날씨 정보 가이드 — iOS WeatherForecastView info.circle sheet 정합.
    if (showWeatherInfoSheet) {
        val selectedCategory by weatherViewModel.selectedCategory.collectAsStateWithLifecycle()
        val isUsingGps by weatherViewModel.isUsingGps.collectAsStateWithLifecycle()
        val locationAccuracyMeters by weatherViewModel.locationAccuracyMeters.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showWeatherInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            WeatherInfoGuideSheet(
                onDismiss = { showWeatherInfoSheet = false },
                initialTopic = selectedWeatherInfoTopic,
                category = selectedCategory,
                onCategoryChanged = { weatherViewModel.setCategory(it, refreshWeather = false) },
                isUsingGps = isUsingGps,
                locationAccuracyMeters = locationAccuracyMeters,
            )
        }
    }
}
