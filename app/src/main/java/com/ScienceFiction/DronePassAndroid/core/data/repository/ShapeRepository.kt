package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.ShapeFirebaseStore
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShapeRepository @Inject constructor(
    private val shapeDao: ShapeDao,
    private val shapeFirebaseStore: ShapeFirebaseStore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val TAG = "ShapeRepository"
    }

    /**
     * 활성(삭제되지 않은) 도형 목록을 Flow로 반환
     * Room DB의 ShapeEntity를 도메인 모델 ShapeModel로 변환
     */
    fun getActiveShapes(): Flow<List<ShapeModel>> {
        return shapeDao.getActiveShapes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 삭제된 도형을 포함한 모든 도형 목록을 Flow로 반환
     */
    fun getAllShapes(): Flow<List<ShapeModel>> {
        return shapeDao.getAllShapes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * ID로 도형 조회
     */
    suspend fun getShapeById(id: String): ShapeModel? {
        return shapeDao.getShapeById(id)?.toDomain()
    }

    /**
     * 새 도형 삽입 (동일 ID 존재 시 교체)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun insertShape(shape: ShapeModel) {
        shapeDao.insertShape(shape.toEntity())
        syncShapeToFirebase(shape)
    }

    /**
     * 도형 정보 업데이트
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun updateShape(shape: ShapeModel) {
        shapeDao.updateShape(shape.toEntity())
        syncShapeToFirebase(shape)
    }

    /**
     * 도형 소프트 삭제 (deletedAt 타임스탬프 설정)
     * 실제로 DB에서 제거하지 않고 삭제 표시만 함.
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun softDeleteShape(shape: ShapeModel) {
        val deletedShape = shape.softDelete()
        shapeDao.updateShape(deletedShape.toEntity())
        syncShapeToFirebase(deletedShape)
    }

    /**
     * 소프트 삭제된 도형 복원 (deletedAt을 null로 설정)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun restoreShape(shape: ShapeModel) {
        val restoredShape = shape.restore()
        shapeDao.updateShape(restoredShape.toEntity())
        syncShapeToFirebase(restoredShape)
    }

    /**
     * 모든 도형 삭제 (하드 삭제, 로컬만)
     * Firestore는 사용자 의도에 따라 별도 처리해야 하므로 자동 푸시하지 않는다.
     */
    suspend fun deleteAllShapes() {
        shapeDao.deleteAllShapes()
    }

    /**
     * 특정 드론에 연결된 활성 도형 수 조회
     */
    suspend fun getActiveShapeCountByDroneId(droneId: String): Int {
        return shapeDao.getActiveShapeCountByDroneId(droneId)
    }

    /**
     * 특정 드론에 연결된 활성 도형들을 다른 드론으로 재할당
     * 로그인 상태이면 Firestore에도 즉시 batch 푸시한다.
     */
    suspend fun reassignShapes(fromDroneId: String, toDroneId: String, toDroneColor: String) {
        val shapes = shapeDao.getActiveShapesByDroneId(fromDroneId)
        val now = System.currentTimeMillis()
        val updatedShapes = shapes.map { shapeEntity ->
            shapeEntity.copy(
                droneId = toDroneId,
                color = toDroneColor,
                updatedAt = now
            )
        }
        updatedShapes.forEach { shapeDao.updateShape(it) }
        syncShapesToFirebase(updatedShapes.map { it.toDomain() })
    }

    /**
     * 특정 드론에 연결된 활성 도형들을 모두 소프트 삭제
     * 로그인 상태이면 Firestore에도 즉시 batch 푸시한다.
     */
    suspend fun softDeleteShapesByDroneId(droneId: String) {
        val shapes = shapeDao.getActiveShapesByDroneId(droneId)
        val now = System.currentTimeMillis()
        val deletedShapes = shapes.map { shapeEntity ->
            shapeEntity.copy(
                deletedAt = now,
                updatedAt = now
            )
        }
        deletedShapes.forEach { shapeDao.updateShape(it) }
        syncShapesToFirebase(deletedShapes.map { it.toDomain() })
    }

    /**
     * 단일 도형을 Firestore에 즉시 푸시한다.
     * 로그인 상태가 아니면 NO-OP. Firestore SDK의 offline persistence가 큐잉/재시도를 담당하므로
     * 호출 자체는 보통 즉시 반환된다. 실패 시 로컬은 유지되며 다음 performFullSync 또는 재로그인 시
     * LWW 머지로 보강된다.
     */
    private suspend fun syncShapeToFirebase(shape: ShapeModel) {
        val userId = auth.currentUser?.uid ?: return
        try {
            shapeFirebaseStore.saveShape(userId, shape)
            shapeFirebaseStore.updateServerMetadata(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase 즉시 푸시 실패: shapeId=${shape.id}", e)
        }
    }

    /**
     * 다수 도형을 Firestore에 batch로 즉시 푸시한다.
     */
    private suspend fun syncShapesToFirebase(shapes: List<ShapeModel>) {
        if (shapes.isEmpty()) return
        val userId = auth.currentUser?.uid ?: return
        try {
            shapeFirebaseStore.saveShapes(userId, shapes)
            shapeFirebaseStore.updateServerMetadata(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase batch 즉시 푸시 실패: count=${shapes.size}", e)
        }
    }

    // ===== Firebase 동기화 메서드 =====

    /**
     * Room 로컬 데이터를 Firebase에 업로드
     * 로그인 상태일 때만 동작
     */
    suspend fun syncToFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncToFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localShapes = shapeDao.getAllShapesOnce().map { it.toDomain() }
            if (localShapes.isNotEmpty()) {
                shapeFirebaseStore.saveShapes(userId, localShapes)
                shapeFirebaseStore.updateServerMetadata(userId)
                Log.d(TAG, "syncToFirebase: ${localShapes.size}개 도형 업로드 완료")
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncToFirebase 실패", e)
            throw e
        }
    }

    /**
     * Firebase 데이터를 Room 로컬 DB로 다운로드
     * LWW(Last Write Wins) 전략 적용
     */
    suspend fun syncFromFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncFromFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val serverShapes = shapeFirebaseStore.loadAllShapesIncludingDeleted(userId)
            serverShapes.forEach { shape ->
                shapeDao.insertShape(shape.toEntity())
            }
            Log.d(TAG, "syncFromFirebase: ${serverShapes.size}개 도형 다운로드 완료")
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
            val localShapes = shapeDao.getAllShapesOnce().map { it.toDomain() }

            // 2. 서버 전체 로드 (삭제된 것 포함)
            val serverShapes = shapeFirebaseStore.loadAllShapesIncludingDeleted(userId)

            // 3. LWW 머지
            val serverById = serverShapes.associateBy { it.id }
            val localById = localShapes.associateBy { it.id }
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
            merged.forEach { shape ->
                shapeDao.insertShape(shape.toEntity())
            }

            // 5. Firebase에 로컬 전용 데이터 업로드
            val localOnly = merged.filter { it.id !in serverById }
            if (localOnly.isNotEmpty()) {
                shapeFirebaseStore.saveShapes(userId, localOnly)
            }

            // 6. 서버 메타데이터 업데이트
            shapeFirebaseStore.updateServerMetadata(userId)

            Log.d(TAG, "performFullSync 완료: 로컬=${localShapes.size}, 서버=${serverShapes.size}, 머지=${merged.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performFullSync 실패", e)
            throw e
        }
    }
}
