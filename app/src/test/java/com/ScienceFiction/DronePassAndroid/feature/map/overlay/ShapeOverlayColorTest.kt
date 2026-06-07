package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeOverlayColorTest {

    @Test
    fun `지도 오버레이 색상 파싱 실패 시 iOS처럼 검정색을 사용한다`() {
        assertEquals(0xFF000000.toInt(), parseMapOverlayColorSafe("not-a-color"))
    }

    @Test
    fun `지도 오버레이 색상 파싱 성공 시 원본 hex 색상을 사용한다`() {
        assertEquals(0xFF123456.toInt(), parseMapOverlayColorSafe("#123456"))
    }

    @Test
    fun `지도 오버레이 색상은 iOS처럼 8자리 hex 의 하위 RGB 만 사용한다`() {
        assertEquals(0xFF123456.toInt(), parseMapOverlayColorSafe("#AA123456"))
    }

    @Test
    fun `지도 오버레이 색상은 iOS처럼 짧은 hex 도 스캐너 결과를 사용한다`() {
        assertEquals(0xFF000001.toInt(), parseMapOverlayColorSafe("#1"))
    }
}
