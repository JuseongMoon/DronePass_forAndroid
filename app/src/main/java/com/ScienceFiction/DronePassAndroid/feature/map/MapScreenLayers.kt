package com.ScienceFiction.DronePassAndroid.feature.map

import android.graphics.PointF
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.feature.kp.KpForecastContent
import com.ScienceFiction.DronePassAndroid.feature.kp.KpSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.kp.KpViewModel
import com.ScienceFiction.DronePassAndroid.feature.map.component.DroneSelectionDropdown
import com.ScienceFiction.DronePassAndroid.feature.map.component.MapFloatingButtons
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.ShapeOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeDetailSheet
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditScreen
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchToolbar
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchViewModel
import com.ScienceFiction.DronePassAndroid.feature.vworld.FlightZoneLayerSelector
import com.ScienceFiction.DronePassAndroid.feature.vworld.FlightZoneOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.vworld.VWorldZoneDetailSheet
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastContent
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherViewModel
import com.naver.maps.map.NaverMap

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
    val flightZones by viewModel.flightZones.collectAsStateWithLifecycle()
    val visibleLayers by viewModel.visibleLayers.collectAsStateWithLifecycle()

    val isSketchMode by sketchViewModel.isSketchMode.collectAsStateWithLifecycle()
    val activeSketches by sketchViewModel.activeSketches.collectAsStateWithLifecycle()
    val currentDrawingPoints by sketchViewModel.currentDrawingPoints.collectAsStateWithLifecycle()
    val currentColor by sketchViewModel.currentColor.collectAsStateWithLifecycle()
    val currentStrokeWidth by sketchViewModel.currentStrokeWidth.collectAsStateWithLifecycle()
    val currentOpacity by sketchViewModel.currentOpacity.collectAsStateWithLifecycle()

    // 비행구역 오버레이 갱신.
    // visibleLayers 변경에 따른 fetch 는 MapViewModel.flightZoneLoadCollector 가
    // (visibleLayers, currentMapBounds) combine 으로 처리한다. 여기서는 fetch 결과인
    // flightZones 의 변경만 오버레이에 반영한다.
    LaunchedEffect(flightZones, mapReady, visibleLayers) {
        if (!mapReady) return@LaunchedEffect
        if (visibleLayers.isEmpty()) {
            flightZoneOverlayManager.clearAllOverlays()
            return@LaunchedEffect
        }
        flightZones.forEach { (layer, zones) ->
            flightZoneOverlayManager.setZones(layer, zones)
        }
    }

    // 도형 오버레이 갱신 (만료/미시작 필터 적용된 filteredShapes 사용)
    LaunchedEffect(filteredShapes, mapReady) {
        if (mapReady) {
            overlayManager.updateOverlays(filteredShapes)
        }
    }

    // 선택 하이라이트
    LaunchedEffect(selectedShapeId, filteredShapes, mapReady) {
        if (mapReady) {
            overlayManager.setHighlight(selectedShapeId, filteredShapes)
        }
    }

    // 저장된 스케치 오버레이
    LaunchedEffect(activeSketches, mapReady) {
        if (mapReady) {
            sketchOverlayManager.updateOverlays(activeSketches)
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

    // 스케치 모드 진입/종료 시 NaverMap 제스처 토글
    LaunchedEffect(isSketchMode, naverMap) {
        naverMap?.uiSettings?.setAllGesturesEnabled(!isSketchMode)
    }
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
    val highlightedDroneId by viewModel.highlightedDroneId.collectAsStateWithLifecycle()
    val visibleLayerCount by viewModel.visibleLayerCount.collectAsStateWithLifecycle()
    val currentKp by kpViewModel.currentKp.collectAsStateWithLifecycle()
    val kpLevel by kpViewModel.kpLevel.collectAsStateWithLifecycle()
    val weatherData by weatherViewModel.weatherData.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        // 드론 선택 드롭다운 (상단 우측)
        // iOS `droneDropdownView`: .padding(.top, safeAreaInsets.top + dropdownTopPadding)
        // dropdownTopPadding 은 화면 크기별 40~60dp. 안드로이드는 statusBarsPadding 으로
        // 상태바 영역을 보호한 뒤, iOS iPhone 12/13/14/15 기준값(40dp) 을 추가 오프셋으로 사용한다.
        if (!isSketchMode && mapReady) {
            DroneSelectionDropdown(
                activeDrones = activeDrones,
                selectedDroneIds = selectedDroneIds,
                highlightedDroneId = highlightedDroneId,
                onToggleSelection = { viewModel.toggleDroneSelection(it) },
                onToggleHighlight = { viewModel.toggleDroneHighlight(it) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp),
            )
        }

        // 플로팅 버튼 (좌측 비행구역 FAB + 우측 도형/스케치/KP/날씨)
        if (!isSketchMode) {
            MapFloatingButtons(
                onCreateShape = {
                    val center = naverMap?.cameraPosition?.target?.let {
                        Coordinate(it.latitude, it.longitude)
                    } ?: Coordinate(37.5665, 126.9780)
                    viewModel.onCreateShapeRequested(center)
                },
                onEnterSketchMode = { sketchViewModel.enterSketchMode() },
                onShowFlightZoneLayers = { viewModel.toggleLayerSelector() },
                flightZoneVisibleLayerCount = visibleLayerCount,
                currentKpValue = currentKp?.kp,
                kpLevelColor = Color(kpLevel.color.toInt()),
                onShowKpForecast = onShowKpForecast,
                currentWeather = weatherData?.current,
                sunrise = weatherData?.sunrise,
                sunset = weatherData?.sunset,
                onWeatherClick = onShowWeather,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ──────────────────────────────────────────────
// 3. MapSketchInput — 스케치 입력 인터셉터 + 툴바
// ──────────────────────────────────────────────

/**
 * 스케치 모드 진입 시에만 표시되는 터치 인터셉터 + 하단 툴바.
 * isSketchMode = false 면 nothing-rendered (caller 가 if 분기로 호출하지 않아도 안전).
 */
@Composable
internal fun MapSketchInput(
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

    if (!isSketchMode) return

    Box(modifier = modifier.fillMaxSize()) {
        // 터치 인터셉트 레이어
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEraserMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val map = naverMap ?: return@detectDragGestures
                            val latLng = map.projection.fromScreenLocation(
                                PointF(offset.x, offset.y)
                            )
                            val coord = Coordinate.fromLatLng(latLng)
                            if (isEraserMode) {
                                sketchViewModel.deleteSketchAtPoint(coord)
                            } else {
                                sketchViewModel.startDrawing(coord)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val map = naverMap ?: return@detectDragGestures
                            val latLng = map.projection.fromScreenLocation(
                                PointF(change.position.x, change.position.y)
                            )
                            val coord = Coordinate.fromLatLng(latLng)
                            if (isEraserMode) {
                                sketchViewModel.deleteSketchAtPoint(coord)
                            } else {
                                sketchViewModel.continueDrawing(coord)
                            }
                        },
                        onDragEnd = {
                            if (!isEraserMode) sketchViewModel.finishDrawing()
                        },
                    )
                },
        )

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
                .padding(bottom = 16.dp),
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
    kpViewModel: KpViewModel,
    weatherViewModel: WeatherViewModel,
    onNavigateToWeather: () -> Unit = {},
) {
    val selectedShape by viewModel.selectedShape.collectAsStateWithLifecycle()
    val showShapeDetail by viewModel.showShapeDetail.collectAsStateWithLifecycle()
    val showShapeEdit by viewModel.showShapeEdit.collectAsStateWithLifecycle()
    val newShapeCoordinate by viewModel.newShapeCoordinate.collectAsStateWithLifecycle()
    val activeDrones by viewModel.activeDrones.collectAsStateWithLifecycle()
    val reverseGeocodedAddress by viewModel.reverseGeocodedAddress.collectAsStateWithLifecycle()
    val visibleLayers by viewModel.visibleLayers.collectAsStateWithLifecycle()
    val flightZones by viewModel.flightZones.collectAsStateWithLifecycle()
    val showLayerSelector by viewModel.showLayerSelector.collectAsStateWithLifecycle()
    val showZoneDetail by viewModel.showZoneDetail.collectAsStateWithLifecycle()
    val selectedZone by viewModel.selectedZone.collectAsStateWithLifecycle()

    // 도형 상세
    if (showShapeDetail) {
        selectedShape?.let { shape ->
            ShapeDetailSheet(
                shape = shape,
                onEdit = { viewModel.onEditShapeRequested(shape) },
                onDelete = { viewModel.deleteShape(shape) },
                onDismiss = { viewModel.dismissShapeDetail() },
                droneName = viewModel.getDroneName(shape.droneId),
            )
        }
    }

    // 도형 생성/편집
    if (showShapeEdit) {
        ShapeEditScreen(
            shape = selectedShape,
            initialCoordinate = newShapeCoordinate,
            drones = activeDrones,
            reverseGeocodedAddress = reverseGeocodedAddress,
            geocodingApi = viewModel.naverGeocodingApi,
            onSave = { viewModel.saveShape(it) },
            onDismiss = { viewModel.dismissShapeEdit() },
        )
    }

    // 비행구역 레이어 선택
    if (showLayerSelector) {
        FlightZoneLayerSelector(
            visibleLayers = visibleLayers,
            displayedZoneCount = flightZones.values.sumOf { it.size },
            onToggleLayer = { viewModel.toggleLayer(it) },
            onShowAll = { viewModel.showAllLayers() },
            onHideAll = { viewModel.hideAllLayers() },
            onDismiss = { viewModel.dismissLayerSelector() },
        )
    }

    // 비행구역 상세
    if (showZoneDetail) {
        selectedZone?.let { zone ->
            VWorldZoneDetailSheet(
                zone = zone,
                findContact = { name -> viewModel.findContact(name) },
                onDismiss = { viewModel.dismissZoneDetail() },
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
                    onInfo = null,
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
                    onInfo = null,
                )
                WeatherForecastContent(
                    viewModel = weatherViewModel,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}
