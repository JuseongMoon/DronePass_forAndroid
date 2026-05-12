package com.ScienceFiction.DronePassAndroid.feature.kp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.ScienceFiction.DronePassAndroid.core.data.repository.KpIndexRepository
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 자동 갱신 Job. Composable 의 ON_START/ON_STOP 라이프사이클에 맞춰 시작/중단된다.
     * 백그라운드에서 무한 새로고침이 도는 것을 차단해 배터리/요금을 절약한다.
     */
    private var autoRefreshJob: Job? = null

    init {
        // 초기 1회 로드만 init 에서 수행. 자동 갱신은 화면이 START 될 때만 시작.
        loadKpData()
    }

    /**
     * Kp 지수 데이터 로드
     */
    fun loadKpData() {
        analyticsLogger.logKpViewed()
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // 현재 Kp 조회
            kpRepository.getCurrentKp().fold(
                onSuccess = { data ->
                    _currentKp.value = data
                    _kpLevel.value = KpLevel.fromKp(data.kp)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )

            // 예보 데이터 조회
            kpRepository.getForecast().fold(
                onSuccess = { forecast ->
                    _forecastData.value = forecast
                },
                onFailure = { /* 현재 Kp만 있어도 OK */ }
            )

            // 27일 장기 예보 조회
            kpRepository.get27DayForecast().fold(
                onSuccess = { forecast ->
                    _longTermForecast.value = forecast
                },
                onFailure = { /* 장기 예보는 선택사항 */ }
            )

            _lastUpdated.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            _isLoading.value = false
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
                loadKpData()
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
}
