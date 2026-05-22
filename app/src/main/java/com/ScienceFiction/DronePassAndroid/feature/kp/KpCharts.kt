package com.ScienceFiction.DronePassAndroid.feature.kp

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ScienceFiction.DronePassAndroid.feature.weather.ChartCard
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── 색상 상수 ─────────────────────────────────────────────
private val WarningYellow = Color(0xFFDAA520)
private val ZoneGreen = Color(0xFF4CAF50)
private val ZoneYellow = Color(0xFFFFC107)
private val ZoneRed = Color(0xFFF44336)

// ─── 48시간 예보 라인 차트 ──────────────────────────────────

/**
 * 48시간 Kp 지수 예보 라인 차트.
 *
 * 날씨 화면의 공통 차트 컴포넌트 [WeatherLineChart] 를 재사용한다. KP 고유 시각화는
 * 옵션 인자로 전달:
 * - Y축 0..9 강제 + 3 step 라벨 (0/3/6/9)
 * - X축 6시간 간격, 자정엔 MM/dd
 * - Kp=5 주의선(노랑) / Kp=7 위험선(빨강)
 * - Kp 0-5/5-7/7-9 배경 색상 zone
 * - observed/predicted segment 별 실선/점선
 * - 각 포인트 KpLevel 색상 원
 * - 현재 시간 빨강 수직선
 */
@Composable
fun KpForecastLineChart(
    forecastData: List<KpIndexData>,
    modifier: Modifier = Modifier,
) {
    if (forecastData.isEmpty()) return

    // NOAA forecast 의 time_tag 는 ISO 8601 (`"2026-05-13T00:00:00"`, T 구분자).
    // iOS 도 동일 패턴으로 디코딩 (`KPIndexModel.swift` line 40).
    // 옛 응답이나 다른 소스 호환 위해 공백 구분자 패턴도 fallback 으로 시도.
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }
    val spaceSdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    // (item, epochMs, isPredicted) 튜플로 한 번에 파싱.
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

    ChartCard(
        title = stringResource(R.string.kp_48hour_chart_title),
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

// ─── 27일 장기예보 바 차트 ──────────────────────────────────

/**
 * 27일 Kp 장기 예보 바 차트
 *
 * - 각 바의 색상: KpLevel에 따라 동적
 * - X축: 날짜 (5일 간격 라벨)
 * - Y축: Kp 값 (0~9)
 * - 수평 기준선: Kp=5 주의선 (노란 점선)
 * - 수평 스크롤 가능
 */
@Composable
fun Kp27DayChart(
    longTermForecast: List<Kp27DayForecast>,
    modifier: Modifier = Modifier
) {
    if (longTermForecast.isEmpty()) return

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    val barWidthDp = 20.dp
    val barSpacingDp = 6.dp
    val leftPaddingDp = 36.dp
    val rightPaddingDp = 16.dp

    // 총 너비 계산
    val totalWidthDp = leftPaddingDp + rightPaddingDp +
        (barWidthDp + barSpacingDp) * longTermForecast.size

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceVariantColor.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.kp_27day_chart_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val scrollState = rememberScrollState()

            Canvas(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .width(totalWidthDp)
                    .height(160.dp)
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

                // ── 텍스트 Paint ──
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

                // ── Y축 라벨 & 그리드 ──
                for (kpVal in 0..9 step 3) {
                    val yPos = chartTop + chartHeight * (1f - kpVal.toFloat() / maxKp)
                    drawContext.canvas.nativeCanvas.drawText(
                        "$kpVal",
                        4.dp.toPx(),
                        yPos + textPaint.textSize / 3f,
                        textPaint
                    )
                    drawLine(
                        color = onSurfaceColor.copy(alpha = 0.1f),
                        start = Offset(leftPadding, yPos),
                        end = Offset(canvasWidth, yPos),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                // ── Kp=5 주의선 ──
                val y5 = chartTop + chartHeight * (1f - 5f / maxKp)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                drawLine(
                    color = WarningYellow,
                    start = Offset(leftPadding, y5),
                    end = Offset(canvasWidth, y5),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                // ── 바 그리기 ──
                longTermForecast.forEachIndexed { index, data ->
                    val barLeft = leftPadding + index * (barWidth + barSpacing)
                    val barRight = barLeft + barWidth
                    val barHeight = (data.kp.toFloat() / maxKp).coerceIn(0f, 1f) * chartHeight
                    val barTop = chartBottom - barHeight

                    val level = KpLevel.fromKp(data.kp)
                    val barColor = Color(level.color.toInt())

                    drawRect(
                        color = barColor.copy(alpha = 0.85f),
                        topLeft = Offset(barLeft, barTop),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )

                    // ── X축 라벨 (5일 간격) ──
                    if (index % 5 == 0 || index == longTermForecast.size - 1) {
                        // 날짜에서 간결한 라벨 추출 (예: "2026 Feb 25" → "02/25")
                        val label = formatShortDate(data.date)
                        val textWidth = smallTextPaint.measureText(label)
                        val textX = barLeft + barWidth / 2f - textWidth / 2f
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            textX,
                            canvasHeight - 4.dp.toPx(),
                            smallTextPaint
                        )
                    }
                }
            }
        }
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
            // 파싱 실패 시 뒤의 숫자 부분 사용
            dateStr.takeLast(5)
        }
    } catch (_: Exception) {
        dateStr.takeLast(5)
    }
}
