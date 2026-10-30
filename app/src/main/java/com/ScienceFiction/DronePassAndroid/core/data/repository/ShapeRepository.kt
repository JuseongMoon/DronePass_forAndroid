package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.sync.idsChangedLocallyDuringSync
import com.ScienceFiction.DronePassAndroid.core.data.local.room.DronePassDatabase
import androidx.room.withTransaction
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.account.AccountSession
import com.ScienceFiction.DronePassAndroid.core.account.SyncTicket
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.ShapeFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncMergeResult
import com.ScienceFiction.DronePassAndroid.core.data.sync.shouldUpdateServerMetadataAfterFullSync
import com.ScienceFiction.DronePassAndroid.core.data.sync.isNewerUpdatedAt
import com.ScienceFiction.DronePassAndroid.core.data.storedEndDateAlarmEnabled
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence
import com.ScienceFiction.DronePassAndroid.domain.model.validateForLocalPersistence
import com.ScienceFiction.DronePassAndroid.service.NotificationScheduler
import com.ScienceFiction.DronePassAndroid.service.buildEndDateAlarmReconcilePlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 도형 색은 연결된 드론 색을 따른다(계약 "삭제·충돌"). 드론 색을 모르면 도형 색을 그대로 둔다. */
internal fun reassignShapeEntityToDrone(
    shape: ShapeEntity,
    toDroneId: String,
    toDroneColor: String?,
    updatedAt: Long,
): ShapeEntity {
    return shape.copy(
        droneId = toDroneId,
        color = toDroneColor ?: shape.color,
        updatedAt = updatedAt,
    )
}

internal fun connectLegacyShapeEntityToDrone(
    shape: ShapeEntity,
    firstDroneId: String,
    firstDroneColor: String?,
    updatedAt: Long,
): ShapeEntity {
    return shape.copy(
        droneId = firstDroneId,
        color = firstDroneColor ?: shape.color,
        updatedAt = updatedAt,
    )
}

/**
 * 도형 색 = 연결된 드론 색(대표 결정 2026-10-12, iOS `ShapeColorRepair.enforceDroneColors`).
 * 활성 도형 중 살아 있는 드론을 가리키면서 색이 다른 것(대소문자 무시)만 드론 색으로 바꾸고 updatedAt 을 [nowMillis] 로 올려 돌려준다.
 * 드론이 없거나 삭제된 도형은 자기 색을 쓴다.
 */
internal fun shapesRecoloredToDrones(
    shapes: List<ShapeModel>,
    drones: List<DroneModel>,
    nowMillis: Long,
): List<ShapeModel> {
    val droneColors = drones.filter { it.deletedAt == null }.associate { it.id to it.color }
    return shapes.mapNotNull { shape ->
        val droneColor = shape.droneId?.let(droneColors::get) ?: return@mapNotNull null
        if (shape.deletedAt != null || shape.color.equals(droneColor, ignoreCase = true)) return@mapNotNull null
        shape.copy(color = droneColor, updatedAt = nowMillis)
    }
}

internal fun softDeleteExpiredShapeEntities(
    shapes: List<ShapeEntity>,
    now: Long,
): List<ShapeEntity> {
    return shapes
        .filter { shape ->
            shape.deletedAt == null && shape.flightEndDate?.let { endDate -> endDate <= now } == true
        }
        .map { shape ->
            shape.copy(
                deletedAt = now,
                updatedAt = now,
            )
        }
}

internal fun shouldKeepLocalShapeMissingOnServer(
    localShapeUpdatedAt: Long,
    lastSyncTime: Long?,
): Boolean {
    return lastSyncTime == null || localShapeUpdatedAt > lastSyncTime
}

internal fun mergeShapesForFullSync(
    localShapes: List<ShapeModel>,
    serverShapes: List<ShapeModel>,
    lastSyncTime: Long?,
): SyncMergeResult<ShapeModel> {
    val serverById = serverShapes.associateBy { it.id }
    val localById = localShapes.associateBy { it.id }
    val allIds = serverById.keys + localById.keys

    val merged = allIds.mapNotNull { id ->
        val local = localById[id]
        val server = serverById[id]
        when {
            local != null && server != null -> if (isNewerUpdatedAt(local.updatedAt, than = server.updatedAt)) local else server
            local != null -> local.takeIf {
                shouldKeepLocalShapeMissingOnServer(
                    localShapeUpdatedAt = it.updatedAt,
                    lastSyncTime = lastSyncTime,
                )
            }
            else -> server
        }
    }

    val toUpload = merged.filter { shape ->
        val server = serverById[shape.id]
        server == null || isNewerUpdatedAt(shape.updatedAt, than = server.updatedAt)
    }

    return SyncMergeResult(merged, toUpload)
}

