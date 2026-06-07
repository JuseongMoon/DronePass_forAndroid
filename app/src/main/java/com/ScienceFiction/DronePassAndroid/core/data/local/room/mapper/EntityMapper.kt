package com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper

import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.DroneEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.SketchEntity
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel

// ===== Shape 매퍼 =====

fun ShapeEntity.toDomain(): ShapeModel = ShapeModel(
    id = id,
    title = title,
    // 손상된 enum 문자열(앱 다운그레이드, 외부 마이그레이션 등)이 들어와도 Flow 전체가
    // 깨지지 않도록 CIRCLE 로 폴백. iOS rawValue("circle")와 Android 레거시("CIRCLE") 모두 허용.
    shapeType = ShapeType.fromWireValue(shapeType),
    baseCoordinate = Coordinate(baseLatitude, baseLongitude),
    address = address,
    radius = radius,
    secondCoordinate = if (secondLatitude != null && secondLongitude != null) {
        Coordinate(secondLatitude, secondLongitude)
    } else {
        null
    },
    polygonCoordinates = polygonCoordinates?.let(Coordinate::listFromJson),
    polylineCoordinates = polylineCoordinates?.let(Coordinate::listFromJson),
    height = height,
    memo = memo,
    color = color,
    droneId = droneId,
    createdAt = createdAt,
    deletedAt = deletedAt,
    flightStartDate = flightStartDate,
    flightEndDate = flightEndDate,
    updatedAt = updatedAt
)

fun ShapeModel.toEntity(): ShapeEntity = ShapeEntity(
    id = id,
    title = title,
    shapeType = shapeType.rawValue,
    baseLatitude = baseCoordinate.latitude,
    baseLongitude = baseCoordinate.longitude,
    address = address,
    radius = radius,
    secondLatitude = secondCoordinate?.latitude,
    secondLongitude = secondCoordinate?.longitude,
    polygonCoordinates = polygonCoordinates?.let(Coordinate::listToJson),
    polylineCoordinates = polylineCoordinates?.let(Coordinate::listToJson),
    height = height,
    memo = memo,
    color = color,
    droneId = droneId,
    createdAt = createdAt,
    deletedAt = deletedAt,
    flightStartDate = flightStartDate,
    flightEndDate = flightEndDate,
    updatedAt = updatedAt
)

// ===== Drone 매퍼 =====

fun DroneEntity.toDomain(): DroneModel = DroneModel(
    id = id,
    name = name,
    color = color,
    serialNumber = serialNumber,
    takeoffWeight = takeoffWeight,
    size = size,
    memo = memo,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun DroneModel.toEntity(): DroneEntity = DroneEntity(
    id = id,
    name = name,
    color = color,
    serialNumber = serialNumber,
    takeoffWeight = takeoffWeight,
    size = size,
    memo = memo,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

// ===== Sketch 매퍼 =====

fun SketchEntity.toDomain(): SketchModel = SketchModel(
    id = id,
    points = Coordinate.listFromJson(points),
    color = color,
    strokeWidth = strokeWidth,
    opacity = opacity,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun SketchModel.toEntity(): SketchEntity = SketchEntity(
    id = id,
    points = Coordinate.listToJson(points),
    color = color,
    strokeWidth = strokeWidth,
    opacity = opacity,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
