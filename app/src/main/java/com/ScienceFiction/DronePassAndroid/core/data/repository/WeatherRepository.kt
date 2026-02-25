package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherResponse
import com.ScienceFiction.DronePassAndroid.core.util.CRICalculator
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    // 3분 캐시
    private var cachedData: WeatherData? = null
    private var cachedLatitude: Double = 0.0
    private var cachedLongitude: Double = 0.0
    private var cacheTimestamp: Long = 0L
    private var cachedCategory: DroneCategory = DroneCategory.TOY

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
        category: DroneCategory = DroneCategory.TOY
    ): Result<WeatherData> {
        // 캐시 확인
        val now = System.currentTimeMillis()
        val locationChanged = kotlin.math.abs(latitude - cachedLatitude) > LOCATION_THRESHOLD ||
                kotlin.math.abs(longitude - cachedLongitude) > LOCATION_THRESHOLD
        val categoryChanged = cachedCategory != category

        if (!locationChanged && !categoryChanged &&
            cachedData != null &&
            now - cacheTimestamp < CACHE_DURATION_MS
        ) {
            return Result.success(cachedData!!)
        }

        return try {
            val response = weatherApi.getWeather(latitude = latitude, longitude = longitude)
            val weatherData = mapToWeatherData(response, category)

            // 캐시 업데이트
            cachedData = weatherData
            cachedLatitude = latitude
            cachedLongitude = longitude
            cachedCategory = category
            cacheTimestamp = now

            Result.success(weatherData)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
                cri = CRICalculator.calculate(temp, dewPt, wind),
                gustDifferenceLevel = GustDifferenceCalculator.evaluate(wind, gusts, category)
            )
        }

        val hourlyData = mapHourlyData(response.hourly)

        // 오늘 일출/일몰
        val sunrise = response.daily?.sunrise?.firstOrNull()
        val sunset = response.daily?.sunset?.firstOrNull()

        return WeatherData(
            current = currentData,
            hourlyForecast = hourlyData,
            sunrise = sunrise,
            sunset = sunset
        )
    }

    /**
     * 시간별 예보 데이터 매핑
     */
    private fun mapHourlyData(hourly: com.ScienceFiction.DronePassAndroid.core.data.remote.weather.HourlyWeather?): List<HourlyWeatherData> {
        if (hourly == null) return emptyList()

        val times = hourly.time ?: return emptyList()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        return times.indices.mapNotNull { i ->
            try {
                val timeStr = times[i]
                val localDateTime = LocalDateTime.parse(timeStr, formatter)
                val epochMillis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
