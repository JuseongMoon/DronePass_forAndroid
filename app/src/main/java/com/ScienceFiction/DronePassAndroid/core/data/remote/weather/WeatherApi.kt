package com.ScienceFiction.DronePassAndroid.core.data.remote.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,dew_point_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m,precipitation,visibility,weather_code",
        @Query("hourly") hourly: String = "temperature_2m,dew_point_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m,precipitation,visibility,weather_code",
        @Query("daily") daily: String = "sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 3,
        @Query("wind_speed_unit") windSpeedUnit: String = "ms"
    ): WeatherResponse
}
