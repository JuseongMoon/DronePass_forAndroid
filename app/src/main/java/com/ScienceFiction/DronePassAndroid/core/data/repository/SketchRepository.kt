package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.SketchFirebaseStore
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SketchRepository @Inject constructor(
    private val sketchDao: SketchDao,
    private val sketchFirebaseStore: SketchFirebaseStore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val TAG = "SketchRepository"
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
     */
    suspend fun insertSketch(sketch: SketchModel) {
        sketchDao.insertSketch(sketch.toEntity())
    }

    /**
     * 스케치 정보 업데이트
     */
    suspend fun updateSketch(sketch: SketchModel) {
        sketchDao.updateSketch(sketch.toEntity())
    }

    /**
     * 스케치 소프트 삭제 (deletedAt 타임스탬프 설정)
     */
    suspend fun softDeleteSketch(sketch: SketchModel) {
        val deletedSketch = sketch.softDelete()
        sketchDao.updateSketch(deletedSketch.toEntity())
    }

    /**
     * 소프트 삭제된 스케치 복원
     */
    suspend fun restoreSketch(sketch: SketchModel) {
        val restoredSketch = sketch.restore()
        sketchDao.updateSketch(restoredSketch.toEntity())
    }

    /**
     * 모든 스케치 삭제 (하드 삭제)
     */
    suspend fun deleteAllSketches() {
        sketchDao.deleteAllSketches()
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
     * Firebase 데이터를 Room 로컬 DB로 다운로드
     */
    suspend fun syncFromFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncFromFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val serverSketches = sketchFirebaseStore.loadAllSketchesIncludingDeleted(userId)
            serverSketches.forEach { sketch ->
                sketchDao.insertSketch(sketch.toEntity())
            }
            Log.d(TAG, "syncFromFirebase: ${serverSketches.size}개 스케치 다운로드 완료")
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
            // 1. 로컬 전체 로드
            val localSketches = sketchDao.getAllSketchesOnce().map { it.toDomain() }

            // 2. 서버 전체 로드 (삭제된 것 포함)
            val serverSketches = sketchFirebaseStore.loadAllSketchesIncludingDeleted(userId)

            // 3. LWW 머지
            val serverById = serverSketches.associateBy { it.id }
            val localById = localSketches.associateBy { it.id }
            val allIds = (serverById.keys + localById.keys)

            val merged = allIds.map { id ->
                val local = localById[id]
                val server = serverById[id]
                when {
                    local == null -> server!!
                    server == null -> local
                    server.updatedAt >= local.updatedAt -> server  // LWW: 서버 우선
                    else -> local
                }
            }

            // 4. Room에 머지 결과 저장
            merged.forEach { sketch ->
                sketchDao.insertSketch(sketch.toEntity())
            }

            // 5. Firebase에 로컬 전용 데이터 업로드
            val localOnly = merged.filter { it.id !in serverById }
            if (localOnly.isNotEmpty()) {
                sketchFirebaseStore.saveSketches(userId, localOnly)
            }

            // 6. 서버 메타데이터 업데이트
            sketchFirebaseStore.updateServerMetadata(userId)

            Log.d(TAG, "performFullSync 완료: 로컬=${localSketches.size}, 서버=${serverSketches.size}, 머지=${merged.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performFullSync 실패", e)
            throw e
        }
    }
}
