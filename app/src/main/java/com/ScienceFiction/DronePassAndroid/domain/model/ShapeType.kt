package com.ScienceFiction.DronePassAndroid.domain.model

/**
 * 도형 종류.
 *
 * 현재는 [CIRCLE] 만 사용한다. iOS 원본은 RECTANGLE/POLYGON 도 지원하지만
 * Android 포팅 1단계에서는 비행구역 시각화 핵심인 원형만 우선 지원하며,
 * 추후 확장 시 [com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.EntityMapper]
 * 의 fromDomain/toDomain 매핑과 ShapeOverlayManager 의 렌더링 분기를 함께 갱신해야 한다.
 */
enum class ShapeType(val koreanName: String) {
    CIRCLE("원형")
}
