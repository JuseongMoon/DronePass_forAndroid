package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.openUriSafely
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageDurationMs
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageOverlay
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Apple WeatherKit 법적 고지·데이터 출처 페이지. 날씨 화면에 반드시 링크해야 한다. */
internal const val WeatherKitLegalAttributionUrl = "https://weatherkit.apple.com/legal-attribution.html"
internal val WeatherKitLogoHeight = 14.dp
private val WeatherStaleNoticeColor = Color(0xFFFF9500)
internal val WeatherSheetNavigationHeaderHeight = 44.dp
internal val WeatherSheetNavigationHeaderActionWidth = 44.dp
internal val WeatherSheetNavigationHeaderHorizontalPadding = 8.dp
internal val WeatherSheetNavigationHeaderDividerThickness = 0.5.dp

@Suppress("UNUSED_PARAMETER")
internal fun isWeatherRefreshActionEnabled(isLoading: Boolean): Boolean = true

internal fun emptyWeatherForecastData(): WeatherData = WeatherData(
    current = null,
    hourlyForecast = emptyList(),
    sunrise = null,
    sunset = null,
    sunriseTimes = emptyList(),
    sunsetTimes = emptyList(),
    utcOffsetSeconds = null,
)

@Suppress("UNUSED_PARAMETER")
internal fun weatherForecastBodyData(
    weatherData: WeatherData?,
    isLoading: Boolean,
    hasError: Boolean,
): WeatherData {
    return weatherData ?: emptyWeatherForecastData()
}

/**
 * iOS `WeatherForecastView` body 정합 — 헤더 없는 순수 콘텐츠.
 * ModalBottomSheet 내부에서 [WeatherSheetHeader] 와 함께 사용한다.
 */
@Composable
fun WeatherForecastContent(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    onWeatherInfoRequested: (WeatherInfoTopic) -> Unit = {},
) {
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val lastUpdateTime by viewModel.lastUpdateTime.collectAsStateWithLifecycle()
    val refreshMessage = stringResource(R.string.weather_refresh)
    var showRefreshToast by remember { mutableStateOf(false) }
    var refreshToastGeneration by remember { mutableIntStateOf(0) }

    // 화면이 START 상태일 때만 3분 자동 갱신.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startAutoRefresh()
                Lifecycle.Event.ON_STOP -> viewModel.stopAutoRefresh()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, refreshMessage) {
        viewModel.refreshCompleted.collect {
            showRefreshToast = false
            refreshToastGeneration += 1
            showRefreshToast = true
        }
    }

    LaunchedEffect(refreshToastGeneration) {
        if (refreshToastGeneration == 0) return@LaunchedEffect
        delay(IosToastMessageDurationMs)
        showRefreshToast = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        val bodyData = weatherForecastBodyData(
            weatherData = weatherData,
            isLoading = isLoading,
            hasError = error != null,
        )
        WeatherForecastBody(
            data = bodyData,
            category = selectedCategory,
            onCategoryChanged = { viewModel.setCategory(it) },
            isLoading = isLoading,
            error = error,
            lastUpdateTime = lastUpdateTime,
            onWeatherInfoRequested = onWeatherInfoRequested,
        )

        IosToastMessageOverlay(
            visible = showRefreshToast,
            message = refreshMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun WeatherForecastBody(
    data: WeatherData,
    category: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit,
    isLoading: Boolean,
    error: WeatherError?,
    lastUpdateTime: Long?,
    onWeatherInfoRequested: (WeatherInfoTopic) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ① 일출/일몰 카드
        item {
            SunTimeline(
                sunrise = data.sunrise,
                sunset = data.sunset,
                sunriseTimes = data.sunriseTimes,
                sunsetTimes = data.sunsetTimes,
                utcOffsetSeconds = data.utcOffsetSeconds,
            )
        }

        // ② 현재 날씨 카드 (통합)
        item {
            CurrentWeatherSection(
                data = data,
                category = category,
                onCategoryChanged = onCategoryChanged,
                isLoading = isLoading,
                error = error,
                onWeatherInfoRequested = onWeatherInfoRequested,
            )
        }

        // ③ 6개 예보 차트 (iOS WeatherManager: 현재 정시 기준 3시간 전부터 3일 뒤까지)
        val chartHours = resolveWeatherForecastChartHours(
            hourlyForecast = data.hourlyForecast,
            utcOffsetSeconds = data.utcOffsetSeconds,
        )
        if (shouldShowWeatherForecastCharts(chartHours)) {
            item { TemperatureChart(hourlyData = chartHours) }
            item { WindSpeedChart(hourlyData = chartHours, category = category) }
            item { GustDifferenceChart(hourlyData = chartHours, category = category) }
            item { PrecipitationChart(hourlyData = chartHours) }
            item { VisibilityChart(hourlyData = chartHours) }
            item { CriChart(hourlyData = chartHours) }
        }

        // ④ 마지막 업데이트 + 출처 (Apple WeatherKit 출처 표시 요구 사항)
        item {
            WeatherAttributionFooter(
                lastUpdateTime = lastUpdateTime,
                isStale = data.isStale,
                onLegalAttributionClick = { openUriSafely(uriHandler, WeatherKitLegalAttributionUrl) },
            )
        }
    }
}

/**
 * 날씨 화면 하단.
 * - 마지막 업데이트: 중계 서버가 Apple 응답을 받은 시각
 * - 서버가 만료된 캐시를 준 경우(stale) 주황색 안내
 * - Apple Weather 로고 + 법적 고지 링크 (Android 는 Apple 로고 글자가 없어 공식 로고 이미지를 쓴다)
 */
@Composable
private fun WeatherAttributionFooter(
    lastUpdateTime: Long?,
    isStale: Boolean,
    onLegalAttributionClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isStale) {
            Text(
                text = stringResource(R.string.weather_stale_notice),
                style = MaterialTheme.typography.labelSmall,
                color = WeatherStaleNoticeColor,
                textAlign = TextAlign.End,
            )
        }
        if (lastUpdateTime != null) {
            val formatted = remember(lastUpdateTime) { formatWeatherLastUpdateTime(lastUpdateTime) }
            Text(
                text = stringResource(R.string.weather_last_update, formatted),
                style = MaterialTheme.typography.labelSmall,
                color = if (isStale) WeatherStaleNoticeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.apple_weather_logo),
                contentDescription = stringResource(R.string.weather_attribution_logo),
                modifier = Modifier.height(WeatherKitLogoHeight),
            )
            Text(
                text = stringResource(R.string.weather_attribution_legal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onLegalAttributionClick),
            )
        }
    }
}

