package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 공유 카드에 지도를 넣을지(D-71 원격 스위치, `features.shareCardMap.enabled`).
 * 운영은 이번 실행에서 받은 원격 값만 따른다. 개발 빌드는 `-PshareCardMapDebug=on|off` 로 빌드했을 때만
 * [debugForce] 가 "on"·"off" 가 되어 원격 값과 관계없이 켜거나 끈다. 릴리스 빌드의 [debugForce] 는 항상 빈 값이다.
 */
internal fun effectiveShareCardMapEnabled(remoteFetchedEnabled: Boolean, debugForce: String): Boolean = when (debugForce) {
    "on" -> true
    "off" -> false
    else -> remoteFetchedEnabled
}

private val ShapeShareNoMapBackground = Color(0xFFF2F2F7)

/**
 * 지도 없는 카드의 지도 칸. 지도(MapView·스냅샷)를 만들지 않는다.
 * 도형 윤곽·축척 막대·북쪽 표시를 그리는 최종 모양은 iOS (a)안 확정 뒤에 맞춘다. 그때까지는 밝은 바탕만 둔다.
 */
@Composable
internal fun ShapeShareNoMapArea(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(ShapeShareNoMapBackground))
}
