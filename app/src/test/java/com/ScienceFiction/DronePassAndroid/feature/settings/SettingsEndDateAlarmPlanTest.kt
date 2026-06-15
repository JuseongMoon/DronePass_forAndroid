package com.ScienceFiction.DronePassAndroid.feature.settings

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.service.buildEndDateAlarmReconcilePlan
import com.ScienceFiction.DronePassAndroid.service.shouldScheduleEndDateAlarm
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsEndDateAlarmPlanTest {

    @Test
    fun `종료일 알림 재예약은 iOS처럼 전체 기존 알림을 취소하고 7일 전 알림이 미래인 도형만 다시 예약한다`() {
        val zoneId = ZoneId.of("Asia/Seoul")
        val now = LocalDateTime.of(2026, 6, 15, 12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val activeFuture = shape(
            id = "active-future",
            flightEndDate = LocalDateTime.of(2026, 6, 23, 12, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
        )
        val withinSevenDays = shape(
            id = "within-seven-days",
            flightEndDate = LocalDateTime.of(2026, 6, 20, 12, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli(),
        )
        val activeNoEndDate = shape(id = "active-no-end-date", flightEndDate = null)
        val expired = shape(id = "expired", flightEndDate = now - 10_000L)
        val deleted = shape(id = "deleted", flightEndDate = now + 10_000L, deletedAt = now)

        val plan = buildEndDateAlarmReconcilePlan(
            listOf(activeFuture, withinSevenDays, activeNoEndDate, expired, deleted),
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(
            listOf("active-future", "within-seven-days", "active-no-end-date", "expired", "deleted"),
            plan.cancelShapeIds,
        )
        assertEquals(listOf(activeFuture), plan.shapesToSchedule)
    }

    @Test
    fun `종료일 알림 예약 대상은 iOS처럼 7일 전 시각이 현재보다 미래일 때만 참이다`() {
        val zoneId = ZoneId.of("Asia/Seoul")
        val now = LocalDateTime.of(2026, 6, 15, 12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        assertTrue(
            shouldScheduleEndDateAlarm(
                shape(
                    id = "future",
                    flightEndDate = LocalDateTime.of(2026, 6, 23, 12, 0)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli(),
                ),
                nowMillis = now,
                zoneId = zoneId,
            ),
        )
        assertFalse(
            shouldScheduleEndDateAlarm(
                shape(
                    id = "within-seven-days",
                    flightEndDate = LocalDateTime.of(2026, 6, 20, 12, 0)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli(),
                ),
                nowMillis = now,
                zoneId = zoneId,
            ),
        )
    }

    private fun shape(
        id: String,
        flightEndDate: Long?,
        deletedAt: Long? = null,
    ): ShapeModel {
        return ShapeModel(
            id = id,
            title = id,
            radius = 100.0,
            flightEndDate = flightEndDate,
            deletedAt = deletedAt,
        )
    }
}
