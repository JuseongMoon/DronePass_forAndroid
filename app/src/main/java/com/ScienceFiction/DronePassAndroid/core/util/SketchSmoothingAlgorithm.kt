package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate

/**
 * Catmull-Rom 스플라인 보간 알고리즘을 사용한 스케치 포인트 스무딩.
 *
 * 사용자가 터치로 그린 거친 포인트들을 부드러운 곡선으로 변환한다.
 * - 포인트 1개: 그대로 반환
 * - 포인트 2개: 선형 보간
 * - 포인트 3개 이상: Catmull-Rom 스플라인 보간
 */
object SketchSmoothingAlgorithm {

    /** Catmull-Rom 스플라인 장력 (0.5 = centripetal) */
    private const val TENSION = 0.5

    /**
     * Catmull-Rom 스플라인 보간으로 포인트를 스무딩한다.
     *
     * @param points 원본 포인트 리스트
     * @param segmentsPerOriginal 원본 포인트 간 보간 세그먼트 수.
     *   null이면 포인트 수에 따라 자동 결정:
     *   - 100개 이상: 5
     *   - 50~99개: 7
     *   - 그 외: 10
     * @return 스무딩된 포인트 리스트
     */
    fun smoothUsingCatmullRom(
        points: List<Coordinate>,
        segmentsPerOriginal: Int? = null
    ): List<Coordinate> {
        if (points.size <= 1) return points
        if (points.size == 2) return linearInterpolation(points[0], points[1])

        val segments = segmentsPerOriginal ?: dynamicSegmentCount(points.size)
        // segments == 0/음수 가드 (iOS: guard segments > 0). 보간 불가하면 원본 반환.
        if (segments <= 0) return points

        // 가상 끝점 생성 (자연스러운 시작/끝을 위해)
        val extendedPoints = buildList {
            // 시작 가상점: 첫 점에서 두 번째 점 방향의 반대로 연장
            val virtualStart = Coordinate(
                latitude = 2.0 * points[0].latitude - points[1].latitude,
                longitude = 2.0 * points[0].longitude - points[1].longitude
            )
            add(virtualStart)
            addAll(points)
            // 끝 가상점: 마지막 점에서 끝에서 두 번째 점 방향의 반대로 연장
            val last = points.last()
            val secondLast = points[points.size - 2]
            val virtualEnd = Coordinate(
                latitude = 2.0 * last.latitude - secondLast.latitude,
                longitude = 2.0 * last.longitude - secondLast.longitude
            )
            add(virtualEnd)
        }

        val result = mutableListOf<Coordinate>()
        // 첫 원본 포인트 (t=0 위치)를 명시 추가 — iOS 원본 알고리즘과 동일.
        result.add(points.first())

        // 각 원본 구간(i=1..extendedPoints.size-3)에 대해 t ∈ (0, 1] 구간을 보간.
        // iOS: `for j in 1...segments { t = j/segments }`. 마지막 t=1 위치가 다음 구간의
        // p2(=원본 포인트)와 일치하므로 별도 endpoint 추가가 필요 없다.
        for (i in 1 until extendedPoints.size - 2) {
            val p0 = extendedPoints[i - 1]
            val p1 = extendedPoints[i]
            val p2 = extendedPoints[i + 1]
            val p3 = extendedPoints[i + 2]

            for (s in 1..segments) {
                val t = s.toDouble() / segments
                val coord = catmullRomPoint(p0, p1, p2, p3, t)
                result.add(coord)
            }
        }

        return result
    }

    /**
     * Catmull-Rom 보간으로 t 위치의 좌표를 계산한다.
     *
     * @param p0 이전 제어점
     * @param p1 시작 제어점
     * @param p2 끝 제어점
     * @param p3 다음 제어점
     * @param t 보간 매개변수 (0.0 ~ 1.0)
     */
    private fun catmullRomPoint(
        p0: Coordinate,
        p1: Coordinate,
        p2: Coordinate,
        p3: Coordinate,
        t: Double
    ): Coordinate {
        val t2 = t * t
        val t3 = t2 * t

        val lat = TENSION * (
            (2.0 * p1.latitude) +
            (-p0.latitude + p2.latitude) * t +
            (2.0 * p0.latitude - 5.0 * p1.latitude + 4.0 * p2.latitude - p3.latitude) * t2 +
            (-p0.latitude + 3.0 * p1.latitude - 3.0 * p2.latitude + p3.latitude) * t3
        )

        val lng = TENSION * (
            (2.0 * p1.longitude) +
            (-p0.longitude + p2.longitude) * t +
            (2.0 * p0.longitude - 5.0 * p1.longitude + 4.0 * p2.longitude - p3.longitude) * t2 +
            (-p0.longitude + 3.0 * p1.longitude - 3.0 * p2.longitude + p3.longitude) * t3
        )

        return Coordinate(latitude = lat, longitude = lng)
    }

    /**
     * 포인트 수에 따라 세그먼트 수를 동적으로 결정한다.
     * 포인트가 많을수록 세그먼트를 줄여 성능을 확보한다.
     */
    private fun dynamicSegmentCount(pointCount: Int): Int {
        return when {
            pointCount >= 100 -> 5
            pointCount >= 50 -> 7
            else -> 10
        }
    }

    /**
     * 두 포인트 사이의 선형 보간.
     * 10개의 중간점을 생성한다.
     */
    private fun linearInterpolation(
        start: Coordinate,
        end: Coordinate
    ): List<Coordinate> {
        val segments = 10
        return (0..segments).map { i ->
            val t = i.toDouble() / segments
            Coordinate(
                latitude = start.latitude + (end.latitude - start.latitude) * t,
                longitude = start.longitude + (end.longitude - start.longitude) * t
            )
        }
    }
}
