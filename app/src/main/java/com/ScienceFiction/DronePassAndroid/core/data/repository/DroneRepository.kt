package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.DroneFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.isCloudSyncEnabled
import com.ScienceFiction.DronePassAndroid.core.data.sync.shouldUpdateServerMetadataAfterFullSync
import com.ScienceFiction.DronePassAndroid.core.data.sync.shouldRunImmediateCloudSync
import com.ScienceFiction.DronePassAndroid.core.util.compareIosLocalizedStandardStrings
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** 기기에서 자동으로 만든 기본 드론의 이름(앱 언어별). iOS `drone.edit.defaultName.first` 와 같다. */
internal val DefaultDroneNames = setOf("내 드론", "My Drone")

/** 기본 드론 색. iOS `PaletteColor.blue` 와 같다. */
internal const val DefaultDroneColor = "#007AFF"

/** 손대지 않은 기본 드론으로 볼 수 있는 생성·수정 시각 차이. */
internal const val UntouchedDroneWindowMillis = 1_000L

/**
 * 손대지 않은 기본 드론: 삭제 표시가 없고, 기본 이름·기본 색(대소문자 무시)이며, 나머지 칸이 모두 비었고
 * (null·"" 같음), 만든 뒤 수정한 적이 없다(|updatedAt − createdAt| ≤ 1초).
 */
internal fun isUntouchedDefaultDrone(drone: DroneModel): Boolean =
    drone.deletedAt == null &&
        drone.name in DefaultDroneNames &&
        drone.color.equals(DefaultDroneColor, ignoreCase = true) &&
        drone.serialNumber.isNullOrEmpty() &&
        drone.takeoffWeight.isNullOrEmpty() &&
        drone.size.isNullOrEmpty() &&
        drone.memo.isNullOrEmpty() &&
        abs(drone.updatedAt - drone.createdAt) <= UntouchedDroneWindowMillis

/**
 * 드론 전체 동기화 결과.
 * - [discardedLocalDroneIds]: 병합 결과·업로드에서 빼고 로컬에서도 지울 기기 기본 드론
 * - [reassignedShapeDroneIds]: 도형의 droneId 를 바꿀 맵(지운 기기 드론 → 서버 기본 드론)
 * - [selectionReplacements]: 선택돼 있던 지운 드론을 대신할 드론(규칙 1 은 D, 규칙 2 는 남은 드론의 이름순 첫째)
 */
internal data class DroneSyncMergeResult(
    val merged: List<DroneModel>,
    val toUpload: List<DroneModel>,
    val discardedLocalDroneIds: Set<String> = emptySet(),
    val reassignedShapeDroneIds: Map<String, String> = emptyMap(),
    val selectionReplacements: Map<String, String> = emptyMap(),
)

/**
 * 드론 전체 동기화 병합. iOS `DroneRepository.merge` 와 같은 규칙이다.
 * - id 기준으로 합치고, 양쪽에 있으면 updatedAt 이 늦은 쪽(같으면 서버)을 쓴다.
 * - 서버의 삭제 표시(deletedAt)도 병합에 넣어 다른 기기에서 지운 드론이 되살아나지 않게 한다.
 * - 서버에 없는 로컬 드론은 지우지 않고 올린다. 계정을 탈퇴한 뒤 다른 계정에 로그인해도
 *   로컬 도형과 원래 드론(id·이름)이 함께 따라가야 하기 때문이다. 삭제 기록은 올리지 않는다.
 * - 이름이 같아도 id 가 다르면 별개 드론으로 둔다.
 *
 * 예외: 로그아웃 상태에서 기기가 자동으로 만든 손대지 않은 기본 드론(L, 서버에 id 없음)은 로그인할 때
 * 계정의 드론과 겹치지 않게 정리한다([localShapeDroneIds] 는 삭제 표시를 포함한 로컬 도형의 droneId).
 * 1. 서버에 손대지 않은 기본 드론 D 가 있으면 L 을 버리고, L 을 쓰던 도형은 D 로 옮긴다
 *    (D 가 여럿이면 createdAt 이 가장 이른 것, 같으면 id 사전순).
 * 2. D 는 없고 살아 있는 서버 드론이 있으면, 도형이 쓰지 않는 L 만 버린다.
 * 3. 살아 있는 서버 드론이 없으면 L 을 그대로 올린다.
 * 서버 쪽 드론은 바꾸지 않는다.
 */
