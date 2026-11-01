package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.core.data.repository.sortActiveDronesForIosList
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.ScienceFiction.DronePassAndroid.domain.model.activeTags
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** iOS 기준 구현과 같은 공유 fixture(team/fixtures/name-sort-cases.json)로 드론·태그 공용 이름 정렬을 확인한다. */
class NameSortOrderTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private data class Item(val id: String, val name: String)

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "name-sort-cases.json")
        val missing = "Shared name sort fixture is missing: $file"
        if (System.getProperty("dronepass.requireSharedFixtures") == "true") {
            check(file.isFile) { missing }
        } else {
            assumeTrue(missing, file.isFile)
        }
        return requireNotNull(adapter.fromJson(file.readText()) as? Map<*, *>)
    }

    private fun Map<*, *>.string(key: String): String = this[key] as? String ?: error("Invalid $key")
    private fun Map<*, *>.list(key: String): List<*> = this[key] as? List<*> ?: error("Invalid $key")
    private fun Map<*, *>.items(): List<Item> = list("items").map { (it as Map<*, *>).let { item -> Item(item.string("id"), item.string("name")) } }
    private fun Map<*, *>.expected(): List<String> = list("expected").map { it as String }

    private fun sortCases(): List<Map<*, *>> = fixture().list("sortCases").map { it as Map<*, *> }

    @Test fun `all name sort cases match the shared fixture`() {
        val cases = sortCases()
        assertEquals(16, cases.size)
        cases.forEach { row ->
            val sorted = row.items().sortedWith(nameThenIdComparator(Item::name, Item::id)).map { it.id }
            assertEquals(row.string("name"), row.expected(), sorted)
        }
    }

    @Test fun `active tags follow the shared name order`() {
        sortCases().forEach { row ->
            val tags = row.items().map { TagModel(id = it.id, name = it.name, createdAt = 0, updatedAt = 0) }
            assertEquals(row.string("name"), row.expected(), tags.activeTags().map { it.id })
        }
    }

    @Test fun `active drones follow the shared name order`() {
        sortCases().forEach { row ->
            val drones = row.items().map { DroneModel(id = it.id, name = it.name) }
            assertEquals(row.string("name"), row.expected(), sortActiveDronesForIosList(drones).map { it.id })
        }
    }
}
