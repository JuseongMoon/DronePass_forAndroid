package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.UUID

data class ShapeModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val shapeType: ShapeType = ShapeType.CIRCLE,
    val baseCoordinate: Coordinate = Coordinate(37.5665, 126.9780),
    val address: String? = null,
    val radius: Double? = null,
    val height: Double? = null,
    val memo: String? = null,
    val color: String = "#007AFF",
    val droneId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val flightStartDate: Long = System.currentTimeMillis(),
    val flightEndDate: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 비행 종료일이 현재 시각보다 과거인지 여부
     */
    val isExpired: Boolean
        get() = flightEndDate?.let { it < System.currentTimeMillis() } ?: false

    /**
     * 비행 시작일이 현재 시각보다 미래인지 여부
     */
    val isNotStarted: Boolean
        get() = flightStartDate > System.currentTimeMillis()

    /**
     * soft delete 되었는지 여부
     */
    val isDeleted: Boolean
        get() = deletedAt != null

    /**
     * 유효한 색상 반환
     */
    val effectiveColor: String
        get() = color

    /**
     * 드론에 연결되었는지 여부
     */
    val isConnectedToDrone: Boolean
        get() = droneId != null

    /**
     * 레거시(드론 미연결) 도형인지 여부
     */
    val isLegacyShape: Boolean
        get() = droneId == null

    /**
     * PaletteColor 열거형으로 변환
     */
    val paletteColor: PaletteColor?
        get() = PaletteColor.fromHex(color)

    /**
     * soft delete 수행
     */
    fun softDelete(): ShapeModel = copy(
        deletedAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    /**
     * soft delete 복원
     */
    fun restore(): ShapeModel = copy(
        deletedAt = null,
        updatedAt = System.currentTimeMillis()
    )
}
