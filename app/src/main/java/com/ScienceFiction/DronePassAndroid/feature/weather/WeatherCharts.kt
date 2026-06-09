package com.ScienceFiction.DronePassAndroid.feature.weather

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── 공통 Line Chart ────────────────────────────────────────────

internal val WeatherLineChartDefaultHeight = 160.dp

/**
 * Y축 배경 색상 영역. WeatherLineChart 의 [backgroundZones] 에 전달.
 *
 * 예: KP 차트의 0-5 초록 / 5-7 노랑 / 7-9 빨강 영역.
 */
internal data class BackgroundZone(
    val range: ClosedFloatingPointRange<Double>,
    val color: Color,
    val alpha: Float = 0.08f,
)

/**
 * 공통 라인 차트. Weather/KP 등 시계열 데이터 표시에 재사용.
 *
 * 기본 사용 (Weather): 데이터 범위에 따라 자동 Y축, fill area + smooth cubic line.
 *
 * KP 등 고급 사용 시 옵션:
 * - [yAxisRange] / [yLabelStep]: Y축 0..9 같은 강제 범위
 * - [xLabelIntervalMs]: X축 라벨 간격 (기본 3시간)
 * - [currentTimeMs]: 현재 시간 빨강 수직선
 * - [predicted]: dataPoints 와 같은 size 의 Boolean 리스트. 양쪽이 모두 true 인 segment 는 점선
 * - [pointColors]: 각 데이터 포인트에 표시할 원의 색 (예: KpLevel 별 색상)
 * - [backgroundZones]: Y축 값 범위별 배경 색상 (KP 의 zone 표시)
 */
