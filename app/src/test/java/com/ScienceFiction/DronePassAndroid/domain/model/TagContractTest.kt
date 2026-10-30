package com.ScienceFiction.DronePassAndroid.domain.model

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** iOS 기준 구현과 같은 공유 fixture(team/fixtures/tag-cases.json)로 태그 병합·필터·가져오기를 확인한다. */
class TagContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "tag-cases.json")
        val missing = "Shared tag fixture is missing: $file"
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
    private fun Map<*, *>.stringList(key: String): List<String> = list(key).map { it as String }
    private fun Map<*, *>.cases(key: String): List<Map<*, *>> = list(key).map { it as Map<*, *> }
    private fun Map<*, *>.millis(key: String): Long? = (this[key] as? String)?.let { Instant.parse(it).toEpochMilli() }

    private fun Map<*, *>.tag(): TagModel = TagModel(
        id = string("id"),
        name = string("name"),
        createdAt = requireNotNull(millis("createdAt")),
        updatedAt = requireNotNull(millis("updatedAt")),
        deletedAt = millis("deletedAt"),
    )

    @Test fun `untagged key matches the shared fixture`() {
        assertEquals(TagFilter.UNTAGGED_KEY, fixture().string("untaggedKey"))
    }

    @Test fun `all tag merge cases match the shared fixture`() {
        val cases = fixture().cases("mergeCases")
        assertEquals(9, cases.size)
        cases.forEach { row ->
            val name = row.string("name")
            val result = mergeTags(
                local = row.cases("local").map { it.tag() },
                server = row.cases("server").map { it.tag() },
            )
            assertEquals(name, row.stringList("expectedMergedIds").toSet(), result.merged.map { it.id }.toSet())
            assertEquals(name, row.stringList("expectedToUploadIds").toSet(), result.toUpload.map { it.id }.toSet())
            assertEquals(
                name,
                row.stringList("expectedDeletedIds").toSet(),
                result.merged.filter { it.isDeleted }.map { it.id }.toSet(),
            )
            (row["expectedNames"] as? Map<*, *>)?.forEach { (id, expectedName) ->
                assertEquals(name, expectedName, result.merged.single { it.id == id }.name)
            }
        }
    }

    @Test fun `all tag filter cases match the shared fixture`() {
        val cases = fixture().cases("filterCases")
        assertEquals(8, cases.size)
        cases.forEach { row ->
            assertEquals(
                row.string("name"),
                row["expectedVisible"] as Boolean,
                TagFilter.isVisible(
                    tagId = row["tagId"] as String?,
                    activeTagIds = row.stringList("activeTagIds").toSet(),
                    hidden = row.stringList("hidden").toSet(),
                ),
            )
        }
    }

    @Test fun `all import rewrite cases match the shared fixture`() {
        val cases = fixture().cases("importRewriteCases")
        assertEquals(3, cases.size)
        val before = 1_000L
        val now = 2_000L
        cases.forEach { row ->
            val name = row.string("name")
            val shape = ShapeModel(tagId = row["tagId"] as String?, updatedAt = before)
            val rewritten = clearingTagIds(listOf(shape), row.stringList("accountTagIds").toSet(), now).single()
            assertEquals(name, row["expectedTagId"] as String?, rewritten.tagId)
            if (row["expectUpdatedAtBump"] as Boolean) {
                assertEquals(name, now, rewritten.updatedAt)
            } else {
                assertEquals(name, before, rewritten.updatedAt)
                assertNotEquals(name, now, rewritten.updatedAt)
            }
        }
    }
}
