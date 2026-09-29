package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitDataSource
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitHour
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitResponse
import com.ScienceFiction.DronePassAndroid.core.util.CRICalculator
import com.ScienceFiction.DronePassAndroid.core.util.CurrentCriSmoother
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** WeatherKit 풍속·돌풍(km/h) → 앱 표시 단위(m/s). iOS `converted(to: .metersPerSecond)` 와 같다. */
internal const val KMH_PER_MPS = 3.6

/** 그래프·예보 계산에 쓰는 미래 범위. iOS FORECAST_DAYS(3일)와 같다. */
internal const val WEATHER_FORECAST_HORIZON_MS = 72L * 60L * 60L * 1000L

/** 일출·일몰을 넘겨 줄 날 수. 알림 예약과 타임라인은 오늘·내일·모레만 쓴다. */
internal const val WEATHER_SUN_EVENT_DAYS = 3

/**
 * 복귀 갱신과 3분 타이머가 거의 같이 울리는 경우 같은 요청을 한 번만 보내기 위한 창.
 * 실제 유효 기간 캐시는 중계 서버가 지키므로 클라이언트는 이 짧은 창만 둔다.
 */
private const val DUPLICATE_REQUEST_WINDOW_MS = 10_000L

/** WeatherKit 격자(0.01°) 안의 위치 이동은 같은 예보다. */
private const val WEATHERKIT_GRID_DEGREES = 0.01

@Singleton
class WeatherRepository @Inject constructor(
    private val dataSource: WeatherKitDataSource,
) {
    private val requestMutex = Mutex()
    private val currentCriSmoother = CurrentCriSmoother()

    @Volatile private var lastResult: WeatherData? = null
    @Volatile private var lastLatitude: Double = Double.NaN
    @Volatile private var lastLongitude: Double = Double.NaN
    @Volatile private var lastCategory: DroneCategory = DroneCategory.IosDefault
    @Volatile private var lastFetchedAtElapsed: Long = 0L

    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        category: DroneCategory = DroneCategory.IosDefault,
        nowMillis: () -> Long = System::currentTimeMillis,
    ): Result<WeatherData> = requestMutex.withLock {
        val now = nowMillis()
        if (isDuplicateRequest(latitude, longitude, category, now)) {
            lastResult?.let { return@withLock Result.success(it) }
        }
        runCatching {
            val zone = ZoneId.systemDefault()
            val response = dataSource.fetch(latitude, longitude, zone.id)
            mapWeatherKitResponse(
                response = response,
                category = category,
                nowMillis = now,
                zone = zone,
                smoothCurrentCri = currentCriSmoother::smooth,
            )
        }.onSuccess { data ->
            lastResult = data
            lastLatitude = latitude
            lastLongitude = longitude
            lastCategory = category
            lastFetchedAtElapsed = now
        }
    }

    private fun isDuplicateRequest(
        latitude: Double,
        longitude: Double,
        category: DroneCategory,
        nowMillis: Long,
    ): Boolean = lastResult != null &&
        category == lastCategory &&
        abs(latitude - lastLatitude) < WEATHERKIT_GRID_DEGREES &&
        abs(longitude - lastLongitude) < WEATHERKIT_GRID_DEGREES &&
        nowMillis - lastFetchedAtElapsed in 0 until DUPLICATE_REQUEST_WINDOW_MS

    /** 수동 새로고침·카테고리 변경: 중복 요청 창을 비워 서버에 다시 묻는다. */
    fun invalidateCache() {
        lastFetchedAtElapsed = 0L
    }
}

/**
 * 중계 서버 응답 → 도메인 모델. 단위·구간 규칙은 iOS `WeatherManager.fetchWeatherData` 를 따른다.
 * - 풍속·돌풍 km/h → m/s, 시정 m → km
 * - 현재 강수는 precipitationIntensity(mm/h), 시간별 강수는 precipitationAmount(mm)
 * - 시간별은 서버가 준 정시-3시간부터 지금+72시간까지
 */
