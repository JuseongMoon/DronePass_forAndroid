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
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherOverlayCard
import java.util.Locale

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
internal val MapFloatingAccentColor = Color(0xFF007AFF) // iOS systemBlue / Color.accentColor
internal val MapCreateShapeButtonSize = 60.dp
internal val MapCreateShapeIconSize = 28.dp
internal val MapCreateShapeButtonShadowElevation = 6.dp
internal val MapSketchButtonSize = 45.dp
internal val MapSketchIconSize = 21.dp
internal val MapSketchButtonShadowElevation = 4.dp
internal val MapStatusGroupSpacing = 8.dp
internal val MapKpButtonHorizontalPadding = 12.dp
internal val MapKpButtonVerticalPadding = 8.dp
internal val MapKpButtonCornerRadius = 12.dp
internal val MapKpButtonShadowElevation = 4.dp
internal val MapKpButtonTextSpacing = 4.dp
internal val MapKpButtonTextSize = 16.sp
internal val FlightZoneFabSize = 50.dp
internal val FlightZoneFabCornerRadius = 12.dp
internal val FlightZoneFabShadowElevation = 4.dp
internal val FlightZoneFabIconSize = 22.dp
internal val FlightZoneFabBadgeFontSize = 10.sp
internal val FlightZoneFabVerticalSpacing = 4.dp
private val FlightZoneActiveColor = MapFloatingAccentColor

internal enum class FlightZoneFabIconStyle {
    OUTLINED_MAP,
    FILLED_MAP,
}

internal data class FlightZoneFabUiState(
    val active: Boolean,
    val iconStyle: FlightZoneFabIconStyle,
    val badgeText: String?,
    val tint: Color,
)

internal fun resolveFlightZoneFabUiState(count: Int): FlightZoneFabUiState {
    val active = count > 0
    return FlightZoneFabUiState(
        active = active,
        iconStyle = if (active) FlightZoneFabIconStyle.FILLED_MAP else FlightZoneFabIconStyle.OUTLINED_MAP,
        badgeText = if (active) count.toString() else null,
        tint = if (active) FlightZoneActiveColor else Color.Gray,
    )
}

internal fun formatMapKpValue(currentKpValue: Double?): String =
    if (currentKpValue != null) {
        String.format(Locale.ROOT, "%.1f", currentKpValue)
    } else {
        "-"
    }

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
    sunriseTimes: List<String> = sunrise?.let(::listOf) ?: emptyList(),
    sunsetTimes: List<String> = sunset?.let(::listOf) ?: emptyList(),
    onWeatherClick: () -> Unit = {},
    isTabletLayout: Boolean = false,
    modifier: Modifier = Modifier
) {
    val paddings = remember(isTabletLayout) {
        resolveMapFloatingButtonPaddings(isTablet = isTabletLayout)
    }

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
                    .padding(start = paddings.edge, bottom = paddings.flightZoneBottom)
            )
        }

        // 우측 하단 상단부: 스케치 / KP / 날씨
        // iOS rightBottomButtonsView: .padding(.bottom, safeArea + 170)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = paddings.edge, bottom = paddings.statusGroupBottom),
            horizontalAlignment = Alignment.End
        ) {
            // 스케치 모드 진입 FAB
            SmallFloatingActionButton(
                onClick = onEnterSketchMode,
                modifier = Modifier.size(MapSketchButtonSize),
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = MapFloatingAccentColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = MapSketchButtonShadowElevation,
                    pressedElevation = MapSketchButtonShadowElevation,
                    focusedElevation = MapSketchButtonShadowElevation,
                    hoveredElevation = MapSketchButtonShadowElevation,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.map_fab_sketch),
                    modifier = Modifier.size(MapSketchIconSize)
                )
            }

            Spacer(modifier = Modifier.height(MapStatusGroupSpacing))

            // KP 지수 버튼
            // iOS `kpIndexButton` 정합: "KP" 라벨 + 값(or "-"), 둘 다 동일한 레벨 색상으로 표시.
            //   - 라벨은 "KP" (대문자) — iOS Text("KP")
            //   - 값 표시는 데이터 있으면 "5.0", 없으면 "-" — iOS `currentKPString`
            //   - 인디케이터 원 제거: 색상은 두 텍스트 자체에 직접 적용
            Surface(
                onClick = onShowKpForecast,
                shape = RoundedCornerShape(MapKpButtonCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = MapKpButtonShadowElevation
            ) {
                val kpValueText = remember(currentKpValue) {
                    formatMapKpValue(currentKpValue)
                }
                Row(
                    modifier = Modifier.padding(
                        horizontal = MapKpButtonHorizontalPadding,
                        vertical = MapKpButtonVerticalPadding,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MapKpButtonTextSpacing)
                ) {
                    Text(
                        text = "KP",
                        fontSize = MapKpButtonTextSize,
                        fontWeight = FontWeight.Bold,
                        color = kpLevelColor
                    )
                    Text(
                        text = kpValueText,
                        fontSize = MapKpButtonTextSize,
                        fontWeight = FontWeight.Bold,
                        color = kpLevelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(MapStatusGroupSpacing))

            // 날씨 오버레이 카드 (KP 아래)
            WeatherOverlayCard(
                currentWeather = currentWeather,
                sunrise = sunrise,
                sunset = sunset,
                sunriseTimes = sunriseTimes,
                sunsetTimes = sunsetTimes,
                onClick = onWeatherClick
            )
        }

        // 도형 추가 FAB — iOS plusButtonView: .padding(.bottom, safeArea + 90)
        FloatingActionButton(
            onClick = onCreateShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = paddings.edge, bottom = paddings.createShapeBottom)
                .size(MapCreateShapeButtonSize),
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = MapFloatingAccentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = MapCreateShapeButtonShadowElevation,
                pressedElevation = MapCreateShapeButtonShadowElevation,
                focusedElevation = MapCreateShapeButtonShadowElevation,
                hoveredElevation = MapCreateShapeButtonShadowElevation,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.map_fab_add_shape),
                modifier = Modifier.size(MapCreateShapeIconSize)
            )
        }
    }
}

internal data class MapFloatingButtonPaddings(
    val edge: Dp,
    val flightZoneBottom: Dp,
    val statusGroupBottom: Dp,
    val createShapeBottom: Dp,
)

internal fun resolveMapFloatingButtonPaddings(
    isTablet: Boolean,
): MapFloatingButtonPaddings = MapFloatingButtonPaddings(
    edge = if (isTablet) 20.dp else 12.dp,
    flightZoneBottom = 100.dp,
    statusGroupBottom = 170.dp,
    createShapeBottom = 90.dp,
)

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
    val state = remember(count) { resolveFlightZoneFabUiState(count) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(FlightZoneFabCornerRadius),
        color = Color.White,
        shadowElevation = FlightZoneFabShadowElevation,
        modifier = modifier.size(FlightZoneFabSize)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = FlightZoneFabVerticalSpacing,
                alignment = Alignment.CenterVertically,
            )
        ) {
            Icon(
                imageVector = if (state.iconStyle == FlightZoneFabIconStyle.FILLED_MAP) {
                    Icons.Filled.Map
                } else {
                    Icons.Outlined.Map
                },
                contentDescription = stringResource(R.string.map_fab_flight_zone_layer),
                tint = state.tint,
                modifier = Modifier.size(FlightZoneFabIconSize)
            )
            state.badgeText?.let { badge ->
                Text(
                    text = badge,
                    fontSize = FlightZoneFabBadgeFontSize,
                    fontWeight = FontWeight.Bold,
                    color = FlightZoneActiveColor,
                )
            }
        }
    }
}
