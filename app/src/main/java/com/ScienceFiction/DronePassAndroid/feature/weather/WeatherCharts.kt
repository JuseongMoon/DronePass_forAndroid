package com.ScienceFiction.DronePassAndroid.feature.weather

import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemRed
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.abs
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.math.floor

// ─── 공통 Line Chart ────────────────────────────────────────────

internal val WeatherForecastChartHeight = 250.dp
internal val WeatherLineChartDefaultHeight = WeatherForecastChartHeight
internal val WeatherPrecipitationChartHeight = WeatherForecastChartHeight
internal val IosWeatherChartCardCornerRadius = IosWeatherForecastCardCornerRadius
internal val IosWeatherChartCardPadding = IosWeatherForecastCardPadding
internal val IosWeatherChartCardSpacing = IosWeatherForecastCardSpacing
internal val IosWeatherChartCardContainerColor = IosWeatherForecastCardContainerColor
internal const val IosWeatherChartVisibleDomainMs = 12L * 60 * 60 * 1000
internal const val IosWeatherChartXLabelIntervalMs = 60L * 60 * 1000
internal val IosWeatherWindSpeedChartColor = Color(0xFF34C759) // iOS .green
internal val IosWeatherGustDifferenceChartColor = Color(0xFF5856D6)
internal val IosWeatherPrecipitationChartColor = Color(0xFF007AFF) // iOS .blue
internal val IosWeatherVisibilityChartColor = Color(0xFFAF52DE) // iOS .purple
internal val IosWeatherCriChartColor = Color(0xFF32ADE6) // iOS .cyan
internal val IosWeatherCriChartYRange = 0.0..100.0
internal val IosWeatherRuleMarkBlue = Color(0xFF007AFF)
internal val IosWeatherRuleMarkOrange = Color(0xFFFF9500)
internal val IosWeatherRuleMarkGreen = Color(0xFF34C759)
internal val IosWeatherRuleMarkYellow = Color(0xFFFFCC00)
internal val IosWeatherRuleMarkRed = Color(0xFFFF3B30)

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

internal data class WeatherThresholdLine(
    val value: Double,
    val color: Color,
)

private data class WeatherChartLegendItem(
    val color: Color,
    val text: String,
)

/**
 * 공통 라인 차트. Weather/KP 등 시계열 데이터 표시에 재사용.
 *
 * 기본 사용 (Weather): 데이터 범위에 따라 자동 Y축, fill area + 직선(iOS LineMark .linear).
 *
 * KP 등 고급 사용 시 옵션:
 * - [yAxisRange] / [yLabelStep]: Y축 0..9 같은 강제 범위
 * - [xLabelIntervalMs]: X축 라벨 간격 (기본 3시간)
 * - [xLabelTimesMs]: X축 라벨을 표시할 명시적 시각들 (KP 차트의 iOS AxisMarks values 정합)
 * - [currentTimeMs]: 현재 시간 빨강 수직선
 * - [currentTimeLabel]: 현재 시간 수직선 위 라벨. iOS 날씨 그래프는 라벨 없이 빨간 선만 그리므로 기본은 null
 * - [predicted]: dataPoints 와 같은 size 의 Boolean 리스트. 양쪽이 모두 true 인 segment 는 점선
 * - [pointColors]: 각 데이터 포인트에 표시할 원의 색 (예: KpLevel 별 색상)
 * - [pointLabels]: 각 데이터 포인트 위에 표시할 라벨 (예: iOS KP PointMark annotation)
 * - [lineSegmentColors]: 각 데이터 포인트에서 시작하는 line segment 색상 (예: iOS KP LineMark foregroundStyle)
 * - [thresholdLines]: iOS Weather RuleMark 처럼 값별 색상이 필요한 기준선
 * - [backgroundZones]: Y축 값 범위별 배경 색상 (KP 의 zone 표시)
 */
