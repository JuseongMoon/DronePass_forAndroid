package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.UUID

private const val MAX_FIREBASE_SKETCH_STROKE_WIDTH = 50.0

data class SketchValidationResult(
    val isValid: Boolean,
    val reason: String? = null,
)

fun SketchModel.validateForFirebasePersistence(): SketchValidationResult {
    if (!isValidFirebaseSketchId(id)) {
        return SketchValidationResult(isValid = false, reason = "invalid id")
    }
    if (!points.all { it.isValidShapeCoordinate() }) {
        return SketchValidationResult(isValid = false, reason = "invalid point")
    }
    if (!isValidFirebaseHexColor(color)) {
        return SketchValidationResult(isValid = false, reason = "invalid color")
    }
    if (!strokeWidth.isFinite() || strokeWidth <= 0.0 || strokeWidth > MAX_FIREBASE_SKETCH_STROKE_WIDTH) {
        return SketchValidationResult(isValid = false, reason = "invalid stroke width")
    }
    if (!opacity.isFinite() || opacity !in 0.0..1.0) {
        return SketchValidationResult(isValid = false, reason = "invalid opacity")
    }
    return SketchValidationResult(isValid = true)
}

fun SketchModel.isValidForFirebasePersistence(): Boolean = validateForFirebasePersistence().isValid

fun validateFirebaseSketchBatch(sketches: List<SketchModel>): SketchValidationResult {
    sketches.forEach { sketch ->
        val validation = sketch.validateForFirebasePersistence()
        if (!validation.isValid) return validation
    }

    if (sketches.map { it.id }.toSet().size != sketches.size) {
        return SketchValidationResult(isValid = false, reason = "duplicate sketch id")
    }

    return SketchValidationResult(isValid = true)
}

private fun isValidFirebaseSketchId(id: String): Boolean {
    return runCatching { UUID.fromString(id) }.isSuccess
}
