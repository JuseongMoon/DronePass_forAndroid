package com.ScienceFiction.DronePassAndroid.feature.settings

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
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
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
        private val KEY_HIDE_EXPIRED_SHAPES = booleanPreferencesKey("hide_expired_shapes")
        private val KEY_HIDE_NOT_STARTED_SHAPES = booleanPreferencesKey("hide_not_started_shapes")
        private val KEY_KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        private val KEY_SHOW_FLIGHT_ZONE_LAYERS = booleanPreferencesKey("show_flight_zone_layers")
        private val KEY_SUNRISE_ALARM_ENABLED = booleanPreferencesKey("sunrise_alarm_enabled")
        private val KEY_SUNSET_ALARM_ENABLED = booleanPreferencesKey("sunset_alarm_enabled")
        private val KEY_END_DATE_ALARM_ENABLED = booleanPreferencesKey("end_date_alarm_enabled")
    }

    /** 인증 상태 */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

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

    /** 비행구역 레이어 표시 */
    val showFlightZoneLayers: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_SHOW_FLIGHT_ZONE_LAYERS] ?: false }
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

    init {
        checkAuthState()
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
     * 비행구역 레이어 표시 토글
     */
    fun toggleShowFlightZoneLayers(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_SHOW_FLIGHT_ZONE_LAYERS] = value
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

    /**
     * 로그아웃
     *
     * 단일 책임 원칙을 위해 AuthViewModel.signOut() 과 동일한 정리 순서를 따른다:
     * 1) FCM 토큰 비활성화 (userId 가 살아있는 동안)
     * 2) 실시간 동기화 리스너 중단
     * 3) Firebase Auth 로그아웃
     */
    fun signOut() {
        runCatching {
            FcmService.deactivateToken(appContext)
        }.onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패", it) }

        runCatching {
            realtimeSyncManager.stopListening()
        }.onFailure { Log.w(TAG, "실시간 동기화 리스너 중단 실패", it) }

        authRepository.signOut()
        _authState.value = AuthState.LoggedOut
    }

    /**
     * 계정 삭제
     *
     * 1. 익명화된 사용 통계를 Firestore에 저장
     * 2. Firestore의 사용자 데이터 전체 삭제 (shapes, drones, sketches, metadata)
     * 3. 로컬 Room DB 전체 삭제
     * 4. Firebase Auth 계정 삭제
     */
    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid

            // 1. 익명화 통계 (분석용, 실패해도 진행)
            runCatching { saveAnonymizedStats() }
                .onFailure { Log.e(TAG, "익명화 통계 저장 실패", it) }

            // 2. Firestore 사용자 데이터 전체 삭제 — CRITICAL.
            //    여기서 실패하면 Auth 계정만 삭제되고 서버에 데이터가 남는 좀비 상태가 되므로
            //    fail-fast 로 Auth 삭제를 보류하고 사용자에게 재시도 요청.
            if (userId != null) {
                val firestoreResult = runCatching { deleteFirestoreUserData(userId) }
                if (firestoreResult.isFailure) {
                    val err = firestoreResult.exceptionOrNull()
                    Log.e(TAG, "Firestore 데이터 삭제 실패 — 계정 삭제 보류", err)
                    onResult(
                        false,
                        err?.localizedMessage ?: "데이터 삭제에 실패했습니다. 네트워크 확인 후 다시 시도해 주세요."
                    )
                    return@launch
                }
                Log.d(TAG, "Firestore 사용자 데이터 삭제 완료: userId=$userId")
            }

            // 3. 로컬 Room DB 전체 삭제 — 실패해도 진행 (앱 재설치/캐시 클리어로 복구 가능).
            runCatching {
                shapeRepository.deleteAllShapes()
                droneRepository.deleteAllDrones()
                sketchRepository.deleteAllSketches()
                Log.d(TAG, "로컬 DB 전체 삭제 완료")
            }.onFailure { Log.e(TAG, "로컬 DB 삭제 실패", it) }

            // 4. FCM 토큰 비활성화 + 실시간 동기화 리스너 중단 (Auth 삭제 전 정리)
            runCatching {
                FcmService.deactivateToken(appContext)
            }.onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패 (계정 삭제 흐름)", it) }
            runCatching {
                realtimeSyncManager.stopListening()
            }.onFailure { Log.w(TAG, "실시간 동기화 리스너 중단 실패 (계정 삭제 흐름)", it) }

            // 5. Firebase Auth 계정 삭제
            authRepository.deleteAccount().fold(
                onSuccess = {
                    _authState.value = AuthState.LoggedOut
                    onResult(true, "계정이 삭제되었습니다.")
                },
                onFailure = { exception ->
                    onResult(false, exception.localizedMessage ?: "계정 삭제 중 오류가 발생했습니다.")
                }
            )
        }
    }

    /**
     * Firestore에서 사용자의 모든 데이터를 삭제
     * users/{userId} 하위의 shapes, drones, sketches, metadata 컬렉션을 삭제
     */
    private suspend fun deleteFirestoreUserData(userId: String) {
        val userDoc = firestore.collection("users").document(userId)
        val collections = listOf("shapes", "drones", "sketches", "metadata")

        for (collectionName in collections) {
            val snapshot = userDoc.collection(collectionName).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        }
    }

    /**
     * 계정 삭제 전 익명화된 사용 통계를 Firestore에 저장
     *
     * analytics/deleted_users/{timestamp} 경로에 저장하며,
     * 개인 식별 정보 없이 사용 패턴 통계만 기록한다.
     */
    private suspend fun saveAnonymizedStats() {
        val user = firebaseAuth.currentUser ?: return

        // 데이터 카운트 수집
        val shapes = shapeRepository.getActiveShapes().first()
        val drones = droneRepository.getActiveDrones().first()
        val sketches = sketchRepository.getActiveSketches().first()

        // 사용 일수 계산 (계정 생성일 ~ 현재)
        val creationTimestamp = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
        val usageDays = (System.currentTimeMillis() - creationTimestamp) / (24 * 60 * 60 * 1000L)

        val stats = hashMapOf(
            "deletedAt" to FieldValue.serverTimestamp(),
            "shapeCount" to shapes.size,
            "droneCount" to drones.size,
            "sketchCount" to sketches.size,
            "usageDays" to usageDays,
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME
        )

        val timestamp = System.currentTimeMillis().toString()
        firestore.collection("analytics")
            .document("deleted_users")
            .collection("records")
            .document(timestamp)
            .set(stats)
            .await()

        Log.d(TAG, "익명화 통계 저장 완료: shapes=${shapes.size}, drones=${drones.size}, sketches=${sketches.size}, usageDays=$usageDays")
    }
}
