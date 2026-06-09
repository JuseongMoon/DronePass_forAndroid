package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.UUID

data class DroneValidationResult(
    val isValid: Boolean,
    val reason: String? = null,
)

fun DroneModel.validateForFirebasePersistence(): DroneValidationResult {
    if (!isValidFirebaseDroneId(id)) {
        return DroneValidationResult(isValid = false, reason = "invalid id")
    }
    if (name.isBlank()) {
        return DroneValidationResult(isValid = false, reason = "blank name")
    }
    if (!isValidFirebaseHexColor(color)) {
        return DroneValidationResult(isValid = false, reason = "invalid color")
    }
    return DroneValidationResult(isValid = true)
}

fun DroneModel.isValidForFirebasePersistence(): Boolean = validateForFirebasePersistence().isValid

fun validateFirebaseDroneBatch(drones: List<DroneModel>): DroneValidationResult {
    drones.forEach { drone ->
        val validation = drone.validateForFirebasePersistence()
        if (!validation.isValid) return validation
    }

    if (drones.map { it.id }.toSet().size != drones.size) {
        return DroneValidationResult(isValid = false, reason = "duplicate drone id")
    }

    return DroneValidationResult(isValid = true)
}

private fun isValidFirebaseDroneId(id: String): Boolean {
    return runCatching { UUID.fromString(id) }.isSuccess
}
