package com.ScienceFiction.DronePassAndroid.feature.weather

import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceLevel
import com.ScienceFiction.DronePassAndroid.core.util.WeatherCodeMapper
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData

/**
 * 지도 화면에 표시되는 작은 날씨 요약 오버레이 카드
 * 탭 시 WeatherForecastScreen으로 이동
 */
@Composable
fun WeatherOverlayCard(
    currentWeather: CurrentWeatherData?,
    sunrise: String?,
    sunset: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentWeather == null) return

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 날씨 아이콘
                Icon(
                    imageVector = WeatherCodeMapper.weatherCodeToIcon(currentWeather.weatherCode),
                    contentDescription = WeatherCodeMapper.weatherCodeToDescription(currentWeather.weatherCode),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 온도
                Text(
                    text = "${String.format(Locale.ROOT, "%.0f", currentWeather.temperature)}\u00B0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 풍속
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = stringResource(R.string.weather_wind_speed),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${String.format(Locale.ROOT, "%.1f", currentWeather.windSpeed)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 돌풍 레벨 인디케이터
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(currentWeather.gustDifferenceLevel.colorLong),
                            RoundedCornerShape(4.dp)
                        )
                )
            }

            // 일출/일몰 정보
            if (sunrise != null || sunset != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sunrise?.let {
                        val time = formatOverlaySunTime(it)
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = stringResource(R.string.weather_overlay_sunrise),
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (sunrise != null && sunset != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    sunset?.let {
                        val time = formatOverlaySunTime(it)
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.weather_overlay_sunset),
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF5C6BC0)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * ISO 날짜시간 문자열에서 시간만 추출
 * "2025-10-12T06:15" -> "06:15"
 */
private fun formatOverlaySunTime(isoTime: String): String {
    return try {
        val parts = isoTime.split("T")
        if (parts.size == 2) parts[1] else isoTime
    } catch (e: Exception) {
        isoTime
    }
}
