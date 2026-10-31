package com.ScienceFiction.DronePassAndroid.feature.shape

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.parseMapOverlayColorSafe
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapOptions
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import kotlin.coroutines.resume
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal enum class ShapeShareMapStyle(
    val mapType: NaverMap.MapType,
    /**
     * 지도 뷰를 몇 배 크기로 그린 뒤 줄여 보여 줄지.
     * 위성 타일은 화면 배율만큼 확대 보간돼 흐리므로, 2배로 그리고 줌을 한 단계 올려 더 상세한 타일을 쓴다.
     * 기본 지도는 글자·도로 굵기가 같이 줄어들어 1배로 둔다 (iOS ShapeShareMapStyle 정합)
     */
    val renderScale: Int,
    @StringRes val titleRes: Int,
) {
    BASIC(NaverMap.MapType.Basic, 1, R.string.shape_share_map_type_basic),
    SATELLITE(NaverMap.MapType.Satellite, 2, R.string.shape_share_map_type_satellite),
    HYBRID(NaverMap.MapType.Hybrid, 2, R.string.shape_share_map_type_hybrid),
    ;

    /** 2배로 그려 줄이면 지명 글자도 같이 작아지므로 그만큼 키운다 (SDK 허용 범위 0~2) */
    val symbolScale: Float get() = renderScale.toFloat()
}

/** 원 둘레와 지도 가장자리 사이 여백(dp, 1배 기준) */
private val ShapeShareMapFitPadding = 40.dp
/** 메인 지도 원 오버레이 일반 상태와 같은 외곽선 두께(dp, 1배 기준) */
private val ShapeShareMapOutlineWidth = 2.dp
/** 메인 지도 원 오버레이 일반 상태와 같은 채우기 불투명도(0.3) */
private const val ShapeShareMapFillAlpha = 0x4D
private const val ShapeShareMapMarkerZoom = 15.0
private const val ShapeShareSnapshotTimeoutMs = 5_000L

/** 미리보기 지도의 로딩 상태와 스냅샷을 담당 */
@Stable
internal class ShapeShareMapController {
    /** 지도 최초 로딩이 끝났는지. 이후 지도 유형 변경은 사용자가 미리보기로 보고 공유한다 */
    var isReady by mutableStateOf(false)
        internal set
    internal var naverMap: NaverMap? = null

    /**
     * 컨트롤은 빼고 지도만 찍는다 (네이버 로고는 지도에 포함된다). 지도 뷰 크기(배율 포함) 그대로의 비트맵.
     * SDK 가 콜백을 주지 않으면 공유 버튼이 계속 막히므로 [ShapeShareSnapshotTimeoutMs] 뒤에 null(오류 안내)로 끝낸다.
     */
    suspend fun snapshot(): Bitmap? {
        val map = naverMap ?: return null
        return withTimeoutOrNull(ShapeShareSnapshotTimeoutMs) {
            suspendCancellableCoroutine { continuation ->
                map.takeSnapshot(false) { bitmap -> if (continuation.isActive) continuation.resume(bitmap) }
            }
        }
    }
}

/**
 * 공유 카드용 지도. 공유할 도형 하나만 그린다(다른 도형·스케치·비행구역 없음).
 * 위성 계열은 지도 뷰를 [ShapeShareMapStyle.renderScale] 배로 만들고 View 배율로 줄여 같은 영역을 보여 준다.
 * View 배율이 적용되도록 TextureView 로 그리고, 줄인 뷰도 터치 좌표가 맞도록 FrameLayout 안에 둔다.
 */
