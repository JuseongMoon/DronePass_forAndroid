package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
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

    @Test
    fun `드론 하이라이트가 없는 지도 오버레이 fill alpha는 iOS와 같다`() {
        val now = System.currentTimeMillis()

        assertEquals(
            0x33007AFF,
            calculateMapOverlayFillColor(
                shape = shape(flightStartDate = now + 60_000),
                highlightedDroneIds = emptySet(),
            ),
        )
        assertEquals(
            0x4D007AFF,
            calculateMapOverlayFillColor(
                shape = shape(flightStartDate = now - 60_000, flightEndDate = now + 60_000),
                highlightedDroneIds = emptySet(),
            ),
        )
    }

    @Test
    fun `드론 하이라이트 fill alpha는 iOS와 같다`() {
        val now = System.currentTimeMillis()

        assertEquals(
            0x80007AFF.toInt(),
            calculateMapOverlayFillColor(
                shape = shape(flightStartDate = now + 60_000),
                highlightedDroneIds = setOf("drone-a"),
            ),
        )
        assertEquals(
            0xB3007AFF.toInt(),
            calculateMapOverlayFillColor(
                shape = shape(flightStartDate = now - 60_000, flightEndDate = now + 60_000),
                highlightedDroneIds = setOf("drone-a"),
            ),
        )
    }

    @Test
    fun `드론 하이라이트 outline 색상과 두께는 iOS와 같다`() {
        val now = System.currentTimeMillis()
        val notStarted = shape(flightStartDate = now + 60_000)
        val active = shape(flightStartDate = now - 60_000, flightEndDate = now + 60_000)

        assertEquals(0x80007AFF.toInt(), calculateMapOverlayOutlineColor(notStarted, emptySet()))
        assertEquals(0xFF333333.toInt(), calculateMapOverlayOutlineColor(notStarted, setOf("drone-a")))
        assertEquals(1, calculateMapOverlayOutlineWidth(notStarted))

        assertEquals(0xFF007AFF.toInt(), calculateMapOverlayOutlineColor(active, emptySet()))
        assertEquals(0xFF333333.toInt(), calculateMapOverlayOutlineColor(active, setOf("drone-a")))
        assertEquals(2, calculateMapOverlayOutlineWidth(active))
    }

    @Test
    fun `만료 도형 지도 오버레이는 iOS처럼 system gray 기반 색상을 쓴다`() {
        val now = System.currentTimeMillis()
        val expired = shape(flightStartDate = now - 120_000, flightEndDate = now - 60_000)

        assertEquals(0x4D8E8E93, calculateMapOverlayFillColor(expired, emptySet()))
        assertEquals(0xFF8E8E93.toInt(), calculateMapOverlayOutlineColor(expired, emptySet()))
    }

    @Test
    fun `선 도형 드론 하이라이트 두께는 iOS 원형 outline 강조 두께와 같다`() {
        val active = shape()

        assertEquals(3, calculateMapPolylineWidth(active, emptySet()))
        assertEquals(5, calculateMapPolylineWidth(active, setOf("drone-a")))
    }

    @Test
    fun `선택 도형 포커스 하이라이트 색상은 iOS system red 이다`() {
        assertEquals(0xFFFF3B30.toInt(), parseMapOverlayColorSafe(MapOverlayFocusHighlightHex))
    }

    private fun shape(
        flightStartDate: Long = System.currentTimeMillis() - 60_000,
        flightEndDate: Long? = System.currentTimeMillis() + 60_000,
    ): ShapeModel {
        return ShapeModel(
            color = "#007AFF",
            droneId = "drone-a",
            flightStartDate = flightStartDate,
            flightEndDate = flightEndDate,
        )
    }
}
