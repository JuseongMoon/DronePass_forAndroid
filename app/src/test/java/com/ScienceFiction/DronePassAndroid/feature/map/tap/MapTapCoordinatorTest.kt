package com.ScienceFiction.DronePassAndroid.feature.map.tap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

/** 결과 반영(iOS MapTapContractTests의 MapTapCoordinator 사례와 같다) */
class MapTapCoordinatorTest {
    private class FakeSource(
        override val tapIndexed: Boolean,
        override var tapShapes: List<MapTapShape>,
    ) : MapTapSource {
        override var tapSignature = 0
        val opened = mutableListOf<MapTapTarget>()
        var cleared = 0

        override fun openTapped(target: MapTapTarget): Boolean {
            if (tapShapes.none { it.target == target }) return false
            opened += target
            return true
        }

        override fun clearTapHighlight() {
            cleared++
        }
    }

    private val p = MapTapLatLng(37.5, 127.0)

    @Test fun `the owner opens its target and an empty tap only clears highlights`() {
        val circleTarget = MapTapTarget.Shape("circle")
        val zoneTarget = MapTapTarget.Zone("zone")
        val circles = FakeSource(false, listOf(MapTapShape.circle(circleTarget, 0.0, p, 100.0)))
        val zones = FakeSource(true, listOf(MapTapShape.circle(zoneTarget, 0.0, p, 5000.0)))
        val coordinator = MapTapCoordinator()
        coordinator.register(circles)
        coordinator.register(zones)
        coordinator.register(zones) // 두 번 등록해도 한 번

        // 작은 원 안 → 원 소스만 연다
        val inside = coordinator.resolve(p, 12.0)
        assertEquals(MapTapResult(circleTarget, MapTapResult.Reason.INSIDE), inside)
        coordinator.apply(inside)
        assertEquals(listOf<MapTapTarget>(circleTarget), circles.opened)
        assertTrue(zones.opened.isEmpty())

        // 둘 다 밖(약 11km 북쪽) → none: 모든 소스의 강조만 푼다(연 것 없음)
        val far = coordinator.resolve(MapTapLatLng(37.6, 127.0), 12.0)
        assertEquals(MapTapResult.None, far)
        coordinator.apply(far)
        assertEquals(1, circles.cleared)
        assertEquals("중복 등록이 없어 한 번", 1, zones.cleared)
        assertEquals(1, circles.opened.size + zones.opened.size)
    }

    @Test fun `the zone index is rebuilt only when the signature changes`() {
        val zones = FakeSource(true, listOf(MapTapShape.circle(MapTapTarget.Zone("a"), 0.0, p, 100.0)))
        val coordinator = MapTapCoordinator()
        coordinator.register(zones)
        val rebuilt = mutableListOf<Boolean>()
        coordinator.onResolved = { rebuilt += it.rebuilt }

        coordinator.resolve(p, 12.0)
        coordinator.resolve(p, 12.0)
        // 레이어를 끄면(보이는 것이 바뀌면) 서명이 바뀌어 다시 만든다 → 후보에서 빠진다
        zones.tapShapes = emptyList()
        zones.tapSignature++
        val afterHide = coordinator.resolve(p, 12.0)

        assertEquals(listOf(true, false, true), rebuilt)
        assertEquals(MapTapResult.None, afterHide)
    }

    @Test fun `tolerance converts the 12dp screen offset to meters`() {
        // 화면 12dp가 동쪽으로 30m인 줌
        val east30m = MapTapLatLng(p.lat, p.lon + 30 / (MapTapShape.METERS_PER_DEGREE * cos(p.lat * PI / 180)))
        assertEquals(30.0, MapTapCoordinator.toleranceMeters(p, east30m), 1e-6)
    }
}
