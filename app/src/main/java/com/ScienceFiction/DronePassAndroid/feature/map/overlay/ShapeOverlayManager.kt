package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import kotlin.math.roundToInt
import android.graphics.Color
import com.ScienceFiction.DronePassAndroid.core.util.parseIosOpaqueRgbHexColor
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Overlay
import com.ScienceFiction.DronePassAndroid.feature.map.tap.MapTapLatLng
import com.ScienceFiction.DronePassAndroid.feature.map.tap.MapTapResolver
import com.ScienceFiction.DronePassAndroid.feature.map.tap.MapTapShape
import com.ScienceFiction.DronePassAndroid.feature.map.tap.MapTapSource
import com.ScienceFiction.DronePassAndroid.feature.map.tap.MapTapTarget

/** 내 도형 오버레이의 전역 z(비행구역 위, 스케치 아래) */
internal const val ShapeOverlayGlobalZIndex = 50

internal fun parseMapOverlayColorSafe(colorString: String): Int {
    return parseIosOpaqueRgbHexColor(colorString) ?: Color.BLACK
}

internal fun shouldRenderMapCircleOverlay(shape: ShapeModel): Boolean {
    return shape.shapeType == ShapeType.CIRCLE && shape.radius != null
}

internal enum class MapShapeOverlayKind {
    CIRCLE,
}

internal fun resolveMapShapeOverlayKind(shape: ShapeModel): MapShapeOverlayKind? {
    return when (shape.shapeType) {
        ShapeType.CIRCLE -> if (shape.radius != null) MapShapeOverlayKind.CIRCLE else null
        ShapeType.RECTANGLE,
        ShapeType.POLYGON,
        ShapeType.POLYLINE -> null
    }
}

internal fun shouldRenderMapShapeOverlay(shape: ShapeModel): Boolean {
    return resolveMapShapeOverlayKind(shape) != null
}

internal fun shapeOverlayCoordinates(shape: ShapeModel): List<Coordinate>? {
    return when (resolveMapShapeOverlayKind(shape)) {
        MapShapeOverlayKind.CIRCLE -> listOf(shape.baseCoordinate)
        null -> null
    }
}

internal fun resolveMapCircleHighlightRadius(shape: ShapeModel): Double? {
    if (!shouldRenderMapCircleOverlay(shape)) return null
    return (shape.radius ?: return null) + 2
}

internal fun resolveMapFocusHighlightRadius(shape: ShapeModel): Double? {
    return (shape.radius ?: return null) + 2
}

internal const val MapOverlaySystemGrayHex = "#8E8E93"
internal const val MapOverlayFocusHighlightHex = "#FF3B30"
internal const val MapOverlayDroneHighlightOutlineHex = "#333333"

internal fun calculateMapOverlayFillColor(
    shape: ShapeModel,
    highlightedDroneIds: Set<String>,
): Int {
    val mainColor = mainMapOverlayColorFor(shape)
    val isHighlighted = isMapDroneHighlighted(shape, highlightedDroneIds)
    val alpha = when {
        shape.isNotStarted && isHighlighted -> 0x80
        shape.isNotStarted -> 0x33
        isHighlighted -> 0xB3
        else -> 0x4D
    }

    return withMapOverlayAlpha(mainColor, alpha)
}

internal fun calculateMapOverlayOutlineColor(
    shape: ShapeModel,
    highlightedDroneIds: Set<String>,
): Int {
    val mainColor = mainMapOverlayColorFor(shape)
    val isHighlighted = isMapDroneHighlighted(shape, highlightedDroneIds)
    return when {
        shape.isNotStarted && isHighlighted -> parseMapOverlayColorSafe(MapOverlayDroneHighlightOutlineHex)
        shape.isNotStarted -> withMapOverlayAlpha(mainColor, 0x80)
        isHighlighted -> parseMapOverlayColorSafe(MapOverlayDroneHighlightOutlineHex)
        else -> mainColor
    }
}

