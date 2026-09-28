package com.ScienceFiction.DronePassAndroid.feature.weather

import com.ScienceFiction.DronePassAndroid.core.util.CRICalculator
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class CRIContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "cri-cases.json")
        val missing = "Shared CRI fixture is missing: $file"
        if (System.getProperty("dronepass.requireSharedFixtures") == "true") {
            check(file.isFile) { missing }
        } else {
            assumeTrue(missing, file.isFile)
        }
        return requireNotNull(adapter.fromJson(file.readText()) as? Map<*, *>)
    }

    private fun Map<*, *>.required(key: String): Any = requireNotNull(this[key]) { "Missing $key" }
    private fun Map<*, *>.map(key: String): Map<*, *> = required(key) as? Map<*, *> ?: error("Invalid $key")
    private fun Map<*, *>.list(key: String): List<*> = required(key) as? List<*> ?: error("Invalid $key")
    private fun Map<*, *>.number(key: String): Double = (required(key) as? Number)?.toDouble() ?: error("Invalid $key")
    private fun Map<*, *>.string(key: String): String = required(key) as? String ?: error("Invalid $key")

    @Test fun `compiled formula matches shared iOS fixture`() {
        val root = fixture()
        assertEquals(1.0, root.number("schemaVersion"), 0.0)
        val formula = root.map("formula")
        assertEquals(
            listOf(CRICalculator.MIN_STABILIZED_TEMPERATURE_C, CRICalculator.MAX_STABILIZED_TEMPERATURE_C),
            formula.list("clampCelsius").map { (it as Number).toDouble() },
        )
        assertEquals(
            CRICalculator.CURVE,
            formula.list("curve").map { point ->
                val coordinates = point as? List<*> ?: error("Invalid curve point")
                require(coordinates.size == 2) { "Curve point must contain x and y" }
                (coordinates[0] as Number).toDouble() to (coordinates[1] as Number).toDouble()
            },
        )
        val fog = formula.map("fog")
        assertEquals(CRICalculator.FOG_VISIBILITY_KM_BELOW, fog.number("visibilityKmBelow"), 0.0)
        assertEquals(CRICalculator.FOG_MAX_SPREAD_C, fog.number("maxSpread"), 0.0)
        assertEquals(CRICalculator.FOG_MINIMUM_CRI, fog.number("minimumValue"), 0.0)
        assertEquals(
            listOf(CRICalculator.MIN_CRI, CRICalculator.MAX_CRI),
            formula.list("clampResult").map { (it as Number).toDouble() },
        )
        val levels = formula.map("levels")
        assertEquals(IosCriModerate, levels.number("caution"), 0.0)
        assertEquals(IosCriHigh, levels.number("warning"), 0.0)
    }

    @Test fun `all shared CRI cases match values and warning levels`() {
        val cases = fixture().list("cases")
        assertEquals(25, cases.size)
        cases.forEach { item ->
            val row = item as? Map<*, *> ?: error("Invalid CRI case")
            check(row.containsKey("visibilityKm")) { "Missing visibilityKm in ${row.string("name")}" }
            val visibility = row["visibilityKm"]?.let { (it as? Number)?.toDouble() ?: error("Invalid visibilityKm") }
            val actual = CRICalculator.calculate(row.number("temperature"), row.number("dewPoint"), visibility)
            assertEquals(row.string("name"), row.number("expectedCRI"), actual, 0.0)
            val expectedIcon = when (row.string("expectedLevel")) {
                "safe" -> WarningIconType.None
                "caution" -> WarningIconType.Caution
                "warning" -> WarningIconType.Warning
                else -> error("Unknown expectedLevel in ${row.string("name")}")
            }
            assertEquals(row.string("name"), expectedIcon, resolveCriWarningIcon(actual))
        }
    }
}
