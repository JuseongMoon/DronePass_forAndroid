package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.openUriSafely
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import com.ScienceFiction.DronePassAndroid.feature.settings.WeatherInfoGuideSheet
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageDurationMs
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageOverlay
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

internal const val WeatherDataSourceUrl = "https://open-meteo.com/"

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

internal fun weatherForecastBodyData(
    weatherData: WeatherData?,
    isLoading: Boolean,
    hasError: Boolean,
): WeatherData? {
    return weatherData ?: if (isLoading || hasError) emptyWeatherForecastData() else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherForecastScreen(
    onBack: () -> Unit = {},
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isUsingGps by viewModel.isUsingGps.collectAsStateWithLifecycle()
    val locationAccuracyMeters by viewModel.locationAccuracyMeters.collectAsStateWithLifecycle()
    var showWeatherInfoSheet by remember { mutableStateOf(false) }
    var selectedWeatherInfoTopic by remember { mutableStateOf<WeatherInfoTopic?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.weather_navigation_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedWeatherInfoTopic = null
                            showWeatherInfoSheet = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.weather_info_button),
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshWeather() },
                        enabled = isWeatherRefreshActionEnabled(isLoading),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.common_refresh),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        WeatherForecastContent(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
            onWeatherInfoRequested = { topic ->
                selectedWeatherInfoTopic = topic
                showWeatherInfoSheet = true
            },
        )
    }

    if (showWeatherInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWeatherInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            WeatherInfoGuideSheet(
                onDismiss = { showWeatherInfoSheet = false },
                initialTopic = selectedWeatherInfoTopic,
                category = selectedCategory,
                onCategoryChanged = { viewModel.setCategory(it) },
                isUsingGps = isUsingGps,
                locationAccuracyMeters = locationAccuracyMeters,
            )
        }
    }
}

/**
 * iOS `WeatherForecastView` body 정합 — 헤더 없는 순수 콘텐츠.
 * 풀스크린 [WeatherForecastScreen] 과 ModalBottomSheet 양쪽에서 재사용.
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
        weatherForecastBodyData(
            weatherData = weatherData,
            isLoading = isLoading,
            hasError = error != null,
        )?.let { data ->
            WeatherForecastBody(
                data = data,
                category = selectedCategory,
                onCategoryChanged = { viewModel.setCategory(it) },
                isLoading = isLoading,
                error = error,
                lastUpdateTime = lastUpdateTime,
                onWeatherInfoRequested = onWeatherInfoRequested,
            )
        }

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

        // ③ 6개 예보 차트 (현재 정시부터 3일)
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

        // ④ 마지막 업데이트 시간
        if (lastUpdateTime != null) {
            item {
                val formatted = remember(lastUpdateTime) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUpdateTime))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(R.string.weather_last_update, formatted),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ⑤ 데이터 출처 (iOS Apple Weather attribution 대응)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.weather_data_source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        openUriSafely(uriHandler, WeatherDataSourceUrl)
                    },
                )
            }
        }
    }
}

internal const val WeatherForecastDays = 3
internal const val WeatherForecastChartHours = WeatherForecastDays * 24

internal fun resolveWeatherForecastChartHours(
    hourlyForecast: List<HourlyWeatherData>,
    nowMillis: Long = System.currentTimeMillis(),
    utcOffsetSeconds: Int? = null,
): List<HourlyWeatherData> {
    val zoneId = resolveWeatherForecastChartZone(utcOffsetSeconds)
    val currentHourStart = resolveCurrentWeatherForecastHourStartMillis(nowMillis, zoneId)
    return hourlyForecast
        .asSequence()
        .filter { it.time >= currentHourStart }
        .take(WeatherForecastChartHours)
        .toList()
}

internal fun resolveCurrentWeatherForecastHourStartMillis(
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    return Instant.ofEpochMilli(nowMillis)
        .atZone(zoneId)
        .truncatedTo(ChronoUnit.HOURS)
        .toInstant()
        .toEpochMilli()
}

private fun resolveWeatherForecastChartZone(utcOffsetSeconds: Int?): ZoneId {
    return runCatching {
        utcOffsetSeconds?.let(ZoneOffset::ofTotalSeconds)
    }.getOrNull() ?: ZoneId.systemDefault()
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
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onInfo != null) {
                IconButton(onClick = onInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.weather_info_button),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Spacer(modifier = Modifier.padding(start = 48.dp))
            }
            Text(
                text = stringResource(R.string.weather_navigation_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onRefresh, enabled = isWeatherRefreshActionEnabled(isLoading)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.weather_refresh),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
