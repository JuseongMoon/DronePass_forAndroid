package com.ScienceFiction.DronePassAndroid.core.data.sync

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore 실시간 동기화 매니저
 *
 * Firestore의 metadata/server 및 metadata/sketchServer 문서에 SnapshotListener를 등록하여
 * 서버 데이터 변경을 감지하고 로컬 데이터와 동기화합니다.
 *
 * iOS의 RealtimeSyncManager.swift와 동일한 패턴:
 * - 2초 디바운싱
 * - 자신의 변경에 의한 트리거 스킵 (서버 lastModified <= 마지막 동기화 시각)
 * - 최대 3회 재시도 (5초, 10초, 15초 간격)
 * - Shape/Drone용 metadata/server 리스너 + Sketch용 metadata/sketchServer 리스너
 */
@Singleton
class RealtimeSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val shapeRepository: ShapeRepository,
    private val droneRepository: DroneRepository,
    private val sketchRepository: SketchRepository
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

    /** Firestore 스냅샷 리스너 등록 참조 (Shape/Drone용 metadata/server) */
    private var metadataListener: ListenerRegistration? = null

    /** Firestore 스냅샷 리스너 등록 참조 (Sketch용 metadata/sketchServer) */
    private var sketchMetadataListener: ListenerRegistration? = null

    /** 동기화 상태 */
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Shape 데이터 변경 시 호출되는 콜백 */
    var onShapesUpdated: (suspend () -> Unit)? = null

    /** Drone 데이터 변경 시 호출되는 콜백 */
    var onDronesUpdated: (suspend () -> Unit)? = null

    /** Sketch 데이터 변경 시 호출되는 콜백 */
    var onSketchesUpdated: (suspend () -> Unit)? = null

    /** 디바운싱용 코루틴 스코프 및 Job */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debounceJob: Job? = null
    private var sketchDebounceJob: Job? = null

    /** 현재 리스닝 중인 userId (중복 리스너 방지) */
    private var currentListeningUserId: String? = null

    /** 동기화 진행 중 여부 (중복 동기화 방지) */
    private var shapeSyncInProgress = false
    private var sketchSyncInProgress = false

    /** 재시도 횟수 추적 */
    private var shapeRetryCount = 0
    private var sketchRetryCount = 0

    /** 마지막 동기화 시각 (자신의 변경 스킵용) */
    private var lastShapeSyncTime: Long = 0L
    private var lastSketchSyncTime: Long = 0L

    /**
     * Firestore SnapshotListener 시작
     *
     * 두 개의 리스너를 설정합니다:
     * 1. users/{userId}/metadata/server - Shape/Drone 변경 감시
     * 2. users/{userId}/metadata/sketchServer - Sketch 변경 감시
     *
     * @param userId 감시할 사용자 ID
     */
    fun startListening(userId: String) {
        // 중복 리스너 방지: 이미 동일 userId로 리스닝 중이면 무시
        if (currentListeningUserId == userId && metadataListener != null) {
            Log.d(TAG, "이미 userId=$userId 에 대해 리스닝 중입니다.")
            return
        }

        // 기존 리스너가 있으면 먼저 정리
        stopListening()

        currentListeningUserId = userId
        Log.d(TAG, "SnapshotListener 시작: userId=$userId")

        // 1. Shape/Drone 메타데이터 리스너 설정
        setupShapeMetadataListener(userId)

        // 2. Sketch 메타데이터 리스너 설정
        setupSketchMetadataListener(userId)

        Log.d(TAG, "SnapshotListener 설정 완료 (Shape/Drone + Sketch)")
    }

    /**
     * Shape/Drone용 메타데이터 리스너 설정
     * users/{userId}/metadata/server 문서의 lastModified 변경을 감시
     */
    private fun setupShapeMetadataListener(userId: String) {
        val docRef = firestore
            .collection("users")
            .document(userId)
            .collection("metadata")
            .document("server")

        metadataListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Shape 메타데이터 SnapshotListener 오류", error)
                _syncState.value = SyncState.Error(error.message ?: "알 수 없는 오류")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // lastModified는 FieldValue.serverTimestamp()로 기록되므로 Timestamp 타입.
                // 레거시 Long 데이터가 있는 경우를 대비해 getLong 폴백 유지.
                val lastModified = snapshot.getTimestamp("lastModified")?.toDate()?.time
                    ?: snapshot.getLong("lastModified")
                    ?: return@addSnapshotListener
                Log.d(TAG, "Shape/Drone 서버 메타데이터 변경 감지: lastModified=$lastModified")

                // 자신의 변경에 의한 트리거 스킵
                if (lastModified <= lastShapeSyncTime) {
                    Log.d(TAG, "자신의 변경(또는 과거 시각)으로 판단되어 Shape/Drone 동기화를 건너뜁니다.")
                    return@addSnapshotListener
                }

                // 디바운싱: 이전 Job 취소 후 2초 대기
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(DEBOUNCE_DELAY_MS)
                    performShapeAndDroneSync()
                }
            }
        }
    }

    /**
     * Sketch용 메타데이터 리스너 설정
     * users/{userId}/metadata/sketchServer 문서의 lastModified 변경을 감시
     */
    private fun setupSketchMetadataListener(userId: String) {
        val docRef = firestore
            .collection("users")
            .document(userId)
            .collection("metadata")
            .document("sketchServer")

        sketchMetadataListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Sketch 메타데이터 SnapshotListener 오류", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // lastModified는 FieldValue.serverTimestamp()로 기록되므로 Timestamp 타입.
                // 레거시 Long 데이터가 있는 경우를 대비해 getLong 폴백 유지.
                val lastModified = snapshot.getTimestamp("lastModified")?.toDate()?.time
                    ?: snapshot.getLong("lastModified")
                    ?: return@addSnapshotListener
                Log.d(TAG, "Sketch 서버 메타데이터 변경 감지: lastModified=$lastModified")

                // 자신의 변경에 의한 트리거 스킵
                if (lastModified <= lastSketchSyncTime) {
                    Log.d(TAG, "자신의 변경(또는 과거 시각)으로 판단되어 Sketch 동기화를 건너뜁니다.")
                    return@addSnapshotListener
                }

                // 디바운싱: 이전 Job 취소 후 2초 대기
                sketchDebounceJob?.cancel()
                sketchDebounceJob = scope.launch {
                    delay(DEBOUNCE_DELAY_MS)
                    performSketchSync()
                }
            }
        }
    }

    /**
     * Shape 및 Drone 동기화 수행 (디바운싱 후 호출)
     *
     * ShapeRepository.performFullSync()와 DroneRepository.performFullSync()를 호출하여
     * LWW 기반 양방향 동기화를 실행합니다.
     */
    private suspend fun performShapeAndDroneSync() {
        if (shapeSyncInProgress) {
            Log.d(TAG, "Shape/Drone 동기화가 이미 진행 중입니다.")
            return
        }

        shapeSyncInProgress = true
        _syncState.value = SyncState.Syncing

        try {
            Log.d(TAG, "Shape/Drone 실시간 동기화 시작")

            // Shape 동기화
            shapeRepository.performFullSync()
            Log.d(TAG, "Shape 동기화 완료")
            onShapesUpdated?.invoke()

            // Drone 동기화
            droneRepository.performFullSync()
            Log.d(TAG, "Drone 동기화 완료")
            onDronesUpdated?.invoke()

            // 동기화 시각 업데이트
            lastShapeSyncTime = System.currentTimeMillis()

            // 재시도 카운터 초기화
            shapeRetryCount = 0

            _syncState.value = SyncState.Success(lastShapeSyncTime)
            Log.d(TAG, "Shape/Drone 실시간 동기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "Shape/Drone 실시간 동기화 실패", e)
            _syncState.value = SyncState.Error(e.message ?: "Shape/Drone 동기화 중 오류 발생")

            // 재시도 스케줄링
            scheduleShapeRetrySync()
        } finally {
            shapeSyncInProgress = false
        }
    }

    /**
     * Sketch 동기화 수행 (디바운싱 후 호출)
     *
     * SketchRepository.performFullSync()를 호출하여
     * LWW 기반 양방향 동기화를 실행합니다.
     */
    private suspend fun performSketchSync() {
        if (sketchSyncInProgress) {
            Log.d(TAG, "Sketch 동기화가 이미 진행 중입니다.")
            return
        }

        sketchSyncInProgress = true

        try {
            Log.d(TAG, "Sketch 실시간 동기화 시작")

            sketchRepository.performFullSync()
            Log.d(TAG, "Sketch 동기화 완료")
            onSketchesUpdated?.invoke()

            // 동기화 시각 업데이트
            lastSketchSyncTime = System.currentTimeMillis()

            // 재시도 카운터 초기화
            sketchRetryCount = 0

            Log.d(TAG, "Sketch 실시간 동기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "Sketch 실시간 동기화 실패", e)

            // 재시도 스케줄링
            scheduleSketchRetrySync()
        } finally {
            sketchSyncInProgress = false
        }
    }

    /**
     * Shape/Drone 동기화 실패 시 재시도 스케줄링
     * 최대 3회, 5초/10초/15초 간격으로 재시도
     */
    private fun scheduleShapeRetrySync() {
        if (shapeRetryCount < MAX_RETRY_COUNT) {
            shapeRetryCount++
            val retryDelay = shapeRetryCount * BASE_RETRY_DELAY_MS
            Log.d(TAG, "${retryDelay}ms 후 Shape/Drone 동기화 재시도 ($shapeRetryCount/$MAX_RETRY_COUNT)")

            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(retryDelay)
                performShapeAndDroneSync()
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
    private fun scheduleSketchRetrySync() {
        if (sketchRetryCount < MAX_RETRY_COUNT) {
            sketchRetryCount++
            val retryDelay = sketchRetryCount * BASE_RETRY_DELAY_MS
            Log.d(TAG, "${retryDelay}ms 후 Sketch 동기화 재시도 ($sketchRetryCount/$MAX_RETRY_COUNT)")

            sketchDebounceJob?.cancel()
            sketchDebounceJob = scope.launch {
                delay(retryDelay)
                performSketchSync()
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

        // Sketch 리스너 제거
        sketchMetadataListener?.remove()
        sketchMetadataListener = null
        sketchDebounceJob?.cancel()
        sketchDebounceJob = null

        // 상태 초기화
        currentListeningUserId = null
        shapeSyncInProgress = false
        sketchSyncInProgress = false
        shapeRetryCount = 0
        sketchRetryCount = 0
        _syncState.value = SyncState.Idle

        Log.d(TAG, "SnapshotListener 중단 (Shape/Drone + Sketch)")
    }
}
