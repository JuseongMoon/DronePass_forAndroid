package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** iOS 기준 구현과 같은 공유 fixture(team/fixtures/drone-merge-cases.json)로 드론 로그인 병합을 확인한다. */
class DroneMergeContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "drone-merge-cases.json")
        val missing = "Shared drone merge fixture is missing: $file"
        if (System.getProperty("dronepass.requireSharedFixtures") == "true") {
            check(file.isFile) { missing }
        } else {
            assumeTrue(missing, file.isFile)
        }
        return requireNotNull(adapter.fromJson(file.readText()) as? Map<*, *>)
    }

    private fun Map<*, *>.required(key: String): Any = requireNotNull(this[key]) { "Missing $key" }
    private fun Map<*, *>.string(key: String): String = required(key) as? String ?: error("Invalid $key")
    private fun Map<*, *>.list(key: String): List<*> = required(key) as? List<*> ?: error("Invalid $key")
    private fun Map<*, *>.map(key: String): Map<*, *> = required(key) as? Map<*, *> ?: error("Invalid $key")
    private fun Map<*, *>.stringList(key: String): List<String> = list(key).map { it as String }
    private fun Map<*, *>.millis(key: String): Long? = (this[key] as? String)?.let { Instant.parse(it).toEpochMilli() }

    private fun Map<*, *>.drone(): DroneModel = DroneModel(
        id = string("id"),
        name = string("name"),
        color = string("color"),
        serialNumber = this["serialNumber"] as? String,
        takeoffWeight = this["takeoffWeight"] as? String,
        size = this["size"] as? String,
        memo = this["memo"] as? String,
        createdAt = requireNotNull(millis("createdAt")),
        updatedAt = requireNotNull(millis("updatedAt")),
        deletedAt = millis("deletedAt"),
    )

    @Test fun `untouched default drone definition matches the Android constants`() {
        val root = fixture()
        assertEquals(1.0, (root.required("schemaVersion") as Number).toDouble(), 0.0)
        val definition = root.map("untouchedDefaultDrone")
        assertEquals(DefaultDroneNames, definition.stringList("names").toSet())
        assertEquals(DefaultDroneColor, definition.string("color"))
        assertEquals("caseInsensitive", definition.string("colorCompare"))
        assertEquals(
            listOf("serialNumber", "takeoffWeight", "size", "memo"),
            definition.stringList("blankFields"),
        )
        assertEquals(UntouchedDroneWindowMillis, ((definition.required("maxEditSeconds") as Number).toDouble() * 1000).toLong())
    }

    @Test fun `all drone merge cases match the shared fixture`() {
        val cases = fixture().list("cases").map { it as Map<*, *> }
        assertEquals(29, cases.size)
        cases.forEach { row ->
            val name = row.string("name")
            val result = mergeDronesForFullSync(
                localDrones = row.list("local").map { (it as Map<*, *>).drone() },
                serverDrones = row.list("server").map { (it as Map<*, *>).drone() },
                localShapeDroneIds = row.stringList("referencedDroneIds"),
            )
            assertEquals(name, row.stringList("expectedMergedIds").toSet(), result.merged.map { it.id }.toSet())
            assertEquals(name, row.stringList("expectedToUploadIds").toSet(), result.toUpload.map { it.id }.toSet())
            assertEquals(
                name,
                row.map("expectedReassignedShapeDroneIds").entries.associate { (k, v) -> k as String to v as String },
                result.reassignedShapeDroneIds,
            )
            (row["expectedDeletedIds"] as? List<*>)?.let { expected ->
                assertEquals(name, expected.map { it as String }.toSet(), result.merged.filter { it.deletedAt != null }.map { it.id }.toSet())
            }
            (row["expectedNames"] as? Map<*, *>)?.forEach { (id, expectedName) ->
                assertEquals(name, expectedName, result.merged.single { it.id == id }.name)
            }
        }
    }
}
