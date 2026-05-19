package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.SketchFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.sync.filterServerNewer
import com.ScienceFiction.DronePassAndroid.core.data.sync.mergeLWW
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SketchRepository @Inject constructor(
    private val sketchDao: SketchDao,
    private val sketchFirebaseStore: SketchFirebaseStore,
    private val auth: FirebaseAuth
) {

    /**
     * 스케치 변경(insert/update/softDelete/restore) → Firebase push 디바운싱 스코프.
     * 스케치는 Undo/Redo, 지우개 등으로 변경 빈도가 높아 매 호출마다 Firebase 푸시하면
     * 부담이 큼. 동일 sketchId 의 변경을 500ms 윈도우로 묶어 마지막 한 번만 push.
     */
    private val debounceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingSyncs = mutableMapOf<String, Job>()
    private val pendingSyncsMutex = Mutex()

    companion object {
        private const val TAG = "SketchRepository"
        private const val SYNC_DEBOUNCE_MS = 500L
    }

    /**
     * 활성(삭제되지 않은) 스케치 목록을 Flow로 반환
     */
    fun getActiveSketches(): Flow<List<SketchModel>> {
        return sketchDao.getActiveSketches().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 삭제된 스케치를 포함한 모든 스케치 목록을 Flow로 반환
     */
    fun getAllSketches(): Flow<List<SketchModel>> {
        return sketchDao.getAllSketches().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * ID로 스케치 조회
     */
    suspend fun getSketchById(id: String): SketchModel? {
        return sketchDao.getSketchById(id)?.toDomain()
    }

    /**
     * 새 스케치 삽입 (동일 ID 존재 시 교체)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun insertSketch(sketch: SketchModel) {
        sketchDao.insertSketch(sketch.toEntity())
        syncSketchToFirebase(sketch)
    }

    /**
     * 스케치 정보 업데이트
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun updateSketch(sketch: SketchModel) {
        sketchDao.updateSketch(sketch.toEntity())
        syncSketchToFirebase(sketch)
    }

    /**
     * 스케치 소프트 삭제 (deletedAt 타임스탬프 설정)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun softDeleteSketch(sketch: SketchModel) {
        val deletedSketch = sketch.softDelete()
        sketchDao.updateSketch(deletedSketch.toEntity())
        syncSketchToFirebase(deletedSketch)
    }

    /**
     * 소프트 삭제된 스케치 복원
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun restoreSketch(sketch: SketchModel) {
        val restoredSketch = sketch.restore()
        sketchDao.updateSketch(restoredSketch.toEntity())
        syncSketchToFirebase(restoredSketch)
    }

    /**
     * 모든 스케치 삭제 (하드 삭제, 로컬만)
     * Firestore는 사용자 의도에 따라 별도 처리해야 하므로 자동 푸시하지 않는다.
     */
    suspend fun deleteAllSketches() {
        sketchDao.deleteAllSketches()
    }

    /**
     * 동일 sketchId 의 변경을 [SYNC_DEBOUNCE_MS] 윈도우로 묶어 마지막 한 번만
     * Firestore 에 푸시한다. Undo/Redo·지우개로 변경 빈도가 매우 높은 스케치 영역의
     * 네트워크/IO 부담을 줄인다 (중간 상태를 모두 송신하지 않고 최종 상태만 전송).
     *
     * 로그인 상태가 아니면 보류 작업도 즉시 NO-OP. 보류 중 동일 id 의 새 변경이 오면
     * 이전 작업을 취소하여 최신 변경만 살아남는다.
     */
    private suspend fun syncSketchToFirebase(sketch: SketchModel) {
        pendingSyncsMutex.withLock {
            pendingSyncs[sketch.id]?.cancel()
            pendingSyncs[sketch.id] = debounceScope.launch {
                delay(SYNC_DEBOUNCE_MS)
                val userId = auth.currentUser?.uid ?: return@launch
                try {
                    sketchFirebaseStore.saveSketch(userId, sketch)
                    sketchFirebaseStore.updateServerMetadata(userId)
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase 디바운스 푸시 실패: sketchId=${sketch.id}", e)
                } finally {
                    pendingSyncsMutex.withLock { pendingSyncs.remove(sketch.id) }
                }
            }
        }
    }

    // ===== Firebase 동기화 메서드 =====

    /**
     * Room 로컬 데이터를 Firebase에 업로드
     */
    suspend fun syncToFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncToFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localSketches = sketchDao.getAllSketchesOnce().map { it.toDomain() }
            if (localSketches.isNotEmpty()) {
                sketchFirebaseStore.saveSketches(userId, localSketches)
                sketchFirebaseStore.updateServerMetadata(userId)
                Log.d(TAG, "syncToFirebase: ${localSketches.size}개 스케치 업로드 완료")
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncToFirebase 실패", e)
            throw e
        }
    }

    /**
     * Firebase 데이터를 Room 로컬 DB로 다운로드 (LWW 적용).
     */
    suspend fun syncFromFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncFromFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val serverSketches = sketchFirebaseStore.loadAllSketchesIncludingDeleted(userId).getOrThrow()
            val localSketches = sketchDao.getAllSketchesOnce().map { it.toDomain() }
            val toApply = filterServerNewer(
                local = localSketches,
                server = serverSketches,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
            )
            if (toApply.isNotEmpty()) {
                sketchDao.insertSketches(toApply.map { it.toEntity() })
            }
            Log.d(TAG, "syncFromFirebase: 서버=${serverSketches.size}, LWW 통과=${toApply.size}")
        } catch (e: Exception) {
            Log.e(TAG, "syncFromFirebase 실패", e)
            throw e
        }
    }

    /**
     * 양방향 동기화 (LWW 충돌 해결). [mergeLWW] 헬퍼 사용.
     */
    suspend fun performFullSync() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "performFullSync: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localSketches = sketchDao.getAllSketchesOnce().map { it.toDomain() }
            val serverSketches = sketchFirebaseStore.loadAllSketchesIncludingDeleted(userId).getOrThrow()

            val result = mergeLWW(
                local = localSketches,
                server = serverSketches,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
            )

            if (result.merged.isNotEmpty()) {
                sketchDao.insertSketches(result.merged.map { it.toEntity() })
            }

            if (result.toUpload.isNotEmpty()) {
                sketchFirebaseStore.saveSketches(userId, result.toUpload)
            }

            sketchFirebaseStore.updateServerMetadata(userId)

            Log.d(TAG, "performFullSync 완료: 로컬=${localSketches.size}, 서버=${serverSketches.size}, 머지=${result.merged.size}, 업로드=${result.toUpload.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performFullSync 실패", e)
            throw e
        }
    }
}
