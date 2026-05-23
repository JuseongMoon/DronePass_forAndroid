package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherOverlayCard

/**
 * 지도 위 플로팅 컨트롤. iOS `MainFloatingButtonView` 를 좌·우 영역으로 분리한 매핑.
 *
 *  - **좌측 하단**: 비행구역 레이어 선택 FAB (iOS `leftBottomButtonsView`).
 *    50dp 흰색 rounded(12) + `map` 아이콘 + 활성 시 파란색 + 선택수 배지.
 *    `flightZoneVisibleLayerCount > 0` 일 때 활성 색상/배지를 표시한다.
 *    iOS 정합 — `koreaFeaturesEnabled` 가 false 면 FAB 자체를 숨김.
 *  - **우측 하단**: 스케치 / KP / 날씨 / 도형 추가 (iOS `rightBottomButtonsView`).
 *    기존 Column 레이아웃 유지.
 */
private val FlightZoneActiveColor = Color(0xFF007AFF) // iOS systemBlue

@Composable
fun MapFloatingButtons(
    onCreateShape: () -> Unit,
    onEnterSketchMode: () -> Unit,
    onShowFlightZoneLayers: () -> Unit = {},
    flightZoneVisibleLayerCount: Int = 0,
    koreaFeaturesEnabled: Boolean = true,
    currentKpValue: Double? = null,
    kpLevelColor: Color = Color.Gray,
    onShowKpForecast: () -> Unit = {},
    currentWeather: CurrentWeatherData? = null,
    sunrise: String? = null,
    sunset: String? = null,
    onWeatherClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 좌측 하단: 비행구역 레이어 FAB
        // iOS leftBottomButtonsView: .padding(.bottom, safeArea + 100)
        // 한국 특화 기능 OFF 시 FAB 숨김 (iOS isKoreaFeaturesEnabled 정합).
        if (koreaFeaturesEnabled) {
            FlightZoneLayerFab(
                count = flightZoneVisibleLayerCount,
                onClick = onShowFlightZoneLayers,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, bottom = 100.dp)
            )
        }

        // 우측 하단 상단부: 스케치 / KP / 날씨
        // iOS rightBottomButtonsView: .padding(.bottom, safeArea + 170)
        // floating tab bar(75dp) 위로 충분한 간격 확보
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 130.dp),
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
            // iOS `kpIndexButton` 정합: "KP" 라벨 + 값(or "-"), 둘 다 동일한 레벨 색상으로 표시.
            //   - 라벨은 "KP" (대문자) — iOS Text("KP")
            //   - 값 표시는 데이터 있으면 "5.0", 없으면 "-" — iOS `currentKPString`
            //   - 인디케이터 원 제거: 색상은 두 텍스트 자체에 직접 적용
            Surface(
                onClick = onShowKpForecast,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                val kpValueText = remember(currentKpValue) {
                    if (currentKpValue != null) String.format(Locale.ROOT, "%.1f", currentKpValue)
                    else "-"
                }
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "KP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = kpLevelColor
                    )
                    Text(
                        text = kpValueText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = kpLevelColor
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
        }

        // 도형 추가 FAB — iOS plusButtonView: .padding(.bottom, safeArea + 90)
        // floating tab bar(75dp) 바로 위 ~15dp 갭으로 떠 있도록 분리 배치
        FloatingActionButton(
            onClick = onCreateShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 50.dp),
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = Color.Black,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.map_fab_add_shape)
            )
        }
    }
}

/**
 * 비행구역 레이어 선택 FAB. iOS `FlightZoneLayerSelector` 의 플로팅 버튼 매핑.
 *
 *  - 50×50dp 정사각형, `RoundedCornerShape(12.dp)`, 흰색 배경, 그림자.
 *  - 활성(`count > 0`) 시 아이콘/숫자가 iOS systemBlue, 비활성 시 회색.
 *  - 활성 시 선택된 레이어 수 배지(10sp, Bold)를 아이콘 하단에 표시.
 */
@Composable
private fun FlightZoneLayerFab(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = count > 0
    val tint = if (active) FlightZoneActiveColor else Color.Gray
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier.size(50.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = stringResource(R.string.map_fab_flight_zone_layer),
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            if (active) {
                Text(
                    text = count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlightZoneActiveColor,
                )
            }
        }
    }
}
