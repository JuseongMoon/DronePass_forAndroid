package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun `스케치 거리 계산은 iOS DistanceCalculator 평균 지구 반지름을 사용한다`() {
        val from = Coordinate(latitude = 37.0, longitude = 127.0)
        val to = Coordinate(latitude = 37.001, longitude = 127.0)

        assertEquals(111.19492664455875, DistanceCalculator.haversine(from, to), 0.000001)
        assertEquals(111.19492664455875, DistanceCalculator.fastApprox(from, to), 0.000001)
    }

    @Test
    fun `점 선분 거리는 iOS처럼 위경도 degree 평면에서 투영한다`() {
        val point = Coordinate(latitude = 37.0, longitude = 128.0)
        val segmentStart = Coordinate(latitude = 37.0, longitude = 127.0)
        val segmentEnd = Coordinate(latitude = 38.0, longitude = 128.0)

        assertEquals(
            71060.57746548571,
            DistanceCalculator.distanceToSegment(point, segmentStart, segmentEnd),
            0.000001,
        )
    }
}
