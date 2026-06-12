package com.ScienceFiction.DronePassAndroid.service

import com.ScienceFiction.DronePassAndroid.R
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationSchedulerTest {

    @Test
    fun `종료일 알림은 iOS와 동일하게 종료일 정확히 7일 전 같은 분으로 계산한다`() {
        val zoneId = ZoneId.of("Asia/Seoul")
        val endDate = LocalDateTime.of(2026, 7, 10, 15, 45, 30)
        val endDateMillis = endDate.atZone(zoneId).toInstant().toEpochMilli()

        assertEquals(
            LocalDateTime.of(2026, 7, 3, 15, 45, 0),
            calculateEndDateNotificationTime(endDateMillis, zoneId),
        )
    }

    @Test
    fun `종료일 알림 PendingIntent action 은 iOS identifier 처럼 도형 ID를 포함해 고유하다`() {
        assertEquals(
            "com.ScienceFiction.DronePassAndroid.notification.END_DATE.shape-a",
            endDateNotificationAction("shape-a"),
        )
        assertEquals(
            "com.ScienceFiction.DronePassAndroid.notification.END_DATE.shape-b",
            endDateNotificationAction("shape-b"),
        )
    }

    @Test
    fun `종료일 알림 본문은 iOS처럼 빈 문자열과 공백 제목도 원문 그대로 포함한다`() {
        assertEquals(
            EndDateNotificationBodyResource(body = R.string.notification_end_date_body),
            endDateNotificationBodyResource(null),
        )
        assertEquals(
            EndDateNotificationBodyResource(
                body = R.string.notification_end_date_body_with_title,
                shapeTitle = "",
            ),
            endDateNotificationBodyResource(""),
        )
        assertEquals(
            EndDateNotificationBodyResource(
                body = R.string.notification_end_date_body_with_title,
                shapeTitle = "   ",
            ),
            endDateNotificationBodyResource("   "),
        )
        assertEquals(
            EndDateNotificationBodyResource(
                body = R.string.notification_end_date_body_with_title,
                shapeTitle = "Flight Area",
            ),
            endDateNotificationBodyResource("Flight Area"),
        )
    }

    @Test
    fun `일출 일몰 알림은 iOS와 동일하게 지난 오늘 값 대신 다음 미래 값을 선택한다`() {
        val now = LocalDateTime.of(2026, 6, 3, 12, 0)

        assertEquals(
            LocalDateTime.of(2026, 6, 4, 7, 15),
            selectNextSunEventTime(
                listOf("2026-06-03T07:15:30", "2026-06-04T07:15:30"),
                now,
            ),
        )
    }

    @Test
    fun `일출 일몰 알림은 오늘 값이 아직 미래면 오늘 값을 선택한다`() {
        val now = LocalDateTime.of(2026, 6, 3, 6, 0)

        assertEquals(
            LocalDateTime.of(2026, 6, 3, 7, 15),
            selectNextSunEventTime(
                listOf("2026-06-03T07:15:30", "2026-06-04T07:15:30"),
                now,
            ),
        )
    }

    @Test
    fun `일출 일몰 알림 trigger millis 는 날씨 응답 timezone offset 을 사용한다`() {
        val triggerTime = LocalDateTime.of(2026, 1, 1, 7, 15, 30)

        assertEquals(
            Instant.parse("2025-12-31T22:15:00Z").toEpochMilli(),
            calculateNotificationTriggerAtMillis(
                triggerTime = triggerTime,
                zoneId = resolveNotificationSunZone(9 * 60 * 60),
            ),
        )
    }

    @Test
    fun `일출 일몰 알림은 미래 후보가 없으면 null을 반환한다`() {
        val now = LocalDateTime.of(2026, 6, 3, 12, 0)

        assertNull(
            selectNextSunEventTime(
                listOf("2026-06-02T07:15", "2026-06-03T07:15"),
                now,
            ),
        )
    }

    @Test
    fun `일출 일몰 예약 계획은 후보가 없어도 iOS처럼 기존 알림을 먼저 취소한다`() {
        val now = LocalDateTime.of(2026, 6, 3, 12, 0)

        listOf(
            null,
            emptyList(),
            listOf("2026-06-02T07:15", "2026-06-03T07:15"),
        ).forEach { candidates ->
            val plan = resolveSunAlarmSchedulePlan(candidates, now)

            assertTrue(plan.shouldCancelExisting)
            assertNull(plan.nextEventTime)
        }
    }

    @Test
    fun `일출 일몰 예약 계획은 미래 후보가 있어도 iOS처럼 기존 알림을 지운 뒤 다음 시간을 사용한다`() {
        val now = LocalDateTime.of(2026, 6, 3, 12, 0)
        val plan = resolveSunAlarmSchedulePlan(
            listOf("2026-06-03T07:15", "2026-06-04T07:15"),
            now,
        )

        assertTrue(plan.shouldCancelExisting)
        assertEquals(LocalDateTime.of(2026, 6, 4, 7, 15), plan.nextEventTime)
    }

    @Test
    fun `일출 알림 제목과 본문은 iOS처럼 30분 10분 리소스를 구분한다`() {
        assertEquals(
            NotificationContentResourceIds(
                title = R.string.notification_sunrise_30min_title,
                body = R.string.notification_sunrise_30min_body,
            ),
            sunriseNotificationContentResources(minutesBefore = 30),
        )
        assertEquals(
            NotificationContentResourceIds(
                title = R.string.notification_sunrise_10min_title,
                body = R.string.notification_sunrise_10min_body,
            ),
            sunriseNotificationContentResources(minutesBefore = 10),
        )
    }

    @Test
    fun `일몰 알림 제목과 본문은 iOS처럼 30분 10분 리소스를 구분한다`() {
        assertEquals(
            NotificationContentResourceIds(
                title = R.string.notification_sunset_30min_title,
                body = R.string.notification_sunset_30min_body,
            ),
            sunsetNotificationContentResources(minutesBefore = 30),
        )
        assertEquals(
            NotificationContentResourceIds(
                title = R.string.notification_sunset_10min_title,
                body = R.string.notification_sunset_10min_body,
            ),
            sunsetNotificationContentResources(minutesBefore = 10),
        )
    }
}
