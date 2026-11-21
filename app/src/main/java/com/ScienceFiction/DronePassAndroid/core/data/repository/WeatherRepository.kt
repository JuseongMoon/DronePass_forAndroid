package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitDataSource
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitHour
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitResponse
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.weatherGridCoordinate
import com.ScienceFiction.DronePassAndroid.core.util.CRICalculator
import com.ScienceFiction.DronePassAndroid.core.util.CurrentCriSmoother
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceCalculator
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** 타임머신 시간별 예보 캐시(D-59): 같은 격자에서 1시간. 실패해도 1시간 뒤 다시 묻는다(iOS 와 같음). */
internal const val TIME_MACHINE_WEATHER_CACHE_MS = 60L * 60L * 1000L

/** 타임머신 카드가 쓰는 한 시간 칸(정시 시작). 값은 iOS NotamHourWeather 와 같은 것만 둔다. */
data class TimeMachineHour(
    val startMillis: Long,
    val temperature: Double?,
    val windDirection: Double?,
    val condition: WeatherCondition?,
    /** mm — 그 시간 강수량. 시간별 예보는 iOS 처럼 강수량으로 아이콘을 고른다. */
    val precipitation: Double,
)

/** 타임머신 시간별 예보. [starts] 는 [hours] 의 시작 시각을 정렬해 둔 것(칸마다 이분 탐색). */
class TimeMachineWeather(
    val gridLatitude: Double,
    val gridLongitude: Double,
    val hours: List<TimeMachineHour>,
) {
    val starts: LongArray = LongArray(hours.size) { hours[it].startMillis }
}

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

    private val timeMachineMutex = Mutex()
    private val _timeMachineWeather = MutableStateFlow<TimeMachineWeather?>(null)
    /** 타임머신 시간별 예보(없으면 null — 카드 날씨 칸은 '–'). */
    val timeMachineWeather: StateFlow<TimeMachineWeather?> = _timeMachineWeather.asStateFlow()
    @Volatile private var timeMachineKey: Triple<Double, Double, Long>? = null

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
                // 샘플 시각은 이 요청을 넣는 순간의 시계(iOS CRISmoother 와 같음).
                smoothCurrentCri = { cri -> currentCriSmoother.smooth(cri, now) },
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

    /** 마지막 날씨 요청 좌표(카드 기준 위치). 타임머신 예보의 격자가 바뀌었는지만 본다. 없으면 null. */
    fun lastRequestedCoordinate(): Pair<Double, Double>? =
        if (lastResult != null && lastLatitude.isFinite() && lastLongitude.isFinite()) lastLatitude to lastLongitude else null

    /** 이 좌표로 타임머신 예보를 다시 받아야 하는지: 처음이거나, 격자가 바뀌었거나, 1시간이 지났을 때. */
    fun needsTimeMachineWeather(latitude: Double, longitude: Double, nowMillis: Long): Boolean {
        val key = timeMachineKey ?: return true
        return key.first != weatherGridCoordinate(latitude) ||
            key.second != weatherGridCoordinate(longitude) ||
            nowMillis - key.third !in 0 until TIME_MACHINE_WEATHER_CACHE_MS
    }

    /**
     * 타임머신 시간별 예보를 받는다(D-59·D-84). 기존 날씨 요청과 따로 보내고, 같은 격자에서 1시간 다시 묻지 않는다.
     * 서버가 아직 타임머신 요청을 모르면 기존 응답(+74시간)의 시간별만 쓴다(그 뒤 칸은 '–').
     */
    suspend fun fetchTimeMachineWeather(
        latitude: Double,
        longitude: Double,
        nowMillis: () -> Long = System::currentTimeMillis,
    ): Result<TimeMachineWeather?> = timeMachineMutex.withLock {
        val now = nowMillis()
        if (!needsTimeMachineWeather(latitude, longitude, now)) return@withLock Result.success(_timeMachineWeather.value)
        val gridLatitude = weatherGridCoordinate(latitude)
        val gridLongitude = weatherGridCoordinate(longitude)
        timeMachineKey = Triple(gridLatitude, gridLongitude, now)
        runCatching {
            val response = dataSource.fetchTimeMachine(latitude, longitude, ZoneId.systemDefault().id)
            TimeMachineWeather(
                gridLatitude = gridLatitude,
                gridLongitude = gridLongitude,
                hours = mapTimeMachineHours(response),
            ).also { _timeMachineWeather.value = it }
        }
    }

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
    val sunriseTimes = days.mapNotNull { it.sunrise.toLocalSecondString(zone) }
    val sunsetTimes = days.mapNotNull { it.sunset.toLocalSecondString(zone) }

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

/** 타임머신 시간별: 서버가 준 칸을 모두 쓴다(72시간으로 자르지 않는다). */
internal fun mapTimeMachineHours(response: WeatherKitResponse): List<TimeMachineHour> =
    response.forecastHourly?.hours.orEmpty()
        .mapNotNull { hour ->
            val start = hour.forecastStart.toEpochMillisOrNull() ?: return@mapNotNull null
            TimeMachineHour(
                startMillis = start,
                temperature = hour.temperature,
                windDirection = hour.windDirection,
                condition = WeatherCondition.fromAppleCode(hour.conditionCode),
                precipitation = hour.precipitationAmount ?: 0.0,
            )
        }
        .sortedBy { it.startMillis }
        .distinctBy { it.startMillis }

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

private val LocalSecondFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

/**
 * 일출·일몰 UTC 시각 → 기기 시간대의 "yyyy-MM-ddTHH:mm:ss".
 * 초까지 남겨 iOS 처럼 초 단위로 비교한다. 분으로 내리면 낮/밤이 최대 59초 일찍 바뀌었다.
 * 타임라인·알림 예약은 이 현지 시각 문자열과 [WeatherData.utcOffsetSeconds] 를 함께 쓴다.
 */
private fun String?.toLocalSecondString(zone: ZoneId): String? =
    this?.let { runCatching { Instant.parse(it).atZone(zone).format(LocalSecondFormatter) }.getOrNull() }

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