@Composable
internal fun WeatherLineChart(
    dataPoints: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    // iOS AreaMark LinearGradient opacity 0.3(위) → 0.1(아래)
    fillAlpha: Float = WeatherChartAreaTopAlpha,
    warningThreshold: Double? = null,
    dangerThreshold: Double? = null,
    invertWarning: Boolean = false,
    thresholdLines: List<WeatherThresholdLine> = emptyList(),
    yAxisLabel: String = "",
    formatValue: (Double) -> String = { String.format(Locale.ROOT, "%.1f", it) },
    yAxisRange: ClosedFloatingPointRange<Double>? = null,
    yLabelStep: Double? = null,
    xLabelIntervalMs: Long? = null,
    xLabelTimesMs: List<Long>? = null,
    currentTimeMs: Long? = null,
    currentTimeLabel: String? = null,
    predicted: List<Boolean> = emptyList(),
    pointColors: List<Color>? = null,
    pointLabels: List<String> = emptyList(),
    lineSegmentColors: List<Color>? = null,
    backgroundZones: List<BackgroundZone> = emptyList(),
    chartHeight: Dp = WeatherLineChartDefaultHeight,
    xLabelFormatter: (Long) -> String = { formatIosTimeChartAxisLabel(it) },
    fillValueGradient: List<Pair<Double, Color>>? = null,
    // iOS PointMark 기본 크기(KP symbolSize 36 포함) ≈ 반지름 3pt. 온도 그래프(symbolSize 60)는 4dp.
    pointRadius: Dp = WeatherChartDefaultPointRadius,
) {
    if (dataPoints.isEmpty()) return
    // 가로 스크롤 뷰포트 안이면 Y축을 스크롤 위치에 고정해 그린다 (iOS chartScrollableAxes 정합).
    val horizontalScroll = LocalChartHorizontalScroll.current

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    // Canvas 내부 nativeCanvas.drawText 의 Paint.textSize 는 px 단위라 sp 환산이 필요.
    // 11.sp 는 Material labelSmall 크기와 비슷하며 사용자 폰트 크기 설정에 반응한다.
    val axisLabelPx = with(LocalDensity.current) { 11.sp.toPx() }

    // 가로 스크롤 중에는 프레임마다 다시 그리므로 텍스트 Paint 는 한 번 만들어 재사용한다.
    // iOS Swift Charts 축 라벨: .secondary 그대로
    val axisLabelArgb = onSurfaceVariant.toArgb()
    val pointLabelArgb = onSurface.toArgb()
    val yLabelPaint = remember(axisLabelArgb, axisLabelPx) {
        Paint().apply {
            color = axisLabelArgb
            textSize = axisLabelPx
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
    }
    val xLabelPaint = remember(axisLabelArgb, axisLabelPx) {
        Paint().apply {
            color = axisLabelArgb
            textSize = axisLabelPx
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val xDateLabelPaint = remember(xLabelPaint) { Paint(xLabelPaint).apply { typeface = Typeface.DEFAULT_BOLD } }
    val xExplicitLabelPaint = remember(xLabelPaint) {
        Paint(xLabelPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, 500, false) }
    }
    val pointLabelPaint = remember(pointLabelArgb, axisLabelPx) {
        Paint().apply {
            color = pointLabelArgb
            textSize = axisLabelPx
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val scrollX = horizontalScroll?.value?.toFloat() ?: 0f
        val lineStrokePx = 2.dp.toPx()
        val pointRadiusPx = pointRadius.toPx()
        val labelGapPx = 6.dp.toPx()
        val rightPadding = 12.dp.toPx()
        val shouldDrawCurrentMarker = shouldDrawWeatherCurrentMarker(
            currentTimeMs = currentTimeMs,
            dataStartMs = dataPoints.first().first,
            dataEndMs = dataPoints.last().first,
        )
        val topPadding = resolveWeatherChartTopPadding(
            hasPointLabels = pointLabels.size == dataPoints.size,
            hasCurrentTimeLabel = shouldDrawCurrentMarker && currentTimeLabel != null,
            axisLabelPx = axisLabelPx,
        )
        val bottomPadding = axisLabelPx + 10.dp.toPx()

        // Y축 범위 계산 — yAxisRange 가 명시되면 그대로 사용, 아니면 데이터 기반 자동 + 10% 패딩.
        val (yMin, yMax) = yAxisRange?.let { it.start to it.endInclusive }
            ?: resolveWeatherChartAutoYRange(
                values = dataPoints.map { it.second },
                thresholds = thresholdLines.map { it.value },
            )

        // Y축 라벨 값과 폭 — 가장 긴 라벨이 잘리지 않도록 왼쪽 여백을 라벨 폭으로 잡는다.
        val yLabelValues = resolveWeatherChartYLabelValues(yMin, yMax, yLabelStep)
        val yLabelTexts = yLabelValues.map(formatValue)
        val leftPadding = (yLabelTexts.maxOfOrNull { yLabelPaint.measureText(it) } ?: 0f) + labelGapPx * 2
        // iOS plotDimension(startPadding:) 처럼 첫 점을 축에서 살짝 띄워 점 라벨이 잘리지 않게 한다.
        val plotStartInset = 10.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding - plotStartInset
        val chartHeight = size.height - topPadding - bottomPadding

        if (chartWidth <= 0 || chartHeight <= 0 || dataPoints.size < 2) return@Canvas

        // X축 범위
        val xMin = dataPoints.first().first.toDouble()
        val xMax = dataPoints.last().first.toDouble()
        val xRange = if (xMax - xMin < 1.0) 1.0 else xMax - xMin

        // 좌표 변환 함수
        fun toScreenX(time: Long): Float =
            leftPadding + plotStartInset + ((time.toDouble() - xMin) / xRange * chartWidth).toFloat()

        fun toScreenY(value: Double): Float =
            topPadding + ((yMax - value) / (yMax - yMin) * chartHeight).toFloat()

        // 플롯은 고정된 Y축 오른쪽에만 그린다. 스크롤로 밀려난 부분은 축 아래로 가려진다.
        clipRect(left = scrollX + leftPadding, top = 0f, right = size.width, bottom = size.height) {
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

        // 격자선 (수평) — Y축 라벨마다 한 줄.
        yLabelValues.forEach { v ->
            val y = toScreenY(v)
            drawLine(
                color = onSurfaceVariant.copy(alpha = WeatherChartGridAlpha),
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1f
            )
        }

        // X축 시간 라벨 — xLabelIntervalMs 명시 시 그 간격, 기본 3시간.
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
                val label = xLabelFormatter(labelTime)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    size.height - 4.dp.toPx(),
                    // iOS 처럼 날짜가 바뀌는 자정 라벨(MM/dd)은 굵게 표시한다.
                    when {
                        xLabelTimesMs != null -> xExplicitLabelPaint // KP 장기 예보 날짜(iOS .medium)
                        '/' in label -> xDateLabelPaint
                        else -> xLabelPaint
                    }
                )
                // 수직 격자선
                drawLine(
                    color = onSurfaceVariant.copy(alpha = WeatherChartGridAlpha),
                    start = Offset(x, topPadding),
                    end = Offset(x, topPadding + chartHeight),
                    strokeWidth = 1f
                )
            }
        }

        thresholdLines.forEach { thresholdLine ->
            if (thresholdLine.value in yMin..yMax) {
                drawWeatherThresholdLine(
                    y = toScreenY(thresholdLine.value),
                    color = thresholdLine.color,
                    left = leftPadding,
                    right = size.width - rightPadding,
                )
            }
        }

        // Warning threshold (노란 점선)
        warningThreshold?.let { threshold ->
            if (threshold in yMin..yMax) {
                drawWeatherThresholdLine(
                    y = toScreenY(threshold),
                    color = IosWeatherRuleMarkOrange,
                    left = leftPadding,
                    right = size.width - rightPadding,
                )
            }
        }

        // Danger threshold (빨간 점선)
        dangerThreshold?.let { threshold ->
            if (threshold in yMin..yMax) {
                drawWeatherThresholdLine(
                    y = toScreenY(threshold),
                    color = IosWeatherRuleMarkRed,
                    left = leftPadding,
                    right = size.width - rightPadding,
                )
            }
        }

        // 현재 시간 빨강 수직선 (옵션)
        if (shouldDrawCurrentMarker && currentTimeMs != null) {
            val nowX = toScreenX(currentTimeMs)
            drawLine(
                color = IosSystemRed, // iOS .red
                start = Offset(nowX, topPadding),
                end = Offset(nowX, topPadding + chartHeight),
                // iOS RuleMark lineWidth: 2 (pt)
                strokeWidth = 2.dp.toPx(),
            )
            currentTimeLabel?.let { label ->
                drawWeatherCurrentTimeLabel(
                    label = label,
                    centerX = nowX,
                    leftBound = leftPadding,
                    rightBound = size.width - rightPadding,
                    textSizePx = axisLabelPx,
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

        // 기본 Weather 차트는 단일 선. iOS LineMark 기본 보간(.linear)처럼 점 사이를 직선으로 잇는다.
        // KP처럼 예측/레벨 색상이 명시되면 segment 단위로 그린다.
        if (!drawSegmentedLine) {
            val linePath = Path()
            linePath.moveTo(screenPoints.first().x, screenPoints.first().y)
            for (i in 1 until screenPoints.size) {
                linePath.lineTo(screenPoints[i].x, screenPoints[i].y)
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
                    brush = fillValueGradient?.let { stops ->
                        weatherValueGradientBrush(
                            stops = stops,
                            yMin = yMin,
                            yMax = yMax,
                            top = topPadding,
                            bottom = topPadding + chartHeight,
                            alpha = IosWeatherValueGradientAreaAlpha,
                        )
                    } ?: Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = fillAlpha),
                            lineColor.copy(alpha = fillAlpha * WeatherChartAreaBottomRatio)
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
                style = Stroke(width = lineStrokePx)
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

            val perSegmentFill = fillValueGradient == null &&
                lineSegmentColors != null && lineSegmentColors.size >= screenPoints.size - 1
            clipRect(
                left = leftPadding,
                top = topPadding,
                right = size.width - rightPadding,
                bottom = topPadding + chartHeight
            ) {
                // iOS KP AreaMark 처럼 구간마다 그 수준 색으로 채운다.
                if (perSegmentFill) {
                    val bottomY = topPadding + chartHeight
                    for (seg in 0 until screenPoints.size - 1) {
                        val color = lineSegmentColors!![seg]
                        val a = screenPoints[seg]
                        val b = screenPoints[seg + 1]
                        val segmentPath = Path().apply {
                            moveTo(a.x, a.y)
                            lineTo(b.x, b.y)
                            lineTo(b.x, bottomY)
                            lineTo(a.x, bottomY)
                            close()
                        }
                        drawPath(
                            path = segmentPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    color.copy(alpha = fillAlpha),
                                    color.copy(alpha = fillAlpha * WeatherChartAreaBottomRatio),
                                ),
                                startY = topPadding,
                                endY = bottomY,
                            ),
                        )
                    }
                } else drawPath(
                    path = fillPath,
                    brush = fillValueGradient?.let { stops ->
                        weatherValueGradientBrush(
                            stops = stops,
                            yMin = yMin,
                            yMax = yMax,
                            top = topPadding,
                            bottom = topPadding + chartHeight,
                            alpha = IosWeatherValueGradientAreaAlpha,
                        )
                    } ?: Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = fillAlpha),
                            lineColor.copy(alpha = fillAlpha * WeatherChartAreaBottomRatio)
                        ),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )
            }

            // segment 별 실선/점선 — KP 의 observed/predicted 표현
            // iOS KP 예측 구간 StrokeStyle(dash: [5, 3])
            val predictedDash = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()), 0f)
            for (i in 0 until screenPoints.size - 1) {
                val isPredictedSegment = predicted.size == dataPoints.size && predicted[i] && predicted[i + 1]
                val segmentColor = weatherLineSegmentColor(
                    lineColor = lineColor,
                    lineSegmentColors = lineSegmentColors,
                    segmentStartIndex = i,
                )
                drawLine(
                    // iOS 는 예측 구간도 같은 색(점선만 다름)으로 그린다.
                    color = segmentColor,
                    start = screenPoints[i],
                    end = screenPoints[i + 1],
                    strokeWidth = lineStrokePx,
                    pathEffect = if (isPredictedSegment) predictedDash else null,
                )
            }
        }

        // 데이터 포인트 원 (옵션) — pointColors 가 명시되면 그 색상으로 표시
        pointColors?.let { colors ->
            screenPoints.forEachIndexed { idx, point ->
                val color = colors.getOrNull(idx) ?: lineColor
                drawCircle(color = color, radius = pointRadiusPx, center = point)
            }
        }

        if (pointLabels.size == screenPoints.size) {
            screenPoints.forEachIndexed { idx, point ->
                drawContext.canvas.nativeCanvas.drawText(
                    pointLabels[idx],
                    point.x,
                    // iOS annotation(position: .top): 점 위쪽으로 약간 띄운다(dp 기준).
                    point.y - pointRadiusPx - 4.dp.toPx(),
                    pointLabelPaint,
                )
            }
        }
        } // clipRect (plot area)

        // 고정 Y축 라벨
        yLabelValues.forEachIndexed { index, v ->
            drawContext.canvas.nativeCanvas.drawText(
                yLabelTexts[index],
                scrollX + leftPadding - labelGapPx,
                toScreenY(v) + axisLabelPx * 0.35f,
                yLabelPaint
            )
        }
    }
}

