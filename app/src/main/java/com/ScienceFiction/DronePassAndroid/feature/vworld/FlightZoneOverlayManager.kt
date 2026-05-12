package com.ScienceFiction.DronePassAndroid.feature.vworld

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.PolygonOverlay

/**
 * 비행구역 오버레이 관리자
 *
 * 네이버 지도에 비행구역 폴리곤을 표시/관리한다.
 * 레이어별 표시/숨기기, 터치 핸들러, 메모리 최적화를 담당한다.
 */
class FlightZoneOverlayManager {

    companion object {
        private const val TAG = "FlightZoneOverlay"
        private const val BASE_Z_INDEX = 100
    }

    private var naverMap: NaverMap? = null

    /** 레이어별 오버레이 목록 */
    private val overlayMap = mutableMapOf<FlightZoneLayer, MutableList<PolygonOverlay>>()

    /** 오버레이 → DroneZoneFeature 매핑 (터치 이벤트용) */
    private val overlayToZoneMap = mutableMapOf<PolygonOverlay, DroneZoneFeature>()

    /** 구역 탭 시 호출되는 콜백 */
    var onZoneTapped: ((DroneZoneFeature) -> Unit)? = null

    /**
     * 지도 인스턴스를 설정한다.
     */
    fun setMap(map: NaverMap) {
        this.naverMap = map
    }

    /**
     * 특정 레이어의 비행구역을 지도에 표시
     *
     * @param layer 비행구역 레이어
     * @param zones 해당 레이어의 구역 목록
     */
    fun setZones(layer: FlightZoneLayer, zones: List<DroneZoneFeature>) {
        // 기존 오버레이 제거
        removeLayerOverlays(layer)

        val overlays = mutableListOf<PolygonOverlay>()

        for (zone in zones) {
            for (polygon in zone.polygons) {
                if (polygon.size < 3) continue

                val coords = polygon.map { (lat, lon) -> LatLng(lat, lon) }

                try {
                    val overlay = PolygonOverlay().apply {
                        this.coords = coords
                        this.color = layer.fillColor.toInt()
                        this.outlineColor = layer.borderColor.toInt()
                        this.outlineWidth = 2
                        this.globalZIndex = BASE_Z_INDEX - layer.priority
                        this.map = naverMap

                        setOnClickListener {
                            onZoneTapped?.invoke(zone)
                            true
                        }
                    }
                    overlays.add(overlay)
                    overlayToZoneMap[overlay] = zone
                } catch (e: Exception) {
                    Log.e(TAG, "폴리곤 오버레이 생성 실패: ${e.message}")
                }
            }
        }

        overlayMap[layer] = overlays
        Log.d(TAG, "${layer.displayName}: ${overlays.size}개 오버레이 표시")
    }

    /**
     * 특정 레이어의 오버레이 표시/숨기기
     */
    fun setLayerVisible(layer: FlightZoneLayer, visible: Boolean) {
        overlayMap[layer]?.forEach { overlay ->
            overlay.map = if (visible) naverMap else null
        }
    }

    /**
     * 특정 레이어의 오버레이 제거
     */
    fun removeLayerOverlays(layer: FlightZoneLayer) {
        overlayMap[layer]?.forEach { overlay ->
            overlay.map = null
            overlayToZoneMap.remove(overlay)
        }
        overlayMap.remove(layer)
    }

    /**
     * 화면 밖의 오버레이를 제거하여 메모리 최적화
     *
     * @param bounds 현재 화면에 표시되는 영역
     */
    fun removeOutOfBoundsOverlays(bounds: LatLngBounds) {
        overlayMap.values.flatten().forEach { overlay ->
            val isInBounds = overlay.coords.any { coord ->
                bounds.contains(coord)
            }
            if (!isInBounds) {
                overlay.map = null
            } else if (overlay.map == null) {
                overlay.map = naverMap
            }
        }
    }

    /**
     * 모든 오버레이를 지도에서 제거
     */
    fun clearAllOverlays() {
        overlayMap.values.flatten().forEach { overlay ->
            overlay.map = null
        }
        overlayMap.clear()
        overlayToZoneMap.clear()
    }

    /**
     * 오버레이 정리 후 NaverMap 참조까지 해제한다.
     * Composable 의 DisposableEffect onDispose 에서 호출하여 Activity 파괴 후 누수 방지.
     */
    fun detach() {
        clearAllOverlays()
        onZoneTapped = null
        naverMap = null
    }

    /**
     * 현재 표시 중인 모든 구역 목록 반환
     */
    fun getAllDisplayedZones(): List<DroneZoneFeature> {
        return overlayToZoneMap.values.toList()
    }
}
