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
     * 특정 좌표로 카메라 이동 (현재 줌 레벨 유지)
     */
    data class MoveWithoutZoom(
        val coordinate: Coordinate
    ) : CameraEvent()
}