/** Y축 라벨 값. [step] 이 있으면 yMin 부터 그 간격, 없으면 상/중/하 3개. */
internal fun resolveWeatherChartYLabelValues(
    yMin: Double,
    yMax: Double,
    step: Double?,
): List<Double> {
    if (step != null && step > 0.0) {
        val values = mutableListOf<Double>()
        var v = yMin
        while (v <= yMax + 0.0001) {
            values += v
            v += step
        }
        return values
    }
    // iOS Charts 자동 AxisMarks 처럼 1·2·5 계열의 보기 좋은 간격으로 3~6개 눈금을 고른다.
    val span = yMax - yMin
    if (span <= 0.0) return listOf(yMin)
    val niceStep = resolveWeatherChartNiceStep(span)
    val first = ceil(yMin / niceStep - 1e-9) * niceStep
    val values = mutableListOf<Double>()
    var v = first
    while (v <= yMax + 1e-9) {
        values += if (abs(v) < 1e-9) 0.0 else v
        v += niceStep
    }
    return values
}

internal fun resolveWeatherChartNiceStep(span: Double, maxTicks: Int = 6): Double {
    val rough = span / (maxTicks - 1)
    val magnitude = 10.0.pow(floor(log10(rough)))
    return listOf(1.0, 2.0, 5.0, 10.0).map { it * magnitude }.first { it >= rough - 1e-9 }
}

