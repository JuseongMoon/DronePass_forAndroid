package com.ScienceFiction.DronePassAndroid.feature.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
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
import com.ScienceFiction.DronePassAndroid.feature.kp.KpForecastContent
import com.ScienceFiction.DronePassAndroid.feature.kp.KpViewModel
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastContent
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.util.FusedLocationSource

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    focusShapeId: String? = null,
    onFocusConsumed: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
    sketchViewModel: SketchViewModel = hiltViewModel(),
    weatherViewModel: WeatherViewModel = hiltViewModel(),
    kpViewModel: KpViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        NaverMapSdk.getInstance(context).client =
            NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_MAP_CLIENT_ID)
        MapView(context)
    }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    val overlayManager = remember { ShapeOverlayManager() }
    val sketchOverlayManager = remember { SketchOverlayManager() }
    val flightZoneOverlayManager = remember { FlightZoneOverlayManager() }

    // ViewModel 상태 수집
    val activeShapes by viewModel.activeShapes.collectAsStateWithLifecycle()
    val filteredShapes by viewModel.filteredShapes.collectAsStateWithLifecycle()
    val selectedShapeId by viewModel.selectedShapeId.collectAsStateWithLifecycle()
    val selectedShape by viewModel.selectedShape.collectAsStateWithLifecycle()
    val showShapeDetail by viewModel.showShapeDetail.collectAsStateWithLifecycle()
    val showShapeEdit by viewModel.showShapeEdit.collectAsStateWithLifecycle()
    val newShapeCoordinate by viewModel.newShapeCoordinate.collectAsStateWithLifecycle()
    val activeDrones by viewModel.activeDrones.collectAsStateWithLifecycle()
    val selectedDroneIds by viewModel.selectedDroneIds.collectAsStateWithLifecycle()
    val highlightedDroneId by viewModel.highlightedDroneId.collectAsStateWithLifecycle()
    val reverseGeocodedAddress by viewModel.reverseGeocodedAddress.collectAsStateWithLifecycle()

    // 스케치 ViewModel 상태 수집
    val isSketchMode by sketchViewModel.isSketchMode.collectAsStateWithLifecycle()
    val isEraserMode by sketchViewModel.isEraserMode.collectAsStateWithLifecycle()
    val activeSketches by sketchViewModel.activeSketches.collectAsStateWithLifecycle()
    val currentDrawingPoints by sketchViewModel.currentDrawingPoints.collectAsStateWithLifecycle()
    val currentColor by sketchViewModel.currentColor.collectAsStateWithLifecycle()
    val currentStrokeWidth by sketchViewModel.currentStrokeWidth.collectAsStateWithLifecycle()
    val currentOpacity by sketchViewModel.currentOpacity.collectAsStateWithLifecycle()
    val canUndo by sketchViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by sketchViewModel.canRedo.collectAsStateWithLifecycle()

    // KP ViewModel 상태 수집
    val currentKp by kpViewModel.currentKp.collectAsStateWithLifecycle()
    val kpLevel by kpViewModel.kpLevel.collectAsStateWithLifecycle()

    // Sheet 상태
    var showKpSheet by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }

    // 날씨 ViewModel 상태 수집
    val weatherData by weatherViewModel.weatherData.collectAsStateWithLifecycle()

    // 비행구역 ViewModel 상태 수집
    val visibleLayers by viewModel.visibleLayers.collectAsStateWithLifecycle()
    val flightZones by viewModel.flightZones.collectAsStateWithLifecycle()
    val showLayerSelector by viewModel.showLayerSelector.collectAsStateWithLifecycle()
    val showZoneDetail by viewModel.showZoneDetail.collectAsStateWithLifecycle()
    val selectedZone by viewModel.selectedZone.collectAsStateWithLifecycle()

    // DataStore 설정값 수집
    val showFlightZoneLayersSetting by viewModel.showFlightZoneLayersSetting.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

    // 위치 권한 상태
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Snackbar 상태
    val snackbarHostState = remember { SnackbarHostState() }

    // 권한 요청 다이얼로그 표시 상태
    var showRationaleDialog by remember { mutableStateOf(false) }

    // 권한이 허용되었는지 확인
    val allPermissionsGranted = locationPermissionsState.permissions.all { it.status.isGranted }

    // 화면 항상 켜기 설정 적용
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }

    // 권한 요청
    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            if (locationPermissionsState.permissions.any { it.status.shouldShowRationale }) {
                showRationaleDialog = true
            } else {
                locationPermissionsState.launchMultiplePermissionRequest()
            }
        }
    }

    // 비행구역 에러 메시지 수집
    LaunchedEffect(Unit) {
        viewModel.flightZonesError.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 오버레이 매니저 콜백 설정
    LaunchedEffect(overlayManager) {
        overlayManager.onShapeTapped = { shapeId ->
            viewModel.onShapeSelected(shapeId)
        }
    }

    // 비행구역 오버레이 매니저 콜백 설정
    LaunchedEffect(flightZoneOverlayManager) {
        flightZoneOverlayManager.onZoneTapped = { zone ->
            viewModel.onZoneSelected(zone)
        }
    }

    // 비행구역 오버레이 갱신: flightZones 변경 시 지도에 반영
    // showFlightZoneLayersSetting이 OFF이면 모든 비행구역 오버레이 클리어
    LaunchedEffect(flightZones, mapReady, showFlightZoneLayersSetting) {
        if (mapReady) {
            if (!showFlightZoneLayersSetting) {
                flightZoneOverlayManager.clearAllOverlays()
            } else {
                flightZones.forEach { (layer, zones) ->
                    flightZoneOverlayManager.setZones(layer, zones)
                }
            }
        }
    }

    // visibleLayers 변경 시 비행구역 로드 트리거
    LaunchedEffect(visibleLayers, mapReady) {
        if (mapReady) {
            val map = naverMap ?: return@LaunchedEffect
            val bounds = map.contentBounds
            viewModel.onMapBoundsChanged(
                southWestLat = bounds.southWest.latitude,
                southWestLon = bounds.southWest.longitude,
                northEastLat = bounds.northEast.latitude,
                northEastLon = bounds.northEast.longitude
            )
        }
    }

    // 오버레이 갱신: filteredShapes 변경 시 지도에 반영 (만료/미시작 도형 필터 적용)
    LaunchedEffect(filteredShapes, mapReady) {
        if (mapReady) {
            overlayManager.updateOverlays(filteredShapes)
        }
    }

    // 선택된 도형 하이라이트 표시
    LaunchedEffect(selectedShapeId, filteredShapes, mapReady) {
        if (mapReady) {
            overlayManager.setHighlight(selectedShapeId, filteredShapes)
        }
    }

    // 스케치 오버레이 갱신: activeSketches 변경 시 지도에 반영
    LaunchedEffect(activeSketches, mapReady) {
        if (mapReady) {
            sketchOverlayManager.updateOverlays(activeSketches)
        }
    }

    // 스케치 프리뷰 오버레이 갱신: 그리는 중 실시간 업데이트
    LaunchedEffect(currentDrawingPoints, currentColor, currentStrokeWidth, currentOpacity) {
        if (currentDrawingPoints.isNotEmpty()) {
            sketchOverlayManager.updatePreviewOverlay(
                points = currentDrawingPoints,
                color = currentColor,
                strokeWidth = currentStrokeWidth,
                opacity = currentOpacity
            )
        } else {
            sketchOverlayManager.clearPreviewOverlay()
        }
    }

    // 스케치 모드 진입/종료 시 지도 제스처 토글
    LaunchedEffect(isSketchMode) {
        naverMap?.uiSettings?.setAllGesturesEnabled(!isSketchMode)
    }

    // 탭 간 연동: 저장 목록에서 도형 선택 시 해당 도형으로 포커스
    LaunchedEffect(focusShapeId, mapReady) {
        if (focusShapeId != null && mapReady) {
            val shape = activeShapes.find { it.id == focusShapeId }
            if (shape != null) {
                viewModel.onShapeSelected(shape.id)
                viewModel.moveCameraToShape(shape)
            }
            onFocusConsumed()
        }
    }

    // 카메라 이벤트 수집
    LaunchedEffect(Unit) {
        viewModel.cameraEvent.collect { event ->
            naverMap?.let { map ->
                when (event) {
                    is CameraEvent.MoveTo -> {
                        val cameraUpdate = CameraUpdate.scrollAndZoomTo(
                            LatLng(event.coordinate.latitude, event.coordinate.longitude),
                            event.zoom
                        ).animate(CameraAnimation.Easing, 500)
                        map.moveCamera(cameraUpdate)
                    }
                    is CameraEvent.MoveWithoutZoom -> {
                        val cameraUpdate = CameraUpdate.scrollTo(
                            LatLng(event.coordinate.latitude, event.coordinate.longitude)
                        ).animate(CameraAnimation.Easing, 500)
                        map.moveCamera(cameraUpdate)
                    }
                }
            }
        }
    }

    // 권한 설명 다이얼로그
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text(stringResource(R.string.map_permission_dialog_title)) },
            text = { Text(stringResource(R.string.map_permission_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        locationPermissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text(stringResource(R.string.map_permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 메인 레이아웃 (Box - MainScreen이 이미 Scaffold 제공)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 네이버 지도 표시
        AndroidView(
            factory = {
                mapView.apply {
                    onCreate(null)
                    getMapAsync { map ->
                        naverMap = map
                        overlayManager.setMap(map)
                        sketchOverlayManager.setMap(map)
                        flightZoneOverlayManager.setMap(map)
                        mapReady = true

                        Log.d("NaverMapDebug", "네이버 지도 준비 완료")

                        // 기본 카메라 위치 설정 (서울 시청)
                        val defaultPosition = CameraPosition(
                            LatLng(37.5665, 126.9780),
                            13.0
                        )
                        map.cameraPosition = defaultPosition

                        // 위치 권한이 허용된 경우 위치 추적 활성화
                        if (allPermissionsGranted) {
                            setupLocationTracking(map, context)
                        }

                        // 지도 UI 설정
                        map.uiSettings.apply {
                            isLocationButtonEnabled = true
                            isZoomControlEnabled = true
                            isCompassEnabled = true
                        }

                        // 카메라 이동 완료 시 비행구역 로드 (Debounce는 ViewModel에서 처리)
                        map.addOnCameraIdleListener {
                            val bounds = map.contentBounds
                            viewModel.onMapBoundsChanged(
                                southWestLat = bounds.southWest.latitude,
                                southWestLon = bounds.southWest.longitude,
                                northEastLat = bounds.northEast.latitude,
                                northEastLon = bounds.northEast.longitude
                            )
                        }

                        // 지도 롱프레스 시 해당 좌표에 도형 생성 + 역지오코딩
                        map.setOnMapLongClickListener { _, latLng ->
                            val coordinate = Coordinate.fromLatLng(latLng)
                            viewModel.onCreateShapeAtCoordinate(coordinate)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                // 권한 상태가 변경되었을 때 위치 추적 재설정
                if (allPermissionsGranted && naverMap != null) {
                    setupLocationTracking(naverMap!!, context)
                }
            }
        )

        // 스케치 모드일 때 터치 인터셉트 레이어
        if (isSketchMode) {
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
                                val coordinate = Coordinate.fromLatLng(latLng)

                                if (isEraserMode) {
                                    sketchViewModel.deleteSketchAtPoint(coordinate)
                                } else {
                                    sketchViewModel.startDrawing(coordinate)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val map = naverMap ?: return@detectDragGestures
                                val latLng = map.projection.fromScreenLocation(
                                    PointF(
                                        change.position.x,
                                        change.position.y
                                    )
                                )
                                val coordinate = Coordinate.fromLatLng(latLng)

                                if (isEraserMode) {
                                    sketchViewModel.deleteSketchAtPoint(coordinate)
                                } else {
                                    sketchViewModel.continueDrawing(coordinate)
                                }
                            },
                            onDragEnd = {
                                if (!isEraserMode) {
                                    sketchViewModel.finishDrawing()
                                }
                            }
                        )
                    }
            )
        }

        // 지도 로딩 중 인디케이터
        if (!mapReady) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 권한이 거부된 경우 안내 메시지
        if (!allPermissionsGranted) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.map_permission_required))
                Button(
                    onClick = { locationPermissionsState.launchMultiplePermissionRequest() }
                ) {
                    Text(stringResource(R.string.map_permission_request))
                }
            }
        }

        // 드론 선택 드롭다운 (상단 우측) - 스케치 모드가 아닐 때만 표시
        if (!isSketchMode && mapReady) {
            DroneSelectionDropdown(
                activeDrones = activeDrones,
                selectedDroneIds = selectedDroneIds,
                highlightedDroneId = highlightedDroneId,
                onToggleSelection = { viewModel.toggleDroneSelection(it) },
                onToggleHighlight = { viewModel.toggleDroneHighlight(it) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            )
        }

        // 플로팅 버튼 (도형 추가 + 스케치 + KP + 날씨 + 레이어) - 스케치 모드가 아닐 때만 표시
        if (!isSketchMode) {
            MapFloatingButtons(
                onCreateShape = {
                    // 현재 지도 중심 좌표를 새 도형의 좌표로 사용
                    val centerCoordinate = naverMap?.cameraPosition?.target?.let {
                        Coordinate(it.latitude, it.longitude)
                    } ?: Coordinate(37.5665, 126.9780)
                    viewModel.onCreateShapeRequested(centerCoordinate)
                },
                onEnterSketchMode = {
                    sketchViewModel.enterSketchMode()
                },
                onShowFlightZoneLayers = {
                    viewModel.toggleLayerSelector()
                },
                flightZoneLayersActive = visibleLayers.isNotEmpty(),
                showFlightZoneLayerButton = showFlightZoneLayersSetting,
                currentKpValue = currentKp?.kp,
                kpLevelColor = Color(kpLevel.color.toInt()),
                onShowKpForecast = { showKpSheet = true },
                currentWeather = weatherData?.current,
                sunrise = weatherData?.sunrise,
                sunset = weatherData?.sunset,
                onWeatherClick = { showWeatherSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }

        // Snackbar (에러 메시지 표시)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // 스케치 모드일 때 툴바 표시 (하단)
        if (isSketchMode) {
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
                    .padding(bottom = 16.dp)
            )
        }
    }

    // 도형 상세 BottomSheet
    if (showShapeDetail) {
        selectedShape?.let { shape ->
            ShapeDetailSheet(
                shape = shape,
                onEdit = {
                    viewModel.onEditShapeRequested(shape)
                },
                onDelete = {
                    viewModel.deleteShape(shape)
                },
                onDismiss = {
                    viewModel.dismissShapeDetail()
                }
            )
        }
    }

    // 도형 생성/편집 BottomSheet
    if (showShapeEdit) {
        ShapeEditScreen(
            shape = selectedShape,
            initialCoordinate = newShapeCoordinate,
            drones = activeDrones,
            reverseGeocodedAddress = reverseGeocodedAddress,
            geocodingApi = viewModel.naverGeocodingApi,
            onSave = { shape ->
                viewModel.saveShape(shape)
            },
            onDismiss = {
                viewModel.dismissShapeEdit()
            }
        )
    }

    // 비행구역 레이어 선택 BottomSheet
    if (showLayerSelector) {
        FlightZoneLayerSelector(
            visibleLayers = visibleLayers,
            onToggleLayer = { layer -> viewModel.toggleLayer(layer) },
            onShowAll = { viewModel.showAllLayers() },
            onHideAll = { viewModel.hideAllLayers() },
            onDismiss = { viewModel.dismissLayerSelector() }
        )
    }

    // 비행구역 상세 BottomSheet
    if (showZoneDetail) {
        selectedZone?.let { zone ->
            VWorldZoneDetailSheet(
                zone = zone,
                onDismiss = { viewModel.dismissZoneDetail() }
            )
        }
    }

    // KP 지수 BottomSheet
    if (showKpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showKpSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            KpForecastContent(
                viewModel = kpViewModel,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    // 날씨 BottomSheet
    if (showWeatherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWeatherSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            WeatherForecastContent(
                viewModel = weatherViewModel,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    // 생명주기 관리 - LifecycleOwner 연동
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            overlayManager.clearOverlays()
            sketchOverlayManager.clearOverlays()
            flightZoneOverlayManager.clearAllOverlays()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@SuppressLint("MissingPermission")
private fun setupLocationTracking(
    map: NaverMap,
    context: android.content.Context
) {
    try {
        val activity = context as? Activity ?: run {
            Log.e("MapScreen", "Context is not an Activity, cannot setup location tracking")
            return
        }

        val locationSource = FusedLocationSource(activity, LOCATION_PERMISSION_REQUEST_CODE)
        map.locationSource = locationSource
        map.locationTrackingMode = LocationTrackingMode.Follow

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val cameraPosition = CameraPosition(
                    LatLng(it.latitude, it.longitude),
                    15.0
                )
                map.cameraPosition = cameraPosition
            }
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "위치 추적 설정 실패: ${e.message}", e)
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
