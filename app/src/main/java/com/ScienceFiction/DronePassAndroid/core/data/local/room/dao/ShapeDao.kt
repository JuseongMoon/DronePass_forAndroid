package com.ScienceFiction.DronePassAndroid.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShapeDao {

    @Query("SELECT * FROM shapes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getActiveShapes(): Flow<List<ShapeEntity>>

    @Query("SELECT * FROM shapes ORDER BY updatedAt DESC")
    fun getAllShapes(): Flow<List<ShapeEntity>>

    @Query("SELECT * FROM shapes")
    suspend fun getAllShapesOnce(): List<ShapeEntity>

    @Query("SELECT * FROM shapes WHERE id = :id")
    suspend fun getShapeById(id: String): ShapeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShape(shape: ShapeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShapes(shapes: List<ShapeEntity>)

    @Update
    suspend fun updateShape(shape: ShapeEntity)

    @Delete
    suspend fun deleteShape(shape: ShapeEntity)

    @Query("DELETE FROM shapes WHERE id IN (:ids)")
    suspend fun deleteShapesByIds(ids: List<String>)

    @Query("DELETE FROM shapes")
    suspend fun deleteAllShapes()

    @Query("SELECT COUNT(*) FROM shapes WHERE droneId = :droneId AND deletedAt IS NULL")
    suspend fun getActiveShapeCountByDroneId(droneId: String): Int

    @Query("SELECT * FROM shapes WHERE droneId = :droneId AND deletedAt IS NULL")
    suspend fun getActiveShapesByDroneId(droneId: String): List<ShapeEntity>

    @Query("SELECT * FROM shapes WHERE deletedAt IS NULL AND flightEndDate IS NOT NULL AND flightEndDate < :now")
    suspend fun getActiveExpiredShapes(now: Long): List<ShapeEntity>

    @Query("SELECT * FROM shapes WHERE droneId IS NULL AND deletedAt IS NULL")
    suspend fun getActiveLegacyShapesWithoutDrone(): List<ShapeEntity>
}
