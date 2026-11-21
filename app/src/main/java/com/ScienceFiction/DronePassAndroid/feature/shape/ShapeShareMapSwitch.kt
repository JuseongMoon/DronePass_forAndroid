package com.ScienceFiction.DronePassAndroid.feature.shape

/**
 * 공유 카드에 지도를 넣을지(D-71 원격 스위치, `features.shareCardMap.enabled`, 계약 SUBSCRIPTION_CONTRACT §6).
 * 운영은 이번 실행에서 받은 원격 값만 따른다. 개발 빌드는 `-PshareCardMapDebug=on|off` 로 빌드했을 때만
 * [debugForce] 가 "on"·"off" 가 되어 원격 값과 관계없이 켜거나 끈다. 릴리스 빌드의 [debugForce] 는 항상 빈 값이다.
 */
internal fun effectiveShareCardMapEnabled(remoteFetchedEnabled: Boolean, debugForce: String): Boolean = when (debugForce) {
    "on" -> true
    "off" -> false
    else -> remoteFetchedEnabled
}
