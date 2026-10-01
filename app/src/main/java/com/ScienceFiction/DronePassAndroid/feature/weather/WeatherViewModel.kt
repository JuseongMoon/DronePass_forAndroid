package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.ScienceFiction.DronePassAndroid.core.location.DeviceLocation
import com.ScienceFiction.DronePassAndroid.core.location.DeviceLocationReader
import com.ScienceFiction.DronePassAndroid.core.location.DeviceLocationResult
import com.ScienceFiction.DronePassAndroid.core.location.LocationConsentRepository
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsagePurpose
import com.ScienceFiction.DronePassAndroid.core.location.MapCenterStore
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import com.ScienceFiction.DronePassAndroid.service.NotificationScheduleRestorer
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherServiceException
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherServiceFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WEATHER_GPS_ACCURACY_THRESHOLD_METERS = 50.0

internal val WeatherDroneCategoryPreferenceKey = stringPreferencesKey("selectedDroneCategory")
internal val LegacyWeatherDroneCategoryPreferenceKey = stringPreferencesKey("weather_drone_category")
internal const val WeatherAutoRefreshIntervalMs = 3 * 60 * 1000L

internal fun storedWeatherDroneCategory(preferences: Preferences): DroneCategory {
    return DroneCategory.fromStoredValue(preferences[WeatherDroneCategoryPreferenceKey])
        ?: DroneCategory.fromStoredValue(preferences[LegacyWeatherDroneCategoryPreferenceKey])
        ?: DroneCategory.IosDefault
}

internal fun shouldFetchWeatherAfterCategorySelection(
    refreshWeather: Boolean = true,
): Boolean {
    return refreshWeather
}

