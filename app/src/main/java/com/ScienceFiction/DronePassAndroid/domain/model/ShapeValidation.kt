package com.ScienceFiction.DronePassAndroid.domain.model

private const val MAX_FIREBASE_RADIUS_METERS = 50_000.0
private const val MAX_FIREBASE_COORDINATE_COUNT = 1_000

data class ShapeValidationResult(
    val isValid: Boolean,
    val reason: String? = null,
)

fun Coordinate.isValidShapeCoordinate(): Boolean {
    return latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0
}

fun ShapeModel.validateForLocalPersistence(): ShapeValidationResult {
    return validateShape(
        maxRadiusMeters = null,
        maxCoordinateCount = null,
    )
}

fun ShapeModel.validateForFirebasePersistence(): ShapeValidationResult {
    return validateShape(
        maxRadiusMeters = MAX_FIREBASE_RADIUS_METERS,
        maxCoordinateCount = MAX_FIREBASE_COORDINATE_COUNT,
    )
}

fun ShapeModel.isValidForLocalPersistence(): Boolean = validateForLocalPersistence().isValid

fun ShapeModel.isValidForFirebasePersistence(): Boolean = validateForFirebasePersistence().isValid

fun validateFirebaseShapeBatch(shapes: List<ShapeModel>): ShapeValidationResult {
    shapes.forEach { shape ->
        val validation = shape.validateForFirebasePersistence()
        if (!validation.isValid) return validation
    }

    if (shapes.map { it.id }.toSet().size != shapes.size) {
        return ShapeValidationResult(isValid = false, reason = "duplicate shape id")
    }

    return ShapeValidationResult(isValid = true)
}

private fun ShapeModel.validateShape(
    maxRadiusMeters: Double?,
    maxCoordinateCount: Int?,
): ShapeValidationResult {
    if (id.isBlank()) {
        return ShapeValidationResult(isValid = false, reason = "blank id")
    }
    if (title.isBlank()) {
        return ShapeValidationResult(isValid = false, reason = "blank title")
    }
    if (!baseCoordinate.isValidShapeCoordinate()) {
        return ShapeValidationResult(isValid = false, reason = "invalid base coordinate")
    }

    return when (shapeType) {
        ShapeType.CIRCLE -> validateCircleRadius(radius, maxRadiusMeters)
        ShapeType.RECTANGLE -> validateOptionalCoordinate(secondCoordinate, "invalid second coordinate")
        ShapeType.POLYGON -> validateCoordinateList(
            coordinates = polygonCoordinates,
            minCount = 3,
            maxCount = maxCoordinateCount,
            label = "polygon coordinates",
        )
        ShapeType.POLYLINE -> validateCoordinateList(
            coordinates = polylineCoordinates,
            minCount = 2,
            maxCount = maxCoordinateCount,
            label = "polyline coordinates",
        )
    }
}

private fun validateCircleRadius(
    radius: Double?,
    maxRadiusMeters: Double?,
): ShapeValidationResult {
    if (radius == null) return ShapeValidationResult(isValid = true)
    if (!radius.isFinite() || radius <= 0.0) {
        return ShapeValidationResult(isValid = false, reason = "invalid radius")
    }
    if (maxRadiusMeters != null && radius > maxRadiusMeters) {
        return ShapeValidationResult(isValid = false, reason = "radius exceeds firebase limit")
    }
    return ShapeValidationResult(isValid = true)
}

private fun validateOptionalCoordinate(
    coordinate: Coordinate?,
    reason: String,
): ShapeValidationResult {
    if (coordinate == null || coordinate.isValidShapeCoordinate()) {
        return ShapeValidationResult(isValid = true)
    }
    return ShapeValidationResult(isValid = false, reason = reason)
}

private fun validateCoordinateList(
    coordinates: List<Coordinate>?,
    minCount: Int,
    maxCount: Int?,
    label: String,
): ShapeValidationResult {
    if (coordinates == null) return ShapeValidationResult(isValid = true)
    if (coordinates.size < minCount) {
        return ShapeValidationResult(isValid = false, reason = "$label below minimum")
    }
    if (maxCount != null && coordinates.size > maxCount) {
        return ShapeValidationResult(isValid = false, reason = "$label exceeds firebase limit")
    }
    if (!coordinates.all { it.isValidShapeCoordinate() }) {
        return ShapeValidationResult(isValid = false, reason = "invalid $label")
    }
    return ShapeValidationResult(isValid = true)
}