internal fun formatWeatherLastUpdateTime(
    timestamp: Long,
    locale: Locale = Locale.getDefault(),
): String {
    val formatter = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
    return formatter.format(Date(timestamp))
}

internal const val WeatherForecastDays = 3
internal const val WeatherForecastChartHours = WeatherForecastDays * 24
internal const val WeatherForecastLookbackMs = 3L * 60L * 60L * 1000L
internal const val WeatherForecastWindowMs = WeatherForecastDays * 24L * 60L * 60L * 1000L

internal fun resolveWeatherForecastChartHours(
    hourlyForecast: List<HourlyWeatherData>,
    nowMillis: Long = System.currentTimeMillis(),
    utcOffsetSeconds: Int? = null,
): List<HourlyWeatherData> {
    val hourMs = 60L * 60L * 1000L
    val offsetMs = (utcOffsetSeconds ?: 0) * 1000L
    val currentHourStartMillis = Math.floorDiv(nowMillis + offsetMs, hourMs) * hourMs - offsetMs
    val startMillis = currentHourStartMillis - WeatherForecastLookbackMs
    val endMillis = nowMillis + WeatherForecastWindowMs
    return hourlyForecast
        .asSequence()
        .filter { it.time in startMillis..endMillis }
        .toList()
}

internal fun shouldShowWeatherForecastCharts(
    chartHours: List<HourlyWeatherData>,
): Boolean = chartHours.isNotEmpty()

/**
 * ModalBottomSheet 내부에서 표시할 시트 헤더 — iOS `WeatherForecastView` 의 NavigationView TopBar 정합.
 * 좌측: info 버튼(선택), 중앙: 제목, 우측: 새로고침 버튼.
 */
@Composable
fun WeatherSheetHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onInfo: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WeatherSheetNavigationHeaderHeight)
                .padding(horizontal = WeatherSheetNavigationHeaderHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(WeatherSheetNavigationHeaderActionWidth),
                contentAlignment = Alignment.Center,
            ) {
                if (onInfo != null) {
                    IconButton(
                        onClick = onInfo,
                        modifier = Modifier.size(WeatherSheetNavigationHeaderActionWidth),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.weather_info_button),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.weather_navigation_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(
                modifier = Modifier.width(WeatherSheetNavigationHeaderActionWidth),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = isWeatherRefreshActionEnabled(isLoading),
                    modifier = Modifier.size(WeatherSheetNavigationHeaderActionWidth),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.weather_refresh),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = WeatherSheetNavigationHeaderDividerThickness,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
