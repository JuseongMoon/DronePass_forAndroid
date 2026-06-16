package com.ScienceFiction.DronePassAndroid.core.data.local.room

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.DroneEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.SketchEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.DroneFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun shapeCrudKeepsIosSoftDeleteRestoreAndHardDeleteContracts() = runBlocking {
        val dao = database.shapeDao()
        val created = shapeEntity(id = "shape-crud", updatedAt = 100L, deletedAt = null)

        dao.insertShape(created)
        assertEquals(created, dao.getShapeById("shape-crud"))
        assertEquals(listOf("shape-crud"), dao.getActiveShapes().first().map { it.id })

        val updated = created.copy(
            title = "Updated",
            memo = "memo",
            updatedAt = 200L,
        )
        dao.updateShape(updated)
        assertEquals("Updated", dao.getShapeById("shape-crud")?.title)
        assertEquals("memo", dao.getShapeById("shape-crud")?.memo)

        val deleted = updated.copy(deletedAt = 300L, updatedAt = 300L)
        dao.updateShape(deleted)
        assertEquals(emptyList<String>(), dao.getActiveShapes().first().map { it.id })
        assertEquals(listOf("shape-crud"), dao.getAllShapes().first().map { it.id })
        assertEquals(300L, dao.getShapeById("shape-crud")?.deletedAt)

        val restored = deleted.copy(deletedAt = null, updatedAt = 400L)
        dao.updateShape(restored)
        assertEquals(listOf("shape-crud"), dao.getActiveShapes().first().map { it.id })
        assertNull(dao.getShapeById("shape-crud")?.deletedAt)

        dao.deleteShape(restored)
        assertNull(dao.getShapeById("shape-crud"))
        assertEquals(emptyList<String>(), dao.getAllShapes().first().map { it.id })
    }

    @Test
    fun shapeModelPersistsAcrossRoomCloseAndReopen() = runBlocking {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "shape_restart_contract_test.db"
        context.deleteDatabase(databaseName)

        val shape = ShapeModel(
            id = "00000000-0000-0000-0000-000000000123",
            title = "Restart Area",
            shapeType = ShapeType.RECTANGLE,
            baseCoordinate = Coordinate(37.5665, 126.9780),
            address = "Seoul",
            radius = null,
            secondCoordinate = Coordinate(37.5675, 126.9790),
            height = 120.5,
            memo = "memo",
            color = "#FF9500",
            droneId = "drone-a",
            createdAt = 1_700_000_000_000L,
            deletedAt = null,
            flightStartDate = 1_700_000_100_000L,
            flightEndDate = 1_700_000_200_000L,
            updatedAt = 1_700_000_300_000L,
        )

        var fileDatabase = Room.databaseBuilder(context, DronePassDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            fileDatabase.shapeDao().insertShape(shape.toEntity())
        } finally {
            fileDatabase.close()
        }

        fileDatabase = Room.databaseBuilder(context, DronePassDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            val restored = fileDatabase.shapeDao().getShapeById(shape.id)?.toDomain()

            assertEquals(shape, restored)
            assertEquals(listOf(shape.id), fileDatabase.shapeDao().getActiveShapes().first().map { it.id })
        } finally {
            fileDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun shapeModelPersistsPolygonPolylineCoordinatesAcrossRoomCloseAndReopen() = runBlocking {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "shape_geometry_restart_contract_test.db"
        context.deleteDatabase(databaseName)

        val polygon = listOf(
            Coordinate(37.5665, 126.9780),
            Coordinate(37.5675, 126.9790),
            Coordinate(37.5685, 126.9800),
        )
        val polyline = listOf(
            Coordinate(36.1, 126.1),
            Coordinate(36.2, 126.2),
        )
        val shape = ShapeModel(
            id = "00000000-0000-0000-0000-000000000124",
            title = "Geometry Area",
            shapeType = ShapeType.POLYGON,
            baseCoordinate = Coordinate(37.5665, 126.9780),
            address = "Seoul",
            radius = 250.0,
            secondCoordinate = Coordinate(37.5675, 126.9790),
            polygonCoordinates = polygon,
            polylineCoordinates = polyline,
            height = 80.0,
            memo = "memo",
            color = "#34C759",
            droneId = "drone-a",
            createdAt = 1_700_000_000_000L,
            deletedAt = null,
            flightStartDate = 1_700_000_100_000L,
            flightEndDate = 1_700_000_200_000L,
            updatedAt = 1_700_000_300_000L,
        )

        var fileDatabase = Room.databaseBuilder(context, DronePassDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            fileDatabase.shapeDao().insertShape(shape.toEntity())
        } finally {
            fileDatabase.close()
        }

        fileDatabase = Room.databaseBuilder(context, DronePassDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            val restored = fileDatabase.shapeDao().getShapeById(shape.id)?.toDomain()

            assertEquals(shape, restored)
            assertEquals(listOf(shape.id), fileDatabase.shapeDao().getActiveShapes().first().map { it.id })
        } finally {
            fileDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun malformedShapeGeometryJsonRestoresEmptyListsWithoutCrash() {
        val restored = shapeEntity(id = "shape-malformed-geometry", updatedAt = 100L, deletedAt = null)
            .copy(
                shapeType = "polygon",
                polygonCoordinates = "[",
                polylineCoordinates = """[{"latitude":37.0}]""",
            )
            .toDomain()

        assertEquals(emptyList<Coordinate>(), restored.polygonCoordinates)
        assertEquals(emptyList<Coordinate>(), restored.polylineCoordinates)
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

    @Test
    fun droneRepositoryCreatesOneDefaultDroneWhenActiveListIsEmpty() = runBlocking {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStoreFile = File(context.filesDir, "drone_repository_default_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        val repository = DroneRepository(
            droneDao = database.droneDao(),
            droneFirebaseStore = DroneFirebaseStore(FirebaseFirestore.getInstance()),
            auth = FirebaseAuth.getInstance(),
            dataStore = dataStore,
            context = context,
        )

        try {
            val created = repository.ensureDefaultDroneIfNeeded()
            val secondCreation = repository.ensureDefaultDroneIfNeeded()
            val activeDrones = database.droneDao().getActiveDrones().first().map { it.toDomain() }
            val modificationTime = dataStore.data.first()[SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME]

            assertEquals(context.getString(R.string.drone_edit_default_name_first), created?.name)
            assertEquals("#007AFF", created?.color)
            assertNull(secondCreation)
            assertEquals(1, activeDrones.size)
            assertEquals(created?.id, activeDrones.single().id)
            assertNull(activeDrones.single().deletedAt)
            assertTrue(modificationTime != null && modificationTime > 0L)
        } finally {
            dataStoreScope.cancel()
            dataStoreFile.delete()
        }
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
