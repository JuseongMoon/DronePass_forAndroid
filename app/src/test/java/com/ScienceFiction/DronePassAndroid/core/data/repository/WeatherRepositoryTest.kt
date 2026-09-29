package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitCurrent
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitDataSource
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitHour
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitHourlyForecast
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherKitResponse
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherServiceException
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherServiceFailure
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.parseWeatherKitResponse
import com.ScienceFiction.DronePassAndroid.core.util.CRICalculator
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

class WeatherRepositoryTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun fixture(): WeatherKitResponse {
        val json = requireNotNull(javaClass.classLoader?.getResource("weatherkit/android-weather-response.json"))
            .readText()
        return parseWeatherKitResponse(json, moshi)
    }

    private fun fixtureNowMillis(): Long = Instant.parse("2026-09-29T02:43:39.868Z").toEpochMilli()

    @Test
    fun `fixture parses every WeatherKit bundle and relay field`() {
        val response = fixture()

        assertEquals(77, response.forecastHourly?.hours?.size)
        assertEquals(10, response.forecastDaily?.days?.size)
        assertEquals("2026-09-29T02:43:39.868Z", response.fetchedAt)
        assertEquals(37.57, response.gridLatitude ?: 0.0, 0.0)
        assertNull(response.stale)
        assertEquals(
            "https://developer.apple.com/weatherkit/data-source-attribution/",
            response.currentWeather?.metadata?.attributionURL,
        )
    }

    @Test
    fun `fixture maps units like iOS WeatherManager`() {
        val data = mapWeatherKitResponse(
            response = fixture(),
            category = DroneCategory.IosDefault,
            nowMillis = fixtureNowMillis(),
            zone = seoul,
        )
        val current = requireNotNull(data.current)

        // km/h → m/s, m → km
        assertEquals(1.06 / 3.6, current.windSpeed, 1e-9)
        assertEquals(10.04 / 3.6, current.windGusts ?: -1.0, 1e-9)
        assertEquals(24.30062, current.visibility ?: -1.0, 1e-9)
        assertEquals(23.16, current.temperature, 0.0)
        assertEquals(13.63, current.dewPoint, 0.0)
        assertEquals(WeatherCondition.CLEAR, current.condition)
        assertEquals(Instant.parse("2026-09-29T02:43:39.868Z").toEpochMilli(), data.fetchedAtMillis)
        assertFalse(data.isStale)

        val firstHour = data.hourlyForecast.first()
        assertEquals(Instant.parse("2026-09-28T23:00:00Z").toEpochMilli(), firstHour.time)
        assertEquals(3.17 / 3.6, firstHour.windSpeed, 1e-9)
        assertEquals(20.654, firstHour.visibility, 1e-9)
        assertEquals(13.16, firstHour.dewPoint, 0.0)
    }

    @Test
    fun `hourly forecast keeps past three hours and stops at now plus 72 hours`() {
        val now = fixtureNowMillis()
        val data = mapWeatherKitResponse(fixture(), DroneCategory.IosDefault, now, seoul)

        val hours = data.hourlyForecast.map { it.time }
        assertEquals(hours.sorted(), hours)
        // 서버는 정시-3시간부터 준다(02:00 UTC 정시 → 23:00).
        assertEquals(Instant.parse("2026-09-28T23:00:00Z").toEpochMilli(), hours.first())
        assertTrue(hours.last() <= now + WEATHER_FORECAST_HORIZON_MS)
        assertEquals(Instant.parse("2026-10-02T02:00:00Z").toEpochMilli(), hours.last())
    }

    @Test
    fun `sunrise and sunset become local minute strings for today tomorrow and the day after`() {
        val data = mapWeatherKitResponse(fixture(), DroneCategory.IosDefault, fixtureNowMillis(), seoul)

        assertEquals(WEATHER_SUN_EVENT_DAYS, data.sunriseTimes.size)
        // 2026-09-28T21:25:27Z → 서울 2026-09-29 06:25
        assertEquals("2026-09-29T06:25", data.sunrise)
        // 2026-09-29T09:19:01Z → 서울 18:19
        assertEquals("2026-09-29T18:19", data.sunset)
        assertEquals(9 * 60 * 60, data.utcOffsetSeconds)
    }

    @Test
    fun `stale relay response and unknown condition code are surfaced`() {
        val response = WeatherKitResponse(
            currentWeather = current(conditionCode = "SomethingNew"),
            stale = true,
        )
        val data = mapWeatherKitResponse(response, DroneCategory.IosDefault, 0L, seoul)

        assertTrue(data.isStale)
        assertNull(data.current?.condition)
    }

    @Test
    fun `current CRI uses iOS moving average while hourly forecast keeps raw rounded CRI`() = runBlocking {
        val dataSource = FakeDataSource(
            WeatherKitResponse(currentWeather = current(temperature = 20.0, dewPoint = 20.0)),
        )
        val repository = WeatherRepository(dataSource)

        val first = repository.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()
        assertEquals(CRICalculator.calculate(20.0, 20.0, 10.0), first.current?.cri ?: -1.0, 0.0)

        repository.invalidateCache()
        dataSource.response = WeatherKitResponse(
            currentWeather = current(temperature = 25.0, dewPoint = 5.0),
            forecastHourly = WeatherKitHourlyForecast(listOf(hour(temperature = 25.0, dewPoint = 5.0))),
        )
        val second = repository.fetchWeather(latitude = 37.0, longitude = 127.0).getOrThrow()

        val expectedCurrent = (
            CRICalculator.calculateUnrounded(20.0, 20.0, 10.0) +
                CRICalculator.calculateUnrounded(25.0, 5.0, 10.0)
            ).div(2.0).roundToInt().toDouble()
        assertEquals(expectedCurrent, second.current?.cri ?: -1.0, 0.0)
        assertEquals(CRICalculator.calculate(25.0, 5.0, 10.0), second.hourlyForecast.single().cri ?: -1.0, 0.0)
    }

    @Test
    fun `hourly gust difference uses observed gust in meters per second`() {
        val response = WeatherKitResponse(
            forecastHourly = WeatherKitHourlyForecast(
                listOf(
                    hour(start = "2026-01-01T00:00:00Z", windSpeedKmh = 18.0, windGustKmh = null),
                    hour(start = "2026-01-01T01:00:00Z", windSpeedKmh = 18.0, windGustKmh = 14.4),
                    hour(start = "2026-01-01T02:00:00Z", windSpeedKmh = 18.0, windGustKmh = 25.2),
                ),
            ),
        )
        val data = mapWeatherKitResponse(
            response,
            DroneCategory.IosDefault,
            Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(),
            seoul,
        )

        assertEquals(0.0, data.hourlyForecast[0].gustDifference, 1e-9)
        assertEquals(0.0, data.hourlyForecast[1].gustDifference, 1e-9)
        assertEquals(2.0, data.hourlyForecast[2].gustDifference, 1e-9)
    }

    @Test
    fun `current and hourly CRI use visibility in kilometers for fog adjustment`() {
        val response = WeatherKitResponse(
            currentWeather = current(temperature = 25.0, dewPoint = 22.5, visibilityMeters = 500.0),
            forecastHourly = WeatherKitHourlyForecast(
                listOf(hour(temperature = 25.0, dewPoint = 22.5, visibilityMeters = 500.0)),
            ),
        )
        val data = mapWeatherKitResponse(response, DroneCategory.IosDefault, 0L, seoul)

        assertEquals(90.0, data.current?.cri ?: -1.0, 0.0)
        assertEquals(90.0, data.hourlyForecast.single().cri ?: -1.0, 0.0)
    }

    @Test
    fun `missing temperature or dew point leaves CRI absent instead of creating a false warning`() {
        val response = WeatherKitResponse(
            currentWeather = current(temperature = -5.0, dewPoint = null),
            forecastHourly = WeatherKitHourlyForecast(listOf(hour(temperature = -5.0, dewPoint = null))),
        )
        val data = mapWeatherKitResponse(response, DroneCategory.IosDefault, 0L, seoul)

        assertNull(data.current?.cri)
        assertNull(data.hourlyForecast.single().cri)
    }

    @Test
    fun `resume and timer requests within a few seconds reach the server once`() = runBlocking {
        val dataSource = FakeDataSource(WeatherKitResponse(currentWeather = current()))
        val repository = WeatherRepository(dataSource)
        var now = 1_000_000L

        repository.fetchWeather(37.0, 127.0, nowMillis = { now }).getOrThrow()
        now += 2_000L
        repository.fetchWeather(37.001, 127.001, nowMillis = { now }).getOrThrow()
        assertEquals(1, dataSource.calls)

        // 수동 새로고침은 항상 서버에 다시 묻는다.
        repository.invalidateCache()
        repository.fetchWeather(37.0, 127.0, nowMillis = { now }).getOrThrow()
        assertEquals(2, dataSource.calls)

        // 다른 격자로 이동하면 바로 다시 묻는다.
        repository.fetchWeather(37.05, 127.0, nowMillis = { now }).getOrThrow()
        assertEquals(3, dataSource.calls)
    }

    @Test
    fun `relay failures surface as failed results`() = runBlocking {
        val repository = WeatherRepository(
            object : WeatherKitDataSource {
                override suspend fun fetch(latitude: Double, longitude: Double, timezone: String): WeatherKitResponse =
                    throw WeatherServiceException(WeatherServiceFailure.UNAVAILABLE)
            },
        )

        val failure = repository.fetchWeather(37.0, 127.0).exceptionOrNull()
        assertEquals(WeatherServiceFailure.UNAVAILABLE, (failure as? WeatherServiceException)?.failure)
    }

    private class FakeDataSource(var response: WeatherKitResponse) : WeatherKitDataSource {
        var calls = 0
        override suspend fun fetch(latitude: Double, longitude: Double, timezone: String): WeatherKitResponse {
            calls += 1
            return response
        }
    }

    private fun current(
        temperature: Double? = 20.0,
        dewPoint: Double? = 10.0,
        windSpeedKmh: Double = 0.0,
        visibilityMeters: Double = 10_000.0,
        conditionCode: String = "Clear",
    ) = WeatherKitCurrent(
        conditionCode = conditionCode,
        temperature = temperature,
        temperatureDewPoint = dewPoint,
        windSpeed = windSpeedKmh,
        windDirection = 0.0,
        visibility = visibilityMeters,
        precipitationIntensity = 0.0,
    )

    private fun hour(
        start: String = "1970-01-01T00:00:00Z",
        temperature: Double? = 20.0,
        dewPoint: Double? = 10.0,
        windSpeedKmh: Double = 0.0,
        windGustKmh: Double? = null,
        visibilityMeters: Double = 10_000.0,
    ) = WeatherKitHour(
        forecastStart = start,
        conditionCode = "Clear",
        temperature = temperature,
        temperatureDewPoint = dewPoint,
        windSpeed = windSpeedKmh,
        windDirection = 0.0,
        windGust = windGustKmh,
        visibility = visibilityMeters,
        precipitationAmount = 0.0,
    )
}
