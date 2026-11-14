package com.ScienceFiction.DronePassAndroid.domain.model

import com.ScienceFiction.DronePassAndroid.core.account.computeImportCandidates
import com.ScienceFiction.DronePassAndroid.core.data.repository.reconcileTagClearWithServer
import com.ScienceFiction.DronePassAndroid.core.data.repository.shapesTagClearedLocallyOnly
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** iOS 기준 구현과 같은 공유 fixture(team/fixtures/tag-cases.json)로 태그 병합·필터·가져오기·이름·삭제를 확인한다. */
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
        // 가져오기 후보 계산(computeImportCandidates)이 계정에 없을 태그를 가리키는 도형의 tagId 를 비우는지.
        // accountTagIds 는 서버의 활성 태그로 넣는다(기기 태그 없음)
        cases.forEach { row ->
            val name = row.string("name")
            val shape = ShapeModel(id = "S1", tagId = row["tagId"] as String?, createdAt = before, updatedAt = before)
            val serverTags = row.stringList("accountTagIds").map { TagModel(id = it, name = it, createdAt = before, updatedAt = before) }
            val candidates = computeImportCandidates(
                localShapes = listOf(shape),
                localSketches = emptyList(),
                localDrones = emptyList(),
                localTags = emptyList(),
                serverShapes = emptyList(),
                serverSketches = emptyList(),
                serverDrones = emptyList(),
                serverTags = serverTags,
                nowMillis = now,
            )
            val rewritten = candidates.shapes.single()
            assertEquals(name, row["expectedTagId"] as String?, rewritten.tagId)
            assertEquals(name, row["expectUpdatedAtBump"] as Boolean, "S1" in candidates.shapeTagIdRewrites)
            if (row["expectUpdatedAtBump"] as Boolean) {
                assertEquals(name, now, rewritten.updatedAt)
            } else {
                assertEquals(name, before, rewritten.updatedAt)
                assertNotEquals(name, now, rewritten.updatedAt)
            }
        }
    }

    // ===== 이름 (D-71 ⑦, iOS TagName) =====

    @Test fun `all tag name cases match the shared fixture`() {
        val cases = fixture().cases("nameCases")
        assertEquals(19, cases.size)
        cases.forEach { row ->
            val name = row.string("name")
            val input = row.string("input")
            val normalized = TagName.normalized(input)
            assertEquals(name, row.string("expectedNormalized"), normalized)
            assertEquals(name, (row.required("expectedLength") as Number).toInt(), TagName.length(normalized))
            assertEquals(name, row.required("expectedValid") as Boolean, TagName.isValid(input))
        }
    }

    @Test fun `all tag name display cases match the shared fixture`() {
        val cases = fixture().cases("nameDisplayCases")
        assertEquals(3, cases.size)
        cases.forEach { row ->
            assertEquals(row.string("name"), row.string("expectedDisplay"), TagName.display(row.string("input")))
        }
    }

    @Test fun `all tag name input cases match the shared fixture`() {
        val cases = fixture().cases("nameInputCases")
        assertEquals(6, cases.size)
        cases.forEach { row ->
            val name = row.string("name")
            val limited = TagName.limitedInput(
                old = row.string("old"),
                new = row.string("new"),
                isLoading = row.required("isLoading") as Boolean,
            )
            assertEquals(name, row.string("expectedText"), limited.text)
            assertEquals(name, row.required("expectedTruncated") as Boolean, limited.truncated)
        }
    }

    // ===== 태그 삭제 때 도형 정리 (D-71 ①, iOS TagDeleteRepair) =====

    private fun Map<*, *>.shape(): ShapeModel = ShapeModel(
        id = string("id"),
        tagId = this["tagId"] as String?,
        memo = this["memo"] as String?,
        createdAt = 0L,
        updatedAt = requireNotNull(millis("updatedAt")),
        deletedAt = millis("deletedAt"),
    )

    private data class ShapeState(val id: String, val tagId: String?, val memo: String?, val updatedAt: Long, val deletedAt: Long?)

    private fun ShapeModel.state() = ShapeState(id, tagId, memo, updatedAt, deletedAt)

    @Test fun `all tag delete cases match the shared fixture`() {
        val cases = fixture().cases("deleteCases")
        assertEquals(7, cases.size)
        cases.forEach { row ->
            val name = row.string("name")
            val tagId = row.string("deletedTagId")
            val now = requireNotNull(row.millis("now"))
            val local = row.cases("local").map { it.shape() }
            val server = (row["server"] as List<*>?)?.map { (it as Map<*, *>).shape() }
            // 기기에 쓸 도형을 덮어 본 결과에서 활성 도형만 본다(서버에서 지워진 도형은 기기에서도 빠진다)
            val (writes, uploadIds) = if (server == null) {
                shapesTagClearedLocallyOnly(local, tagId) to emptySet()
            } else {
                val result = reconcileTagClearWithServer(local, server, tagId, now)
                result.localWrites to result.toUpload.map { it.id }.toSet()
            }
            val writtenById = writes.associateBy { it.id }
            val device = (local.map { writtenById[it.id] ?: it } + writes.filter { w -> local.none { it.id == w.id } })
                .filter { it.deletedAt == null }
            assertEquals(
                name,
                row.cases("expectedShapes").map { it.shape().state() }.sortedBy { it.id },
                device.map { it.state() }.sortedBy { it.id },
            )
            assertEquals(name, row.stringList("expectedToUploadIds").toSet(), uploadIds)
        }
    }
}
