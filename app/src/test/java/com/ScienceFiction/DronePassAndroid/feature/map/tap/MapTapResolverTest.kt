package com.ScienceFiction.DronePassAndroid.feature.map.tap

import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.naver.maps.map.overlay.PolygonOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 지도 탭 판정(11/7 대표): ① 선 근처면 가장 가까운 선, ② 아니면 포함하는 것 중 가장 작은 것, 면적이 같으면 위에 그려진 것,
 * ③ 아무것도 아니면 none(지도 강조만 해제). 종류(내 도형·비행구역)별 우선순위는 없다.
 * iOS MapTapResolverTests와 같은 사례다. 공유 fixture는 [MapTapContractTest]가 읽는다.
 */
class MapTapResolverTest {
    private val origin = MapTapLatLng(37.5, 127.0)

    /** 원점에서 동쪽 x m, 북쪽 y m */
    private fun at(x: Double, y: Double) = MapTapLatLng(
        origin.lat + y / MapTapShape.METERS_PER_DEGREE,
        origin.lon + x / (MapTapShape.METERS_PER_DEGREE * cos(origin.lat * PI / 180)),
    )

    private fun t(name: String) = MapTapTarget.Shape(name)

    private fun square(
        target: MapTapTarget,
        cx: Double,
        cy: Double,
        half: Double,
        order: Double = 0.0,
        hole: Double? = null,
    ): MapTapShape {
        val rings = mutableListOf(
            listOf(at(cx - half, cy - half), at(cx + half, cy - half), at(cx + half, cy + half), at(cx - half, cy + half)),
        )
        if (hole != null) {
            rings += listOf(at(cx - hole, cy - hole), at(cx + hole, cy - hole), at(cx + hole, cy + hole), at(cx - hole, cy + hole))
        }
        return requireNotNull(MapTapShape.polygon(target, order, rings))
    }

    private fun circle(target: MapTapTarget, cx: Double, cy: Double, r: Double, order: Double = 0.0) =
        MapTapShape.circle(target, order, at(cx, cy), r)

    private fun resolve(
        shapes: List<MapTapShape>,
        x: Double,
        y: Double,
        tol: Double = 50.0,
        indexed: Boolean = true,
    ): MapTapTarget? = if (indexed) {
        MapTapResolver.resolve(at(x, y), tol, MapTapIndex(shapes)).target
    } else {
        MapTapResolver.resolve(at(x, y), tol, null, shapes).target
    }

    @Test fun `inside smallest wins regardless of kind or draw order`() {
        // 큰 사각형(10km)이 위에 그려져 있어도 안쪽 작은 사각형·원이 열린다
        val shapes = listOf(
            square(t("BIG"), 0.0, 0.0, 5000.0, order = 10.0),
            square(t("ZONE"), 2000.0, 0.0, 800.0, order = -10.0),
            circle(t("MINE"), -2000.0, 0.0, 300.0),
        )
        assertEquals(t("ZONE"), resolve(shapes, 2000.0, 100.0))
        assertEquals(t("MINE"), resolve(shapes, -2000.0, 0.0))
        assertEquals("작은 것 밖, 큰 것 안", t("BIG"), resolve(shapes, 0.0, 3000.0))
        assertNull("아무것도 없음", resolve(shapes, 9000.0, 0.0))
    }

    @Test fun `line beats inside`() {
        // 작은 원 안이지만 큰 사각형의 테두리(x = 5000)에서 20m → 큰 사각형의 선
        val shapes = listOf(square(t("BIG"), 0.0, 0.0, 5000.0), circle(t("SMALL"), 4900.0, 0.0, 200.0))
        assertEquals(t("BIG"), resolve(shapes, 4980.0, 0.0))
        // 선에서 멀어지면(80m) 안쪽 규칙 → 작은 원
        assertEquals(t("SMALL"), resolve(shapes, 4920.0, 0.0))
    }

    @Test fun `closest line wins and a tie goes to the smaller`() {
        val shapes = listOf(square(t("A"), 0.0, 0.0, 1000.0), square(t("B"), 2040.0, 0.0, 1000.0))
        assertEquals("A 선 30m, B 선 10m", t("B"), resolve(shapes, 1030.0, 0.0))
        // 같은 선을 공유(맞닿음) → 거리가 같으니 면적이 작은 것
        val touching = listOf(square(t("A"), 0.0, 0.0, 1000.0), square(t("C"), 1500.0, 0.0, 500.0))
        assertEquals(t("C"), resolve(touching, 1000.0, 300.0))
    }

    @Test fun `equal area goes to the top drawn`() {
        val shapes = listOf(
            square(t("LOW"), 0.0, 0.0, 1000.0, order = 1.0),
            square(t("HIGH"), 0.0, 0.0, 1000.0, order = 2.0),
        )
        assertEquals(t("HIGH"), resolve(shapes, 0.0, 0.0))
        assertEquals(t("HIGH"), resolve(shapes.reversed(), 0.0, 0.0))
    }

