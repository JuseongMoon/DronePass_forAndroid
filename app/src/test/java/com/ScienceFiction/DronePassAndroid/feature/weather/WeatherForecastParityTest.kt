package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.preferencesOf
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceLevel
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageAnimationDurationMs
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageBackgroundAlpha
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageBottomPadding
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageCornerRadius
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageDurationMs
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageHorizontalPadding
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageVerticalPadding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WeatherForecastParityTest {

    @Test
    fun `forecast charts use iOS three day hourly data window from one hour before now`() {
        assertEquals(3, WeatherForecastDays)
        assertEquals(72, WeatherForecastChartHours)
        assertEquals(HourMs, WeatherForecastLookbackMs)
        assertEquals(72 * HourMs, WeatherForecastWindowMs)

        val hourly = (0 until 100).map { index ->
            hourlyWeather(time = index * HourMs, temperature = index.toDouble())
        }
        val nowMillis = 10 * HourMs + 42 * 60 * 1000L

        val chartHours = resolveWeatherForecastChartHours(hourly, nowMillis = nowMillis)

        assertEquals(73, chartHours.size)
        assertEquals(10 * HourMs, chartHours.first().time)
        assertEquals(82 * HourMs, chartHours.last().time)
    }

    @Test
    fun `forecast charts match iOS inclusive boundary when now is exactly on the hour`() {
        val hourly = (0 until 100).map { index ->
            hourlyWeather(time = index * HourMs, temperature = index.toDouble())
        }
        val nowMillis = 10 * HourMs

        val chartHours = resolveWeatherForecastChartHours(hourly, nowMillis = nowMillis)

        assertEquals(74, chartHours.size)
        assertEquals(9 * HourMs, chartHours.first().time)
        assertEquals(82 * HourMs, chartHours.last().time)
    }

    @Test
    fun `forecast charts are visible whenever iOS hourly forecast is not empty`() {
        assertFalse(shouldShowWeatherForecastCharts(emptyList()))
        assertTrue(shouldShowWeatherForecastCharts(listOf(hourlyWeather())))
    }

    @Test
    fun `forecast chart heights match iOS WeatherForecastView chart frames`() {
        assertEquals(250.dp, WeatherForecastChartHeight)
        assertEquals(WeatherForecastChartHeight, WeatherLineChartDefaultHeight)
        assertEquals(WeatherForecastChartHeight, WeatherPrecipitationChartHeight)
    }

    @Test
    fun `forecast chart card tokens match iOS WeatherForecastView`() {
        assertEquals(16.dp, IosWeatherForecastCardCornerRadius)
        assertEquals(16.dp, IosWeatherForecastCardPadding)
        assertEquals(12.dp, IosWeatherForecastCardSpacing)
        assertEquals(0xFFF2F2F7.toInt(), IosWeatherForecastCardContainerColor.toArgb())
        assertEquals(20.sp, IosCurrentWeatherHeaderTitleFontSize)
        assertEquals(15.sp, IosCurrentWeatherHeaderDroneWeightFontSize)
        assertEquals(FontWeight.SemiBold, IosCurrentWeatherHeaderTitleFontWeight)
        assertEquals(FontWeight.Normal, IosCurrentWeatherHeaderRegularFontWeight)
        assertEquals(8.dp, IosWeatherDroneCategoryButtonCornerRadius)
        assertEquals(10.dp, IosWeatherDroneCategoryButtonHorizontalPadding)
        assertEquals(6.dp, IosWeatherDroneCategoryButtonVerticalPadding)
        assertEquals(4.dp, IosWeatherDroneCategoryButtonSpacing)
        assertEquals(0.1f, IosWeatherDroneCategoryButtonBackgroundAlpha, 0f)
        assertEquals(12.dp, IosWeatherDroneCategoryLeadingIconSize)
        assertEquals(11.dp, IosWeatherDroneCategoryChevronIconSize)
        assertEquals(15.sp, IosWeatherDroneCategoryLabelFontSize)
        assertEquals(FontWeight.Medium, IosWeatherDroneCategoryLabelFontWeight)
        assertEquals(15.dp, IosWeatherDroneCategoryCheckmarkSize)
        assertEquals(11.sp, IosWeatherDroneCategoryExampleFontSize)
        assertEquals(200.dp, IosCurrentWeatherEmptyStateHeight)
        assertEquals(12.dp, IosCurrentWeatherPreviewCornerRadius)
        assertEquals(16.dp, IosCurrentWeatherPreviewPadding)
        assertEquals(64.dp, IosCurrentWeatherPreviewIconSize)
        assertEquals(94.dp, IosCurrentWeatherPreviewIconSlotWidth)
        assertEquals(0xFFFFFFFF.toInt(), IosCurrentWeatherPreviewContainerColor.toArgb())
        assertEquals(32.sp, IosCurrentWeatherPreviewTemperatureFontSize)
        assertEquals(20.sp, IosCurrentWeatherPreviewConditionFontSize)
        assertEquals(15.sp, IosCurrentWeatherPreviewTemperatureRangeFontSize)
        assertEquals(FontWeight.SemiBold, IosCurrentWeatherPreviewTemperatureFontWeight)
        assertEquals(FontWeight.Normal, IosCurrentWeatherPreviewRegularFontWeight)
        assertEquals(8.dp, IosCurrentWeatherPreviewTemperatureRangeSpacing)
        assertEquals(12.dp, IosWeatherDataCellCornerRadius)
        assertEquals(12.dp, IosWeatherDataCellHorizontalPadding)
        assertEquals(12.dp, IosWeatherDataCellVerticalPadding)
        assertEquals(62.dp, IosWeatherDataCellMinHeight)
        assertEquals(24.dp, IosWeatherDataCellIconSize)
        assertEquals(32.dp, IosWeatherDataCellIconSlotWidth)
        assertEquals(16.dp, IosWeatherDataCellWarningIconSize)
        assertEquals(0xFFFFFFFF.toInt(), IosWeatherDataCellContainerColor.toArgb())
        assertEquals(8.dp, IosWeatherDisclaimerTopPadding)
        assertEquals(12.dp, IosWeatherDisclaimerIconSize)
        assertEquals(4.dp, IosWeatherDisclaimerSpacing)
        assertEquals(1f, IosWeatherDisclaimerColorAlpha, 0f)
        assertEquals(16.dp, IosWeatherChartCardCornerRadius)
        assertEquals(16.dp, IosWeatherChartCardPadding)
        assertEquals(12.dp, IosWeatherChartCardSpacing)
        assertEquals(0xFFF2F2F7.toInt(), IosWeatherChartCardContainerColor.toArgb())
    }

    @Test
    fun `current weather data cell typography matches iOS weatherDataCard`() {
        assertEquals(17.sp, IosWeatherDataCellLabelFontSize)
        assertEquals(15.sp, IosWeatherDataCellLabelWithSubTextFontSize)
        assertEquals(20.sp, IosWeatherDataCellValueFontSize)
        assertEquals(17.sp, IosWeatherDataCellValueWithSubTextFontSize)
        assertEquals(11.sp, IosWeatherDataCellSubTextFontSize)
        assertEquals(FontWeight.SemiBold, IosWeatherDataCellHeadlineFontWeight)
        assertEquals(FontWeight.Normal, IosWeatherDataCellRegularFontWeight)
        assertEquals(FontWeight.SemiBold, IosWeatherDataCellValueFontWeight)
    }

    @Test
    fun `forecast refresh toast matches iOS ToastMessageModifier`() {
        assertEquals(2_000L, IosToastMessageDurationMs)
        assertEquals(300, IosToastMessageAnimationDurationMs)
        assertEquals(0.7f, IosToastMessageBackgroundAlpha, 0f)
        assertEquals(100.dp, IosToastMessageBottomPadding)
        assertEquals(12.dp, IosToastMessageCornerRadius)
        assertEquals(16.dp, IosToastMessageHorizontalPadding)
        assertEquals(10.dp, IosToastMessageVerticalPadding)
    }

    @Test
    fun `forecast refresh action stays enabled while loading like iOS toolbar`() {
        assertTrue(isWeatherRefreshActionEnabled(isLoading = false))
        assertTrue(isWeatherRefreshActionEnabled(isLoading = true))
    }

    @Test
    fun `weather sheet header matches iOS inline navigation toolbar`() {
        assertEquals(44.dp, WeatherSheetNavigationHeaderHeight)
        assertEquals(44.dp, WeatherSheetNavigationHeaderActionWidth)
        assertEquals(8.dp, WeatherSheetNavigationHeaderHorizontalPadding)
        assertEquals(0.5f, WeatherSheetNavigationHeaderDividerThickness.value, 0f)
    }

    @Test
    fun `last update time uses iOS localized short time style without forced seconds`() {
        val timestamp = 1_700_000_000_000L

        assertEquals(
            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(Date(timestamp)),
            formatWeatherLastUpdateTime(timestamp, Locale.US),
        )
        assertEquals(
            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.KOREA).format(Date(timestamp)),
            formatWeatherLastUpdateTime(timestamp, Locale.KOREA),
        )
    }

    @Test
    fun `forecast content keeps iOS card body during initial loading and error states`() {
        val existing = WeatherData(
            current = null,
            hourlyForecast = listOf(hourlyWeather()),
            sunrise = null,
            sunset = null,
        )

        assertEquals(existing, weatherForecastBodyData(existing, isLoading = false, hasError = false))
        assertNull(weatherForecastBodyData(null, isLoading = false, hasError = false))

        val loadingPlaceholder = weatherForecastBodyData(null, isLoading = true, hasError = false)!!
        assertNull(loadingPlaceholder.current)
        assertTrue(loadingPlaceholder.hourlyForecast.isEmpty())
        assertTrue(loadingPlaceholder.sunriseTimes.isEmpty())
        assertTrue(loadingPlaceholder.sunsetTimes.isEmpty())

        val errorPlaceholder = weatherForecastBodyData(null, isLoading = false, hasError = true)!!
        assertNull(errorPlaceholder.current)
        assertTrue(errorPlaceholder.hourlyForecast.isEmpty())
    }

    @Test
    fun `current weather reload indicator matches iOS refresh overlay boundary`() {
        assertEquals(8.dp, IosWeatherReloadingIndicatorCornerRadius)
        assertEquals(8.dp, IosWeatherReloadingIndicatorPadding)
        assertEquals(16.dp, IosWeatherReloadingIndicatorSize)
        assertEquals(2.dp, IosWeatherReloadingIndicatorStrokeWidth)
        assertEquals(0.9f, IosWeatherReloadingIndicatorBackgroundAlpha, 0f)
        assertFalse(shouldShowWeatherReloadingIndicator(isLoading = false, listOf(hourlyWeather())))
        assertFalse(shouldShowWeatherReloadingIndicator(isLoading = true, emptyList()))
        assertTrue(shouldShowWeatherReloadingIndicator(isLoading = true, listOf(hourlyWeather())))
    }

    @Test
    fun `current weather card empty forecast state matches iOS branch priority`() {
        assertEquals(
            CurrentWeatherContentState.Loading,
            resolveCurrentWeatherContentState(
                hourlyForecast = emptyList(),
                isLoading = true,
                hasError = false,
            ),
        )
        assertEquals(
            CurrentWeatherContentState.Loading,
            resolveCurrentWeatherContentState(
                hourlyForecast = emptyList(),
                isLoading = true,
                hasError = true,
            ),
        )
        assertEquals(
            CurrentWeatherContentState.Error,
            resolveCurrentWeatherContentState(
                hourlyForecast = emptyList(),
                isLoading = false,
                hasError = true,
            ),
        )
        assertEquals(
            CurrentWeatherContentState.Data,
            resolveCurrentWeatherContentState(
                hourlyForecast = emptyList(),
                isLoading = false,
                hasError = false,
            ),
        )
        assertEquals(
            CurrentWeatherContentState.Data,
            resolveCurrentWeatherContentState(
                hourlyForecast = listOf(hourlyWeather()),
                isLoading = true,
                hasError = true,
            ),
        )
    }

    @Test
    fun `forecast charts use iOS twelve hour visible domain with hourly labels`() {
        assertEquals(12 * HourMs, IosWeatherChartVisibleDomainMs)
        assertEquals(HourMs, IosWeatherChartXLabelIntervalMs)
        assertEquals(
            2.0,
            resolveScrollableTimeChartWidthScale(
                dataPoints = listOf(0L to 1.0, 24 * HourMs to 2.0),
                visibleDomainMs = IosWeatherChartVisibleDomainMs,
            ).toDouble(),
            0.0,
        )
        assertEquals(
            1.0,
            resolveScrollableTimeChartWidthScale(
                dataPoints = listOf(0L to 1.0, 6 * HourMs to 2.0),
                visibleDomainMs = IosWeatherChartVisibleDomainMs,
            ).toDouble(),
            0.0,
        )
    }

    @Test
    fun `forecast chart axis labels use iOS date label at midnight`() {
        val utc = TimeZone.getTimeZone("UTC")

        assertEquals(
            "01/02",
            formatIosTimeChartAxisLabel(
                timeMillis = 24 * HourMs,
                timeZone = utc,
                locale = Locale.US,
            ),
        )
        assertEquals(
            "13",
            formatIosTimeChartAxisLabel(
                timeMillis = 13 * HourMs,
                timeZone = utc,
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun `precipitation chart y axis keeps iOS minimum ten millimeter range`() {
        assertEquals(10.0, resolveIosPrecipitationYMax(emptyList()), 0.0)
        assertEquals(10.0, resolveIosPrecipitationYMax(listOf(0.0, 3.0)), 0.0)
        assertEquals(15.0, resolveIosPrecipitationYMax(listOf(12.1)), 0.0)
    }

    @Test
    fun `temperature chart y axis uses iOS five degree padded range`() {
        assertEquals(-30.0..50.0, resolveIosTemperatureYRange(emptyList()))
        assertEquals(5.0..30.0, resolveIosTemperatureYRange(listOf(10.1, 24.9)))
        assertEquals(-30.0..50.0, resolveIosTemperatureYRange(listOf(-100.0, 100.0)))
        assertEquals(-30.0..50.0, resolveIosTemperatureYRange(listOf(60.0)))
    }

    @Test
    fun `weather line charts use iOS PointMark colors`() {
        assertEquals(
            listOf(
                IosWeatherWindSpeedChartColor.toArgb(),
                IosWeatherGustDifferenceChartColor.toArgb(),
                IosWeatherPrecipitationChartColor.toArgb(),
                IosWeatherVisibilityChartColor.toArgb(),
                IosWeatherCriChartColor.toArgb(),
            ),
            listOf(
                0xFF4CAF50.toInt(),
                0xFF5856D6.toInt(),
                0xFF2196F3.toInt(),
                0xFF9C27B0.toInt(),
                0xFF00BCD4.toInt(),
            ),
        )
        assertEquals(
            List(3) { IosWeatherWindSpeedChartColor.toArgb() },
            weatherChartPointColors(3, IosWeatherWindSpeedChartColor).map { it.toArgb() },
        )
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
        assertEquals("-", formatNullableIosTemperatureDegrees(null))
    }

    @Test
    fun `current weather visibility uses iOS one decimal kilometer format`() {
        assertEquals("10.0 km", formatIosVisibilityKilometers(10.0))
        assertEquals("2.4 km", formatIosVisibilityKilometers(2.44))
        assertEquals("2.5 km", formatIosVisibilityKilometers(2.45))
        assertEquals("-", formatNullableIosVisibilityKilometers(null))
    }

    @Test
    fun `current weather visibility uses current conditions instead of first hourly forecast`() {
        val weatherData = WeatherData(
            current = currentWeather(visibility = 2.4),
            hourlyForecast = listOf(hourlyWeather(visibility = 10.0)),
            sunrise = null,
            sunset = null,
        )

        assertEquals(2.4, resolveCurrentWeatherVisibility(weatherData) ?: -1.0, 0.0)
    }

    @Test
    fun `current weather fallback strings match iOS manager computed values`() {
        assertEquals("-", MissingWeatherValueText)
        assertEquals("-", formatIosMetersPerSecond(null))
        assertEquals("5.2 m/s", formatIosMetersPerSecond(5.24))
        assertEquals("-", formatIosPrecipitationIntensity(null))
        assertEquals("1.2 mm/h", formatIosPrecipitationIntensity(1.24))
        assertEquals("0.1 cm/h", formatIosPrecipitationIntensity(1.24, isSnowing = true))
        assertEquals("-", formatIosCri(null))
        assertEquals("43", formatIosCri(42.6))
    }

    @Test
    fun `current precipitation label and unit switch to snowfall for iOS snow conditions`() {
        assertFalse(isSnowingWeatherCode(61))
        assertEquals(R.string.weather_precipitation, resolvePrecipitationLabelRes(61))

        assertTrue(isSnowingWeatherCode(71))
        assertTrue(isSnowingWeatherCode(86))
        assertEquals(R.string.weather_snowfall, resolvePrecipitationLabelRes(71))
    }

    @Test
    fun `missing current weather visibility keeps iOS moderate warning fallback`() {
        assertEquals(WarningIconType.Caution, resolveNullableVisibilityWarningIcon(null))
        assertEquals(WarningIconType.Warning, resolveNullableVisibilityWarningIcon(1.99))
        assertEquals(WarningIconType.None, resolveNullableVisibilityWarningIcon(10.0))
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
    fun `weather data cell treats blank subtext as present like iOS`() {
        assertFalse(weatherDataCellHasSubText(null))
        assertTrue(weatherDataCellHasSubText(""))
        assertTrue(weatherDataCellHasSubText(" "))
        assertTrue(weatherDataCellHasSubText("국지 돌풍"))
    }

    @Test
    fun `weather load failure keeps iOS localized description boundary`() {
        val noDetail = WeatherError.LoadFailed(null)
        assertEquals(R.string.weather_error_load_failed, noDetail.messageRes)
        assertNull(noDetail.formatArg)

        val emptyDetail = WeatherError.LoadFailed("")
        assertEquals(R.string.weather_error_load_failed_detail, emptyDetail.messageRes)
        assertEquals("", emptyDetail.formatArg)

        val blankDetail = WeatherError.LoadFailed("   ")
        assertEquals(R.string.weather_error_load_failed_detail, blankDetail.messageRes)
        assertEquals("   ", blankDetail.formatArg)

        val normalDetail = WeatherError.LoadFailed("network")
        assertEquals(R.string.weather_error_load_failed_detail, normalDetail.messageRes)
        assertEquals("network", normalDetail.formatArg)
    }

    @Test
    fun `weather charts use iOS drone category thresholds`() {
        assertEquals(7.0 to 9.0, iosWindSpeedThresholds(DroneCategory.TOY))
        assertEquals(12.0 to 15.0, iosWindSpeedThresholds(DroneCategory.CLASS2))
        assertEquals(6.0 to 8.5, iosGustDifferenceThresholds(DroneCategory.CLASS4))
        assertEquals(9.0 to 12.0, iosGustDifferenceThresholds(DroneCategory.CLASS2))
        assertEquals(-10.0, IosTemperatureLowCautionC, 0.0)
        assertEquals(35.0, IosTemperatureHighCautionC, 0.0)
        assertEquals(10.0, IosVisibilityGoodKm, 0.0)
        assertEquals(2.0, IosVisibilityPoorKm, 0.0)
        assertEquals(40.0, IosCriModerate, 0.0)
        assertEquals(70.0, IosCriHigh, 0.0)
        assertEquals(0.0..100.0, IosWeatherCriChartYRange)
    }

    @Test
    fun `weather chart threshold lines use iOS RuleMark colors`() {
        assertThresholdLines(
            temperatureChartThresholdLines(),
            values = listOf(-10.0, 35.0),
            colors = listOf(0xFF007AFF.toInt(), 0xFFFF9500.toInt()),
        )
        assertThresholdLines(
            windSpeedChartThresholdLines(DroneCategory.CLASS2),
            values = listOf(12.0, 15.0),
            colors = listOf(0xFFFF9500.toInt(), 0xFFFF3B30.toInt()),
        )
        assertThresholdLines(
            gustDifferenceChartThresholdLines(DroneCategory.CLASS4),
            values = listOf(6.0, 8.5),
            colors = listOf(0xFFFF9500.toInt(), 0xFFFF3B30.toInt()),
        )
        assertThresholdLines(
            visibilityChartThresholdLines(),
            values = listOf(2.0, 10.0),
            colors = listOf(0xFFFF3B30.toInt(), 0xFF34C759.toInt()),
        )
        assertThresholdLines(
            criChartThresholdLines(),
            values = listOf(40.0, 70.0),
            colors = listOf(0xFFFFCC00.toInt(), 0xFFFF3B30.toInt()),
        )
    }

    @Test
    fun `weather charts show iOS current time marker only inside the forecast range`() {
        val dataPoints = listOf(
            10_000L to 1.0,
            20_000L to 2.0,
            30_000L to 3.0,
        )

        assertEquals(20_000L, resolveWeatherChartCurrentTimeMarkerMs(dataPoints, 20_000L))
        assertNull(resolveWeatherChartCurrentTimeMarkerMs(dataPoints, 9_999L))
        assertNull(resolveWeatherChartCurrentTimeMarkerMs(dataPoints, 30_001L))
        assertNull(resolveWeatherChartCurrentTimeMarkerMs(emptyList(), 20_000L))

        assertTrue(shouldDrawWeatherCurrentMarker(20_000L, 10_000L, 30_000L))
        assertFalse(shouldDrawWeatherCurrentMarker(9_999L, 10_000L, 30_000L))
        assertFalse(shouldDrawWeatherCurrentMarker(null, 10_000L, 30_000L))
        assertEquals(12f, resolveWeatherChartTopPadding(false, false, 11f), 0f)
        assertEquals(23f, resolveWeatherChartTopPadding(false, true, 11f), 0f)
        assertEquals(23f, resolveWeatherChartTopPadding(true, false, 11f), 0f)
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

    @Test
    fun `weather category selection refreshes only after a coordinate baseline exists`() {
        assertFalse(shouldFetchWeatherAfterCategorySelection(latitude = 0.0, longitude = 0.0))
        assertTrue(shouldFetchWeatherAfterCategorySelection(latitude = 37.5665, longitude = 0.0))
        assertTrue(shouldFetchWeatherAfterCategorySelection(latitude = 0.0, longitude = 126.9780))
    }

    @Test
    fun `weather info guide category selection only updates the stored category like iOS WeatherInfoView`() {
        assertFalse(
            shouldFetchWeatherAfterCategorySelection(
                latitude = 37.5665,
                longitude = 126.9780,
                refreshWeather = false,
            ),
        )
    }

    @Test
    fun `weather location lookup does not fall back to Seoul when iOS has no location`() {
        assertEquals(
            WeatherLocationFetchSource.CurrentLocation,
            resolveWeatherLocationFetchSource(
                hasCurrentLocation = true,
                hasLastKnownLocation = false,
            ),
        )
        assertEquals(
            WeatherLocationFetchSource.CurrentLocation,
            resolveWeatherLocationFetchSource(
                hasCurrentLocation = true,
                hasLastKnownLocation = true,
            ),
        )
        assertEquals(
            WeatherLocationFetchSource.LastKnownLocation,
            resolveWeatherLocationFetchSource(
                hasCurrentLocation = false,
                hasLastKnownLocation = true,
            ),
        )
        assertEquals(
            WeatherLocationFetchSource.Unavailable,
            resolveWeatherLocationFetchSource(
                hasCurrentLocation = false,
                hasLastKnownLocation = false,
            ),
        )
    }

    private fun assertThresholdLines(
        thresholdLines: List<WeatherThresholdLine>,
        values: List<Double>,
        colors: List<Int>,
    ) {
        assertEquals(values, thresholdLines.map { it.value })
        assertEquals(colors, thresholdLines.map { it.color.toArgb() })
    }

    private fun hourlyWeather(
        time: Long = 0L,
        temperature: Double = 20.0,
        visibility: Double = 10.0,
    ): HourlyWeatherData = HourlyWeatherData(
        time = time,
        temperature = temperature,
        windSpeed = 1.0,
        windDirection = 0.0,
        windGusts = null,
        gustDifference = 0.0,
        precipitation = 0.0,
        visibility = visibility,
        dewPoint = 10.0,
        cri = 0.0,
    )

    private fun currentWeather(
        visibility: Double?,
    ): CurrentWeatherData = CurrentWeatherData(
        temperature = 20.0,
        dewPoint = 10.0,
        windSpeed = 1.0,
        windDirection = 0.0,
        windGusts = null,
        precipitation = 0.0,
        visibility = visibility,
        weatherCode = 0,
        cri = 0.0,
        gustDifferenceLevel = GustDifferenceLevel.SAFE,
    )

    private companion object {
        const val HourMs = 60 * 60 * 1000L
    }
}
