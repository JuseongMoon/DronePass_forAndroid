package com.ScienceFiction.DronePassAndroid.core.data.sync

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.account.AccountSession
import com.ScienceFiction.DronePassAndroid.core.account.StaleSyncTicketException
import com.ScienceFiction.DronePassAndroid.core.account.SyncTicket
import com.ScienceFiction.DronePassAndroid.core.account.isPermissionDenied
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

internal enum class RealtimeForceSyncDomain {
    ShapeDrone,
    Sketch,
}

internal fun realtimeForceSyncDomains(): List<RealtimeForceSyncDomain> {
    return listOf(
        RealtimeForceSyncDomain.ShapeDrone,
        RealtimeForceSyncDomain.Sketch,
    )
}

internal fun shouldRethrowRealtimeSyncFailure(manualRequest: Boolean): Boolean {
    return manualRequest
}

/**
 * 실패한 동기화를 다시 시도할지. 계정이 바뀌어 티켓이 무효가 됐거나, 쓰기가 PERMISSION_DENIED 로 거부된
 * 계정(다른 기기에서 탈퇴 진행 중·완료)은 다시 시도하지 않는다. 같은 오류를 되풀이하지 않기 위해서다.
 */
internal fun shouldRetryRealtimeSyncFailure(error: Throwable): Boolean {
    return error !is StaleSyncTicketException && !error.isPermissionDenied()
}

internal fun shouldScheduleRealtimeSync(
    serverLastModified: Long,
    lastSyncTime: Long?,
    lastLocalModificationTime: Long?,
): Boolean {
    if (lastLocalModificationTime != null && serverLastModified <= lastLocalModificationTime) {
        return false
    }
    return serverLastModified > (lastSyncTime ?: Long.MIN_VALUE)
}

internal fun shouldScheduleDroneCollectionSync(hasPendingWrites: Boolean): Boolean {
    return !hasPendingWrites
}

internal fun hasRealtimeRemoteChanges(
    serverLastModified: Long?,
    lastSyncTime: Long?,
    lastLocalModificationTime: Long?,
): Boolean {
    return serverLastModified != null &&
        shouldScheduleRealtimeSync(
            serverLastModified = serverLastModified,
            lastSyncTime = lastSyncTime,
            lastLocalModificationTime = lastLocalModificationTime,
        )
}

internal fun hasForegroundShapeMetadataChange(
    serverLastModified: Long?,
    lastSyncTime: Long?,
    lastLocalModificationTime: Long?,
): Boolean {
    return hasRealtimeRemoteChanges(
        serverLastModified = serverLastModified,
        lastSyncTime = lastSyncTime,
        lastLocalModificationTime = lastLocalModificationTime,
    )
}

internal const val RealtimeSyncRestartDelayMs = 500L

/**
 * Firestore 실시간 동기화 매니저
 *
 * Firestore의 metadata/server, drones 컬렉션, metadata/sketchServer 에 SnapshotListener를 등록하여
 * 서버 데이터 변경을 감지하고 로컬 데이터와 동기화합니다.
 *
 * iOS의 RealtimeSyncManager.swift와 동일한 패턴:
 * - 2초 디바운싱
 * - 자신의 변경에 의한 트리거 스킵 (서버 lastModified <= 마지막 동기화 시각)
 * - 최대 3회 재시도 (5초, 10초, 15초 간격)
 * - Shape용 metadata/server 리스너 + iOS DroneFirebaseStore 정합용 drones 컬렉션 리스너
 * - Sketch용 metadata/sketchServer 리스너
 */
