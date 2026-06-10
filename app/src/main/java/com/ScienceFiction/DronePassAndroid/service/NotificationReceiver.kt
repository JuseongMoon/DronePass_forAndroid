package com.ScienceFiction.DronePassAndroid.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ScienceFiction.DronePassAndroid.R

/**
 * 알림 브로드캐스트 리시버
 * AlarmManager에서 예약된 시간에 발동되어 실제 알림을 표시합니다.
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(NotificationScheduler.EXTRA_NOTIFICATION_TYPE) ?: return
        val title = intent.getStringExtra(NotificationScheduler.EXTRA_NOTIFICATION_TITLE)
            ?: context.getString(R.string.app_name)
        val body = intent.getStringExtra(NotificationScheduler.EXTRA_NOTIFICATION_BODY) ?: ""
        val shapeId = intent.getStringExtra(NotificationScheduler.EXTRA_SHAPE_ID)

        Log.d(TAG, "알림 수신: type=$type, title=$title, shapeId=$shapeId")

        foregroundNotificationForLocalDelivery(
            title = title,
            body = body,
            shapeId = shapeId,
            appInForeground = AppForegroundState.isForeground,
        )?.let(ForegroundNotificationBus::publish)

        // 알림 채널 생성 확인
        FcmService.createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = when (type) {
            NotificationScheduler.TYPE_SUNRISE -> 1001
            NotificationScheduler.TYPE_SUNSET -> 1002
            NotificationScheduler.TYPE_END_DATE -> {
                2000 + ((shapeId?.hashCode() ?: 0) and 0x7FFFFFFF) % 10000
            }
            else -> System.currentTimeMillis().toInt()
        }
        val clickIntent = buildNotificationClickIntent(
            context = context,
            shapeId = shapeId,
            title = title,
            body = body,
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 일출/일몰/비행 종료는 시간 민감 알림 채널(IMPORTANCE_HIGH)로 보낸다.
        // FCM 푸시 등 일반 정보 알림은 FcmService 가 CHANNEL_ID 를 그대로 사용.
        val notification = NotificationCompat.Builder(context, FcmService.CHANNEL_ID_TIME_SENSITIVE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
