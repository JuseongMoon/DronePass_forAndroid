package com.ScienceFiction.DronePassAndroid.feature.map.tap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 빈 곳 탭(판정 none)은 지도 강조만 푼다(11/7 대표, 계약 "지도 탭 판정").
 * 도형 하이라이트는 선택(저장 목록·상세 시트 대상)과 별개 상태이고, 비행구역은 외곽선 강조만 푼다.
 */
class MapTapHighlightContractTest {
    private fun source(path: String): String {
        val candidates = listOf("src/main/java/com/ScienceFiction/DronePassAndroid/$path", "app/src/main/java/com/ScienceFiction/DronePassAndroid/$path")
        return candidates.map(::File).first { it.isFile }.readText()
    }

    private fun body(source: String, signature: String): String {
        val start = source.indexOf(signature)
        check(start >= 0) { "missing $signature" }
        var depth = 0
        var i = source.indexOf('{', start)
        val open = i
        while (i < source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return source.substring(open, i + 1)
            }
            i++
        }
        error("unclosed $signature")
    }

    @Test fun `selection always sets the map highlight and an empty tap clears only the highlight`() {
        val vm = source("feature/map/MapViewModel.kt")
        // 선택을 바꾸는 곳은 모두 하이라이트도 함께 정하는 setSelectedShape를 거친다
        assertEquals(1, Regex("""_selectedShapeId\.value = """).findAll(vm).count())
        // 도형 id로 강조하는 곳은 선택(setSelectedShape)과 길게 누르기 메뉴(D-83 (2)) 두 곳뿐이다
        assertEquals(2, Regex("""_highlightedShapeId\.value = shapeId""").findAll(vm).count())
        assertTrue(body(vm, "private fun setSelectedShape(").contains("_highlightedShapeId.value = shapeId"))
        // 길게 누르기는 선택 없이 강조만 하고(목록·시트는 그대로), 메뉴를 닫으면 누르기 전 강조로 돌린다
        val longPress = body(vm, "fun onShapeLongPressed(")
        assertTrue(longPress.contains("_highlightedShapeId.value = shapeId"))
        assertTrue(longPress.contains("highlightBeforeShapeLongPress = _highlightedShapeId.value"))
        assertFalse(longPress.contains("_selectedShapeId.value"))
        assertFalse(longPress.contains("_showShapeDetail") || longPress.contains("_showShapeEdit"))

        val clear = body(vm, "fun clearMapTapHighlight()")
        assertTrue(clear.contains("_highlightedShapeId.value = null"))
        assertFalse("목록 선택은 그대로", clear.contains("_selectedShapeId"))
        assertFalse("열린 시트는 그대로", clear.contains("_showShapeDetail") || clear.contains("_showShapeEdit"))
    }

    @Test fun `the map draws the highlight from the separate highlight state`() {
        val layers = source("feature/map/MapScreenLayers.kt")
        assertTrue(layers.contains("overlayManager.setHighlight(highlightedShapeId, filteredShapes)"))
        assertFalse(layers.contains("setHighlight(selectedShapeId"))
        assertTrue(source("feature/map/MapScreen.kt").contains("overlayManager.onTapHighlightCleared = { viewModel.clearMapTapHighlight() }"))
    }

    @Test fun `an empty tap clears only the zone outline and overlays take no clicks`() {
        val zones = source("feature/vworld/FlightZoneOverlayManager.kt")
        assertTrue(body(zones, "override fun clearTapHighlight()").contains("clearSelection()"))
        assertFalse(zones.contains("setOnClickListener"))
        assertFalse(source("feature/map/overlay/ShapeOverlayManager.kt").contains("setOnClickListener"))
        assertTrue(source("feature/map/MapScreen.kt").contains("map.setOnMapClickListener"))
    }
}