@Singleton
class RealtimeSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val session: AccountSession,
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    private val sketchRepository: SketchRepository,
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        private const val TAG = "RealtimeSyncManager"
        /** 디바운싱 간격 (밀리초) */
        private const val DEBOUNCE_DELAY_MS = 2000L
        /** 최대 재시도 횟수 */
        private const val MAX_RETRY_COUNT = 3
        /** 재시도 기본 간격 (밀리초) - n번째 재시도 = n * BASE_RETRY_DELAY_MS */
        private const val BASE_RETRY_DELAY_MS = 5000L
    }

    /** Firestore 스냅샷 리스너 등록 참조 (Shape용 metadata/server) */
    private var metadataListener: ListenerRegistration? = null

    /** iOS DroneFirebaseStore 는 metadata/server 를 갱신하지 않으므로 drones 컬렉션도 직접 감시한다. */
    private var droneCollectionListener: ListenerRegistration? = null

    /** Firestore 스냅샷 리스너 등록 참조 (Sketch용 metadata/sketchServer) */
    private var sketchMetadataListener: ListenerRegistration? = null

    /** 동기화 상태 */
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * 실시간 동기화 리스닝 활성 여부 (iOS `RealtimeSyncManager.isRealtimeSyncListening` 정합).
     * startListening 성공 시 true, stopListening 호출 시 false.
     * ProfileViewModel 이 "Active - Real-time syncing" 상태 라벨 계산에 사용.
     */
    private val _isRealtimeSyncEnabled = MutableStateFlow(false)
    val isRealtimeSyncEnabled: StateFlow<Boolean> = _isRealtimeSyncEnabled.asStateFlow()

    /**
     * 마지막 동기화 시각 (iOS `lastSyncDate` 정합). 성공 시 갱신.
     * ProfileViewModel 이 "Last sync: HH:mm" 표시에 사용.
     */
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    // 데이터 변경 콜백은 Room Flow 가 UI 까지 직접 흐르므로 별도 신호가 불필요.
    // 이전에는 onShapesUpdated/onDronesUpdated/onSketchesUpdated 가 선언만 되어 있고
    // 어디에서도 설정되지 않아 데드 코드였음. 제거하여 향후 디버깅 혼란을 차단한다.

    /** 디바운싱용 코루틴 스코프 및 Job */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debounceJob: Job? = null
    private var sketchDebounceJob: Job? = null

    /** 리스너를 시작한 티켓. 리스너·디바운스·재시도는 모두 이 티켓의 계정으로만 동기화한다. */
    @Volatile
    private var listeningTicket: SyncTicket? = null

    /** 동기화 진행 중 여부 (중복 동기화 방지). 멀티스레드 race 방지를 위해 AtomicBoolean. */
    private val shapeSyncInProgress = AtomicBoolean(false)
    private val sketchSyncInProgress = AtomicBoolean(false)

    /** 재시도 횟수 추적 */
    private var shapeRetryCount = 0
    private var sketchRetryCount = 0

    /** 마지막 동기화 시각 (자신의 변경 스킵용) */
    private var lastShapeSyncTime: Long = 0L
    private var lastSketchSyncTime: Long = 0L

    /**
     * Firestore SnapshotListener 시작
     *
     * 세 개의 리스너를 설정합니다:
     * 1. users/{userId}/metadata/server - Shape 변경 및 Android Drone metadata 감시
     * 2. users/{userId}/drones - iOS DroneFirebaseStore 변경 감시
     * 3. users/{userId}/metadata/sketchServer - Sketch 변경 감시
     *
     * @param ticket 동기화 관문이 준 티켓. 관문이 바뀌면 리스너 콜백과 재시도가 모두 멈춘다.
     */
    fun startListening(ticket: SyncTicket) {
        if (!session.isValid(ticket)) {
            Log.d(TAG, "무효한 티켓이라 리스너를 시작하지 않습니다.")
            return
        }
        val current = listeningTicket
        // 중복 리스너 방지: 이미 같은 관문(같은 계정·세대)으로 리스닝 중이면 무시
        if (
            current != null && current.uid == ticket.uid && session.isValid(current) &&
            metadataListener != null &&
            droneCollectionListener != null &&
            sketchMetadataListener != null
        ) {
            Log.d(TAG, "이미 같은 계정으로 리스닝 중입니다.")
            return
        }

        // 기존 리스너가 있으면 먼저 정리
        stopListening()

        listeningTicket = ticket
        Log.d(TAG, "SnapshotListener 시작")

        // 1. Shape/Drone 메타데이터 리스너 설정
        setupShapeMetadataListener(ticket)

        // 2. iOS 드론 변경 감지를 위한 컬렉션 리스너 설정
        setupDroneCollectionListener(ticket)

        // 3. Sketch 메타데이터 리스너 설정
        setupSketchMetadataListener(ticket)

        _isRealtimeSyncEnabled.value = true
        Log.d(TAG, "SnapshotListener 설정 완료 (Shape metadata + Drone collection + Sketch)")
    }

    /**
     * Shape/Drone용 메타데이터 리스너 설정
     * users/{userId}/metadata/server 문서의 lastModified 변경을 감시
     */
    private fun setupShapeMetadataListener(ticket: SyncTicket) {
        val docRef = firestore
            .collection("users")
            .document(ticket.uid)
            .collection("metadata")
            .document("server")

        metadataListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Shape 메타데이터 SnapshotListener 오류", error)
                if (error.isPermissionDenied()) session.reportPermissionDenied(ticket)
                _syncState.value = SyncState.Error(error.message ?: "알 수 없는 오류")
                return@addSnapshotListener
            }
            if (!session.isValid(ticket)) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                // lastModified는 FieldValue.serverTimestamp()로 기록되므로 Timestamp 타입.
                // 레거시 Long 데이터가 있는 경우를 대비해 getLong 폴백 유지.
                val lastModified = snapshot.getTimestamp("lastModified")?.toDate()?.time
                    ?: snapshot.getLong("lastModified")
                    ?: return@addSnapshotListener
                Log.d(TAG, "Shape/Drone 서버 메타데이터 변경 감지: lastModified=$lastModified")

                scope.launch {
                    val preferences = dataStore.data.first()
                    val shouldSchedule = shouldScheduleRealtimeSync(
                        serverLastModified = lastModified,
                        lastSyncTime = preferences[SyncPreferenceKeys.LAST_SYNC_TIME] ?: lastShapeSyncTime,
                        lastLocalModificationTime = preferences[SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME],
                    )
                    if (!shouldSchedule) {
                        Log.d(TAG, "자신의 변경(또는 이미 반영된 시각)으로 판단되어 Shape/Drone 동기화를 건너뜁니다.")
                        return@launch
                    }

                    scheduleShapeAndDroneSyncDebounced(ticket)
                }
            }
        }
    }

    /**
     * iOS DroneFirebaseStore.save/delete 는 metadata/server 를 갱신하지 않고 drones 컬렉션만 바꾼다.
     * Android metadata 리스너만으로는 iOS 드론-only 변경을 실시간으로 받을 수 없으므로 컬렉션을 직접 감시한다.
     */
    private fun setupDroneCollectionListener(ticket: SyncTicket) {
        val collectionRef = firestore
            .collection("users")
            .document(ticket.uid)
            .collection("drones")

        droneCollectionListener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Drone 컬렉션 SnapshotListener 오류", error)
                if (error.isPermissionDenied()) session.reportPermissionDenied(ticket)
                _syncState.value = SyncState.Error(error.message ?: "알 수 없는 오류")
                return@addSnapshotListener
            }
            if (!session.isValid(ticket)) return@addSnapshotListener

            if (snapshot == null) return@addSnapshotListener
            if (!shouldScheduleDroneCollectionSync(snapshot.metadata.hasPendingWrites())) {
                Log.d(TAG, "로컬 pending write 이므로 Drone 컬렉션 동기화를 건너뜁니다.")
                return@addSnapshotListener
            }

            scheduleShapeAndDroneSyncDebounced(ticket)
        }
    }

    /**
     * Sketch용 메타데이터 리스너 설정
     * users/{userId}/metadata/sketchServer 문서의 lastModified 변경을 감시
     */
    private fun setupSketchMetadataListener(ticket: SyncTicket) {
        val docRef = firestore
            .collection("users")
            .document(ticket.uid)
            .collection("metadata")
            .document("sketchServer")

        sketchMetadataListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Sketch 메타데이터 SnapshotListener 오류", error)
                if (error.isPermissionDenied()) session.reportPermissionDenied(ticket)
                return@addSnapshotListener
            }
            if (!session.isValid(ticket)) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                // lastModified는 FieldValue.serverTimestamp()로 기록되므로 Timestamp 타입.
                // 레거시 Long 데이터가 있는 경우를 대비해 getLong 폴백 유지.
                val lastModified = snapshot.getTimestamp("lastModified")?.toDate()?.time
                    ?: snapshot.getLong("lastModified")
                    ?: return@addSnapshotListener
                Log.d(TAG, "Sketch 서버 메타데이터 변경 감지: lastModified=$lastModified")

                scope.launch {
                    val preferences = dataStore.data.first()
                    val shouldSchedule = shouldScheduleRealtimeSync(
                        serverLastModified = lastModified,
                        lastSyncTime = preferences[SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME] ?: lastSketchSyncTime,
                        lastLocalModificationTime = preferences[SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME],
                    )
                    if (!shouldSchedule) {
                        Log.d(TAG, "자신의 변경(또는 이미 반영된 시각)으로 판단되어 Sketch 동기화를 건너뜁니다.")
                        return@launch
                    }

                    // 디바운싱: 이전 Job 취소 후 2초 대기
                    sketchDebounceJob?.cancel()
                    sketchDebounceJob = scope.launch {
                        delay(DEBOUNCE_DELAY_MS)
                        performSketchSync(ticket)
                    }
                }
            }
        }
    }

    private fun scheduleShapeAndDroneSyncDebounced(ticket: SyncTicket) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_DELAY_MS)
            performShapeAndDroneSync(ticket)
        }
    }

    /**
     * Shape 및 Drone 동기화 수행 (디바운싱 후 호출)
     *
     * ShapeRepository.performFullSync()와 DroneRepository.performFullSync()를 호출하여
     * LWW 기반 양방향 동기화를 실행합니다.
     */
    private suspend fun performShapeAndDroneSync(ticket: SyncTicket, manualRequest: Boolean = false) {
        if (!session.isValid(ticket)) {
            Log.d(TAG, "관문이 바뀌어 Shape/Drone 동기화를 건너뜁니다.")
            return
        }
        // compareAndSet 으로 race 없이 단일 진입 보장
        if (!shapeSyncInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "Shape/Drone 동기화가 이미 진행 중입니다.")
            return
        }
        _syncState.value = SyncState.Syncing

        try {
            Log.d(TAG, "Shape/Drone 실시간 동기화 시작")

            // Shape/Drone 동기화 (Room Flow 가 UI 까지 자동 전파되므로 별도 콜백 불필요)
            // 도형이 드론 id 를 가리키므로 드론을 먼저 올린다.
            droneRepository.performFullSync(ticket)
            Log.d(TAG, "Drone 동기화 완료")

            shapeRepository.performFullSync(ticket)
            Log.d(TAG, "Shape 동기화 완료")

            // 동기화 시각 업데이트
            val syncTime = System.currentTimeMillis()
            session.commit(ticket) {
                lastShapeSyncTime = syncTime
                val syncedShapeBaseline = encodeAccountSwitchShapeBaseline(
                    buildAccountSwitchShapeBaseline(shapeRepository.getAllShapes().first()),
                )
                dataStore.edit { preferences ->
                    preferences.recordShapeRealtimeSyncSuccess(
                        syncTimeMillis = syncTime,
                        syncedShapeBaseline = syncedShapeBaseline,
                    )
                }
            }
            _lastSyncTime.value = lastShapeSyncTime

            // 재시도 카운터 초기화
            shapeRetryCount = 0

            _syncState.value = SyncState.Success(lastShapeSyncTime)
            Log.d(TAG, "Shape/Drone 실시간 동기화 완료")
        } catch (e: Exception) {
            if (e is StaleSyncTicketException) {
                Log.d(TAG, "관문이 바뀌어 Shape/Drone 동기화 결과를 버립니다.")
                _syncState.value = SyncState.Idle
                return
            }
            Log.e(TAG, "Shape/Drone 실시간 동기화 실패", e)
            _syncState.value = SyncState.Error(e.message ?: "Shape/Drone 동기화 중 오류 발생")

            // 재시도 스케줄링
            if (shouldRetryRealtimeSyncFailure(e)) scheduleShapeRetrySync(ticket)
            if (shouldRethrowRealtimeSyncFailure(manualRequest)) {
                throw e
            }
        } finally {
            shapeSyncInProgress.set(false)
        }
    }

    /**
     * Sketch 동기화 수행 (디바운싱 후 호출)
     *
     * SketchRepository.performFullSync()를 호출하여
     * LWW 기반 양방향 동기화를 실행합니다.
     */
    private suspend fun performSketchSync(ticket: SyncTicket, manualRequest: Boolean = false) {
        if (!session.isValid(ticket)) {
            Log.d(TAG, "관문이 바뀌어 Sketch 동기화를 건너뜁니다.")
            return
        }
        if (!sketchSyncInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "Sketch 동기화가 이미 진행 중입니다.")
            return
        }

        try {
            Log.d(TAG, "Sketch 실시간 동기화 시작")

            sketchRepository.performFullSync(ticket)
            Log.d(TAG, "Sketch 동기화 완료")

            val syncTime = System.currentTimeMillis()
            session.commit(ticket) {
                lastSketchSyncTime = syncTime
                dataStore.edit { preferences ->
                    preferences.recordSketchRealtimeSyncSuccess(syncTime)
                }
            }
            sketchRetryCount = 0

            Log.d(TAG, "Sketch 실시간 동기화 완료")
        } catch (e: Exception) {
            if (e is StaleSyncTicketException) {
                Log.d(TAG, "관문이 바뀌어 Sketch 동기화 결과를 버립니다.")
                return
            }
            Log.e(TAG, "Sketch 실시간 동기화 실패", e)
            if (shouldRetryRealtimeSyncFailure(e)) scheduleSketchRetrySync(ticket)
            if (shouldRethrowRealtimeSyncFailure(manualRequest)) {
                throw e
            }
        } finally {
            sketchSyncInProgress.set(false)
        }
    }

    /**
     * Shape/Drone 동기화 실패 시 재시도 스케줄링
     * 최대 3회, 5초/10초/15초 간격으로 재시도
     */
    private fun scheduleShapeRetrySync(ticket: SyncTicket) {
        if (shapeRetryCount < MAX_RETRY_COUNT) {
            shapeRetryCount++
            val retryDelay = shapeRetryCount * BASE_RETRY_DELAY_MS
            Log.d(TAG, "${retryDelay}ms 후 Shape/Drone 동기화 재시도 ($shapeRetryCount/$MAX_RETRY_COUNT)")

            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(retryDelay)
                // 시작할 때의 티켓으로만 재시도한다. 계정이 바뀌었거나 리스너가 멈췄으면 건너뛴다.
                if (!session.isValid(ticket)) {
                    Log.d(TAG, "관문이 바뀌어 Shape/Drone 재시도를 건너뜁니다.")
                    return@launch
                }
                performShapeAndDroneSync(ticket)
            }
        } else {
            Log.e(TAG, "Shape/Drone 최대 재시도 횟수 초과. 실시간 동기화를 일시 중지합니다.")
            shapeRetryCount = 0
        }
    }

    /**
     * Sketch 동기화 실패 시 재시도 스케줄링
     * 최대 3회, 5초/10초/15초 간격으로 재시도
     */
    private fun scheduleSketchRetrySync(ticket: SyncTicket) {
        if (sketchRetryCount < MAX_RETRY_COUNT) {
            sketchRetryCount++
            val retryDelay = sketchRetryCount * BASE_RETRY_DELAY_MS
            Log.d(TAG, "${retryDelay}ms 후 Sketch 동기화 재시도 ($sketchRetryCount/$MAX_RETRY_COUNT)")

            sketchDebounceJob?.cancel()
            sketchDebounceJob = scope.launch {
                delay(retryDelay)
                if (!session.isValid(ticket)) {
                    Log.d(TAG, "관문이 바뀌어 Sketch 재시도를 건너뜁니다.")
                    return@launch
                }
                performSketchSync(ticket)
            }
        } else {
            Log.e(TAG, "Sketch 최대 재시도 횟수 초과. 실시간 동기화를 일시 중지합니다.")
            sketchRetryCount = 0
        }
    }

    /**
     * SnapshotListener 정리 및 리스닝 중단
     */
    fun stopListening() {
        // Shape/Drone 리스너 제거
        metadataListener?.remove()
        metadataListener = null
        debounceJob?.cancel()
        debounceJob = null

        droneCollectionListener?.remove()
        droneCollectionListener = null

        // Sketch 리스너 제거
        sketchMetadataListener?.remove()
        sketchMetadataListener = null
        sketchDebounceJob?.cancel()
        sketchDebounceJob = null

        // 상태 초기화
        listeningTicket = null
        shapeSyncInProgress.set(false)
        sketchSyncInProgress.set(false)
        shapeRetryCount = 0
        sketchRetryCount = 0
        _syncState.value = SyncState.Idle
        _isRealtimeSyncEnabled.value = false

        Log.d(TAG, "SnapshotListener 중단 (Shape/Drone + Sketch)")
    }

    /**
     * 리스너를 멈추고, 진행 중인 디바운스·재시도·동기화 작업이 끝날 때까지 기다린다(로그아웃 ④).
     * 관문을 먼저 닫은 뒤 부르므로 기다리는 동안 끝나는 작업도 기기 저장소에는 반영하지 못한다.
     */
    suspend fun stopListeningAndWait() {
        val jobs = listOfNotNull(debounceJob, sketchDebounceJob)
        stopListening()
        jobs.forEach { it.cancelAndJoin() }
    }

    fun resetSyncTrackingForAccountSwitch() {
        lastShapeSyncTime = 0L
        lastSketchSyncTime = 0L
        _lastSyncTime.value = null
    }

    /**
     * iOS `RealtimeSyncManager.forceSyncNow()` 정합 — 디바운싱 우회 즉시 동기화.
     * ProfileViewModel 의 수동 백업 / 토글 ON 시 동기화 chain 의 진입점.
     * Shape/Drone 과 Sketch 는 서로 다른 metadata 문서를 쓰지만 수동 백업은 전체 데이터를 대상으로 한다.
     * 외부 호출은 rate-limit (UI 디바운스) 으로 보호할 것 — Repository 자체는 가드 안 함.
     */
    suspend fun forceSyncNow() {
        val ticket = session.syncTicket() ?: throw StaleSyncTicketException()
        realtimeForceSyncDomains().forEach { domain ->
            when (domain) {
                RealtimeForceSyncDomain.ShapeDrone -> performShapeAndDroneSync(ticket, manualRequest = true)
                RealtimeForceSyncDomain.Sketch -> performSketchSync(ticket, manualRequest = true)
            }
        }
    }

    /**
     * iOS `resetAndRestartRealtimeSync()` 정합 — 동기화 관문의 현재 티켓으로
     * stopListening → 500ms 후 startListening 재시작. 관문이 닫혀 있으면 아무것도 하지 않는다.
     */
    suspend fun resetAndRestartRealtimeSync() {
        session.syncTicket() ?: return
        stopListening()
        delay(RealtimeSyncRestartDelayMs)
        startListening(session.syncTicket() ?: return)
    }

    suspend fun hasForegroundShapeRemoteChanges(): Boolean {
        val ticket = session.syncTicket() ?: return false
        val preferences = dataStore.data.first()
        val shapeServerLastModified = fetchMetadataLastModified(
            ticket = ticket,
            documentId = "server",
        )
        return hasForegroundShapeMetadataChange(
            serverLastModified = shapeServerLastModified,
            lastSyncTime = preferences[SyncPreferenceKeys.LAST_SYNC_TIME],
            lastLocalModificationTime = preferences[SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME],
        )
    }

    private suspend fun fetchMetadataLastModified(ticket: SyncTicket, documentId: String): Long? {
        val snapshot = session.remote(ticket) { uid ->
            firestore
                .collection("users")
                .document(uid)
                .collection("metadata")
                .document(documentId)
                .get()
                .await()
        }
        if (!snapshot.exists()) return null
        return snapshot.getTimestamp("lastModified")?.toDate()?.time
            ?: snapshot.getLong("lastModified")
    }
}