/** 날씨·일출/일몰의 기준 위치. 위치 동의가 없으면 지도 중심을 쓰고 화면에 그 사실을 표시한다. */
enum class WeatherLocationBasis {
    DEVICE,
    MAP_CENTER,
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val deviceLocationReader: DeviceLocationReader,
    private val locationConsentRepository: LocationConsentRepository,
    private val mapCenterStore: MapCenterStore,
    private val dataStore: DataStore<Preferences>,
    private val analyticsLogger: AnalyticsLogger,
    private val notificationScheduleRestorer: NotificationScheduleRestorer,
) : ViewModel() {

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

    private val _locationAccuracyMeters = MutableStateFlow<Double?>(null)
    val locationAccuracyMeters: StateFlow<Double?> = _locationAccuracyMeters.asStateFlow()

    private val _isUsingGps = MutableStateFlow(false)
    val isUsingGps: StateFlow<Boolean> = _isUsingGps.asStateFlow()

    private val _locationBasis = MutableStateFlow(WeatherLocationBasis.DEVICE)
    val locationBasis: StateFlow<WeatherLocationBasis> = _locationBasis.asStateFlow()

    private val _refreshCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshCompleted: SharedFlow<Unit> = _refreshCompleted

    val selectedCategory: StateFlow<DroneCategory> = dataStore.data
        .map { preferences -> storedWeatherDroneCategory(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DroneCategory.IosDefault)

    /**
     * 자동 갱신 Job. Composable 의 ON_START/ON_STOP 라이프사이클에 맞춰 시작/중단된다.
     * 백그라운드 무한 새로고침으로 인한 배터리/요금 부담을 차단한다.
     */
    private var autoRefreshJob: Job? = null

    init {
        // 초기 1회 로드만 init 에서. 자동 갱신은 화면이 START 될 때만 시작.
        refreshWeather(showRefreshMessage = false)
        // 위치 동의·철회 즉시 기준 위치를 바꿔 다시 불러온다(철회하면 기기 위치 사용을 바로 멈춘다).
        viewModelScope.launch {
            locationConsentRepository.allowed.drop(1).collect {
                weatherRepository.invalidateCache()
                fetchCurrentLocationAndWeather()
            }
        }
    }

    /**
     * 드론 카테고리 변경.
     *
     * iOS 는 WeatherForecastView 의 현재 날씨 카드에서 선택할 때만 날씨를 다시 가져오고,
     * WeatherInfoView 의 설명용 선택 메뉴에서는 저장값만 바꾼다.
     */
    fun setCategory(category: DroneCategory, refreshWeather: Boolean = true) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[WeatherDroneCategoryPreferenceKey] = category.iosRawValue
                preferences.remove(LegacyWeatherDroneCategoryPreferenceKey)
            }
            // WeatherForecastView 는 카테고리 변경 시 현재 위치 기준으로 강제 갱신한다.
            if (
                shouldFetchWeatherAfterCategorySelection(
                    refreshWeather = refreshWeather,
                )
            ) {
                weatherRepository.invalidateCache()
                fetchCurrentLocationAndWeather(categoryOverride = category)
            }
        }
    }

    /**
     * 위치 권한이 새로 허용된 직후 한 번 다시 불러온다.
     * 첫 실행에서는 권한을 허용하기 전에 보낸 요청이 권한 오류로 끝나, 그대로 두면 다음 자동 갱신(3분)까지
     * "위치 권한이 필요합니다" 가 남는다.
     */
    fun refreshAfterLocationPermissionGranted() {
        viewModelScope.launch { fetchCurrentLocationAndWeather() }
    }

    /**
     * 수동 갱신
     */
    fun refreshWeather(showRefreshMessage: Boolean = true) {
        analyticsLogger.logWeatherViewed()
        viewModelScope.launch {
            weatherRepository.invalidateCache()
            fetchCurrentLocationAndWeather()
            if (showRefreshMessage) {
                _refreshCompleted.tryEmit(Unit)
            }
        }
    }

    /**
     * 화면이 START 상태일 때만 3분 간격 자동 갱신을 시작한다.
     * Composable 의 DisposableEffect(ON_START) 에서 호출한다.
     *
     * 앱으로 돌아오면(START) 곧바로 한 번 갱신한다. 유효 기간 안의 요청은 중계 서버 캐시가
     * 받아 주므로 Apple 호출이 늘지 않고, 시작 직후 init 갱신과 겹치면 리포지토리가 한 번만 보낸다.
     */
    fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            fetchCurrentLocationAndWeather()
            while (isActive) {
                delay(WeatherAutoRefreshIntervalMs)
                weatherRepository.invalidateCache()
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
     * 현재 위치 가져와서 날씨 조회.
     * 위치 동의가 없으면 기기 위치를 읽지 않고, 그 시점의 지도 중심으로 요청한다.
     */
    private suspend fun fetchCurrentLocationAndWeather(categoryOverride: DroneCategory? = null) {
        _isLoading.value = true
        _error.value = null

        when (
            val result = deviceLocationReader.read(
                LocationUsagePurpose.WEATHER,
                LocationUsagePurpose.SUNRISE_SUNSET,
            )
        ) {
            is DeviceLocationResult.Available -> fetchWeatherForUserLocation(
                location = result.location,
                categoryOverride = categoryOverride,
            )
            DeviceLocationResult.NotAllowed -> fetchWeatherForMapCenter(categoryOverride)
            DeviceLocationResult.PermissionDenied -> failWeatherLocation(WeatherError.LocationPermission)
            DeviceLocationResult.Unavailable -> failWeatherLocation(WeatherError.LocationUnavailable)
        }
    }

    private suspend fun fetchWeatherForUserLocation(
        location: DeviceLocation,
        categoryOverride: DroneCategory? = null,
    ) {
        _locationBasis.value = WeatherLocationBasis.DEVICE
        updateLocationAccuracy(location)
        fetchWeatherInternal(
            latitude = location.latitude,
            longitude = location.longitude,
            category = categoryOverride ?: selectedCategory.value,
            isUserLocationBacked = true,
        )
    }

    private suspend fun fetchWeatherForMapCenter(categoryOverride: DroneCategory? = null) {
        _locationBasis.value = WeatherLocationBasis.MAP_CENTER
        clearLocationAccuracy()
        val center = mapCenterStore.weatherBasisCenter()
        fetchWeatherInternal(
            latitude = center.latitude,
            longitude = center.longitude,
            category = categoryOverride ?: selectedCategory.value,
            isUserLocationBacked = false,
        )
    }

    private fun failWeatherLocation(error: WeatherError) {
        _error.value = error
        clearLocationAccuracy()
        _isLoading.value = false
    }

    private fun updateLocationAccuracy(location: DeviceLocation) {
        val state = resolveWeatherLocationAccuracy(location.accuracyMeters)
        _locationAccuracyMeters.value = state.accuracyMeters
        _isUsingGps.value = state.isUsingGps
    }

    private fun clearLocationAccuracy() {
        _locationAccuracyMeters.value = null
        _isUsingGps.value = false
    }

    /**
     * 날씨 API 호출.
     * 일출·일몰 알림은 날씨를 받은 같은 기준 위치(기기 위치 또는 지도 중심)로 다시 예약한다.
     * 기기 위치 캐시는 동의 상태에서 기기 위치로 받았을 때만 남긴다.
     */
    private suspend fun fetchWeatherInternal(
        latitude: Double,
        longitude: Double,
        category: DroneCategory,
        isUserLocationBacked: Boolean,
    ) {
        weatherRepository.fetchWeather(latitude, longitude, category)
            .onSuccess { data ->
                _weatherData.value = data
                _error.value = null
                // 중계 서버가 Apple 응답을 받은 시각. 만료 캐시(stale)를 받으면 그 시각이 그대로 보인다.
                _lastUpdateTime.value = data.fetchedAtMillis ?: System.currentTimeMillis()
                if (isUserLocationBacked) {
                    runCatching { cacheSunAlarmLocation(latitude, longitude) }
                }
                runCatching {
                    val scheduled = notificationScheduleRestorer.rescheduleSunAlarmsForWeatherData(data)
                    if (scheduled && isUserLocationBacked) {
                        locationConsentRepository.recordUsage(LocationUsagePurpose.SUNRISE_ALERT)
                    }
                }
            }
            .onFailure { cause ->
                _error.value = resolveWeatherLoadError(cause)
            }
        _isLoading.value = false
    }

    private suspend fun cacheSunAlarmLocation(latitude: Double, longitude: Double) {
        // 읽는 도중 철회됐다면 캐시를 다시 남기지 않는다.
        if (!locationConsentRepository.isAllowed()) return
        dataStore.edit { preferences ->
            preferences[UserLocationKeys.KEY_LAST_LATITUDE] = latitude
            preferences[UserLocationKeys.KEY_LAST_LONGITUDE] = longitude
        }
    }
}

