package com.ScienceFiction.DronePassAndroid.feature.kp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastChartHeight
import com.ScienceFiction.DronePassAndroid.feature.weather.resolveTimeChartLabelTimes
import com.ScienceFiction.DronePassAndroid.feature.weather.shouldDrawWeatherLineAsSegments
import com.ScienceFiction.DronePassAndroid.feature.weather.weatherLineSegmentColor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class KpChartsTest {

    @Test
    fun `chart state follows iOS loading error empty data order`() {
        assertEquals(
            KpChartState.Loading,
            resolveKpChartState(isLoading = true, hasError = true, hasData = true),
        )
        assertEquals(
            KpChartState.Error,
            resolveKpChartState(isLoading = false, hasError = true, hasData = true),
        )
        assertEquals(
            KpChartState.Empty,
            resolveKpChartState(isLoading = false, hasError = false, hasData = false),
        )
        assertEquals(
            KpChartState.Data,
            resolveKpChartState(isLoading = false, hasError = false, hasData = true),
        )
    }

    @Test
    fun `data source links use the same official URLs as iOS Link views`() {
        assertEquals("https://kp.gfz.de/", kpDataSourceUrl(KpDataSource.GFZ_CURRENT))
        assertEquals(
            "https://www.swpc.noaa.gov/products/noaa-planetary-k-index-forecast",
            kpDataSourceUrl(KpDataSource.NOAA_48_HOUR),
        )
        assertEquals(
            "https://www.swpc.noaa.gov/products/27-day-outlook-107-cm-radio-flux-and-geomagnetic-indices",
            kpDataSourceUrl(KpDataSource.NOAA_27_DAY),
        )
    }

    @Test
    fun `initial load fetches current KP only when shared current KP is missing`() {
        assertEquals(
            true,
            resolveKpDataLoadPlan(
                trigger = KpDataLoadTrigger.Initial,
                hasCurrentKp = false,
            ).fetchCurrent,
        )
        assertEquals(
            false,
            resolveKpDataLoadPlan(
                trigger = KpDataLoadTrigger.Initial,
                hasCurrentKp = true,
            ).fetchCurrent,
        )
    }

    @Test
    fun `initial load uses iOS force refresh policy`() {
        assertEquals(
            true,
            resolveKpDataLoadPlan(
                trigger = KpDataLoadTrigger.Initial,
                hasCurrentKp = false,
            ).forceRefresh,
        )
    }

    @Test
    fun `manual refresh matches iOS by refreshing NOAA forecast data only`() {
        listOf(false, true).forEach { hasCurrentKp ->
            val plan = resolveKpDataLoadPlan(
                trigger = KpDataLoadTrigger.UserRefresh,
                hasCurrentKp = hasCurrentKp,
            )

            assertEquals(false, plan.fetchCurrent)
            assertEquals(true, plan.fetchForecast)
            assertEquals(true, plan.fetchLongTermForecast)
            assertEquals(true, plan.forceRefresh)
        }
    }

    @Test
    fun `refresh action stays enabled while loading like iOS toolbar`() {
        assertEquals(true, isKpRefreshActionEnabled(isLoading = false))
        assertEquals(true, isKpRefreshActionEnabled(isLoading = true))
    }

    @Test
    fun `toolbar does not show an extra loading indicator like iOS`() {
        assertEquals(false, KpToolbarShowsLoadingIndicator)
    }

    @Test
    fun `auto refresh matches iOS forecast screen by refreshing NOAA forecast data only`() {
        listOf(false, true).forEach { hasCurrentKp ->
            val plan = resolveKpDataLoadPlan(
                trigger = KpDataLoadTrigger.AutoRefresh,
                hasCurrentKp = hasCurrentKp,
            )

            assertEquals(false, plan.fetchCurrent)
            assertEquals(true, plan.fetchForecast)
            assertEquals(true, plan.fetchLongTermForecast)
            assertEquals(true, plan.forceRefresh)
        }
    }

    @Test
    fun `auto refresh interval matches iOS KP forecast five minute loop`() {
        assertEquals(5 * 60 * 1000L, KpAutoRefreshIntervalMs)
    }

    @Test
    fun `current KP card level name uses iOS localizedName resources`() {
        assertEquals(R.string.kp_info_level_normal_name, kpLevelNameRes(KpLevel.NORMAL))
        assertEquals(R.string.kp_info_level_g1_name, kpLevelNameRes(KpLevel.G1))
        assertEquals(R.string.kp_info_level_g2_name, kpLevelNameRes(KpLevel.G2))
        assertEquals(R.string.kp_info_level_g3_name, kpLevelNameRes(KpLevel.G3))
        assertEquals(R.string.kp_info_level_g4_name, kpLevelNameRes(KpLevel.G4))
        assertEquals(R.string.kp_info_level_g5_name, kpLevelNameRes(KpLevel.G5))
    }

    @Test
    fun `current KP card description uses iOS levelDescription resources`() {
        assertEquals(R.string.kp_level_normal_desc, kpLevelDescriptionRes(KpLevel.NORMAL))
        assertEquals(R.string.kp_level_g1_desc, kpLevelDescriptionRes(KpLevel.G1))
        assertEquals(R.string.kp_level_g2_desc, kpLevelDescriptionRes(KpLevel.G2))
        assertEquals(R.string.kp_level_g3_desc, kpLevelDescriptionRes(KpLevel.G3))
        assertEquals(R.string.kp_level_g4_desc, kpLevelDescriptionRes(KpLevel.G4))
        assertEquals(R.string.kp_level_g5_desc, kpLevelDescriptionRes(KpLevel.G5))
    }

    @Test
    fun `current KP value font size matches iOS currentKPCard`() {
        assertEquals(60, KpCurrentValueFontSizeSp)
    }

    @Test
    fun `KP forecast body and current card tokens match iOS KPForecastView`() {
        assertEquals(16.dp, IosKpForecastContentHorizontalPadding)
        assertEquals(16.dp, IosKpForecastContentVerticalPadding)
        assertEquals(20.dp, IosKpForecastContentSpacing)
        assertEquals(12.dp, IosCurrentKpSectionSpacing)
        assertEquals(16.dp, IosCurrentKpCardCornerRadius)
        assertEquals(16.dp, IosCurrentKpCardPadding)
        assertEquals(12.dp, IosCurrentKpCardHorizontalSpacing)
        assertEquals(10.dp, IosCurrentKpDetailLeadingPadding)
        assertEquals(8.dp, IosCurrentKpDetailSpacing)
        assertEquals(6.dp, IosCurrentKpLevelRowSpacing)
        assertEquals(20.dp, IosCurrentKpLevelIconSize)
        assertEquals(0.1f, IosCurrentKpCardBackgroundAlpha, 0f)
    }

    @Test
    fun `current KP card value keeps iOS hyphen fallback when data is missing`() {
        assertEquals("-", formatCurrentKpValue(null))
        assertEquals(
            "4.7",
            formatCurrentKpValue(
                KpIndexData(
                    timeTag = "2026-02-24 12:00:00",
                    kp = 4.7,
                ),
            ),
        )
    }

    @Test
    fun `KP forecast chart height matches iOS forecast chart frame`() {
        assertEquals(250.dp, KpForecastChartHeight)
    }

    @Test
    fun `KP chart point labels match iOS point annotations`() {
        assertEquals("0.0", formatKpChartPointLabel(0.0))
        assertEquals("4.7", formatKpChartPointLabel(4.74))
        assertEquals("4.8", formatKpChartPointLabel(4.75))
    }

    @Test
    fun `KP chart line segments use iOS KP level colors`() {
        val colors = kpChartPointColors(listOf(4.9, 5.0, 6.0, 7.0, 8.0, 9.0))

        assertEquals(
            listOf(
                KpLevel.NORMAL.color.toInt(),
                KpLevel.G1.color.toInt(),
                KpLevel.G2.color.toInt(),
                KpLevel.G3.color.toInt(),
                KpLevel.G4.color.toInt(),
                KpLevel.G5.color.toInt(),
            ),
            colors.map { it.toArgb() },
        )
        assertEquals(
            KpLevel.G2.color.toInt(),
            weatherLineSegmentColor(
                lineColor = Color.Black,
                lineSegmentColors = colors,
                segmentStartIndex = 2,
            ).toArgb(),
        )
        assertEquals(
            Color.Black.toArgb(),
            weatherLineSegmentColor(
                lineColor = Color.Black,
                lineSegmentColors = colors,
                segmentStartIndex = colors.size,
            ).toArgb(),
        )
        assertEquals(
            true,
            shouldDrawWeatherLineAsSegments(
                dataPointCount = colors.size,
                predicted = List(colors.size) { false },
                lineSegmentColors = colors,
            ),
        )
    }

    @Test
    fun `KP charts use iOS visible domains and source data label intervals`() {
        val hourMs = 60L * 60 * 1000

        assertEquals(24 * hourMs, KpForecastVisibleDomainMs)
        assertEquals(3 * hourMs, KpForecastLabelIntervalMs)
        assertEquals(786_240_000L, Kp27DayVisibleDomainMs)
        assertEquals(24 * hourMs, Kp27DayLabelIntervalMs)
        assertEquals(1.0, KpYAxisLabelStep, 0.0)
    }

    @Test
    fun `KP and weather forecast chart heights share the same iOS frame`() {
        assertEquals(WeatherForecastChartHeight, KpForecastChartHeight)
    }

    @Test
    fun `KP chart card tokens match iOS KPForecastView`() {
        assertEquals(16.dp, IosKpChartCardCornerRadius)
        assertEquals(16.dp, IosKpChartCardPadding)
        assertEquals(12.dp, IosKpChartCardSpacing)
        assertEquals(0xFFF2F2F7.toInt(), IosKpChartCardContainerColor.toArgb())
        assertEquals(200.dp, IosKpChartPlaceholderHeight)
        assertEquals(40.dp, IosKpChartErrorIconSize)
        assertEquals(8.dp, IosKpChartErrorSpacing)
        assertEquals(0xFFFF9500.toInt(), IosKpChartErrorIconColor.toArgb())
    }

    @Test
    fun `48 hour forecast time tags parse as UTC like iOS KPIndexData date`() {
        val expected = utcMillis(
            year = 2026,
            month = Calendar.FEBRUARY,
            day = 24,
            hour = 12,
        )

        assertEquals(expected, parseKpForecastUtcMillis("2026-02-24 12:00:00"))
        assertEquals(expected + 123, parseKpForecastUtcMillis("2026-02-24 12:00:00.123"))
        assertEquals(expected, parseKpForecastUtcMillis("2026-02-24T12:00:00"))
    }

    @Test
    fun `48 hour forecast uses iOS past six hours through future forty eight hours window`() {
        val now = utcMillis(
            year = 2026,
            month = Calendar.FEBRUARY,
            day = 24,
            hour = 12,
        )

        val filtered = filterKpNext48HoursForecast(
            forecastData = listOf(
                KpIndexData(timeTag = "2026-02-24 05:59:59", kp = 1.0, observed = "observed"),
                KpIndexData(timeTag = "2026-02-24 06:00:00", kp = 2.0, observed = "observed"),
                KpIndexData(timeTag = "2026-02-24 12:00:00", kp = 3.0, observed = "estimated"),
                KpIndexData(timeTag = "2026-02-26 12:00:00", kp = 4.0, observed = "predicted"),
                KpIndexData(timeTag = "2026-02-26 12:00:01", kp = 5.0, observed = "predicted"),
                KpIndexData(timeTag = "not-a-date", kp = 6.0, observed = "predicted"),
            ),
            nowMillis = now,
        )

        assertEquals(listOf(2.0, 3.0, 4.0), filtered.map { it.item.kp })
        assertEquals(listOf(false, false, true), filtered.map { it.isPredicted })
    }

    @Test
    fun `27 day forecast date parses as UTC noon like iOS decoder`() {
        val expected = utcMillis(
            year = 2026,
            month = Calendar.FEBRUARY,
            day = 25,
            hour = 12,
        )

        assertEquals(expected, parseKp27DayForecastNoonUtcMillis("2026 Feb 25"))
    }

    @Test
    fun `27 day current marker is shifted by twelve hours like iOS chart`() {
        assertEquals(43_201_000L, kp27DayCurrentMarkerMillis(nowMillis = 1_000L))
    }

    @Test
    fun `27 day forecast points use parsed UTC noon timestamps`() {
        val expectedTime = utcMillis(
            year = 2026,
            month = Calendar.FEBRUARY,
            day = 25,
            hour = 12,
        )

        assertEquals(
            listOf(expectedTime to 5.0),
            kp27DayLineChartPoints(
                listOf(
                    Kp27DayForecast(
                        date = "2026 Feb 25",
                        kp = 5.0,
                        ap = 20,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `KP x axis labels can use iOS source data timestamps instead of rounded intervals`() {
        val hourMs = 60L * 60 * 1000
        val sourceTimes = listOf(
            12 * hourMs,
            36 * hourMs,
            60 * hourMs,
        )

        assertEquals(
            sourceTimes,
            resolveTimeChartLabelTimes(
                dataStartMs = sourceTimes.first(),
                dataEndMs = sourceTimes.last(),
                intervalMs = Kp27DayLabelIntervalMs,
                explicitLabelTimesMs = sourceTimes,
            ),
        )
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
    ): Long {
        return GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
    }
}
