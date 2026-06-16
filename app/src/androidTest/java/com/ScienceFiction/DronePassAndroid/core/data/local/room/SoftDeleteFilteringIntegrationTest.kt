package com.ScienceFiction.DronePassAndroid.core.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.DroneEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.SketchEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SoftDeleteFilteringIntegrationTest {

    private lateinit var database: DronePassDatabase

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, DronePassDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun shapeActiveQueriesExcludeSoftDeletedRowsButAllQueriesKeepTombstones() = runBlocking {
        database.shapeDao().insertShapes(
            listOf(
                shapeEntity(id = "active-newer", updatedAt = 300L, deletedAt = null),
                shapeEntity(id = "deleted", updatedAt = 400L, deletedAt = 500L),
                shapeEntity(id = "active-older", updatedAt = 100L, deletedAt = null),
            ),
        )

        assertEquals(
            listOf("active-newer", "active-older"),
            database.shapeDao().getActiveShapes().first().map { it.id },
        )
        assertEquals(
            listOf("deleted", "active-newer", "active-older"),
            database.shapeDao().getAllShapes().first().map { it.id },
        )
    }

    @Test
    fun sketchActiveQueriesExcludeSoftDeletedRowsButAllQueriesKeepTombstones() = runBlocking {
        database.sketchDao().insertSketches(
            listOf(
                sketchEntity(id = "active-newer", updatedAt = 300L, deletedAt = null),
                sketchEntity(id = "deleted", updatedAt = 400L, deletedAt = 500L),
                sketchEntity(id = "active-older", updatedAt = 100L, deletedAt = null),
            ),
        )

        assertEquals(
            listOf("active-newer", "active-older"),
            database.sketchDao().getActiveSketches().first().map { it.id },
        )
        assertEquals(
            listOf("deleted", "active-newer", "active-older"),
            database.sketchDao().getAllSketches().first().map { it.id },
        )
    }

    @Test
    fun droneActiveQueriesExcludeSoftDeletedRowsButAllQueriesKeepTombstones() = runBlocking {
        database.droneDao().insertDrones(
            listOf(
                droneEntity(id = "active-newer", updatedAt = 300L, deletedAt = null),
                droneEntity(id = "deleted", updatedAt = 400L, deletedAt = 500L),
                droneEntity(id = "active-older", updatedAt = 100L, deletedAt = null),
            ),
        )

        assertEquals(
            listOf("active-newer", "active-older"),
            database.droneDao().getActiveDrones().first().map { it.id },
        )
        assertEquals(
            listOf("deleted", "active-newer", "active-older"),
            database.droneDao().getAllDrones().first().map { it.id },
        )
        assertEquals(2, database.droneDao().getActiveDroneCount())
    }

    private fun shapeEntity(
        id: String,
        updatedAt: Long,
        deletedAt: Long?,
    ): ShapeEntity = ShapeEntity(
        id = id,
        title = id,
        shapeType = "circle",
        baseLatitude = 37.0,
        baseLongitude = 127.0,
        address = null,
        radius = 100.0,
        secondLatitude = null,
        secondLongitude = null,
        polygonCoordinates = null,
        polylineCoordinates = null,
        height = null,
        memo = null,
        color = "#007AFF",
        droneId = null,
        createdAt = 1L,
        deletedAt = deletedAt,
        flightStartDate = 1L,
        flightEndDate = null,
        updatedAt = updatedAt,
    )

    private fun sketchEntity(
        id: String,
        updatedAt: Long,
        deletedAt: Long?,
    ): SketchEntity = SketchEntity(
        id = id,
        points = """[{"latitude":37.0,"longitude":127.0},{"latitude":37.1,"longitude":127.1}]""",
        color = "#FF0000",
        strokeWidth = 4.0,
        opacity = 1.0,
        createdAt = 1L,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    private fun droneEntity(
        id: String,
        updatedAt: Long,
        deletedAt: Long?,
    ): DroneEntity = DroneEntity(
        id = id,
        name = id,
        color = "#007AFF",
        serialNumber = null,
        takeoffWeight = null,
        size = null,
        memo = null,
        createdAt = 1L,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
