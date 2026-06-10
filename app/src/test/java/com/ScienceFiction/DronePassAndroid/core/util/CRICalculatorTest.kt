package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CRICalculator 단위 테스트
 *
 * 비정상 입력(NaN/Infinity)에 대해 안전 sentinel(NaN)이 반환되는지,
 * 유한한 극값은 iOS WeatherManager처럼 안정화되어 1..100 범위의 값이 반환되는지를 검증한다.
 */
class CRICalculatorTest {

    // region 정상 입력

    @Test
    fun `포화 상태(온도=이슬점)는 deltaT=0이라 매우 높은 CRI`() {
        val cri = CRICalculator.calculate(temperature = 20.0, dewPoint = 20.0, windSpeed = 0.0)
        assertTrue("포화 상태 CRI는 90 이상이어야 함 (실제: $cri)", cri >= 90.0)
        assertTrue("CRI는 1..100 범위", cri in 1.0..100.0)
    }

    @Test
    fun `건조한 대기(deltaT=20)는 낮은 CRI`() {
        // 온도 25°C, 이슬점 5°C → deltaT=20 → criDeltaT=100-200=-100 → clamp 1
        val cri = CRICalculator.calculate(temperature = 25.0, dewPoint = 5.0, windSpeed = 0.0)
        assertTrue("건조한 대기 CRI는 ≤ 50 (실제: $cri)", cri <= 50.0)
    }

    @Test
    fun `강풍은 CRI를 RH 이하로 떨어뜨리지 않는다 (하한 보정)`() {
        // 포화 상태 + 강풍 10m/s → criCombined=100, windFactor=0.8 → 80
        // 하지만 하한 = criRH = 100 → 결과 100
        val cri = CRICalculator.calculate(temperature = 20.0, dewPoint = 20.0, windSpeed = 10.0)
        assertEquals(100.0, cri, 0.01)
    }

    @Test
    fun `최종 CRI는 iOS WeatherManager처럼 정수로 반올림된다`() {
        val cri = CRICalculator.calculate(temperature = 20.0, dewPoint = 15.0, windSpeed = 10.0)

        assertEquals(73.0, cri, 0.0)
    }

    @Test
    fun `현재 CRI smoother는 iOS WeatherManager처럼 최근 5틱 이동평균 후 반올림한다`() {
        val smoother = CurrentCriSmoother()

        assertEquals(10.0, smoother.smooth(10.0), 0.0)
        assertEquals(20.0, smoother.smooth(30.0), 0.0)
        assertEquals(30.0, smoother.smooth(50.0), 0.0)
        assertEquals(40.0, smoother.smooth(70.0), 0.0)
        assertEquals(50.0, smoother.smooth(90.0), 0.0)
        assertEquals(60.0, smoother.smooth(60.0), 0.0)
    }

    @Test
    fun `현재 CRI smoother는 비정상 입력을 버퍼에 넣지 않는다`() {
        val smoother = CurrentCriSmoother()

        assertEquals(50.0, smoother.smooth(50.0), 0.0)
        assertTrue(smoother.smooth(Double.NaN).isNaN())
        assertEquals(60.0, smoother.smooth(70.0), 0.0)
    }

    // endregion

    // region 비정상 입력 가드

    @Test
    fun `NaN 입력은 NaN 반환`() {
        assertTrue(CRICalculator.calculate(Double.NaN, 20.0, 0.0).isNaN())
        assertTrue(CRICalculator.calculate(20.0, Double.NaN, 0.0).isNaN())
        assertTrue(CRICalculator.calculate(20.0, 20.0, Double.NaN).isNaN())
    }

    @Test
    fun `Infinity 입력은 NaN 반환`() {
        assertTrue(CRICalculator.calculate(Double.POSITIVE_INFINITY, 20.0, 0.0).isNaN())
        assertTrue(CRICalculator.calculate(Double.NEGATIVE_INFINITY, 20.0, 0.0).isNaN())
        assertTrue(CRICalculator.calculate(20.0, Double.POSITIVE_INFINITY, 0.0).isNaN())
    }

    @Test
    fun `음수 풍속은 0으로 클램프되어 NaN이 아닌 정상 값 반환`() {
        // 음수 풍속(API 오류)이 들어와도 CRI 자체는 계산되어야 함
        val cri = CRICalculator.calculate(temperature = 20.0, dewPoint = 18.0, windSpeed = -3.0)
        assertFalse("음수 풍속은 NaN이 아님", cri.isNaN())
        assertTrue("CRI 1..100", cri in 1.0..100.0)
    }

    @Test
    fun `유한한 온도 이슬점 극값은 iOS WeatherManager처럼 안정화되어 계산된다`() {
        assertEquals(100.0, CRICalculator.calculate(-300.0, 20.0, 0.0), 0.0)
        assertEquals(1.0, CRICalculator.calculate(20.0, -300.0, 0.0), 0.0)
        assertFalse(CRICalculator.calculate(-244.0, 0.0, 0.0).isNaN())
    }

    // endregion

    // region 일관성

    @Test
    fun `결과는 항상 1과 100 사이`() {
        // 다양한 입력 조합
        val cases = listOf(
            Triple(0.0, -10.0, 0.0),
            Triple(30.0, 25.0, 1.0),
            Triple(-30.0, -40.0, 0.5),
            Triple(15.0, 14.9, 0.0),
            Triple(50.0, 49.0, 0.0)
        )
        for ((t, dp, w) in cases) {
            val cri = CRICalculator.calculate(t, dp, w)
            assertFalse("정상 입력 ($t, $dp, $w) 결과는 NaN 아님", cri.isNaN())
            assertTrue("CRI 범위 (입력=$t, $dp, $w → $cri)", cri in 1.0..100.0)
        }
    }

    // endregion
}
