package com.ScienceFiction.DronePassAndroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 기기 재부팅 시 알림을 재예약하는 BroadcastReceiver
 *
 * AlarmManager로 예약된 알림은 기기 재부팅 시 모두 사라지므로,
 * BOOT_COMPLETED 이벤트를 수신하여 활성화된 알림을 다시 예약한다.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationScheduleRestorer: NotificationScheduleRestorer

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "기기 재부팅 감지 - 알림 재예약 시작")

        val pendingResult = goAsync()
        scope.launch {
            try {
                notificationScheduleRestorer.rescheduleEnabledAlarms("boot_completed")
            } catch (e: Exception) {
                Log.e(TAG, "알림 재예약 실패", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
