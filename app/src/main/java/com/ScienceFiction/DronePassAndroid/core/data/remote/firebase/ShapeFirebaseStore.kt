package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import android.util.Log
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.domain.model.isValidForFirebaseRead
import com.ScienceFiction.DronePassAndroid.domain.model.isValidForFirebasePersistence
import com.ScienceFiction.DronePassAndroid.domain.model.normalizeFirebaseHexColorForRead
import com.ScienceFiction.DronePassAndroid.domain.model.normalizeFirebaseHexColorForWrite
import com.ScienceFiction.DronePassAndroid.domain.model.validateFirebaseShapeBatch
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
import kotlin.math.round

private const val COORDINATE_SCALE = 1_000_000.0

internal fun roundCoordinateComponent(value: Double): Double {
    return round(value * COORDINATE_SCALE) / COORDINATE_SCALE
}

internal fun coordinateToFirestoreMap(coordinate: Coordinate): Map<String, Double> {
    return mapOf(
        "latitude" to roundCoordinateComponent(coordinate.latitude),
        "longitude" to roundCoordinateComponent(coordinate.longitude)
    )
}

private fun firestoreMapToCoordinate(value: Any?): Coordinate? {
    val map = value as? Map<*, *> ?: return null
    val latitude = map["latitude"] as? Double ?: return null
    val longitude = map["longitude"] as? Double ?: return null
    return Coordinate(latitude = latitude, longitude = longitude)
}

private fun coordinatesToFirestoreList(coordinates: List<Coordinate>): List<Map<String, Double>> {
    return coordinates.map(::coordinateToFirestoreMap)
}

private fun firestoreListToCoordinates(value: Any?): List<Coordinate>? {
    val list = value as? List<*> ?: return null
    return list.mapNotNull(::firestoreMapToCoordinate)
}

private fun timestampMillis(value: Any?): Long? {
    return (value as? Timestamp)?.toDate()?.time
}

private fun isValidShapeId(id: String): Boolean {
    return runCatching { UUID.fromString(id) }.isSuccess
}

internal class ShapeFirebaseInvalidDataException(reason: String?) :
    IllegalStateException("Invalid shape data: ${reason ?: "unknown"}")

internal fun shapeToFirestoreDocumentData(shape: ShapeModel): Map<String, Any?> {
    return mapOf(
        "id" to shape.id,
        "title" to shape.title,
        "shapeType" to shape.shapeType.rawValue,
        "baseCoordinate" to coordinateToFirestoreMap(shape.baseCoordinate),
        "radius" to shape.radius.takeIf { shape.shapeType == ShapeType.CIRCLE },
        "secondCoordinate" to shape.secondCoordinate
            ?.takeIf { shape.shapeType == ShapeType.RECTANGLE }
            ?.let(::coordinateToFirestoreMap),
        "polygonCoordinates" to shape.polygonCoordinates
            ?.takeIf { shape.shapeType == ShapeType.POLYGON }
            ?.let(::coordinatesToFirestoreList),
        "polylineCoordinates" to shape.polylineCoordinates
            ?.takeIf { shape.shapeType == ShapeType.POLYLINE }
            ?.let(::coordinatesToFirestoreList),
        "height" to shape.height,
        "memo" to shape.memo.orEmpty(),
        "address" to shape.address.orEmpty(),
        "color" to normalizeFirebaseHexColorForWrite(shape.color),
        "droneId" to shape.droneId,
        "createdAt" to Timestamp(Date(shape.createdAt)),
        "updatedAt" to Timestamp(Date(shape.updatedAt)),
        "flightStartDate" to Timestamp(Date(shape.flightStartDate)),
        "flightEndDate" to shape.flightEndDate?.let { Timestamp(Date(it)) },
        "deletedAt" to shape.deletedAt?.let { Timestamp(Date(it)) }
    )
}

