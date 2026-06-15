package com.ScienceFiction.DronePassAndroid.service

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal data class EndDateAlarmReconcilePlan(
    val cancelShapeIds: List<String>,
    val shapesToSchedule: List<ShapeModel>,
)

internal fun shouldScheduleEndDateAlarm(
    shape: ShapeModel,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): Boolean {
    if (shape.isDeleted) return false
    val flightEndDate = shape.flightEndDate ?: return false
    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
    return calculateEndDateNotificationTime(flightEndDate, zoneId).isAfter(now)
}

internal fun buildEndDateAlarmReconcilePlan(
    shapes: List<ShapeModel>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): EndDateAlarmReconcilePlan {
    return EndDateAlarmReconcilePlan(
        cancelShapeIds = shapes.map { it.id },
        shapesToSchedule = shapes.filter { shape ->
            shouldScheduleEndDateAlarm(shape, nowMillis, zoneId)
        },
    )
}
