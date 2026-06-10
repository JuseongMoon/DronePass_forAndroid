package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.HourlyWeather
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherResponse
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun `hourly weather times use API utc offset instead of device timezone`() = runBlocking {
        val response = WeatherResponse(
            current = null,
            hourly = HourlyWeather(
                time = listOf("2026-01-01T00:00"),
                temperature = listOf(3.0),
                dewPoint = null,
                windSpeed = null,
                windDirection = null,
                windGusts = null,
                precipitation = null,
                visibility = null,
                weatherCode = null,
            ),
            daily = null,
            utcOffsetSeconds = 9 * 60 * 60,
        )
        val repository = WeatherRepository(FakeWeatherApi(response))

        val data = repository.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()

        assertEquals(
            LocalDateTime.parse("2026-01-01T00:00")
                .atOffset(ZoneOffset.ofHours(9))
                .toInstant()
                .toEpochMilli(),
            data.hourlyForecast.single().time,
        )
    }

    private class FakeWeatherApi(
        private val response: WeatherResponse,
    ) : WeatherApi {
        override suspend fun getWeather(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int,
            windSpeedUnit: String,
        ): WeatherResponse = response
    }
}
