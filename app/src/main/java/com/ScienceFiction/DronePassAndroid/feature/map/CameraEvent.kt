package com.ScienceFiction.DronePassAndroid.feature.map

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate

/**
 * 지도 카메라 이동 이벤트
 * SharedFlow로 전달되는 1회성 이벤트
 */
sealed class CameraEvent {

    /**
     * 특정 좌표 및 줌 레벨로 카메라 이동
     */
    data class MoveTo(
        val coordinate: Coordinate,
        val zoom: Double
    ) : CameraEvent()

    /**
     * iOS MoveToShapeNotification 과 같은 2단계 이동.
     * 먼저 현재 중심에서 목표 줌으로 맞춘 뒤, 지도 projection 기준 오프셋 중심으로 이동한다.
     * 도형 하이라이트는 iOS MapViewModel 처럼 첫 번째 줌 단계 이후 적용한다.
     */
    data class MoveToShape(
        val coordinate: Coordinate,
        val zoom: Double,
        val highlightShapeId: String? = null,
    ) : CameraEvent()

    /**
     * 특정 좌표로 카메라 이동 (현재 줌 레벨 유지)
     */
    data class MoveWithoutZoom(
        val coordinate: Coordinate
    ) : CameraEvent()
}