internal fun mergeDronesForFullSync(
    localDrones: List<DroneModel>,
    serverDrones: List<DroneModel>,
    localShapeDroneIds: Collection<String?> = emptyList(),
): DroneSyncMergeResult {
    val serverById = serverDrones.associateBy { it.id }
    val localById = localDrones.associateBy { it.id }
    val allIds = serverById.keys + localById.keys

    val mergedAll = allIds.mapNotNull { id ->
        val local = localById[id]
        val server = serverById[id]
        when {
            local != null && server != null -> if (server.updatedAt >= local.updatedAt) server else local
            else -> local ?: server
        }
    }

    // 서버에 없는 드론의 삭제 기록은 올리지 않는다(iOS DroneRepository.merge 와 같음).
    val toUploadAll = mergedAll.filter { drone ->
        val server = serverById[drone.id]
        if (server == null) drone.deletedAt == null else drone.updatedAt > server.updatedAt
    }

    val earliest = compareBy<DroneModel>({ it.createdAt }, { it.id })
    val aliveServer = serverDrones.filter { it.deletedAt == null }
    val serverDefault = aliveServer.filter(::isUntouchedDefaultDrone).minWithOrNull(earliest)
    val fallbackServer = aliveServer.minWithOrNull(earliest)
    val referenced = localShapeDroneIds.filterNotNull().toSet()

    val discarded = mutableSetOf<String>()
    val reassigned = mutableMapOf<String, String>()
    val selection = mutableMapOf<String, String>()
    val fallbackSelectionIds = mutableSetOf<String>()
    if (fallbackServer != null) {
        localDrones
            .filter { it.id !in serverById && isUntouchedDefaultDrone(it) }
            .forEach { local ->
                when {
                    serverDefault != null -> {
                        discarded += local.id
                        // 도형이 쓰지 않아도 맵에는 넣는다(iOS fixture 와 같음). 실제로 바뀌는 도형이 없을 뿐이다.
                        reassigned[local.id] = serverDefault.id
                        selection[local.id] = serverDefault.id
                    }
                    local.id !in referenced -> {
                        discarded += local.id
                        fallbackSelectionIds += local.id
                    }
                }
            }
    }

    val merged = mergedAll.filter { it.id !in discarded }
    // D 가 없어 버린 L 의 선택은 iOS 처럼 남은 활성 드론을 이름순으로 정렬한 첫째로 옮긴다.
    sortActiveDronesForIosList(merged.filter { it.deletedAt == null }).firstOrNull()?.let { first ->
        fallbackSelectionIds.forEach { selection[it] = first.id }
    }

    return DroneSyncMergeResult(
        merged = merged,
        toUpload = toUploadAll.filter { it.id !in discarded },
        discardedLocalDroneIds = discarded,
        reassignedShapeDroneIds = reassigned,
        selectionReplacements = selection,
    )
}

internal fun shouldCreateDefaultDrone(
    activeDroneCount: Int,
    isLoggedIn: Boolean,
    deferWhenLoggedIn: Boolean,
): Boolean {
    if (activeDroneCount > 0) return false
    return !(deferWhenLoggedIn && isLoggedIn)
}

internal fun sortActiveDronesForIosList(
    drones: List<DroneModel>,
    locale: Locale = Locale.getDefault(),
): List<DroneModel> {
    return drones.sortedWith { first, second ->
        compareIosLocalizedStandardStrings(first.name, second.name, locale)
    }
}

