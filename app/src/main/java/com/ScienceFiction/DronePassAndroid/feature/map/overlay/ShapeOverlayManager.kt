package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import android.graphics.Color
import com.ScienceFiction.DronePassAndroid.core.util.parseIosOpaqueRgbHexColor
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Overlay

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
class ShapeOverlayManager {

    private var naverMap: NaverMap? = null

    /** shapeId → Overlay 매핑. Diff 기반으로 신규/변경/삭제만 반영한다. */
    private val overlays = mutableMapOf<String, Overlay>()

    /** updateOverlays 의 Diff 비교를 위해 마지막으로 적용된 shape state 캐시. */
    private val appliedShapeKeys = mutableMapOf<String, ShapeKey>()

    private var highlightOverlay: Overlay? = null

    private var highlightedDroneIds: Set<String> = emptySet()

    /** 도형 탭 시 호출되는 콜백. shapeId를 전달한다. */
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
                    this.outlineWidth = calculateOutlineWidth(shape)
                }
            }
            null -> return
        }.apply {
            this.globalZIndex = 50
            this.map = map
            setOnClickListener {
                onShapeTapped?.invoke(shape.id)
                true
            }
        }
        overlays[shape.id] = overlay
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
            this.outlineWidth = 5
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
    // 오버레이 제거
    // ──────────────────────────────────────────────

    /**
     * 모든 오버레이를 지도에서 제거하고 내부 상태를 초기화한다.
     */
    fun clearOverlays() {
        overlays.values.forEach { overlay -> overlay.map = null }
        overlays.clear()
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
        naverMap = null
    }
}
