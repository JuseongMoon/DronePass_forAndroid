package com.ScienceFiction.DronePassAndroid.core.account

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.domain.model.validateForFirebasePersistence

/** 계정에 올릴 항목. [skippedCount] 는 서버 검증을 통과하지 못해 뺀 항목 수. */
internal data class AccountUploadBatch(
    val drones: List<DroneModel>,
    val shapes: List<ShapeModel>,
    val sketches: List<SketchModel>,
    val skippedCount: Int,
)

/**
 * 가져오기·로그아웃 업로드 전에 항목을 정리한다. 서버 배치 저장은 하나라도 무효면 묶음 전체를 거부해,
 * 공백 제목 도형 하나 때문에 가져오기와 로그아웃 업로드가 계속 실패했다.
 * - 공백뿐인 도형 제목·드론 이름은 기본값으로 바꿔 올린다(updatedAt 은 그대로).
 * - 그래도 무효인 항목(반경 0 등)과 중복 id 는 빼고 나머지를 올린다. 저장소의 즉시·전체 동기화와 같은 방침이다.
 */
internal fun prepareAccountUpload(
    drones: List<DroneModel>,
    shapes: List<ShapeModel>,
    sketches: List<SketchModel>,
    defaultShapeTitle: String,
    defaultDroneName: String,
): AccountUploadBatch {
    val fixedDrones = drones.map { if (it.name.isBlank()) it.copy(name = defaultDroneName) else it }
        .filter { it.validateForFirebasePersistence().isValid }
        .distinctBy { it.id }
    val fixedShapes = shapes.map { if (it.title.isBlank()) it.copy(title = defaultShapeTitle) else it }
        .filter { it.validateForFirebasePersistence().isValid }
        .distinctBy { it.id }
    val validSketches = sketches
        .filter { it.validateForFirebasePersistence().isValid }
        .distinctBy { it.id }
    val skipped = (drones.size - fixedDrones.size) + (shapes.size - fixedShapes.size) + (sketches.size - validSketches.size)
    return AccountUploadBatch(fixedDrones, fixedShapes, validSketches, skipped)
}
