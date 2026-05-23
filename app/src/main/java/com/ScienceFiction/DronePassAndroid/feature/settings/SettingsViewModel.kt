package com.ScienceFiction.DronePassAndroid.feature.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.KpIndexRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthRepository
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthState
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.ScienceFiction.DronePassAndroid.service.NotificationScheduler
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val DEFAULT_LATITUDE = 37.5665
private const val DEFAULT_LONGITUDE = 126.9780

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val notificationScheduler: NotificationScheduler,
    private val weatherRepository: WeatherRepository,
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    private val sketchRepository: SketchRepository,
    private val firestore: FirebaseFirestore,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val realtimeSyncManager: RealtimeSyncManager,
    private val kpIndexRepository: KpIndexRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
        private val KEY_HIDE_EXPIRED_SHAPES = booleanPreferencesKey("hide_expired_shapes")
        private val KEY_HIDE_NOT_STARTED_SHAPES = booleanPreferencesKey("hide_not_started_shapes")
        private val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        private val KEY_SUNRISE_ALARM_ENABLED = booleanPreferencesKey("sunrise_alarm_enabled")
        private val KEY_SUNSET_ALARM_ENABLED = booleanPreferencesKey("sunset_alarm_enabled")
        private val KEY_END_DATE_ALARM_ENABLED = booleanPreferencesKey("end_date_alarm_enabled")
        private val KEY_KOREA_FEATURES_ENABLED = booleanPreferencesKey("korea_features_enabled")
    }

    /** 인증 상태 */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * 현재 Kp 지수 문자열 (iOS `viewModel.currentKPString` 정합 — "%.1f" 형식).
     * KpIndexRepository.currentKpFlow 를 구독하여 자동 갱신. 데이터 없으면 "-".
     */
    val currentKpString: StateFlow<String> = kpIndexRepository.currentKpFlow
        .map { kpData ->
            kpData?.kp?.let { String.format(java.util.Locale.ROOT, "%.1f", it) } ?: "-"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "-")

    /** 만료된 도형 숨기기 */
    val hideExpiredShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_HIDE_EXPIRED_SHAPES] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 시작 전 도형 숨기기 */
    val hideNotStartedShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_HIDE_NOT_STARTED_SHAPES] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 화면 항상 켜기 */
    val keepScreenAwake: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_KEEP_SCREEN_AWAKE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 일출 알림 활성화 */
    val sunriseAlarmEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_SUNRISE_ALARM_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 일몰 알림 활성화 */
    val sunsetAlarmEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_SUNSET_ALARM_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 종료일 알림 활성화 */
    val endDateAlarmEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_END_DATE_ALARM_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 로그인 여부 */
    val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    /**
     * 현재 앱 언어 (iOS UserDefaults `AppleLanguages` 정합).
     * AppCompatDelegate.getApplicationLocales 기반.
     */
    private val _currentLanguage = MutableStateFlow(readCurrentAppLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    /**
     * 한국 특화 기능 활성화 (iOS `isKoreaFeaturesEnabled` 정합).
     * 기본값: 시스템 언어 ko 면 true, 그 외 false.
     * OFF 전이 시 MapViewModel 이 collect 하여 모든 FlightZone 레이어를 해제한다.
     */
    val koreaFeaturesEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEY_KOREA_FEATURES_ENABLED]
                ?: (readCurrentAppLanguage() == AppLanguage.Korean)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        checkAuthState()
        // iOS settings.kp.current 정합 — 화면 진입 시 즉시 최신 Kp 값 표시.
        // KpViewModel 가 떠있지 않은 경우(설정만 단독 진입) 에도 currentKpFlow 가 채워지도록 1회 호출.
        viewModelScope.launch {
            runCatching { kpIndexRepository.getCurrentKp() }
        }
    }

    /**
     * 앱 언어 변경 — iOS `UserDefaults.set(...)` + `AppleLanguages` 정합.
     * AppCompatDelegate API 호출 → Activity 자동 재생성 → 전체 UI 언어 전환.
     * 호출자(UI)는 다이얼로그 안내 후 약간의 delay 를 두고 호출하여 dismiss animation 보장.
     */
    fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        _currentLanguage.value = language
    }

    /**
     * 한국 특화 기능 토글 (iOS `settingManager.isKoreaFeaturesEnabled = newValue` 정합).
     * OFF 전이 시 MapViewModel 이 visibleLayers 를 모두 해제한다.
     */
    fun toggleKoreaFeatures(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_KOREA_FEATURES_ENABLED] = value
            }
        }
    }

    private fun readCurrentAppLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!locales.isEmpty) locales.get(0)?.language else null
        return AppLanguage.fromTag(tag)
    }

    /**
     * 인증 상태 확인
     */
    fun checkAuthState() {
        val currentUser = firebaseAuth.currentUser
        _authState.value = if (currentUser != null) {
            AuthState.LoggedIn(currentUser)
        } else {
            AuthState.LoggedOut
        }
    }

    /**
     * 만료 도형 숨기기 토글
     */
    fun toggleHideExpiredShapes(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_HIDE_EXPIRED_SHAPES] = value
            }
        }
    }

    /**
     * 시작 전 도형 숨기기 토글
     */
    fun toggleHideNotStartedShapes(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_HIDE_NOT_STARTED_SHAPES] = value
            }
        }
    }

    /**
     * 화면 항상 켜기 토글
     */
    fun toggleKeepScreenAwake(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_KEEP_SCREEN_AWAKE] = value
            }
        }
    }

    /**
     * 일출 알림 토글
     */
    fun toggleSunriseAlarm(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SUNRISE_ALARM_ENABLED] = value
            }
            if (value) {
                val (lat, lon) = getUserLocation()
                val weatherData = try {
                    weatherRepository.fetchWeather(lat, lon).getOrNull()
                } catch (e: Exception) {
                    null
                }
                notificationScheduler.scheduleSunriseAlarms(weatherData?.sunrise)
            } else {
                notificationScheduler.cancelSunriseAlarms()
            }
        }
    }

    /**
     * 일몰 알림 토글
     */
    fun toggleSunsetAlarm(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SUNSET_ALARM_ENABLED] = value
            }
            if (value) {
                val (lat, lon) = getUserLocation()
                val weatherData = try {
                    weatherRepository.fetchWeather(lat, lon).getOrNull()
                } catch (e: Exception) {
                    null
                }
                notificationScheduler.scheduleSunsetAlarms(weatherData?.sunset)
            } else {
                notificationScheduler.cancelSunsetAlarms()
            }
        }
    }

    /**
     * 사용자의 현재 위치를 가져온다.
     * 위치를 가져올 수 없는 경우 기본 좌표(서울)를 반환한다.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getUserLocation(): Pair<Double, Double> {
        val resolved = try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()
            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    Pair(lastLocation.latitude, lastLocation.longitude)
                } else {
                    Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "위치 조회 실패, 기본 좌표 사용: ${e.message}")
            Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        }

        // 위치 캐시 갱신 (BootCompletedReceiver 가 재부팅 후 사용).
        // 폴백 좌표는 캐시하지 않아 다음 실행 때 재시도가 가능하게 한다.
        if (resolved.first != DEFAULT_LATITUDE || resolved.second != DEFAULT_LONGITUDE) {
            runCatching {
                dataStore.edit { prefs ->
                    prefs[UserLocationKeys.KEY_LAST_LATITUDE] = resolved.first
                    prefs[UserLocationKeys.KEY_LAST_LONGITUDE] = resolved.second
                }
            }.onFailure { Log.w(TAG, "위치 캐시 갱신 실패", it) }
        }

        return resolved
    }

    /**
     * 종료일 알림 토글
     */
    fun toggleEndDateAlarm(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_END_DATE_ALARM_ENABLED] = value
            }
            if (value) {
                rescheduleAllEndDateAlarms()
            } else {
                cancelAllEndDateAlarms()
            }
        }
    }

    /**
     * 만료된 도형 전체 삭제
     * 모든 활성 도형 중 isExpired == true인 도형을 소프트 삭제
     *
     * @param onResult 삭제 결과 콜백 (삭제된 도형 수)
     */
    fun deleteAllExpiredShapes(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val shapes = shapeRepository.getActiveShapes().first()
                val expiredShapes = shapes.filter { it.isExpired }
                expiredShapes.forEach { shape ->
                    shapeRepository.softDeleteShape(shape)
                }
                onResult(expiredShapes.size)
            } catch (e: Exception) {
                onResult(0)
            }
        }
    }

    /**
     * 모든 활성 도형의 종료일 알림 재예약
     */
    private suspend fun rescheduleAllEndDateAlarms() {
        try {
            val shapes = shapeRepository.getActiveShapes().first()
            shapes.forEach { shape ->
                val endDate = shape.flightEndDate
                if (endDate != null && !shape.isExpired) {
                    notificationScheduler.scheduleEndDateAlarm(shape.id, endDate)
                }
            }
        } catch (e: Exception) {
            // 실패 시 무시
        }
    }

    /**
     * 모든 활성 도형의 종료일 알림 취소
     */
    private suspend fun cancelAllEndDateAlarms() {
        try {
            val shapes = shapeRepository.getActiveShapes().first()
            shapes.forEach { shape ->
                notificationScheduler.cancelEndDateAlarm(shape.id)
            }
        } catch (e: Exception) {
            // 실패 시 무시
        }
    }

    // signOut / deleteAccount / saveAnonymizedStats / deleteFirestoreUserData 함수는
    // ProfileViewModel 로 이전되어 ProfileView 시트(약관/계정 관리 섹션)에서 호출된다.
}
