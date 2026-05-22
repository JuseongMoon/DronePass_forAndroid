package com.ScienceFiction.DronePassAndroid.feature.weather

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherForecastScreen(
    onBack: () -> Unit = {},
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

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
                        onClick = { viewModel.refreshWeather() },
                        enabled = !isLoading,
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
        )
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
) {
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val lastUpdateTime by viewModel.lastUpdateTime.collectAsStateWithLifecycle()

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

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading && weatherData == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error != null && weatherData == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(error!!.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.refreshWeather() }) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            }
            else -> {
                weatherData?.let { data ->
                    WeatherForecastBody(
                        data = data,
                        category = selectedCategory,
                        onCategoryChanged = { viewModel.setCategory(it) },
                        isLoading = isLoading,
                        lastUpdateTime = lastUpdateTime,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherForecastBody(
    data: WeatherData,
    category: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit,
    isLoading: Boolean,
    lastUpdateTime: Long?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ① 일출/일몰 카드
        item {
            SunTimeline(sunrise = data.sunrise, sunset = data.sunset)
        }

        // ② 현재 날씨 카드 (통합)
        item {
            CurrentWeatherSection(
                data = data,
                category = category,
                onCategoryChanged = onCategoryChanged,
                isLoading = isLoading,
            )
        }

        // ③ 6개 예보 차트 (24시간)
        val now = System.currentTimeMillis()
        val chartHours = data.hourlyForecast.filter { it.time >= now }.take(24)
        if (chartHours.size >= 2) {
            item { TemperatureChart(hourlyData = chartHours) }
            item { WindSpeedChart(hourlyData = chartHours) }
            item { GustDifferenceChart(hourlyData = chartHours) }
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
                )
            }
        }
    }
}

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
            IconButton(onClick = onRefresh, enabled = !isLoading) {
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
