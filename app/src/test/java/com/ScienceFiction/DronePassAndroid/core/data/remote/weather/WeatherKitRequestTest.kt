package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 날씨 요청에 측위 원본 좌표가 실리지 않는지 고정한다.
 * 기대값은 서버 `gridCoordinate`(Node)로 계산한 결과다.
 */
class WeatherKitRequestTest {

    @Test
    fun `request carries grid coordinates instead of raw device location`() {
        val request = weatherKitRequest(37.566535, 126.977969, "Asia/Seoul")

        assertEquals(setOf("latitude", "longitude", "timezone"), request.keys)
        assertEquals(37.57, request["latitude"] as Double, 0.0)
        assertEquals(126.98, request["longitude"] as Double, 0.0)
        assertEquals("Asia/Seoul", request["timezone"])
    }

    @Test
    fun `grid coordinate matches server gridCoordinate`() {
        val cases = listOf(
            37.5665 to 37.57,
            126.978 to 126.98,
            // .xx5 경계: 이진 표현이 살짝 작아도 1e-10 보정으로 올림된다.
            37.565 to 37.57,
            126.975 to 126.98,
            1.005 to 1.01,
            2.675 to 2.68,
            0.015 to 0.02,
            0.005 to 0.01,
            37.56499999 to 37.56,
            126.98499 to 126.98,
            // 음수는 절댓값 기준으로 반올림한다.
            -33.865 to -33.87,
            -151.205 to -151.21,
            -1.005 to -1.01,
            -0.005 to -0.01,
            // 범위 끝.
            90.0 to 90.0,
            -90.0 to -90.0,
            180.0 to 180.0,
            -180.0 to -180.0,
        )
        for ((input, expected) in cases) {
            assertEquals("input=$input", expected, weatherGridCoordinate(input), 0.0)
        }
    }

    @Test
    fun `grid coordinate is idempotent so server rounding keeps the grid`() {
        val inputs = listOf(37.5665, 126.975, -33.865, 1.005, -0.005, 0.0)
        for (input in inputs) {
            val once = weatherGridCoordinate(input)
            assertEquals("input=$input", once, weatherGridCoordinate(once), 0.0)
        }
    }

    @Test
    fun `negative zero becomes positive zero`() {
        for (input in listOf(-0.004, -0.0, 0.0)) {
            val result = weatherGridCoordinate(input)
            assertEquals(0.0, result, 0.0)
            assertTrue("input=$input", 1.0 / result > 0)
        }
    }

    @Test
    fun `non finite values pass through for the server to reject`() {
        assertTrue(weatherGridCoordinate(Double.NaN).isNaN())
        assertEquals(Double.POSITIVE_INFINITY, weatherGridCoordinate(Double.POSITIVE_INFINITY), 0.0)
    }
}
