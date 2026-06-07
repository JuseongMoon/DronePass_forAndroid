package com.ScienceFiction.DronePassAndroid.feature.kp

import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
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
    fun `manual refresh matches iOS by refreshing NOAA forecast data only`() {
        listOf(false, true).forEach { hasCurrentKp ->
            val plan = resolveKpDataLoadPlan(
                trigger = KpDataLoadTrigger.UserRefresh,
                hasCurrentKp = hasCurrentKp,
            )

            assertEquals(false, plan.fetchCurrent)
            assertEquals(true, plan.fetchForecast)
            assertEquals(true, plan.fetchLongTermForecast)
        }
    }

    @Test
    fun `auto refresh matches iOS by refreshing NOAA forecast data only`() {
        listOf(false, true).forEach { hasCurrentKp ->
            val plan = resolveKpDataLoadPlan(
                trigger = KpDataLoadTrigger.AutoRefresh,
                hasCurrentKp = hasCurrentKp,
            )

            assertEquals(false, plan.fetchCurrent)
            assertEquals(true, plan.fetchForecast)
            assertEquals(true, plan.fetchLongTermForecast)
        }
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
