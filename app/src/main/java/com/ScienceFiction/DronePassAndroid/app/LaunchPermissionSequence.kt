package com.ScienceFiction.DronePassAndroid.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 첫 실행 권한 요청 순서.
 *
 * Android 는 런타임 권한 요청을 한 번에 하나만 처리한다. 지도 화면의 위치 요청과 MainActivity 의 알림 요청이
 * 동시에 나가면 뒤 요청이 조용히 버려지는데, 알림 쪽은 "이미 요청함" 으로 저장돼 다시 묻지 않게 된다.
 * iOS 처럼 위치 → 알림 순서로 묻도록, 알림 요청은 위치 요청이 끝날 때까지 기다린다.
 */
internal object LaunchPermissionSequence {
    private val _locationRequestSettled = MutableStateFlow(false)
    val locationRequestSettled: StateFlow<Boolean> = _locationRequestSettled.asStateFlow()

    /** 위치 권한이 이미 있거나, 요청 대화상자가 닫혔을 때 호출한다. */
    fun markLocationRequestSettled() {
        _locationRequestSettled.value = true
    }
}
