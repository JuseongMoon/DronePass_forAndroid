package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.Calendar
import java.util.TimeZone

data class ShapeModel(
    val id: String = newCanonicalFirestoreUuid(),
    val title: String = "",
    val shapeType: ShapeType = ShapeType.CIRCLE,
    val baseCoordinate: Coordinate = Coordinate(37.5665, 126.9780),
    val address: String? = null,
    val radius: Double? = null,
    val secondCoordinate: Coordinate? = null,
    val polygonCoordinates: List<Coordinate>? = null,
    val polylineCoordinates: List<Coordinate>? = null,
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
        get() = flightEndDate?.let { it <= System.currentTimeMillis() } ?: false

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
     * 일 단위로 입력한 도형인지. 입력 방식은 저장되지 않아 저장된 시각으로 판정한다.
     * 일 단위 모드는 시작을 00:00:00, 종료를 23:59(초 무관)로 맞춘다 (iOS isDateOnlyFlight 정합).
     */
    fun isDateOnlyFlight(timeZone: TimeZone = TimeZone.getDefault()): Boolean {
        val start = Calendar.getInstance(timeZone).apply { timeInMillis = flightStartDate }
        if (start.get(Calendar.HOUR_OF_DAY) != 0 ||
            start.get(Calendar.MINUTE) != 0 ||
            start.get(Calendar.SECOND) != 0
        ) {
            return false
        }
        val end = flightEndDate ?: return true
        val endTime = Calendar.getInstance(timeZone).apply { timeInMillis = end }
        return endTime.get(Calendar.HOUR_OF_DAY) == 23 && endTime.get(Calendar.MINUTE) == 59
    }

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
