package com.ScienceFiction.DronePassAndroid.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shapes")
data class ShapeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val shapeType: String,
    val baseLatitude: Double,
    val baseLongitude: Double,
    val address: String?,
    val radius: Double?,
    val height: Double?,
    val memo: String?,
    val color: String,
    val droneId: String?,
    val createdAt: Long,
    val deletedAt: Long?,
    val flightStartDate: Long,
    val flightEndDate: Long?,
    val updatedAt: Long
)
