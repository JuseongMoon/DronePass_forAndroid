package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.ShapeFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.filterServerNewer
import com.ScienceFiction.DronePassAndroid.core.data.sync.mergeLWW
import com.ScienceFiction.DronePassAndroid.core.data.sync.shouldUpdateServerMetadataAfterFullSync
import com.ScienceFiction.DronePassAndroid.core.data.storedEndDateAlarmEnabled
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence
import com.ScienceFiction.DronePassAndroid.domain.model.validateForLocalPersistence
import com.ScienceFiction.DronePassAndroid.service.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal fun reassignShapeEntityToDrone(
    shape: ShapeEntity,
    toDroneId: String,
    updatedAt: Long,
): ShapeEntity {
    return shape.copy(
        droneId = toDroneId,
        updatedAt = updatedAt,
    )
}

internal fun connectLegacyShapeEntityToDrone(
    shape: ShapeEntity,
    firstDroneId: String,
    updatedAt: Long,
): ShapeEntity {
    return shape.copy(
        droneId = firstDroneId,
        updatedAt = updatedAt,
    )
}

internal fun softDeleteExpiredShapeEntities(
    shapes: List<ShapeEntity>,
    now: Long,
): List<ShapeEntity> {
    return shapes
        .filter { shape ->
            shape.deletedAt == null && shape.flightEndDate?.let { endDate -> endDate < now } == true
        }
        .map { shape ->
            shape.copy(
                deletedAt = now,
                updatedAt = now,
            )
        }
}

