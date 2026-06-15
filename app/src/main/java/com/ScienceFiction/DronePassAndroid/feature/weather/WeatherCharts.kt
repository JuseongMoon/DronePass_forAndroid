package com.ScienceFiction.DronePassAndroid.feature.weather

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.ScienceFiction.DronePassAndroid.core.util.interpolateTemperatureColor
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

// ─── 공통 Line Chart ────────────────────────────────────────────

internal val WeatherForecastChartHeight = 250.dp
internal val WeatherLineChartDefaultHeight = WeatherForecastChartHeight
internal val WeatherPrecipitationChartHeight = WeatherForecastChartHeight
internal const val IosWeatherChartVisibleDomainMs = 12L * 60 * 60 * 1000
internal const val IosWeatherChartXLabelIntervalMs = 60L * 60 * 1000
internal val IosWeatherWindSpeedChartColor = Color(0xFF4CAF50)
internal val IosWeatherGustDifferenceChartColor = Color(0xFF5856D6)
internal val IosWeatherPrecipitationChartColor = Color(0xFF2196F3)
internal val IosWeatherVisibilityChartColor = Color(0xFF9C27B0)
internal val IosWeatherCriChartColor = Color(0xFF00BCD4)

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
 * - [xLabelTimesMs]: X축 라벨을 표시할 명시적 시각들 (KP 차트의 iOS AxisMarks values 정합)
 * - [currentTimeMs]: 현재 시간 빨강 수직선
 * - [predicted]: dataPoints 와 같은 size 의 Boolean 리스트. 양쪽이 모두 true 인 segment 는 점선
 * - [pointColors]: 각 데이터 포인트에 표시할 원의 색 (예: KpLevel 별 색상)
 * - [pointLabels]: 각 데이터 포인트 위에 표시할 라벨 (예: iOS KP PointMark annotation)
 * - [lineSegmentColors]: 각 데이터 포인트에서 시작하는 line segment 색상 (예: iOS KP LineMark foregroundStyle)
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
    xLabelTimesMs: List<Long>? = null,
    currentTimeMs: Long? = null,
    predicted: List<Boolean> = emptyList(),
    pointColors: List<Color>? = null,
    pointLabels: List<String> = emptyList(),
    lineSegmentColors: List<Color>? = null,
    backgroundZones: List<BackgroundZone> = emptyList(),
    chartHeight: Dp = WeatherLineChartDefaultHeight,
) {
    if (dataPoints.isEmpty()) return

    val onSurface = MaterialTheme.colorScheme.onSurface
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
        val topPadding = if (pointLabels.size == dataPoints.size) axisLabelPx + 12f else 12f
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

        // 격자선 (수평). yLabelStep 이 있으면 iOS AxisMarks 처럼 각 라벨마다 표시한다.
        if (yLabelStep == null || yLabelStep <= 0.0) {
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
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.1f),
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1f
                )
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
        val labelTimes = resolveTimeChartLabelTimes(
            dataStartMs = dataPoints.first().first,
            dataEndMs = dataPoints.last().first,
            intervalMs = intervalMs,
            explicitLabelTimesMs = xLabelTimesMs,
        )
        for (labelTime in labelTimes) {
            val x = toScreenX(labelTime)
            if (x >= leftPadding && x <= size.width - rightPadding) {
                drawContext.canvas.nativeCanvas.drawText(
                    formatIosTimeChartAxisLabel(labelTime),
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

        val drawSegmentedLine = shouldDrawWeatherLineAsSegments(
            dataPointCount = dataPoints.size,
            predicted = predicted,
            lineSegmentColors = lineSegmentColors,
        )

        // 기본 Weather 차트는 단일 부드러운 곡선 (기존 동작).
        // KP처럼 예측/레벨 색상이 명시되면 iOS LineMark처럼 segment 단위로 직선을 그린다.
        if (!drawSegmentedLine) {
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
            val linePath = Path()
            linePath.moveTo(screenPoints.first().x, screenPoints.first().y)
            for (i in 1 until screenPoints.size) {
                linePath.lineTo(screenPoints[i].x, screenPoints[i].y)
            }

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

            // segment 별 실선/점선 — KP 의 observed/predicted 표현
            val predictedDash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            for (i in 0 until screenPoints.size - 1) {
                val isPredictedSegment = predicted.size == dataPoints.size && predicted[i] && predicted[i + 1]
                val segmentColor = weatherLineSegmentColor(
                    lineColor = lineColor,
                    lineSegmentColors = lineSegmentColors,
                    segmentStartIndex = i,
                )
                drawLine(
                    color = if (isPredictedSegment) segmentColor.copy(alpha = 0.6f) else segmentColor,
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

        if (pointLabels.size == screenPoints.size) {
            val pointLabelPaint = Paint().apply {
                color = onSurface.toArgb()
                textSize = axisLabelPx
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                typeface = Typeface.DEFAULT
            }
            screenPoints.forEachIndexed { idx, point ->
                drawContext.canvas.nativeCanvas.drawText(
                    pointLabels[idx],
                    point.x,
                    point.y - 8f,
                    pointLabelPaint,
                )
            }
        }
    }
}

internal fun shouldDrawWeatherLineAsSegments(
    dataPointCount: Int,
    predicted: List<Boolean>,
    lineSegmentColors: List<Color>?,
): Boolean {
    return lineSegmentColors?.size == dataPointCount ||
        (predicted.size == dataPointCount && predicted.any { it })
}

internal fun weatherLineSegmentColor(
    lineColor: Color,
    lineSegmentColors: List<Color>?,
    segmentStartIndex: Int,
): Color {
    return lineSegmentColors?.getOrNull(segmentStartIndex) ?: lineColor
}

internal fun weatherChartPointColors(
    dataPointCount: Int,
    color: Color,
): List<Color> = List(dataPointCount) { color }

internal fun resolveTimeChartLabelTimes(
    dataStartMs: Long,
    dataEndMs: Long,
    intervalMs: Long,
    explicitLabelTimesMs: List<Long>? = null,
): List<Long> {
    if (dataEndMs < dataStartMs) return emptyList()
    explicitLabelTimesMs?.let { explicitTimes ->
        return explicitTimes
            .filter { it in dataStartMs..dataEndMs }
            .distinct()
    }
    if (intervalMs <= 0L) return emptyList()

    val labelTimes = mutableListOf<Long>()
    val startLabel = ((dataStartMs / intervalMs) + 1) * intervalMs
    var labelTime = startLabel
    while (labelTime <= dataEndMs) {
        labelTimes.add(labelTime)
        labelTime += intervalMs
    }
    return labelTimes
}

internal fun formatIosTimeChartAxisLabel(
    timeMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val timeFormat = SimpleDateFormat("HH", locale).apply {
        this.timeZone = timeZone
    }
    val hour = timeFormat.format(Date(timeMillis))
    if (hour != "00") return hour

    return SimpleDateFormat("MM/dd", locale).apply {
        this.timeZone = timeZone
    }.format(Date(timeMillis))
}

@Composable
internal fun ScrollableTimeChartViewport(
    dataPoints: List<Pair<Long, Double>>,
    visibleDomainMs: Long,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportWidth = maxWidth
        val widthScale = remember(dataPoints, visibleDomainMs) {
            resolveScrollableTimeChartWidthScale(dataPoints, visibleDomainMs)
        }
        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            content(Modifier.width(viewportWidth * widthScale))
        }
    }
}

internal fun resolveScrollableTimeChartWidthScale(
    dataPoints: List<Pair<Long, Double>>,
    visibleDomainMs: Long,
): Float {
    if (dataPoints.size < 2 || visibleDomainMs <= 0L) return 1f
    val durationMs = (dataPoints.last().first - dataPoints.first().first).coerceAtLeast(0L)
    return maxOf(1.0, durationMs.toDouble() / visibleDomainMs.toDouble()).toFloat()
}

internal fun resolveIosPrecipitationYMax(precipitations: List<Double>): Double {
    val maxPrecip = precipitations.maxOrNull() ?: 0.0
    return maxOf(10.0, ceil(maxPrecip * 1.2))
}

// ─── 개별 차트 6종 ────────────────────────────────────────────

@Composable
fun TemperatureChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.temperature }
    val pointColors = hourlyData.map { interpolateTemperatureColor(it.temperature) }
    ChartCard(
        title = stringResource(R.string.weather_chart_temperature),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = Color(0xFFFF6B35),
                fillAlpha = 0.15f,
                yAxisLabel = "\u00B0C",
                formatValue = { String.format(Locale.ROOT, "%.0f\u00B0", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = pointColors,
                lineSegmentColors = pointColors,
            )
        }
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
    val lineColor = IosWeatherWindSpeedChartColor
    ChartCard(
        title = stringResource(R.string.weather_chart_wind_speed),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                fillAlpha = 0.1f,
                warningThreshold = cautionThreshold,
                dangerThreshold = dangerThreshold,
                yAxisLabel = "m/s",
                formatValue = { String.format(Locale.ROOT, "%.1f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
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
    val lineColor = IosWeatherGustDifferenceChartColor
    ChartCard(
        title = stringResource(R.string.weather_chart_gust_difference),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                fillAlpha = 0.1f,
                warningThreshold = cautionThreshold,
                dangerThreshold = dangerThreshold,
                yAxisLabel = "m/s",
                formatValue = { String.format(Locale.ROOT, "%.1f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
    }
}

@Composable
fun PrecipitationChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.precipitation }
    val lineColor = IosWeatherPrecipitationChartColor
    val yMax = resolveIosPrecipitationYMax(dataPoints.map { it.second })
    ChartCard(
        title = stringResource(R.string.weather_chart_precipitation),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                fillAlpha = 0.1f,
                yAxisRange = 0.0..yMax,
                yAxisLabel = "mm/h",
                formatValue = { String.format(Locale.ROOT, "%.1f", it) },
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
                chartHeight = WeatherPrecipitationChartHeight,
            )
        }
    }
}

@Composable
fun VisibilityChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.visibility }
    val lineColor = IosWeatherVisibilityChartColor
    ChartCard(
        title = stringResource(R.string.weather_chart_visibility),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                fillAlpha = 0.1f,
                warningThreshold = IosVisibilityGoodKm,
                dangerThreshold = IosVisibilityPoorKm,
                yAxisLabel = "km",
                formatValue = { String.format(Locale.ROOT, "%.0f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
    }
}

@Composable
fun CriChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.cri }
    val lineColor = IosWeatherCriChartColor
    ChartCard(
        title = stringResource(R.string.weather_chart_cri),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                fillAlpha = 0.1f,
                warningThreshold = IosCriModerate,
                dangerThreshold = IosCriHigh,
                yAxisLabel = "%",
                formatValue = { String.format(Locale.ROOT, "%.0f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
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
