package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import android.util.Log
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

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
     * 활성(deletedAt == null) 드론만 로드
     */
    suspend fun loadDrones(userId: String): List<DroneModel> {
        return try {
            val snapshot = dronesCollection(userId).get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDataToDrone(data)
            }.filter { it.deletedAt == null }
        } catch (e: Exception) {
            Log.e(TAG, "드론 로드 실패: userId=$userId", e)
            emptyList()
        }
    }

    /**
     * 삭제된 드론을 포함한 전체 드론 로드
     */
    suspend fun loadAllDronesIncludingDeleted(userId: String): List<DroneModel> {
        return try {
            val snapshot = dronesCollection(userId).get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDataToDrone(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "전체 드론 로드 실패: userId=$userId", e)
            emptyList()
        }
    }

    // endregion

    // region 쓰기

    /**
     * 단일 드론 저장 (merge 모드)
     */
    suspend fun saveDrone(userId: String, drone: DroneModel) {
        try {
            val data = droneToFirestoreData(drone)
            dronesCollection(userId)
                .document(drone.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "드론 저장 실패: userId=$userId, droneId=${drone.id}", e)
        }
    }

    /**
     * 배치 저장 (500개 단위로 분할)
     * Firestore WriteBatch는 최대 500개 연산 제한이 있으므로 chunked 처리
     */
    suspend fun saveDrones(userId: String, drones: List<DroneModel>) {
        try {
            drones.chunked(500).forEach { chunk ->
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
        }
    }

    /**
     * 드론 소프트 삭제 (deletedAt, updatedAt만 업데이트)
     */
    suspend fun deleteDrone(userId: String, droneId: String) {
        try {
            val now = Timestamp(Date(System.currentTimeMillis()))
            dronesCollection(userId).document(droneId).update(
                mapOf(
                    "deletedAt" to now,
                    "updatedAt" to now
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "드론 소프트 삭제 실패: userId=$userId, droneId=$droneId", e)
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
        }
    }

    // endregion

    // region Firestore 변환

    /**
     * DroneModel -> Firestore 문서 데이터로 변환
     */
    fun droneToFirestoreData(drone: DroneModel): Map<String, Any?> {
        return mapOf(
            "id" to drone.id,
            "name" to drone.name,
            "color" to drone.color,
            "serialNumber" to drone.serialNumber,
            "takeoffWeight" to drone.takeoffWeight,
            "size" to drone.size,
            "memo" to drone.memo,
            "createdAt" to Timestamp(Date(drone.createdAt)),
            "updatedAt" to Timestamp(Date(drone.updatedAt)),
            "deletedAt" to drone.deletedAt?.let { Timestamp(Date(it)) }
        )
    }

    /**
     * Firestore 문서 데이터 -> DroneModel로 변환
     */
    fun firestoreDataToDrone(data: Map<String, Any?>): DroneModel? {
        return try {
            val id = data["id"] as? String ?: return null

            val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()
            val updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()
            val deletedAt = (data["deletedAt"] as? Timestamp)?.toDate()?.time

            DroneModel(
                id = id,
                name = data["name"] as? String ?: "",
                color = data["color"] as? String ?: "#007AFF",
                serialNumber = data["serialNumber"] as? String,
                takeoffWeight = data["takeoffWeight"] as? String,
                size = data["size"] as? String,
                memo = data["memo"] as? String,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 데이터 -> DroneModel 변환 실패", e)
            null
        }
    }

    // endregion
}