    @Test fun `a hole is not inside`() {
        val shapes = listOf(square(t("BIG"), 0.0, 0.0, 5000.0, hole = 2000.0))
        assertNull("구멍 안", resolve(shapes, 0.0, 0.0))
        assertEquals(t("BIG"), resolve(shapes, 3500.0, 0.0))
        assertEquals("구멍 테두리도 선", t("BIG"), resolve(shapes, 2010.0, 0.0))
    }

    @Test fun `circle line and area`() {
        val c = circle(t("SMALL"), 0.0, 0.0, 1000.0)
        assertEquals(PI * 1_000_000, c.areaM2, 1.0)
        assertEquals(30.0, c.measure(at(1030.0, 0.0)).lineM, 0.5)
        assertFalse(c.measure(at(1030.0, 0.0)).inside)
        assertEquals("밖이어도 선 근처", t("SMALL"), resolve(listOf(c), 1030.0, 0.0))
        assertNull(resolve(listOf(c), 1100.0, 0.0))
    }

    @Test fun `index and linear scan agree`() {
        val shapes = (0 until 200).map { i ->
            val x = (i % 20) * 3000.0
            val y = (i / 20) * 3000.0
            if (i % 2 == 0) square(t("S$i"), x, y, 1200.0) else circle(t("C$i"), x, y, 1200.0)
        } + square(t("BIG"), 30_000.0, 15_000.0, 40_000.0) // 여러 칸에 걸침
        listOf(0.0 to 0.0, 1190.0 to 0.0, 3000.0 to 3100.0, 45_000.0 to 20_000.0, 1500.0 to 1500.0).forEach { (x, y) ->
            assertEquals("$x,$y", resolve(shapes, x, y, indexed = false), resolve(shapes, x, y))
        }
    }

    @Test fun `a shape wider than the cell limit is always a candidate`() {
        // 0.05° 칸 4096개를 넘게 덮는 형상은 격자 대신 늘 보는 목록에 들어간다
        val huge = square(t("HUGE"), 0.0, 0.0, 300_000.0)
        assertEquals(t("HUGE"), resolve(listOf(huge), 1000.0, 1000.0))
        assertEquals(t("HUGE"), resolve(listOf(huge), 299_990.0, 0.0))
    }

    @Test fun `draw order grows with what is drawn on top`() {
        // 네이버 지도는 globalZIndex가 큰 것을 위에, 같으면 zIndex가 큰 것을 위에 그린다.
        // 판정의 drawOrder도 같은 방향이어야 한다(위에 그려진 것이 큰 값).
        val zoneBase = PolygonOverlay.DEFAULT_GLOBAL_Z_INDEX
        val prohibited = MapTapResolver.drawOrder(zoneBase, 100 - FlightZoneLayer.PROHIBITED.priority)
        val culturalHeritage = MapTapResolver.drawOrder(zoneBase, 100 - FlightZoneLayer.CULTURAL_HERITAGE.priority)
        val myCircle = MapTapResolver.drawOrder(50, 0)
        val sketch = MapTapResolver.drawOrder(100, 0) // SketchOverlayManager.SKETCH_Z_INDEX

        assertTrue("위험도가 높은 레이어가 위", prohibited > culturalHeritage)
        assertTrue("내 원(50)이 비행구역 위", myCircle > prohibited)
        assertTrue("스케치(100)가 내 원 위", sketch > myCircle)
        // zIndex는 같은 globalZIndex 안에서만 순서를 정한다. |zIndex| < 500이면 다른 층을 넘지 않는다(앱은 0~100만 쓴다)
        assertTrue(MapTapResolver.drawOrder(51, -499) > MapTapResolver.drawOrder(50, 499))

        // 면적이 같으면 그 값이 큰 것(위에 그려진 것)을 연다
        val low = square(t("LOW"), 0.0, 0.0, 1000.0, order = culturalHeritage)
        val high = square(t("HIGH"), 0.0, 0.0, 1000.0, order = prohibited)
        assertEquals(t("HIGH"), resolve(listOf(low, high), 0.0, 0.0))
    }

    /** 탭 1번 1ms 이하 목표: 형상 3,000개, 꼭짓점 200개 다각형 포함(iOS와 같은 구성) */
    @Test fun `resolving a tap is fast`() {
        val shapes = (0 until 3000).map { i ->
            val x = (i % 60) * 5000.0
            val y = (i / 60) * 5000.0
            if (i % 3 == 0) {
                val ring = (0 until 200).map { k ->
                    val a = k / 200.0 * 2 * PI
                    at(x + cos(a) * 2500, y + sin(a) * 2500)
                }
                requireNotNull(MapTapShape.polygon(t("P$i"), 0.0, listOf(ring)))
            } else {
                circle(t("C$i"), x, y, 2000.0)
            }
        }
        val index = MapTapIndex(shapes)
        val taps = 1000
        fun run() = (0 until taps).forEach { i ->
            MapTapResolver.resolve(at((i * 137 % 300_000).toDouble(), (i * 53 % 250_000).toDouble()), 300.0, index)
        }
        run() // JIT 예열
        val start = System.nanoTime()
        run()
        val ms = (System.nanoTime() - start) / 1e6 / taps
        println("MapTapResolver 탭 1번 평균 ${"%.4f".format(ms)}ms (형상 ${shapes.size})")
        assertTrue("탭 1번 ${ms}ms", ms < 1.0)
    }
}
