package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherInfoTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InfoGuideSheetsTest {

    @Test
    fun `KP guide lists all storm levels in iOS order`() {
        val items = kpLevelGuideItems()

        assertEquals(
            listOf(KpLevel.NORMAL, KpLevel.G1, KpLevel.G2, KpLevel.G3, KpLevel.G4, KpLevel.G5),
            items.map { it.level },
        )
        assertEquals(R.string.kp_info_level_normal_name, items.first().nameRes)
        assertEquals(R.string.kp_info_level_normal_range, items.first().rangeRes)
        assertEquals(R.string.kp_info_level_g5_name, items.last().nameRes)
        assertEquals(R.string.kp_info_level_g5_advice, items.last().adviceRes)
    }

    @Test
    fun `KP guide highlights flight caution from G3 and above`() {
        assertFalse(isKpLevelAdviceDanger(KpLevel.NORMAL))
        assertFalse(isKpLevelAdviceDanger(KpLevel.G1))
        assertFalse(isKpLevelAdviceDanger(KpLevel.G2))
        assertTrue(isKpLevelAdviceDanger(KpLevel.G3))
        assertTrue(isKpLevelAdviceDanger(KpLevel.G4))
        assertTrue(isKpLevelAdviceDanger(KpLevel.G5))
    }

    @Test
    fun `KP guide level icons map to iOS KPLevel symbols`() {
        assertEquals(Icons.Default.CheckCircle, kpInfoLevelIcon(KpLevel.NORMAL))
        assertEquals(Icons.Default.Circle, kpInfoLevelIcon(KpLevel.G1))
        assertEquals(Icons.Default.Brightness6, kpInfoLevelIcon(KpLevel.G2))
        assertEquals(Icons.Default.Error, kpInfoLevelIcon(KpLevel.G3))
        assertEquals(Icons.Default.Warning, kpInfoLevelIcon(KpLevel.G4))
        assertEquals(Icons.Default.Block, kpInfoLevelIcon(KpLevel.G5))
    }

    @Test
    fun `KP guide overview keeps iOS source section order`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/InfoGuideSheets.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/InfoGuideSheets.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.kp_info_sources_title",
                "R.string.kp_info_sources_body",
                "R.string.kp_info_source_gfz",
                "R.string.kp_info_source_noaa",
                "R.string.kp_info_source_difference",
                "R.string.kp_info_source_reason_station",
                "R.string.kp_info_source_reason_interval",
                "R.string.kp_info_source_reason_forecast",
                "R.string.kp_info_source_note",
            ),
        )
    }

    @Test
    fun `weather guide lists all elements in iOS order`() {
        val items = weatherElementGuideItems(DroneCategory.CLASS3)

        assertEquals(
            listOf(
                WeatherInfoTopic.WindSpeed,
                WeatherInfoTopic.GustDifference,
                WeatherInfoTopic.Precipitation,
                WeatherInfoTopic.Visibility,
                WeatherInfoTopic.Temperature,
                WeatherInfoTopic.Cri,
            ),
            items.map { it.topic },
        )
        assertEquals(R.string.weather_info_wind_title, items.first().titleRes)
        assertEquals(R.string.weather_info_cri_note, items.last().noteRes)
    }

    @Test
    fun `weather guide initial scroll index targets tapped iOS element card`() {
        assertNull(weatherInfoScrollIndex(null))
        assertEquals(3, weatherInfoScrollIndex(WeatherInfoTopic.WindSpeed))
        assertEquals(4, weatherInfoScrollIndex(WeatherInfoTopic.GustDifference))
        assertEquals(5, weatherInfoScrollIndex(WeatherInfoTopic.Precipitation))
        assertEquals(6, weatherInfoScrollIndex(WeatherInfoTopic.Visibility))
        assertEquals(7, weatherInfoScrollIndex(WeatherInfoTopic.Temperature))
        assertEquals(8, weatherInfoScrollIndex(WeatherInfoTopic.Cri))
    }

    @Test
    fun `weather guide uses iOS wind thresholds by drone category`() {
        assertEquals(7.0 to 9.0, weatherGuideWindSpeedThresholds(DroneCategory.TOY))
        assertEquals(8.5 to 10.5, weatherGuideWindSpeedThresholds(DroneCategory.CLASS4))
        assertEquals(10.0 to 12.0, weatherGuideWindSpeedThresholds(DroneCategory.CLASS3))
        assertEquals(12.0 to 15.0, weatherGuideWindSpeedThresholds(DroneCategory.CLASS2))

        assertEquals(5.0 to 7.0, weatherGuideGustDifferenceThresholds(DroneCategory.TOY))
        assertEquals(6.0 to 8.5, weatherGuideGustDifferenceThresholds(DroneCategory.CLASS4))
        assertEquals(7.0 to 9.5, weatherGuideGustDifferenceThresholds(DroneCategory.CLASS3))
        assertEquals(9.0 to 12.0, weatherGuideGustDifferenceThresholds(DroneCategory.CLASS2))
    }

    @Test
    fun `weather guide location warning follows iOS GPS and accuracy guard`() {
        assertTrue(
            shouldShowWeatherLocationAccuracyWarning(
                isUsingGps = false,
                locationAccuracyMeters = 120.0,
            ),
        )
        assertFalse(
            shouldShowWeatherLocationAccuracyWarning(
                isUsingGps = true,
                locationAccuracyMeters = 120.0,
            ),
        )
        assertFalse(
            shouldShowWeatherLocationAccuracyWarning(
                isUsingGps = false,
                locationAccuracyMeters = null,
            ),
        )
    }

    @Test
    fun `weather guide category notes use iOS raw category values`() {
        val items = weatherElementGuideItems(DroneCategory.CLASS3)

        val wind = items.first { it.topic == WeatherInfoTopic.WindSpeed }
        val gust = items.first { it.topic == WeatherInfoTopic.GustDifference }

        assertEquals("2kg~7kg", wind.noteArgs.first())
        assertEquals("2kg~7kg", gust.noteArgs.first())
    }

    @Test
    fun `weather guide category menu marks selected category like iOS`() {
        assertTrue(
            shouldShowWeatherCategoryMenuCheckmark(
                entry = DroneCategory.CLASS3,
                selected = DroneCategory.CLASS3,
            ),
        )
        assertFalse(
            shouldShowWeatherCategoryMenuCheckmark(
                entry = DroneCategory.CLASS4,
                selected = DroneCategory.CLASS3,
            ),
        )
    }

    @Test
    fun `weather guide category menu uses iOS compact selector tokens`() {
        assertEquals(8.dp, WeatherGuideCategoryMenuHorizontalPadding)
        assertEquals(4.dp, WeatherGuideCategoryMenuVerticalPadding)
        assertEquals(12.dp, WeatherGuideCategoryMenuIconSize)
    }

    @Test
    fun `info guide header uses iOS inline title action slots`() {
        assertEquals(44.dp, InfoGuideHeaderHeight)
        assertEquals(72.dp, InfoGuideHeaderActionWidth)
        assertEquals(8.dp, InfoGuideHeaderHorizontalPadding)
        assertEquals(0.5f, InfoGuideHeaderDividerThickness.value, 0f)
    }

    @Test
    fun `info guide cards use iOS leading icon and note spacing tokens`() {
        assertEquals(50.dp, InfoGuideLeadingIconSlotWidth)
        assertEquals(30.dp, InfoGuideLeadingIconSize)
        assertEquals(8.dp, InfoGuideElementNoteTopPadding)
    }

    @Test
    fun `weather guide note parser matches iOS FormattedNoteText line rules`() {
        assertEquals(
            listOf(
                FormattedGuideNoteLine("plain text", FormattedGuideNoteLineStyle.Bullet),
                FormattedGuideNoteLine("", FormattedGuideNoteLineStyle.Blank),
                FormattedGuideNoteLine("existing bullet", FormattedGuideNoteLineStyle.Bullet),
                FormattedGuideNoteLine("① numbered header", FormattedGuideNoteLineStyle.Header),
                FormattedGuideNoteLine("⚠️ caution header", FormattedGuideNoteLineStyle.Header),
                FormattedGuideNoteLine("→ indented detail", FormattedGuideNoteLineStyle.Indented),
            ),
            parseFormattedGuideNoteLines(
                """
                plain text

                • existing bullet
                ① numbered header
                ⚠️ caution header
                → indented detail
                """.trimIndent(),
            ),
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in InfoGuideSheets.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
