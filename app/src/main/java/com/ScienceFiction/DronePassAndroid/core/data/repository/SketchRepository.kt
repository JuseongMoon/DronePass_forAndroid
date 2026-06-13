package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.SketchFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncMergeResult
import com.ScienceFiction.DronePassAndroid.core.data.sync.filterServerNewer
import com.ScienceFiction.DronePassAndroid.core.data.sync.isCloudSyncEnabled
import com.ScienceFiction.DronePassAndroid.core.data.sync.shouldUpdateServerMetadataAfterFullSync
import com.ScienceFiction.DronePassAndroid.core.data.sync.shouldRunImmediateCloudSync
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val SKETCH_REPOSITORY_VALIDATION_TAG = "SketchRepository"

data class SketchCounts(
    val totalCount: Int,
    val activeCount: Int,
)

internal data class SketchEditSyncPlan(
    val existingSketchIdsOnEnter: Set<String>,
    val pendingUpserts: Map<String, SketchModel> = emptyMap(),
    val pendingDeleteIds: Set<String> = emptySet(),
)

internal fun trackSketchUpsertForEditSession(
    plan: SketchEditSyncPlan,
    sketch: SketchModel,
): SketchEditSyncPlan {
    return plan.copy(
        pendingUpserts = plan.pendingUpserts + (sketch.id to sketch),
        pendingDeleteIds = plan.pendingDeleteIds - sketch.id,
    )
}

internal fun trackSketchDeleteForEditSession(
    plan: SketchEditSyncPlan,
    sketchId: String,
): SketchEditSyncPlan {
    return if (sketchId in plan.existingSketchIdsOnEnter) {
        plan.copy(
            pendingUpserts = plan.pendingUpserts - sketchId,
            pendingDeleteIds = plan.pendingDeleteIds + sketchId,
        )
    } else {
        plan.copy(
            pendingUpserts = plan.pendingUpserts - sketchId,
            pendingDeleteIds = plan.pendingDeleteIds - sketchId,
        )
    }
}

internal fun shouldKeepLocalSketchMissingOnServer(
    localSketchUpdatedAt: Long,
    lastSyncTime: Long?,
): Boolean {
    return lastSyncTime == null || localSketchUpdatedAt > lastSyncTime
}

internal fun mergeSketchesForFullSync(
    localSketches: List<SketchModel>,
    serverSketches: List<SketchModel>,
    lastSyncTime: Long?,
): SyncMergeResult<SketchModel> {
    val serverById = serverSketches.associateBy { it.id }
    val localById = localSketches.associateBy { it.id }
    val allIds = serverById.keys + localById.keys

    val merged = allIds.mapNotNull { id ->
        val local = localById[id]
        val server = serverById[id]
        when {
            local != null && server != null -> if (server.updatedAt >= local.updatedAt) server else local
            local != null -> local.takeIf {
                shouldKeepLocalSketchMissingOnServer(
                    localSketchUpdatedAt = it.updatedAt,
                    lastSyncTime = lastSyncTime,
                )
            }
            else -> server
        }
    }

    val toUpload = merged.filter { sketch ->
        val server = serverById[sketch.id]
        server == null || sketch.updatedAt > server.updatedAt
    }

    return SyncMergeResult(merged, toUpload)
}

