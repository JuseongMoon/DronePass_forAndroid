package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightRestrictionLevel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 비행구역 관련 계산 유틸리티
 *
 * - 포인트 인 폴리곤 (Ray Casting Algorithm)
 * - 거리 계산 (Haversine Formula, WGS-84 적도반지름)
 * - 비행 가능 여부 판정
 * - 바운딩 박스 생성
 */
object FlightZoneCalculator {

    /**
     * WGS-84 적도 반지름 (미터).
     * iOS 원본(FlightZoneCalculator.swift) 및 [DistanceCalculator]와 동일한 값.
     */
    private const val EARTH_RADIUS_M = 6_378_137.0

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
        // a ≈ 1 인 대원거리 입력에서 수치 안정성을 위해 atan2 사용 (iOS 원본과 동일)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /**
     * 특정 좌표에서의 비행 가능 여부 판정
     *
     * 비행금지(PROHIBITED), 비행제한(RESTRICTED), 사전협의(CONSULTATION) 구역
     * 내부이면 iOS 처럼 비행 불가/승인 필요로 판정한다.
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
        val prohibitedZones = mutableListOf<DroneZoneFeature>()
        val restrictedZones = mutableListOf<DroneZoneFeature>()
        val advisoryZones = mutableListOf<DroneZoneFeature>()

        for (zone in zones) {
            for (polygon in zone.polygons) {
                if (isPointInPolygon(lat, lon, polygon)) {
                    when (zone.layer.restrictionLevel) {
                        FlightRestrictionLevel.PROHIBITED -> prohibitedZones.add(zone)
                        FlightRestrictionLevel.RESTRICTED,
                        FlightRestrictionLevel.CONSULTATION -> restrictedZones.add(zone)
                        FlightRestrictionLevel.ADVISORY -> advisoryZones.add(zone)
                    }
                    break // 하나의 폴리곤에서 포함되면 다음 구역으로
                }
            }
        }

        return when {
            prohibitedZones.isNotEmpty() -> FlightPermissionResult(
                canFly = false,
                level = FlightRestrictionLevel.PROHIBITED,
                zones = prohibitedZones,
                message = "${prohibitedZones.layerNames()} 구역입니다. 비행이 금지되어 있습니다."
            )
            restrictedZones.isNotEmpty() -> FlightPermissionResult(
                canFly = false,
                level = FlightRestrictionLevel.RESTRICTED,
                zones = restrictedZones,
                message = "${restrictedZones.layerNames()} 구역입니다. 비행 승인이 필요합니다."
            )
            advisoryZones.isNotEmpty() -> FlightPermissionResult(
                canFly = true,
                level = FlightRestrictionLevel.ADVISORY,
                zones = advisoryZones,
                message = "비행 가능하나 주의가 필요한 지역입니다."
            )
            else -> FlightPermissionResult(
                canFly = true,
                level = FlightRestrictionLevel.ADVISORY,
                zones = emptyList(),
                message = "해당 지역은 비행 가능합니다."
            )
        }
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
        // 위도 1° ≈ 111.32km (WGS-84 평균). 단축 표기로 111.0 사용 — bbox 는 안전 마진이
        // 있어 0.3% 오차 허용 가능. 정밀 거리 비교에는 [distance] 사용.
        val latDelta = radiusKm / KM_PER_DEGREE_LAT
        val lonDelta = radiusKm / (KM_PER_DEGREE_LAT * cos(Math.toRadians(lat)))

        val minLat = lat - latDelta
        val maxLat = lat + latDelta
        val minLon = lon - abs(lonDelta)
        val maxLon = lon + abs(lonDelta)

        return "$minLon,$minLat,$maxLon,$maxLat"
    }

    /** 위도 1° 당 거리 (km), bbox 근사 계산용. */
    private const val KM_PER_DEGREE_LAT = 111.0

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

private fun List<DroneZoneFeature>.layerNames(): String =
    joinToString(separator = ", ") { it.layer.displayName }

/**
 * 비행 가능 여부 판정 결과
 */
data class FlightPermissionResult(
    val canFly: Boolean,
    val level: FlightRestrictionLevel?,
    val zones: List<DroneZoneFeature>,
    val message: String
)
