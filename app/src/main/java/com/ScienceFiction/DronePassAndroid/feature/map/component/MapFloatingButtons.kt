package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import com.naver.maps.map.widget.ZoomControlView
import com.naver.maps.map.NaverMap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherOverlayCard
import kotlinx.coroutines.delay
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
// iOS plus 28pt 글리프 ≈ Material Add 36dp
internal val MapCreateShapeIconSize = 36.dp
internal val MapCreateShapeButtonShadowElevation = 6.dp
internal val MapSketchButtonSize = 45.dp
internal val MapZoomControlToSketchSpacing = 10.dp
internal val MapSketchIconSize = 21.dp
internal val MapSketchButtonShadowElevation = 4.dp
/** 스케치(그리기) 진입 아이콘. 연필 끝(pencil.tip)은 작은 크기에서 삼각형처럼 보여 펜 아이콘을 쓴다. */
internal val MapSketchButtonIcon: ImageVector = Icons.Rounded.Edit
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
// iOS map 22pt 글리프 ≈ Material 27dp
internal val FlightZoneFabIconSize = 27.dp
internal val FlightZoneFabBadgeFontSize = 10.sp
internal val FlightZoneFabVerticalSpacing = 4.dp
internal const val FlightZoneFabIdleScale = 1.0f
internal const val FlightZoneFabPulseScale = 1.1f
internal const val FlightZoneFabPulseResetDelayMillis = 200L
internal const val FlightZoneFabPulseDampingRatio = 0.6f
internal val FlightZoneFabOpenHapticType = HapticFeedbackType.TextHandleMove
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
    modifier: Modifier = Modifier,
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
    weatherUtcOffsetSeconds: Int? = null,
    weatherBasisLabel: String? = null,
    onWeatherClick: () -> Unit = {},
    isTabletLayout: Boolean = false,
    naverMap: NaverMap? = null,
    isSketchMode: Boolean = false,
) {
    val density = LocalDensity.current
    // 스케치 모드에서도 확대/축소를 같은 자리에 두기 위해, 숨긴 버튼 묶음의 높이를 기억해 둔다(iOS 는 SDK 컨트롤이 그대로 남는다).
    var statusGroupHeight by remember { mutableStateOf(0.dp) }
    val paddings = remember(isTabletLayout) {
        resolveMapFloatingButtonPaddings(isTablet = isTabletLayout)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 좌측 하단: 비행구역 레이어 FAB
        // iOS 는 SDK 현위치 버튼 바로 위(약 15pt 간격)에 둔다. Android 현위치 버튼은 지도 하단 콘텐츠 여백
        // (MapBottomContentPadding) 때문에 화면 아래에서 약 139dp 까지 올라와 있어, 그 위로 올려 겹치지 않게 한다.
        // 한국 특화 기능 OFF 시 FAB 숨김 (iOS isKoreaFeaturesEnabled 정합).
        if (koreaFeaturesEnabled && !isSketchMode) {
            FlightZoneLayerFab(
                count = flightZoneVisibleLayerCount,
                onClick = onShowFlightZoneLayers,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = paddings.edge, bottom = paddings.flightZoneBottom)
            )
        }

        // 우측 하단 상단부: 스케치 / KP / 날씨
        // iOS rightBottomButtonsView: .padding(.bottom, safeArea + 170)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = paddings.edge, bottom = paddings.statusGroupBottom),
            horizontalAlignment = Alignment.End
        ) {
            // 확대/축소 컨트롤. SDK 기본 위치(화면 세로 가운데)에 두면 화면 높이에 따라 스케치 버튼과 겹친다.
            // 스택 맨 위에 붙여 iOS 실측 간격(축소 아래 → 스케치 위 약 10pt)을 어느 화면에서나 유지한다.
            if (naverMap != null) {
                AndroidView(
                    factory = { context -> ZoomControlView(context) },
                    update = { view -> view.map = naverMap },
                )
                Spacer(modifier = Modifier.height(MapZoomControlToSketchSpacing))
            }

            if (isSketchMode) {
                Spacer(modifier = Modifier.height(statusGroupHeight))
            } else {
                Column(
                    modifier = Modifier.onSizeChanged { size ->
                        statusGroupHeight = with(density) { size.height.toDp() }
                    },
                    horizontalAlignment = Alignment.End,
                ) {
                    // 스케치 모드 진입 FAB
                    SmallFloatingActionButton(
                        onClick = onEnterSketchMode,
                        modifier = Modifier.size(MapSketchButtonSize),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MapFloatingAccentColor,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = MapSketchButtonShadowElevation,
                            pressedElevation = MapSketchButtonShadowElevation,
                            focusedElevation = MapSketchButtonShadowElevation,
                            hoveredElevation = MapSketchButtonShadowElevation,
                        ),
                    ) {
                        Icon(
                            imageVector = MapSketchButtonIcon,
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
                    // iOS .buttonStyle(.plain): 최소 터치 크기(48dp) 여백·누름 효과 없이 VStack(spacing: 8) 간격을 지킨다.
                    Surface(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onShowKpForecast,
                        ),
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
                        onClick = onWeatherClick,
                        utcOffsetSeconds = weatherUtcOffsetSeconds,
                        basisLabel = weatherBasisLabel,
                    )
                }
            }
        }

        // 도형 추가 FAB — iOS plusButtonView: .padding(.bottom, safeArea + 90)
        if (!isSketchMode) {
            FloatingActionButton(
                onClick = onCreateShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
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
}

/** SDK 현위치 버튼(아래 87~139dp) 위. 보이는 카드 간격이 iOS 처럼 약 10pt 가 되도록 맞춘다. */
internal val MapFlightZoneFabBottom = 144.dp

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
    flightZoneBottom = MapFlightZoneFabBottom,
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
    val haptic = LocalHapticFeedback.current
    var observedCount by remember { mutableIntStateOf(count) }
    var isAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(count) {
        if (observedCount != count) {
            observedCount = count
            isAnimating = true
            delay(FlightZoneFabPulseResetDelayMillis)
            isAnimating = false
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) FlightZoneFabPulseScale else FlightZoneFabIdleScale,
        animationSpec = spring(
            dampingRatio = FlightZoneFabPulseDampingRatio,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "flightZoneFabPulseScale",
    )
    Surface(
        onClick = {
            haptic.performHapticFeedback(FlightZoneFabOpenHapticType)
            onClick()
        },
        shape = RoundedCornerShape(FlightZoneFabCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = FlightZoneFabShadowElevation,
        modifier = modifier
            .size(FlightZoneFabSize)
            .scale(scale)
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
                    // 본문 스타일의 24sp 줄 높이를 물려받으면 숫자가 아이콘에서 멀어진다(iOS .system(size: 10)).
                    lineHeight = FlightZoneFabBadgeFontSize,
                    fontWeight = FontWeight.Bold,
                    color = FlightZoneActiveColor,
                )
            }
        }
    }
}
