package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.datastore.preferences.core.preferencesOf
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceLevel
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class WeatherForecastParityTest {

    @Test
    fun `forecast charts use iOS three day hourly data window from current hour`() {
        val hourly = (0 until 90).map { index ->
            hourlyWeather(time = index * HourMs, temperature = index.toDouble())
        }
        val nowMillis = 10 * HourMs + 42 * 60 * 1000L

        val chartHours = resolveWeatherForecastChartHours(hourly, nowMillis = nowMillis)

        assertEquals(72, chartHours.size)
        assertEquals(10 * HourMs, chartHours.first().time)
        assertEquals(81 * HourMs, chartHours.last().time)
    }

    @Test
    fun `forecast current hour start floors to the local hour`() {
        val nowMillis = 10 * HourMs + 42 * 60 * 1000L + 12_345L

        assertEquals(
            10 * HourMs,
            resolveCurrentWeatherForecastHourStartMillis(
                nowMillis = nowMillis,
                zoneId = ZoneOffset.UTC,
            ),
        )
    }

    @Test
    fun `forecast charts are visible whenever iOS hourly forecast is not empty`() {
        assertFalse(shouldShowWeatherForecastCharts(emptyList()))
        assertTrue(shouldShowWeatherForecastCharts(listOf(hourlyWeather())))
    }

    @Test
    fun `weather data source text opens the Android provider attribution URL`() {
        assertEquals("https://open-meteo.com/", WeatherDataSourceUrl)
    }

    @Test
    fun `current weather high and low use the full iOS forecast range`() {
        val hourly = listOf(
            hourlyWeather(time = 0L, temperature = 10.0),
            hourlyWeather(time = 24L, temperature = 12.0),
            hourlyWeather(time = 48L, temperature = -3.0),
            hourlyWeather(time = 71L, temperature = 31.0),
        )

        assertEquals(31.0 to -3.0, resolveForecastTemperatureRange(hourly))
    }

    @Test
    fun `current weather temperatures use iOS integer degree format`() {
        assertEquals("20°", formatIosTemperatureDegrees(20.4))
        assertEquals("21°", formatIosTemperatureDegrees(20.6))
        assertEquals("-3°", formatIosTemperatureDegrees(-3.4))
    }

    @Test
    fun `weather warning icons use iOS WeatherThresholds boundary rules`() {
        assertEquals(WarningIconType.None, resolveTemperatureWarningIcon(-10.0))
        assertEquals(WarningIconType.Caution, resolveTemperatureWarningIcon(-10.1))
        assertEquals(WarningIconType.None, resolveTemperatureWarningIcon(35.0))
        assertEquals(WarningIconType.Caution, resolveTemperatureWarningIcon(35.1))

        assertEquals(WarningIconType.None, resolvePrecipitationWarningIcon(0.0))
        assertEquals(WarningIconType.Caution, resolvePrecipitationWarningIcon(0.01))

        assertEquals(WarningIconType.Warning, resolveVisibilityWarningIcon(1.99))
        assertEquals(WarningIconType.Caution, resolveVisibilityWarningIcon(2.0))
        assertEquals(WarningIconType.Caution, resolveVisibilityWarningIcon(9.99))
        assertEquals(WarningIconType.None, resolveVisibilityWarningIcon(10.0))

        assertEquals(WarningIconType.None, resolveCriWarningIcon(39.99))
        assertEquals(WarningIconType.Caution, resolveCriWarningIcon(40.0))
        assertEquals(WarningIconType.Warning, resolveCriWarningIcon(70.0))
    }

    @Test
    fun `wind direction label uses iOS eight point localized compass boundaries`() {
        assertEquals(R.string.weather_direction_n, resolveWindDirectionLabelRes(0.0))
        assertEquals(R.string.weather_direction_n, resolveWindDirectionLabelRes(22.49))
        assertEquals(R.string.weather_direction_ne, resolveWindDirectionLabelRes(22.5))
        assertEquals(R.string.weather_direction_ne, resolveWindDirectionLabelRes(67.49))
        assertEquals(R.string.weather_direction_e, resolveWindDirectionLabelRes(67.5))
        assertEquals(R.string.weather_direction_se, resolveWindDirectionLabelRes(112.5))
        assertEquals(R.string.weather_direction_s, resolveWindDirectionLabelRes(157.5))
        assertEquals(R.string.weather_direction_sw, resolveWindDirectionLabelRes(202.5))
        assertEquals(R.string.weather_direction_w, resolveWindDirectionLabelRes(247.5))
        assertEquals(R.string.weather_direction_nw, resolveWindDirectionLabelRes(292.5))
        assertEquals(R.string.weather_direction_n, resolveWindDirectionLabelRes(337.5))
        assertEquals(R.string.weather_direction_n, resolveWindDirectionLabelRes(360.0))
    }

    @Test
    fun `localized gust warning shows iOS short subtext only for localized gust`() {
        assertNull(resolveGustDifferenceSubTextRes(GustDifferenceLevel.SAFE))
        assertEquals(R.string.weather_gust_warning, resolveGustDifferenceSubTextRes(GustDifferenceLevel.LOCALIZED_GUST))
        assertNull(resolveGustDifferenceSubTextRes(GustDifferenceLevel.CAUTION))
        assertNull(resolveGustDifferenceSubTextRes(GustDifferenceLevel.DANGER))
    }

    @Test
    fun `weather charts use iOS drone category thresholds`() {
        assertEquals(7.0 to 9.0, iosWindSpeedThresholds(DroneCategory.TOY))
        assertEquals(12.0 to 15.0, iosWindSpeedThresholds(DroneCategory.CLASS2))
        assertEquals(6.0 to 8.5, iosGustDifferenceThresholds(DroneCategory.CLASS4))
        assertEquals(9.0 to 12.0, iosGustDifferenceThresholds(DroneCategory.CLASS2))
        assertEquals(10.0, IosVisibilityGoodKm, 0.0)
        assertEquals(2.0, IosVisibilityPoorKm, 0.0)
        assertEquals(40.0, IosCriModerate, 0.0)
        assertEquals(70.0, IosCriHigh, 0.0)
    }

    @Test
    fun `weather drone category defaults to iOS class3`() {
        assertEquals(DroneCategory.CLASS3, DroneCategory.IosDefault)
        assertEquals(DroneCategory.CLASS3, DroneCategory.fromStoredValue("CLASS3"))
        assertEquals(DroneCategory.CLASS3, DroneCategory.fromStoredValue("2kg~7kg"))
        assertEquals(DroneCategory.TOY, DroneCategory.fromStoredValue("≤250g"))
        assertEquals(DroneCategory.TOY, DroneCategory.fromStoredValue("250g 이하"))
    }

    @Test
    fun `weather drone category text resources match iOS localized fields`() {
        assertEquals(R.string.weather_drone_category_toy, DroneCategory.TOY.labelRes)
        assertEquals(R.string.weather_drone_category_toy_description, DroneCategory.TOY.descriptionRes)
        assertEquals(R.string.weather_drone_category_toy_examples, DroneCategory.TOY.examplesRes)
        assertEquals(R.string.weather_drone_category_class4, DroneCategory.CLASS4.labelRes)
        assertEquals(R.string.weather_drone_category_class4_description, DroneCategory.CLASS4.descriptionRes)
        assertEquals(R.string.weather_drone_category_class4_examples, DroneCategory.CLASS4.examplesRes)
        assertEquals(R.string.weather_drone_category_class3, DroneCategory.CLASS3.labelRes)
        assertEquals(R.string.weather_drone_category_class3_description, DroneCategory.CLASS3.descriptionRes)
        assertEquals(R.string.weather_drone_category_class3_examples, DroneCategory.CLASS3.examplesRes)
        assertEquals(R.string.weather_drone_category_class2, DroneCategory.CLASS2.labelRes)
        assertEquals(R.string.weather_drone_category_class2_description, DroneCategory.CLASS2.descriptionRes)
        assertEquals(R.string.weather_drone_category_class2_examples, DroneCategory.CLASS2.examplesRes)
    }

    @Test
    fun `weather drone category preference uses iOS key with Android legacy fallback`() {
        assertEquals("selectedDroneCategory", WeatherDroneCategoryPreferenceKey.name)
        assertEquals("weather_drone_category", LegacyWeatherDroneCategoryPreferenceKey.name)
        assertEquals(
            DroneCategory.CLASS3,
            storedWeatherDroneCategory(preferencesOf()),
        )
        assertEquals(
            DroneCategory.CLASS2,
            storedWeatherDroneCategory(
                preferencesOf(WeatherDroneCategoryPreferenceKey to "7kg~25kg"),
            ),
        )
        assertEquals(
            DroneCategory.CLASS4,
            storedWeatherDroneCategory(
                preferencesOf(
                    WeatherDroneCategoryPreferenceKey to "invalid",
                    LegacyWeatherDroneCategoryPreferenceKey to "CLASS4",
                ),
            ),
        )
        assertEquals(
            DroneCategory.CLASS3,
            storedWeatherDroneCategory(
                preferencesOf(WeatherDroneCategoryPreferenceKey to "invalid"),
            ),
        )
    }

    private fun hourlyWeather(
        time: Long = 0L,
        temperature: Double = 20.0,
    ): HourlyWeatherData = HourlyWeatherData(
        time = time,
        temperature = temperature,
        windSpeed = 1.0,
        windDirection = 0.0,
        windGusts = null,
        gustDifference = 0.0,
        precipitation = 0.0,
        visibility = 10.0,
        dewPoint = 10.0,
        cri = 0.0,
    )

    private companion object {
        const val HourMs = 60 * 60 * 1000L
    }
}