/** iOS RuleMark StrokeStyle(lineWidth: 1, dash: [5, 5]) — pt 값을 dp 로 그린다(px 로 그리면 고밀도 화면에서 흐려진다). */
private fun DrawScope.drawWeatherThresholdLine(
    y: Float,
    color: Color,
    left: Float,
    right: Float,
) {
    val dash = WeatherChartRuleMarkDash.toPx()
    drawLine(
        color = color,
        start = Offset(left, y),
        end = Offset(right, y),
        strokeWidth = WeatherChartRuleMarkWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
    )
}

private fun DrawScope.drawWeatherCurrentTimeLabel(
    label: String,
    centerX: Float,
    leftBound: Float,
    rightBound: Float,
    textSizePx: Float,
) {
    val horizontalPadding = 6f
    val verticalPadding = 2f
    val labelPaint = Paint().apply {
        color = Color.White.toArgb()
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT
    }
    val labelWidth = labelPaint.measureText(label) + horizontalPadding * 2
    val labelHeight = textSizePx + verticalPadding * 2
    val availableWidth = rightBound - leftBound
    val labelCenterX = if (availableWidth >= labelWidth) {
        centerX.coerceIn(
            leftBound + labelWidth / 2,
            rightBound - labelWidth / 2,
        )
    } else {
        (leftBound + rightBound) / 2
    }
    val labelTop = 2f
    drawRoundRect(
        color = Color.Red,
        topLeft = Offset(labelCenterX - labelWidth / 2, labelTop),
        size = Size(labelWidth, labelHeight),
        cornerRadius = CornerRadius(4f, 4f),
    )
    drawContext.canvas.nativeCanvas.drawText(
        label,
        labelCenterX,
        labelTop + verticalPadding + textSizePx,
        labelPaint,
    )
}

