package com.ScienceFiction.DronePassAndroid.feature.sketch

import android.graphics.Color
import com.ScienceFiction.DronePassAndroid.core.util.SketchPointsCache
import com.ScienceFiction.DronePassAndroid.core.util.SketchSmoothingAlgorithm
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.PolylineOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 스케치 폴리라인 오버레이를 관리하는 클래스.
 *
 * NaverMap 위에 스케치를 PolylineOverlay로 렌더링한다.
 * 저장된 스케치 오버레이와 실시간 프리뷰 오버레이를 분리하여 관리한다.
 */
class SketchOverlayManager {

    private var naverMap: NaverMap? = null

    /**
     * Polyline 두께 변환에 사용하는 display density (1dp = `density` px).
     * setMap 시 NaverMap 의 Context resources 로부터 정확한 값으로 갱신된다.
     * 폴백 2.5 는 xxhdpi 근사 (이전 하드코딩과 호환).
     */
    private var density: Float = 2.5f

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
     * NaverMap 인스턴스를 설정한다. density 는 [setDensity] 로 주입하지 않으면
     * Resources.getSystem 의 시스템 기본 displayMetrics 를 사용한다.
     */
    fun setMap(map: NaverMap) {
        this.naverMap = map
        density = android.content.res.Resources.getSystem().displayMetrics.density
    }

    /**
     * 정확한 dpToPx 변환을 위해 호출자(MapScreen)에서 LocalDensity 값을 주입한다.
     * 호출하지 않아도 [setMap] 의 시스템 density 폴백으로 동작.
     */
    fun setDensity(density: Float) {
        this.density = density.coerceAtLeast(0.5f)
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

        val candidates = sketches.filter { it.points.size >= MIN_POINTS_FOR_POLYLINE }
        if (candidates.isEmpty()) return

        // Catmull-Rom 스무딩을 sketch 별 병렬 (Dispatchers.Default) 로 수행.
        // 단일 sketch 의 캐시 미스는 무겁고, 다중 sketch 가 있는 경우 순차 처리는 N배 지연.
        // SketchPointsCache 가 in-flight 추적으로 중복 계산을 막아주므로 안전.
        val smoothed = coroutineScope {
            withContext(Dispatchers.Default) {
                candidates.map { sketch ->
                    async { sketch to SketchPointsCache.getSmoothedPoints(sketch) }
                }.awaitAll()
            }
        }

        smoothed.forEach { (sketch, smoothedPoints) ->
            if (smoothedPoints.size < MIN_POINTS_FOR_POLYLINE) return@forEach
            val latLngs = smoothedPoints.map { it.toLatLng() }

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
     * dp 값을 픽셀 단위로 변환한다. NaverMap PolylineOverlay 의 width 는 px 단위.
     * [density] 는 setMap/setDensity 로 주입된 displayMetrics 기반 값.
     */
    private fun dpToPx(dp: Double): Int {
        return (dp * density).toInt().coerceAtLeast(1)
    }
}
