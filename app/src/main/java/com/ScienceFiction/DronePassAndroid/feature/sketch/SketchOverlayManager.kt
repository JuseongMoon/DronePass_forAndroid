package com.ScienceFiction.DronePassAndroid.feature.sketch

import android.graphics.Color
import com.ScienceFiction.DronePassAndroid.core.util.SketchPointsCache
import com.ScienceFiction.DronePassAndroid.core.util.SketchSmoothingAlgorithm
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.PolylineOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 스케치 폴리라인 오버레이를 관리하는 클래스.
 *
 * NaverMap 위에 스케치를 PolylineOverlay로 렌더링한다.
 * 저장된 스케치 오버레이와 실시간 프리뷰 오버레이를 분리하여 관리한다.
 */
class SketchOverlayManager {

    private var naverMap: NaverMap? = null

    /** 저장된 스케치 오버레이들 (sketchId → PolylineOverlay) */
    private val overlays = mutableMapOf<String, PolylineOverlay>()

    /** 실시간 그리기 프리뷰 오버레이 */
    private var previewOverlay: PolylineOverlay? = null

    /** 스케치 오버레이의 글로벌 Z 인덱스 (도형 오버레이보다 위에 표시) */
    private companion object {
        const val SKETCH_Z_INDEX = 100
        const val PREVIEW_Z_INDEX = 110
        const val MIN_POINTS_FOR_POLYLINE = 2
    }

    /**
     * NaverMap 인스턴스를 설정한다.
     */
    fun setMap(map: NaverMap) {
        this.naverMap = map
    }

    // ──────────────────────────────────────────────
    // 저장된 스케치 오버레이 관리
    // ──────────────────────────────────────────────

    /**
     * 스케치 목록으로 전체 오버레이를 갱신한다.
     *
     * 기존 오버레이를 diff하여 필요한 것만 추가/제거한다.
     * 스무딩된 포인트를 사용하여 부드러운 폴리라인을 렌더링한다.
     *
     * @param sketches 활성 스케치 모델 목록
     */
    suspend fun updateOverlays(sketches: List<SketchModel>) {
        val map = naverMap ?: return

        val currentIds = overlays.keys.toSet()
        val newIds = sketches.map { it.id }.toSet()

        // 삭제된 스케치의 오버레이 제거
        val removedIds = currentIds - newIds
        removedIds.forEach { id ->
            overlays.remove(id)?.map = null
        }

        // 새로 추가되거나 업데이트가 필요한 스케치 처리
        for (sketch in sketches) {
            if (sketch.points.size < MIN_POINTS_FOR_POLYLINE) continue

            // 스무딩된 포인트 가져오기 (캐시 활용)
            val smoothedPoints = withContext(Dispatchers.Default) {
                SketchPointsCache.getSmoothedPoints(sketch)
            }

            if (smoothedPoints.size < MIN_POINTS_FOR_POLYLINE) continue

            val latLngs = smoothedPoints.map { it.toLatLng() }

            // 기존 오버레이가 있으면 업데이트, 없으면 새로 생성
            val existing = overlays[sketch.id]
            if (existing != null) {
                existing.coords = latLngs
                existing.color = parseSketchColor(sketch.color, sketch.opacity)
                existing.width = dpToPx(sketch.strokeWidth)
            } else {
                val polyline = PolylineOverlay().apply {
                    coords = latLngs
                    color = parseSketchColor(sketch.color, sketch.opacity)
                    width = dpToPx(sketch.strokeWidth)
                    globalZIndex = SKETCH_Z_INDEX
                    this.map = map
                }
                overlays[sketch.id] = polyline
            }
        }
    }

    // ──────────────────────────────────────────────
    // 실시간 프리뷰 오버레이
    // ──────────────────────────────────────────────

    /**
     * 실시간 그리기 프리뷰 오버레이를 업데이트한다.
     *
     * @param points 현재 그리는 중인 포인트 리스트
     * @param color 펜 색상 (HEX)
     * @param strokeWidth 펜 두께
     * @param opacity 펜 투명도
     */
    fun updatePreviewOverlay(
        points: List<Coordinate>,
        color: String,
        strokeWidth: Double,
        opacity: Double
    ) {
        val map = naverMap ?: return

        if (points.size < MIN_POINTS_FOR_POLYLINE) {
            clearPreviewOverlay()
            return
        }

        // 프리뷰는 실시간이므로 스무딩 없이 원본 포인트 사용
        // (성능을 위해, 완료 시에만 스무딩 적용)
        val latLngs = points.map { it.toLatLng() }

        val overlay = previewOverlay ?: PolylineOverlay().also {
            it.globalZIndex = PREVIEW_Z_INDEX
            it.map = map
            previewOverlay = it
        }

        overlay.coords = latLngs
        overlay.color = parseSketchColor(color, opacity)
        overlay.width = dpToPx(strokeWidth)
    }

    /**
     * 프리뷰 오버레이를 제거한다.
     */
    fun clearPreviewOverlay() {
        previewOverlay?.map = null
        previewOverlay = null
    }

    // ──────────────────────────────────────────────
    // 전체 오버레이 제거
    // ──────────────────────────────────────────────

    /**
     * 저장된 스케치 오버레이와 프리뷰 오버레이를 모두 제거한다.
     */
    fun clearOverlays() {
        overlays.values.forEach { it.map = null }
        overlays.clear()
        clearPreviewOverlay()
    }

    /**
     * 오버레이 정리 후 NaverMap 참조까지 해제한다.
     * Composable 의 DisposableEffect onDispose 에서 호출하여 Activity 파괴 후 누수 방지.
     */
    fun detach() {
        clearOverlays()
        naverMap = null
    }

    // ──────────────────────────────────────────────
    // 유틸리티
    // ──────────────────────────────────────────────

    /**
     * HEX 색상 문자열과 투명도를 조합하여 ARGB int 값을 생성한다.
     */
    private fun parseSketchColor(hex: String, opacity: Double): Int {
        return try {
            val baseColor = Color.parseColor(hex)
            val alpha = (opacity * 255).toInt().coerceIn(0, 255)
            Color.argb(
                alpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
        } catch (e: Exception) {
            Color.RED
        }
    }

    /**
     * dp 값을 픽셀 단위로 변환한다.
     * NaverMap PolylineOverlay의 width는 픽셀 단위이다.
     */
    private fun dpToPx(dp: Double): Int {
        // 일반적인 Android 디바이스 밀도 기준
        // 실제 런타임에서는 Resources를 통해 정확한 밀도를 구할 수 있지만,
        // 오버레이 매니저는 Context를 가지지 않으므로 2.5 배율 사용 (xxhdpi 근사)
        return (dp * 2.5).toInt().coerceAtLeast(1)
    }
}