internal fun shapeFromFirestoreData(data: Map<String, Any?>): ShapeModel? {
    val id = data["id"] as? String ?: return null
    if (!isValidShapeId(id)) return null
    val title = data["title"] as? String ?: return null
    val color = normalizeFirebaseHexColorForRead(
        color = data["color"] as? String ?: return null,
        fallback = "#007AFF",
    )
    val shapeType = ShapeType.parseWireValue(data["shapeType"] as? String) ?: return null

    val flightStartDate = timestampMillis(data["flightStartDate"])
        ?: timestampMillis(data["startedAt"])
        ?: return null
    val flightEndDate = timestampMillis(data["flightEndDate"])
        ?: timestampMillis(data["expireDate"])
    val createdAt = timestampMillis(data["createdAt"]) ?: flightStartDate
    val updatedAt = timestampMillis(data["updatedAt"]) ?: createdAt
    val deletedAt = timestampMillis(data["deletedAt"])
    val baseCoordinate = firestoreMapToCoordinate(data["baseCoordinate"]) ?: return null
    val radius = if (shapeType == ShapeType.CIRCLE) {
        data["radius"] as? Double
    } else {
        null
    }
    val secondCoordinate = if (shapeType == ShapeType.RECTANGLE) {
        firestoreMapToCoordinate(data["secondCoordinate"]) ?: return null
    } else {
        null
    }
    val polygonCoordinates = if (shapeType == ShapeType.POLYGON) {
        firestoreListToCoordinates(data["polygonCoordinates"])
            ?.takeIf { it.size >= 3 }
            ?: return null
    } else {
        null
    }
    val polylineCoordinates = if (shapeType == ShapeType.POLYLINE) {
        firestoreListToCoordinates(data["polylineCoordinates"])
            ?.takeIf { it.size >= 2 }
            ?: return null
    } else {
        null
    }

    return ShapeModel(
        id = id,
        title = title,
        shapeType = shapeType,
        baseCoordinate = baseCoordinate,
        address = data["address"] as? String,
        radius = radius,
        secondCoordinate = secondCoordinate,
        polygonCoordinates = polygonCoordinates,
        polylineCoordinates = polylineCoordinates,
        height = data["height"] as? Double,
        memo = data["memo"] as? String,
        color = color,
        droneId = data["droneId"] as? String,
        createdAt = createdAt,
        updatedAt = updatedAt,
        flightStartDate = flightStartDate,
        flightEndDate = flightEndDate,
        deletedAt = deletedAt
    ).takeIf { it.isValidForFirebaseRead() }
}

internal fun shapeFromFirestoreDocument(documentId: String, data: Map<String, Any?>): ShapeModel? {
    val shape = shapeFromFirestoreData(data) ?: return null
    return shape.takeIf { it.id == documentId }
}

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
                firestoreDocumentToShape(doc.id, data)
            }.filter { it.deletedAt == null }

            val validation = validateFirebaseShapeBatch(shapes)
            if (!validation.isValid) {
                return Result.failure(ShapeFirebaseInvalidDataException(validation.reason))
            }

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
                firestoreDocumentToShape(doc.id, data)
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
            val validation = shape.validateForFirebasePersistence()
            if (!validation.isValid) {
                throw ShapeFirebaseInvalidDataException(validation.reason)
            }

            val data = shapeToFirestoreData(shape)
            shapesCollection(userId)
                .document(shape.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "도형 저장 실패: userId=$userId, shapeId=${shape.id}", e)
            throw e
        }
    }

    /**
     * 배치 저장 (500개 단위로 분할)
     * Firestore WriteBatch는 최대 500개 연산 제한이 있으므로 chunked 처리
     */
    suspend fun saveShapes(userId: String, shapes: List<ShapeModel>) {
        try {
            val validation = validateFirebaseShapeBatch(shapes)
            if (!validation.isValid) {
                throw ShapeFirebaseInvalidDataException(validation.reason)
            }

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
            throw e
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
            if (isMissingFirestoreDocument(e)) {
                Log.d(TAG, "도형 소프트 삭제 스킵: 서버 문서가 이미 없음 userId=$userId, shapeId=$shapeId")
                return
            }
            Log.e(TAG, "도형 소프트 삭제 실패: userId=$userId, shapeId=$shapeId", e)
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
     * ShapeModel -> Firestore 문서 데이터로 변환
     */
    fun shapeToFirestoreData(shape: ShapeModel): Map<String, Any?> {
        return shapeToFirestoreDocumentData(shape)
    }

    /**
     * Firestore 문서 데이터 -> ShapeModel로 변환
     * 레거시 필드 폴백 지원: startedAt -> flightStartDate, expireDate -> flightEndDate
     */
    @Suppress("UNCHECKED_CAST")
    fun firestoreDataToShape(data: Map<String, Any?>): ShapeModel? {
        return try {
            val shape = shapeFromFirestoreData(data)
            if (shape == null) {
                Log.w(TAG, "필수 필드 누락/손상으로 도형 스킵: id=${data["id"]}")
            }
            shape
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 데이터 -> ShapeModel 변환 실패", e)
            null
        }
    }

    fun firestoreDocumentToShape(documentId: String, data: Map<String, Any?>): ShapeModel? {
        return try {
            val shape = shapeFromFirestoreDocument(documentId, data)
            if (shape == null) {
                Log.w(TAG, "문서 ID 불일치 또는 필수 필드 누락/손상으로 도형 스킵: docId=$documentId, id=${data["id"]}")
            }
            shape
        } catch (e: Exception) {
            Log.e(TAG, "Firestore 문서 -> ShapeModel 변환 실패: docId=$documentId", e)
            null
        }
    }

    // endregion
}