@Composable
internal fun WeatherLineChart(
    dataPoints: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillAlpha: Float = 0.1f,
    warningThreshold: Double? = null,
    dangerThreshold: Double? = null,
    invertWarning: Boolean = false,
    yAxisLabel: String = "",
    formatValue: (Double) -> String = { String.format(Locale.ROOT, "%.1f", it) },
    yAxisRange: ClosedFloatingPointRange<Double>? = null,
    yLabelStep: Double? = null,
    xLabelIntervalMs: Long? = null,
    currentTimeMs: Long? = null,
    predicted: List<Boolean> = emptyList(),
    pointColors: List<Color>? = null,
    backgroundZones: List<BackgroundZone> = emptyList(),
    chartHeight: Dp = WeatherLineChartDefaultHeight,
) {
    if (dataPoints.isEmpty()) return

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    // Canvas 내부 nativeCanvas.drawText 의 Paint.textSize 는 px 단위라 sp 환산이 필요.
    // 11.sp 는 Material labelSmall 크기와 비슷하며 사용자 폰트 크기 설정에 반응한다.
    val axisLabelPx = with(LocalDensity.current) { 11.sp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val leftPadding = 48f
        val rightPadding = 16f
        val topPadding = 12f
        val bottomPadding = 28f

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        if (chartWidth <= 0 || chartHeight <= 0 || dataPoints.size < 2) return@Canvas

        // Y축 범위 계산 — yAxisRange 가 명시되면 그대로 사용, 아니면 데이터 기반 자동 + 10% 패딩.
        val (yMin, yMax) = if (yAxisRange != null) {
            yAxisRange.start to yAxisRange.endInclusive
        } else {
            val rawMin = dataPoints.minOf { it.second }
            val rawMax = dataPoints.maxOf { it.second }
            val range = if (rawMax - rawMin < 0.001) 1.0 else rawMax - rawMin
            val padding10 = range * 0.1
            (rawMin - padding10) to (rawMax + padding10)
        }

        // X축 범위
        val xMin = dataPoints.first().first.toDouble()
        val xMax = dataPoints.last().first.toDouble()
        val xRange = if (xMax - xMin < 1.0) 1.0 else xMax - xMin

        // 좌표 변환 함수
        fun toScreenX(time: Long): Float =
            leftPadding + ((time.toDouble() - xMin) / xRange * chartWidth).toFloat()

        fun toScreenY(value: Double): Float =
            topPadding + ((yMax - value) / (yMax - yMin) * chartHeight).toFloat()

        // ── 배경 색상 영역 (옵션) ──
        // 예: KP 의 0-5 초록 / 5-7 노랑 / 7-9 빨강 zone
        backgroundZones.forEach { zone ->
            val zoneTop = toScreenY(zone.range.endInclusive.coerceAtMost(yMax))
            val zoneBottom = toScreenY(zone.range.start.coerceAtLeast(yMin))
            if (zoneBottom > zoneTop) {
                drawRect(
                    color = zone.color.copy(alpha = zone.alpha),
                    topLeft = Offset(leftPadding, zoneTop),
                    size = Size(chartWidth, zoneBottom - zoneTop),
                )
            }
        }

        // 격자선 (수평, 3개)
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = topPadding + chartHeight * i / gridCount
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.1f),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1f
            )
        }

        // Y축 라벨 — yLabelStep 명시 시 그 간격으로 0..yMax 표시. 기본은 3개(상/중/하).
        val yLabelPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.7f).toArgb()
            textSize = axisLabelPx
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        if (yLabelStep != null && yLabelStep > 0.0) {
            var v = yMin
            while (v <= yMax + 0.0001) {
                val y = toScreenY(v)
                drawContext.canvas.nativeCanvas.drawText(
                    formatValue(v),
                    leftPadding - 6f,
                    y + 6f,
                    yLabelPaint
                )
                v += yLabelStep
            }
        } else {
            for (i in 0..2) {
                val value = yMax - (yMax - yMin) * i / 2
                val y = topPadding + chartHeight * i / 2
                drawContext.canvas.nativeCanvas.drawText(
                    formatValue(value),
                    leftPadding - 6f,
                    y + 6f,
                    yLabelPaint
                )
            }
        }

        // X축 시간 라벨 — xLabelIntervalMs 명시 시 그 간격, 기본 3시간.
        val xLabelPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.7f).toArgb()
            textSize = axisLabelPx
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val intervalMs = xLabelIntervalMs ?: (3 * 60 * 60 * 1000L)
        val timeFormat = SimpleDateFormat("HH", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val firstTime = dataPoints.first().first
        // 첫 데이터 시간을 interval 단위로 올림
        val startLabel = ((firstTime / intervalMs) + 1) * intervalMs
        var labelTime = startLabel
        while (labelTime <= dataPoints.last().first) {
            val x = toScreenX(labelTime)
            if (x >= leftPadding && x <= size.width - rightPadding) {
                // 자정(00시)이면 날짜 라벨로 대체 — KP 처럼 다일에 걸친 차트 가독성 향상.
                val hour = timeFormat.format(Date(labelTime))
                val label = if (hour == "00") dateFormat.format(Date(labelTime)) else hour
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    size.height - 2f,
                    xLabelPaint
                )
                // 수직 격자선
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.06f),
                    start = Offset(x, topPadding),
                    end = Offset(x, topPadding + chartHeight),
                    strokeWidth = 1f
                )
            }
            labelTime += intervalMs
        }

        // Warning threshold (노란 점선)
        warningThreshold?.let { threshold ->
            if (threshold in yMin..yMax) {
                val y = toScreenY(threshold)
                drawLine(
                    color = Color(0xFFFFB300),
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }

        // Danger threshold (빨간 점선)
        dangerThreshold?.let { threshold ->
            if (threshold in yMin..yMax) {
                val y = toScreenY(threshold)
                drawLine(
                    color = Color(0xFFF44336),
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }

        // 현재 시간 빨강 수직선 (옵션)
        currentTimeMs?.let { now ->
            if (now in dataPoints.first().first..dataPoints.last().first) {
                val nowX = toScreenX(now)
                drawLine(
                    color = Color(0xFFFF0000),
                    start = Offset(nowX, topPadding),
                    end = Offset(nowX, topPadding + chartHeight),
                    strokeWidth = 2f,
                )
            }
        }

        // 데이터 포인트를 화면 좌표로 변환
        val screenPoints = dataPoints.map { (time, value) ->
            Offset(toScreenX(time), toScreenY(value))
        }

        // predicted 가 비어있으면 단일 부드러운 곡선 (기존 동작).
        // 명시되면 segment 별로 실선/점선 분기하여 직선으로 그린다 (cubic + dashed 조합은 어색).
        if (predicted.size != dataPoints.size || predicted.all { !it }) {
            // 부드러운 곡선 Path (cubicTo) — 기존 동작
            val linePath = Path()
            linePath.moveTo(screenPoints.first().x, screenPoints.first().y)
            for (i in 1 until screenPoints.size) {
                val prev = screenPoints[i - 1]
                val curr = screenPoints[i]
                val cpx = (prev.x + curr.x) / 2
                linePath.cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
            }

            // 영역 채우기 (라인 아래)
            val fillPath = Path()
            fillPath.addPath(linePath)
            fillPath.lineTo(screenPoints.last().x, topPadding + chartHeight)
            fillPath.lineTo(screenPoints.first().x, topPadding + chartHeight)
            fillPath.close()

            clipRect(
                left = leftPadding,
                top = topPadding,
                right = size.width - rightPadding,
                bottom = topPadding + chartHeight
            ) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = fillAlpha),
                            lineColor.copy(alpha = 0.01f)
                        ),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )
            }

            // 라인 그리기
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3f)
            )
        } else {
            // segment 별 실선/점선 — KP 의 observed/predicted 표현
            val predictedDash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            for (i in 0 until screenPoints.size - 1) {
                val isPredictedSegment = predicted[i] && predicted[i + 1]
                drawLine(
                    color = if (isPredictedSegment) lineColor.copy(alpha = 0.6f) else lineColor,
                    start = screenPoints[i],
                    end = screenPoints[i + 1],
                    strokeWidth = 3f,
                    pathEffect = if (isPredictedSegment) predictedDash else null,
                )
            }
        }

        // 데이터 포인트 원 (옵션) — pointColors 가 명시되면 그 색상으로 표시
        pointColors?.let { colors ->
            screenPoints.forEachIndexed { idx, point ->
                val color = colors.getOrNull(idx) ?: lineColor
                drawCircle(color = color, radius = 4f, center = point)
            }
        }
    }
}

