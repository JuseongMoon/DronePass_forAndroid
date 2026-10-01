package com.ScienceFiction.DronePassAndroid.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 원격 설정(`limits.json`)의 기능 플래그. 구독 설정과 같은 파일·같은 캐시에서 읽는다([SubscriptionManager]).
 * 파일이 없거나 값이 없으면 모두 꺼진 상태다.
 */
object RemoteFeatureFlags {
    private val _locationAuditUploadEnabled = MutableStateFlow(false)

    /** 위치정보 이용사실 확인자료의 서버 업로드(사양 v2 B-6). 서버 함수가 배포된 뒤에 켠다. */
    val locationAuditUploadEnabled: StateFlow<Boolean> = _locationAuditUploadEnabled.asStateFlow()

    internal fun update(config: RemoteSubscriptionConfig) {
        _locationAuditUploadEnabled.value = config.locationAuditUploadEnabled
    }
}
