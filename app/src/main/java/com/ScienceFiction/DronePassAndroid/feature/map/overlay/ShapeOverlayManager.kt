package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import android.graphics.Color
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Overlay
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType

/**
 * 네이버 Maps SDK 오버레이를 관리하는 클래스.
 *
 * NaverMap 인스턴스를 받아서 ShapeModel 리스트를 오버레이로 렌더링한다.
 * 도형 탭 이벤트, 하이라이트 표시, 오버레이 갱신/제거를 담당한다.
 */
class ShapeOverlayManager {

    private var naverMap: NaverMap? = null
    private val overlays = mutableListOf<CircleOverlay>()
    private val overlayToShapeMap = mutableMapOf<CircleOverlay, String>()
    private var highlightOverlay: CircleOverlay? = null

    /** 도형 탭 시 호출되는 콜백. shapeId를 전달한다. */
    var onShapeTapped: ((String) -> Unit)? = null

    /**
     * 지도 인스턴스를 설정한다.
     */
    fun setMap(map: NaverMap) {
        this.naverMap = map
    }

    // ──────────────────────────────────────────────
    // 전체 오버레이 갱신
    // ──────────────────────────────────────────────

    /**
     * shapes 리스트를 기반으로 전체 오버레이를 갱신한다.
     * 기존 오버레이를 모두 제거한 뒤 새로 추가한다.
     */
    fun updateOverlays(shapes: List<ShapeModel>) {
        clearOverlays()
        shapes.forEach { shape -> addCircleOverlay(shape) }
    }

    // ──────────────────────────────────────────────
    // 원형 오버레이 추가
    // ──────────────────────────────────────────────

    /**
     * 원형 오버레이를 추가한다.
     */
    private fun addCircleOverlay(shape: ShapeModel) {
        val radius = shape.radius ?: return
        val center = shape.baseCoordinate.toLatLng()

        val circleOverlay = CircleOverlay().apply {
            this.center = center
            this.radius = radius
            this.color = calculateFillColor(shape)
            this.outlineColor = calculateOutlineColor(shape)
            this.outlineWidth = if (shape.isNotStarted) 1 else 2
            this.globalZIndex = 50
            this.map = naverMap

            setOnClickListener {
                onShapeTapped?.invoke(shape.id)
                true
            }
        }

        overlays.add(circleOverlay)
        overlayToShapeMap[circleOverlay] = shape.id
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
        val radius = shape.radius ?: return

        highlightOverlay = CircleOverlay().apply {
            this.center = shape.baseCoordinate.toLatLng()
            this.radius = radius + 2
            this.color = Color.TRANSPARENT
            this.outlineColor = Color.RED
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
     * - 만료됨: 알파 20%
     * - 미시작: 알파 20%
     * - 활성: 알파 30%
     */
    private fun calculateFillColor(shape: ShapeModel): Int {
        val baseColor = parseColorSafe(shape.effectiveColor)

        val alpha = when {
            shape.isExpired -> 0x33       // 20%
            shape.isNotStarted -> 0x33    // 20%
            else -> 0x4D                  // 30%
        }

        return Color.argb(
            alpha,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )
    }

    /**
     * 도형의 상태에 따른 외곽선 색상을 계산한다.
     * 만료된 도형은 회색, 그 외는 도형의 색상을 사용한다.
     */
    private fun calculateOutlineColor(shape: ShapeModel): Int {
        return if (shape.isExpired) {
            Color.GRAY
        } else {
            parseColorSafe(shape.effectiveColor)
        }
    }

    /**
     * 색상 문자열을 안전하게 파싱한다.
     * 파싱 실패 시 기본 파란색(#007AFF)을 반환한다.
     */
    private fun parseColorSafe(colorString: String): Int {
        return try {
            Color.parseColor(colorString)
        } catch (e: Exception) {
            Color.parseColor("#007AFF")
        }
    }

    // ──────────────────────────────────────────────
    // 오버레이 제거
    // ──────────────────────────────────────────────

    /**
     * 모든 오버레이를 지도에서 제거하고 내부 상태를 초기화한다.
     */
    fun clearOverlays() {
        overlays.forEach { overlay ->
            overlay.map = null
        }
        overlays.clear()
        overlayToShapeMap.clear()

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
