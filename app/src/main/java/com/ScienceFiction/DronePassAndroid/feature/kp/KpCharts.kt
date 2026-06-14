package com.ScienceFiction.DronePassAndroid.feature.kp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.openUriSafely
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import com.ScienceFiction.DronePassAndroid.feature.weather.BackgroundZone
import com.ScienceFiction.DronePassAndroid.feature.weather.ScrollableTimeChartViewport
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherLineChart
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ─── 색상 상수 ─────────────────────────────────────────────
private val WarningYellow = Color(0xFFDAA520)
private val ZoneGreen = Color(0xFF4CAF50)
private val ZoneYellow = Color(0xFFFFC107)
private val ZoneRed = Color(0xFFF44336)
internal val KpForecastChartHeight = 250.dp
private const val KpForecastPastWindowMs = 6L * 60 * 60 * 1000
private const val KpForecastFutureWindowMs = 48L * 60 * 60 * 1000
internal const val KpForecastVisibleDomainMs = 24L * 60 * 60 * 1000
internal const val KpForecastLabelIntervalMs = 3L * 60 * 60 * 1000
internal const val Kp27DayVisibleDomainMs = 786_240L * 1000
internal const val Kp27DayLabelIntervalMs = 24L * 60 * 60 * 1000
internal const val KpYAxisLabelStep = 1.0

internal enum class KpDataSource {
    GFZ_CURRENT,
    NOAA_48_HOUR,
    NOAA_27_DAY,
}

internal fun kpDataSourceUrl(source: KpDataSource): String = when (source) {
    KpDataSource.GFZ_CURRENT -> "https://kp.gfz.de/"
    KpDataSource.NOAA_48_HOUR ->
        "https://www.swpc.noaa.gov/products/noaa-planetary-k-index-forecast"
    KpDataSource.NOAA_27_DAY ->
        "https://www.swpc.noaa.gov/products/27-day-outlook-107-cm-radio-flux-and-geomagnetic-indices"
}

// ─── 48시간 예보 라인 차트 (iOS forecastChart 정합) ──────────

@Composable
fun KpForecastLineChart(
    forecastData: List<KpIndexData>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(forecastData) { filterKpNext48HoursForecast(forecastData) }

    val dataPoints = parsed.map { it.timeMillis to it.item.kp }
    val predicted = parsed.map { it.isPredicted }
    val pointColors = parsed.map { Color(KpLevel.fromKp(it.item.kp).color.toInt()) }
    val xLabelTimes = parsed.map { it.timeMillis }
    val primaryColor = MaterialTheme.colorScheme.primary

    KpChartCard(
        sectionTitle = stringResource(R.string.kp_section_forecast48),
        noteBadge = stringResource(R.string.kp_forecast_note),
        dataSource = stringResource(R.string.kp_data_source_noaa),
        dataSourceType = KpDataSource.NOAA_48_HOUR,
        modifier = modifier,
    ) {
        when (resolveKpChartState(isLoading, errorMessage != null, parsed.isNotEmpty())) {
            KpChartState.Loading -> KpChartLoadingPlaceholder()
            KpChartState.Error -> KpChartErrorPlaceholder(errorMessage.orEmpty())
            KpChartState.Empty -> KpChartNoDataPlaceholder()
            KpChartState.Data -> ScrollableTimeChartViewport(
                dataPoints = dataPoints,
                visibleDomainMs = KpForecastVisibleDomainMs,
            ) { chartModifier ->
                WeatherLineChart(
                    dataPoints = dataPoints,
                    modifier = chartModifier,
                    lineColor = primaryColor,
                    warningThreshold = 5.0,
                    dangerThreshold = 7.0,
                    yAxisRange = 0.0..9.0,
                    yLabelStep = KpYAxisLabelStep,
                    xLabelIntervalMs = KpForecastLabelIntervalMs,
                    xLabelTimesMs = xLabelTimes,
                    currentTimeMs = System.currentTimeMillis(),
                    predicted = predicted,
                    pointColors = pointColors,
                    backgroundZones = listOf(
                        BackgroundZone(0.0..5.0, ZoneGreen),
                        BackgroundZone(5.0..7.0, ZoneYellow),
                        BackgroundZone(7.0..9.0, ZoneRed),
                    ),
                    formatValue = { it.toInt().toString() },
                    chartHeight = KpForecastChartHeight,
                )
            }
        }
    }
}

internal data class KpForecastChartPoint(
    val item: KpIndexData,
    val timeMillis: Long,
    val isPredicted: Boolean,
)

internal fun filterKpNext48HoursForecast(
    forecastData: List<KpIndexData>,
    nowMillis: Long = System.currentTimeMillis(),
): List<KpForecastChartPoint> {
    val pastThreshold = nowMillis - KpForecastPastWindowMs
    val futureThreshold = nowMillis + KpForecastFutureWindowMs
    return forecastData.mapNotNull { item ->
        val timeMillis = parseKpForecastUtcMillis(item.timeTag) ?: return@mapNotNull null
        if (timeMillis in pastThreshold..futureThreshold) {
            KpForecastChartPoint(
                item = item,
                timeMillis = timeMillis,
                isPredicted = item.observed == "predicted",
            )
        } else {
            null
        }
    }
}

internal fun parseKpForecastUtcMillis(timeTag: String): Long? {
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            val position = ParsePosition(0)
            val date = formatter.parse(timeTag, position)
            if (date != null && position.index == timeTag.length) date.time else null
        }.getOrNull()
    }
}

// ─── 27일 장기예보 라인 차트 (iOS longTermForecastChart 정합) ──

