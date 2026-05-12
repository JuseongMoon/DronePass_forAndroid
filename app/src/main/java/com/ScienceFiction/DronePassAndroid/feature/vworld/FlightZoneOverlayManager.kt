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

    /**
     * 레이어별 [zoneId → PolygonOverlay 리스트] 맵.
     * Diff 비교를 위해 zone 단위로 추적 (한 zone 이 MultiPolygon 인 경우 N개의 PolygonOverlay).
     */
    private val overlayMap = mutableMapOf<FlightZoneLayer, MutableMap<String, List<PolygonOverlay>>>()

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
     * 특정 레이어의 비행구역을 Diff 기반으로 지도에 반영한다.
     *
     * - 새로 도착한 zone (id 가 캐시에 없음) → PolygonOverlay 생성
     * - 사라진 zone (캐시에 있지만 새 zones 에 없음) → 제거
     * - 동일 id 의 zone → skip (이미 그려져 있음, 좌표 변동이 거의 없는 정적 GIS 데이터 가정)
     *
     * 이전: removeLayerOverlays 로 모든 오버레이를 destroy 후 전체 재생성 → 카메라 이동마다
     * 모든 폴리곤(수십~수백개)을 다시 그려 60fps 기준 위반 위험.
     */
    fun setZones(layer: FlightZoneLayer, zones: List<DroneZoneFeature>) {
        val map = naverMap ?: return
        val layerCache = overlayMap.getOrPut(layer) { mutableMapOf() }

        val newZoneIds = zones.map { it.id }.toSet()
        val currentIds = layerCache.keys.toSet()

        // 1. 사라진 zone 의 오버레이 제거
        val removedIds = currentIds - newZoneIds
        removedIds.forEach { id ->
            layerCache.remove(id)?.forEach { overlay ->
                overlay.map = null
                overlayToZoneMap.remove(overlay)
            }
        }

        // 2. 신규 zone 만 PolygonOverlay 생성 (기존 id 는 정적 GIS 데이터이므로 skip)
        var added = 0
        for (zone in zones) {
            if (layerCache.containsKey(zone.id)) continue
            val zoneOverlays = mutableListOf<PolygonOverlay>()
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
                        this.map = map

                        setOnClickListener {
                            onZoneTapped?.invoke(zone)
                            true
                        }
                    }
                    zoneOverlays.add(overlay)
                    overlayToZoneMap[overlay] = zone
                } catch (e: Exception) {
                    Log.e(TAG, "폴리곤 오버레이 생성 실패: ${e.message}")
                }
            }
            if (zoneOverlays.isNotEmpty()) {
                layerCache[zone.id] = zoneOverlays
                added++
            }
        }

        Log.d(TAG, "${layer.displayName}: 신규=$added, 유지=${currentIds.intersect(newZoneIds).size}, 제거=${removedIds.size}")
    }

    /**
     * 특정 레이어의 오버레이 표시/숨기기
     */
    fun setLayerVisible(layer: FlightZoneLayer, visible: Boolean) {
        overlayMap[layer]?.values?.flatten()?.forEach { overlay ->
            overlay.map = if (visible) naverMap else null
        }
    }

    /**
     * 특정 레이어의 오버레이 제거
     */
    fun removeLayerOverlays(layer: FlightZoneLayer) {
        overlayMap[layer]?.values?.flatten()?.forEach { overlay ->
            overlay.map = null
            overlayToZoneMap.remove(overlay)
        }
        overlayMap.remove(layer)
    }

    /**
     * 화면 밖 오버레이의 표시만 토글 (메모리 절약). 인스턴스는 캐시에 유지하여
     * 카메라 재진입 시 재사용할 수 있게 한다.
     *
     * 현재는 setZones() 의 Diff 가 자동으로 in-bounds 셋만 반영하므로 호출이 필수가 아니지만,
     * 카메라 디바운스 사이 짧은 구간에서 화면 밖 오버레이가 잠시 보이는 것을 막고 싶을 때 사용 가능.
     */
    fun setOutOfBoundsVisibility(bounds: LatLngBounds) {
        overlayMap.values.flatMap { it.values }.flatten().forEach { overlay ->
            val isInBounds = overlay.coords.any { coord -> bounds.contains(coord) }
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
        overlayMap.values.flatMap { it.values }.flatten().forEach { overlay ->
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