// ─── 강수량 Bar Chart ────────────────────────────────────────────

@Composable
private fun PrecipitationBarChart(
    dataPoints: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    currentTimeMs: Long? = null,
) {
    if (dataPoints.isEmpty()) return

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = Color(0xFF42A5F5)
    val axisLabelPx = with(LocalDensity.current) { 11.sp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val leftPadding = 48f
        val rightPadding = 16f
        val topPadding = 12f
        val bottomPadding = 28f

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        val rawMax = dataPoints.maxOf { it.second }
        val yMax = if (rawMax < 0.1) 1.0 else rawMax * 1.1
        val yMin = 0.0

        val xMin = dataPoints.first().first.toDouble()
        val xMax = dataPoints.last().first.toDouble()
        val xRange = if (xMax - xMin < 1.0) 1.0 else xMax - xMin

        fun toScreenX(time: Long): Float =
            leftPadding + ((time.toDouble() - xMin) / xRange * chartWidth).toFloat()

        fun toScreenY(value: Double): Float =
            topPadding + ((yMax - value) / (yMax - yMin) * chartHeight).toFloat()

        // 격자선
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = topPadding + chartHeight * i / gridCount
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.1f),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1f
            )
        }

        // Y축 라벨
        val yLabelPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.7f).toArgb()
            textSize = axisLabelPx
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        for (i in 0..2) {
            val value = yMax - (yMax - yMin) * i / 2
            val y = topPadding + chartHeight * i / 2
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.ROOT, "%.1f", value),
                leftPadding - 6f,
                y + 6f,
                yLabelPaint
            )
        }

        // X축 시간 라벨
        val xLabelPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.7f).toArgb()
            textSize = axisLabelPx
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val timeFormat = SimpleDateFormat("HH", Locale.getDefault())
        val threeHoursMs = 3 * 60 * 60 * 1000L
        val firstTime = dataPoints.first().first
        val startLabel = ((firstTime / threeHoursMs) + 1) * threeHoursMs
        var labelTime = startLabel
        while (labelTime <= dataPoints.last().first) {
            val x = toScreenX(labelTime)
            if (x >= leftPadding && x <= size.width - rightPadding) {
                drawContext.canvas.nativeCanvas.drawText(
                    timeFormat.format(Date(labelTime)),
                    x,
                    size.height - 2f,
                    xLabelPaint
                )
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.06f),
                    start = Offset(x, topPadding),
                    end = Offset(x, topPadding + chartHeight),
                    strokeWidth = 1f
                )
            }
            labelTime += threeHoursMs
        }

        // 현재 시간 빨강 수직선 (iOS WeatherForecastView RuleMark 정합)
        currentTimeMs?.let { now ->
            if (now in dataPoints.first().first..dataPoints.last().first) {
                val nowX = toScreenX(now)
                drawLine(
                    color = Color(0xFFFF0000),
                    start = Offset(nowX, topPadding),
                    end = Offset(nowX, topPadding + chartHeight),
                    strokeWidth = 2f,
                )
            }
        }

        // 바 그리기
        val barWidthRatio = 0.6f
        val totalBars = dataPoints.size
        val maxBarWidth = chartWidth / totalBars * barWidthRatio
        val barWidth = maxBarWidth.coerceAtMost(20f)
        val cornerRadius = barWidth / 2

        for ((time, value) in dataPoints) {
            if (value <= 0.0) continue
            val x = toScreenX(time)
            val yTop = toScreenY(value)
            val yBottom = toScreenY(0.0)
            val barHeight = yBottom - yTop

            // 둥근 상단 바
            val barPath = Path().apply {
                if (barHeight <= cornerRadius) {
                    // 바가 작으면 단순 직사각형
                    addRect(Rect(
                        left = x - barWidth / 2,
                        top = yTop,
                        right = x + barWidth / 2,
                        bottom = yBottom
                    ))
                } else {
                    moveTo(x - barWidth / 2, yBottom)
                    lineTo(x - barWidth / 2, yTop + cornerRadius)
                    // 왼쪽 상단 모서리
                    cubicTo(
                        x - barWidth / 2, yTop,
                        x - barWidth / 2, yTop,
                        x, yTop
                    )
                    // 오른쪽 상단 모서리
                    cubicTo(
                        x + barWidth / 2, yTop,
                        x + barWidth / 2, yTop,
                        x + barWidth / 2, yTop + cornerRadius
                    )
                    lineTo(x + barWidth / 2, yBottom)
                    close()
                }
            }
            drawPath(barPath, barColor)
        }
    }
}

