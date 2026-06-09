package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class SketchSmoothingAlgorithmTest {

    @Test
    fun `스케치 스무딩은 iOS처럼 포인트가 2개 미만이면 원본을 반환한다`() {
        val point = Coordinate(latitude = 37.0, longitude = 127.0)

        assertEquals(emptyList<Coordinate>(), SketchSmoothingAlgorithm.smoothUsingCatmullRom(emptyList()))
        assertEquals(listOf(point), SketchSmoothingAlgorithm.smoothUsingCatmullRom(listOf(point)))
    }

    @Test
    fun `스케치 스무딩은 iOS처럼 2개 포인트를 선형 보간한다`() {
        val smoothed = SketchSmoothingAlgorithm.smoothUsingCatmullRom(
            points = listOf(
                Coordinate(latitude = 0.0, longitude = 0.0),
                Coordinate(latitude = 10.0, longitude = 20.0),
            ),
            segmentsPerOriginal = 2,
        )

        assertEquals(
            listOf(
                Coordinate(latitude = 0.0, longitude = 0.0),
                Coordinate(latitude = 5.0, longitude = 10.0),
                Coordinate(latitude = 10.0, longitude = 20.0),
            ),
            smoothed,
        )
    }

    @Test
    fun `스케치 스무딩은 iOS처럼 3개 이상 포인트에 Catmull Rom 가상 끝점을 적용한다`() {
        val smoothed = SketchSmoothingAlgorithm.smoothUsingCatmullRom(
            points = listOf(
                Coordinate(latitude = 0.0, longitude = 0.0),
                Coordinate(latitude = 0.0, longitude = 10.0),
                Coordinate(latitude = 10.0, longitude = 10.0),
            ),
            segmentsPerOriginal = 2,
        )

        assertEquals(5, smoothed.size)
        assertCoordinateEquals(0.0, 0.0, smoothed[0])
        assertCoordinateEquals(-0.625, 5.625, smoothed[1])
        assertCoordinateEquals(0.0, 10.0, smoothed[2])
        assertCoordinateEquals(4.375, 10.625, smoothed[3])
        assertCoordinateEquals(10.0, 10.0, smoothed[4])
    }

    @Test
    fun `스케치 스무딩 자동 세그먼트 수는 iOS 포인트 개수 규칙을 따른다`() {
        assertEquals(11, smoothStraightLine(pointCount = 2).size)
        assertEquals(481, smoothStraightLine(pointCount = 49).size)
        assertEquals(344, smoothStraightLine(pointCount = 50).size)
        assertEquals(496, smoothStraightLine(pointCount = 100).size)
    }

    @Test
    fun `스케치 스무딩은 iOS처럼 보간 세그먼트가 0 이하이면 원본을 반환한다`() {
        val points = listOf(
            Coordinate(latitude = 0.0, longitude = 0.0),
            Coordinate(latitude = 10.0, longitude = 20.0),
        )

        assertEquals(points, SketchSmoothingAlgorithm.smoothUsingCatmullRom(points, segmentsPerOriginal = 0))
        assertEquals(points, SketchSmoothingAlgorithm.smoothUsingCatmullRom(points, segmentsPerOriginal = -1))
    }

    private fun smoothStraightLine(pointCount: Int): List<Coordinate> {
        val points = List(pointCount) { index ->
            Coordinate(latitude = index.toDouble(), longitude = index.toDouble())
        }
        return SketchSmoothingAlgorithm.smoothUsingCatmullRom(points)
    }

    private fun assertCoordinateEquals(
        expectedLatitude: Double,
        expectedLongitude: Double,
        actual: Coordinate,
    ) {
        assertEquals(expectedLatitude, actual.latitude, 0.000001)
        assertEquals(expectedLongitude, actual.longitude, 0.000001)
    }
}
