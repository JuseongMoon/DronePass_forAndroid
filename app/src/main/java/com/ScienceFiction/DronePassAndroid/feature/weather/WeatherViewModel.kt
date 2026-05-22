package com.ScienceFiction.DronePassAndroid.feature.weather

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val dataStore: DataStore<Preferences>,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    companion object {
        private val KEY_DRONE_CATEGORY = stringPreferencesKey("weather_drone_category")
        private const val AUTO_REFRESH_INTERVAL_MS = 3 * 60 * 1000L // 3분
        private const val DEFAULT_LATITUDE = 37.5665
        private const val DEFAULT_LONGITUDE = 126.9780
    }

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러 상태. UI 에서 `stringResource(error.messageRes)` 로 다국어 변환한다 (D-M9).
     */
    private val _error = MutableStateFlow<WeatherError?>(null)
    val error: StateFlow<WeatherError?> = _error.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow<Long?>(null)
    val lastUpdateTime: StateFlow<Long?> = _lastUpdateTime.asStateFlow()

    val selectedCategory: StateFlow<DroneCategory> = dataStore.data
        .map { preferences ->
            val name = preferences[KEY_DRONE_CATEGORY] ?: DroneCategory.TOY.name
            try {
                DroneCategory.valueOf(name)
            } catch (e: IllegalArgumentException) {
                DroneCategory.TOY
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DroneCategory.TOY)

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    /**
     * 자동 갱신 Job. Composable 의 ON_START/ON_STOP 라이프사이클에 맞춰 시작/중단된다.
     * 백그라운드 무한 새로고침으로 인한 배터리/요금 부담을 차단한다.
     */
    private var autoRefreshJob: Job? = null

    init {
        // 초기 1회 로드만 init 에서. 자동 갱신은 화면이 START 될 때만 시작.
        refreshWeather()
    }

    /**
     * 드론 카테고리 변경
     */
    fun setCategory(category: DroneCategory) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_DRONE_CATEGORY] = category.name
            }
            // 카테고리 변경 시 날씨 데이터 재계산
            if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                fetchWeatherInternal(currentLatitude, currentLongitude, category)
            }
        }
    }

    /**
     * 수동 갱신
     */
    fun refreshWeather() {
        analyticsLogger.logWeatherViewed()
        viewModelScope.launch {
            weatherRepository.invalidateCache()
            fetchCurrentLocationAndWeather()
        }
    }

    /**
     * 화면이 START 상태일 때만 3분 간격 자동 갱신을 시작한다.
     * Composable 의 DisposableEffect(ON_START) 에서 호출한다.
     */
    fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                fetchCurrentLocationAndWeather()
            }
        }
    }

    /**
     * 화면이 STOP 될 때 자동 갱신을 중단한다.
     */
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /**
     * 현재 위치 가져와서 날씨 조회
     */
    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocationAndWeather() {
        _isLoading.value = true
        _error.value = null

        try {
            val cancellationToken = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).await()

            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude
                fetchWeatherInternal(location.latitude, location.longitude, selectedCategory.value)
            } else {
                // 마지막 알려진 위치 시도
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    currentLatitude = lastLocation.latitude
                    currentLongitude = lastLocation.longitude
                    fetchWeatherInternal(lastLocation.latitude, lastLocation.longitude, selectedCategory.value)
                } else {
                    // 기본 위치 (서울)
                    currentLatitude = DEFAULT_LATITUDE
                    currentLongitude = DEFAULT_LONGITUDE
                    fetchWeatherInternal(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, selectedCategory.value)
                }
            }
        } catch (e: SecurityException) {
            _error.value = WeatherError.LocationPermission
            // 기본 위치로 시도
            currentLatitude = DEFAULT_LATITUDE
            currentLongitude = DEFAULT_LONGITUDE
            fetchWeatherInternal(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, selectedCategory.value)
        } catch (e: Exception) {
            _error.value = WeatherError.LocationUnavailable
            _isLoading.value = false
        }
    }

    /**
     * 날씨 API 호출
     */
    private suspend fun fetchWeatherInternal(
        latitude: Double,
        longitude: Double,
        category: DroneCategory
    ) {
        weatherRepository.fetchWeather(latitude, longitude, category)
            .onSuccess { data ->
                _weatherData.value = data
                _error.value = null
                _lastUpdateTime.value = System.currentTimeMillis()
            }
            .onFailure { _ ->
                _error.value = WeatherError.LoadFailed
            }
        _isLoading.value = false
    }
}

/**
 * 날씨 화면에서 발생할 수 있는 에러 종류.
 *
 * UI 는 [messageRes] 를 `stringResource()` 로 변환해 다국어 표시한다 (D-M9).
 */
sealed class WeatherError(@StringRes val messageRes: Int) {
    /** 위치 권한 거부됨 (Android 6.0+ runtime permission) */
    data object LocationPermission : WeatherError(R.string.weather_error_location_permission)

    /** 위치 서비스 비활성 / 마지막 위치도 없음 */
    data object LocationUnavailable : WeatherError(R.string.weather_error_location_unavailable)

    /** Open-Meteo API 호출 실패 (네트워크/서버 오류) */
    data object LoadFailed : WeatherError(R.string.weather_error_load_failed)

    /** 분류 불가 — 마지막 폴백 */
    data object Unknown : WeatherError(R.string.weather_error_unknown)
}
