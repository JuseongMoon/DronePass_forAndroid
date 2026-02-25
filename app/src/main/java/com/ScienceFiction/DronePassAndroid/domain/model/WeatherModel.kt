package com.ScienceFiction.DronePassAndroid.domain.model

import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceLevel

data class WeatherData(
    val current: CurrentWeatherData?,
    val hourlyForecast: List<HourlyWeatherData>,
    val sunrise: String?,
    val sunset: String?
)

data class CurrentWeatherData(
    val temperature: Double,       // Celsius
    val dewPoint: Double,          // Celsius
    val windSpeed: Double,         // m/s
    val windDirection: Double,     // degrees
    val windGusts: Double?,        // m/s (nullable)
    val precipitation: Double,     // mm
    val weatherCode: Int,
    val cri: Double,               // Condensation Risk Index 0-100
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
    val cri: Double
)
