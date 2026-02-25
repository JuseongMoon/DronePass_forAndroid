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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import java.util.Calendar

private val NightColor = Color(0xFF1A237E)
private val SunriseMarkerColor = Color(0xFFFF8F00)
private val SunsetMarkerColor = Color(0xFF1A237E)
private val CurrentOuterColor = Color.White
private val CurrentInnerColor = Color(0xFFFF8F00)

/**
 * 일출/일몰 타임라인 프로그레스 바
 */
@Composable
fun SunTimeline(
    sunrise: String?,
    sunset: String?,
    modifier: Modifier = Modifier
) {
    if (sunrise == null && sunset == null) return

    // ISO "2026-02-24T06:45" -> 시간 파싱
    val sunriseMinutes = parseTimeToMinutes(sunrise)
    val sunsetMinutes = parseTimeToMinutes(sunset)

    if (sunriseMinutes == null && sunsetMinutes == null) return

    val sunriseTime = sunrise?.let { formatSunTimeDisplay(it) } ?: ""
    val sunsetTime = sunset?.let { formatSunTimeDisplay(it) } ?: ""

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

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
                text = stringResource(R.string.weather_sun_timeline),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val totalMinutes = 24 * 60f
                val barHeight = 12.dp.toPx()
                val barY = 8.dp.toPx()
                val barRadius = barHeight / 2

                val sunriseX = sunriseMinutes?.let { it / totalMinutes * size.width } ?: 0f
                val sunsetX = sunsetMinutes?.let { it / totalMinutes * size.width } ?: size.width

                // 밤 구간: 0시~일출
                if (sunriseMinutes != null && sunriseX > 0f) {
                    drawRoundRect(
                        color = NightColor,
                        topLeft = Offset(0f, barY),
                        size = Size(sunriseX, barHeight),
                        cornerRadius = CornerRadius(barRadius, barRadius)
                    )
                }

                // 낮 구간: 일출~일몰 (그라데이션)
                if (sunriseMinutes != null && sunsetMinutes != null) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFB300),
                                Color(0xFF81D4FA),
                                Color(0xFFFF8F00)
                            ),
                            startX = sunriseX,
                            endX = sunsetX
                        ),
                        topLeft = Offset(sunriseX, barY),
                        size = Size(sunsetX - sunriseX, barHeight),
                        cornerRadius = CornerRadius(barRadius, barRadius)
                    )
                }

                // 밤 구간: 일몰~24시
                if (sunsetMinutes != null && sunsetX < size.width) {
                    drawRoundRect(
                        color = NightColor,
                        topLeft = Offset(sunsetX, barY),
                        size = Size(size.width - sunsetX, barHeight),
                        cornerRadius = CornerRadius(barRadius, barRadius)
                    )
                }

                // 일출 마커
                if (sunriseMinutes != null) {
                    val markerRadius = 8.dp.toPx()
                    val markerCenterY = barY + barHeight / 2
                    drawCircle(
                        color = SunriseMarkerColor,
                        radius = markerRadius,
                        center = Offset(sunriseX, markerCenterY)
                    )
                }

                // 일몰 마커
                if (sunsetMinutes != null) {
                    val markerRadius = 8.dp.toPx()
                    val markerCenterY = barY + barHeight / 2
                    drawCircle(
                        color = SunsetMarkerColor,
                        radius = markerRadius,
                        center = Offset(sunsetX, markerCenterY)
                    )
                }

                // 현재 시간 마커
                val now = Calendar.getInstance()
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60f + now.get(Calendar.MINUTE)
                val currentX = currentMinutes / totalMinutes * size.width
                val markerCenterY = barY + barHeight / 2
                val outerRadius = 12.dp.toPx() / 2
                val innerRadius = 10.dp.toPx() / 2

                drawCircle(
                    color = CurrentOuterColor,
                    radius = outerRadius,
                    center = Offset(currentX, markerCenterY)
                )
                drawCircle(
                    color = CurrentInnerColor,
                    radius = innerRadius,
                    center = Offset(currentX, markerCenterY)
                )

                // 아래 라벨
                val labelY = barY + barHeight + 24.dp.toPx()

                val labelPaint = Paint().apply {
                    color = onSurfaceVariant.copy(alpha = 0.8f).toArgb()
                    textSize = 24f
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT
                }

                // 일출 라벨 (왼쪽)
                if (sunriseMinutes != null && sunriseTime.isNotEmpty()) {
                    labelPaint.textAlign = Paint.Align.LEFT
                    val sunLabel = "\u2600 $sunriseTime"
                    val labelX = (sunriseX - 10f).coerceAtLeast(0f)
                    drawContext.canvas.nativeCanvas.drawText(
                        sunLabel,
                        labelX,
                        labelY,
                        labelPaint
                    )
                }

                // 현재 시간 라벨 (중앙)
                labelPaint.textAlign = Paint.Align.CENTER
                drawContext.canvas.nativeCanvas.drawText(
                    "\u25CF \uD604\uC7AC",  // ● 현재
                    currentX.coerceIn(40f, size.width - 40f),
                    labelY,
                    labelPaint
                )

                // 일몰 라벨 (오른쪽)
                if (sunsetMinutes != null && sunsetTime.isNotEmpty()) {
                    labelPaint.textAlign = Paint.Align.RIGHT
                    val moonLabel = "\uD83C\uDF19 $sunsetTime"
                    val labelXEnd = (sunsetX + 10f).coerceAtMost(size.width)
                    drawContext.canvas.nativeCanvas.drawText(
                        moonLabel,
                        labelXEnd,
                        labelY,
                        labelPaint
                    )
                }
            }
        }
    }
}

/**
 * ISO "2026-02-24T06:45" -> 분 단위 (6*60+45 = 405)
 */
private fun parseTimeToMinutes(isoTime: String?): Float? {
    if (isoTime == null) return null
    return try {
        val timePart = isoTime.split("T").getOrNull(1) ?: return null
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            hours * 60f + minutes
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * ISO "2026-02-24T06:45" -> "06:45"
 */
private fun formatSunTimeDisplay(isoTime: String): String {
    return try {
        val timePart = isoTime.split("T").getOrNull(1) ?: isoTime
        timePart
    } catch (e: Exception) {
        isoTime
    }
}
