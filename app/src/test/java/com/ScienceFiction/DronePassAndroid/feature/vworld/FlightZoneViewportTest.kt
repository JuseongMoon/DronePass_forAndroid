package com.ScienceFiction.DronePassAndroid.feature.vworld

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 비행금지구역(P-73) 한가운데로 확대하면 구역 꼭짓점이 모두 화면 밖이라 구역 표시가 사라졌다(3.6.1 에뮬레이터 재현).
 * 화면 안에 있는지는 경계 사각형이 겹치는지로 판단한다.
 */
class FlightZoneViewportTest {

    // 남산 부근을 덮는 큰 구역과, 그 안쪽 100m 축척 화면
    private val zone = doubleArrayOf(37.51, 126.94, 37.58, 127.02)
    private val zoomedInsideView = doubleArrayOf(37.548, 126.983, 37.556, 126.991)

    private fun intersects(z: DoubleArray, v: DoubleArray) =
        flightZoneIntersectsViewport(z[0], z[1], z[2], z[3], v[0], v[1], v[2], v[3])

    @Test
    fun `a zone that covers the whole screen stays visible`() {
        assertTrue(intersects(zone, zoomedInsideView))
    }

    @Test
    fun `a zone that crosses the screen without a vertex inside stays visible`() {
        // 길쭉한 구역이 화면을 가로지른다: 꼭짓점은 좌우 화면 밖
        val strip = doubleArrayOf(37.551, 126.90, 37.553, 127.10)
        assertTrue(intersects(strip, zoomedInsideView))
    }

    @Test
    fun `a zone away from the screen is hidden`() {
        val farZone = doubleArrayOf(35.10, 129.00, 35.20, 129.10)
        assertFalse(intersects(farZone, zoomedInsideView))
        // 화면 바로 북쪽에 붙어 있지만 겹치지 않는 구역
        assertFalse(intersects(doubleArrayOf(37.557, 126.983, 37.560, 126.991), zoomedInsideView))
    }
}
