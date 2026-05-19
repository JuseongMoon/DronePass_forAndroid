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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── 색상 상수 ─────────────────────────────────────────────
private val WarningYellow = Color(0xFFDAA520)
private val DangerRed = Color(0xFFFF4500)
private val ZoneGreen = Color(0xFF4CAF50)
private val ZoneYellow = Color(0xFFFFC107)
private val ZoneRed = Color(0xFFF44336)
private val CurrentTimeLine = Color(0xFFFF0000)

// ─── 48시간 예보 라인 차트 ──────────────────────────────────

/**
 * 48시간 Kp 지수 예보 라인 차트
 *
 * - observed/estimated: 실선, predicted: 점선
 * - 배경 색상 영역 (초록/노랑/빨강)
 * - Kp=5 주의선, Kp=7 위험선
 * - 현재 시간 수직선
 * - 각 데이터 포인트에 원 표시
 */
@Composable
fun Kp48HourChart(
    forecastData: List<KpIndexData>,
    modifier: Modifier = Modifier
) {
    if (forecastData.isEmpty()) return

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    // 시간 파싱 (한 번만)
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val parsedData = remember(forecastData) {
        forecastData.mapNotNull { item ->
            try {
                val time = sdf.parse(item.timeTag)
                if (time != null) item to time.time else null
            } catch (_: Exception) {
                null
            }
        }
    }

    if (parsedData.isEmpty()) return

    val minTime = parsedData.minOf { it.second }
    val maxTime = parsedData.maxOf { it.second }
    val timeRange = (maxTime - minTime).toFloat().coerceAtLeast(1f)
    val nowMillis = System.currentTimeMillis()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceVariantColor.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.kp_48hour_chart_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas 차트
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // 여백
                val leftPadding = 36.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 8.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val chartLeft = leftPadding
                val chartRight = canvasWidth - rightPadding
                val chartTop = topPadding
                val chartBottom = canvasHeight - bottomPadding
                val chartWidth = chartRight - chartLeft
                val chartHeight = chartBottom - chartTop

                val maxKp = 9f

                // ── 텍스트 Paint ──
                val textPaint = Paint().apply {
                    color = onSurfaceColor.toArgb()
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT
                }

                val smallTextPaint = Paint().apply {
                    color = onSurfaceColor.copy(alpha = 0.6f).toArgb()
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT
                }

                // ── 배경 색상 영역 ──
                // 0~5: 초록 톤
                val y5 = chartTop + chartHeight * (1f - 5f / maxKp)
                val y7 = chartTop + chartHeight * (1f - 7f / maxKp)

                drawRect(
                    color = ZoneGreen.copy(alpha = 0.08f),
                    topLeft = Offset(chartLeft, y5),
                    size = androidx.compose.ui.geometry.Size(chartWidth, chartBottom - y5)
                )
                // 5~7: 노란 톤
                drawRect(
                    color = ZoneYellow.copy(alpha = 0.08f),
                    topLeft = Offset(chartLeft, y7),
                    size = androidx.compose.ui.geometry.Size(chartWidth, y5 - y7)
                )
                // 7~9: 빨간 톤
                drawRect(
                    color = ZoneRed.copy(alpha = 0.08f),
                    topLeft = Offset(chartLeft, chartTop),
                    size = androidx.compose.ui.geometry.Size(chartWidth, y7 - chartTop)
                )

                // ── 수평 기준선 ──
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                // Kp=5 주의선
                drawLine(
                    color = WarningYellow,
                    start = Offset(chartLeft, y5),
                    end = Offset(chartRight, y5),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                // Kp=7 위험선
                drawLine(
                    color = DangerRed,
                    start = Offset(chartLeft, y7),
                    end = Offset(chartRight, y7),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                // ── Y축 라벨 ──
                for (kpVal in 0..9 step 3) {
                    val yPos = chartTop + chartHeight * (1f - kpVal.toFloat() / maxKp)
                    drawContext.canvas.nativeCanvas.drawText(
                        "$kpVal",
                        4.dp.toPx(),
                        yPos + textPaint.textSize / 3f,
                        textPaint
                    )
                    // 그리드 가로선
                    drawLine(
                        color = onSurfaceColor.copy(alpha = 0.1f),
                        start = Offset(chartLeft, yPos),
                        end = Offset(chartRight, yPos),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                // ── X축 라벨 (6시간 간격) ──
                val xLabelSdf = SimpleDateFormat("HH", Locale.getDefault())
                val dateLabelSdf = SimpleDateFormat("MM/dd", Locale.getDefault())

                // 주요 시간 라벨 표시
                val labelIntervalMs = 6 * 3600 * 1000L // 6시간
                var labelTime = minTime - (minTime % labelIntervalMs) + labelIntervalMs
                while (labelTime <= maxTime) {
                    val xPos = chartLeft + chartWidth * ((labelTime - minTime).toFloat() / timeRange)
                    if (xPos in chartLeft..chartRight) {
                        val hourStr = xLabelSdf.format(Date(labelTime))
                        val dateStr = dateLabelSdf.format(Date(labelTime))

                        // 자정이면 날짜도 표시
                        val label = if (hourStr == "00") dateStr else hourStr
                        val textWidth = smallTextPaint.measureText(label)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            xPos - textWidth / 2f,
                            canvasHeight - 2.dp.toPx(),
                            smallTextPaint
                        )

                        // 세로 눈금선
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.08f),
                            start = Offset(xPos, chartTop),
                            end = Offset(xPos, chartBottom),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                    labelTime += labelIntervalMs
                }

                // ── 현재 시간 수직선 ──
                if (nowMillis in minTime..maxTime) {
                    val nowX = chartLeft + chartWidth * ((nowMillis - minTime).toFloat() / timeRange)
                    drawLine(
                        color = CurrentTimeLine,
                        start = Offset(nowX, chartTop),
                        end = Offset(nowX, chartBottom),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // ── 데이터 라인 & 포인트 ──
                val pointRadius = 3.dp.toPx()
                val lineStrokeWidth = 2.dp.toPx()
                val predictedDash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

                for (i in 0 until parsedData.size - 1) {
                    val (itemA, timeA) = parsedData[i]
                    val (itemB, timeB) = parsedData[i + 1]

                    val xA = chartLeft + chartWidth * ((timeA - minTime).toFloat() / timeRange)
                    val yA = chartTop + chartHeight * (1f - itemA.kp.toFloat() / maxKp)
                    val xB = chartLeft + chartWidth * ((timeB - minTime).toFloat() / timeRange)
                    val yB = chartTop + chartHeight * (1f - itemB.kp.toFloat() / maxKp)

                    // 선분의 타입 결정: 양쪽 모두 predicted이면 점선
                    val isPredicted = itemA.observed == "predicted" && itemB.observed == "predicted"
                    val lineColor = if (isPredicted) primaryColor.copy(alpha = 0.6f) else primaryColor
                    val effect = if (isPredicted) predictedDash else null

                    drawLine(
                        color = lineColor,
                        start = Offset(xA, yA),
                        end = Offset(xB, yB),
                        strokeWidth = lineStrokeWidth,
                        pathEffect = effect
                    )
                }

                // 포인트 그리기
                for ((item, time) in parsedData) {
                    val x = chartLeft + chartWidth * ((time - minTime).toFloat() / timeRange)
                    val y = chartTop + chartHeight * (1f - item.kp.toFloat() / maxKp)
                    val level = KpLevel.fromKp(item.kp)

                    drawCircle(
                        color = Color(level.color.toInt()),
                        radius = pointRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
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
