package com.ScienceFiction.DronePassAndroid.core.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sketches")
data class SketchEntity(
    @PrimaryKey val id: String,
    val points: String,
    val color: String,
    val strokeWidth: Double,
    val opacity: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