internal fun mapWeatherKitResponse(
    response: WeatherKitResponse,
    category: DroneCategory,
    nowMillis: Long,
    zone: ZoneId,
    smoothCurrentCri: (Double) -> Double = { it },
): WeatherData {
    val current = response.currentWeather?.let { current ->
        val temperature = current.temperature
        val dewPoint = current.temperatureDewPoint
        val windSpeed = (current.windSpeed ?: 0.0) / KMH_PER_MPS
        val windGust = current.windGust?.div(KMH_PER_MPS)
        val visibilityKm = current.visibility?.div(1000.0)
        val cri = if (temperature != null && dewPoint != null) {
            CRICalculator.calculateUnrounded(temperature, dewPoint, visibilityKm)
                .takeIf { it.isFinite() }
                ?.let(smoothCurrentCri)
        } else {
            null
        }
        CurrentWeatherData(
            temperature = temperature ?: 0.0,
            dewPoint = dewPoint ?: 0.0,
            windSpeed = windSpeed,
            windDirection = current.windDirection ?: 0.0,
            windGusts = windGust,
            precipitation = current.precipitationIntensity ?: 0.0,
            visibility = visibilityKm,
            condition = WeatherCondition.fromAppleCode(current.conditionCode),
            cri = cri,
            gustDifferenceLevel = GustDifferenceCalculator.evaluate(windSpeed, windGust, category),
        )
    }

    val horizon = nowMillis + WEATHER_FORECAST_HORIZON_MS
    val hourly = response.forecastHourly?.hours.orEmpty()
        .mapNotNull { hour -> hour.toHourlyWeatherData() }
        .filter { it.time <= horizon }
        .sortedBy { it.time }

    val days = response.forecastDaily?.days.orEmpty()
        .sortedBy { it.forecastStart }
        .take(WEATHER_SUN_EVENT_DAYS)
    val sunriseTimes = days.mapNotNull { it.sunrise.toLocalMinuteString(zone) }
    val sunsetTimes = days.mapNotNull { it.sunset.toLocalMinuteString(zone) }

    return WeatherData(
        current = current,
        hourlyForecast = hourly,
        sunrise = sunriseTimes.firstOrNull(),
        sunset = sunsetTimes.firstOrNull(),
        sunriseTimes = sunriseTimes,
        sunsetTimes = sunsetTimes,
        utcOffsetSeconds = zone.rules.getOffset(Instant.ofEpochMilli(nowMillis)).totalSeconds,
        fetchedAtMillis = response.fetchedAt.toEpochMillisOrNull(),
        isStale = response.stale == true,
        dataSourceAttributionUrl = response.currentWeather?.metadata?.attributionURL,
    )
}

private fun WeatherKitHour.toHourlyWeatherData(): HourlyWeatherData? {
    val time = forecastStart.toEpochMillisOrNull() ?: return null
    val windSpeed = (windSpeed ?: 0.0) / KMH_PER_MPS
    val windGust = windGust?.div(KMH_PER_MPS)
    val visibilityKm = visibility?.div(1000.0)
    val cri = if (temperature != null && temperatureDewPoint != null) {
        CRICalculator.calculate(temperature, temperatureDewPoint, visibilityKm).takeIf { it.isFinite() }
    } else {
        null
    }
    return HourlyWeatherData(
        time = time,
        temperature = temperature ?: 0.0,
        windSpeed = windSpeed,
        windDirection = windDirection ?: 0.0,
        windGusts = windGust,
        gustDifference = GustDifferenceCalculator.calculateObservedGustDifference(windSpeed, windGust),
        precipitation = precipitationAmount ?: 0.0,
        visibility = visibilityKm ?: 0.0,
        dewPoint = temperatureDewPoint ?: 0.0,
        cri = cri,
    )
}

private val LocalMinuteFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

/**
 * 일출·일몰 UTC 시각 → 기기 시간대의 "yyyy-MM-ddTHH:mm".
 * 타임라인·알림 예약은 이 현지 시각 문자열과 [WeatherData.utcOffsetSeconds] 를 함께 쓴다.
 */
private fun String?.toLocalMinuteString(zone: ZoneId): String? =
    this?.let { runCatching { Instant.parse(it).atZone(zone).format(LocalMinuteFormatter) }.getOrNull() }

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
