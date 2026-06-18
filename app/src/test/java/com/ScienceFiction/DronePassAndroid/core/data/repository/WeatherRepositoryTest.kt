package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.CurrentWeather
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.HourlyWeather
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherResponse
import com.ScienceFiction.DronePassAndroid.core.util.CRICalculator
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(9 * 60 * 60, data.utcOffsetSeconds)
    }

    @Test
    fun `current CRI uses iOS moving average while hourly forecast keeps raw rounded CRI`() = runBlocking {
        val firstCurrent = currentWeather(temperature = 20.0, dewPoint = 20.0, windSpeed = 0.0)
        val secondCurrent = currentWeather(temperature = 25.0, dewPoint = 5.0, windSpeed = 0.0)
        val secondHourly = hourlyWeather(temperature = 25.0, dewPoint = 5.0, windSpeed = 0.0)
        val api = FakeWeatherApi(weatherResponse(current = firstCurrent))
        val repository = WeatherRepository(api)

        val first = repository.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()
        assertEquals(CRICalculator.calculate(20.0, 20.0, 0.0), first.current?.cri ?: -1.0, 0.0)

        repository.invalidateCache()
        api.response = weatherResponse(current = secondCurrent, hourly = secondHourly)
        val second = repository.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()

        val expectedCurrent = (
            CRICalculator.calculateUnrounded(20.0, 20.0, 0.0) +
                CRICalculator.calculateUnrounded(25.0, 5.0, 0.0)
            ).div(2.0).roundToInt().toDouble()
        assertEquals(expectedCurrent, second.current?.cri ?: -1.0, 0.0)
        assertEquals(
            CRICalculator.calculate(25.0, 5.0, 0.0),
            second.hourlyForecast.single().cri,
            0.0,
        )
    }

    @Test
    fun `hourly gust difference uses iOS observed gust fallback without estimated gust`() = runBlocking {
        val withoutObservedGust = WeatherRepository(
            FakeWeatherApi(
                weatherResponse(
                    hourly = hourlyWeather(
                        windSpeed = 5.0,
                        windGusts = null,
                    ),
                ),
            ),
        )

        val noGustData = withoutObservedGust.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()
        assertEquals(0.0, noGustData.hourlyForecast.single().gustDifference, 0.0)

        val withObservedGusts = WeatherRepository(
            FakeWeatherApi(
                weatherResponse(
                    hourly = HourlyWeather(
                        time = listOf("2026-01-01T00:00", "2026-01-01T01:00"),
                        temperature = listOf(20.0, 20.0),
                        dewPoint = listOf(10.0, 10.0),
                        windSpeed = listOf(5.0, 5.0),
                        windDirection = listOf(0.0, 0.0),
                        windGusts = listOf(4.0, 7.0),
                        precipitation = listOf(0.0, 0.0),
                        visibility = listOf(10000.0, 10000.0),
                        weatherCode = listOf(0, 0),
                    ),
                ),
            ),
        )
        val observedData = withObservedGusts.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()

        assertEquals(0.0, observedData.hourlyForecast[0].gustDifference, 0.0)
        assertEquals(2.0, observedData.hourlyForecast[1].gustDifference, 0.0)
    }

    @Test
    fun `current visibility is requested and mapped like iOS current weather visibility`() = runBlocking {
        val api = FakeWeatherApi(
            weatherResponse(
                current = currentWeather(
                    temperature = 20.0,
                    dewPoint = 10.0,
                    windSpeed = 1.0,
                    visibility = 2400.0,
                ),
            ),
        )
        val repository = WeatherRepository(api)

        val data = repository.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()

        assertTrue(api.lastCurrentQuery?.contains("visibility") == true)
        assertEquals(2.4, data.current?.visibility ?: -1.0, 0.0)
    }

    private fun weatherResponse(
        current: CurrentWeather? = null,
        hourly: HourlyWeather? = null,
    ): WeatherResponse = WeatherResponse(
        current = current,
        hourly = hourly,
        daily = null,
        utcOffsetSeconds = 9 * 60 * 60,
    )

    private fun currentWeather(
        temperature: Double,
        dewPoint: Double,
        windSpeed: Double,
        visibility: Double? = 10000.0,
    ): CurrentWeather = CurrentWeather(
        temperature = temperature,
        dewPoint = dewPoint,
        windSpeed = windSpeed,
        windDirection = 0.0,
        windGusts = null,
        precipitation = 0.0,
        visibility = visibility,
        weatherCode = 0,
    )

    private fun hourlyWeather(
        temperature: Double = 20.0,
        dewPoint: Double = 10.0,
        windSpeed: Double = 0.0,
        windGusts: List<Double>? = null,
    ): HourlyWeather = HourlyWeather(
        time = listOf("2026-01-01T00:00"),
        temperature = listOf(temperature),
        dewPoint = listOf(dewPoint),
        windSpeed = listOf(windSpeed),
        windDirection = listOf(0.0),
        windGusts = windGusts,
        precipitation = listOf(0.0),
        visibility = listOf(10000.0),
        weatherCode = listOf(0),
    )

    private class FakeWeatherApi(
        var response: WeatherResponse,
    ) : WeatherApi {
        var lastCurrentQuery: String? = null

        override suspend fun getWeather(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int,
            windSpeedUnit: String,
        ): WeatherResponse {
            lastCurrentQuery = current
            return response
        }
    }
}