internal data class WeatherLocationAccuracyState(
    val accuracyMeters: Double?,
    val isUsingGps: Boolean,
)

internal fun resolveWeatherLocationAccuracy(accuracyMeters: Float?): WeatherLocationAccuracyState {
    val normalizedAccuracy = accuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?.toDouble()

    return WeatherLocationAccuracyState(
        accuracyMeters = normalizedAccuracy,
        isUsingGps = normalizedAccuracy != null && normalizedAccuracy <= WEATHER_GPS_ACCURACY_THRESHOLD_METERS,
    )
}

/**
 * 날씨 화면에서 발생할 수 있는 에러 종류.
 *
 * UI 는 [messageRes] 를 `stringResource()` 로 변환해 다국어 표시한다 (D-M9).
 */
sealed class WeatherError(
    @StringRes val messageRes: Int,
    val formatArg: String? = null,
) {
    /** 위치 권한 거부됨 (Android 6.0+ runtime permission) */
    data object LocationPermission : WeatherError(R.string.weather_error_location_permission)

    /** 위치 서비스 비활성 / 마지막 위치도 없음 */
    data object LocationUnavailable : WeatherError(R.string.weather_error_location_unavailable)

    /** 중계 서버 상한 초과 등으로 줄 수 있는 날씨가 없음 (`unavailable`) */
    data object ServiceUnavailable : WeatherError(R.string.weather_error_service_unavailable)

    /** App Check 검증 실패 (`permission-denied`) — 비공식 빌드나 오래된 앱 */
    data object AppVerification : WeatherError(R.string.weather_error_app_verification)

    /** 날씨 호출 실패 (네트워크 오류 등) */
    data class LoadFailed(
        val detail: String?,
    ) : WeatherError(
        messageRes = if (detail == null) {
            R.string.weather_error_load_failed
        } else {
            R.string.weather_error_load_failed_detail
        },
        formatArg = detail,
    )

    /** 분류 불가 — 마지막 폴백 */
    data object Unknown : WeatherError(R.string.weather_error_unknown)
}

internal fun resolveWeatherLoadError(cause: Throwable): WeatherError = when (
    (cause as? WeatherServiceException)?.failure
) {
    WeatherServiceFailure.UNAVAILABLE -> WeatherError.ServiceUnavailable
    WeatherServiceFailure.APP_VERIFICATION -> WeatherError.AppVerification
    else -> WeatherError.LoadFailed(cause.localizedMessage)
}
