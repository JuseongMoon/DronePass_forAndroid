package com.ScienceFiction.DronePassAndroid.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.SketchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SketchDao {

    @Query("SELECT * FROM sketches WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getActiveSketches(): Flow<List<SketchEntity>>

    @Query("SELECT * FROM sketches ORDER BY updatedAt DESC")
    fun getAllSketches(): Flow<List<SketchEntity>>

    @Query("SELECT * FROM sketches")
    suspend fun getAllSketchesOnce(): List<SketchEntity>

    @Query("SELECT * FROM sketches WHERE id = :id")
    suspend fun getSketchById(id: String): SketchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSketch(sketch: SketchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSketches(sketches: List<SketchEntity>)

    @Update
    suspend fun updateSketch(sketch: SketchEntity)

    @Delete
    suspend fun deleteSketch(sketch: SketchEntity)

    @Query("DELETE FROM sketches WHERE id IN (:ids)")
    suspend fun deleteSketchesByIds(ids: List<String>)

    @Query("DELETE FROM sketches")
    suspend fun deleteAllSketches()
}
