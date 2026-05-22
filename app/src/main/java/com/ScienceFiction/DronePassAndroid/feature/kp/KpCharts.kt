package com.ScienceFiction.DronePassAndroid.feature.kp

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import com.ScienceFiction.DronePassAndroid.feature.weather.BackgroundZone
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── 색상 상수 ─────────────────────────────────────────────
private val WarningYellow = Color(0xFFDAA520)
private val ZoneGreen = Color(0xFF4CAF50)
private val ZoneYellow = Color(0xFFFFC107)
private val ZoneRed = Color(0xFFF44336)

// ─── 48시간 예보 라인 차트 (iOS forecastChart 정합) ──────────

@Composable
fun KpForecastLineChart(
    forecastData: List<KpIndexData>,
    modifier: Modifier = Modifier,
) {
    if (forecastData.isEmpty()) return

    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }
    val spaceSdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val parsed = remember(forecastData) {
        forecastData.mapNotNull { item ->
            val ms = runCatching { isoSdf.parse(item.timeTag)?.time }.getOrNull()
                ?: runCatching { spaceSdf.parse(item.timeTag)?.time }.getOrNull()
            ms?.let { Triple(item, it, item.observed == "predicted") }
        }
    }
    if (parsed.isEmpty()) return

    val dataPoints = parsed.map { (item, ms, _) -> ms to item.kp }
    val predicted = parsed.map { it.third }
    val pointColors = parsed.map { Color(KpLevel.fromKp(it.first.kp).color.toInt()) }
    val primaryColor = MaterialTheme.colorScheme.primary

    KpChartCard(
        sectionTitle = stringResource(R.string.kp_section_forecast48),
        noteBadge = stringResource(R.string.kp_forecast_note),
        dataSource = stringResource(R.string.kp_data_source_noaa),
        modifier = modifier,
    ) {
        WeatherLineChart(
            dataPoints = dataPoints,
            lineColor = primaryColor,
            warningThreshold = 5.0,
            dangerThreshold = 7.0,
            yAxisRange = 0.0..9.0,
            yLabelStep = 3.0,
            xLabelIntervalMs = 6 * 60 * 60 * 1000L,
            currentTimeMs = System.currentTimeMillis(),
            predicted = predicted,
            pointColors = pointColors,
            backgroundZones = listOf(
                BackgroundZone(0.0..5.0, ZoneGreen),
                BackgroundZone(5.0..7.0, ZoneYellow),
                BackgroundZone(7.0..9.0, ZoneRed),
            ),
            formatValue = { it.toInt().toString() },
        )
    }
}

// ─── 27일 장기예보 바 차트 (iOS longTermForecastChart 정합) ──

@Composable
fun Kp27DayChart(
    longTermForecast: List<Kp27DayForecast>,
    modifier: Modifier = Modifier,
) {
    if (longTermForecast.isEmpty()) return

    KpChartCard(
        sectionTitle = stringResource(R.string.kp_section_long_term),
        noteBadge = null,
        dataSource = stringResource(R.string.kp_data_source_noaa),
        modifier = modifier,
    ) {
        Kp27DayBarCanvas(longTermForecast = longTermForecast)
    }
}

@Composable
private fun Kp27DayBarCanvas(longTermForecast: List<Kp27DayForecast>) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val barWidthDp = 20.dp
    val barSpacingDp = 6.dp
    val leftPaddingDp = 36.dp
    val rightPaddingDp = 16.dp

    val totalWidthDp = leftPaddingDp + rightPaddingDp +
        (barWidthDp + barSpacingDp) * longTermForecast.size

    val scrollState = rememberScrollState()

    Canvas(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .width(totalWidthDp)
            .height(160.dp),
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val leftPadding = leftPaddingDp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 28.dp.toPx()

        val chartTop = topPadding
        val chartBottom = canvasHeight - bottomPadding
        val chartHeight = chartBottom - chartTop

        val maxKp = 9f

        val barWidth = barWidthDp.toPx()
        val barSpacing = barSpacingDp.toPx()

        val textPaint = Paint().apply {
            color = onSurfaceColor.toArgb()
            textSize = 10.sp.toPx()
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        val smallTextPaint = Paint().apply {
            color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
            textSize = 8.sp.toPx()
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        // Y축 라벨 & 그리드 (0/3/6/9)
        for (kpVal in 0..9 step 3) {
            val yPos = chartTop + chartHeight * (1f - kpVal.toFloat() / maxKp)
            drawContext.canvas.nativeCanvas.drawText(
                "$kpVal",
                4.dp.toPx(),
                yPos + textPaint.textSize / 3f,
                textPaint,
            )
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.1f),
                start = Offset(leftPadding, yPos),
                end = Offset(canvasWidth, yPos),
                strokeWidth = 0.5.dp.toPx(),
            )
        }

        // Kp=5 주의선
        val y5 = chartTop + chartHeight * (1f - 5f / maxKp)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        drawLine(
            color = WarningYellow,
            start = Offset(leftPadding, y5),
            end = Offset(canvasWidth, y5),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect,
        )

        // 바 그리기
        longTermForecast.forEachIndexed { index, data ->
            val barLeft = leftPadding + index * (barWidth + barSpacing)
            val barHeight = (data.kp.toFloat() / maxKp).coerceIn(0f, 1f) * chartHeight
            val barTop = chartBottom - barHeight

            val level = KpLevel.fromKp(data.kp)
            val barColor = Color(level.color.toInt())

            drawRect(
                color = barColor.copy(alpha = 0.85f),
                topLeft = Offset(barLeft, barTop),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )

            // X축 라벨 (5일 간격)
            if (index % 5 == 0 || index == longTermForecast.size - 1) {
                val label = formatShortDate(data.date)
                val textWidth = smallTextPaint.measureText(label)
                val textX = barLeft + barWidth / 2f - textWidth / 2f
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    textX,
                    canvasHeight - 4.dp.toPx(),
                    smallTextPaint,
                )
            }
        }
    }
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

            // Data source (iOS Link, Android Text only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = dataSource,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

/**
 * "2026 Feb 25" 형태의 날짜를 "02/25"로 간결하게 변환
 */
private fun formatShortDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy MMM dd", Locale.US)
        val date = sdf.parse(dateStr)
        if (date != null) {
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
        } else {
            dateStr.takeLast(5)
        }
    } catch (_: Exception) {
        dateStr.takeLast(5)
    }
}
