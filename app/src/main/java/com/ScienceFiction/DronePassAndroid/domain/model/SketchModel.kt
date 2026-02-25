package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.UUID

data class SketchModel(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Coordinate> = emptyList(),
    val color: String = "#FF0000",
    val strokeWidth: Double = 3.0,
    val opacity: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    /**
     * soft delete 되었는지 여부
     */
    val isDeleted: Boolean
        get() = deletedAt != null

    /**
     * 포인트가 존재하는지 여부
     */
    val hasPoints: Boolean
        get() = points.isNotEmpty()

    /**
     * 포인트 개수
     */
    val pointCount: Int
        get() = points.size

    /**
     * PaletteColor 열거형으로 변환
     */
    val paletteColor: PaletteColor?
        get() = PaletteColor.fromHex(color)

    /**
     * soft delete 수행
     */
    fun softDelete(): SketchModel = copy(
        deletedAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    /**
     * soft delete 복원
     */
    fun restore(): SketchModel = copy(
        deletedAt = null,
        updatedAt = System.currentTimeMillis()
    )
}
