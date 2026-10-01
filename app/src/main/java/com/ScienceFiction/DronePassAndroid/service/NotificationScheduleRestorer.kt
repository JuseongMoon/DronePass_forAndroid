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
import com.ScienceFiction.DronePassAndroid.core.location.LocationConsentRepository
import com.ScienceFiction.DronePassAndroid.core.location.LocationPurpose
import com.ScienceFiction.DronePassAndroid.core.location.LocationRecipient
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsage
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsagePurpose
import com.ScienceFiction.DronePassAndroid.core.location.MapCenterStore
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
    private val locationConsentRepository: LocationConsentRepository,
    private val mapCenterStore: MapCenterStore,
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

    /** 켜진 일출·일몰 알림을 [weatherData] 로 다시 예약한다. 하나라도 예약했으면 true. */
    suspend fun rescheduleSunAlarmsForWeatherData(weatherData: WeatherData): Boolean {
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
        return sunriseEnabled || sunsetEnabled
    }

    /**
     * 기준 위치: 날씨·일출/일몰(P2) 동의 상태면 마지막으로 읽은 기기 위치(0.01° 캐시), 아니면 마지막으로 본 지도 중심.
     * 동의 없이는 기기 위치 캐시를 읽지 않는다(철회 때 캐시도 지워진다).
     */
    private suspend fun rescheduleSunAlarms(
        preferences: Preferences,
        sunriseEnabled: Boolean,
        sunsetEnabled: Boolean,
    ) {
        val (lat, lon) = if (locationConsentRepository.isAllowed(LocationPurpose.WEATHER_AND_SUN)) {
            val location = storedSunAlarmLocation(preferences)
            if (location == null) {
                Log.w(TAG, "저장된 위치가 없어 일출/일몰 알림 복구를 건너뜀")
                return
            }
            // 캐시한 위치로 날씨(일출·일몰 시각)를 받는다.
            locationConsentRepository.recordUsage(
                LocationUsage(LocationUsagePurpose.SUNRISE_ALERT, LocationRecipient.WEATHERKIT_VIA_SERVER),
            )
            location
        } else {
            val center = mapCenterStore.weatherBasisCenter()
            center.latitude to center.longitude
        }

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
        val plan = buildEndDateAlarmReconcilePlan(shapeRepository.getAllShapes().first())
        notificationScheduler.cancelKnownEndDateAlarms(plan.cancelShapeIds)
        plan.shapesToSchedule.forEach { shape ->
            val flightEndDate = shape.flightEndDate ?: return@forEach
            notificationScheduler.scheduleEndDateAlarm(
                shapeId = shape.id,
                flightEndDate = flightEndDate,
                shapeTitle = shape.title,
            )
        }
    }
}
