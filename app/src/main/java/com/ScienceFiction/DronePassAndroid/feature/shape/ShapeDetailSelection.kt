package com.ScienceFiction.DronePassAndroid.feature.shape

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel

internal fun resolveSelectedShapeSnapshot(
    selectedShapeId: String?,
    activeShapes: List<ShapeModel>,
): ShapeModel? {
    return selectedShapeId?.let { id -> activeShapes.find { it.id == id } }
}
