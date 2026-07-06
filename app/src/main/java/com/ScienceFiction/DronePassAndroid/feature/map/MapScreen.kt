package com.ScienceFiction.DronePassAndroid.feature.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.PointF
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.feature.kp.KpViewModel
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.ShapeOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchViewModel
import com.ScienceFiction.DronePassAndroid.feature.vworld.FlightZoneOverlayManager
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

internal fun shouldHandleMapLongClickForShapeCreation(
    isSketchMode: Boolean,
): Boolean = !isSketchMode

/**
 * 지도 메인 화면.
 *
 * 본체는 NaverMap factory + lifecycle + 권한 + 위치 추적 + 카메라 이벤트만 담당하고,
 * 각 영역별 state collect 와 UI 렌더링은 [MapScreenLayers] 의 4개 자식 Composable
 * ([MapOverlayEffects], [MapFloatingControls], [MapSketchInput], [MapBottomSheets])
 * 에 위임한다. 자식별로 recomposition 범위가 좁혀져 25+ Flow 중 어느 하나가 emit 해도
 * 부모 함수가 전체 재컴포지션되지 않는다 (B-M6 처리).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    focusShapeId: String? = null,
    onFocusConsumed: () -> Unit = {},
    editShapeId: String? = null,
    onEditShapeConsumed: () -> Unit = {},
    duplicateShapeId: String? = null,
    onDuplicateShapeConsumed: () -> Unit = {},
    onShapeListFocusRequested: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
    sketchViewModel: SketchViewModel = hiltViewModel(),
    weatherViewModel: WeatherViewModel = hiltViewModel(),
    kpViewModel: KpViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val mapView = remember {
        NaverMapSdk.getInstance(context).client =
            NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_MAP_KEY_ID)
        MapView(context)
    }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }

    // OverlayManager 3종은 NaverMap lifecycle 에 묶이므로 Composable 의 remember 로 유지.
    // ViewModel 로 이전하면 MapView/NaverMap 생명주기보다 길어져 detach 가 누락될 위험.
    val overlayManager = remember { ShapeOverlayManager() }
    val sketchOverlayManager = remember { SketchOverlayManager() }
    val flightZoneOverlayManager = remember { FlightZoneOverlayManager() }

    // 디바이스 density 를 SketchOverlayManager 에 주입 (dp→px 정확도 향상)
    val displayDensity = density.density
    LaunchedEffect(sketchOverlayManager, displayDensity) {
        sketchOverlayManager.setDensity(displayDensity)
    }

    // 좌측 하단 컨트롤(내 위치 버튼/로고/축척)이 floating tab bar 위로 떠 보이도록 bottom 패딩 적용.
    // iOS NaverMapView.contentInset(bottom: -33) 대응 — Android 는 floating tab bar(60+15dp) 만큼 추가로 비워야 한다.
    val mapBottomPaddingPx = with(density) { MapBottomContentPadding.roundToPx() }
    val shapeFocusOffsets = resolveShapeFocusOffsets(
        isTablet = configuration.smallestScreenWidthDp >= TabletSmallestWidthDp,
    )
    val shapeFocusOffsetXPx = with(density) { shapeFocusOffsets.x.toPx() }
    val shapeFocusOffsetYPx = with(density) { shapeFocusOffsets.y.toPx() }

    // factory 에서 등록한 NaverMap 리스너 참조 — onDispose 에서 해제할 수 있도록 보관.
    var cameraIdleListener by remember { mutableStateOf<NaverMap.OnCameraIdleListener?>(null) }
    var mapLongClickListener by remember { mutableStateOf<NaverMap.OnMapLongClickListener?>(null) }

    // 위치 권한
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )
    val fineLocationGranted = locationPermissionsState.permissions
        .firstOrNull { it.permission == Manifest.permission.ACCESS_FINE_LOCATION }
        ?.status
        ?.isGranted == true
    val coarseLocationGranted = locationPermissionsState.permissions
        .firstOrNull { it.permission == Manifest.permission.ACCESS_COARSE_LOCATION }
        ?.status
        ?.isGranted == true
    val locationPermissionGranted = hasUsableMapLocationPermission(
        fineLocationGranted = fineLocationGranted,
        coarseLocationGranted = coarseLocationGranted,
    )
    val snackbarHostState = remember { SnackbarHostState() }

    // KP/Weather 시트는 부모에서 보유 (FloatingControls 가 열고 BottomSheets 가 표시)
    var showKpSheet by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }

    // 화면 항상 켜기 설정 (Phase 3.4 B-M3 양방향 적용)
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val activeShapesForPendingRequests by viewModel.activeShapes.collectAsStateWithLifecycle()
    val visibleShapesForPendingRequests by viewModel.filteredShapes.collectAsStateWithLifecycle()
    val pendingNewShapeRequest by viewModel.pendingNewShapeRequest.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // 권한 요청 (앱 초기 진입 시 1회)
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // 비행구역 에러 메시지 → Snackbar
    LaunchedEffect(Unit) {
        viewModel.flightZonesError.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedShapeFocusEvent.collect { shapeId ->
            onShapeListFocusRequested(shapeId)
        }
    }

    // 오버레이 매니저 콜백 — SideEffect 로 매 successful recomposition 후 최신 ViewModel 참조 할당
    SideEffect {
        overlayManager.onShapeTapped = { shapeId -> viewModel.onShapeOverlayTapped(shapeId) }
        flightZoneOverlayManager.onZoneTapped = { zone -> viewModel.onZoneSelected(zone) }
    }

    // 탭 간 연동: 저장 목록에서 도형 선택 시 해당 도형으로 포커스
    LaunchedEffect(focusShapeId, mapReady, visibleShapesForPendingRequests, activeShapesForPendingRequests) {
        if (focusShapeId != null && mapReady) {
            val visibleShapes = visibleShapesForPendingRequests
            val shape = resolvePendingMapShapeRequestTarget(focusShapeId, visibleShapes)
            if (shape != null) {
                viewModel.moveCameraToShape(shape, skipIfAlreadyFocused = true)
                onFocusConsumed()
            } else if (shouldConsumeMissingMapShapeRequest(activeShapesForPendingRequests)) {
                onFocusConsumed()
            }
        }
    }

    // 탭 간 연동: 저장 목록에서 편집 진입 시 상세 시트 건너뛰고 바로 편집 시트.
    // (focusShapeId 경로는 onShapeSelected → ShapeDetailSheet 자동 표시되므로 별도 채널 필요)
    LaunchedEffect(editShapeId, mapReady, visibleShapesForPendingRequests, activeShapesForPendingRequests) {
        if (editShapeId != null && mapReady) {
            val visibleShapes = visibleShapesForPendingRequests
            val shape = resolvePendingMapShapeRequestTarget(editShapeId, visibleShapes)
            if (shape != null) {
                viewModel.onEditShapeRequested(shape)
                viewModel.moveCameraToShape(shape)
                onEditShapeConsumed()
            } else if (shouldConsumeMissingMapShapeRequest(activeShapesForPendingRequests)) {
                onEditShapeConsumed()
            }
        }
    }

    // 탭 간 연동: 저장 목록 상세의 복제는 지도 상세와 동일하게 복제 편집 시트로 진입.
    LaunchedEffect(duplicateShapeId, mapReady, visibleShapesForPendingRequests, activeShapesForPendingRequests) {
        if (duplicateShapeId != null && mapReady) {
            val visibleShapes = visibleShapesForPendingRequests
            val shape = resolvePendingMapShapeRequestTarget(duplicateShapeId, visibleShapes)
            if (shape != null) {
                viewModel.onDuplicateRequested(shape)
                viewModel.moveCameraToShape(shape)
                onDuplicateShapeConsumed()
            } else if (shouldConsumeMissingMapShapeRequest(activeShapesForPendingRequests)) {
                onDuplicateShapeConsumed()
            }
        }
    }

    // 위치 추적 설정 — 권한과 mapReady 가 모두 충족된 시점에 단 1회.
    LaunchedEffect(locationPermissionGranted, mapReady) {
        if (locationPermissionGranted && mapReady) {
            naverMap?.let { setupLocationTracking(it, context) }
        }
    }

    // 카메라 이벤트 (MoveTo / MoveWithoutZoom)
    LaunchedEffect(shapeFocusOffsetXPx, shapeFocusOffsetYPx) {
        viewModel.cameraEvent.collectLatest { event ->
            naverMap?.let { map ->
                when (event) {
                    is CameraEvent.MoveTo -> {
                        val cameraUpdate = CameraUpdate.scrollAndZoomTo(
                            LatLng(event.coordinate.latitude, event.coordinate.longitude),
                            event.zoom,
                        ).animate(CameraAnimation.Easing, 500)
                        map.moveCamera(cameraUpdate)
                    }
                    is CameraEvent.MoveToShape -> {
                        val center = LatLng(event.coordinate.latitude, event.coordinate.longitude)

                        val zoomUpdate = CameraUpdate.zoomTo(event.zoom)
                            .animate(CameraAnimation.Easing, ShapeFocusZoomDurationMs)
                        map.moveCamera(zoomUpdate)

                        delay(ShapeFocusSecondStepDelayMs)
                        event.highlightShapeId?.let(viewModel::selectShapeForMapFocus)

                        val offsetCenter = offsetLatLng(
                            center = center,
                            map = map,
                            offsetX = shapeFocusOffsetXPx,
                            offsetY = shapeFocusOffsetYPx,
                        )
                        val cameraUpdate = CameraUpdate.toCameraPosition(
                            CameraPosition(offsetCenter, event.zoom),
                        ).animate(CameraAnimation.Easing, ShapeFocusMoveDurationMs)
                        map.moveCamera(cameraUpdate)
                    }
                    is CameraEvent.MoveWithoutZoom -> {
                        val center = LatLng(event.coordinate.latitude, event.coordinate.longitude)
                        val offsetCenter = offsetLatLng(
                            center = center,
                            map = map,
                            offsetX = shapeFocusOffsetXPx,
                            offsetY = shapeFocusOffsetYPx,
                        )
                        val cameraUpdate = CameraUpdate.toCameraPosition(
                            CameraPosition(offsetCenter, map.cameraPosition.zoom),
                        ).animate(CameraAnimation.Easing, 500)
                        map.moveCamera(cameraUpdate)
                    }
                }
            }
        }
    }

    // ── 자식 1: 오버레이 갱신 LaunchedEffect 묶음 (UI 없음) ──
    MapOverlayEffects(
        naverMap = naverMap,
        mapReady = mapReady,
        overlayManager = overlayManager,
        sketchOverlayManager = sketchOverlayManager,
        flightZoneOverlayManager = flightZoneOverlayManager,
        viewModel = viewModel,
        sketchViewModel = sketchViewModel,
    )

    pendingNewShapeRequest?.let { request ->
        val addressNotFoundFallback = stringResource(R.string.map_address_not_found)
        val titleRes = when (request.dialogType) {
            NewShapeConfirmDialogType.CONFIRM -> R.string.map_new_shape_alert_title
            NewShapeConfirmDialogType.GEOCODING_FAILED -> R.string.map_address_search_failed_title
        }
        val messageRes = when (request.dialogType) {
            NewShapeConfirmDialogType.CONFIRM -> R.string.map_new_shape_alert_message
            NewShapeConfirmDialogType.GEOCODING_FAILED -> R.string.map_address_search_failed_message
        }

        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingNewShapeRequest() },
            title = { Text(stringResource(titleRes)) },
            text = { Text(stringResource(messageRes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmPendingNewShapeRequest(addressNotFoundFallback)
                    }
                ) {
                    Text(stringResource(R.string.common_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPendingNewShapeRequest() }) {
                    Text(stringResource(R.string.common_no))
                }
            },
        )
    }

    // 메인 레이아웃 (Box - MainScreen 이 이미 Scaffold 제공)
    Box(modifier = Modifier.fillMaxSize()) {
        // 네이버 지도
        AndroidView(
            factory = {
                mapView.apply {
                    onCreate(null)
                    getMapAsync { map ->
                        naverMap = map
                        overlayManager.setMap(map)
                        sketchOverlayManager.setMap(map)
                        flightZoneOverlayManager.setMap(map)

                        Log.d("NaverMapDebug", "네이버 지도 준비 완료")

                        map.cameraPosition = CameraPosition(
                            LatLng(MapDefaultSeoulLatitude, MapDefaultSeoulLongitude),
                            MapInitialZoomLevel,
                        )
                        map.uiSettings.apply {
                            isLocationButtonEnabled = true
                            isZoomControlEnabled = true
                            isCompassEnabled = true
                        }
                        map.setContentPadding(0, 0, 0, mapBottomPaddingPx)
                        viewModel.updateCurrentBoundsFrom(map)
                        map.setOnSymbolClickListener {
                            // iOS NaverMapView.Coordinator.didTap symbol 과 동일하게 POI 라벨 탭을 소비한다.
                            shouldConsumeNaverMapSymbolTap()
                        }

                        val cameraListener = NaverMap.OnCameraIdleListener {
                            viewModel.updateCurrentBoundsFrom(map)
                            // iOS removeOverlaysOutsideViewport(buffer=0.2) 매핑:
                            // viewport 밖 폴리곤 가시성만 토글해 그리기 비용을 줄인다.
                            // 인스턴스는 캐시에 유지하므로 카메라 재진입 시 즉시 복원된다.
                            flightZoneOverlayManager.setOutOfBoundsVisibility(map.contentBounds.expand(0.2))
                        }
                        map.addOnCameraIdleListener(cameraListener)
                        cameraIdleListener = cameraListener

                        val longClickListener = NaverMap.OnMapLongClickListener { _, latLng ->
                            if (
                                shouldHandleMapLongClickForShapeCreation(
                                    isSketchMode = sketchViewModel.isSketchMode.value,
                                )
                            ) {
                                viewModel.onCreateShapeAtCoordinate(Coordinate.fromLatLng(latLng))
                            }
                        }
                        map.setOnMapLongClickListener(longClickListener)
                        mapLongClickListener = longClickListener

                        mapReady = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── 자식 3: 스케치 입력 인터셉터 + 툴바 (isSketchMode = true 일 때만 내부에서 렌더) ──
        MapSketchInput(
            mapView = mapView,
            naverMap = naverMap,
            sketchViewModel = sketchViewModel,
        )

        // 지도 로딩 중 인디케이터
        if (!mapReady) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // ── 자식 2: 드론 드롭다운 + FAB + KP/Weather 카드 ──
        MapFloatingControls(
            mapReady = mapReady,
            naverMap = naverMap,
            viewModel = viewModel,
            sketchViewModel = sketchViewModel,
            kpViewModel = kpViewModel,
            weatherViewModel = weatherViewModel,
            onShowKpForecast = { showKpSheet = true },
            onShowWeather = { showWeatherSheet = true },
            modifier = Modifier.fillMaxSize(),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }

    // ── 자식 4: BottomSheets 6종 ──
    MapBottomSheets(
        showKpSheet = showKpSheet,
        onDismissKpSheet = { showKpSheet = false },
        showWeatherSheet = showWeatherSheet,
        onDismissWeatherSheet = { showWeatherSheet = false },
        viewModel = viewModel,
        flightZoneOverlayManager = flightZoneOverlayManager,
        kpViewModel = kpViewModel,
        weatherViewModel = weatherViewModel,
    )

    // 생명주기 관리 - LifecycleOwner 연동
    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            kpViewModel.startAutoRefresh()
            weatherViewModel.startAutoRefresh()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    kpViewModel.startAutoRefresh()
                    weatherViewModel.startAutoRefresh()
                }
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    kpViewModel.stopAutoRefresh()
                    weatherViewModel.stopAutoRefresh()
                }
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            // factory 에서 등록한 NaverMap 리스너 제거 (중복 등록 방지)
            naverMap?.let { map ->
                cameraIdleListener?.let { map.removeOnCameraIdleListener(it) }
                map.onMapLongClickListener = null
                map.onSymbolClickListener = null
            }
            cameraIdleListener = null
            mapLongClickListener = null

            // OverlayManager 3종 detach → NaverMap 참조 해제 (Activity 누수 방지)
            overlayManager.detach()
            sketchOverlayManager.detach()
            flightZoneOverlayManager.detach()

            lifecycleOwner.lifecycle.removeObserver(observer)
            kpViewModel.stopAutoRefresh()
            weatherViewModel.stopAutoRefresh()
        }
    }
}

private fun offsetLatLng(
    center: LatLng,
    map: NaverMap,
    offsetX: Float,
    offsetY: Float,
): LatLng {
    val point = map.projection.toScreenLocation(center)
    if (point.x.isNaN() || point.y.isNaN()) return center

    val offsetPoint = PointF(point.x + offsetX, point.y + offsetY)
    val offsetCenter = map.projection.fromScreenLocation(offsetPoint)
    return if (offsetCenter.isValid) offsetCenter else center
}

private fun MapViewModel.updateCurrentBoundsFrom(map: NaverMap) {
    val bounds = map.contentBounds
    onMapBoundsChanged(
        southWestLat = bounds.southWest.latitude,
        southWestLon = bounds.southWest.longitude,
        northEastLat = bounds.northEast.latitude,
        northEastLon = bounds.northEast.longitude,
    )
}

internal fun hasUsableMapLocationPermission(
    fineLocationGranted: Boolean,
    coarseLocationGranted: Boolean,
): Boolean {
    return fineLocationGranted || coarseLocationGranted
}

internal fun shouldRequestCurrentLocationFallback(lastLocationAvailable: Boolean): Boolean {
    return !lastLocationAvailable
}

private fun centerMapOnUserLocation(map: NaverMap, location: Location) {
    map.cameraPosition = CameraPosition(
        LatLng(location.latitude, location.longitude),
        MapUserLocationZoomLevel,
    )
}

@SuppressLint("MissingPermission")
private fun setupLocationTracking(map: NaverMap, context: android.content.Context) {
    try {
        val activity = context as? Activity ?: run {
            Log.e("MapScreen", "Context is not an Activity, cannot setup location tracking")
            return
        }

        val locationSource = FusedLocationSource(activity, LOCATION_PERMISSION_REQUEST_CODE)
        map.locationSource = locationSource
        map.locationTrackingMode = MapInitialLocationTrackingMode

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fun requestCurrentLocationFallback() {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token,
            ).addOnSuccessListener { currentLocation ->
                currentLocation?.let { centerMapOnUserLocation(map, it) }
            }.addOnFailureListener { error ->
                Log.w("MapScreen", "현재 위치 fallback 조회 실패: ${error.message}")
            }
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val lastLocationAvailable = location != null
            if (lastLocationAvailable) {
                location?.let { centerMapOnUserLocation(map, it) }
            } else if (shouldRequestCurrentLocationFallback(lastLocationAvailable)) {
                requestCurrentLocationFallback()
            }
        }.addOnFailureListener { error ->
            Log.w("MapScreen", "마지막 위치 조회 실패, 현재 위치 fallback 시도: ${error.message}")
            requestCurrentLocationFallback()
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "위치 추적 설정 실패: ${e.message}", e)
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
private const val TabletSmallestWidthDp = 600
internal const val MapDefaultSeoulLatitude = 37.575563
internal const val MapDefaultSeoulLongitude = 126.976793
internal const val MapInitialZoomLevel = 12.0
internal const val MapUserLocationZoomLevel = 16.0
internal val MapInitialLocationTrackingMode = LocationTrackingMode.NoFollow
internal const val ShapeFocusZoomDurationMs = 300L
internal const val ShapeFocusSecondStepDelayMs = 300L
internal const val ShapeFocusMoveDurationMs = 500L

internal val MapBottomContentPadding = 45.dp
private val ShapeFocusPhoneOffsetY = 200.dp
private val ShapeFocusTabletOffsetX = (-100).dp

internal data class ShapeFocusOffsets(
    val x: Dp,
    val y: Dp,
)

internal fun resolveShapeFocusOffsets(isTablet: Boolean): ShapeFocusOffsets {
    return if (isTablet) {
        ShapeFocusOffsets(x = ShapeFocusTabletOffsetX, y = 0.dp)
    } else {
        ShapeFocusOffsets(x = 0.dp, y = ShapeFocusPhoneOffsetY)
    }
}

/**
 * 사각형 [LatLngBounds] 를 양 방향으로 [ratio] 만큼 확장한다 (iOS 의 20% 버퍼 매핑).
 * 카메라 viewport 가장자리 근처 폴리곤이 카메라 idle 직후 잠시 사라지는 깜빡임을 막는다.
 */
private fun LatLngBounds.expand(ratio: Double): LatLngBounds {
    val south = southWest.latitude
    val west = southWest.longitude
    val north = northEast.latitude
    val east = northEast.longitude
    val latSpan = (north - south) * ratio
    val lonSpan = (east - west) * ratio
    return LatLngBounds(
        LatLng(south - latSpan, west - lonSpan),
        LatLng(north + latSpan, east + lonSpan),
    )
}