// ─── 개별 차트 6종 ────────────────────────────────────────────

@Composable
fun TemperatureChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.temperature }
    ChartCard(
        title = stringResource(R.string.weather_chart_temperature),
        modifier = modifier,
    ) {
        WeatherLineChart(
            dataPoints = dataPoints,
            lineColor = Color(0xFFFF6B35),
            fillAlpha = 0.15f,
            yAxisLabel = "\u00B0C",
            formatValue = { String.format("%.0f\u00B0", it) },
            currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
        )
    }
}

@Composable
fun WindSpeedChart(
    hourlyData: List<HourlyWeatherData>,
    category: DroneCategory,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.windSpeed }
    val (cautionThreshold, dangerThreshold) = iosWindSpeedThresholds(category)
    ChartCard(
        title = stringResource(R.string.weather_chart_wind_speed),
        modifier = modifier,
    ) {
        WeatherLineChart(
            dataPoints = dataPoints,
            lineColor = Color(0xFF42A5F5),
            fillAlpha = 0.1f,
            warningThreshold = cautionThreshold,
            dangerThreshold = dangerThreshold,
            yAxisLabel = "m/s",
            formatValue = { String.format(Locale.ROOT, "%.1f", it) },
            currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
        )
    }
}

@Composable
fun GustDifferenceChart(
    hourlyData: List<HourlyWeatherData>,
    category: DroneCategory,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.gustDifference }
    val (cautionThreshold, dangerThreshold) = iosGustDifferenceThresholds(category)
    ChartCard(
        title = stringResource(R.string.weather_chart_gust_difference),
        modifier = modifier,
    ) {
        WeatherLineChart(
            dataPoints = dataPoints,
            lineColor = Color(0xFFFF7043),
            fillAlpha = 0.1f,
            warningThreshold = cautionThreshold,
            dangerThreshold = dangerThreshold,
            yAxisLabel = "m/s",
            formatValue = { String.format(Locale.ROOT, "%.1f", it) },
            currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
        )
    }
}

@Composable
fun PrecipitationChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.precipitation }
    ChartCard(
        title = stringResource(R.string.weather_chart_precipitation),
        modifier = modifier,
    ) {
        PrecipitationBarChart(
            dataPoints = dataPoints,
            currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
        )
    }
}

@Composable
fun VisibilityChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.visibility }
    ChartCard(
        title = stringResource(R.string.weather_chart_visibility),
        modifier = modifier,
    ) {
        WeatherLineChart(
            dataPoints = dataPoints,
            lineColor = Color(0xFF78909C),
            fillAlpha = 0.1f,
            warningThreshold = IosVisibilityGoodKm,
            dangerThreshold = IosVisibilityPoorKm,
            yAxisLabel = "km",
            formatValue = { String.format(Locale.ROOT, "%.0f", it) },
            currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
        )
    }
}

@Composable
fun CriChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.cri }
    ChartCard(
        title = stringResource(R.string.weather_chart_cri),
        modifier = modifier,
    ) {
        WeatherLineChart(
            dataPoints = dataPoints,
            lineColor = Color(0xFF26A69A),
            fillAlpha = 0.1f,
            warningThreshold = IosCriModerate,
            dangerThreshold = IosCriHigh,
            yAxisLabel = "%",
            formatValue = { String.format(Locale.ROOT, "%.0f", it) },
            currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
        )
    }
}

internal fun resolveWeatherChartCurrentTimeMarkerMs(
    dataPoints: List<Pair<Long, Double>>,
    nowMillis: Long = System.currentTimeMillis(),
): Long? {
    if (dataPoints.isEmpty()) return null
    return nowMillis.takeIf { it in dataPoints.first().first..dataPoints.last().first }
}

// ─── 차트 Card 래퍼 ────────────────────────────────────────────

@Composable
internal fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            content()
        }
    }
}
