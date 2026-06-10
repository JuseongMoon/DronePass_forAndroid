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
    deletingDroneId: String? = null,
): DroneDeleteValidationError? {
    if (activeDrones.size <= 1) {
        return DroneDeleteValidationError.CANNOT_DELETE_LAST_DRONE
    }
    val hasInvalidReassignTarget =
        connectedShapeCount > 0 &&
            shapeHandling is ShapeHandling.Reassign &&
            (
                shapeHandling.targetDroneId == deletingDroneId ||
                    activeDrones.none { it.id == shapeHandling.targetDroneId }
            )
    if (hasInvalidReassignTarget) {
        return DroneDeleteValidationError.TARGET_DRONE_NOT_FOUND
    }
    return null
}