internal fun resolveWeatherChartTopPadding(
    hasPointLabels: Boolean,
    hasCurrentTimeLabel: Boolean,
    axisLabelPx: Float,
): Float {
    val labelPadding = if (hasPointLabels || hasCurrentTimeLabel) axisLabelPx + 12f else 12f
    return maxOf(12f, labelPadding)
}

internal fun shouldDrawWeatherCurrentMarker(
    currentTimeMs: Long?,
    dataStartMs: Long,
    dataEndMs: Long,
): Boolean {
    return currentTimeMs != null && currentTimeMs in dataStartMs..dataEndMs
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
    // iOS AxisMarks(values:) 는 첫 데이터 시각도 라벨을 찍는다(정각이면 포함).
    val startLabel = ((dataStartMs + intervalMs - 1) / intervalMs) * intervalMs
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

/** 가로 스크롤 차트의 스크롤 상태. [WeatherLineChart] 가 Y축을 고정 위치에 그리는 데 쓴다. */
internal val LocalChartHorizontalScroll = staticCompositionLocalOf<ScrollState?> { null }

/**
 * 가로 스크롤 시계열 차트 뷰포트.
 * [initialScrollTimeMs] 를 주면 처음 표시할 때 그 시각이 왼쪽 1/5 지점에 오도록 스크롤한다.
 */
@Composable
internal fun ScrollableTimeChartViewport(
    dataPoints: List<Pair<Long, Double>>,
    visibleDomainMs: Long,
    modifier: Modifier = Modifier,
    initialScrollTimeMs: Long? = null,
    content: @Composable (Modifier) -> Unit,
) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportWidth = maxWidth
        val widthScale = remember(dataPoints, visibleDomainMs) {
            resolveScrollableTimeChartWidthScale(dataPoints, visibleDomainMs)
        }
        val viewportWidthPx = constraints.maxWidth
        LaunchedEffect(dataPoints, initialScrollTimeMs, viewportWidthPx) {
            val target = resolveInitialChartScrollPx(
                dataPoints = dataPoints,
                targetTimeMs = initialScrollTimeMs ?: return@LaunchedEffect,
                contentWidthPx = viewportWidthPx * widthScale,
                viewportWidthPx = viewportWidthPx.toFloat(),
            )
            scrollState.scrollTo(target)
        }
        CompositionLocalProvider(LocalChartHorizontalScroll provides scrollState) {
            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                content(Modifier.width(viewportWidth * widthScale))
            }
        }
    }
}

