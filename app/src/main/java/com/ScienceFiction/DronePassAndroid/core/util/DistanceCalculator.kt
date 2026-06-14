package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 지구상의 두 지점 간 거리를 계산하는 유틸리티.
 *
 * 두 가지 계산 방식을 제공한다:
 * - [haversine]: 정확한 거리 계산 (Haversine 공식)
 * - [fastApprox]: 빠른 근사 거리 계산 (Equirectangular 근사, 50km 이내 정확)
 *
 * 스케치 지우개 기능을 위한 점-선분 최단 거리 계산도 제공한다.
 */
object DistanceCalculator {

    /**
     * iOS DistanceCalculator.swift 와 동일한 평균 지구 반지름 (미터).
     *
     * 이 유틸리티는 스케치 샘플링/지우개 거리 계산에 쓰인다. VWorld 비행구역 계산은
     * iOS FlightZoneCalculator.swift 와 별도로 WGS-84 반지름을 사용한다.
     */
    private const val EARTH_RADIUS = 6_371_000.0

    // ──────────────────────────────────────────────
    // Haversine 공식 (정확한 거리)
    // ──────────────────────────────────────────────

    /**
     * Haversine 공식을 사용하여 두 좌표 간의 거리를 미터 단위로 계산한다.
     *
     * 구면 삼각법에 기반한 정확한 거리 계산으로,
     * 지구 어디서든 정확한 결과를 제공한다.
     *
     * @param lat1 시작점 위도 (도 단위)
     * @param lon1 시작점 경도 (도 단위)
     * @param lat2 끝점 위도 (도 단위)
     * @param lon2 끝점 경도 (도 단위)
     * @return 두 지점 간의 거리 (미터)
     */
    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(radLat1) * cos(radLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        // a ≈ 1 인 대원거리 입력에서 수치 안정성을 위해 atan2 사용 (iOS 원본과 동일)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }

    /**
     * Haversine 공식을 사용하여 두 Coordinate 간의 거리를 미터 단위로 계산한다.
     *
     * @param from 시작 좌표
     * @param to 끝 좌표
     * @return 두 지점 간의 거리 (미터)
     */
    fun haversine(from: Coordinate, to: Coordinate): Double {
        return haversine(from.latitude, from.longitude, to.latitude, to.longitude)
    }

    // ──────────────────────────────────────────────
    // Equirectangular 근사 (빠른 거리)
    // ──────────────────────────────────────────────

    /**
     * Equirectangular 근사를 사용하여 두 좌표 간의 거리를 미터 단위로 빠르게 계산한다.
     *
     * Haversine보다 계산이 빠르지만, 약 50km 이내의 거리에서만 정확하다.
     * 대량의 거리 비교가 필요한 경우(예: 가까운 도형 검색)에 적합하다.
     *
     * @param lat1 시작점 위도 (도 단위)
     * @param lon1 시작점 경도 (도 단위)
     * @param lat2 끝점 위도 (도 단위)
     * @param lon2 끝점 경도 (도 단위)
     * @return 두 지점 간의 근사 거리 (미터)
     */
    fun fastApprox(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)
        val radLon1 = Math.toRadians(lon1)
        val radLon2 = Math.toRadians(lon2)

        val x = (radLon2 - radLon1) * cos((radLat1 + radLat2) / 2)
        val y = radLat2 - radLat1

        return sqrt(x * x + y * y) * EARTH_RADIUS
    }

    /**
     * Equirectangular 근사를 사용하여 두 Coordinate 간의 거리를 미터 단위로 빠르게 계산한다.
     *
     * @param from 시작 좌표
     * @param to 끝 좌표
     * @return 두 지점 간의 근사 거리 (미터)
     */
    fun fastApprox(from: Coordinate, to: Coordinate): Double {
        return fastApprox(from.latitude, from.longitude, to.latitude, to.longitude)
    }

    // ──────────────────────────────────────────────
    // 점-선분 최단 거리 (스케치 지우개용)
    // ──────────────────────────────────────────────

    /**
     * 점에서 선분까지의 최단 거리를 미터 단위로 계산한다.
     *
     * 스케치 지우개 기능에서 사용자의 터치 지점과 스케치 선분 사이의
     * 거리를 판단할 때 사용한다.
     *
     * 선분의 수선의 발이 선분 위에 있으면 수선의 길이를,
     * 선분 밖에 있으면 선분의 양 끝점까지의 거리 중 짧은 값을 반환한다.
     *
     * @param point 기준 점
     * @param segmentStart 선분의 시작점
     * @param segmentEnd 선분의 끝점
     * @return 점에서 선분까지의 최단 거리 (미터)
     */
    fun distanceToSegment(
        point: Coordinate,
        segmentStart: Coordinate,
        segmentEnd: Coordinate
    ): Double {
        // 선분의 길이가 0인 경우 (시작점과 끝점이 동일)
        if (segmentStart.latitude == segmentEnd.latitude &&
            segmentStart.longitude == segmentEnd.longitude
        ) {
            return haversine(point, segmentStart)
        }

        // iOS DistanceCalculator.toSegment 와 동일하게 위도/경도 degree 평면에서 투영한다.
        val pointX = point.longitude
        val pointY = point.latitude
        val startX = segmentStart.longitude
        val startY = segmentStart.latitude
        val endX = segmentEnd.longitude
        val endY = segmentEnd.latitude

        val dx = endX - startX
        val dy = endY - startY

        // 선분의 제곱 길이
        val segmentLengthSq = dx * dx + dy * dy

        // 점에서 선분 시작점까지의 벡터
        val px = pointX - startX
        val py = pointY - startY

        // 선분 위의 투영 비율 (0..1 범위로 클램프)
        val t = ((px * dx + py * dy) / segmentLengthSq).coerceIn(0.0, 1.0)

        val closestLon = startX + t * dx
        val closestLat = startY + t * dy

        return haversine(point.latitude, point.longitude, closestLat, closestLon)
    }
}
