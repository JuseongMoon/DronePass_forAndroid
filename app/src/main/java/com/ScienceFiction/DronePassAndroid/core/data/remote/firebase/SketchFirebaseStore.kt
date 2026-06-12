package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import android.util.Log
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.domain.model.isValidForFirebasePersistence
import com.ScienceFiction.DronePassAndroid.domain.model.isValidShapeCoordinate
import com.ScienceFiction.DronePassAndroid.domain.model.normalizeFirebaseHexColorForWrite
import com.ScienceFiction.DronePassAndroid.domain.model.validateFirebaseSketchBatch
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import kotlin.math.round
import javax.inject.Inject
import javax.inject.Singleton

internal fun roundSketchCoordinateForFirestore(value: Double): Double {
    return round(value * 1_000_000.0) / 1_000_000.0
}

internal fun roundSketchOpacityForFirestore(opacity: Double): Double {
    return round(opacity * 100.0) / 100.0
}

private fun sketchTimestampMillis(value: Any?): Long? {
    return (value as? Timestamp)?.toDate()?.time
}

private fun isValidSketchId(id: String): Boolean {
    return runCatching { UUID.fromString(id) }.isSuccess
}

private fun firestoreMapToSketchPoint(value: Any?): Coordinate? {
    val map = value as? Map<*, *> ?: return null
    val latitude = map["latitude"] as? Double ?: return null
    val longitude = map["longitude"] as? Double ?: return null
    return Coordinate(latitude = latitude, longitude = longitude)
        .takeIf { it.isValidShapeCoordinate() }
}

private fun firestoreListToSketchPoints(value: Any?): List<Coordinate> {
    val list = value as? List<*> ?: return emptyList()
    return list.mapNotNull(::firestoreMapToSketchPoint)
}

internal class SketchFirebaseInvalidDataException(reason: String?) :
    IllegalStateException("Invalid sketch data: ${reason ?: "unknown"}")

internal fun sketchToFirestoreDocumentData(sketch: SketchModel): Map<String, Any?> {
    return mapOf(
        "id" to sketch.id,
        "points" to sketch.points.map { point ->
            mapOf(
                "latitude" to roundSketchCoordinateForFirestore(point.latitude),
                "longitude" to roundSketchCoordinateForFirestore(point.longitude)
            )
        },
        "color" to normalizeFirebaseHexColorForWrite(sketch.color),
        "strokeWidth" to sketch.strokeWidth,
        "opacity" to roundSketchOpacityForFirestore(sketch.opacity),
        "createdAt" to Timestamp(Date(sketch.createdAt)),
        "updatedAt" to Timestamp(Date(sketch.updatedAt)),
        "deletedAt" to sketch.deletedAt?.let { Timestamp(Date(it)) }
    )
}

internal fun sketchFromFirestoreData(
    data: Map<String, Any?>,
    nowMillis: Long = System.currentTimeMillis(),
): SketchModel? {
    val id = data["id"] as? String ?: return null
    if (!isValidSketchId(id)) return null

    val createdAt = sketchTimestampMillis(data["createdAt"]) ?: nowMillis
    val updatedAt = sketchTimestampMillis(data["updatedAt"]) ?: createdAt
    val deletedAt = sketchTimestampMillis(data["deletedAt"])

    val points = firestoreListToSketchPoints(data["points"])

    return SketchModel(
        id = id,
        points = points,
        color = data["color"] as? String ?: "#FF0000",
        strokeWidth = data["strokeWidth"]?.let { it as? Double ?: return null } ?: 3.0,
        opacity = data["opacity"]?.let { it as? Double ?: return null } ?: 1.0,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    ).takeIf { it.isValidForFirebasePersistence() }
}

internal fun sketchFromFirestoreDocument(documentId: String, data: Map<String, Any?>): SketchModel? {
    val sketch = sketchFromFirestoreData(data) ?: return null
    return sketch.takeIf { it.id == documentId }
}

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
                firestoreDocumentToSketch(doc.id, data)
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
                firestoreDocumentToSketch(doc.id, data)
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
            val validation = sketch.validateForFirebasePersistence()
            if (!validation.isValid) {
                throw SketchFirebaseInvalidDataException(validation.reason)
            }

            val data = sketchToFirestoreData(sketch)
            sketchesCollection(userId)
                .document(sketch.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "스케치 저장 실패: userId=$userId, sketchId=${sketch.id}", e)
            throw e
        }
    }

    /**
     * 배치 저장 (500개 단위로 분할)
     * Firestore WriteBatch는 최대 500개 연산 제한이 있으므로 chunked 처리
     */
    suspend fun saveSketches(userId: String, sketches: List<SketchModel>) {
        try {
            val validation = validateFirebaseSketchBatch(sketches)
            if (!validation.isValid) {
                throw SketchFirebaseInvalidDataException(validation.reason)
            }

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
            throw e
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
            if (isMissingFirestoreDocument(e)) {
                Log.d(TAG, "스케치 소프트 삭제 스킵: 서버 문서가 이미 없음 userId=$userId, sketchId=$sketchId")
                return
            }
            Log.e(TAG, "스케치 소프트 삭제 실패: userId=$userId, sketchId=$sketchId", e)
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
     * SketchModel -> Firestore 문서 데이터로 변환
     * points는 List<Map<String, Double>> 형태로 저장 (각 {latitude, longitude})
     */
    fun sketchToFirestoreData(sketch: SketchModel): Map<String, Any?> {
        return sketchToFirestoreDocumentData(sketch)
    }

    /**
     * Firestore 문서 데이터 -> SketchModel로 변환.
     *
     * iOS SketchFirebaseStore 는 points 배열 안에서 파싱 가능한 좌표만 compactMap 으로 살리고,
     * points 필드가 없거나 배열이 아니면 빈 배열로 읽는다. Android도 같은 관대 파싱을 따른다.
     */
    fun firestoreDataToSketch(data: Map<String, Any?>): SketchModel? {
        return try {
            sketchFromFirestoreData(data)
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 데이터 -> SketchModel 변환 실패", e)
            null
        }
    }

    fun firestoreDocumentToSketch(documentId: String, data: Map<String, Any?>): SketchModel? {
        return try {
            val sketch = sketchFromFirestoreDocument(documentId, data)
            if (sketch == null) {
                Log.w(TAG, "문서 ID 불일치 또는 필수 필드 누락/손상으로 스케치 스킵: docId=$documentId, id=${data["id"]}")
            }
            sketch
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 문서 -> SketchModel 변환 실패: docId=$documentId", e)
            null
        }
    }

    // endregion
}
