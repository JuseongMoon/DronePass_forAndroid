package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherOverlayCard

@Composable
fun MapFloatingButtons(
    onCreateShape: () -> Unit,
    onEnterSketchMode: () -> Unit,
    onShowFlightZoneLayers: () -> Unit = {},
    flightZoneLayersActive: Boolean = false,
    showFlightZoneLayerButton: Boolean = true,
    currentKpValue: Double? = null,
    kpLevelColor: Color = Color.Gray,
    onShowKpForecast: () -> Unit = {},
    // 날씨 카드 관련 파라미터
    currentWeather: CurrentWeatherData? = null,
    sunrise: String? = null,
    sunset: String? = null,
    onWeatherClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // 스케치 모드 진입 FAB
        SmallFloatingActionButton(
            onClick = onEnterSketchMode,
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = Color.Black
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.map_fab_sketch),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // KP 지수 버튼
        Surface(
            onClick = onShowKpForecast,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (currentKpValue != null) "Kp ${String.format("%.1f", currentKpValue)}" else "Kp --",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(kpLevelColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 날씨 오버레이 카드 (KP 아래)
        WeatherOverlayCard(
            currentWeather = currentWeather,
            sunrise = sunrise,
            sunset = sunset,
            onClick = onWeatherClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 비행구역 레이어 FAB - 설정에서 활성화된 경우에만 표시
        if (showFlightZoneLayerButton) {
            SmallFloatingActionButton(
                onClick = onShowFlightZoneLayers,
                containerColor = if (flightZoneLayersActive) {
                    Color(0xFFFF8C00).copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (flightZoneLayersActive) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = stringResource(R.string.map_fab_flight_zone_layer),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 도형 추가 FAB
        FloatingActionButton(
            onClick = onCreateShape,
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = Color.Black
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.map_fab_add_shape)
            )
        }
    }
}