@Composable
internal fun ShapeShareMap(
    shape: ShapeModel,
    colorHex: String,
    style: ShapeShareMapStyle,
    controller: ShapeShareMapController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val widthPx = with(density) { ShapeShareCardWidth.roundToPx() }
    val heightPx = with(density) { ShapeShareCardMapHeight.roundToPx() }
    val fitPaddingPx = with(density) { ShapeShareMapFitPadding.toPx() }
    val outlineWidthPx = with(density) { ShapeShareMapOutlineWidth.toPx() }

    val initialStyle = remember { style }
    val mapView = remember {
        val sdk = NaverMapSdk.getInstance(context)
        if (sdk.client !is NaverMapSdk.NcpKeyClient) {
            sdk.client = NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_MAP_KEY_ID)
        }
        val options = NaverMapOptions()
            .useTextureView(true)
            .mapType(initialStyle.mapType)
            .camera(CameraPosition(shape.baseCoordinate.toLatLng(), ShapeShareMapMarkerZoom))
            .zoomControlEnabled(false)
            .compassEnabled(false)
            .scaleBarEnabled(false)
            .locationButtonEnabled(false)
            .indoorLevelPickerEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)
        MapView(context, options).apply {
            pivotX = 0f
            pivotY = 0f
        }
    }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var circle by remember { mutableStateOf<CircleOverlay?>(null) }
    var appliedStyle by remember { mutableStateOf(initialStyle) }

    fun applyViewScale(scale: Int) {
        mapView.layoutParams = FrameLayout.LayoutParams(widthPx * scale, heightPx * scale)
        mapView.scaleX = 1f / scale
        mapView.scaleY = 1f / scale
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ShapeShareMapTouchContainer(ctx).apply {
                addView(mapView)
                applyViewScale(initialStyle.renderScale)
            }
        },
    )

    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(null)
        val color = parseMapOverlayColorSafe(colorHex)
        val center = shape.baseCoordinate.toLatLng()
        var loadListener: NaverMap.OnLoadListener? = null
        mapView.getMapAsync { map ->
            map.symbolScale = initialStyle.symbolScale
            val radius = shape.radius
            // 도형이 다 보이는 초기 카메라(인자: 현재 렌더 배율). 지도 크기가 정해진 뒤(최초 로딩 완료) 다시 적용한다
            val initialCamera: (Int) -> CameraUpdate
            if (shape.shapeType == ShapeType.CIRCLE && radius != null && radius > 0) {
                // 메인 지도 원 오버레이의 일반 상태 스타일과 같다 (만료 회색은 적용하지 않는다)
                val overlay = CircleOverlay(center, radius).apply {
                    this.color = (ShapeShareMapFillAlpha shl 24) or (color and 0x00FFFFFF)
                    outlineColor = color
                    outlineWidth = (outlineWidthPx * initialStyle.renderScale).roundToInt()
                    globalZIndex = 50
                    this.map = map
                }
                circle = overlay
                val bounds = overlay.bounds
                initialCamera = { scale -> CameraUpdate.fitBounds(bounds, (fitPaddingPx * scale).roundToInt()) }
            } else {
                // 원이 아닌 도형은 앱에서 만드는 경로가 없다. 들어오면 기준 좌표에 마커만 찍는다
                Marker(center).apply {
                    iconTintColor = color
                    this.map = map
                }
                initialCamera = { scale ->
                    CameraUpdate.scrollAndZoomTo(center, ShapeShareMapMarkerZoom + log2(scale.toDouble()))
                }
            }
            map.moveCamera(initialCamera(appliedStyle.renderScale))
            // 생성 직후에는 지도 뷰가 배치 전이라 fit 줌이 과하게 잡힌다. 최초 로딩이 끝난 뒤 다시 맞춘다
            val listener = NaverMap.OnLoadListener {
                mapView.post {
                    map.moveCamera(initialCamera(appliedStyle.renderScale))
                    controller.isReady = true
                }
            }
            map.addOnLoadListener(listener)
            loadListener = listener
            controller.naverMap = map
            naverMap = map
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            naverMap?.let { map -> loadListener?.let(map::removeOnLoadListener) }
            controller.naverMap = null
            val state = lifecycleOwner.lifecycle.currentState
            if (state.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (state.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(style, naverMap) {
        val map = naverMap ?: return@LaunchedEffect
        val previous = appliedStyle
        if (previous == style) return@LaunchedEffect
        appliedStyle = style

        applyViewScale(style.renderScale)
        map.mapType = style.mapType
        map.symbolScale = style.symbolScale
        circle?.outlineWidth = (outlineWidthPx * style.renderScale).roundToInt()

        // 지도 뷰가 배율만큼 커지거나 작아지므로, 보이는 영역이 그대로이도록 줌을 log2(배율 비)만큼 옮긴다
        val zoomDelta = log2(style.renderScale.toDouble() / previous.renderScale)
        val position = map.cameraPosition
        map.moveCamera(CameraUpdate.scrollAndZoomTo(position.target, position.zoom + zoomDelta))
    }
}

/**
 * 지도를 끄는 동안 바깥(미리보기 시트의 끌어 닫기·세로 스크롤)이 터치를 가로채지 않게 한다.
 * Compose 인터롭은 이 요청을 받으면 이후 이벤트를 먼저 이 뷰에 넘기고 소비한다.
 */
@SuppressLint("ViewConstructor")
private class ShapeShareMapTouchContainer(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.dispatchTouchEvent(event)
    }
}
