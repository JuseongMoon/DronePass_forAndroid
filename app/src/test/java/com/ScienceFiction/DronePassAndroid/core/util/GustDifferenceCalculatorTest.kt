package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GustDifferenceCalculatorTest {

    @Before
    fun resetCalculatorState() {
        GustDifferenceCalculator.resetForTesting()
    }

    @Test
    fun `class3 low sustained wind uses iOS localized gust policy`() {
        val level = GustDifferenceCalculator.evaluate(
            sustainedWind = 2.0,
            gustWind = 8.0,
            category = DroneCategory.CLASS3
        )

        assertEquals(GustDifferenceLevel.LOCALIZED_GUST, level)
    }

    @Test
    fun `class3 very low sustained wind needs two localized gust votes like iOS`() {
        val level = GustDifferenceCalculator.evaluate(
            sustainedWind = 0.5,
            gustWind = 7.0,
            category = DroneCategory.CLASS3
        )

        assertEquals(GustDifferenceLevel.SAFE, level)
    }

    @Test
    fun `single danger axis becomes caution like iOS`() {
        val level = GustDifferenceCalculator.evaluate(
            sustainedWind = 10.0,
            gustWind = 13.1,
            category = DroneCategory.CLASS3
        )

        assertEquals(GustDifferenceLevel.CAUTION, level)
    }

    @Test
    fun `toy absolute gust uses iOS separated gust danger threshold`() {
        val level = GustDifferenceCalculator.evaluate(
            sustainedWind = 8.0,
            gustWind = 9.5,
            category = DroneCategory.TOY
        )

        assertEquals(GustDifferenceLevel.SAFE, level)
    }

    @Test
    fun `gust difference is never negative when observed gust is below sustained wind`() {
        val difference = GustDifferenceCalculator.calculateGustDifference(
            sustainedWind = 8.0,
            gustWind = 6.0
        )

        assertEquals(0.0, difference, 0.0)
    }

    @Test
    fun `caution level uses iOS hysteresis before returning to safe`() {
        assertEquals(
            GustDifferenceLevel.CAUTION,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 10.0,
                gustWind = 13.1,
                category = DroneCategory.CLASS3,
            ),
        )

        assertEquals(
            GustDifferenceLevel.CAUTION,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 10.0,
                gustWind = 12.1,
                category = DroneCategory.CLASS3,
            ),
        )

        assertEquals(
            GustDifferenceLevel.SAFE,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 9.0,
                gustWind = 10.0,
                category = DroneCategory.CLASS3,
            ),
        )
    }

    @Test
    fun `class3 localized gust turns off when votes drop to iOS hysteresis threshold`() {
        assertEquals(
            GustDifferenceLevel.LOCALIZED_GUST,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 2.0,
                gustWind = 8.0,
                category = DroneCategory.CLASS3,
            ),
        )

        assertEquals(
            GustDifferenceLevel.SAFE,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 2.0,
                gustWind = 4.0,
                category = DroneCategory.CLASS3,
            ),
        )
    }

    @Test
    fun `toy localized gust keeps warning with one vote until iOS zero vote release`() {
        assertEquals(
            GustDifferenceLevel.LOCALIZED_GUST,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 2.0,
                gustWind = 5.0,
                category = DroneCategory.TOY,
            ),
        )

        assertEquals(
            GustDifferenceLevel.LOCALIZED_GUST,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 2.0,
                gustWind = 3.1,
                category = DroneCategory.TOY,
            ),
        )

        assertEquals(
            GustDifferenceLevel.SAFE,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 2.0,
                gustWind = 2.5,
                category = DroneCategory.TOY,
            ),
        )
    }

    @Test
    fun `danger level demotes to caution when iOS danger votes fall to one`() {
        assertEquals(
            GustDifferenceLevel.DANGER,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 7.0,
                gustWind = 11.0,
                category = DroneCategory.TOY,
            ),
        )

        assertEquals(
            GustDifferenceLevel.CAUTION,
            GustDifferenceCalculator.evaluate(
                sustainedWind = 7.0,
                gustWind = 10.8,
                category = DroneCategory.TOY,
            ),
        )
    }
}
