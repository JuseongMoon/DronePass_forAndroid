package com.ScienceFiction.DronePassAndroid.service

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.ScienceFiction.DronePassAndroid.core.data.storedEndDateAlarmEnabled
import com.ScienceFiction.DronePassAndroid.core.data.storedSunAlarmLocation
import com.ScienceFiction.DronePassAndroid.core.data.storedSunriseAlarmEnabled
import com.ScienceFiction.DronePassAndroid.core.data.storedSunsetAlarmEnabled
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS SettingManager.restoreNotificationSchedules 와 같은 앱 시작/부팅 알림 복구 담당.
 */
@Singleton
class NotificationScheduleRestorer @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val notificationScheduler: NotificationScheduler,
    private val weatherRepository: WeatherRepository,
    private val shapeRepository: ShapeRepository,
) {
    companion object {
        private const val TAG = "NotificationScheduleRestorer"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun rescheduleEnabledAlarmsAsync(reason: String) {
        scope.launch {
            runCatching { rescheduleEnabledAlarms(reason) }
                .onFailure { Log.e(TAG, "알림 재예약 실패: reason=$reason", it) }
        }
    }

    suspend fun rescheduleEnabledAlarms(reason: String) {
        val preferences = dataStore.data.first()
        val sunriseEnabled = storedSunriseAlarmEnabled(preferences)
        val sunsetEnabled = storedSunsetAlarmEnabled(preferences)
        val endDateEnabled = storedEndDateAlarmEnabled(preferences)

        Log.d(
            TAG,
            "알림 설정 복구: reason=$reason, sunrise=$sunriseEnabled, sunset=$sunsetEnabled, endDate=$endDateEnabled",
        )

        if (sunriseEnabled || sunsetEnabled) {
            rescheduleSunAlarms(preferences, sunriseEnabled, sunsetEnabled)
        }

        if (endDateEnabled) {
            rescheduleEndDateAlarms()
        }
    }

    suspend fun rescheduleSunAlarmsForWeatherData(weatherData: WeatherData) {
        val preferences = dataStore.data.first()
        val sunriseEnabled = storedSunriseAlarmEnabled(preferences)
        val sunsetEnabled = storedSunsetAlarmEnabled(preferences)

        if (sunriseEnabled) {
            notificationScheduler.scheduleSunriseAlarms(
                sunriseTimeStrings = weatherData.sunriseTimes,
                utcOffsetSeconds = weatherData.utcOffsetSeconds,
            )
        }
        if (sunsetEnabled) {
            notificationScheduler.scheduleSunsetAlarms(
                sunsetTimeStrings = weatherData.sunsetTimes,
                utcOffsetSeconds = weatherData.utcOffsetSeconds,
            )
        }
    }

    private suspend fun rescheduleSunAlarms(
        preferences: Preferences,
        sunriseEnabled: Boolean,
        sunsetEnabled: Boolean,
    ) {
        val location = storedSunAlarmLocation(preferences)
        if (location == null) {
            Log.w(TAG, "저장된 위치가 없어 일출/일몰 알림 복구를 건너뜀")
            return
        }
        val (lat, lon) = location

        val weatherData = weatherRepository.fetchWeather(lat, lon).getOrNull()
        if (sunriseEnabled) {
            notificationScheduler.scheduleSunriseAlarms(
                sunriseTimeStrings = weatherData?.sunriseTimes,
                utcOffsetSeconds = weatherData?.utcOffsetSeconds,
            )
        }
        if (sunsetEnabled) {
            notificationScheduler.scheduleSunsetAlarms(
                sunsetTimeStrings = weatherData?.sunsetTimes,
                utcOffsetSeconds = weatherData?.utcOffsetSeconds,
            )
        }
    }

    private suspend fun rescheduleEndDateAlarms() {
        shapeRepository.getActiveShapes().first()
            .filter { it.flightEndDate != null && !it.isExpired }
            .forEach { shape ->
                notificationScheduler.scheduleEndDateAlarm(
                    shapeId = shape.id,
                    flightEndDate = shape.flightEndDate!!,
                    shapeTitle = shape.title,
                )
            }
    }
}
