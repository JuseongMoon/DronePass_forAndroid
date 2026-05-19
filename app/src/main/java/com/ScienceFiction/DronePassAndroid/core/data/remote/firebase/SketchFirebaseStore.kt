package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import android.util.Log
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore의 sketches 컬렉션과 통신하는 Store 클래스.
 * 경로: users/{userId}/sketches/{sketchId}
 */
@Singleton
class SketchFirebaseStore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "SketchFirebaseStore"
    }

    // region 컬렉션 경로 헬퍼

    private fun sketchesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("sketches")

    // RealtimeSyncManager가 metadata/sketchServer 를 리스닝하므로 동일 경로에 기록해야 한다.
    // metadata/server 는 Shape/Drone 공용. Sketch는 별도 문서를 사용해 Shape/Drone 동기화와 분리한다.
    private fun metadataDocument(userId: String) =
        firestore.collection("users").document(userId)
            .collection("metadata").document("sketchServer")

    // endregion

    // region 읽기

    /**
     * 활성(deletedAt == null) 스케치만 로드.
     * 네트워크/권한 오류와 "서버에 데이터 없음" 을 구분하기 위해 Result 반환.
     */
    suspend fun loadSketches(userId: String): Result<List<SketchModel>> {
        return try {
            val snapshot = sketchesCollection(userId).get().await()
            val sketches = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDataToSketch(data)
            }.filter { it.deletedAt == null }
            Result.success(sketches)
        } catch (e: Exception) {
            Log.e(TAG, "스케치 로드 실패: userId=$userId", e)
            Result.failure(e)
        }
    }

    /**
     * 삭제된 스케치를 포함한 전체 스케치 로드.
     */
    suspend fun loadAllSketchesIncludingDeleted(userId: String): Result<List<SketchModel>> {
        return try {
            val snapshot = sketchesCollection(userId).get().await()
            val sketches = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDataToSketch(data)
            }
            Result.success(sketches)
        } catch (e: Exception) {
            Log.e(TAG, "전체 스케치 로드 실패: userId=$userId", e)
            Result.failure(e)
        }
    }

    // endregion

    // region 쓰기

    /**
     * 단일 스케치 저장 (merge 모드)
     */
    suspend fun saveSketch(userId: String, sketch: SketchModel) {
        try {
            val data = sketchToFirestoreData(sketch)
            sketchesCollection(userId)
                .document(sketch.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "스케치 저장 실패: userId=$userId, sketchId=${sketch.id}", e)
        }
    }

    /**
     * 배치 저장 (500개 단위로 분할)
     * Firestore WriteBatch는 최대 500개 연산 제한이 있으므로 chunked 처리
     */
    suspend fun saveSketches(userId: String, sketches: List<SketchModel>) {
        try {
            sketches.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { sketch ->
                    val data = sketchToFirestoreData(sketch)
                    val docRef = sketchesCollection(userId).document(sketch.id)
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "스케치 배치 저장 실패: userId=$userId, count=${sketches.size}", e)
        }
    }

    /**
     * 스케치 소프트 삭제 (deletedAt, updatedAt만 업데이트)
     */
    suspend fun softDeleteSketch(userId: String, sketchId: String) {
        try {
            val now = Timestamp(Date(System.currentTimeMillis()))
            sketchesCollection(userId).document(sketchId).update(
                mapOf(
                    "deletedAt" to now,
                    "updatedAt" to now
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "스케치 소프트 삭제 실패: userId=$userId, sketchId=$sketchId", e)
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
     * SketchModel -> Firestore 문서 데이터로 변환
     * points는 List<Map<String, Double>> 형태로 저장 (각 {latitude, longitude})
     */
    fun sketchToFirestoreData(sketch: SketchModel): Map<String, Any?> {
        return mapOf(
            "id" to sketch.id,
            "points" to sketch.points.map { point ->
                mapOf(
                    "latitude" to point.latitude,
                    "longitude" to point.longitude
                )
            },
            "color" to sketch.color,
            "strokeWidth" to sketch.strokeWidth,
            "opacity" to sketch.opacity,
            "createdAt" to Timestamp(Date(sketch.createdAt)),
            "updatedAt" to Timestamp(Date(sketch.updatedAt)),
            "deletedAt" to sketch.deletedAt?.let { Timestamp(Date(it)) }
        )
    }

    /**
     * Firestore 문서 데이터 -> SketchModel로 변환.
     *
     * Firestore SDK 가 반환하는 Map 은 Any 컨테이너이므로, 이전의
     * `as? List<Map<String, Any>>` 는 erasure 후 List 만 확인하던 unchecked cast 였다.
     * 각 원소를 단계별로 Map<*, *> → Number 로 안전 검사하여 손상된 문서에서도
     * 부분 복구 가능하도록 한다.
     */
    fun firestoreDataToSketch(data: Map<String, Any?>): SketchModel? {
        return try {
            val id = data["id"] as? String ?: return null

            val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()
            val updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()
            val deletedAt = (data["deletedAt"] as? Timestamp)?.toDate()?.time

            // points 파싱: List<*> 로 받은 뒤 각 원소를 Map<*, *> 단계 검사.
            val rawPoints = data["points"] as? List<*> ?: emptyList<Any?>()
            val points = rawPoints.mapNotNull { entry ->
                val pointMap = entry as? Map<*, *> ?: return@mapNotNull null
                val latitude = (pointMap["latitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                val longitude = (pointMap["longitude"] as? Number)?.toDouble() ?: return@mapNotNull null
                Coordinate(latitude = latitude, longitude = longitude)
            }

            SketchModel(
                id = id,
                points = points,
                color = data["color"] as? String ?: "#FF0000",
                strokeWidth = (data["strokeWidth"] as? Number)?.toDouble() ?: 3.0,
                opacity = (data["opacity"] as? Number)?.toDouble() ?: 1.0,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 데이터 -> SketchModel 변환 실패", e)
            null
        }
    }

    // endregion
}
