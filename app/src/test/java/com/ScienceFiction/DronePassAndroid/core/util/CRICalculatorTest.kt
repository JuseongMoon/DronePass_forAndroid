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
}
