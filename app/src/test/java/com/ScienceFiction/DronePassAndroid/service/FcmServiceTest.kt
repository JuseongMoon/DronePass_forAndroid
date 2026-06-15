package com.ScienceFiction.DronePassAndroid.service

import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmServiceTest {

    @Test
    fun `FCM device data 는 iOS 와 같은 활성 토큰 필드를 포함한다`() {
        val data = buildFcmDeviceData(
            token = "token-1",
            appVersion = "1.0",
        )

        assertEquals("token-1", data["fcmToken"])
        assertEquals("android", data["platform"])
        assertEquals("1.0", data["appVersion"])
        assertEquals(true, data["isActive"])
        assertTrue(data["createdAt"] is FieldValue)
        assertTrue(data["updatedAt"] is FieldValue)
        assertFalse(data.containsKey("lastUpdated"))
    }

    @Test
    fun `FCM device data 는 iOS 처럼 매 저장마다 createdAt 과 updatedAt 을 함께 쓴다`() {
        val data = buildFcmDeviceData(
            token = "token-1",
            appVersion = "1.0",
        )

        assertTrue(data["createdAt"] is FieldValue)
        assertTrue(data["updatedAt"] is FieldValue)
    }

    @Test
    fun `FCM token 비활성화 데이터는 iOS 와 같이 isActive false 와 updatedAt 을 기록한다`() {
        val data = buildFcmDeactivateData()

        assertFalse(data.containsKey("fcmToken"))
        assertEquals(false, data["isActive"])
        assertTrue(data["updatedAt"] is FieldValue)
        assertFalse(data.containsKey("lastUpdated"))
    }

    @Test
    fun `FCM device id 저장 키는 iOS 와 같은 DeviceUUID 를 사용한다`() {
        assertEquals("DeviceUUID", FCM_DEVICE_ID_PREFERENCE_KEY)
    }

    @Test
    fun `기존 Android device id 는 iOS 식 저장 키로 마이그레이션할 수 있다`() {
        assertEquals("legacy-id", selectStoredFcmDeviceId(primary = null, legacy = "legacy-id"))
        assertEquals("primary-id", selectStoredFcmDeviceId(primary = "primary-id", legacy = "legacy-id"))
        assertNull(selectStoredFcmDeviceId(primary = " ", legacy = ""))
        assertTrue(shouldMigrateLegacyFcmDeviceId(primary = null, legacy = "legacy-id"))
        assertFalse(shouldMigrateLegacyFcmDeviceId(primary = "primary-id", legacy = "legacy-id"))
    }

    @Test
    fun `FCM appVersion 은 iOS 처럼 버전과 빌드 번호를 함께 기록한다`() {
        assertEquals("1.2.3 (45)", formatFcmAppVersion(versionName = "1.2.3", versionCode = 45))
        assertEquals("Unknown (Unknown)", formatFcmAppVersion(versionName = " ", versionCode = null))
    }

    @Test
    fun `FCM token 갱신 로그는 release 에서 원문 토큰을 노출하지 않는다`() {
        val token = "secret-fcm-token"
        val message = fcmTokenUpdatedLogMessage(token)

        assertFalse(message.contains(token))
        assertTrue(message.contains("length=${token.length}"))
    }

    @Test
    fun `FCM device id 로그는 release 에서 원문 식별자를 노출하지 않는다`() {
        val deviceId = "secret-device-id"
        val messages = listOf(
            fcmDeviceIdCreatedLogMessage(deviceId),
            fcmTokenStoredLogMessage(deviceId),
            fcmTokenDeactivatedLogMessage(deviceId),
        )

        messages.forEach { message ->
            assertFalse(message.contains(deviceId))
            assertTrue(message.contains(deviceId.length.toString()))
        }
    }

    @Test
    fun `로컬 알림 수신 로그는 제목과 shapeId 원문을 노출하지 않는다`() {
        val title = "비공개 도형 종료 알림"
        val shapeId = "secret-shape-id"
        val message = notificationReceivedLogMessage(
            type = NotificationScheduler.TYPE_END_DATE,
            title = title,
            shapeId = shapeId,
        )

        assertFalse(message.contains(title))
        assertFalse(message.contains(shapeId))
        assertTrue(message.contains("titleLength=${title.length}"))
        assertTrue(message.contains("hasShapeId=true"))
    }

    @Test
    fun `FCM 데이터에서 camelCase shapeId 를 알림 포커스 대상으로 추출한다`() {
        val shapeId = extractNotificationShapeId(
            mapOf(
                "title" to "비행 종료일 알림",
                "shapeId" to "shape-123",
            )
        )

        assertEquals("shape-123", shapeId)
    }

    @Test
    fun `FCM 데이터에서 snake_case shape_id 를 알림 포커스 대상으로 추출한다`() {
        val shapeId = extractNotificationShapeId(
            mapOf(
                "title" to "비행 종료일 알림",
                NotificationScheduler.EXTRA_SHAPE_ID to "shape-456",
            )
        )

        assertEquals("shape-456", shapeId)
    }

    @Test
    fun `빈 shapeId 는 알림 포커스 대상으로 사용하지 않는다`() {
        val shapeId = extractNotificationShapeId(
            mapOf(
                "shapeId" to "   ",
                NotificationScheduler.EXTRA_SHAPE_ID to "",
            )
        )

        assertNull(shapeId)
    }

    @Test
    fun `알림 클릭 payload 는 iOS 탭 팝업처럼 제목 본문 원문을 보존하고 shapeId 만 정규화한다`() {
        val payload = notificationClickPayload(
            shapeId = " shape-789 ",
            title = " 비행 종료일 알림 ",
            body = " 곧 비행이 종료됩니다. ",
        )

        assertEquals("shape-789", payload.shapeId)
        assertEquals(" 비행 종료일 알림 ", payload.title)
        assertEquals(" 곧 비행이 종료됩니다. ", payload.body)
    }

    @Test
    fun `알림 클릭 payload 에 제목과 본문 키가 있으면 iOS처럼 빈 문자열도 팝업으로 복원한다`() {
        val notification = foregroundNotificationFromClickPayload(
            notificationClickPayload(
                shapeId = "shape-1",
                title = " ",
                body = "",
            )
        )

        requireNotNull(notification)
        assertEquals(" ", notification.title)
        assertEquals("", notification.body)
        assertEquals("shape-1", notification.shapeId)
    }

    @Test
    fun `알림 클릭 payload 에 제목과 본문 값이 모두 없으면 팝업을 만들지 않는다`() {
        val notification = foregroundNotificationFromClickPayload(
            notificationClickPayload(
                shapeId = "shape-1",
                title = null,
                body = null,
            )
        )

        assertNull(notification)
    }

    @Test
    fun `FCM data payload 제목 본문은 iOS처럼 알림 탭 팝업 데이터로 복원한다`() {
        val notification = extractForegroundNotification(
            mapOf(
                "shapeId" to "shape-123",
                "title" to " 비행 종료일 알림 ",
                "body" to " 7일 뒤 종료됩니다. ",
            )
        )

        requireNotNull(notification)
        assertEquals("shape-123", notification.shapeId)
        assertEquals(" 비행 종료일 알림 ", notification.title)
        assertEquals(" 7일 뒤 종료됩니다. ", notification.body)
    }

    @Test
    fun `FCM data payload 는 iOS처럼 첫 제목 본문 키가 빈 문자열이어도 fallback 키로 대체하지 않는다`() {
        val notification = extractForegroundNotification(
            mapOf(
                "shapeId" to "shape-123",
                NotificationScheduler.EXTRA_NOTIFICATION_TITLE to "",
                "title" to "대체 제목",
                NotificationScheduler.EXTRA_NOTIFICATION_BODY to " ",
                "body" to "대체 본문",
            )
        )

        requireNotNull(notification)
        assertEquals("", notification.title)
        assertEquals(" ", notification.body)
    }

    @Test
    fun `FCM data-only 표시 제목 본문은 로컬 extra 키도 iOS처럼 사용한다`() {
        val data = mapOf(
            NotificationScheduler.EXTRA_NOTIFICATION_TITLE to "도형 종료일 알림",
            NotificationScheduler.EXTRA_NOTIFICATION_BODY to "도형이 곧 종료됩니다.",
            "title" to "대체 제목",
            "body" to "대체 본문",
        )

        assertEquals("도형 종료일 알림", extractNotificationTitle(data))
        assertEquals("도형이 곧 종료됩니다.", extractNotificationBody(data))
    }

    @Test
    fun `FCM data-only 표시 제목 본문도 첫 키가 빈 문자열이면 fallback 하지 않는다`() {
        val data = mapOf(
            NotificationScheduler.EXTRA_NOTIFICATION_TITLE to "",
            NotificationScheduler.EXTRA_NOTIFICATION_BODY to " ",
            "title" to "대체 제목",
            "body" to "대체 본문",
        )

        assertEquals("", extractNotificationTitle(data))
        assertEquals(" ", extractNotificationBody(data))
    }

    @Test
    fun `로컬 알림 extras 제목 본문도 iOS처럼 알림 탭 팝업 데이터로 복원한다`() {
        val notification = extractForegroundNotification(
            mapOf(
                NotificationScheduler.EXTRA_SHAPE_ID to "shape-456",
                NotificationScheduler.EXTRA_NOTIFICATION_TITLE to "도형 종료일 알림",
                NotificationScheduler.EXTRA_NOTIFICATION_BODY to "도형이 곧 종료됩니다.",
            )
        )

        requireNotNull(notification)
        assertEquals("shape-456", notification.shapeId)
        assertEquals("도형 종료일 알림", notification.title)
        assertEquals("도형이 곧 종료됩니다.", notification.body)
    }

    @Test
    fun `로컬 알림은 iOS willPresent 처럼 앱 포그라운드에서만 팝업 이벤트를 만든다`() {
        val notification = foregroundNotificationForLocalDelivery(
            title = "일출 10분 전",
            body = "일출까지 10분 남았습니다.",
            shapeId = " shape-1 ",
            appInForeground = true,
        )

        requireNotNull(notification)
        assertEquals("일출 10분 전", notification.title)
        assertEquals("일출까지 10분 남았습니다.", notification.body)
        assertEquals("shape-1", notification.shapeId)

        assertNull(
            foregroundNotificationForLocalDelivery(
                title = "일출 10분 전",
                body = "일출까지 10분 남았습니다.",
                shapeId = "shape-1",
                appInForeground = false,
            )
        )
    }

    @Test
    fun `FCM 수신 팝업은 iOS willPresent 처럼 앱 포그라운드에서만 만든다`() {
        val notification = foregroundNotificationForRemoteDelivery(
            title = "비행 종료일 알림",
            body = "7일 뒤 종료됩니다.",
            shapeId = " shape-2 ",
            appInForeground = true,
        )

        requireNotNull(notification)
        assertEquals("비행 종료일 알림", notification.title)
        assertEquals("7일 뒤 종료됩니다.", notification.body)
        assertEquals("shape-2", notification.shapeId)

        assertNull(
            foregroundNotificationForRemoteDelivery(
                title = "비행 종료일 알림",
                body = "7일 뒤 종료됩니다.",
                shapeId = "shape-2",
                appInForeground = false,
            )
        )
    }
}
