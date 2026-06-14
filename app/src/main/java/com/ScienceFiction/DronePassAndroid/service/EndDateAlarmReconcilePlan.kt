package com.ScienceFiction.DronePassAndroid.service

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel

internal data class EndDateAlarmReconcilePlan(
    val cancelShapeIds: List<String>,
    val shapesToSchedule: List<ShapeModel>,
)

internal fun buildEndDateAlarmReconcilePlan(shapes: List<ShapeModel>): EndDateAlarmReconcilePlan {
    return EndDateAlarmReconcilePlan(
        cancelShapeIds = shapes.map { it.id },
        shapesToSchedule = shapes.filter { shape ->
            !shape.isDeleted && !shape.isExpired && shape.flightEndDate != null
        },
    )
}
