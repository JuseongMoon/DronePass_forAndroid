package com.ScienceFiction.DronePassAndroid.core.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.DroneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DroneDao {

    @Query("SELECT * FROM drones WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getActiveDrones(): Flow<List<DroneEntity>>

    @Query("SELECT * FROM drones ORDER BY updatedAt DESC")
    fun getAllDrones(): Flow<List<DroneEntity>>

    @Query("SELECT * FROM drones")
    suspend fun getAllDronesOnce(): List<DroneEntity>

    @Query("SELECT * FROM drones WHERE id = :id")
    suspend fun getDroneById(id: String): DroneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrone(drone: DroneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrones(drones: List<DroneEntity>)

    @Update
    suspend fun updateDrone(drone: DroneEntity)

    @Delete
    suspend fun deleteDrone(drone: DroneEntity)

    @Query("DELETE FROM drones")
    suspend fun deleteAllDrones()
}
