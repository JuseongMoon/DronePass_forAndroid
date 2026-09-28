package com.ScienceFiction.DronePassAndroid.subscription

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.time.Instant

class SubscriptionContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory")
            ?: error("IOS_PROJECT_DIR is required for SubscriptionContractTest")
        val file = File(directory, "quota-cases.json")
        check(file.isFile) { "Shared quota fixture is missing: $file" }
        return requireNotNull(adapter.fromJson(file.readText()) as? Map<*, *>)
    }

    private fun Map<*, *>.required(key: String): Any = requireNotNull(this[key]) { "Missing $key" }
    private fun Map<*, *>.string(key: String): String = required(key) as? String ?: error("Invalid $key")
    private fun Map<*, *>.int(key: String): Int = (required(key) as? Number)?.toInt() ?: error("Invalid $key")
    private fun Map<*, *>.bool(key: String): Boolean = required(key) as? Boolean ?: error("Invalid $key")
    private fun Map<*, *>.map(key: String): Map<*, *> = required(key) as? Map<*, *> ?: error("Invalid $key")
    private fun Map<*, *>.cases(key: String): List<Map<*, *>> =
        (required(key) as? List<*>)?.map { it as? Map<*, *> ?: error("Invalid $key row") } ?: error("Invalid $key")
    private fun Map<*, *>.optionalString(key: String): String? {
        check(containsKey(key)) { "Missing $key" }
        return this[key]?.let { it as? String ?: error("Invalid $key") }
    }
    private fun Map<*, *>.optionalMap(key: String): Map<*, *>? {
        check(containsKey(key)) { "Missing $key" }
        return this[key]?.let { it as? Map<*, *> ?: error("Invalid $key") }
    }
    private fun Map<*, *>.limits(): QuotaLimits = QuotaLimits(int("freeShapes"), int("freeSketches"), int("freeDrones"))
    private fun Map<*, *>.cutoff(): LegacyCutoff = LegacyCutoff(int("iosOriginalBuildBefore"), Instant.parse(string("accountCreatedBefore")))

    @Test fun `compiled defaults match shared fixture`() {
        val root = fixture()
        assertEquals(QuotaLimits.fallback, root.map("limits").limits())
        assertEquals(LegacyCutoff.fallback, root.map("legacyCutoff").cutoff())
    }

    @Test fun `all decision cases match shared fixture`() {
        fixture().cases("decisionCases").forEach { row ->
            val action = when (row.string("action")) {
                "createShape" -> QuotaAction.CREATE_SHAPE
                "duplicateShape" -> QuotaAction.DUPLICATE_SHAPE
                "startSketchStroke" -> QuotaAction.START_SKETCH_STROKE
                "addDrone" -> QuotaAction.ADD_DRONE
                else -> error("Unknown action: ${row.string("action")}")
            }
            val entitlement = when (row.string("entitlement")) {
                "unknown" -> EntitlementState.UNKNOWN
                "free" -> EntitlementState.FREE
                "pro" -> EntitlementState.PRO
                else -> error("Unknown entitlement")
            }
            val deletedAts = row.cases("shapeGroups").flatMap { group ->
                group.optionalString("flightEndDate")
                List(group.int("count")) { group.optionalString("deletedAt")?.let(Instant::parse)?.toEpochMilli() }
            }
            val count = when (action.limitKind) {
                QuotaLimitKind.SHAPES -> QuotaPolicy.shapeCount(deletedAts)
                QuotaLimitKind.SKETCHES -> row.int("sketchCount")
                QuotaLimitKind.DRONES -> row.int("droneCount")
            }
            val result = QuotaPolicy.evaluate(action, { count }, entitlement, QuotaLimits.fallback)
            val actual = when (result) {
                QuotaDecision.Allowed -> "allowed"
                is QuotaDecision.Blocked -> "blocked:${result.kind.name.lowercase()}"
            }
            assertEquals(row.string("name"), row.string("expected"), actual)
        }
    }

    @Test fun `all legacy and merge cases match shared fixture`() {
        val root = fixture()
        root.cases("legacyCases").forEach { row ->
            val date = row.optionalString("accountCreatedAt")?.let(Instant::parse)
            val version = row.optionalString("originalAppVersion")
            val kind = LegacyPolicy.legacyKind(version, row.bool("isProduction"), date, LegacyCutoff.fallback)
            assertEquals(row.string("name"), row.bool("expectedLegacy"), LegacyPolicy.isLegacyUser(version, row.bool("isProduction"), date, LegacyCutoff.fallback))
            val expected = row.optionalString("expectedKind")
            assertEquals(row.string("name"), expected, kind?.let { if (it == LegacyKind.EARLY_ACCESS) "earlyAccess" else "originalDownload" })
        }
        root.cases("limitsMergeCases").forEach { row ->
            assertEquals(row.string("name"), row.map("expected").limits(), QuotaLimits.fallback.raised(row.optionalMap("remote")?.limits()))
        }
        root.cases("legacyCutoffMergeCases").forEach { row ->
            assertEquals(row.string("name"), row.map("expected").cutoff(), LegacyCutoff.fallback.extended(row.optionalMap("remote")?.cutoff()))
        }
    }

    @Test fun `all remote config and version cases match shared fixture`() {
        val root = fixture()
        root.cases("remoteConfigCases").forEach { row ->
            val config = RemoteSubscriptionConfig.parse(row.string("json"), row.string("platform"))
            if (row["invalid"] == true) {
                assertNull(row.string("name"), config)
            } else {
                assertNotNull(row.string("name"), config)
                assertEquals(row.string("name"), row.optionalMap("expectedLimits")?.limits(), config!!.limits)
                assertEquals(row.string("name"), row.optionalMap("expectedLegacyCutoff")?.cutoff(), config.legacyCutoff)
                assertEquals(row.string("name"), row.optionalString("expectedMinSupportedVersion"), config.minSupportedVersion)
            }
        }
        root.cases("versionCases").forEach { row ->
            assertEquals(isVersionOlder(row.string("current"), row.string("minimum")), row.bool("expectedOlder"))
        }
    }

    @Test fun `all account tokens and count buckets match shared fixture`() {
        val root = fixture()
        root.cases("appAccountTokenCases").forEach { row ->
            assertEquals(row.string("uid"), row.string("expected"), appAccountToken(row.string("uid")))
        }
        root.cases("countBucketCases").forEach { row ->
            assertEquals(row.int("count").toString(), row.string("expected"), countBucket(row.int("count")))
        }
    }
}
