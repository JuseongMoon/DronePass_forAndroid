package com.ScienceFiction.DronePassAndroid.feature.profile

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthRepository
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * iOS `ProfileView` 정합 ViewModel.
 * 동기화(클라우드 백업/실시간 sync) + 약관/정책 + 계정 관리(로그아웃·탈퇴) 흐름을 담당.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    private val sketchRepository: SketchRepository,
    private val firestore: FirebaseFirestore,
    private val realtimeSyncManager: RealtimeSyncManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
        private val KEY_CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
        private val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
    }

    /** 실시간 클라우드 동기화 활성화 (iOS `isCloudBackupEnabled` 정합). */
    val isCloudBackupEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[KEY_CLOUD_BACKUP_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 마지막 백업/동기화 시각 (UI 표시용 — DataStore 영속). */
    val lastBackupTime: StateFlow<Long?> = dataStore.data
        .map { preferences -> preferences[KEY_LAST_BACKUP_TIME] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** RealtimeSyncManager 의 마지막 실시간 동기화 시각 (메모리 상태 — UI 표시용). */
    val lastRealtimeSyncTime: StateFlow<Long?> = realtimeSyncManager.lastSyncTime

    /** 로그인 여부 — Firebase AuthStateListener 로 자동 갱신. */
    private val _isLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** 진행 중 동기화 (수동 백업·토글 ON 시 활성). */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** 로그아웃·탈퇴 진행 중 (UI 버튼 비활성화용). */
    private val _isAccountActionInProgress = MutableStateFlow(false)
    val isAccountActionInProgress: StateFlow<Boolean> = _isAccountActionInProgress.asStateFlow()

    /** 1회성 동기화 결과 알림 (Toast/SnackBar 용). */
    private val _syncResultMessage = MutableSharedFlow<SyncResult>()
    val syncResultMessage: SharedFlow<SyncResult> = _syncResultMessage.asSharedFlow()

    /**
     * 5종 동기화 상태 (iOS `realtimeCloudSyncStatusText` 정합).
     * isSyncing → Syncing
     * !isLoggedIn → LoginRequired
     * !cloudBackupEnabled → Disabled
     * isRealtimeSyncEnabled → Active
     * else → Waiting
     */
    val syncStatus: StateFlow<ProfileSyncStatus> = combine(
        _isSyncing,
        isLoggedIn,
        isCloudBackupEnabled,
        realtimeSyncManager.isRealtimeSyncEnabled,
    ) { syncing, loggedIn, cloudEnabled, realtimeEnabled ->
        when {
            syncing -> ProfileSyncStatus.Syncing
            !loggedIn -> ProfileSyncStatus.LoginRequired
            !cloudEnabled -> ProfileSyncStatus.Disabled
            realtimeEnabled -> ProfileSyncStatus.Active
            else -> ProfileSyncStatus.Waiting
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileSyncStatus.Disabled)

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _isLoggedIn.value = auth.currentUser != null
        }
    }

    /**
     * 실시간 클라우드 동기화 토글 (iOS `isCloudBackupEnabled` onChange 정합).
     * ON 전이 시 즉시 백업 + 실시간 리스너 재시작.
     */
    fun setCloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_CLOUD_BACKUP_ENABLED] = enabled }
            if (enabled && firebaseAuth.currentUser != null) {
                syncToCloudInternal(notifyResult = false)
                realtimeSyncManager.resetAndRestartRealtimeSync()
            } else if (!enabled) {
                runCatching { realtimeSyncManager.stopListening() }
            }
        }
    }

    /**
     * 수동 백업 (iOS `syncToCloud` 정합). 결과는 syncResultMessage 로 1회 emit.
     */
    fun syncToCloud() {
        viewModelScope.launch {
            syncToCloudInternal(notifyResult = true)
        }
    }

    private suspend fun syncToCloudInternal(notifyResult: Boolean) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        try {
            realtimeSyncManager.forceSyncNow()
            val now = System.currentTimeMillis()
            dataStore.edit { it[KEY_LAST_BACKUP_TIME] = now }
            if (notifyResult) {
                val shapeCount = shapeRepository.getActiveShapes().first().size
                _syncResultMessage.emit(SyncResult.Success(shapeCount))
            }
        } catch (e: Exception) {
            Log.e(TAG, "수동 백업 실패", e)
            if (notifyResult) {
                _syncResultMessage.emit(SyncResult.Failure(e.localizedMessage ?: "Unknown"))
            }
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * 로그아웃 (iOS `ProfileView.logout()` 정합).
     */
    fun signOut(onComplete: () -> Unit = {}) {
        if (_isAccountActionInProgress.value) return
        _isAccountActionInProgress.value = true
        viewModelScope.launch {
            // 1) FCM 토큰 비활성화 (userId 살아있는 동안)
            runCatching { FcmService.deactivateToken(appContext) }
                .onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패", it) }
            // 2) 실시간 동기화 리스너 중단
            runCatching { realtimeSyncManager.stopListening() }
                .onFailure { Log.w(TAG, "리스너 중단 실패", it) }
            // 3) Firebase Auth 로그아웃
            authRepository.signOut()
            _isAccountActionInProgress.value = false
            onComplete()
        }
    }

    /**
     * 계정 삭제 (iOS `ProfileView.deleteAccount()` 정합 — 2단계 확인 후 호출).
     */
    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        if (_isAccountActionInProgress.value) return
        _isAccountActionInProgress.value = true
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid

            runCatching { saveAnonymizedStats() }
                .onFailure { Log.e(TAG, "익명화 통계 저장 실패", it) }

            if (userId != null) {
                val firestoreResult = runCatching { deleteFirestoreUserData(userId) }
                if (firestoreResult.isFailure) {
                    val err = firestoreResult.exceptionOrNull()
                    Log.e(TAG, "Firestore 데이터 삭제 실패 — 계정 삭제 보류", err)
                    _isAccountActionInProgress.value = false
                    onResult(
                        false,
                        err?.localizedMessage
                            ?: "데이터 삭제에 실패했습니다. 네트워크 확인 후 다시 시도해 주세요.",
                    )
                    return@launch
                }
            }

            runCatching {
                shapeRepository.deleteAllShapes()
                droneRepository.deleteAllDrones()
                sketchRepository.deleteAllSketches()
            }.onFailure { Log.e(TAG, "로컬 DB 삭제 실패", it) }

            runCatching { FcmService.deactivateToken(appContext) }
            runCatching { realtimeSyncManager.stopListening() }

            authRepository.deleteAccount().fold(
                onSuccess = {
                    _isAccountActionInProgress.value = false
                    onResult(true, "계정이 삭제되었습니다.")
                },
                onFailure = { exception ->
                    _isAccountActionInProgress.value = false
                    onResult(false, exception.localizedMessage ?: "계정 삭제 중 오류가 발생했습니다.")
                },
            )
        }
    }

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

    private suspend fun saveAnonymizedStats() {
        val shapeCount = runCatching { shapeRepository.getActiveShapes().first().size }.getOrDefault(0)
        val droneCount = runCatching { droneRepository.getActiveDrones().first().size }.getOrDefault(0)
        val timestamp = System.currentTimeMillis()
        firestore.collection("analytics")
            .document("deleted_users")
            .collection("entries")
            .document(timestamp.toString())
            .set(
                mapOf(
                    "timestamp" to timestamp,
                    "shapeCount" to shapeCount,
                    "droneCount" to droneCount,
                    "platform" to "android",
                    "appVersion" to BuildConfig.VERSION_NAME,
                ),
            )
            .await()
    }

    /** 동기화 결과 — Toast/SnackBar 용 1회성 메시지. */
    sealed class SyncResult {
        data class Success(val shapeCount: Int) : SyncResult()
        data class Failure(val message: String) : SyncResult()
    }
}
