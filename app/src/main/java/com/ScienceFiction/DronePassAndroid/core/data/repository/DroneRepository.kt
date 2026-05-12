package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.DroneFirebaseStore
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DroneRepository @Inject constructor(
    private val droneDao: DroneDao,
    private val droneFirebaseStore: DroneFirebaseStore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val TAG = "DroneRepository"
    }

    /**
     * 활성(삭제되지 않은) 드론 목록을 Flow로 반환
     */
    fun getActiveDrones(): Flow<List<DroneModel>> {
        return droneDao.getActiveDrones().map { entities ->
            entities.map { it.toDomain() }
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
     * 새 드론 삽입 (동일 ID 존재 시 교체)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun insertDrone(drone: DroneModel) {
        droneDao.insertDrone(drone.toEntity())
        syncDroneToFirebase(drone)
    }

    /**
     * 드론 정보 업데이트
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun updateDrone(drone: DroneModel) {
        droneDao.updateDrone(drone.toEntity())
        syncDroneToFirebase(drone)
    }

    /**
     * 드론 소프트 삭제 (deletedAt 타임스탬프 설정)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun softDeleteDrone(drone: DroneModel) {
        val deletedDrone = drone.softDelete()
        droneDao.updateDrone(deletedDrone.toEntity())
        syncDroneToFirebase(deletedDrone)
    }

    /**
     * 소프트 삭제된 드론 복원
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun restoreDrone(drone: DroneModel) {
        val restoredDrone = drone.restore()
        droneDao.updateDrone(restoredDrone.toEntity())
        syncDroneToFirebase(restoredDrone)
    }

    /**
     * 모든 드론 삭제 (하드 삭제, 로컬만)
     * Firestore는 사용자 의도에 따라 별도 처리해야 하므로 자동 푸시하지 않는다.
     */
    suspend fun deleteAllDrones() {
        droneDao.deleteAllDrones()
    }

    /**
     * 단일 드론을 Firestore에 즉시 푸시한다.
     * 로그인 상태가 아니면 NO-OP. Firestore SDK의 offline persistence가 큐잉/재시도를 담당한다.
     */
    private suspend fun syncDroneToFirebase(drone: DroneModel) {
        val userId = auth.currentUser?.uid ?: return
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
            val localDrones = droneDao.getAllDronesOnce().map { it.toDomain() }
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
     * 이전: forEach 단순 덮어쓰기 → 로컬 우세 데이터가 서버 값으로 덮임.
     */
    suspend fun syncFromFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncFromFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val serverDrones = droneFirebaseStore.loadAllDronesIncludingDeleted(userId).getOrThrow()
            val localDronesById = droneDao.getAllDronesOnce().associateBy { it.id }
            var applied = 0
            serverDrones.forEach { serverDrone ->
                val local = localDronesById[serverDrone.id]?.toDomain()
                if (local == null || serverDrone.updatedAt >= local.updatedAt) {
                    droneDao.insertDrone(serverDrone.toEntity())
                    applied++
                }
            }
            Log.d(TAG, "syncFromFirebase: 서버=${serverDrones.size}, LWW 통과=$applied")
        } catch (e: Exception) {
            Log.e(TAG, "syncFromFirebase 실패", e)
            throw e
        }
    }

    /**
     * 양방향 동기화 (LWW 충돌 해결)
     *
     * 1. 로컬 전체 로드
     * 2. 서버 전체 로드 (삭제된 것 포함)
     * 3. LWW 머지 (updatedAt이 더 큰 쪽 우선)
     * 4. Room에 머지 결과 저장
     * 5. Firebase에 로컬 전용 데이터 업로드
     * 6. 서버 메타데이터 업데이트
     */
    suspend fun performFullSync() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "performFullSync: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localDrones = droneDao.getAllDronesOnce().map { it.toDomain() }
            val serverDrones = droneFirebaseStore.loadAllDronesIncludingDeleted(userId).getOrThrow()

            val serverById = serverDrones.associateBy { it.id }
            val localById = localDrones.associateBy { it.id }
            val allIds = (serverById.keys + localById.keys)

            val merged = allIds.map { id ->
                val local = localById[id]
                val server = serverById[id]
                when {
                    local == null -> server!!
                    server == null -> local
                    server.updatedAt >= local.updatedAt -> server
                    else -> local
                }
            }

            merged.forEach { drone -> droneDao.insertDrone(drone.toEntity()) }

            // 서버에 없는 로컬 + 로컬-우세(LWW 통과) 모두 업로드.
            val toUpload = merged.filter { mergedDrone ->
                val server = serverById[mergedDrone.id]
                server == null || mergedDrone.updatedAt > server.updatedAt
            }
            if (toUpload.isNotEmpty()) {
                droneFirebaseStore.saveDrones(userId, toUpload)
            }

            droneFirebaseStore.updateServerMetadata(userId)

            Log.d(TAG, "performFullSync 완료: 로컬=${localDrones.size}, 서버=${serverDrones.size}, 머지=${merged.size}, 업로드=${toUpload.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performFullSync 실패", e)
            throw e
        }
    }
}
