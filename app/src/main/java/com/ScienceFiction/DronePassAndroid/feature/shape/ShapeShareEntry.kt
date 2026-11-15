package com.ScienceFiction.DronePassAndroid.feature.shape

/**
 * 도형 상세의 공유 버튼(공유 카드 입구)을 보여 줄지(D-73).
 * 공유 카드는 지도 이용 허락을 받은 뒤 지도와 함께 낸다. 그때까지 입구만 숨기고 공유 화면·지도 없는 카드 코드는 그대로 둔다.
 * 원격 설정이나 빌드 설정과 묶지 않는다. 낼 때 [isEnabledInRelease] 만 true 로 바꾼다(iOS `ShareCardEntry` 와 같은 방식).
 */
internal object ShapeShareEntry {
    const val isEnabledInRelease: Boolean = false

    /**
     * 개발 빌드는 `-PshareCardEntryDebug=on|off` 로 빌드했을 때만 [debugForce] 가 "on"·"off" 가 되어 상수와 관계없이 켜거나 끈다.
     * 릴리스 빌드의 [debugForce] 는 항상 빈 값이다.
     */
    fun isVisible(debugForce: String, enabledInRelease: Boolean = isEnabledInRelease): Boolean = when (debugForce) {
        "on" -> true
        "off" -> false
        else -> enabledInRelease
    }
}
