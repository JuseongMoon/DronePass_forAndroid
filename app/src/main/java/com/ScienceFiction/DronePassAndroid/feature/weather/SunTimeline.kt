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
import java.time.Duration
import java.time.LocalDateTime
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
    sunriseTimes: List<String> = sunrise?.let(::listOf) ?: emptyList(),
    sunsetTimes: List<String> = sunset?.let(::listOf) ?: emptyList(),
    utcOffsetSeconds: Int? = null,
    modifier: Modifier = Modifier,
) {
    val nowDateTime = rememberSunEventNow(utcOffsetSeconds)
    val timelineState = remember(sunrise, sunset, sunriseTimes, sunsetTimes, utcOffsetSeconds, nowDateTime) {
        resolveSunTimelineState(
            sunriseIsoList = sunriseTimes.ifEmpty { sunrise?.let(::listOf).orEmpty() },
            sunsetIsoList = sunsetTimes.ifEmpty { sunset?.let(::listOf).orEmpty() },
            now = nowDateTime,
        )
    } ?: return

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
                    timelineState = timelineState,
                    now = nowDateTime,
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
                            if (timelineState.nextEvent.isNextSunset) R.string.weather_until_sunset
                            else R.string.weather_until_sunrise,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = timelineState.nextEvent.timeUntilFormatted,
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
    timelineState: SunTimelineState,
    now: LocalDateTime,
) {
    val isDaytime = timelineState.isDaytime
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
            time = timelineState.startDateTime.toLocalTime(),
        )

        // 가운데 프로그레스 바
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            val totalWidth = maxWidth
            val progress = calculateSunTimelineProgress(timelineState, now)
            val marker = resolveSunTimelineMarker(timelineState)

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
            time = timelineState.endDateTime.toLocalTime(),
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

internal data class NoonMidnightMarker(
    val progress: Float,
    val labelRes: Int,
)

internal data class SunTimelineState(
    val isDaytime: Boolean,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val nextEvent: com.ScienceFiction.DronePassAndroid.core.util.NextSunEvent,
)

internal fun resolveSunTimelineState(
    sunriseIsoList: List<String>,
    sunsetIsoList: List<String>,
    now: LocalDateTime,
): SunTimelineState? {
    val sunrises = sunriseIsoList.mapNotNull(::parseDateTime)
    val sunsets = sunsetIsoList.mapNotNull(::parseDateTime)
    val todaySunrise = sunrises.firstOrNull { it.toLocalDate() == now.toLocalDate() }
    val todaySunset = sunsets.firstOrNull { it.toLocalDate() == now.toLocalDate() }

    if (todaySunrise != null && todaySunset != null &&
        !now.isBefore(todaySunrise) && now.isBefore(todaySunset)
    ) {
        return SunTimelineState(
            isDaytime = true,
            startDateTime = todaySunrise,
            endDateTime = todaySunset,
            nextEvent = nextSunEvent(sunriseIsoList, sunsetIsoList, now),
        )
    }

    val nextSunrise = sunrises
        .filter { it.isAfter(now) }
        .minByOrNull { it }
    if (nextSunrise != null) {
        val previousSunset = sunsets
            .filter { !it.isAfter(now) }
            .maxByOrNull { it }
            ?: sunsets.firstOrNull { it.toLocalDate() == nextSunrise.toLocalDate() }?.minusDays(1)
            ?: return null

        return SunTimelineState(
            isDaytime = false,
            startDateTime = previousSunset,
            endDateTime = nextSunrise,
            nextEvent = nextSunEvent(sunriseIsoList, sunsetIsoList, now),
        )
    }

    return resolveSunTimelineStateFromTimes(
        sunriseIso = sunriseIsoList.firstOrNull(),
        sunsetIso = sunsetIsoList.firstOrNull(),
        now = now,
    )
}

/**
 * iOS `calculateNoonMidnightMarker`:
 *  - 낮이면 정오(12:00) 위치, 밤이면 자정(00:00) 위치를 반환.
 *  - progressBar 의 0.0~1.0 범위로 정규화. 범위 밖이면 null.
 */
internal fun resolveSunTimelineMarker(state: SunTimelineState): NoonMidnightMarker? {
    val target = if (state.isDaytime) {
        state.startDateTime.toLocalDate().atTime(12, 0)
    } else {
        state.endDateTime.toLocalDate().atStartOfDay()
    }
    if (target.isBefore(state.startDateTime) || target.isAfter(state.endDateTime)) return null

    val spanMinutes = Duration.between(state.startDateTime, state.endDateTime).toMinutes()
    if (spanMinutes <= 0) return null
    val targetMinutes = Duration.between(state.startDateTime, target).toMinutes()
    val progress = targetMinutes.toFloat() / spanMinutes
    if (progress !in 0f..1f) return null
    return NoonMidnightMarker(
        progress = progress,
        labelRes = if (state.isDaytime) R.string.weather_noon else R.string.weather_midnight,
    )
}

/**
 * 현재 시간이 일출~일몰(낮) 또는 일몰~다음일출(밤) 구간에서 차지하는 위치 (0.0~1.0).
 */
internal fun calculateSunTimelineProgress(state: SunTimelineState, now: LocalDateTime): Float {
    val spanMillis = Duration.between(state.startDateTime, state.endDateTime).toMillis()
    if (spanMillis <= 0L) return 0.5f
    val currentMillis = Duration.between(state.startDateTime, now).toMillis()
    return (currentMillis.toFloat() / spanMillis).coerceIn(0f, 1f)
}

private fun resolveSunTimelineStateFromTimes(
    sunriseIso: String?,
    sunsetIso: String?,
    now: LocalDateTime,
): SunTimelineState? {
    val sunriseTime = parseTime(sunriseIso) ?: return null
    val sunsetTime = parseTime(sunsetIso) ?: return null
    val todaySunrise = now.toLocalDate().atTime(sunriseTime)
    val todaySunset = now.toLocalDate().atTime(sunsetTime)

    return when {
        !now.isBefore(todaySunrise) && now.isBefore(todaySunset) -> SunTimelineState(
            isDaytime = true,
            startDateTime = todaySunrise,
            endDateTime = todaySunset,
            nextEvent = nextSunEvent(sunriseIso, sunsetIso, now),
        )
        now.isBefore(todaySunrise) -> SunTimelineState(
            isDaytime = false,
            startDateTime = todaySunset.minusDays(1),
            endDateTime = todaySunrise,
            nextEvent = nextSunEvent(sunriseIso, sunsetIso, now),
        )
        else -> SunTimelineState(
            isDaytime = false,
            startDateTime = todaySunset,
            endDateTime = todaySunrise.plusDays(1),
            nextEvent = nextSunEvent(sunriseIso, sunsetIso, now),
        )
    }
}

private fun parseDateTime(iso: String?): LocalDateTime? {
    if (iso.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse(iso)
    } catch (_: Exception) {
        null
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
