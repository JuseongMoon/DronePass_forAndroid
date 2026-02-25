package com.ScienceFiction.DronePassAndroid.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drones")
data class DroneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val serialNumber: String?,
    val takeoffWeight: String?,
    val size: String?,
    val memo: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