@Singleton
class SketchRepository @Inject constructor(
    private val sketchDao: SketchDao,
    private val sketchFirebaseStore: SketchFirebaseStore,
    private val auth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * 스케치 변경(insert/update/softDelete/restore) → Firebase push 디바운싱 스코프.
     * 스케치는 Undo/Redo, 지우개 등으로 변경 빈도가 높아 매 호출마다 Firebase 푸시하면
     * 부담이 큼. 동일 sketchId 의 변경을 500ms 윈도우로 묶어 마지막 한 번만 push.
     */
    private val debounceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingSyncs = mutableMapOf<String, PendingSketchSync>()
    private val pendingSyncsMutex = Mutex()
    private var editSyncPlan: SketchEditSyncPlan? = null

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

    suspend fun getSketchCounts(): SketchCounts {
        val sketches = sketchDao.getAllSketchesOnce().map { it.toDomain() }
        return SketchCounts(
            totalCount = sketches.size,
            activeCount = sketches.count { !it.isDeleted },
        )
    }

    /**
     * iOS `SketchRepository.resetModifiedTracking()` 정합.
     *
     * 스케치 모드 중에는 로컬 Room만 즉시 변경하고, 원격 변경은 완료 버튼을 누를 때
     * 한 번에 반영한다. 세션 시작 시 이미 있던 스케치 ID만 원격 삭제 대상으로 삼는다.
     */
    fun beginSketchEditSession(existingSketchIdsOnEnter: Set<String>) {
        editSyncPlan = SketchEditSyncPlan(existingSketchIdsOnEnter = existingSketchIdsOnEnter)
    }

    suspend fun beginSketchEditSession() {
        beginSketchEditSession(
            existingSketchIdsOnEnter = sketchDao.getAllSketchesOnce()
                .filter { it.deletedAt == null }
                .map { it.id }
                .toSet(),
        )
    }

    /**
     * 새 스케치 삽입 (동일 ID 존재 시 교체)
     * 스케치 모드 세션 중이면 원격 푸시는 완료 시점까지 보류한다.
     */
    suspend fun insertSketch(sketch: SketchModel) {
        sketchDao.insertSketch(sketch.toEntity())
        markSketchLocalModification()
        if (!trackSessionUpsertIfNeeded(sketch)) {
            syncSketchToFirebase(sketch)
        }
    }

    /**
     * 스케치 정보 업데이트
     * 스케치 모드 세션 중이면 원격 푸시는 완료 시점까지 보류한다.
     */
    suspend fun updateSketch(sketch: SketchModel) {
        sketchDao.updateSketch(sketch.toEntity())
        markSketchLocalModification()
        if (!trackSessionUpsertIfNeeded(sketch)) {
            syncSketchToFirebase(sketch)
        }
    }

    /**
     * 스케치 소프트 삭제 (deletedAt 타임스탬프 설정)
     * 스케치 모드 세션 중이면 기존 스케치만 완료 시점 원격 삭제 대상으로 기록한다.
     */
    suspend fun softDeleteSketch(sketch: SketchModel) {
        val deletedSketch = sketch.softDelete()
        sketchDao.updateSketch(deletedSketch.toEntity())
        markSketchLocalModification()
        if (!trackSessionDeleteIfNeeded(deletedSketch.id)) {
            syncSketchToFirebase(deletedSketch)
        }
    }

    /**
     * 소프트 삭제된 스케치 복원
     * 스케치 모드 세션 중이면 완료 시점 업로드 대상으로 기록한다.
     */
    suspend fun restoreSketch(sketch: SketchModel) {
        val restoredSketch = sketch.restore()
        sketchDao.updateSketch(restoredSketch.toEntity())
        markSketchLocalModification()
        if (!trackSessionUpsertIfNeeded(restoredSketch)) {
            syncSketchToFirebase(restoredSketch)
        }
    }

    /**
     * 모든 활성 스케치 삭제.
     *
     * iOS와 동일하게 하드 삭제가 아니라 deletedAt을 남기는 soft delete로 처리한다.
     * 그래야 undo/redo가 가능하고 Firebase LWW 동기화에서도 삭제 의도가 보존된다.
     *
     * @return 삭제 처리된 스케치 목록. ViewModel은 이 목록을 Undo 액션으로 저장한다.
     */
    suspend fun deleteAllSketches(): List<SketchModel> {
        val now = System.currentTimeMillis()
        val activeSketches = sketchDao.getAllSketchesOnce()
            .map { it.toDomain() }
            .filter { !it.isDeleted }
        val deletedSketches = softDeleteActiveSketchModels(activeSketches, now)
        if (deletedSketches.isEmpty()) return emptyList()

        sketchDao.insertSketches(deletedSketches.map { it.toEntity() })
        markSketchLocalModification(now)
        if (editSyncPlan != null) {
            cancelPendingSyncs(deletedSketches.map { it.id }.toSet())
            deletedSketches.forEach { trackSessionDeleteIfNeeded(it.id) }
        } else {
            cancelPendingSyncs(deletedSketches.map { it.id }.toSet())
            syncSketchesToFirebase(deletedSketches)
        }
        return deletedSketches
    }

    /**
     * 계정 전환 시 이전 계정의 로컬 스케치가 새 계정으로 업로드되지 않도록
     * Firebase 푸시 없이 로컬 Room 데이터와 대기 중인 디바운스 작업만 비운다.
     */
    suspend fun deleteAllSketchesLocally() {
        val sketchIds = sketchDao.getAllSketchesOnce().map { it.id }.toSet()
        cancelPendingSyncs(sketchIds)
        sketchDao.deleteAllSketches()
    }

    /**
     * 동일 sketchId 의 변경을 [SYNC_DEBOUNCE_MS] 윈도우로 묶어 마지막 한 번만
     * Firestore 에 푸시한다. Undo/Redo·지우개로 변경 빈도가 매우 높은 스케치 영역의
     * 네트워크/IO 부담을 줄인다 (중간 상태를 모두 송신하지 않고 최종 상태만 전송).
     *
     * 로그인 + 클라우드 백업 ON 상태가 아니면 보류 작업도 즉시 NO-OP.
     * 보류 중 동일 id 의 새 변경이 오면 이전 작업을 취소하여 최신 변경만 살아남는다.
     */
    private suspend fun syncSketchToFirebase(sketch: SketchModel) {
        if (!sketch.isValidForFirebaseWrite("syncSketchToFirebase")) return
        if (currentImmediateCloudSyncUserId("syncSketchToFirebase") == null) {
            pendingSyncsMutex.withLock {
                pendingSyncs.remove(sketch.id)?.job?.cancel()
            }
            return
        }

        pendingSyncsMutex.withLock {
            pendingSyncs[sketch.id]?.job?.cancel()
            lateinit var syncJob: Job
            syncJob = debounceScope.launch {
                delay(SYNC_DEBOUNCE_MS)
                val userId = currentImmediateCloudSyncUserId("syncSketchToFirebase") ?: return@launch
                try {
                    sketchFirebaseStore.saveSketch(userId, sketch)
                    sketchFirebaseStore.updateServerMetadata(userId)
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase 디바운스 푸시 실패: sketchId=${sketch.id}", e)
                } finally {
                    pendingSyncsMutex.withLock {
                        if (isCompletedSketchSyncStillPending(pendingSyncs[sketch.id]?.job, syncJob)) {
                            pendingSyncs.remove(sketch.id)
                        }
                    }
                }
            }
            pendingSyncs[sketch.id] = PendingSketchSync(sketch = sketch, job = syncJob)
        }
    }

    private suspend fun syncSketchesToFirebase(sketches: List<SketchModel>) {
        val userId = currentImmediateCloudSyncUserId("syncSketchesToFirebase") ?: return
        val validSketches = sketches.filterValidForFirebaseWrite("syncSketchesToFirebase")
        if (validSketches.isEmpty()) return

        try {
            sketchFirebaseStore.saveSketches(userId, validSketches)
            sketchFirebaseStore.updateServerMetadata(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase 배치 푸시 실패: count=${validSketches.size}", e)
        }
    }

    private suspend fun cancelPendingSyncs(sketchIds: Set<String>) {
        pendingSyncsMutex.withLock {
            sketchIds.forEach { id ->
                pendingSyncs.remove(id)?.job?.cancel()
            }
        }
    }

    private fun trackSessionUpsertIfNeeded(sketch: SketchModel): Boolean {
        val plan = editSyncPlan ?: return false
        editSyncPlan = trackSketchUpsertForEditSession(plan, sketch)
        return true
    }

    private fun trackSessionDeleteIfNeeded(sketchId: String): Boolean {
        val plan = editSyncPlan ?: return false
        editSyncPlan = trackSketchDeleteForEditSession(plan, sketchId)
        return true
    }

    private suspend fun markSketchLocalModification(now: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME] = now
        }
    }

    // ===== Firebase 동기화 메서드 =====

    /**
     * iOS `syncToFirebaseOnComplete()` 정합.
     *
     * 스케치 모드 세션 중 변경은 완료 시점까지 보류한다. 세션 밖에서 발생한 변경은
     * 기존 디바운스 대기분을 즉시 업로드한다.
     */
    suspend fun syncToFirebaseOnComplete() {
        val sessionPlan = editSyncPlan
        if (sessionPlan != null) {
            editSyncPlan = null
            syncSketchEditSessionToFirebase(sessionPlan)
            return
        }

        val pendingSketches = pendingSyncsMutex.withLock {
            val sketches = pendingSyncs.values.map { it.sketch }
            pendingSyncs.values.forEach { it.job.cancel() }
            pendingSyncs.clear()
            sketches
        }
        if (pendingSketches.isEmpty()) return

        syncSketchesToFirebase(pendingSketches)
    }

    private suspend fun syncSketchEditSessionToFirebase(plan: SketchEditSyncPlan) {
        val userId = currentImmediateCloudSyncUserId("syncSketchEditSessionToFirebase") ?: return
        val validUpserts = plan.pendingUpserts.values
            .toList()
            .filterValidForFirebaseWrite("syncSketchEditSessionToFirebase/upsert")
        if (validUpserts.isEmpty() && plan.pendingDeleteIds.isEmpty()) return

        try {
            plan.pendingDeleteIds.forEach { sketchId ->
                sketchFirebaseStore.softDeleteSketch(userId, sketchId)
            }
            if (validUpserts.isNotEmpty()) {
                sketchFirebaseStore.saveSketches(userId, validUpserts)
            }
            sketchFirebaseStore.updateServerMetadata(userId)
        } catch (e: Exception) {
            Log.w(
                TAG,
                "스케치 완료 동기화 실패: upserts=${validUpserts.size}, deletes=${plan.pendingDeleteIds.size}",
                e,
            )
        }
    }

    private suspend fun currentImmediateCloudSyncUserId(operation: String): String? {
        val userId = auth.currentUser?.uid
        val shouldSync = shouldRunImmediateCloudSync(
            isLoggedIn = userId != null,
            cloudSyncEnabled = dataStore.isCloudSyncEnabled(),
        )
        if (!shouldSync) {
            Log.d(TAG, "$operation: 클라우드 백업 비활성화 또는 로그아웃 상태로 즉시 푸시 생략")
        }
        return userId?.takeIf { shouldSync }
    }

    /**
     * Room 로컬 데이터를 Firebase에 업로드
     */
    suspend fun syncToFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncToFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localSketches = sketchDao.getAllSketchesOnce()
                .map { it.toDomain() }
                .filterValidForFirebaseWrite("syncToFirebase")
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
            val serverSketches = sketchFirebaseStore.loadAllSketchesIncludingDeleted(userId)
                .getOrThrow()
                .filterValidForFirebaseWrite("syncFromFirebase/server")
            val localSketches = sketchDao.getAllSketchesOnce().map { it.toDomain() }
            val lastSyncTime = dataStore.data.first()[SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME]
            val serverIds = serverSketches.mapTo(mutableSetOf()) { it.id }
            val staleLocalIds = localSketches
                .filter { it.id !in serverIds }
                .filterNot {
                    shouldKeepLocalSketchMissingOnServer(
                        localSketchUpdatedAt = it.updatedAt,
                        lastSyncTime = lastSyncTime,
                    )
                }
                .map { it.id }
            val toApply = filterServerNewer(
                local = localSketches,
                server = serverSketches,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
            )
            if (staleLocalIds.isNotEmpty()) {
                sketchDao.deleteSketchesByIds(staleLocalIds)
            }
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
     * 양방향 동기화 (LWW 충돌 해결).
     *
     * 서버에 없는 로컬 스케치는 상대 플랫폼/계정 정리/레거시 hard delete 등으로
     * 문서가 실제 삭제된 경우일 수 있다. 마지막 Sketch 동기화 시각 이전 항목이면
     * 원격 삭제로 보고 재업로드하지 않는다.
     */
    suspend fun performFullSync() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "performFullSync: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localSketches = sketchDao.getAllSketchesOnce()
                .map { it.toDomain() }
                .filterValidForFirebaseWrite("performFullSync/local")
            val serverSketches = sketchFirebaseStore.loadAllSketchesIncludingDeleted(userId)
                .getOrThrow()
                .filterValidForFirebaseWrite("performFullSync/server")
            val lastSyncTime = dataStore.data.first()[SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME]

            val result = mergeSketchesForFullSync(
                localSketches = localSketches,
                serverSketches = serverSketches,
                lastSyncTime = lastSyncTime,
            )

            val mergedIds = result.merged.mapTo(mutableSetOf()) { it.id }
            val staleLocalIds = localSketches
                .map { it.id }
                .filter { it !in mergedIds }
            if (staleLocalIds.isNotEmpty()) {
                sketchDao.deleteSketchesByIds(staleLocalIds)
            }
            if (result.merged.isNotEmpty()) {
                sketchDao.insertSketches(result.merged.map { it.toEntity() })
            }

            val toUpload = result.toUpload.filterValidForFirebaseWrite("performFullSync/upload")
            if (toUpload.isNotEmpty()) {
                sketchFirebaseStore.saveSketches(userId, toUpload)
            }

            if (shouldUpdateServerMetadataAfterFullSync(toUpload.size)) {
                sketchFirebaseStore.updateServerMetadata(userId)
            }

            Log.d(TAG, "performFullSync 완료: 로컬=${localSketches.size}, 서버=${serverSketches.size}, 머지=${result.merged.size}, 업로드=${result.toUpload.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performFullSync 실패", e)
            throw e
        }
    }
}

