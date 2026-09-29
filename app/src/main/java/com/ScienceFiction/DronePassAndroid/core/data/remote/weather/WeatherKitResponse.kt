package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import com.squareup.moshi.JsonClass

/**
 * `getAndroidWeather` callable 응답.
 *
 * Apple WeatherKit REST 의 `currentWeather`, `forecastHourly`, `forecastDaily` 를 그대로 두고
 * 중계 서버가 [fetchedAt]·[expiresAt]·격자 좌표·[stale] 을 덧붙인다.
 * 단위는 Apple 원본 그대로다: 기온 °C, windSpeed·windGust km/h, visibility m.
 * 앱에서 쓰지 않는 필드는 선언하지 않는다(Moshi 는 모르는 필드를 무시한다).
 */
@JsonClass(generateAdapter = true)
data class WeatherKitResponse(
    val currentWeather: WeatherKitCurrent? = null,
    val forecastHourly: WeatherKitHourlyForecast? = null,
    val forecastDaily: WeatherKitDailyForecast? = null,
    val fetchedAt: String? = null,
    val expiresAt: String? = null,
    val gridLatitude: Double? = null,
    val gridLongitude: Double? = null,
    /** 일일 상한이나 Apple 오류로 만료된 캐시를 돌려줄 때만 true. */
    val stale: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class WeatherKitMetadata(
    val attributionURL: String? = null,
    val expireTime: String? = null,
)

@JsonClass(generateAdapter = true)
data class WeatherKitCurrent(
    val metadata: WeatherKitMetadata? = null,
    val asOf: String? = null,
    val conditionCode: String? = null,
    val temperature: Double? = null,
    val temperatureDewPoint: Double? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null,
    val windGust: Double? = null,
    val visibility: Double? = null,
    /** mm/h — 지금 내리는 강수 강도. iOS precipitationIntensity 와 같은 값. */
    val precipitationIntensity: Double? = null,
)

@JsonClass(generateAdapter = true)
data class WeatherKitHourlyForecast(
    val hours: List<WeatherKitHour> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WeatherKitHour(
    val forecastStart: String,
    val conditionCode: String? = null,
    val temperature: Double? = null,
    val temperatureDewPoint: Double? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null,
    val windGust: Double? = null,
    val visibility: Double? = null,
    /** mm — 해당 시간의 강수량. iOS 그래프의 precipitationAmount 와 같은 값. */
    val precipitationAmount: Double? = null,
)

@JsonClass(generateAdapter = true)
data class WeatherKitDailyForecast(
    val days: List<WeatherKitDay> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WeatherKitDay(
    val forecastStart: String,
    val sunrise: String? = null,
    val sunset: String? = null,
)
