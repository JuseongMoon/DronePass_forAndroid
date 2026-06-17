package com.ScienceFiction.DronePassAndroid.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationReceiverTest {

    @Test
    fun `예약된 로컬 알림 ID는 iOS identifier 처럼 request code 별로 유지된다`() {
        assertEquals(
            10001,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNRISE,
                shapeId = null,
                explicitNotificationId = 10001,
            ),
        )
        assertEquals(
            10002,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNRISE,
                shapeId = null,
                explicitNotificationId = 10002,
            ),
        )
        assertEquals(
            10003,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNSET,
                shapeId = null,
                explicitNotificationId = 10003,
            ),
        )
        assertEquals(
            10004,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNSET,
                shapeId = null,
                explicitNotificationId = 10004,
            ),
        )
    }

    @Test
    fun `기존 예약 알림처럼 명시 ID가 없으면 타입별 legacy ID로 표시된다`() {
        assertEquals(
            1001,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNRISE,
                shapeId = null,
                explicitNotificationId = null,
            ),
        )
        assertEquals(
            1002,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNSET,
                shapeId = null,
                explicitNotificationId = null,
            ),
        )
        assertEquals(
            1001,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNRISE,
                shapeId = null,
                explicitNotificationId = 0,
            ),
        )
        assertEquals(
            1002,
            localNotificationId(
                type = NotificationScheduler.TYPE_SUNSET,
                shapeId = null,
                explicitNotificationId = -1,
            ),
        )
    }

    @Test
    fun `종료일 알림 legacy ID는 도형별로 안정적으로 분리된다`() {
        val first = localNotificationId(
            type = NotificationScheduler.TYPE_END_DATE,
            shapeId = "shape-a",
            explicitNotificationId = null,
            fallbackTimeMillis = 1L,
        )
        val second = localNotificationId(
            type = NotificationScheduler.TYPE_END_DATE,
            shapeId = "shape-a",
            explicitNotificationId = null,
            fallbackTimeMillis = 2L,
        )
        val other = localNotificationId(
            type = NotificationScheduler.TYPE_END_DATE,
            shapeId = "shape-b",
            explicitNotificationId = null,
            fallbackTimeMillis = 1L,
        )

        assertEquals(first, second)
        assertNotEquals(first, other)
    }

    @Test
    fun `종료일 알림 표시 키는 해시 충돌 도형도 iOS identifier 처럼 shapeId 태그로 분리한다`() {
        val first = localNotificationDeliveryKey(
            type = NotificationScheduler.TYPE_END_DATE,
            shapeId = "FB",
            explicitNotificationId = null,
            fallbackTimeMillis = 1L,
        )
        val second = localNotificationDeliveryKey(
            type = NotificationScheduler.TYPE_END_DATE,
            shapeId = "Ea",
            explicitNotificationId = null,
            fallbackTimeMillis = 1L,
        )

        assertEquals(first.id, second.id)
        assertEquals("end_date_FB", first.tag)
        assertEquals("end_date_Ea", second.tag)
        assertNotEquals(first, second)
    }

    @Test
    fun `종료일 알림 표시 태그는 공백 shapeId 를 iOS처럼 식별자 없음으로 취급한다`() {
        assertEquals(
            LocalNotificationDeliveryKey(tag = null, id = 2000),
            localNotificationDeliveryKey(
                type = NotificationScheduler.TYPE_END_DATE,
                shapeId = "   ",
                explicitNotificationId = null,
                fallbackTimeMillis = 1L,
            ),
        )
    }

    @Test
    fun `알 수 없는 로컬 알림 타입은 기존처럼 시간 기반 ID로 fallback 한다`() {
        assertEquals(
            1234,
            localNotificationId(
                type = "unknown",
                shapeId = null,
                explicitNotificationId = null,
                fallbackTimeMillis = 1234L,
            ),
        )
    }
}
