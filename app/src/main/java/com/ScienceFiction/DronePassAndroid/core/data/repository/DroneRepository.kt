package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.DroneFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncMergeResult
import com.ScienceFiction.DronePassAndroid.core.data.sync.filterServerNewer
import com.ScienceFiction.DronePassAndroid.core.data.sync.shouldUpdateServerMetadataAfterFullSync
import com.ScienceFiction.DronePassAndroid.core.util.compareIosLocalizedStandardStrings
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal fun shouldKeepLocalDroneMissingOnServer(
    localDroneUpdatedAt: Long,
    lastSyncTime: Long?,
): Boolean {
    return lastSyncTime == null || localDroneUpdatedAt > lastSyncTime
}

internal fun mergeDronesForFullSync(
    localDrones: List<DroneModel>,
    serverDrones: List<DroneModel>,
    lastSyncTime: Long?,
): SyncMergeResult<DroneModel> {
    val serverById = serverDrones.associateBy { it.id }
    val localById = localDrones.associateBy { it.id }
    val allIds = serverById.keys + localById.keys

    val merged = allIds.mapNotNull { id ->
        val local = localById[id]
        val server = serverById[id]
        when {
            local == null -> server!!
            server == null -> local.takeIf {
                shouldKeepLocalDroneMissingOnServer(
                    localDroneUpdatedAt = it.updatedAt,
                    lastSyncTime = lastSyncTime,
                )
            }
            server.updatedAt >= local.updatedAt -> server
            else -> local
        }
    }

    val toUpload = merged.filter { drone ->
        val server = serverById[drone.id]
        server == null || drone.updatedAt > server.updatedAt
    }

    return SyncMergeResult(merged, toUpload)
}