internal fun resolveInitialChartScrollPx(
    dataPoints: List<Pair<Long, Double>>,
    targetTimeMs: Long,
    contentWidthPx: Float,
    viewportWidthPx: Float,
): Int {
    if (dataPoints.size < 2 || contentWidthPx <= viewportWidthPx) return 0
    val start = dataPoints.first().first
    val end = dataPoints.last().first
    if (end <= start) return 0
    val fraction = ((targetTimeMs - start).toDouble() / (end - start)).coerceIn(0.0, 1.0)
    val target = fraction * contentWidthPx - viewportWidthPx * 0.2
    return target.coerceIn(0.0, (contentWidthPx - viewportWidthPx).toDouble()).toInt()
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

internal const val IosTemperatureChartYStride = 5.0

/**
 * Swift Charts 자동 Y 범위 정합: 양수 데이터는 0 을 포함하고, RuleMark(주의/위험선) 값도 범위에 넣어
 * 데이터가 낮을 때도 기준선이 항상 보이게 한다. 위쪽만 10% 여유를 둔다.
 */
internal fun resolveWeatherChartAutoYRange(
    values: List<Double>,
    thresholds: List<Double>,
): Pair<Double, Double> {
    val all = values + thresholds
    val rawMin = minOf(0.0, all.minOrNull() ?: 0.0)
    val rawMax = maxOf(rawMin + 1.0, all.maxOrNull() ?: 1.0)
    return rawMin to rawMax + (rawMax - rawMin) * 0.1
}
internal val WeatherChartDefaultPointRadius = 3.dp
internal const val WeatherChartAreaTopAlpha = 0.3f
internal const val WeatherChartAreaBottomRatio = 1f / 3f
/** iOS AxisGridLine: 가로·세로 같은 옅은 선 */
internal const val WeatherChartGridAlpha = 0.1f
internal val WeatherChartRuleMarkWidth = 1.dp
internal val WeatherChartRuleMarkDash = 5.dp
/** iOS 온도 PointMark symbolSize(60) ≈ 지름 7.7pt */
internal val IosTemperatureChartPointRadius = 4.dp
internal const val IosWeatherValueGradientAreaAlpha = 0.3f

/** iOS calculateTemperatureGradientStops 의 온도 기준점. 영역 채우기를 Y축 값에 맞춘 색 그라데이션으로 그린다. */
internal val IosTemperatureAreaGradientStops: List<Pair<Double, Color>> = listOf(
    -10.0 to Color(0xFF0072FF),
    7.5 to Color(0xFF00C8FF),
    17.5 to Color(0xFFA6E22E),
    27.5 to Color(0xFFFFA500),
    40.0 to Color(0xFFFF3B30),
)

/** 값 기준 색 정지점을 Y축 범위에 맞춰 세로 그라데이션으로 바꾼다(범위 밖 정지점은 끝에 붙인다). */
internal fun weatherValueGradientBrush(
    stops: List<Pair<Double, Color>>,
    yMin: Double,
    yMax: Double,
    top: Float,
    bottom: Float,
    alpha: Float,
): Brush {
    val span = (yMax - yMin).takeIf { it > 0.0 } ?: 1.0
    val colorStops = stops
        .map { (value, color) -> ((yMax - value) / span).toFloat().coerceIn(0f, 1f) to color.copy(alpha = alpha) }
        .sortedBy { it.first }
        .toTypedArray()
    return Brush.verticalGradient(colorStops = colorStops, startY = top, endY = bottom)
}

internal fun resolveIosTemperatureYRange(temperatures: List<Double>): ClosedFloatingPointRange<Double> {
    if (temperatures.isEmpty()) return -30.0..50.0

    val minTemp = temperatures.minOrNull() ?: 0.0
    val maxTemp = temperatures.maxOrNull() ?: 30.0
    val yMin = maxOf(-30.0, floor((minTemp - 5.0) / 5.0) * 5.0)
    val yMax = minOf(50.0, ceil((maxTemp + 5.0) / 5.0) * 5.0)

    return if (yMin <= yMax) yMin..yMax else -30.0..50.0
}

internal fun temperatureChartThresholdLines(): List<WeatherThresholdLine> = listOf(
    WeatherThresholdLine(IosTemperatureLowCautionC, IosWeatherRuleMarkBlue),
    WeatherThresholdLine(IosTemperatureHighCautionC, IosWeatherRuleMarkOrange),
)

internal fun windSpeedChartThresholdLines(category: DroneCategory): List<WeatherThresholdLine> {
    val (cautionThreshold, dangerThreshold) = iosWindSpeedThresholds(category)
    return listOf(
        WeatherThresholdLine(cautionThreshold, IosWeatherRuleMarkOrange),
        WeatherThresholdLine(dangerThreshold, IosWeatherRuleMarkRed),
    )
}

internal fun gustDifferenceChartThresholdLines(category: DroneCategory): List<WeatherThresholdLine> {
    val (cautionThreshold, dangerThreshold) = iosGustDifferenceThresholds(category)
    return listOf(
        WeatherThresholdLine(cautionThreshold, IosWeatherRuleMarkOrange),
        WeatherThresholdLine(dangerThreshold, IosWeatherRuleMarkRed),
    )
}

internal fun visibilityChartThresholdLines(): List<WeatherThresholdLine> = listOf(
    WeatherThresholdLine(IosVisibilityPoorKm, IosWeatherRuleMarkRed),
    WeatherThresholdLine(IosVisibilityGoodKm, IosWeatherRuleMarkGreen),
)

internal fun criChartThresholdLines(): List<WeatherThresholdLine> = listOf(
    WeatherThresholdLine(IosCriModerate, IosWeatherRuleMarkYellow),
    WeatherThresholdLine(IosCriHigh, IosWeatherRuleMarkRed),
)

// ─── 개별 차트 6종 ────────────────────────────────────────────

@Composable
fun TemperatureChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = hourlyData.map { it.time to it.temperature }
    val pointColors = hourlyData.map { interpolateTemperatureColor(it.temperature) }
    val yAxisRange = resolveIosTemperatureYRange(hourlyData.map { it.temperature })
    val thresholdLines = temperatureChartThresholdLines()
    val forecastPeriodLabel = weatherForecastPeriodLabel()
    ChartCard(
        title = stringResource(R.string.weather_chart_temperature, forecastPeriodLabel),
        unitLabel = stringResource(R.string.weather_unit_celsius),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = Color(0xFFFF6B35),
                yAxisRange = yAxisRange,
                thresholdLines = thresholdLines,
                yAxisLabel = "\u00B0C",
                // iOS: Y축은 5 간격, 라벨은 숫자만("%.0f").
                formatValue = { String.format(Locale.ROOT, "%.0f", it) },
                yLabelStep = IosTemperatureChartYStride,
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = pointColors,
                lineSegmentColors = pointColors,
                fillValueGradient = IosTemperatureAreaGradientStops,
                pointRadius = IosTemperatureChartPointRadius,
            )
        }
        WeatherChartLegend(
            items = listOf(
                WeatherChartLegendItem(
                    color = thresholdLines[0].color,
                    text = stringResource(
                        R.string.weather_legend_low_temp,
                        IosTemperatureLowCautionC.toInt(),
                    ),
                ),
                WeatherChartLegendItem(
                    color = thresholdLines[1].color,
                    text = stringResource(
                        R.string.weather_legend_high_temp,
                        IosTemperatureHighCautionC.toInt(),
                    ),
                ),
            ),
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
    val thresholdLines = windSpeedChartThresholdLines(category)
    val lineColor = IosWeatherWindSpeedChartColor
    val forecastPeriodLabel = weatherForecastPeriodLabel()
    ChartCard(
        title = stringResource(R.string.weather_chart_wind_speed, forecastPeriodLabel),
        unitLabel = stringResource(R.string.weather_unit_mps),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                thresholdLines = thresholdLines,
                yAxisLabel = "m/s",
                formatValue = { String.format(Locale.ROOT, "%.0f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
        WeatherChartLegend(
            items = listOf(
                WeatherChartLegendItem(
                    color = thresholdLines[0].color,
                    text = stringResource(R.string.weather_legend_caution, thresholdLines[0].value),
                ),
                WeatherChartLegendItem(
                    color = thresholdLines[1].color,
                    text = stringResource(R.string.weather_legend_danger, thresholdLines[1].value),
                ),
            ),
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
    val thresholdLines = gustDifferenceChartThresholdLines(category)
    val lineColor = IosWeatherGustDifferenceChartColor
    val forecastPeriodLabel = weatherForecastPeriodLabel()
    ChartCard(
        title = stringResource(R.string.weather_chart_gust_difference, forecastPeriodLabel),
        unitLabel = stringResource(R.string.weather_unit_mps),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                thresholdLines = thresholdLines,
                yAxisLabel = "m/s",
                formatValue = { String.format(Locale.ROOT, "%.0f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
        WeatherChartLegend(
            items = listOf(
                WeatherChartLegendItem(
                    color = thresholdLines[0].color,
                    text = stringResource(R.string.weather_legend_caution, thresholdLines[0].value),
                ),
                WeatherChartLegendItem(
                    color = thresholdLines[1].color,
                    text = stringResource(R.string.weather_legend_danger, thresholdLines[1].value),
                ),
            ),
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
    val lineColor = IosWeatherPrecipitationChartColor
    val yMax = resolveIosPrecipitationYMax(dataPoints.map { it.second })
    val forecastPeriodLabel = weatherForecastPeriodLabel()
    ChartCard(
        title = stringResource(R.string.weather_chart_precipitation, forecastPeriodLabel),
        unitLabel = stringResource(R.string.weather_unit_mmph),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
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
    val thresholdLines = visibilityChartThresholdLines()
    val forecastPeriodLabel = weatherForecastPeriodLabel()
    ChartCard(
        title = stringResource(R.string.weather_chart_visibility, forecastPeriodLabel),
        unitLabel = stringResource(R.string.weather_unit_km),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                thresholdLines = thresholdLines,
                yAxisLabel = "km",
                formatValue = { String.format(Locale.ROOT, "%.0f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
        WeatherChartLegend(
            items = listOf(
                WeatherChartLegendItem(
                    color = thresholdLines[0].color,
                    text = stringResource(R.string.weather_legend_poor, IosVisibilityPoorKm.toInt()),
                ),
                WeatherChartLegendItem(
                    color = thresholdLines[1].color,
                    text = stringResource(R.string.weather_legend_good, IosVisibilityGoodKm.toInt()),
                ),
            ),
        )
    }
}

@Composable
fun CriChart(
    hourlyData: List<HourlyWeatherData>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val dataPoints = criChartDataPoints(hourlyData)
    val lineColor = IosWeatherCriChartColor
    val thresholdLines = criChartThresholdLines()
    val forecastPeriodLabel = weatherForecastPeriodLabel()
    ChartCard(
        title = stringResource(R.string.weather_chart_cri, forecastPeriodLabel),
        unitLabel = stringResource(R.string.weather_unit_custom),
        modifier = modifier,
    ) {
        ScrollableTimeChartViewport(dataPoints, IosWeatherChartVisibleDomainMs) { chartModifier ->
            WeatherLineChart(
                dataPoints = dataPoints,
                modifier = chartModifier,
                lineColor = lineColor,
                yAxisRange = IosWeatherCriChartYRange,
                thresholdLines = thresholdLines,
                yAxisLabel = "%",
                formatValue = { String.format(Locale.ROOT, "%.0f", it) },
                xLabelIntervalMs = IosWeatherChartXLabelIntervalMs,
                currentTimeMs = resolveWeatherChartCurrentTimeMarkerMs(dataPoints, nowMillis),
                pointColors = weatherChartPointColors(dataPoints.size, lineColor),
            )
        }
        WeatherChartLegend(
            items = listOf(
                WeatherChartLegendItem(
                    color = thresholdLines[0].color,
                    text = stringResource(R.string.weather_legend_cri_caution),
                ),
                WeatherChartLegendItem(
                    color = thresholdLines[1].color,
                    text = stringResource(R.string.weather_legend_cri_warning),
                ),
            ),
        )
    }
}

internal fun criChartDataPoints(hourlyData: List<HourlyWeatherData>): List<Pair<Long, Double>> =
    hourlyData.mapNotNull { hour ->
        hour.cri?.takeIf { it.isFinite() }?.let { hour.time to it }
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
    unitLabel: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IosWeatherChartCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = IosWeatherChartCardContainerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IosWeatherChartCardPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = IosWeatherChartCardSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    // iOS .title3 semibold
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                unitLabel?.let { label ->
                    Text(
                        text = label,
                        // iOS .caption
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun weatherForecastPeriodLabel(): String {
    return if (WeatherForecastDays == 1) {
        stringResource(R.string.weather_forecast_hours, WeatherForecastChartHours)
    } else {
        stringResource(R.string.weather_forecast_days, WeatherForecastDays)
    }
}

@Composable
private fun WeatherChartLegend(
    items: List<WeatherChartLegendItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 23.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            WeatherChartLegendItemView(item)
        }
    }
}

@Composable
private fun WeatherChartLegendItemView(item: WeatherChartLegendItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(item.color),
        )
        Text(
            text = item.text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