@Singleton
class ShapeRepository @Inject constructor(
    private val shapeDao: ShapeDao,
    private val shapeFirebaseStore: ShapeFirebaseStore,
    private val auth: FirebaseAuth,
    private val dataStore: DataStore<Preferences>,
    private val notificationScheduler: NotificationScheduler,
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
        if (!shape.isValidForLocalWrite("insertShape")) return

        shapeDao.insertShape(shape.toEntity())
        markShapeLocalModification()
        updateEndDateAlarmForShape(shape)
        syncShapeToFirebase(shape)
    }

    /**
     * 도형 정보 업데이트
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun updateShape(shape: ShapeModel) {
        if (!shape.isValidForLocalWrite("updateShape")) return

        shapeDao.updateShape(shape.toEntity())
        markShapeLocalModification()
        updateEndDateAlarmForShape(shape)
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
        markShapeLocalModification()
        notificationScheduler.cancelEndDateAlarm(shape.id)
        syncShapeToFirebase(deletedShape)
    }

    /**
     * 소프트 삭제된 도형 복원 (deletedAt을 null로 설정)
     * 로그인 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun restoreShape(shape: ShapeModel) {
        val restoredShape = shape.restore()
        shapeDao.updateShape(restoredShape.toEntity())
        markShapeLocalModification()
        updateEndDateAlarmForShape(restoredShape)
        syncShapeToFirebase(restoredShape)
    }

    /**
     * 모든 도형 삭제 (하드 삭제, 로컬만)
     * Firestore는 사용자 의도에 따라 별도 처리해야 하므로 자동 푸시하지 않는다.
     */
    suspend fun deleteAllShapes() {
        val shapes = shapeDao.getAllShapesOnce()
        shapeDao.deleteAllShapes()
        shapes.forEach { shape -> notificationScheduler.cancelEndDateAlarm(shape.id) }
    }

    /**
     * 만료된 활성 도형들을 모두 소프트 삭제
     * 로그인 상태이면 Firestore에도 즉시 batch 푸시한다.
     */
    suspend fun deleteExpiredShapes(): Int {
        val now = System.currentTimeMillis()
        val deletedShapes = softDeleteExpiredShapeEntities(
            shapes = shapeDao.getActiveExpiredShapes(now),
            now = now,
        )
        if (deletedShapes.isEmpty()) return 0

        shapeDao.insertShapes(deletedShapes)
        markShapeLocalModification(now)
        deletedShapes.forEach { shape -> notificationScheduler.cancelEndDateAlarm(shape.id) }
        syncShapesToFirebase(deletedShapes.map { it.toDomain() })
        return deletedShapes.size
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
    suspend fun reassignShapes(fromDroneId: String, toDroneId: String) {
        val shapes = shapeDao.getActiveShapesByDroneId(fromDroneId)
        val now = System.currentTimeMillis()
        val updatedShapes = shapes.map { shapeEntity ->
            reassignShapeEntityToDrone(shapeEntity, toDroneId, now)
        }
        // REPLACE 충돌 정책으로 update == insert (단일 트랜잭션 1회).
        shapeDao.insertShapes(updatedShapes)
        markShapeLocalModification(now)
        syncShapesToFirebase(updatedShapes.map { it.toDomain() })
    }

    /**
     * iOS DroneManager.migrateLegacyShapes 정합.
     * droneId 가 없는 활성 레거시 도형을 첫 번째 활성 드론에 실제로 연결한다.
     */
    suspend fun migrateLegacyShapesToDrone(firstDroneId: String) {
        val shapes = shapeDao.getActiveLegacyShapesWithoutDrone()
        if (shapes.isEmpty()) return

        val now = System.currentTimeMillis()
        val updatedShapes = shapes.map { shapeEntity ->
            connectLegacyShapeEntityToDrone(shapeEntity, firstDroneId, now)
        }
        shapeDao.insertShapes(updatedShapes)
        markShapeLocalModification(now)
        syncShapesToFirebase(updatedShapes.map { it.toDomain() })
        Log.d(TAG, "레거시 도형 드론 연결 완료: count=${updatedShapes.size}, droneId=$firstDroneId")
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
        shapeDao.insertShapes(deletedShapes)
        markShapeLocalModification(now)
        deletedShapes.forEach { shape -> notificationScheduler.cancelEndDateAlarm(shape.id) }
        syncShapesToFirebase(deletedShapes.map { it.toDomain() })
    }

    private suspend fun markShapeLocalModification(now: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME] = now
        }
    }

    /**
     * 단일 도형을 Firestore에 즉시 푸시한다.
     * 로그인 상태가 아니면 NO-OP. Firestore SDK의 offline persistence가 큐잉/재시도를 담당하므로
     * 호출 자체는 보통 즉시 반환된다. 실패 시 로컬은 유지되며 다음 performFullSync 또는 재로그인 시
     * LWW 머지로 보강된다.
     */
    private suspend fun syncShapeToFirebase(shape: ShapeModel) {
        val userId = auth.currentUser?.uid ?: return
        if (!shape.isValidForFirebaseWrite("syncShapeToFirebase")) return

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
        val validShapes = shapes.filterValidForFirebaseWrite("syncShapesToFirebase")
        if (validShapes.isEmpty()) return

        try {
            shapeFirebaseStore.saveShapes(userId, validShapes)
            shapeFirebaseStore.updateServerMetadata(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase batch 즉시 푸시 실패: count=${validShapes.size}", e)
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
            val localShapes = shapeDao.getAllShapesOnce()
                .map { it.toDomain() }
                .filterValidForFirebaseWrite("syncToFirebase")
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
     * Firebase 데이터를 Room 로컬 DB로 다운로드 (LWW 적용).
     * 서버 값이 로컬 이상으로 새로운 것만 [filterServerNewer] 로 추려 배치 insert.
     */
    suspend fun syncFromFirebase() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "syncFromFirebase: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val serverShapes = shapeFirebaseStore.loadAllShapesIncludingDeleted(userId)
                .getOrThrow()
                .filterValidForFirebaseWrite("syncFromFirebase/server")
            val localShapes = shapeDao.getAllShapesOnce().map { it.toDomain() }
            val toApply = filterServerNewer(
                local = localShapes,
                server = serverShapes,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
            )
            if (toApply.isNotEmpty()) {
                shapeDao.insertShapes(toApply.map { it.toEntity() })
                reconcileEndDateAlarmsWithLocalShapes()
            }
            Log.d(TAG, "syncFromFirebase: 서버=${serverShapes.size}, LWW 통과=${toApply.size}")
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
            val localShapes = shapeDao.getAllShapesOnce()
                .map { it.toDomain() }
                .filterValidForFirebaseWrite("performFullSync/local")
            val serverShapes = shapeFirebaseStore.loadAllShapesIncludingDeleted(userId)
                .getOrThrow()
                .filterValidForFirebaseWrite("performFullSync/server")

            val result = mergeLWW(
                local = localShapes,
                server = serverShapes,
                idOf = { it.id },
                updatedAtOf = { it.updatedAt },
            )

            // Room: 머지 결과 배치 저장
            if (result.merged.isNotEmpty()) {
                shapeDao.insertShapes(result.merged.map { it.toEntity() })
                reconcileEndDateAlarmsWithLocalShapes()
            }

            // Firebase: 로컬이 LWW 에서 이긴 항목 업로드
            val toUpload = result.toUpload.filterValidForFirebaseWrite("performFullSync/upload")
            if (toUpload.isNotEmpty()) {
                shapeFirebaseStore.saveShapes(userId, toUpload)
            }

            if (shouldUpdateServerMetadataAfterFullSync(toUpload.size)) {
                shapeFirebaseStore.updateServerMetadata(userId)
            }

            Log.d(TAG, "performFullSync 완료: 로컬=${localShapes.size}, 서버=${serverShapes.size}, 머지=${result.merged.size}, 업로드=${result.toUpload.size}")
        } catch (e: Exception) {
            Log.e(TAG, "performFullSync 실패", e)
            throw e
        }
    }

    private fun ShapeModel.isValidForLocalWrite(operation: String): Boolean {
        val validation = validateForLocalPersistence()
        if (!validation.isValid) {
            Log.w(TAG, "$operation: 유효하지 않은 도형 로컬 저장 스킵: shapeId=$id, reason=${validation.reason}")
        }
        return validation.isValid
    }

    private suspend fun updateEndDateAlarmForShape(shape: ShapeModel) {
        val preferences = dataStore.data.first()
        val flightEndDate = shape.flightEndDate
        if (!storedEndDateAlarmEnabled(preferences) || shape.isDeleted || flightEndDate == null) {
            notificationScheduler.cancelEndDateAlarm(shape.id)
            return
        }

        notificationScheduler.scheduleEndDateAlarm(
            shapeId = shape.id,
            flightEndDate = flightEndDate,
            shapeTitle = shape.title,
        )
    }

    private suspend fun reconcileEndDateAlarmsWithLocalShapes() {
        val preferences = dataStore.data.first()
        if (!storedEndDateAlarmEnabled(preferences)) return

        shapeDao.getAllShapesOnce()
            .map { it.toDomain() }
            .forEach { shape -> updateEndDateAlarmForShape(shape) }
    }

    private fun ShapeModel.isValidForFirebaseWrite(operation: String): Boolean {
        val validation = validateForFirebasePersistence()
        if (!validation.isValid) {
            Log.w(TAG, "$operation: 유효하지 않은 도형 Firebase 저장 스킵: shapeId=$id, reason=${validation.reason}")
        }
        return validation.isValid
    }

    private fun List<ShapeModel>.filterValidForFirebaseWrite(operation: String): List<ShapeModel> {
        val seenIds = mutableSetOf<String>()
        return filter { shape ->
            when {
                !shape.isValidForFirebaseWrite(operation) -> false
                !seenIds.add(shape.id) -> {
                    Log.w(TAG, "$operation: 중복 도형 ID 스킵: shapeId=${shape.id}")
                    false
                }
                else -> true
            }
        }
    }
}
