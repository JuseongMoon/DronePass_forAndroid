package com.ScienceFiction.DronePassAndroid.feature.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.NotificationPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.KpIndexRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.core.data.storedEndDateAlarmEnabled
import com.ScienceFiction.DronePassAndroid.core.data.storedSunriseAlarmEnabled
import com.ScienceFiction.DronePassAndroid.core.data.storedSunsetAlarmEnabled
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthRepository
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthState
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
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

internal data class EndDateAlarmReconcilePlan(
    val cancelShapeIds: List<String>,
    val shapesToSchedule: List<ShapeModel>,
)

internal fun buildEndDateAlarmReconcilePlan(shapes: List<ShapeModel>): EndDateAlarmReconcilePlan {
    return EndDateAlarmReconcilePlan(
        cancelShapeIds = shapes.map { it.id },
        shapesToSchedule = shapes.filter { shape ->
            !shape.isDeleted && !shape.isExpired && shape.flightEndDate != null
        },
    )
}

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
        .map { preferences -> storedHideExpiredShapes(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 시작 전 도형 숨기기 */
    val hideNotStartedShapes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedHideNotStartedShapes(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 화면 항상 켜기 */
    val keepScreenAwake: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedKeepScreenAwake(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 일출 알림 활성화 */
    val sunriseAlarmEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedSunriseAlarmEnabled(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 일몰 알림 활성화 */
    val sunsetAlarmEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedSunsetAlarmEnabled(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 종료일 알림 활성화 */
    val endDateAlarmEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> storedEndDateAlarmEnabled(preferences) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 로그인 여부 */
    val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    /**
     * 현재 앱 언어 (iOS UserDefaults `AppleLanguages` 정합).
     * 앱 자체 저장값과 Android per-app language 를 함께 반영한다.
     */
    private val _currentLanguage = MutableStateFlow(resolveCurrentAppLanguage(appContext))
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    /**
     * 한국 특화 기능 활성화 (iOS `isKoreaFeaturesEnabled` 정합).
     * 첫 실행 기본값: 앱 언어가 한국어면 true, 그 외 false. 이후에는 저장값을 우선한다.
     * ON/OFF 전이 시 MapViewModel 이 collect 하여 모든 FlightZone 레이어를 해제한다.
     */
    val koreaFeaturesEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            resolveKoreaFeaturesEnabled(
                storedValue = storedKoreaFeaturesEnabled(preferences),
                language = resolveCurrentAppLanguage(appContext),
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            defaultKoreaFeaturesEnabled(appContext),
        )

    init {
        initializeKoreaFeaturesSetting()
        checkAuthState()
        // iOS settings.kp.current 정합 — 화면 진입 시 즉시 최신 Kp 값 표시.
        // KpViewModel 가 떠있지 않은 경우(설정만 단독 진입) 에도 currentKpFlow 가 채워지도록 1회 호출.
        viewModelScope.launch {
            runCatching { kpIndexRepository.getCurrentKp() }
        }
    }

    /**
     * 앱 언어 변경 — iOS `UserDefaults.set(...)` + `AppleLanguages` 정합.
     * 앱 자체 저장값과 Android per-app language 를 함께 갱신한다.
     */
    fun setLanguage(language: AppLanguage) {
        persistAppLanguage(appContext, language)
        applyAppLanguageToRuntime(appContext, language)
        _currentLanguage.value = language
    }

    /**
     * 한국 특화 기능 토글 (iOS `settingManager.isKoreaFeaturesEnabled = newValue` 정합).
     * ON/OFF 전이 시 MapViewModel 이 visibleLayers 를 모두 해제한다.
     */
    fun toggleKoreaFeatures(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SettingsPreferenceKeys.KOREA_FEATURES_ENABLED] = value
                preferences.remove(SettingsPreferenceKeys.LEGACY_KOREA_FEATURES_ENABLED)
            }
        }
    }

    private fun initializeKoreaFeaturesSetting() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                if (preferences[SettingsPreferenceKeys.KOREA_FEATURES_ENABLED] == null) {
                    val legacyValue = preferences[SettingsPreferenceKeys.LEGACY_KOREA_FEATURES_ENABLED]
                    preferences[SettingsPreferenceKeys.KOREA_FEATURES_ENABLED] =
                        legacyValue ?: defaultKoreaFeaturesEnabled()
                    preferences.remove(SettingsPreferenceKeys.LEGACY_KOREA_FEATURES_ENABLED)
                }
            }
        }
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
                preferences[SettingsPreferenceKeys.HIDE_EXPIRED_SHAPES] = value
                preferences.remove(SettingsPreferenceKeys.LEGACY_HIDE_EXPIRED_SHAPES)
            }
        }
    }

    /**
     * 시작 전 도형 숨기기 토글
     */
    fun toggleHideNotStartedShapes(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SettingsPreferenceKeys.HIDE_NOT_STARTED_SHAPES] = value
                preferences.remove(SettingsPreferenceKeys.LEGACY_HIDE_NOT_STARTED_SHAPES)
            }
        }
    }

    /**
     * 화면 항상 켜기 토글
     */
    fun toggleKeepScreenAwake(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SettingsPreferenceKeys.KEEP_SCREEN_AWAKE] = value
                preferences.remove(SettingsPreferenceKeys.LEGACY_KEEP_SCREEN_AWAKE)
            }
        }
    }

    /**
     * 일출 알림 토글
     */
    fun toggleSunriseAlarm(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[NotificationPreferenceKeys.SUNRISE_ALARM_ENABLED] = value
                preferences.remove(NotificationPreferenceKeys.LEGACY_SUNRISE_ALARM_ENABLED)
            }
            if (value) {
                val location = getUserLocationOrNull()
                if (location != null) {
                    val (lat, lon) = location
                    val weatherData = try {
                        weatherRepository.fetchWeather(lat, lon).getOrNull()
                    } catch (e: Exception) {
                        null
                    }
                    notificationScheduler.scheduleSunriseAlarms(
                        sunriseTimeStrings = weatherData?.sunriseTimes,
                        utcOffsetSeconds = weatherData?.utcOffsetSeconds,
                    )
                }
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
                preferences[NotificationPreferenceKeys.SUNSET_ALARM_ENABLED] = value
                preferences.remove(NotificationPreferenceKeys.LEGACY_SUNSET_ALARM_ENABLED)
            }
            if (value) {
                val location = getUserLocationOrNull()
                if (location != null) {
                    val (lat, lon) = location
                    val weatherData = try {
                        weatherRepository.fetchWeather(lat, lon).getOrNull()
                    } catch (e: Exception) {
                        null
                    }
                    notificationScheduler.scheduleSunsetAlarms(
                        sunsetTimeStrings = weatherData?.sunsetTimes,
                        utcOffsetSeconds = weatherData?.utcOffsetSeconds,
                    )
                }
            } else {
                notificationScheduler.cancelSunsetAlarms()
            }
        }
    }

    /**
     * 사용자의 현재 위치를 가져온다.
     * 위치를 가져올 수 없으면 iOS처럼 일출/일몰 알림 예약을 건너뛰도록 null을 반환한다.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getUserLocationOrNull(): Pair<Double, Double>? {
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
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "위치 조회 실패, 일출/일몰 알림 예약 건너뜀: ${e.message}")
            null
        }

        // 위치 캐시 갱신 (BootCompletedReceiver 가 재부팅 후 사용).
        if (resolved != null) {
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
                preferences[NotificationPreferenceKeys.END_DATE_ALARM_ENABLED] = value
                preferences.remove(NotificationPreferenceKeys.LEGACY_END_DATE_ALARM_ENABLED)
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
     */
    fun deleteAllExpiredShapes() {
        viewModelScope.launch {
            try {
                shapeRepository.deleteExpiredShapes()
            } catch (e: Exception) {
                Log.e(TAG, "만료된 도형 삭제 실패", e)
            }
        }
    }

    /**
     * 모든 활성 도형의 종료일 알림 재예약
     */
    private suspend fun rescheduleAllEndDateAlarms() {
        try {
            val plan = buildEndDateAlarmReconcilePlan(shapeRepository.getAllShapes().first())
            plan.cancelShapeIds.forEach { shapeId ->
                notificationScheduler.cancelEndDateAlarm(shapeId)
            }
            plan.shapesToSchedule.forEach { shape ->
                val flightEndDate = shape.flightEndDate ?: return@forEach
                notificationScheduler.scheduleEndDateAlarm(
                    shapeId = shape.id,
                    flightEndDate = flightEndDate,
                    shapeTitle = shape.title,
                )
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
            val shapes = shapeRepository.getAllShapes().first()
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
