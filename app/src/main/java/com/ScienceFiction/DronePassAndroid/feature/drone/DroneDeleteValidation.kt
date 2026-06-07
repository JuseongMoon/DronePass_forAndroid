package com.ScienceFiction.DronePassAndroid.feature.drone

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel

enum class DroneDeleteValidationError {
    CANNOT_DELETE_LAST_DRONE,
    TARGET_DRONE_NOT_FOUND,
}

internal fun validateDroneDeleteRequest(
    activeDrones: List<DroneModel>,
    connectedShapeCount: Int,
    shapeHandling: ShapeHandling,
): DroneDeleteValidationError? {
    if (activeDrones.size <= 1) {
        return DroneDeleteValidationError.CANNOT_DELETE_LAST_DRONE
    }
    if (
        connectedShapeCount > 0 &&
        shapeHandling is ShapeHandling.Reassign &&
        activeDrones.none { it.id == shapeHandling.targetDroneId }
    ) {
        return DroneDeleteValidationError.TARGET_DRONE_NOT_FOUND
    }
    return null
}