@Composable
fun Kp27DayChart(
    longTermForecast: List<Kp27DayForecast>,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    KpChartCard(
        sectionTitle = stringResource(R.string.kp_section_long_term),
        noteBadge = stringResource(R.string.kp_forecast27_note),
        dataSource = stringResource(R.string.kp_data_source_noaa),
        dataSourceType = KpDataSource.NOAA_27_DAY,
        modifier = modifier,
    ) {
        when (resolveKpChartState(isLoading, hasError = false, hasData = longTermForecast.isNotEmpty())) {
            KpChartState.Loading -> KpChartLoadingPlaceholder()
            KpChartState.Error -> KpChartErrorPlaceholder("")
            KpChartState.Empty -> KpChartNoDataPlaceholder()
            KpChartState.Data -> Kp27DayLineChart(longTermForecast = longTermForecast)
        }
    }
}

internal enum class KpChartState {
    Loading,
    Error,
    Empty,
    Data,
}

internal fun resolveKpChartState(
    isLoading: Boolean,
    hasError: Boolean,
    hasData: Boolean,
): KpChartState = when {
    isLoading -> KpChartState.Loading
    hasError -> KpChartState.Error
    !hasData -> KpChartState.Empty
    else -> KpChartState.Data
}

@Composable
private fun Kp27DayLineChart(longTermForecast: List<Kp27DayForecast>) {
    val parsedForecast = remember(longTermForecast) {
        longTermForecast.mapNotNull { forecast ->
            parseKp27DayForecastNoonUtcMillis(forecast.date)?.let { timeMs ->
                timeMs to forecast
            }
        }
    }
    val dataPoints = parsedForecast.map { (timeMs, forecast) -> timeMs to forecast.kp }
    val pointColors = parsedForecast.map { (_, forecast) -> Color(KpLevel.fromKp(forecast.kp).color.toInt()) }
    val xLabelTimes = parsedForecast.map { (timeMs, _) -> timeMs }
    val primaryColor = MaterialTheme.colorScheme.primary

    ScrollableTimeChartViewport(
        dataPoints = dataPoints,
        visibleDomainMs = Kp27DayVisibleDomainMs,
    ) { chartModifier ->
        WeatherLineChart(
            dataPoints = dataPoints,
            modifier = chartModifier,
            lineColor = primaryColor,
            warningThreshold = 5.0,
            dangerThreshold = 7.0,
            yAxisRange = 0.0..9.0,
            yLabelStep = KpYAxisLabelStep,
            xLabelIntervalMs = Kp27DayLabelIntervalMs,
            xLabelTimesMs = xLabelTimes,
            currentTimeMs = kp27DayCurrentMarkerMillis(),
            pointColors = pointColors,
            backgroundZones = listOf(
                BackgroundZone(0.0..5.0, ZoneGreen),
                BackgroundZone(5.0..7.0, ZoneYellow),
                BackgroundZone(7.0..9.0, ZoneRed),
            ),
            formatValue = { it.toInt().toString() },
            chartHeight = KpForecastChartHeight,
        )
    }
}

internal fun kp27DayLineChartPoints(longTermForecast: List<Kp27DayForecast>): List<Pair<Long, Double>> {
    return longTermForecast.mapNotNull { forecast ->
        parseKp27DayForecastNoonUtcMillis(forecast.date)?.let { it to forecast.kp }
    }
}

internal fun kp27DayCurrentMarkerMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    return nowMillis + 12L * 60 * 60 * 1000
}

internal fun parseKp27DayForecastNoonUtcMillis(dateStr: String): Long? {
    return runCatching {
        val sdf = SimpleDateFormat("yyyy MMM d", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        sdf.parse(dateStr)?.time?.plus(12L * 60 * 60 * 1000)
    }.getOrNull()
}

// ─── 공용: KP 차트 카드 (헤더 + 차트 + 범례 + 출처) ──────────

/**
 * iOS `forecastChart` / `longTermForecastChart` 의 카드 구조 정합:
 * 헤더(섹션 제목 + 선택적 배지) → 차트(content slot) → KP 6레벨 범례 → 데이터 출처.
 */
@Composable
private fun KpChartCard(
    sectionTitle: String,
    noteBadge: String?,
    dataSource: String,
    dataSourceType: KpDataSource,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: 제목 + 우측 배지 (iOS .title3 semibold + Capsule(.tertiarySystemBackground) note)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (noteBadge != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = noteBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Chart slot
            content()

            // Legend (6 KpLevel)
            KpLegend()

            // Data source (iOS Link 정합)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                KpDataSourceLink(label = dataSource, source = dataSourceType)
            }
        }
    }
}

@Composable
internal fun KpDataSourceLink(label: String, source: KpDataSource) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable {
            openUriSafely(uriHandler, kpDataSourceUrl(source))
        },
    )
}

@Composable
private fun KpChartLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun KpChartErrorPlaceholder(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = WarningYellow,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KpChartNoDataPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.kp_no_data),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KpLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(level = KpLevel.NORMAL, label = stringResource(R.string.kp_legend_normal))
            LegendItem(level = KpLevel.G1, label = stringResource(R.string.kp_legend_g1))
            LegendItem(level = KpLevel.G2, label = stringResource(R.string.kp_legend_g2))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(level = KpLevel.G3, label = stringResource(R.string.kp_legend_g3))
            LegendItem(level = KpLevel.G4, label = stringResource(R.string.kp_legend_g4))
            LegendItem(level = KpLevel.G5, label = stringResource(R.string.kp_legend_g5))
        }
    }
}

@Composable
private fun LegendItem(level: KpLevel, label: String) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(level.color.toInt())),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