/**
 * iOS NMFOverlay.outlineWidth 는 pt, Android 네이버 SDK 의 outlineWidth 는 px 단위다.
 * iOS 와 같은 값(pt)을 dp 로 보고 px 로 바꿔야 두께가 같아진다(이 변환이 없으면 약 1/3 두께).
 */
internal const val MapOverlayFocusHighlightOutlinePoints = 5

internal fun mapOverlayPointsToPx(points: Int, density: Float): Int =
    (points * density).roundToInt().coerceAtLeast(1)

/** iOS 와 같은 pt 값. 실제 SDK 에는 [mapOverlayPointsToPx] 로 바꿔 넣는다. */
internal fun calculateMapOverlayOutlineWidth(shape: ShapeModel): Int {
    return if (shape.isNotStarted) 1 else 2
}

private fun mainMapOverlayColorFor(shape: ShapeModel): Int {
    return if (shape.isExpired) {
        parseMapOverlayColorSafe(MapOverlaySystemGrayHex)
    } else {
        parseMapOverlayColorSafe(shape.color)
    }
}

private fun isMapDroneHighlighted(
    shape: ShapeModel,
    highlightedDroneIds: Set<String>,
): Boolean {
    return shape.droneId?.let { it in highlightedDroneIds } ?: false
}

private fun withMapOverlayAlpha(color: Int, alpha: Int): Int {
    return ((alpha and 0xFF) shl 24) or (color and 0x00FFFFFF)
}

internal fun uniqueMapOverlayShapesByFirstId(shapes: List<ShapeModel>): List<ShapeModel> {
    return shapes.distinctBy { it.id }
}

/**
 * 네이버 Maps SDK 오버레이를 관리하는 클래스.
 *
 * NaverMap 인스턴스를 받아서 iOS MapViewModel 과 같이 원형 ShapeModel 만 오버레이로 렌더링한다.
 * 도형 탭 이벤트, 하이라이트 표시, 오버레이 갱신/제거를 담당한다.
 */
class ShapeOverlayManager : MapTapSource {

    private var naverMap: NaverMap? = null

    /** 외곽선 pt → px 변환용 display density. */
    private var density: Float = android.content.res.Resources.getSystem().displayMetrics.density

    fun setDensity(density: Float) {
        this.density = density.coerceAtLeast(0.5f)
    }

    /** shapeId → Overlay 매핑. Diff 기반으로 신규/변경/삭제만 반영한다. */
    private val overlays = mutableMapOf<String, Overlay>()

    /** updateOverlays 의 Diff 비교를 위해 마지막으로 적용된 shape state 캐시. */
    private val appliedShapeKeys = mutableMapOf<String, ShapeKey>()

    private var highlightOverlay: Overlay? = null

    private var highlightedDroneIds: Set<String> = emptySet()

    /** 도형 탭 판정 결과로 열 때 호출되는 콜백. shapeId를 전달한다. */
    var onShapeTapped: ((String) -> Unit)? = null

    /** Diff 비교를 위한 도형 속성 키 (변경 감지에 영향을 주는 모든 필드). */
    private data class ShapeKey(
        val kind: MapShapeOverlayKind,
        val lat: Double,
        val lon: Double,
        val radius: Double?,
        val coordinates: List<Coordinate>,
        val color: String,
        val droneId: String?,
        val isExpired: Boolean,
        val isNotStarted: Boolean,
        val updatedAt: Long
    )

    private fun ShapeModel.toKey(): ShapeKey? {
        val kind = resolveMapShapeOverlayKind(this) ?: return null
        val coordinates = shapeOverlayCoordinates(this) ?: return null
        return ShapeKey(
            kind = kind,
            lat = baseCoordinate.latitude,
            lon = baseCoordinate.longitude,
            radius = radius,
            coordinates = coordinates,
            color = color,
            droneId = droneId,
            isExpired = isExpired,
            isNotStarted = isNotStarted,
            updatedAt = updatedAt
        )
    }

    /**
     * 지도 인스턴스를 설정한다.
     */
    fun setMap(map: NaverMap) {
        this.naverMap = map
    }

    // ──────────────────────────────────────────────
    // Diff 기반 오버레이 갱신
    // ──────────────────────────────────────────────

