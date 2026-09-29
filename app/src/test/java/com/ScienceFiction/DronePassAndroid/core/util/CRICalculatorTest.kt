package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CRICalculatorTest {
    @Test fun `half values round upward after interpolation`() {
        assertEquals(27.5, CRICalculator.calculateUnrounded(20.0, 15.75, null), 0.0)
        assertEquals(28.0, CRICalculator.calculate(20.0, 15.75, null), 0.0)
    }

    @Test fun `fog adjustment needs both low visibility and small dew point spread`() {
        assertEquals(90.0, CRICalculator.calculate(25.0, 22.5, 0.5), 0.0)
        assertEquals(55.0, CRICalculator.calculate(25.0, 22.5, 1.0), 0.0)
        assertEquals(39.0, CRICalculator.calculate(25.0, 21.9, 0.5), 0.0)
    }

    @Test fun `nonfinite temperature or dew point returns NaN`() {
        assertTrue(CRICalculator.calculate(Double.NaN, 20.0, null).isNaN())
        assertTrue(CRICalculator.calculate(20.0, Double.POSITIVE_INFINITY, null).isNaN())
        assertTrue(CRICalculator.calculate(Double.NEGATIVE_INFINITY, 20.0, 0.5).isNaN())
    }

    @Test fun `finite extreme temperatures are clamped`() {
        assertEquals(100.0, CRICalculator.calculate(-300.0, 20.0, null), 0.0)
        assertEquals(1.0, CRICalculator.calculate(20.0, -300.0, null), 0.0)
    }

    @Test fun `current CRI smoother averages five unrounded samples before rounding`() {
        val smoother = CurrentCriSmoother()
        assertEquals(10.0, smoother.smooth(10.0), 0.0)
        assertEquals(20.0, smoother.smooth(30.0), 0.0)
        assertEquals(30.0, smoother.smooth(50.0), 0.0)
        assertEquals(40.0, smoother.smooth(70.0), 0.0)
        assertEquals(50.0, smoother.smooth(90.0), 0.0)
        assertEquals(60.0, smoother.smooth(60.0), 0.0)
    }

    @Test fun `invalid sample does not enter smoothing buffer`() {
        val smoother = CurrentCriSmoother()
        assertEquals(50.0, smoother.smooth(50.0), 0.0)
        assertTrue(smoother.smooth(Double.NaN).isNaN())
        assertEquals(60.0, smoother.smooth(70.0), 0.0)
    }

    // iOS DronePassTests/CRISmootherTests.swift 와 같은 사례. 3분 = 샘플 간격, 15분 초과 = 초기화.
    private val minute = 60_000L

    @Test fun `smoother averages samples inside the window`() {
        val smoother = CurrentCriSmoother()
        assertEquals(10.0, smoother.smooth(10.0, atMillis = 0L), 0.0)
        assertEquals(20.0, smoother.smooth(30.0, atMillis = 3 * minute), 0.0)
    }

    @Test fun `smoother keeps only the latest five samples`() {
        val smoother = CurrentCriSmoother()
        listOf(100.0, 0.0, 0.0, 0.0, 0.0).forEachIndexed { index, cri ->
            smoother.smooth(cri, atMillis = index * 3 * minute)
        }
        // 0 은 1..100 으로 제한되어 1 이 된다. 100 이 창에서 빠지면 평균은 1 이다.
        assertEquals(1.0, smoother.smooth(0.0, atMillis = 15 * minute), 0.0)
        assertEquals(5, smoother.sampleCount)
    }

    @Test fun `gap of 14 minutes 59 seconds keeps the average`() {
        val smoother = CurrentCriSmoother()
        smoother.smooth(80.0, atMillis = 0L)
        assertEquals(50.0, smoother.smooth(20.0, atMillis = 15 * minute - 1_000L), 0.0)
    }

    @Test fun `gap of exactly 15 minutes keeps the average`() {
        val smoother = CurrentCriSmoother()
        smoother.smooth(80.0, atMillis = 0L)
        assertEquals(50.0, smoother.smooth(20.0, atMillis = 15 * minute), 0.0)
        assertEquals(2, smoother.sampleCount)
    }

    @Test fun `gap longer than 15 minutes restarts the average`() {
        val smoother = CurrentCriSmoother()
        smoother.smooth(80.0, atMillis = 0L)
        smoother.smooth(80.0, atMillis = 3 * minute)
        assertEquals(20.0, smoother.smooth(20.0, atMillis = 3 * minute + 15 * minute + 1_000L), 0.0)
        assertEquals(1, smoother.sampleCount)
    }

    @Test fun `clock moving backwards keeps the average`() {
        val smoother = CurrentCriSmoother()
        smoother.smooth(80.0, atMillis = 60 * minute)
        assertEquals(50.0, smoother.smooth(20.0, atMillis = 0L), 0.0)
    }

    @Test fun `default clock is injectable`() {
        var now = 0L
        val smoother = CurrentCriSmoother(nowMillis = { now })
        smoother.smooth(80.0)
        now = 16 * minute
        assertEquals(20.0, smoother.smooth(20.0), 0.0)
    }
}
