package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightRestrictionLevel
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 비행구역 관련 계산 유틸리티
 *
 * - 포인트 인 폴리곤 (Ray Casting Algorithm)
 * - 거리 계산 (Haversine Formula)
 * - 비행 가능 여부 판정
 * - 바운딩 박스 생성
 */
object FlightZoneCalculator {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * 점이 폴리곤 내부에 있는지 판정 (Ray Casting Algorithm)
     *
     * iOS FlightZoneCalculator.swift 의 isPointInPolygon 과 동일한 식을 사용한다.
     * 변수명을 latI/lonI 등으로 명시하여 좌표축 혼선을 방지한다.
     *
     * 안전 기준: 비행구역 오판 0건 허용. 변경 시 FlightZoneCalculatorTest 의
     * 단위 테스트(정사각형 내부/외부/꼭짓점/변/홀수교차)를 반드시 통과해야 한다.
     *
     * @param lat 검사할 점의 위도
     * @param lon 검사할 점의 경도
     * @param polygon 폴리곤 좌표 리스트 (Pair(lat, lon))
     * @return 내부이면 true
     */
    fun isPointInPolygon(
        lat: Double,
        lon: Double,
        polygon: List<Pair<Double, Double>>
    ): Boolean {
        if (polygon.size < 3) return false

        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val (latI, lonI) = polygon[i]
            val (latJ, lonJ) = polygon[j]

            // Ray casting: 점에서 동쪽으로 수평선을 그어 폴리곤 변과의 교차 횟수 세기.
            // 점의 위도가 두 꼭짓점의 위도 사이일 때만 변 교차 후보가 된다.
            if ((latI > lat) != (latJ > lat) &&
                lon < (lonJ - lonI) * (lat - latI) / (latJ - latI) + lonI
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * 두 좌표 간 거리 계산 (Haversine Formula)
     *
     * @param lat1 첫 번째 점의 위도
     * @param lon1 첫 번째 점의 경도
     * @param lat2 두 번째 점의 위도
     * @param lon2 두 번째 점의 경도
     * @return 거리 (미터)
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c * 1000 // 미터로 변환
    }

    /**
     * 특정 좌표에서의 비행 가능 여부 판정
     *
     * 비행금지(PROHIBITED) 또는 비행제한(RESTRICTED) 구역 내부이면 비행 불가.
     * 안전 최우선: 비행금지 -> 비행가능 오판 0건을 보장한다.
     *
     * @param lat 검사할 위도
     * @param lon 검사할 경도
     * @param zones 비행구역 리스트
     * @return FlightPermissionResult
     */
    fun checkFlightPermission(
        lat: Double,
        lon: Double,
        zones: List<DroneZoneFeature>
    ): FlightPermissionResult {
        val containingZones = mutableListOf<DroneZoneFeature>()

        for (zone in zones) {
            for (polygon in zone.polygons) {
                if (isPointInPolygon(lat, lon, polygon)) {
                    containingZones.add(zone)
                    break // 하나의 폴리곤에서 포함되면 다음 구역으로
                }
            }
        }

        if (containingZones.isEmpty()) {
            return FlightPermissionResult(
                canFly = true,
                level = null,
                zones = emptyList(),
                message = "비행 가능 구역입니다."
            )
        }

        // 가장 높은 제한 수준 찾기 (PROHIBITED > RESTRICTED > CONSULTATION > ADVISORY)
        val highestRestriction = containingZones.minByOrNull { it.layer.priority }
        val level = highestRestriction?.layer?.restrictionLevel

        val canFly = level != FlightRestrictionLevel.PROHIBITED &&
                level != FlightRestrictionLevel.RESTRICTED

        val message = when (level) {
            FlightRestrictionLevel.PROHIBITED -> "비행금지구역입니다. 비행이 불가합니다."
            FlightRestrictionLevel.RESTRICTED -> "비행제한구역입니다. 허가 없이 비행할 수 없습니다."
            FlightRestrictionLevel.CONSULTATION -> "사전협의가 필요한 구역입니다."
            FlightRestrictionLevel.ADVISORY -> "주의가 필요한 구역입니다."
            null -> "비행 가능 구역입니다."
        }

        return FlightPermissionResult(
            canFly = canFly,
            level = level,
            zones = containingZones,
            message = message
        )
    }

    /**
     * 좌표와 반경으로 바운딩 박스 생성
     *
     * @param lat 중심 위도
     * @param lon 중심 경도
     * @param radiusKm 반경 (킬로미터)
     * @return bbox 문자열 (minLon,minLat,maxLon,maxLat)
     */
    fun createBoundingBox(lat: Double, lon: Double, radiusKm: Double): String {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(lat)))

        val minLat = lat - latDelta
        val maxLat = lat + latDelta
        val minLon = lon - abs(lonDelta)
        val maxLon = lon + abs(lonDelta)

        return "$minLon,$minLat,$maxLon,$maxLat"
    }

    /**
     * 네이버 지도 영역(LatLngBounds)으로부터 bbox 문자열 생성
     *
     * @param southWestLat 남서쪽 위도
     * @param southWestLon 남서쪽 경도
     * @param northEastLat 북동쪽 위도
     * @param northEastLon 북동쪽 경도
     * @return bbox 문자열 (minLon,minLat,maxLon,maxLat)
     */
    fun createBoundingBoxFromBounds(
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ): String {
        return "$southWestLon,$southWestLat,$northEastLon,$northEastLat"
    }
}

/**
 * 비행 가능 여부 판정 결과
 */
data class FlightPermissionResult(
    val canFly: Boolean,
    val level: FlightRestrictionLevel?,
    val zones: List<DroneZoneFeature>,
    val message: String
)
