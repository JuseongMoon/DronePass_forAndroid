package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val current: CurrentWeather?,
    val hourly: HourlyWeather?,
    val daily: DailyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "dew_point_2m") val dewPoint: Double?,
    @Json(name = "wind_speed_10m") val windSpeed: Double?,
    @Json(name = "wind_direction_10m") val windDirection: Double?,
    @Json(name = "wind_gusts_10m") val windGusts: Double?,
    val precipitation: Double?,
    @Json(name = "weather_code") val weatherCode: Int?
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String>?,
    @Json(name = "temperature_2m") val temperature: List<Double>?,
    @Json(name = "dew_point_2m") val dewPoint: List<Double>?,
    @Json(name = "wind_speed_10m") val windSpeed: List<Double>?,
    @Json(name = "wind_direction_10m") val windDirection: List<Double>?,
    @Json(name = "wind_gusts_10m") val windGusts: List<Double>?,
    val precipitation: List<Double>?,
    val visibility: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>?,
    val sunrise: List<String>?,
    val sunset: List<String>?
)
