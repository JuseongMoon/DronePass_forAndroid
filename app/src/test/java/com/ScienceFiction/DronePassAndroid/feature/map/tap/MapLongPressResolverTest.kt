package com.ScienceFiction.DronePassAndroid.feature.map.tap

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

/**
 * 지도 길게 누르기 판정(D-83 (2)): 스케치 모드는 무시, 지금 지도에 그려진 내 도형만 놓고 탭 판정과 같은 규칙으로
 * 도형을 고르면 그 도형 메뉴, 없으면 새 도형 흐름. 비행구역·숨긴 도형은 후보에서 뺀다.
 */
class MapLongPressResolverTest {
    private val origin = MapTapLatLng(37.5, 127.0)

    /** 원점에서 동쪽 x m, 북쪽 y m */
    private fun at(x: Double, y: Double) = MapTapLatLng(
        origin.lat + y / MapTapShape.METERS_PER_DEGREE,
        origin.lon + x / (MapTapShape.METERS_PER_DEGREE * cos(origin.lat * PI / 180)),
    )

    private fun circle(id: String, cx: Double, cy: Double, r: Double, order: Double = 0.0) =
        MapTapShape.circle(MapTapTarget.Shape(id), order, at(cx, cy), r)

    private fun zoneSquare(key: String, cx: Double, cy: Double, half: Double) = requireNotNull(
        MapTapShape.polygon(
            MapTapTarget.Zone(key),
            0.0,
            listOf(listOf(at(cx - half, cy - half), at(cx + half, cy - half), at(cx + half, cy + half), at(cx - half, cy + half))),
        ),
    )

    private fun drawn(vararg shapes: MapTapShape) = shapes.map { MapLongPressCandidate(it, isDrawn = true) }

    private fun press(
        candidates: List<MapLongPressCandidate>,
        x: Double,
        y: Double,
        sketch: Boolean = false,
        tol: Double = 12.0,
    ) = MapLongPressResolver.resolve(at(x, y), tol, candidates, isSketchMode = sketch)

    private fun menu(id: String, reason: MapTapResult.Reason) = MapLongPressAction.ShapeMenu(id, reason)

    @Test fun `도형 안을 길게 누르면 그 도형 메뉴`() {
        assertEquals(menu("A", MapTapResult.Reason.INSIDE), press(drawn(circle("A", 0.0, 0.0, 100.0)), 10.0, 0.0))
    }

    @Test fun `도형 테두리 12pt 안이면 바깥이어도 그 도형 메뉴(탭과 같은 선 판정)`() {
        val shapes = drawn(circle("A", 0.0, 0.0, 100.0))
        assertEquals(menu("A", MapTapResult.Reason.LINE), press(shapes, 108.0, 0.0))
        assertEquals(menu("A", MapTapResult.Reason.LINE), press(shapes, 95.0, 0.0))
    }

    @Test fun `빈 곳은 지금처럼 새 도형`() {
        assertEquals(MapLongPressAction.NewShape, press(drawn(circle("A", 0.0, 0.0, 100.0)), 200.0, 0.0))
        assertEquals(MapLongPressAction.NewShape, press(emptyList(), 0.0, 0.0))
    }

    @Test fun `스케치 모드에서는 도형 위든 빈 곳이든 아무것도 하지 않는다`() {
        val shapes = drawn(circle("A", 0.0, 0.0, 100.0))
        assertEquals(MapLongPressAction.Ignore, press(shapes, 0.0, 0.0, sketch = true))
        assertEquals(MapLongPressAction.Ignore, press(shapes, 500.0, 0.0, sketch = true))
        assertEquals(MapLongPressAction.Ignore, press(emptyList(), 0.0, 0.0, sketch = true))
    }

    @Test fun `겹친 곳은 탭 판정과 같이 안쪽 중 가장 작은 도형`() {
        val shapes = drawn(circle("BIG", 0.0, 0.0, 300.0), circle("SMALL", 40.0, 0.0, 80.0))
        assertEquals(menu("SMALL", MapTapResult.Reason.INSIDE), press(shapes, 40.0, 0.0))
        // 작은 원 밖, 큰 원 안
        assertEquals(menu("BIG", MapTapResult.Reason.INSIDE), press(shapes, -150.0, 0.0))
    }