    /**
     * shapes 리스트를 기반으로 오버레이를 갱신한다.
     * Diff: 신규는 add, 사라진 것은 remove, 속성 변경된 것만 재생성한다.
     * 한 도형의 updatedAt 만 변해도 100개를 전부 destroy/create 하던 동작을 피한다.
     */
    fun updateOverlays(shapes: List<ShapeModel>, highlightedDroneIds: Set<String>) {
        val didHighlightChange = this.highlightedDroneIds != highlightedDroneIds
        this.highlightedDroneIds = highlightedDroneIds

        val map = naverMap ?: return
        val uniqueShapes = uniqueMapOverlayShapesByFirstId(shapes)
        val newKeys = uniqueShapes.mapNotNull { shape -> shape.toKey()?.let { shape.id to it } }.toMap()

        // 1. 사라진 도형의 오버레이 제거
        val removedIds = overlays.keys - newKeys.keys
        removedIds.forEach { id ->
            overlays.remove(id)?.map = null
            tapShapeById.remove(id)
            appliedShapeKeys.remove(id)
        }

        // 2. 신규/변경된 도형만 add 또는 재생성
        uniqueShapes.forEach { shape ->
            val newKey = newKeys[shape.id] ?: return@forEach
            val oldKey = appliedShapeKeys[shape.id]
            when {
                oldKey == null -> {
                    addShapeOverlay(shape, map)
                    appliedShapeKeys[shape.id] = newKey
                }
                oldKey != newKey || didHighlightChange -> {
                    overlays.remove(shape.id)?.map = null
                    tapShapeById.remove(shape.id)
                    addShapeOverlay(shape, map)
                    appliedShapeKeys[shape.id] = newKey
                }
                // oldKey == newKey 면 in-place 갱신도 불필요 → skip
            }
        }
    }

    // ──────────────────────────────────────────────
    // 도형 오버레이 add
    // ──────────────────────────────────────────────

    /**
     * 도형 오버레이를 새로 추가한다.
     */
    private fun addShapeOverlay(shape: ShapeModel, map: NaverMap) {
        val overlay = when (resolveMapShapeOverlayKind(shape)) {
            MapShapeOverlayKind.CIRCLE -> {
                val radius = shape.radius ?: return
                CircleOverlay().apply {
                    this.center = shape.baseCoordinate.toLatLng()
                    this.radius = radius
                    this.color = calculateFillColor(shape)
                    this.outlineColor = calculateOutlineColor(shape)
                    this.outlineWidth = mapOverlayPointsToPx(calculateOutlineWidth(shape), density)
                }
            }
            null -> return
        }.apply {
            this.globalZIndex = ShapeOverlayGlobalZIndex
            this.map = map
            // 탭은 받지 않는다(클릭 리스너 없음). 지도 탭에서 MapTapResolver가 판정한다(11/7 대표, tapShapes·openTapped)
        }
        overlays[shape.id] = overlay
        // 탭 판정 형상은 도형 값으로 미리 만든다(탭마다 오버레이 getter를 부르지 않는다)
        val circle = overlay as? CircleOverlay
        if (circle != null) {
            tapShapeById[shape.id] = MapTapShape.circle(
                target = MapTapTarget.Shape(shape.id),
                drawOrder = MapTapResolver.drawOrder(ShapeOverlayGlobalZIndex, 0),
                center = MapTapLatLng(shape.baseCoordinate.latitude, shape.baseCoordinate.longitude),
                radiusM = shape.radius ?: circle.radius,
            )
        }
    }

    // ──────────────────────────────────────────────
    // 하이라이트 (선택된 도형 강조)
    // ──────────────────────────────────────────────

