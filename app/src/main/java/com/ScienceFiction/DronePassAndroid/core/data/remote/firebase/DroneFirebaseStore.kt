package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import android.util.Log
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.isValidForFirebaseRead
import com.ScienceFiction.DronePassAndroid.domain.model.isValidForFirebasePersistence
import com.ScienceFiction.DronePassAndroid.domain.model.normalizeFirebaseHexColorForRead
import com.ScienceFiction.DronePassAndroid.domain.model.normalizeFirebaseHexColorForWrite
import com.ScienceFiction.DronePassAndroid.domain.model.validateFirebaseDroneBatch
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun droneTimestampMillis(value: Any?): Long? {
    return (value as? Timestamp)?.toDate()?.time
}

private fun isValidDroneId(id: String): Boolean {
    return runCatching { UUID.fromString(id) }.isSuccess
}

internal class DroneFirebaseInvalidDataException(reason: String?) :
    IllegalStateException("Invalid drone data: ${reason ?: "unknown"}")

private val DRONE_OPTIONAL_FIRESTORE_FIELDS = listOf(
    "serialNumber",
    "takeoffWeight",
    "size",
    "memo",
    "deletedAt",
)

internal fun droneToFirestoreDocumentData(drone: DroneModel): Map<String, Any> {
    val data = mutableMapOf<String, Any>(
        "id" to drone.id,
        "name" to drone.name,
        "color" to normalizeFirebaseHexColorForWrite(drone.color),
        "createdAt" to Timestamp(Date(drone.createdAt)),
        "updatedAt" to Timestamp(Date(drone.updatedAt)),
    )
    drone.serialNumber?.let { data["serialNumber"] = it }
    drone.takeoffWeight?.let { data["takeoffWeight"] = it }
    drone.size?.let { data["size"] = it }
    drone.memo?.let { data["memo"] = it }
    drone.deletedAt?.let { data["deletedAt"] = Timestamp(Date(it)) }
    return data
}

internal fun droneToFirestoreMergeData(drone: DroneModel): Map<String, Any> {
    val data = droneToFirestoreDocumentData(drone).toMutableMap()
    DRONE_OPTIONAL_FIRESTORE_FIELDS.forEach { field ->
        if (!data.containsKey(field)) {
            data[field] = FieldValue.delete()
        }
    }
    return data
}

internal fun droneSoftDeleteFirestoreUpdateData(deletedAtMillis: Long): Map<String, Any> {
    val tombstone = Timestamp(Date(deletedAtMillis))
    return mapOf(
        "deletedAt" to tombstone,
        "updatedAt" to tombstone,
    )
}

internal fun droneFromFirestoreData(data: Map<String, Any?>): DroneModel? {
    val id = data["id"] as? String ?: return null
    if (!isValidDroneId(id)) return null
    val name = data["name"] as? String ?: return null
    val color = normalizeFirebaseHexColorForRead(
        color = data["color"] as? String ?: return null,
        fallback = "#007AFF",
    )
    val createdAt = droneTimestampMillis(data["createdAt"]) ?: return null
    val updatedAt = droneTimestampMillis(data["updatedAt"]) ?: return null
    if (hasInvalidFirestoreTimestampField(data, "deletedAt")) return null
    val deletedAt = droneTimestampMillis(data["deletedAt"])

    return DroneModel(
        id = id,
        name = name,
        color = color,
        serialNumber = data["serialNumber"] as? String,
        takeoffWeight = data["takeoffWeight"] as? String,
        size = data["size"] as? String,
        memo = data["memo"] as? String,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    ).takeIf { it.isValidForFirebaseRead() }
}

internal fun droneFromFirestoreDocument(documentId: String, data: Map<String, Any?>): DroneModel? {
    val drone = droneFromFirestoreData(data) ?: return null
    return drone.takeIf { it.id == documentId }
}

/**
 * Firestore의 drones 컬렉션과 통신하는 Store 클래스.
 * 경로: users/{userId}/drones/{droneId}
 */