@Singleton
class ShapeRepository @Inject constructor(
    private val shapeDao: ShapeDao,
    private val shapeFirebaseStore: ShapeFirebaseStore,
    private val session: AccountSession,
    private val dataStore: DataStore<Preferences>,
    private val notificationScheduler: NotificationScheduler,
    private val database: DronePassDatabase,
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
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
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
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
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
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
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
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
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
        notificationScheduler.cancelKnownEndDateAlarms(shapes.map { it.id })
    }

    /**
     * 만료된 활성 도형들을 모두 소프트 삭제
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 batch 푸시한다.
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
     * 특정 드론에 연결된 활성 도형들을 다른 드론으로 재할당하고 색도 그 드론 색으로 바꾼다.
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 batch 푸시한다.
     */
    suspend fun reassignShapes(fromDroneId: String, toDrone: DroneModel) {
        val shapes = shapeDao.getActiveShapesByDroneId(fromDroneId)
        val now = System.currentTimeMillis()
        val updatedShapes = shapes.map { shapeEntity ->
            reassignShapeEntityToDrone(shapeEntity, toDrone.id, toDrone.color, now)
        }
        // REPLACE 충돌 정책으로 update == insert (단일 트랜잭션 1회).
        shapeDao.insertShapes(updatedShapes)
        markShapeLocalModification(now)
        syncShapesToFirebase(updatedShapes.map { it.toDomain() })
    }

    /**
     * iOS DroneManager.migrateLegacyShapes 정합.
     * droneId 가 없는 활성 레거시 도형을 첫 번째 활성 드론에 실제로 연결하고 색도 그 드론 색으로 바꾼다.
     */
    suspend fun migrateLegacyShapesToDrone(firstDrone: DroneModel) {
        val shapes = shapeDao.getActiveLegacyShapesWithoutDrone()
        if (shapes.isEmpty()) return

        val now = System.currentTimeMillis()
        val updatedShapes = shapes.map { shapeEntity ->
            connectLegacyShapeEntityToDrone(shapeEntity, firstDrone.id, firstDrone.color, now)
        }
        shapeDao.insertShapes(updatedShapes)
        markShapeLocalModification(now)
        syncShapesToFirebase(updatedShapes.map { it.toDomain() })
        Log.d(TAG, "레거시 도형 드론 연결 완료: count=${updatedShapes.size}, droneId=${firstDrone.id}")
    }

    /**
     * 도형 색을 연결된 드론 색으로 맞춘다([shapesRecoloredToDrones]). 바뀐 도형은 updatedAt 이 올라가고,
     * 관문이 열려 있으면 바로 올린다. 닫혀 있으면 기기에만 반영되고 다음 업로드·동기화에서 올라간다.
     * 드론 색을 바꿨을 때, 관문을 연 뒤 첫 동기화, 로그아웃 전 업로드 전에 부른다. 바꾼 도형 수를 돌려준다.
     */
    suspend fun syncShapeColorsWithDrones(drones: List<DroneModel>): Int {
        val now = System.currentTimeMillis()
        val recolored = recolorShapesToDronesLocally(drones, now)
        if (recolored.isEmpty()) return 0
        markShapeLocalModification(now)
        syncShapesToFirebase(recolored)
        Log.d(TAG, "도형 색을 드론 색으로 맞춤: count=${recolored.size}")
        return recolored.size
    }

    /** 기기 도형을 드론 색으로 맞춰 저장하고 바뀐 도형을 돌려준다. 읽기와 쓰기를 한 트랜잭션으로 묶는다. */
    private suspend fun recolorShapesToDronesLocally(drones: List<DroneModel>, now: Long): List<ShapeModel> =
        database.withTransaction {
            shapesRecoloredToDrones(shapeDao.getAllShapesOnce().map { it.toDomain() }, drones, now)
                .also { changed -> if (changed.isNotEmpty()) shapeDao.insertShapes(changed.map { it.toEntity() }) }
        }

    /**
     * 특정 드론에 연결된 활성 도형들을 모두 소프트 삭제
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 batch 푸시한다.
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

    /**
     * 태그를 지운 뒤: 그 태그를 단 활성 도형의 태그를 비우고 updatedAt 을 올려 저장한다(도형은 지우지 않는다).
     * 관문이 열려 있으면 바로 올린다. 바꾼 도형 수를 돌려준다. iOS TagManager.deleteTag 정합.
     */
    suspend fun clearTagFromShapes(tagId: String): Int {
        val now = System.currentTimeMillis()
        val cleared = database.withTransaction {
            shapeDao.getActiveShapesByTagId(tagId)
                .map { it.copy(tagId = null, updatedAt = now) }
                .also { if (it.isNotEmpty()) shapeDao.insertShapes(it) }
        }
        if (cleared.isEmpty()) return 0
        markShapeLocalModification(now)
        syncShapesToFirebase(cleared.map { it.toDomain() })
        return cleared.size
    }

    private suspend fun markShapeLocalModification(now: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME] = now
        }
    }

    /**
     * 단일 도형을 Firestore에 즉시 푸시한다.
     * 동기화 관문이 닫혀 있으면(비로그인·가져오기 확인 대기 등) NO-OP. 실패 시 로컬은 유지되며
     * 다음 performFullSync 에서 LWW 머지로 보강된다.
     */
    private suspend fun syncShapeToFirebase(shape: ShapeModel) {
        val ticket = immediateSyncTicket("syncShapeToFirebase") ?: return
        if (!shape.isValidForFirebaseWrite("syncShapeToFirebase")) return

        try {
            shapeFirebaseStore.saveShape(ticket, shape)
            shapeFirebaseStore.updateServerMetadata(ticket)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase 즉시 푸시 실패: shapeId=${shape.id}", e)
        }
    }

    /**
     * 다수 도형을 Firestore에 batch로 즉시 푸시한다.
     */
    private suspend fun syncShapesToFirebase(shapes: List<ShapeModel>) {
        if (shapes.isEmpty()) return
        val ticket = immediateSyncTicket("syncShapesToFirebase") ?: return
        val validShapes = shapes.filterValidForFirebaseWrite("syncShapesToFirebase")
        if (validShapes.isEmpty()) return

        try {
            shapeFirebaseStore.saveShapes(ticket, validShapes)
            shapeFirebaseStore.updateServerMetadata(ticket)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase batch 즉시 푸시 실패: count=${validShapes.size}", e)
        }
    }

    private fun immediateSyncTicket(operation: String): SyncTicket? {
        val ticket = session.syncTicket()
        if (ticket == null) {
            Log.d(TAG, "$operation: 동기화 관문이 닫혀 있어 즉시 푸시 생략")
        }
        return ticket
    }

    // ===== Firebase 동기화 메서드 =====

    /**
     * 양방향 동기화 (LWW 충돌 해결).
     *
     * 서버에 없는 로컬 도형은 상대 플랫폼/계정 정리/레거시 hard delete 등으로
     * 문서가 실제 삭제된 경우일 수 있다. 마지막 Shape/Drone 동기화 시각 이전
     * 항목이면 원격 삭제로 보고 재업로드하지 않는다.
     *
     * [ticket] 은 작업을 시작할 때 한 번 받은 것을 끝까지 쓴다. 서버를 읽은 뒤 기기 저장소에 반영하기 직전에
     * 티켓을 다시 확인하므로, 그 사이 계정이 바뀌었으면 아무것도 반영하지 않는다([StaleSyncTicketException]).
     */
    /** 서버에 올린 도형 수(병합 업로드 + 색 맞춤 업로드)를 돌려준다(수동 백업 결과 표시용). */
    suspend fun performFullSync(ticket: SyncTicket): Int {
        try {
            val localEntities = shapeDao.getAllShapesOnce()
            val snapshotUpdatedAt = localEntities.associate { it.id to it.updatedAt }
            val localShapes = localEntities
                .map { it.toDomain() }
                .filterValidForFirebaseWrite("performFullSync/local")
            val serverShapes = shapeFirebaseStore.loadAllShapesIncludingDeleted(ticket)
                .getOrThrow()
                .filterValidForFirebaseWrite("performFullSync/server")
            val lastSyncTime = dataStore.data.first()[SyncPreferenceKeys.LAST_SYNC_TIME]

            val result = mergeShapesForFullSync(
                localShapes = localShapes,
                serverShapes = serverShapes,
                lastSyncTime = lastSyncTime,
            )

            val mergedIds = result.merged.mapTo(mutableSetOf()) { it.id }
            val staleLocalIds = localShapes
                .map { it.id }
                .filter { it !in mergedIds }
            val changedIds = session.commit(ticket) {
                // 반영하는 동안 사용자 쓰기가 끼어들지 않도록 다시 읽기와 쓰기를 한 트랜잭션으로 묶는다.
                database.withTransaction {
                    val changed = idsChangedLocallyDuringSync(
                        snapshotUpdatedAt,
                        shapeDao.getAllShapesOnce().associate { it.id to it.updatedAt },
                    )
                    val staleIds = staleLocalIds.filter { it !in changed }
                    val toWrite = result.merged.filter { it.id !in changed }
                    var shouldReconcileAlarms = false
                    if (staleIds.isNotEmpty()) {
                        shapeDao.deleteShapesByIds(staleIds)
                        shouldReconcileAlarms = true
                    }
                    if (toWrite.isNotEmpty()) {
                        shapeDao.insertShapes(toWrite.map { it.toEntity() })
                        shouldReconcileAlarms = true
                    }
                    if (shouldReconcileAlarms) {
                        reconcileEndDateAlarmsWithLocalShapes(additionalCancelShapeIds = staleIds)
                    }
                    changed
                }
            }
            if (changedIds.isNotEmpty()) Log.d(TAG, "동기화 중 기기에서 바뀐 도형 ${changedIds.size}개는 이번 반영에서 뺌")

            // Firebase: 로컬이 LWW 에서 이긴 항목 업로드
            val toUpload = result.toUpload.filter { it.id !in changedIds }.filterValidForFirebaseWrite("performFullSync/upload")
            if (toUpload.isNotEmpty()) {
                shapeFirebaseStore.saveShapes(ticket, toUpload)
            }

            if (shouldUpdateServerMetadataAfterFullSync(toUpload.size)) {
                shapeFirebaseStore.updateServerMetadata(ticket)
            }

            // 도형 색 = 연결된 드론 색(계약). 기기 값이 아니라 서버 병합 뒤 상태로 판단한다: 구버전에서 드론 색만 바뀐 도형이
            // 내려왔거나, 서버 값이 기기와 같은 시각인데 색만 다르면(서버 값을 받았으므로) 여기서 맞춰 올린다.
            // 맞춘 도형은 다음 동기화에서 서버와 같아지므로 다시 올리지 않는다.
            val recolorTime = System.currentTimeMillis()
            val recolored = session.commit(ticket) {
                recolorShapesToDronesLocally(database.droneDao().getAllDronesOnce().map { it.toDomain() }, recolorTime)
            }.filterValidForFirebaseWrite("performFullSync/recolor")
            if (recolored.isNotEmpty()) {
                markShapeLocalModification(recolorTime)
                shapeFirebaseStore.saveShapes(ticket, recolored)
                shapeFirebaseStore.updateServerMetadata(ticket)
                Log.d(TAG, "병합 뒤 도형 색을 드론 색으로 맞춤: count=${recolored.size}")
            }

            Log.d(TAG, "performFullSync 완료: 로컬=${localShapes.size}, 서버=${serverShapes.size}, 머지=${result.merged.size}, 업로드=${result.toUpload.size}")
            return toUpload.size + recolored.size
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

    private suspend fun reconcileEndDateAlarmsWithLocalShapes(
        additionalCancelShapeIds: List<String> = emptyList(),
    ) {
        val preferences = dataStore.data.first()
        val shapes = shapeDao.getAllShapesOnce()
            .map { it.toDomain() }
        val plan = buildEndDateAlarmReconcilePlan(
            shapes = shapes,
            additionalCancelShapeIds = additionalCancelShapeIds,
        )

        notificationScheduler.cancelKnownEndDateAlarms(plan.cancelShapeIds)
        if (!storedEndDateAlarmEnabled(preferences)) return

        plan.shapesToSchedule.forEach { shape ->
            val flightEndDate = shape.flightEndDate ?: return@forEach
            notificationScheduler.scheduleEndDateAlarm(
                shapeId = shape.id,
                flightEndDate = flightEndDate,
                shapeTitle = shape.title,
            )
        }
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
