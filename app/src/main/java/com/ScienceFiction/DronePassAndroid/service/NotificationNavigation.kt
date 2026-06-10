package com.ScienceFiction.DronePassAndroid.service

import android.content.Context
import android.content.Intent
import com.ScienceFiction.DronePassAndroid.MainActivity

private val ShapeIdExtraKeys = listOf(
    "shapeId",
    NotificationScheduler.EXTRA_SHAPE_ID,
    "shapeID",
)

private val NotificationTitleExtraKeys = listOf(
    NotificationScheduler.EXTRA_NOTIFICATION_TITLE,
    "title",
)

private val NotificationBodyExtraKeys = listOf(
    NotificationScheduler.EXTRA_NOTIFICATION_BODY,
    "body",
    "message",
)

internal fun normalizeNotificationShapeId(shapeId: String?): String? {
    return shapeId?.trim()?.takeIf { it.isNotEmpty() }
}

internal fun normalizeNotificationText(text: String?): String? {
    return text?.trim()?.takeIf { it.isNotEmpty() }
}

internal data class NotificationClickPayload(
    val shapeId: String?,
    val title: String?,
    val body: String?,
)

internal fun notificationClickPayload(
    shapeId: String?,
    title: String?,
    body: String?,
): NotificationClickPayload {
    return NotificationClickPayload(
        shapeId = normalizeNotificationShapeId(shapeId),
        title = normalizeNotificationText(title),
        body = normalizeNotificationText(body),
    )
}

internal fun extractNotificationShapeId(data: Map<String, String>): String? {
    for (key in ShapeIdExtraKeys) {
        normalizeNotificationShapeId(data[key])?.let { return it }
    }
    return null
}

private fun extractNotificationText(
    data: Map<String, String>,
    keys: List<String>,
): String? {
    for (key in keys) {
        normalizeNotificationText(data[key])?.let { return it }
    }
    return null
}

private fun extractNotificationText(
    intent: Intent,
    keys: List<String>,
): String? {
    for (key in keys) {
        normalizeNotificationText(intent.getStringExtra(key))?.let { return it }
    }
    return null
}

internal fun extractNotificationShapeId(intent: Intent?): String? {
    if (intent == null) return null
    for (key in ShapeIdExtraKeys) {
        normalizeNotificationShapeId(intent.getStringExtra(key))?.let { return it }
    }
    return null
}

internal fun extractForegroundNotification(data: Map<String, String>): ForegroundNotification? {
    return foregroundNotificationFromClickPayload(
        notificationClickPayload(
            shapeId = extractNotificationShapeId(data),
            title = extractNotificationText(data, NotificationTitleExtraKeys),
            body = extractNotificationText(data, NotificationBodyExtraKeys),
        )
    )
}

internal fun foregroundNotificationFromClickPayload(
    payload: NotificationClickPayload,
): ForegroundNotification? {
    val title = payload.title
    val body = payload.body
    if (title == null && body == null) return null

    return ForegroundNotification(
        title = title.orEmpty(),
        body = body.orEmpty(),
        shapeId = payload.shapeId,
    )
}

internal fun foregroundNotificationForLocalDelivery(
    title: String,
    body: String,
    shapeId: String?,
    appInForeground: Boolean,
): ForegroundNotification? {
    if (!appInForeground) return null
    return ForegroundNotification(
        title = title,
        body = body,
        shapeId = normalizeNotificationShapeId(shapeId),
    )
}

internal fun foregroundNotificationForRemoteDelivery(
    title: String,
    body: String,
    shapeId: String?,
    appInForeground: Boolean,
): ForegroundNotification? {
    if (!appInForeground) return null
    return ForegroundNotification(
        title = title,
        body = body,
        shapeId = normalizeNotificationShapeId(shapeId),
    )
}

internal fun extractForegroundNotification(intent: Intent?): ForegroundNotification? {
    if (intent == null) return null
    return foregroundNotificationFromClickPayload(
        notificationClickPayload(
            shapeId = extractNotificationShapeId(intent),
            title = extractNotificationText(intent, NotificationTitleExtraKeys),
            body = extractNotificationText(intent, NotificationBodyExtraKeys),
        )
    )
}

internal fun buildNotificationClickIntent(
    context: Context,
    shapeId: String? = null,
    title: String? = null,
    body: String? = null,
): Intent {
    val payload = notificationClickPayload(
        shapeId = shapeId,
        title = title,
        body = body,
    )
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        payload.shapeId?.let {
            putExtra(NotificationScheduler.EXTRA_SHAPE_ID, it)
        }
        payload.title?.let {
            putExtra(NotificationScheduler.EXTRA_NOTIFICATION_TITLE, it)
        }
        payload.body?.let {
            putExtra(NotificationScheduler.EXTRA_NOTIFICATION_BODY, it)
        }
    }
}
