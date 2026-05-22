package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.core.util.WeatherCodeMapper
import com.ScienceFiction.DronePassAndroid.core.util.interpolateTemperatureColor
import com.ScienceFiction.DronePassAndroid.core.util.nextSunEvent
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import java.util.Locale

// iOS MainFloatingButtonView.weatherWindIconButton 의 색상을 그대로 매핑.
private val WeatherIconColor = Color(0xFF007AFF) // iOS .blue
private val WindArrowColor = Color(0xFF30B0C0) // iOS .teal
private val SunEventColor = Color(0xFFFF9500) // iOS .orange

/**
 * 지도 화면 우측 하단의 날씨/풍향/일출일몰 floating 카드.
 * iOS `MainFloatingButtonView.weatherWindIconButton` 와 1:1 매핑.
 *
 *  - 윗줄: `날씨 아이콘(.blue)` + `풍향 화살표(.teal, windDir+180° 회전)` + `온도(6단계 그라데이션 색)`
 *  - 아랫줄: `다음 sun event 아이콘(.orange)` + `남은 시간 "HH:MM"(.orange)`
 *
 * `currentWeather` 가 null 이어도 카드 자체는 항상 표시되고 값은 "-" / "--:--" 로 폴백한다
 * (iOS WeatherManager 의 nil 처리와 동일).
 */
@Composable
fun WeatherOverlayCard(
    currentWeather: CurrentWeatherData?,
    sunrise: String?,
    sunset: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sunEvent = nextSunEvent(sunrise, sunset)
    val temperatureText = currentWeather?.let {
        String.format(Locale.ROOT, "%.0f°", it.temperature)
    } ?: "-"
    val windRotationDegrees = ((currentWeather?.windDirection ?: 0.0) + 180.0).toFloat()
    val temperatureColor = interpolateTemperatureColor(currentWeather?.temperature)

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White, // iOS Color(UIColor.systemBackground) 라이트 모드
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 윗줄: 날씨 + 풍향 + 온도 — iOS HStack(spacing: 8)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = WeatherCodeMapper.weatherCodeToIcon(
                        currentWeather?.weatherCode ?: 0,
                    ),
                    contentDescription = null,
                    tint = WeatherIconColor,
                    modifier = Modifier.size(21.dp),
                )
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = WindArrowColor,
                    modifier = Modifier
                        .size(21.dp)
                        .rotate(windRotationDegrees),
                )
                Text(
                    text = temperatureText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = temperatureColor,
                )
            }

            // 아랫줄: 다음 sun event 아이콘 + 남은 시간 — iOS HStack(spacing: 8)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (sunEvent.isNextSunset) {
                        Icons.Default.NightlightRound
                    } else {
                        Icons.Default.WbTwilight
                    },
                    contentDescription = null,
                    tint = SunEventColor,
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    text = sunEvent.timeUntilFormatted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunEventColor,
                )
            }
        }
    }
}
