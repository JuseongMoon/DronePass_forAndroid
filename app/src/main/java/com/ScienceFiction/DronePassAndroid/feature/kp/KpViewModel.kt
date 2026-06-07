package com.ScienceFiction.DronePassAndroid.feature.kp

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.data.repository.KpIndexRepository
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

internal enum class KpDataLoadTrigger {
    Initial,
    UserRefresh,
    AutoRefresh,
}

internal data class KpDataLoadPlan(
    val fetchCurrent: Boolean,
    val fetchForecast: Boolean,
    val fetchLongTermForecast: Boolean,
    val forceRefresh: Boolean,
)

internal fun resolveKpDataLoadPlan(
    trigger: KpDataLoadTrigger,
    hasCurrentKp: Boolean,
): KpDataLoadPlan {
    return KpDataLoadPlan(
        fetchCurrent = when (trigger) {
            KpDataLoadTrigger.Initial -> !hasCurrentKp
            KpDataLoadTrigger.UserRefresh -> false
            KpDataLoadTrigger.AutoRefresh -> false
        },
        fetchForecast = true,
        fetchLongTermForecast = true,
        forceRefresh = true,
    )
}

/**
 * Kp 지수 ViewModel
 *
 * 현재 Kp 수치, 레벨, 24시간 예보 데이터를 관리한다.
 * 5분 간격으로 자동 갱신한다.
 */
@HiltViewModel
class KpViewModel @Inject constructor(
    private val kpRepository: KpIndexRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1000L // 5분
    }

    private val _currentKp = MutableStateFlow<KpIndexData?>(null)
    val currentKp: StateFlow<KpIndexData?> = _currentKp.asStateFlow()

    private val _kpLevel = MutableStateFlow(KpLevel.NORMAL)
    val kpLevel: StateFlow<KpLevel> = _kpLevel.asStateFlow()

    private val _forecastData = MutableStateFlow<List<KpIndexData>>(emptyList())
    val forecastData: StateFlow<List<KpIndexData>> = _forecastData.asStateFlow()

    private val _longTermForecast = MutableStateFlow<List<Kp27DayForecast>>(emptyList())
    val longTermForecast: StateFlow<List<Kp27DayForecast>> = _longTermForecast.asStateFlow()

    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated: StateFlow<String?> = _lastUpdated.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _refreshCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshCompleted: SharedFlow<Unit> = _refreshCompleted

    /**
     * 에러 상태. UI 에서 `stringResource(error.messageRes)` 로 다국어 변환한다 (D-M9).
     */
    private val _errorMessage = MutableStateFlow<KpError?>(null)
    val errorMessage: StateFlow<KpError?> = _errorMessage.asStateFlow()

    /**
     * 자동 갱신 Job. Composable 의 ON_START/ON_STOP 라이프사이클에 맞춰 시작/중단된다.
     * 백그라운드에서 무한 새로고침이 도는 것을 차단해 배터리/요금을 절약한다.
     */
    private var autoRefreshJob: Job? = null

    init {
        observeCurrentKp()
        // iOS KPForecastView 와 동일하게 화면 진입 시 예보(NOAA)만 강제 갱신한다.
        // 단, 앱 공통 현재 KP(GFZ)가 아직 비어 있는 직접 진입 상황에서는 1회만 보충한다.
        loadKpData(trigger = KpDataLoadTrigger.Initial, showRefreshMessage = false)
    }

    /**
     * Kp 예보 데이터 로드.
     *
     * iOS `KPForecastView` 의 refresh / auto refresh 는
     * `fetchKPData(forceRefresh: true, fetchGFZ: false, fetchNOAA: true)` 이므로
     * 수동 갱신에서는 현재 KP(GFZ)를 다시 요청하지 않는다.
     * 화면 자동 갱신도 iOS `KPForecastView` 의 5분 루프와 같이 NOAA 예보만 갱신한다.
     */
    fun loadKpData(showRefreshMessage: Boolean = true) {
        loadKpData(trigger = KpDataLoadTrigger.UserRefresh, showRefreshMessage = showRefreshMessage)
    }

    private fun loadKpData(
        trigger: KpDataLoadTrigger,
        showRefreshMessage: Boolean,
    ) {
        analyticsLogger.logKpViewed()
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val plan = resolveKpDataLoadPlan(
                trigger = trigger,
                hasCurrentKp = _currentKp.value != null || kpRepository.currentKpFlow.value != null,
            )

            var currentFailed = false
            var forecastFailed = false
            var longTermFailed = false

            if (plan.fetchCurrent) {
                kpRepository.getCurrentKp(forceRefresh = plan.forceRefresh).fold(
                    onSuccess = { data ->
                        updateCurrentKp(data)
                    },
                    onFailure = {
                        currentFailed = true
                    },
                )
            }

            // 예보 데이터 조회
            if (plan.fetchForecast) {
                kpRepository.getForecast(forceRefresh = plan.forceRefresh).fold(
                    onSuccess = { forecast ->
                        _forecastData.value = forecast
                    },
                    onFailure = {
                        forecastFailed = true
                    },
                )
            }

            // 27일 장기 예보 조회
            if (plan.fetchLongTermForecast) {
                kpRepository.get27DayForecast(forceRefresh = plan.forceRefresh).fold(
                    onSuccess = { forecast ->
                        _longTermForecast.value = forecast
                    },
                    onFailure = {
                        longTermFailed = true
                    },
                )
            }

            if (
                forecastFailed &&
                longTermFailed &&
                _forecastData.value.isEmpty() &&
                _longTermForecast.value.isEmpty()
            ) {
                _errorMessage.value = if (currentFailed && _currentKp.value == null) {
                    KpError.LoadFailed
                } else {
                    KpError.ForecastFailed
                }
            }

            _lastUpdated.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            _isLoading.value = false
            if (showRefreshMessage) {
                _refreshCompleted.tryEmit(Unit)
            }
        }
    }

    /**
     * 화면이 START 상태일 때만 5분 간격 자동 갱신을 시작한다.
     * Composable 의 DisposableEffect(ON_START) 에서 호출한다.
     */
    fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                loadKpData(trigger = KpDataLoadTrigger.AutoRefresh, showRefreshMessage = true)
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

    private fun observeCurrentKp() {
        viewModelScope.launch {
            kpRepository.currentKpFlow.collect { data ->
                if (data != null) {
                    updateCurrentKp(data)
                }
            }
        }
    }

    private fun updateCurrentKp(data: KpIndexData) {
        _currentKp.value = data
        _kpLevel.value = KpLevel.fromKp(data.kp)
    }
}

/**
 * Kp 화면에서 발생할 수 있는 에러 종류.
 *
 * UI 는 [messageRes] 를 `stringResource()` 로 변환해 다국어 표시한다 (D-M9).
 */
sealed class KpError(@StringRes val messageRes: Int) {
    /** NOAA SWPC / GFZ Potsdam 모두 실패 — 현재 Kp 조회 불가 */
    data object LoadFailed : KpError(R.string.kp_error_load_failed)

    /** 예보 데이터 로드 실패 (현재 Kp 만 있음) */
    data object ForecastFailed : KpError(R.string.kp_error_forecast_failed)

    /** 분류 불가 — 마지막 폴백 */
    data object Unknown : KpError(R.string.kp_error_unknown)
}