@Singleton
class DroneRepository @Inject constructor(
    private val droneDao: DroneDao,
    private val droneFirebaseStore: DroneFirebaseStore,
    private val auth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "DroneRepository"
    }

    /**
     * 활성(삭제되지 않은) 드론 목록을 Flow로 반환
     */
    fun getActiveDrones(): Flow<List<DroneModel>> {
        val locale = Locale.getDefault()
        return droneDao.getActiveDrones().map { entities ->
            entities.map { it.toDomain() }
                .sortedWith { a, b -> compareIosLocalizedStandardStrings(a.name, b.name, locale) }
        }
    }

    /**
     * 삭제된 드론을 포함한 모든 드론 목록을 Flow로 반환
     */
    fun getAllDrones(): Flow<List<DroneModel>> {
        return droneDao.getAllDrones().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * ID로 드론 조회
     */
    suspend fun getDroneById(id: String): DroneModel? {
        return droneDao.getDroneById(id)?.toDomain()
    }

    /**
     * iOS DroneManager.setupInitialDroneIfNeeded 정합.
     * 활성 드론이 하나도 없으면 기본 드론을 생성하고 즉시 저장한다.
     */
    suspend fun ensureDefaultDroneIfNeeded(): DroneModel? {
        if (droneDao.getActiveDroneCount() > 0) return null

        val defaultDrone = DroneModel.createDefault(
            defaultName = context.getString(R.string.drone_edit_default_name_first),
        )
        droneDao.insertDrone(defaultDrone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(defaultDrone)
        Log.d(TAG, "기본 드론 자동 생성: droneId=${defaultDrone.id}")
        return defaultDrone
    }

    /**
     * 새 드론 삽입 (동일 ID 존재 시 교체)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun insertDrone(drone: DroneModel) {
        droneDao.insertDrone(drone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(drone)
    }

    /**
     * 드론 정보 업데이트
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun updateDrone(drone: DroneModel) {
        droneDao.updateDrone(drone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(drone)
    }

    /**
     * 드론 소프트 삭제 (deletedAt 타임스탬프 설정)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun softDeleteDrone(drone: DroneModel) {
        val deletedDrone = drone.softDelete()
        droneDao.updateDrone(deletedDrone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(deletedDrone)
    }

    /**
     * 소프트 삭제된 드론 복원
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun restoreDrone(drone: DroneModel) {
        val restoredDrone = drone.restore()
        droneDao.updateDrone(restoredDrone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(restoredDrone)
    }

    /**
     * 모든 드론 삭제 (하드 삭제, 로컬만)
     * Firestore는 사용자 의도에 따라 별도 처리해야 하므로 자동 푸시하지 않는다.
     */
    suspend fun deleteAllDrones() {
        droneDao.deleteAllDrones()
    }

    private suspend fun markDroneLocalModification(now: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME] = now
        }
    }

    /**
     * 단일 드론을 Firestore에 즉시 푸시한다.
     * 로그인 상태가 아니면 NO-OP. Firestore SDK의 offline persistence가 큐잉/재시도를 담당한다.
     */
    private suspend fun syncDroneToFirebase(drone: DroneModel) {
        val userId = auth.currentUser?.uid ?: return
        if (!drone.isValidForFirebaseWrite("syncDroneToFirebase")) return
        try {
            droneFirebaseStore.saveDrone(userId, drone)
            droneFirebaseStore.updateServerMetadata(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase 즉시 푸시 실패: droneId=${drone.id}", e)
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
            val localDrones = droneDao.getAllDronesOnce()
                .map { it.toDomain() }
                .filterValidForFirebaseWrite("syncToFirebase")
            if (localDrones.isNotEmpty()) {
                droneFirebaseStore.saveDrones(userId, localDrones)
                droneFirebaseStore.updateServerMetadata(userId)
                Log.d(TAG, "syncToFirebase: ${localDrones.size}개 드론 업로드 완료")
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
            val serverDrones = droneFirebaseStore.loadAllDronesIncludingDeleted(userId).getOrThrow()
            val localDrones = droneDao.getAllDronesOnce().map { it.toDomain() }
            val lastSyncTime = dataStore.data.first()[SyncPreferenceKeys.LAST_SYNC_TIME]
            val serverIds = serverDrones.mapTo(mutableSetOf()) { it.id }
            val staleLocalIds = localDrones
                .filter { it.id !in serverIds }
                .filterNot {
                    shouldKeepLocalDroneMissingOnServer(
                        localDroneUpdatedAt = it.updatedAt,
                        lastSyncTime = lastSyncTime,
                    )
                }
                .map { it.id }
            val toApply = filterServerNewer(
                local = localDrones,
                server = serverDrones,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
            )
            if (staleLocalIds.isNotEmpty()) {
                droneDao.deleteDronesByIds(staleLocalIds)
            }
            if (toApply.isNotEmpty()) {
                droneDao.insertDrones(toApply.map { it.toEntity() })
            }
            Log.d(TAG, "syncFromFirebase: 서버=${serverDrones.size}, LWW 통과=${toApply.size}")
        } catch (e: Exception) {
            Log.e(TAG, "syncFromFirebase 실패", e)
            throw e
        }
    }

    /**
     * 양방향 동기화 (LWW 충돌 해결).
     *
     * iOS DroneFirebaseStore.delete 는 문서를 hard delete 하므로,
     * 마지막 동기화 시각 이전 로컬 항목이 서버에서 사라졌다면 iOS 삭제로 보고 재업로드하지 않는다.
     */
    suspend fun performFullSync() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "performFullSync: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localDrones = droneDao.getAllDronesOnce().map { it.toDomain() }
            val serverDrones = droneFirebaseStore.loadAllDronesIncludingDeleted(userId).getOrThrow()
            val lastSyncTime = dataStore.data.first()[SyncPreferenceKeys.LAST_SYNC_TIME]

            val result = mergeDronesForFullSync(
                localDrones = localDrones,
                serverDrones = serverDrones,
                lastSyncTime = lastSyncTime,
            )

            val mergedIds = result.merged.mapTo(mutableSetOf()) { it.id }
            val staleLocalIds = localDrones
                .map { it.id }
                .filter { it !in mergedIds }
            if (staleLocalIds.isNotEmpty()) {
                droneDao.deleteDronesByIds(staleLocalIds)
            }
            if (result.merged.isNotEmpty()) droneDao.insertDrones(result.merged.map { it.toEntity() })

            val toUpload = result.toUpload.filterValidForFirebaseWrite("performFullSync/upload")
            if (toUpload.isNotEmpty()) {
                droneFirebaseStore.saveDrones(userId, toUpload)
            }

            if (shouldUpdateServerMetadataAfterFullSync(toUpload.size)) {
                droneFirebaseStore.updateServerMetadata(userId)
            }

            Log.d(TAG, "performFullSync 완료: 로컬=${localDrones.size}, 서버=${serverDrones.size}, 머지=${result.merged.size}, 업로드=${toUpload.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performFullSync 실패", e)
            throw e
        }
    }

    private fun DroneModel.isValidForFirebaseWrite(operation: String): Boolean {
        val validation = validateForFirebasePersistence()
        if (!validation.isValid) {
            Log.w(TAG, "$operation: 유효하지 않은 드론 Firebase 저장 스킵: droneId=$id, reason=${validation.reason}")
        }
        return validation.isValid
    }

    private fun List<DroneModel>.filterValidForFirebaseWrite(operation: String): List<DroneModel> {
        val seenIds = mutableSetOf<String>()
        return filter { drone ->
            when {
                !drone.isValidForFirebaseWrite(operation) -> false
                !seenIds.add(drone.id) -> {
                    Log.w(TAG, "$operation: 중복 드론 ID 스킵: droneId=${drone.id}")
                    false
                }
                else -> true
            }
        }
    }
}
