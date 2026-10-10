package com.ScienceFiction.DronePassAndroid.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.withTimeoutOrNull
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
    @ApplicationContext private val appContext: Context,
) {
    companion object {
        private const val TAG = "NotificationScheduleRestorer"

        /** 알람 브로드캐스트 안에서도 끝나도록(제한 약 60초) 날씨 조회를 끊는다. 끊기면 저장된 시각으로 예약한다. */
        private const val SUN_WEATHER_FETCH_TIMEOUT_MS = 25_000L
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

    /**
     * 그날 마지막 일출·일몰 알림(10분 전)을 보여 준 뒤 다음 이벤트를 예약한다. 앱을 열지 않아도 매일 이어진다.
     * 알림 리시버가 goAsync 안에서 부른다.
     */
    suspend fun rescheduleSunAlarmsAfterDelivery(reason: String) {
        val preferences = dataStore.data.first()
        val sunriseEnabled = storedSunriseAlarmEnabled(preferences)
        val sunsetEnabled = storedSunsetAlarmEnabled(preferences)
        Log.d(TAG, "알림 표시 후 일출·일몰 재예약: reason=$reason, sunrise=$sunriseEnabled, sunset=$sunsetEnabled")
        if (sunriseEnabled || sunsetEnabled) {
            rescheduleSunAlarms(preferences, sunriseEnabled, sunsetEnabled)
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
        // OS 위치 권한을 끈 것은 "일시 중지"라 캐시한 기기 위치도 쓰지 않고 지도 중심으로 대체한다.
        val useDeviceLocation = locationConsentRepository.isAllowed(LocationPurpose.WEATHER_AND_SUN) &&
            hasLocationPermission(appContext)
        val basis = if (useDeviceLocation) {
            storedSunAlarmLocation(preferences)?.also {
                // 캐시한 위치로 날씨(일출·일몰 시각)를 받는다.
                locationConsentRepository.recordUsage(
                    LocationUsage(LocationUsagePurpose.SUNRISE_ALERT, LocationRecipient.WEATHERKIT_VIA_SERVER),
                )
            } ?: run {
                Log.w(TAG, "저장된 위치가 없어 저장된 일출·일몰 시각으로 예약합니다")
                null
            }
        } else {
            val center = mapCenterStore.weatherBasisCenter()
            center.latitude to center.longitude
        }

        // 받지 못하면 null 로 넘긴다. 스케줄러가 기존 알림을 지우지 않고 저장된 시각으로 예약한다.
        val weatherData = basis?.let { (lat, lon) ->
            withTimeoutOrNull(SUN_WEATHER_FETCH_TIMEOUT_MS) { weatherRepository.fetchWeather(lat, lon).getOrNull() }
        }
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

private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
