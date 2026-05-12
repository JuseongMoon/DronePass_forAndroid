package com.ScienceFiction.DronePassAndroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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

    @Inject lateinit var dataStore: DataStore<Preferences>
    @Inject lateinit var notificationScheduler: NotificationScheduler
    @Inject lateinit var weatherRepository: WeatherRepository
    @Inject lateinit var shapeRepository: ShapeRepository

    companion object {
        private const val TAG = "BootCompletedReceiver"
        private val KEY_SUNRISE_ALARM_ENABLED = booleanPreferencesKey("sunrise_alarm_enabled")
        private val KEY_SUNSET_ALARM_ENABLED = booleanPreferencesKey("sunset_alarm_enabled")
        private val KEY_END_DATE_ALARM_ENABLED = booleanPreferencesKey("end_date_alarm_enabled")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "기기 재부팅 감지 - 알림 재예약 시작")

        val pendingResult = goAsync()
        scope.launch {
            try {
                rescheduleAlarms()
            } catch (e: Exception) {
                Log.e(TAG, "알림 재예약 실패", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAlarms() {
        val preferences = dataStore.data.first()

        val sunriseEnabled = preferences[KEY_SUNRISE_ALARM_ENABLED] ?: false
        val sunsetEnabled = preferences[KEY_SUNSET_ALARM_ENABLED] ?: false
        val endDateEnabled = preferences[KEY_END_DATE_ALARM_ENABLED] ?: false

        Log.d(TAG, "알림 설정: sunrise=$sunriseEnabled, sunset=$sunsetEnabled, endDate=$endDateEnabled")

        // 일출/일몰 알림 재예약
        if (sunriseEnabled || sunsetEnabled) {
            try {
                // SettingsViewModel.getUserLocation 이 마지막 호출 시점에 캐시한 좌표를 사용한다.
                // 캐시가 없으면(첫 실행 직후 재부팅 등) 서울 시청 폴백.
                val lat = preferences[UserLocationKeys.KEY_LAST_LATITUDE]
                    ?: UserLocationKeys.FALLBACK_LATITUDE
                val lon = preferences[UserLocationKeys.KEY_LAST_LONGITUDE]
                    ?: UserLocationKeys.FALLBACK_LONGITUDE
                val weatherData = weatherRepository.fetchWeather(lat, lon).getOrNull()

                if (sunriseEnabled) {
                    notificationScheduler.scheduleSunriseAlarms(weatherData?.sunrise)
                    Log.d(TAG, "일출 알림 재예약 완료")
                }
                if (sunsetEnabled) {
                    notificationScheduler.scheduleSunsetAlarms(weatherData?.sunset)
                    Log.d(TAG, "일몰 알림 재예약 완료")
                }
            } catch (e: Exception) {
                Log.e(TAG, "일출/일몰 알림 재예약 실패", e)
            }
        }

        // 비행 종료일 알림 재예약
        if (endDateEnabled) {
            try {
                val shapes = shapeRepository.getActiveShapes().first()
                var count = 0
                shapes.filter { it.flightEndDate != null && !it.isExpired }
                    .forEach { shape ->
                        notificationScheduler.scheduleEndDateAlarm(shape.id, shape.flightEndDate!!)
                        count++
                    }
                Log.d(TAG, "비행 종료일 알림 ${count}개 재예약 완료")
            } catch (e: Exception) {
                Log.e(TAG, "비행 종료일 알림 재예약 실패", e)
            }
        }
    }
}
