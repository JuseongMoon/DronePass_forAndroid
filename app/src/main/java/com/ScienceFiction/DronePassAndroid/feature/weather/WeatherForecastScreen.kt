package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherForecastScreen(
    onBack: () -> Unit = {},
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weather_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshWeather() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
                    }
                }
            )
        }
    ) { innerPadding ->
        WeatherForecastContent(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * 날씨 예보 본문 Content (BottomSheet에서도 재사용 가능)
 */
@Composable
fun WeatherForecastContent(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    // 화면이 START 상태일 때만 3분 자동 갱신. ON_STOP 시 중단하여 백그라운드
    // 무한 새로고침으로 인한 배터리/요금 소모를 차단한다.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> viewModel.startAutoRefresh()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> viewModel.stopAutoRefresh()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isLoading && weatherData == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null && weatherData == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(error!!.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.refreshWeather() }) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            }
            else -> {
                WeatherContentInternal(
                    weatherData = weatherData,
                    selectedCategory = selectedCategory,
                    onCategoryChanged = { viewModel.setCategory(it) }
                )
            }
        }
    }
}

@Composable
private fun WeatherContentInternal(
    weatherData: WeatherData?,
    selectedCategory: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        weatherData?.current?.let { current ->
            item {
                FlightSuitabilityCard(
                    current = current,
                    category = selectedCategory
                )
            }
        }

        weatherData?.current?.let { current ->
            item {
                CurrentWeatherCard(
                    current = current,
                    sunrise = weatherData.sunrise,
                    sunset = weatherData.sunset
                )
            }
        }

        // 일출/일몰 타임라인
        if (weatherData?.sunrise != null || weatherData?.sunset != null) {
            item {
                SunTimeline(
                    sunrise = weatherData.sunrise,
                    sunset = weatherData.sunset
                )
            }
        }

        item {
            DroneCategorySelector(
                selectedCategory = selectedCategory,
                onCategoryChanged = onCategoryChanged
            )
        }

        weatherData?.current?.let { current ->
            item {
                WindCompassCard(
                    windDirection = current.windDirection,
                    windSpeed = current.windSpeed
                )
            }

            item {
                WeatherDataGrid(current = current, category = selectedCategory)
            }
        }

        if (weatherData?.hourlyForecast?.isNotEmpty() == true) {
            val now = System.currentTimeMillis()
            val chartHours = weatherData.hourlyForecast
                .filter { it.time >= now }
                .take(24)

            if (chartHours.size >= 2) {
                // 날씨 예보 차트 6종
                item {
                    Text(
                        text = stringResource(R.string.weather_hourly_forecast),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item { TemperatureChart(hourlyData = chartHours) }
                item { WindSpeedChart(hourlyData = chartHours) }
                item { GustDifferenceChart(hourlyData = chartHours) }
                item { PrecipitationChart(hourlyData = chartHours) }
                item { VisibilityChart(hourlyData = chartHours) }
                item { CriChart(hourlyData = chartHours) }
            }

            items(chartHours) { hourly ->
                HourlyWeatherItem(
                    hourly = hourly,
                    category = selectedCategory
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
