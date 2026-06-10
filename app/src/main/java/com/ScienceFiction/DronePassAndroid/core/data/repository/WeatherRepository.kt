package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherResponse
import com.ScienceFiction.DronePassAndroid.core.util.CRICalculator
import com.ScienceFiction.DronePassAndroid.core.util.CurrentCriSmoother
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    // 3분 캐시 — 동시 fetchWeather 호출에서 가시성 + R/M/W 직렬화.
    @Volatile private var cachedData: WeatherData? = null
    @Volatile private var cachedLatitude: Double = 0.0
    @Volatile private var cachedLongitude: Double = 0.0
    @Volatile private var cacheTimestamp: Long = 0L
    @Volatile private var cachedCategory: DroneCategory = DroneCategory.IosDefault
    private val cacheMutex = Mutex()
    private val currentCriSmoother = CurrentCriSmoother()

    companion object {
        private const val CACHE_DURATION_MS = 3 * 60 * 1000L // 3분
        private const val LOCATION_THRESHOLD = 0.01 // 약 1km 이내 위치 변경은 캐시 사용
    }

    /**
     * 날씨 데이터 가져오기 (3분 캐시 적용)
     */
    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        category: DroneCategory = DroneCategory.IosDefault
    ): Result<WeatherData> {
        // 캐시 hit-path: 락 없이 빠르게 검사 (@Volatile 가시성 보장)
        if (isCacheHit(latitude, longitude, category)) {
            cachedData?.let { return Result.success(it) }
        }

        // miss-path: 직렬화하여 동일 좌표 동시 호출이 동일 API 를 다회 호출하지 않도록.
        return cacheMutex.withLock {
            if (isCacheHit(latitude, longitude, category)) {
                cachedData?.let { return@withLock Result.success(it) }
            }
            try {
                val response = weatherApi.getWeather(latitude = latitude, longitude = longitude)
                val weatherData = mapToWeatherData(response, category)

                cachedData = weatherData
                cachedLatitude = latitude
                cachedLongitude = longitude
                cachedCategory = category
                cacheTimestamp = System.currentTimeMillis()

                Result.success(weatherData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun isCacheHit(latitude: Double, longitude: Double, category: DroneCategory): Boolean {
        val locationChanged = kotlin.math.abs(latitude - cachedLatitude) > LOCATION_THRESHOLD ||
                kotlin.math.abs(longitude - cachedLongitude) > LOCATION_THRESHOLD
        val categoryChanged = cachedCategory != category
        return !locationChanged && !categoryChanged &&
                cachedData != null &&
                System.currentTimeMillis() - cacheTimestamp < CACHE_DURATION_MS
    }

    /**
     * 캐시 무효화 (수동 갱신 시)
     */
    fun invalidateCache() {
        cacheTimestamp = 0L
    }

    /**
     * API 응답 -> 도메인 모델 변환
     */
    private fun mapToWeatherData(response: WeatherResponse, category: DroneCategory): WeatherData {
        val currentData = response.current?.let { current ->
            val temp = current.temperature ?: 0.0
            val dewPt = current.dewPoint ?: 0.0
            val wind = current.windSpeed ?: 0.0
            val gusts = current.windGusts

            CurrentWeatherData(
                temperature = temp,
                dewPoint = dewPt,
                windSpeed = wind,
                windDirection = current.windDirection ?: 0.0,
                windGusts = gusts,
                precipitation = current.precipitation ?: 0.0,
                weatherCode = current.weatherCode ?: 0,
                cri = currentCriSmoother.smooth(CRICalculator.calculateUnrounded(temp, dewPt, wind)),
                gustDifferenceLevel = GustDifferenceCalculator.evaluate(wind, gusts, category)
            )
        }

        val hourlyData = mapHourlyData(
            hourly = response.hourly,
            utcOffsetSeconds = response.utcOffsetSeconds,
        )

        val sunriseTimes = response.daily?.sunrise.orEmpty()
        val sunsetTimes = response.daily?.sunset.orEmpty()
        val sunrise = sunriseTimes.firstOrNull()
        val sunset = sunsetTimes.firstOrNull()

        return WeatherData(
            current = currentData,
            hourlyForecast = hourlyData,
            sunrise = sunrise,
            sunset = sunset,
            sunriseTimes = sunriseTimes,
            sunsetTimes = sunsetTimes,
            utcOffsetSeconds = response.utcOffsetSeconds,
        )
    }

    /**
     * 시간별 예보 데이터 매핑
     */
    private fun mapHourlyData(
        hourly: com.ScienceFiction.DronePassAndroid.core.data.remote.weather.HourlyWeather?,
        utcOffsetSeconds: Int?,
    ): List<HourlyWeatherData> {
        if (hourly == null) return emptyList()

        val times = hourly.time ?: return emptyList()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val responseOffset = runCatching {
            utcOffsetSeconds?.let(ZoneOffset::ofTotalSeconds)
        }.getOrNull()

        return times.indices.mapNotNull { i ->
            try {
                val timeStr = times[i]
                val localDateTime = LocalDateTime.parse(timeStr, formatter)
                val instant = responseOffset
                    ?.let { localDateTime.atOffset(it).toInstant() }
                    ?: localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                val epochMillis = instant.toEpochMilli()

                val temp = hourly.temperature?.getOrNull(i) ?: 0.0
                val dewPt = hourly.dewPoint?.getOrNull(i) ?: 0.0
                val wind = hourly.windSpeed?.getOrNull(i) ?: 0.0
                val gusts = hourly.windGusts?.getOrNull(i)
                val visibilityMeters = hourly.visibility?.getOrNull(i) ?: 10000.0

                HourlyWeatherData(
                    time = epochMillis,
                    temperature = temp,
                    windSpeed = wind,
                    windDirection = hourly.windDirection?.getOrNull(i) ?: 0.0,
                    windGusts = gusts,
                    gustDifference = GustDifferenceCalculator.calculateGustDifference(wind, gusts),
                    precipitation = hourly.precipitation?.getOrNull(i) ?: 0.0,
                    visibility = visibilityMeters / 1000.0, // m -> km
                    dewPoint = dewPt,
                    cri = CRICalculator.calculate(temp, dewPt, wind)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
