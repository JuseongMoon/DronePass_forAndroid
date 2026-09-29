package com.ScienceFiction.DronePassAndroid.domain.model

import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceLevel

data class WeatherData(
    val current: CurrentWeatherData?,
    val hourlyForecast: List<HourlyWeatherData>,
    val sunrise: String?,
    val sunset: String?,
    val sunriseTimes: List<String> = sunrise?.let(::listOf) ?: emptyList(),
    val sunsetTimes: List<String> = sunset?.let(::listOf) ?: emptyList(),
    val utcOffsetSeconds: Int? = null,
    /** 중계 서버가 Apple 응답을 받은 시각(epoch millis). 화면의 "마지막 업데이트"에 쓴다. */
    val fetchedAtMillis: Long? = null,
    /** 서버 상한 초과나 Apple 오류로 만료된 캐시를 받은 경우. */
    val isStale: Boolean = false,
    /** WeatherKit 원본 데이터 제공자 안내 페이지(metadata.attributionURL). */
    val dataSourceAttributionUrl: String? = null,
)

data class CurrentWeatherData(
    val temperature: Double,       // Celsius
    val dewPoint: Double,          // Celsius
    val windSpeed: Double,         // m/s
    val windDirection: Double,     // degrees
    val windGusts: Double?,        // m/s (nullable)
    val precipitation: Double,     // mm/h (WeatherKit precipitationIntensity)
    val visibility: Double?,       // km
    val condition: WeatherCondition?,
    val cri: Double?,              // Condensation Risk Index 1-100; null when inputs are missing
    val gustDifferenceLevel: GustDifferenceLevel
)

data class HourlyWeatherData(
    val time: Long,                // epoch millis
    val temperature: Double,
    val windSpeed: Double,
    val windDirection: Double,
    val windGusts: Double?,
    val gustDifference: Double,
    val precipitation: Double,
    val visibility: Double,        // km
    val dewPoint: Double,
    val cri: Double?
)
