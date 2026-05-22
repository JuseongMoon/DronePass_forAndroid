package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.nextSunEvent
import java.time.LocalTime

private val SunEventColor = Color(0xFFFF9500) // iOS .orange

// iOS daytime gradient (orange.opacity(0.8) → yellow.opacity(0.6))
private val DaytimeGradientColors = listOf(
    Color(0xFFFF9500).copy(alpha = 0.85f),
    Color(0xFFFFCC00).copy(alpha = 0.65f),
)

// iOS night gradient (indigo.opacity(0.8) → purple.opacity(0.6))
private val NighttimeGradientColors = listOf(
    Color(0xFF5856D6).copy(alpha = 0.85f),
    Color(0xFFAF52DE).copy(alpha = 0.65f),
)

/**
 * iOS `WeatherForecastView.sunriseSunsetCard` 동등 이식.
 * 일출/일몰 타임라인 + 정오/자정 마커 + 원형 현재 시간 뱃지 + 남은 시간 표시.
 */
@Composable
fun SunTimeline(
    sunrise: String?,
    sunset: String?,
    modifier: Modifier = Modifier,
) {
    val sunriseTime = remember(sunrise) { parseTime(sunrise) }
    val sunsetTime = remember(sunset) { parseTime(sunset) }
    if (sunriseTime == null || sunsetTime == null) return

    val now = remember { LocalTime.now() }
    val isDaytime = !now.isBefore(sunriseTime) && now.isBefore(sunsetTime)
    val nextEvent = nextSunEvent(sunrise, sunset)

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
            // Title row (iOS: title3 semibold secondary)
            Text(
                text = stringResource(R.string.weather_section_sunrise_sunset),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TimelineProgressBar(
                    isDaytime = isDaytime,
                    sunriseTime = sunriseTime,
                    sunsetTime = sunsetTime,
                    now = now,
                )

                // 남은 시간 (HStack)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = SunEventColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(
                            if (isDaytime) R.string.weather_until_sunset
                            else R.string.weather_until_sunrise,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = nextEvent.timeUntilFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SunEventColor,
                    )
                    Text(
                        text = stringResource(R.string.weather_remaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineProgressBar(
    isDaytime: Boolean,
    sunriseTime: LocalTime,
    sunsetTime: LocalTime,
    now: LocalTime,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 시작 측 (낮: 일출 / 밤: 일몰)
        SideIconTime(
            iconDaytime = isDaytime,  // 낮이면 sunrise(WbSunny), 밤이면 sunset(Bedtime)
            time = if (isDaytime) sunriseTime else sunsetTime,
        )

        // 가운데 프로그레스 바
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            val totalWidth = maxWidth
            val progress = calculateProgress(isDaytime, sunriseTime, sunsetTime, now)
            val marker = calculateNoonMidnightMarker(isDaytime, sunriseTime, sunsetTime)

            // 배경 라인 (gray.opacity(0.3), 2dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.Gray.copy(alpha = 0.3f)),
            )

            // 정오/자정 마커 (Diamond + 수직선 + 라벨)
            if (marker != null) {
                val markerX = totalWidth * marker.progress - totalWidth / 2
                Column(
                    modifier = Modifier.offset(x = markerX),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Diamond (8x8 회전된 사각형)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .rotate(45f)
                            .background(Color.Gray.copy(alpha = 0.5f)),
                    )
                    // Vertical line (1x20)
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                    )
                    // Label
                    Text(
                        text = stringResource(marker.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 현재 시간 원형 뱃지 (Circle 36dp, gradient, shadow)
            val badgeX = totalWidth * progress - totalWidth / 2
            Box(
                modifier = Modifier
                    .offset(x = badgeX)
                    .size(36.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = if (isDaytime) SunEventColor else Color(0xFF5856D6),
                        spotColor = if (isDaytime) SunEventColor else Color(0xFF5856D6),
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDaytime) DaytimeGradientColors else NighttimeGradientColors,
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isDaytime) Icons.Default.WbSunny else Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // 끝 측 (낮: 일몰 / 밤: 일출(내일))
        SideIconTime(
            iconDaytime = !isDaytime,  // 낮이면 sunset(Bedtime), 밤이면 sunrise(WbSunny)
            time = if (isDaytime) sunsetTime else sunriseTime,
        )
    }
}

@Composable
private fun SideIconTime(
    iconDaytime: Boolean,
    time: LocalTime,
) {
    Column(
        modifier = Modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (iconDaytime) Icons.Default.WbSunny else Icons.Default.Bedtime,
            contentDescription = null,
            tint = SunEventColor,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = formatHourMinute(time),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class NoonMidnightMarker(
    val progress: Float,
    val labelRes: Int,
)

/**
 * iOS `calculateNoonMidnightMarker`:
 *  - 낮이면 정오(12:00) 위치, 밤이면 자정(00:00) 위치를 반환.
 *  - progressBar 의 0.0~1.0 범위로 정규화. 범위 밖이면 null.
 */
private fun calculateNoonMidnightMarker(
    isDaytime: Boolean,
    sunriseTime: LocalTime,
    sunsetTime: LocalTime,
): NoonMidnightMarker? {
    if (isDaytime) {
        val sunriseSec = sunriseTime.toSecondOfDay()
        val sunsetSec = sunsetTime.toSecondOfDay()
        val noonSec = 12 * 3600
        val span = sunsetSec - sunriseSec
        if (span <= 0) return null
        val progress = (noonSec - sunriseSec).toFloat() / span
        if (progress !in 0f..1f) return null
        return NoonMidnightMarker(progress, R.string.weather_noon)
    } else {
        // 밤: 일몰 ~ 다음날 일출 (24h 보정)
        val sunsetSec = sunsetTime.toSecondOfDay()
        val sunriseSec = sunriseTime.toSecondOfDay() + 24 * 3600
        val midnightSec = 24 * 3600
        val span = sunriseSec - sunsetSec
        if (span <= 0) return null
        val progress = (midnightSec - sunsetSec).toFloat() / span
        if (progress !in 0f..1f) return null
        return NoonMidnightMarker(progress, R.string.weather_midnight)
    }
}

/**
 * 현재 시간이 일출~일몰(낮) 또는 일몰~다음일출(밤) 구간에서 차지하는 위치 (0.0~1.0).
 */
private fun calculateProgress(
    isDaytime: Boolean,
    sunriseTime: LocalTime,
    sunsetTime: LocalTime,
    now: LocalTime,
): Float {
    val nowSec = now.toSecondOfDay()
    return if (isDaytime) {
        val span = sunsetTime.toSecondOfDay() - sunriseTime.toSecondOfDay()
        if (span <= 0) 0.5f
        else ((nowSec - sunriseTime.toSecondOfDay()).toFloat() / span).coerceIn(0f, 1f)
    } else {
        val sunsetSec = sunsetTime.toSecondOfDay()
        val sunriseSecNext = sunriseTime.toSecondOfDay() + 24 * 3600
        val span = sunriseSecNext - sunsetSec
        if (span <= 0) 0.5f
        val nowAdjusted = if (nowSec < sunsetSec) nowSec + 24 * 3600 else nowSec
        ((nowAdjusted - sunsetSec).toFloat() / span).coerceIn(0f, 1f)
    }
}

/**
 * ISO "2026-02-24T06:45" -> LocalTime(06:45).
 */
private fun parseTime(iso: String?): LocalTime? {
    if (iso.isNullOrBlank()) return null
    return try {
        val timePart = iso.substringAfter('T', missingDelimiterValue = iso)
        LocalTime.parse(timePart.take(5))
    } catch (e: Exception) {
        null
    }
}

private fun formatHourMinute(time: LocalTime): String =
    "%02d:%02d".format(time.hour, time.minute)
