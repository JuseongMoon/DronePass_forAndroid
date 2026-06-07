package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.UUID

data class DroneModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val color: String = PaletteColor.BLUE.hex,
    val serialNumber: String? = null,
    val takeoffWeight: String? = null,
    val size: String? = null,
    val memo: String? = null,
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
     * PaletteColor 열거형으로 변환
     */
    val paletteColor: PaletteColor?
        get() = PaletteColor.fromHex(color)

    /**
     * soft delete 수행
     */
    fun softDelete(): DroneModel = copy(
        deletedAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    /**
     * soft delete 복원
     */
    fun restore(): DroneModel = copy(
        deletedAt = null,
        updatedAt = System.currentTimeMillis()
    )

    companion object {
        /**
         * 기본 드론 생성
         */
        fun createDefault(defaultName: String = "내 드론"): DroneModel = DroneModel(
            name = defaultName,
            color = PaletteColor.BLUE.hex
        )

        /**
         * 순서 인덱스에 따른 색상 매핑으로 새 드론 생성
         */
        fun createNew(index: Int): DroneModel = DroneModel(
            name = "드론 ${index + 1}",
            color = PaletteColor.droneColorAtIndex(index).hex
        )
    }
}

/**
 * 삭제되지 않은 활성 드론만 필터링
 */
val List<DroneModel>.active: List<DroneModel>
    get() = filter { !it.isDeleted }

/**
 * ID로 드론 검색
 */
fun List<DroneModel>.findById(id: String): DroneModel? =
    find { it.id == id }

/**
 * 이름으로 드론 검색
 */
fun List<DroneModel>.findByName(name: String): DroneModel? =
    find { it.name == name }
