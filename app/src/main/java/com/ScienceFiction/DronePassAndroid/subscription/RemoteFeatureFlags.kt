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

    private val _shareCardMapEnabled = MutableStateFlow(false)

    /**
     * 공유 카드에 지도를 넣을지(D-71, NCP 약관). **이번 실행에서 받은 설정**이 true 일 때만 켠다.
     * 설정 받기 전·오프라인·저장본뿐이면 false 다. 저장본에서 되살리는 [update] 는 이 값을 건드리지 않는다.
     */
    val shareCardMapEnabled: StateFlow<Boolean> = _shareCardMapEnabled.asStateFlow()

    /** 저장본이나 이번 실행에서 받은 설정의 플래그. */
    internal fun update(config: RemoteSubscriptionConfig) {
        _locationAuditUploadEnabled.value = config.locationAuditUploadEnabled
    }

    /** 이번 실행에서 서버에서 받은 설정에만 부른다(계약 §6 저장본 규칙). */
    internal fun updateFetched(config: RemoteSubscriptionConfig) {
        update(config)
        _shareCardMapEnabled.value = config.shareCardMapEnabled
    }
}