private data class PendingSketchSync(
    val sketch: SketchModel,
    val job: Job,
)

internal fun isCompletedSketchSyncStillPending(
    pendingJob: Job?,
    completedJob: Job,
): Boolean {
    return pendingJob === completedJob
}

internal fun softDeleteActiveSketchModels(
    sketches: List<SketchModel>,
    now: Long,
): List<SketchModel> {
    return sketches
        .filter { !it.isDeleted }
        .map { sketch ->
            sketch.copy(
                deletedAt = now,
                updatedAt = now,
            )
        }
}

private fun SketchModel.isValidForFirebaseWrite(operation: String): Boolean {
    val validation = validateForFirebasePersistence()
    if (!validation.isValid) {
        Log.w(SKETCH_REPOSITORY_VALIDATION_TAG, "$operation: 유효하지 않은 스케치 Firebase 저장 스킵: sketchId=$id, reason=${validation.reason}")
    }
    return validation.isValid
}

private fun List<SketchModel>.filterValidForFirebaseWrite(operation: String): List<SketchModel> {
    val seenIds = mutableSetOf<String>()
    return filter { sketch ->
        when {
            !sketch.isValidForFirebaseWrite(operation) -> false
            !seenIds.add(sketch.id) -> {
                Log.w(SKETCH_REPOSITORY_VALIDATION_TAG, "$operation: 중복 스케치 ID 스킵: sketchId=${sketch.id}")
                false
            }
            else -> true
        }
    }
}