    /**
     * 선택된 도형을 하이라이트 표시한다.
     *
     * @param shapeId 하이라이트할 도형의 ID. null이면 하이라이트를 해제한다.
     * @param shapes 전체 도형 리스트
     */
    fun setHighlight(shapeId: String?, shapes: List<ShapeModel>) {
        // 기존 하이라이트 제거
        highlightOverlay?.map = null
        highlightOverlay = null

        val shape = shapes.find { it.id == shapeId } ?: return

        val radius = resolveMapFocusHighlightRadius(shape) ?: return
        highlightOverlay = CircleOverlay().apply {
            this.center = shape.baseCoordinate.toLatLng()
            this.radius = radius
            this.color = Color.TRANSPARENT
            this.outlineColor = parseColorSafe(MapOverlayFocusHighlightHex)
            this.outlineWidth = mapOverlayPointsToPx(MapOverlayFocusHighlightOutlinePoints, density)
            this.globalZIndex = 60
            this.map = naverMap
        }
    }

    // ──────────────────────────────────────────────
    // 색상 계산
    // ──────────────────────────────────────────────

    /**
     * 도형의 상태에 따른 채우기 색상(ARGB)을 계산한다.
     * iOS createCircleOverlay/updateOverlayProperties 정합.
     * - 미시작: 20%, 강조 50%
     * - 활성/만료: 30%, 강조 70%
     * - 만료 도형은 채움과 외곽선 모두 systemGray 기반
     */
    private fun calculateFillColor(shape: ShapeModel): Int {
        return calculateMapOverlayFillColor(shape, highlightedDroneIds)
    }

    /**
     * 도형의 상태에 따른 외곽선 색상을 계산한다.
     */
    private fun calculateOutlineColor(shape: ShapeModel): Int {
        return calculateMapOverlayOutlineColor(shape, highlightedDroneIds)
    }

    private fun calculateOutlineWidth(shape: ShapeModel): Int {
        return calculateMapOverlayOutlineWidth(shape)
    }

    /**
     * 색상 문자열을 안전하게 파싱한다.
     * iOS MapViewModel 의 `UIColor(hex:) ?? .black` fallback 과 동일하게 검정색을 반환한다.
     */
    private fun parseColorSafe(colorString: String): Int {
        return parseMapOverlayColorSafe(colorString)
    }

    // ──────────────────────────────────────────────
    // 지도 탭 판정 소스: 내 원(수가 적어 색인하지 않고 탭마다 본다)
    // ──────────────────────────────────────────────

    override val tapIndexed: Boolean get() = false
    override val tapSignature: Int get() = 0

    /** 지도에 올린 내 원의 탭 판정 형상(오버레이를 올리고 내릴 때 함께 바뀐다) */
    private val tapShapeById = mutableMapOf<String, MapTapShape>()

    /** 지금 지도에 보이는 내 원. 올린 오버레이는 모두 보이고, 숨길 때는 오버레이와 함께 지운다 */
    override val tapShapes: List<MapTapShape>
        get() = tapShapeById.values.toList()

    /** 판정 결과로 도형을 연다(전에 원 클릭 리스너가 하던 일) */
    override fun openTapped(target: MapTapTarget): Boolean {
        if (target !is MapTapTarget.Shape) return false
        if (overlays.containsKey(target.id)) onShapeTapped?.invoke(target.id)
        return true
    }

    /** 빈 곳 탭: 지도 하이라이트만 푼다. 저장 목록 선택은 별개라 그대로 둔다 */
    var onTapHighlightCleared: (() -> Unit)? = null

    override fun clearTapHighlight() {
        onTapHighlightCleared?.invoke()
    }

    // ──────────────────────────────────────────────
    // 오버레이 제거
    // ──────────────────────────────────────────────

    /**
     * 모든 오버레이를 지도에서 제거하고 내부 상태를 초기화한다.
     */
    fun clearOverlays() {
        overlays.values.forEach { overlay -> overlay.map = null }
        overlays.clear()
        tapShapeById.clear()
        appliedShapeKeys.clear()

        highlightOverlay?.map = null
        highlightOverlay = null
    }

    /**
     * 오버레이 정리 후 NaverMap 참조까지 해제한다.
     * Composable 의 [androidx.compose.runtime.DisposableEffect] onDispose 에서 호출하여
     * Activity 파괴 후 NaverMap 인스턴스 누수를 방지한다.
     */
    fun detach() {
        clearOverlays()
        onShapeTapped = null
        onTapHighlightCleared = null
        naverMap = null
    }
}
