package com.ScienceFiction.DronePassAndroid.feature.profile

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.google.firebase.Timestamp
import java.util.Date

internal fun buildAnonymizedUserData(
    shapes: List<ShapeModel>,
    drones: List<DroneModel>,
    cloudSyncEnabled: Boolean,
    deletedAtMillis: Long,
    accountCreatedAtMillis: Long?,
): Map<String, Any> = buildMap {
    put("deletedAt", Timestamp(Date(deletedAtMillis)))
    put("totalShapesCreated", shapes.size)
    put("cloudSyncEnabled", cloudSyncEnabled)
    put("devicePlatform", "Android")
    put("totalDronesUsed", drones.count { !it.isDeleted })
    put("shapeTypeDistribution", calculateShapeTypeDistribution(shapes))
    if (accountCreatedAtMillis != null) {
        put("accountCreatedAt", Timestamp(Date(accountCreatedAtMillis)))
    }
}

internal fun calculateShapeTypeDistribution(shapes: List<ShapeModel>): Map<String, Int> {
    return shapes.groupingBy { it.shapeType.rawValue }.eachCount()
}

internal fun shapeToAnonymizedData(shape: ShapeModel): Map<String, Any> = buildMap {
    put("id", shape.id)
    put("title", shape.title)
    put("shapeType", shape.shapeType.rawValue)
    put("baseCoordinate", coordinateToAnonymizedData(shape.baseCoordinate))
    put("memo", shape.memo ?: "")
    put("address", shape.address ?: "")
    put("createdAt", Timestamp(Date(shape.createdAt)))
    put("flightStartDate", Timestamp(Date(shape.flightStartDate)))
    put("color", shape.color)
    put("updatedAt", Timestamp(Date(shape.updatedAt)))

    shape.flightEndDate?.let { put("flightEndDate", Timestamp(Date(it))) }
    shape.deletedAt?.let { put("deletedAt", Timestamp(Date(it))) }
    shape.droneId?.let { put("droneId", it) }
    shape.radius?.let { put("radius", it) }
    shape.secondCoordinate?.let { put("secondCoordinate", coordinateToAnonymizedData(it)) }
    shape.polygonCoordinates?.let { coordinates ->
        put("polygonCoordinates", coordinates.map(::coordinateToAnonymizedData))
    }
    shape.polylineCoordinates?.let { coordinates ->
        put("polylineCoordinates", coordinates.map(::coordinateToAnonymizedData))
    }
}

internal fun droneToAnonymizedData(drone: DroneModel): Map<String, Any> = buildMap {
    put("id", drone.id)
    put("name", drone.name)
    put("color", drone.color)
    put("createdAt", Timestamp(Date(drone.createdAt)))
    put("updatedAt", Timestamp(Date(drone.updatedAt)))
    put("isDeleted", drone.isDeleted)
    drone.deletedAt?.let { put("deletedAt", Timestamp(Date(it))) }
}

private fun coordinateToAnonymizedData(coordinate: Coordinate): Map<String, Double> {
    return mapOf(
        "latitude" to coordinate.latitude,
        "longitude" to coordinate.longitude,
    )
}
