package com.ScienceFiction.DronePassAndroid.feature.sketch

import org.junit.Assert.assertEquals
import org.junit.Test

class SketchOverlayColorTest {

    @Test
    fun `스케치 오버레이 색상은 iOS처럼 8자리 hex 의 하위 RGB 에 opacity 를 적용한다`() {
        assertEquals(0x7F123456, parseSketchOverlayColorSafe("#AA123456", opacity = 0.5))
    }

    @Test
    fun `스케치 오버레이 색상 파싱 실패 시 iOS처럼 빨강에 opacity 를 적용한다`() {
        assertEquals(0x66FF0000, parseSketchOverlayColorSafe("not-a-color", opacity = 0.4))
    }

    @Test
    fun `스케치 오버레이 opacity 는 iOS처럼 0에서 255 사이로 제한한다`() {
        assertEquals(0x00FF3B30, parseSketchOverlayColorSafe("#FF3B30", opacity = -1.0))
        assertEquals(0xFFFF3B30.toInt(), parseSketchOverlayColorSafe("#FF3B30", opacity = 2.0))
    }
}