@Singleton
class DroneFirebaseStore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "DroneFirebaseStore"
    }

    // region 컬렉션 경로 헬퍼

    private fun dronesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("drones")

    private fun metadataDocument(userId: String) =
        firestore.collection("users").document(userId)
            .collection("metadata").document("server")

    // endregion

    // region 읽기

    /**
     * 활성(deletedAt == null) 드론만 로드.
     * 네트워크/권한 오류와 "서버에 데이터 없음" 을 구분하기 위해 Result 반환.
     */
    suspend fun loadDrones(userId: String): Result<List<DroneModel>> {
        return try {
            val snapshot = dronesCollection(userId).get().await()
            val drones = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDocumentToDrone(doc.id, data)
            }.filter { it.deletedAt == null }
            Result.success(drones)
        } catch (e: Exception) {
            Log.e(TAG, "드론 로드 실패: userId=$userId", e)
            Result.failure(e)
        }
    }

    /**
     * 삭제된 드론을 포함한 전체 드론 로드.
     */
    suspend fun loadAllDronesIncludingDeleted(userId: String): Result<List<DroneModel>> {
        return try {
            val snapshot = dronesCollection(userId).get().await()
            val drones = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDocumentToDrone(doc.id, data)
            }
            Result.success(drones)
        } catch (e: Exception) {
            Log.e(TAG, "전체 드론 로드 실패: userId=$userId", e)
            Result.failure(e)
        }
    }

    // endregion

    // region 쓰기

    /**
     * 단일 드론 저장 (merge 모드)
     */
    suspend fun saveDrone(userId: String, drone: DroneModel) {
        try {
            val validation = drone.validateForFirebasePersistence()
            if (!validation.isValid) {
                throw DroneFirebaseInvalidDataException(validation.reason)
            }

            val data = droneToFirestoreData(drone)
            dronesCollection(userId)
                .document(drone.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "드론 저장 실패: userId=$userId, droneId=${drone.id}", e)
            throw e
        }
    }

    /**
     * 배치 저장 (500개 단위로 분할)
     * Firestore WriteBatch는 최대 500개 연산 제한이 있으므로 chunked 처리
     */
    suspend fun saveDrones(userId: String, drones: List<DroneModel>) {
        try {
            val validation = validateFirebaseDroneBatch(drones)
            if (!validation.isValid) {
                throw DroneFirebaseInvalidDataException(validation.reason)
            }

            firestoreWriteChunks(drones).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { drone ->
                    val data = droneToFirestoreData(drone)
                    val docRef = dronesCollection(userId).document(drone.id)
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "드론 배치 저장 실패: userId=$userId, count=${drones.size}", e)
            throw e
        }
    }

    /**
     * 드론 소프트 삭제 (deletedAt, updatedAt만 업데이트)
     */
    suspend fun deleteDrone(userId: String, droneId: String) {
        try {
            dronesCollection(userId).document(droneId).update(
                droneSoftDeleteFirestoreUpdateData(System.currentTimeMillis())
            ).await()
        } catch (e: Exception) {
            if (isMissingFirestoreDocument(e)) {
                Log.d(TAG, "드론 소프트 삭제 스킵: 서버 문서가 이미 없음 userId=$userId, droneId=$droneId")
                return
            }
            Log.e(TAG, "드론 소프트 삭제 실패: userId=$userId, droneId=$droneId", e)
            throw e
        }
    }

    /**
     * 서버 메타데이터 업데이트 (metadata/server 문서에 lastModified 타임스탬프 기록)
     */
    suspend fun updateServerMetadata(userId: String) {
        try {
            metadataDocument(userId).set(
                mapOf("lastModified" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "서버 메타데이터 업데이트 실패: userId=$userId", e)
            throw e
        }
    }

    // endregion

    // region Firestore 변환

    /**
     * DroneModel -> Firestore 문서 데이터로 변환
     */
    fun droneToFirestoreData(drone: DroneModel): Map<String, Any> {
        return droneToFirestoreMergeData(drone)
    }

    fun firestoreDocumentToDrone(documentId: String, data: Map<String, Any?>): DroneModel? {
        return try {
            val drone = droneFromFirestoreDocument(documentId, data)
            if (drone == null) {
                Log.w(TAG, "문서 ID 불일치 또는 필수 필드 누락/손상으로 드론 스킵: docId=$documentId, id=${data["id"]}")
            }
            drone
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 문서 -> DroneModel 변환 실패: docId=$documentId", e)
            null
        }
    }

    // endregion
}
