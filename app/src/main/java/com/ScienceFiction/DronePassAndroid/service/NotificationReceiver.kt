package com.ScienceFiction.DronePassAndroid.service

import android.app.NotificationManager
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

        Log.d(TAG, "알림 수신: type=$type, title=$title")

        // 알림 채널 생성 확인
        FcmService.createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, FcmService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val notificationId = when (type) {
            NotificationScheduler.TYPE_SUNRISE -> 1001
            NotificationScheduler.TYPE_SUNSET -> 1002
            NotificationScheduler.TYPE_END_DATE -> {
                val shapeId = intent.getStringExtra(NotificationScheduler.EXTRA_SHAPE_ID)
                2000 + ((shapeId?.hashCode() ?: 0) and 0x7FFFFFFF) % 10000
            }
            else -> System.currentTimeMillis().toInt()
        }

        notificationManager.notify(notificationId, notification)
    }
}