    @Test fun `겹친 곳에서도 선 근처가 안쪽보다 먼저(탭과 같음)`() {
        // 작은 원 안이지만 큰 원 테두리 5m 옆
        val shapes = drawn(circle("BIG", 0.0, 0.0, 300.0), circle("SMALL", 250.0, 0.0, 80.0))
        assertEquals(menu("BIG", MapTapResult.Reason.LINE), press(shapes, 295.0, 0.0))
    }

    @Test fun `면적이 같으면 위에 그려진 도형`() {
        val shapes = drawn(circle("LOW", 0.0, 0.0, 100.0, order = 1.0), circle("HIGH", 0.0, 0.0, 100.0, order = 2.0))
        assertEquals(menu("HIGH", MapTapResult.Reason.INSIDE), press(shapes, 0.0, 0.0))
    }

    @Test fun `겹친 곳의 답은 같은 도형들로 한 탭 판정과 같다`() {
        val shapes = listOf(
            circle("A", 0.0, 0.0, 300.0),
            circle("B", 120.0, 0.0, 150.0),
            circle("C", 120.0, 40.0, 60.0, order = 3.0),
            circle("D", 120.0, 40.0, 60.0, order = 1.0),
        )
        for (x in -320..320 step 20) {
            for (y in -320..320 step 20) {
                val tap = MapTapResolver.resolve(at(x.toDouble(), y.toDouble()), 12.0, null, shapes)
                val expected = (tap.target as? MapTapTarget.Shape)?.let { menu(it.id, tap.reason) }
                    ?: MapLongPressAction.NewShape
                assertEquals("($x, $y)", expected, press(drawn(*shapes.toTypedArray()), x.toDouble(), y.toDouble()))
            }
        }
    }

    @Test fun `비행구역은 후보가 아니다 - 탭은 구역을 열어도 길게 누르기는 내 도형 메뉴`() {
        val circle = circle("MINE", 0.0, 0.0, 300.0)
        val zone = zoneSquare("ZONE", 0.0, 0.0, 50.0)
        val candidates = drawn(circle, zone)
        // 구역 테두리 근처(내 원 안): 탭은 구역
        assertEquals(MapTapTarget.Zone("ZONE"), MapTapResolver.resolve(at(52.0, 0.0), 12.0, null, listOf(circle, zone)).target)
        assertEquals(menu("MINE", MapTapResult.Reason.INSIDE), press(candidates, 52.0, 0.0))
        // 구역 안쪽(더 작다): 탭은 구역, 길게 누르기는 내 원
        assertEquals(menu("MINE", MapTapResult.Reason.INSIDE), press(candidates, 0.0, 0.0))
    }

    @Test fun `비행구역만 있는 곳은 지금처럼 새 도형`() {
        assertEquals(MapLongPressAction.NewShape, press(drawn(zoneSquare("ZONE", 0.0, 0.0, 50.0)), 0.0, 0.0))
        assertEquals(MapLongPressAction.NewShape, press(drawn(zoneSquare("ZONE", 0.0, 0.0, 50.0)), 50.0, 0.0))
    }

    @Test fun `숨긴 도형은 후보가 아니다`() {
        val visible = MapLongPressCandidate(circle("VISIBLE", 0.0, 0.0, 300.0), isDrawn = true)
        val hidden = MapLongPressCandidate(circle("HIDDEN", 0.0, 0.0, 50.0), isDrawn = false)
        // 숨긴 작은 원이 있어도 보이는 큰 원
        assertEquals(menu("VISIBLE", MapTapResult.Reason.INSIDE), press(listOf(visible, hidden), 0.0, 0.0))
        // 숨긴 원 테두리 근처도 보이는 큰 원
        assertEquals(menu("VISIBLE", MapTapResult.Reason.INSIDE), press(listOf(visible, hidden), 50.0, 0.0))
        // 숨긴 도형만 있으면 새 도형
        assertEquals(MapLongPressAction.NewShape, press(listOf(hidden), 0.0, 0.0))
    }

    @Test fun `결과 이름은 공유 fixture의 action 값과 같다`() {
        assertEquals("ignore", MapLongPressAction.Ignore.raw)
        assertEquals("newShape", MapLongPressAction.NewShape.raw)
        assertEquals("shapeMenu", menu("A", MapTapResult.Reason.LINE).raw)
    }
}
