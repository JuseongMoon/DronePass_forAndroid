package com.ScienceFiction.DronePassAndroid.feature.settings

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsEndDateAlarmPlanTest {

    @Test
    fun `종료일 알림 재예약은 iOS처럼 전체 기존 알림을 취소하고 활성 미래 도형만 다시 예약한다`() {
        val now = System.currentTimeMillis()
        val activeFuture = shape(id = "active-future", flightEndDate = now + 10_000L)
        val activeNoEndDate = shape(id = "active-no-end-date", flightEndDate = null)
        val expired = shape(id = "expired", flightEndDate = now - 10_000L)
        val deleted = shape(id = "deleted", flightEndDate = now + 10_000L, deletedAt = now)

        val plan = buildEndDateAlarmReconcilePlan(
            listOf(activeFuture, activeNoEndDate, expired, deleted),
        )

        assertEquals(
            listOf("active-future", "active-no-end-date", "expired", "deleted"),
            plan.cancelShapeIds,
        )
        assertEquals(listOf(activeFuture), plan.shapesToSchedule)
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
