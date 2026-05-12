package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import android.util.Log
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore의 shapes 컬렉션과 통신하는 Store 클래스.
 * 경로: users/{userId}/shapes/{shapeId}
 */
@Singleton
class ShapeFirebaseStore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ShapeFirebaseStore"
    }

    // region 컬렉션 경로 헬퍼

    private fun shapesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("shapes")

    private fun metadataDocument(userId: String) =
        firestore.collection("users").document(userId)
            .collection("metadata").document("server")

    // endregion

    // region 읽기

    /**
     * 활성(deletedAt == null) 도형만 로드.
     *
     * 네트워크/권한 오류와 "서버에 데이터 없음" 을 구분하기 위해 [Result] 를 반환한다.
     * 호출자는 실패 시 동기화를 중단하고 재시도 큐로 위임해야 한다.
     * 이전: 실패 시 emptyList() 반환 → 호출자가 "서버에 데이터 없음" 으로 오인하여
     * 잘못된 머지/덮어쓰기를 수행할 위험이 있었음.
     */
    suspend fun loadShapes(userId: String): Result<List<ShapeModel>> {
        return try {
            val snapshot = shapesCollection(userId).get().await()
            val shapes = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDataToShape(data)
            }.filter { it.deletedAt == null }
            Result.success(shapes)
        } catch (e: Exception) {
            Log.e(TAG, "도형 로드 실패: userId=$userId", e)
            Result.failure(e)
        }
    }

    /**
     * 삭제된 도형을 포함한 전체 도형 로드.
     */
    suspend fun loadAllShapesIncludingDeleted(userId: String): Result<List<ShapeModel>> {
        return try {
            val snapshot = shapesCollection(userId).get().await()
            val shapes = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                firestoreDataToShape(data)
            }
            Result.success(shapes)
        } catch (e: Exception) {
            Log.e(TAG, "전체 도형 로드 실패: userId=$userId", e)
            Result.failure(e)
        }
    }

    // endregion

    // region 쓰기

    /**
     * 단일 도형 저장 (merge 모드)
     */
    suspend fun saveShape(userId: String, shape: ShapeModel) {
        try {
            val data = shapeToFirestoreData(shape)
            shapesCollection(userId)
                .document(shape.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "도형 저장 실패: userId=$userId, shapeId=${shape.id}", e)
        }
    }

    /**
     * 배치 저장 (500개 단위로 분할)
     * Firestore WriteBatch는 최대 500개 연산 제한이 있으므로 chunked 처리
     */
    suspend fun saveShapes(userId: String, shapes: List<ShapeModel>) {
        try {
            shapes.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { shape ->
                    val data = shapeToFirestoreData(shape)
                    val docRef = shapesCollection(userId).document(shape.id)
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "도형 배치 저장 실패: userId=$userId, count=${shapes.size}", e)
        }
    }

    /**
     * 도형 소프트 삭제 (deletedAt, updatedAt만 업데이트)
     */
    suspend fun softDeleteShape(userId: String, shapeId: String) {
        try {
            val now = Timestamp(Date(System.currentTimeMillis()))
            shapesCollection(userId).document(shapeId).update(
                mapOf(
                    "deletedAt" to now,
                    "updatedAt" to now
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "도형 소프트 삭제 실패: userId=$userId, shapeId=$shapeId", e)
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
     * ShapeModel -> Firestore 문서 데이터로 변환
     */
    fun shapeToFirestoreData(shape: ShapeModel): Map<String, Any?> {
        return mapOf(
            "id" to shape.id,
            "title" to shape.title,
            "shapeType" to shape.shapeType.name, // "CIRCLE"
            "baseCoordinate" to mapOf(
                "latitude" to shape.baseCoordinate.latitude,
                "longitude" to shape.baseCoordinate.longitude
            ),
            "radius" to shape.radius,
            "height" to shape.height,
            "memo" to shape.memo,
            "address" to shape.address,
            "color" to shape.color,
            "droneId" to shape.droneId,
            "createdAt" to Timestamp(Date(shape.createdAt)),
            "updatedAt" to Timestamp(Date(shape.updatedAt)),
            "flightStartDate" to Timestamp(Date(shape.flightStartDate)),
            "flightEndDate" to shape.flightEndDate?.let { Timestamp(Date(it)) },
            "deletedAt" to shape.deletedAt?.let { Timestamp(Date(it)) }
        )
    }

    /**
     * Firestore 문서 데이터 -> ShapeModel로 변환
     * 레거시 필드 폴백 지원: startedAt -> flightStartDate, expireDate -> flightEndDate
     */
    @Suppress("UNCHECKED_CAST")
    fun firestoreDataToShape(data: Map<String, Any?>): ShapeModel? {
        return try {
            val id = data["id"] as? String ?: return null

            // 레거시 필드 폴백: flightStartDate
            val flightStartDate = (data["flightStartDate"] as? Timestamp)?.toDate()?.time
                ?: (data["startedAt"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()

            // 레거시 필드 폴백: flightEndDate
            val flightEndDate = (data["flightEndDate"] as? Timestamp)?.toDate()?.time
                ?: (data["expireDate"] as? Timestamp)?.toDate()?.time

            // createdAt이 없으면 flightStartDate 사용
            val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time
                ?: flightStartDate

            val updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()

            val deletedAt = (data["deletedAt"] as? Timestamp)?.toDate()?.time

            // baseCoordinate 파싱. 좌표가 없거나 숫자 변환 실패 시 데이터 손상으로 간주하고
            // null 반환 → 호출자가 mapNotNull 로 스킵. 이전: 서울 시청(37.5665, 126.978)
            // 으로 fallback 하여 손상을 침묵 처리하던 문제 제거.
            val coordMap = data["baseCoordinate"] as? Map<*, *>
            val lat = (coordMap?.get("latitude") as? Number)?.toDouble()
            val lon = (coordMap?.get("longitude") as? Number)?.toDouble()
            if (lat == null || lon == null) {
                Log.w(TAG, "baseCoordinate 누락/손상으로 도형 스킵: id=$id")
                return null
            }
            val baseCoordinate = Coordinate(latitude = lat, longitude = lon)

            // shapeType 파싱
            val shapeTypeString = data["shapeType"] as? String ?: "CIRCLE"
            val shapeType = try {
                ShapeType.valueOf(shapeTypeString)
            } catch (e: IllegalArgumentException) {
                ShapeType.CIRCLE
            }

            ShapeModel(
                id = id,
                title = data["title"] as? String ?: "",
                shapeType = shapeType,
                baseCoordinate = baseCoordinate,
                address = data["address"] as? String,
                radius = (data["radius"] as? Number)?.toDouble(),
                height = (data["height"] as? Number)?.toDouble(),
                memo = data["memo"] as? String,
                color = data["color"] as? String ?: "#007AFF",
                droneId = data["droneId"] as? String,
                createdAt = createdAt,
                updatedAt = updatedAt,
                flightStartDate = flightStartDate,
                flightEndDate = flightEndDate,
                deletedAt = deletedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 데이터 -> ShapeModel 변환 실패", e)
            null
        }
    }

    // endregion
}
