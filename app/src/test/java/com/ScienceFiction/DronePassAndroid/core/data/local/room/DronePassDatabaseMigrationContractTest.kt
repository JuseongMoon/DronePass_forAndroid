package com.ScienceFiction.DronePassAndroid.core.data.local.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DronePassDatabaseMigrationContractTest {

    @Test
    fun `direct v1 to v3 migration preserves iOS geometry columns`() {
        val v1 = readSchema(version = 1)
        val v2 = readSchema(version = 2)
        val v3 = readSchema(version = 3)

        ExpectedTables.forEach { table ->
            assertTrue(v1.hasTable(table))
            assertTrue(v3.hasTable(table))
        }
        GeometryColumns.forEach { column ->
            assertTrue(v1.hasColumn(column))
            assertFalse(v2.hasColumn(column))
            assertTrue(v3.hasColumn(column))
        }
        assertEquals(identityHash(v1), identityHash(v3))
    }

    @Test
    fun `database registers both sequential and direct migration paths`() {
        val migrationPaths = DronePassDatabase.allMigrations
            .map { it.startVersion to it.endVersion }
            .toSet()

        assertEquals(
            setOf(1 to 2, 2 to 3, 1 to 3),
            migrationPaths,
        )
    }

    private fun String.hasColumn(columnName: String): Boolean =
        contains("\"columnName\": \"$columnName\"")

    private fun String.hasTable(tableName: String): Boolean =
        contains("\"tableName\": \"$tableName\"")

    private fun identityHash(schemaJson: String): String {
        val match = Regex("\"identityHash\"\\s*:\\s*\"([^\"]+)\"").find(schemaJson)
        return requireNotNull(match) { "identityHash not found" }.groupValues[1]
    }

    private fun readSchema(version: Int): String =
        File(schemaDirectory(), "$version.json").readText()

    private fun schemaDirectory(): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return listOf(
            File(userDir, SchemaPath),
            File(userDir, "app/$SchemaPath"),
        ).first { it.exists() }
    }

    private companion object {
        const val SchemaPath =
            "schemas/com.ScienceFiction.DronePassAndroid.core.data.local.room.DronePassDatabase"

        val ExpectedTables = listOf("shapes", "drones", "sketches")

        val GeometryColumns = listOf(
            "secondLatitude",
            "secondLongitude",
            "polygonCoordinates",
            "polylineCoordinates",
        )
    }
}