@Singleton
class DroneRepository @Inject constructor(
    private val droneDao: DroneDao,
    private val droneFirebaseStore: DroneFirebaseStore,
    private val shapeDao: ShapeDao,
    private val droneSelectionState: DroneSelectionState,
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
            sortActiveDronesForIosList(
                drones = entities.map { it.toDomain() },
                locale = locale,
            )
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
     *
     * 지도 화면 초기화 중 로그인 사용자의 로컬 DB가 비어 있더라도, Firebase full sync 전에는
     * 기본 드론을 만들지 않는다. iOS DroneManager 도 앱 시작 즉시 만드는 기본 드론은 임시
     * 메모리 상태이며, 실제 loadDrones() 이후에도 드론이 없을 때만 저장한다.
     */
    suspend fun ensureDefaultDroneIfNeeded(deferWhenLoggedIn: Boolean = false): DroneModel? {
        val activeDroneCount = droneDao.getActiveDroneCount()
        if (!shouldCreateDefaultDrone(
                activeDroneCount = activeDroneCount,
                isLoggedIn = auth.currentUser != null,
                deferWhenLoggedIn = deferWhenLoggedIn,
            )
        ) {
            return null
        }

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
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun insertDrone(drone: DroneModel) {
        droneDao.insertDrone(drone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(drone)
    }

    /**
     * 드론 정보 업데이트
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun updateDrone(drone: DroneModel) {
        droneDao.updateDrone(drone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(drone)
    }

    /**
     * 드론 소프트 삭제 (deletedAt 타임스탬프 설정)
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
     */
    suspend fun softDeleteDrone(drone: DroneModel) {
        val deletedDrone = drone.softDelete()
        droneDao.updateDrone(deletedDrone.toEntity())
        markDroneLocalModification()
        syncDroneToFirebase(deletedDrone)
    }

    /**
     * 소프트 삭제된 드론 복원
     * 로그인 + 클라우드 백업 ON 상태이면 Firestore에도 즉시 푸시한다.
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
     * 로그인 + 클라우드 백업 ON 상태가 아니면 NO-OP.
     */
    private suspend fun syncDroneToFirebase(drone: DroneModel) {
        val userId = currentImmediateCloudSyncUserId("syncDroneToFirebase") ?: return
        if (!drone.isValidForFirebaseWrite("syncDroneToFirebase")) return
        try {
            droneFirebaseStore.saveDrone(userId, drone)
            droneFirebaseStore.updateServerMetadata(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase 즉시 푸시 실패: droneId=${drone.id}", e)
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

    // ===== Firebase 동기화 메서드 =====

    /**
     * 양방향 동기화 (LWW 충돌 해결). 병합 규칙은 [mergeDronesForFullSync] 참고.
     */
    suspend fun performFullSync() {
        val userId = auth.currentUser?.uid ?: run {
            Log.d(TAG, "performFullSync: 로그인 상태가 아닙니다.")
            return
        }

        try {
            val localDrones = droneDao.getAllDronesOnce().map { it.toDomain() }
            val serverDrones = droneFirebaseStore.loadAllDronesIncludingDeleted(userId).getOrThrow()
            val localShapes = shapeDao.getAllShapesOnce()
            val result = mergeDronesForFullSync(
                localDrones = localDrones,
                serverDrones = serverDrones,
                localShapeDroneIds = localShapes.map { it.droneId },
            )

            if (result.merged.isNotEmpty()) droneDao.insertDrones(result.merged.map { it.toEntity() })

            // 로그인 전에 기기가 만든 손대지 않은 기본 드론: 도형은 계정의 기본 드론으로 옮기고(바로 이어지는
            // 도형 전체 동기화가 더 늦은 updatedAt 으로 올린다), 드론은 지운다. 삭제 기록은 올리지 않는다.
            if (result.reassignedShapeDroneIds.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val moved = localShapes.mapNotNull { shape ->
                    val target = shape.droneId?.let(result.reassignedShapeDroneIds::get) ?: return@mapNotNull null
                    shape.copy(droneId = target, updatedAt = now)
                }
                shapeDao.insertShapes(moved)
                Log.d(TAG, "기기 기본 드론의 도형 ${moved.size}개를 계정 기본 드론으로 옮김")
            }
            if (result.discardedLocalDroneIds.isNotEmpty()) {
                droneDao.deleteDronesByIds(result.discardedLocalDroneIds.toList())
                droneSelectionState.replaceDrones(result.selectionReplacements)
                Log.d(TAG, "손대지 않은 기기 기본 드론 ${result.discardedLocalDroneIds.size}대를 정리함")
            }

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
