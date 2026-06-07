package com.ScienceFiction.DronePassAndroid.feature.profile

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncState
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthRepository
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

internal val FIRESTORE_USER_SUBCOLLECTIONS_TO_DELETE = listOf(
    "shapes",
    "drones",
    "sketches",
    "metadata",
    "devices",
)

private const val FIRESTORE_BATCH_LIMIT = 500
internal const val FIRESTORE_USER_DELETE_MAX_ATTEMPTS = 3
private const val FIRESTORE_USER_DELETE_RETRY_DELAY_MS = 1_000L

internal fun chunkFirestoreDocumentIdsForBatchDelete(documentIds: List<String>): List<List<String>> {
    return documentIds.chunked(FIRESTORE_BATCH_LIMIT)
}

internal fun shouldRetryFirestoreUserDelete(completedAttempts: Int): Boolean {
    return completedAttempts < FIRESTORE_USER_DELETE_MAX_ATTEMPTS
}

internal fun shouldContinueAccountDeletionAfterFirestoreDeleteFailure(): Boolean = true

internal fun shouldNotifyProfileSyncResultForCloudToggle(
    enabled: Boolean,
    isLoggedIn: Boolean,
): Boolean {
    return enabled && isLoggedIn
}

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
    private val firestore: FirebaseFirestore,
    private val realtimeSyncManager: RealtimeSyncManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    /** 실시간 클라우드 동기화 활성화 (iOS `isCloudBackupEnabled` 정합). */
    val isCloudBackupEnabled: StateFlow<Boolean> = dataStore.data
        .map(::storedCloudBackupEnabled)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 마지막 백업/동기화 시각 (UI 표시용 — DataStore 영속). */
    val lastBackupTime: StateFlow<Long?> = dataStore.data
        .map(::storedLastBackupTime)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** RealtimeSyncManager 의 마지막 실시간 동기화 시각 (메모리 상태 — UI 표시용). */
    val lastRealtimeSyncTime: StateFlow<Long?> = realtimeSyncManager.lastSyncTime

    /** 로그인 여부 — Firebase AuthStateListener 로 자동 갱신. */
    private val _isLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    /** 진행 중 동기화 (수동 백업·토글 ON 시 활성). */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = combine(
        _isSyncing,
        realtimeSyncManager.syncState,
    ) { manualSyncing, syncState ->
        manualSyncing || syncState is SyncState.Syncing
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 로그아웃·탈퇴 진행 중 (UI 버튼 비활성화용). */
    private val _isAccountActionInProgress = MutableStateFlow(false)
    val isAccountActionInProgress: StateFlow<Boolean> = _isAccountActionInProgress.asStateFlow()

    /** 1회성 동기화 결과 알림 (iOS alert 정합). */
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
        isSyncing,
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
            dataStore.edit {
                it[ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED] = enabled
                it.remove(ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED)
            }
            if (enabled && firebaseAuth.currentUser != null) {
                syncToCloudInternal(
                    notifyResult = shouldNotifyProfileSyncResultForCloudToggle(
                        enabled = enabled,
                        isLoggedIn = true,
                    ),
                )
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
            dataStore.edit {
                it[ProfilePreferenceKeys.LAST_BACKUP_TIME] = now
                it.remove(ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME)
            }
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
            // 1) 로그아웃 직전 로컬 데이터를 Firebase 로 동기화 (iOS ProfileView.logout 정합).
            if (firebaseAuth.currentUser != null) {
                runCatching { realtimeSyncManager.forceSyncNow() }
                    .onFailure { Log.w(TAG, "로그아웃 전 동기화 실패", it) }
            }
            // 2) FCM 토큰 비활성화 (userId 살아있는 동안)
            runCatching { FcmService.deactivateToken(appContext) }
                .onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패", it) }
            // 3) 실시간 동기화 리스너 중단
            runCatching { realtimeSyncManager.stopListening() }
                .onFailure { Log.w(TAG, "리스너 중단 실패", it) }
            // 4) Firebase Auth 로그아웃
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

            runCatching { FcmService.deactivateToken(appContext) }
                .onFailure { Log.w(TAG, "FCM 토큰 비활성화 실패", it) }

            if (userId != null) {
                val firestoreResult = runCatching { deleteFirestoreUserData(userId) }
                if (firestoreResult.isFailure) {
                    val err = firestoreResult.exceptionOrNull()
                    Log.w(TAG, "Firestore 데이터 삭제 실패 — iOS처럼 Auth 계정 삭제는 계속 진행", err)
                    if (!shouldContinueAccountDeletionAfterFirestoreDeleteFailure()) {
                        _isAccountActionInProgress.value = false
                        onResult(
                            false,
                            err?.localizedMessage
                                ?: appContext.getString(R.string.profile_delete_data_error),
                        )
                        return@launch
                    }
                }
            }

            dataStore.edit {
                it[ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED] = false
                it.remove(ProfilePreferenceKeys.LAST_BACKUP_TIME)
                it.remove(ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED)
                it.remove(ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME)
            }

            runCatching { realtimeSyncManager.stopListening() }

            authRepository.deleteAccount().fold(
                onSuccess = {
                    _isAccountActionInProgress.value = false
                    onResult(true, appContext.getString(R.string.profile_delete_account_success))
                },
                onFailure = { exception ->
                    _isAccountActionInProgress.value = false
                    onResult(false, exception.localizedMessage ?: appContext.getString(R.string.profile_delete_account_error))
                },
            )
        }
    }

    private suspend fun deleteFirestoreUserData(userId: String) {
        var completedAttempts = 0
        var lastError: Throwable? = null

        while (shouldRetryFirestoreUserDelete(completedAttempts)) {
            completedAttempts += 1
            try {
                deleteFirestoreUserDataOnce(userId)
                return
            } catch (error: Throwable) {
                lastError = error
                if (shouldRetryFirestoreUserDelete(completedAttempts)) {
                    Log.w(TAG, "Firestore 데이터 삭제 실패, 재시도 ${completedAttempts}/$FIRESTORE_USER_DELETE_MAX_ATTEMPTS", error)
                    delay(FIRESTORE_USER_DELETE_RETRY_DELAY_MS)
                }
            }
        }

        throw lastError ?: IllegalStateException()
    }

    private suspend fun deleteFirestoreUserDataOnce(userId: String) {
        val userDoc = firestore.collection("users").document(userId)
        for (collectionName in FIRESTORE_USER_SUBCOLLECTIONS_TO_DELETE) {
            deleteFirestoreUserSubcollection(userDoc, collectionName)
        }
        userDoc.delete().await()
    }

    private suspend fun deleteFirestoreUserSubcollection(
        userDoc: DocumentReference,
        collectionName: String,
    ) {
        val snapshot = userDoc.collection(collectionName).get().await()
        val batches = chunkFirestoreDocumentIdsForBatchDelete(snapshot.documents.map { it.id })
        batches.forEach { documentIds ->
            val batch = firestore.batch()
            documentIds.forEach { documentId ->
                batch.delete(userDoc.collection(collectionName).document(documentId))
            }
            batch.commit().await()
        }
    }

    private suspend fun saveAnonymizedStats() {
        val shapes = runCatching { shapeRepository.getActiveShapes().first() }.getOrDefault(emptyList())
        val drones = runCatching { droneRepository.getAllDrones().first() }.getOrDefault(emptyList())
        val cloudSyncEnabled = storedCloudBackupEnabled(dataStore.data.first())
        val now = System.currentTimeMillis()
        val accountCreatedAt = firebaseAuth.currentUser
            ?.metadata
            ?.creationTimestamp
            ?.takeIf { it > 0L }
        val anonymousUserRef = firestore.collection("analytics")
            .document("deleted_users")
            .collection("users")
            .document(UUID.randomUUID().toString())

        anonymousUserRef.set(
            buildAnonymizedUserData(
                shapes = shapes,
                drones = drones,
                cloudSyncEnabled = cloudSyncEnabled,
                deletedAtMillis = now,
                accountCreatedAtMillis = accountCreatedAt,
            ),
        ).await()

        shapes
            .map(::shapeToAnonymizedData)
            .chunked(FIRESTORE_BATCH_LIMIT)
            .forEach { batchData ->
                val batch = firestore.batch()
                batchData.forEach { shapeData ->
                    val shapeId = shapeData["id"] as? String ?: return@forEach
                    batch.set(anonymousUserRef.collection("shapes").document(shapeId), shapeData)
                }
                batch.commit().await()
            }

        drones
            .map(::droneToAnonymizedData)
            .chunked(FIRESTORE_BATCH_LIMIT)
            .forEach { batchData ->
                val batch = firestore.batch()
                batchData.forEach { droneData ->
                    val droneId = droneData["id"] as? String ?: return@forEach
                    batch.set(anonymousUserRef.collection("drones").document(droneId), droneData)
                }
                batch.commit().await()
            }
    }

    /** 동기화 결과 — Toast/SnackBar 용 1회성 메시지. */
    sealed class SyncResult {
        data class Success(val shapeCount: Int) : SyncResult()
        data class Failure(val message: String) : SyncResult()
    }
}
