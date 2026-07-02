package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShapeOverlayRenderTest {

    @Test
    fun `지도 오버레이는 iOS MapViewModel처럼 원형 도형만 렌더링한다`() {
        val circle = ShapeModel(
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 100.0,
        )
        val rectangle = ShapeModel(
            shapeType = ShapeType.RECTANGLE,
            secondCoordinate = Coordinate(37.1, 127.1),
        )
        val polygon = ShapeModel(
            shapeType = ShapeType.POLYGON,
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.1),
                Coordinate(37.1, 127.1),
            ),
        )
        val polyline = ShapeModel(
            shapeType = ShapeType.POLYLINE,
            polylineCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.1, 127.1),
            ),
        )

        assertEquals(true, shouldRenderMapShapeOverlay(circle))
        assertEquals(listOf(circle.baseCoordinate), shapeOverlayCoordinates(circle))
        assertEquals(false, shouldRenderMapShapeOverlay(rectangle))
        assertEquals(false, shouldRenderMapShapeOverlay(polygon))
        assertEquals(false, shouldRenderMapShapeOverlay(polyline))
        assertNull(shapeOverlayCoordinates(rectangle))
        assertNull(shapeOverlayCoordinates(polygon))
        assertNull(shapeOverlayCoordinates(polyline))
    }

    @Test
    fun `원형 도형이어도 반경이 없으면 지도 오버레이를 만들지 않는다`() {
        val shape = ShapeModel(
            shapeType = ShapeType.CIRCLE,
            radius = null,
        )

        assertEquals(false, shouldRenderMapCircleOverlay(shape))
        assertEquals(false, shouldRenderMapShapeOverlay(shape))
        assertNull(shapeOverlayCoordinates(shape))
    }

    @Test
    fun `원형 도형 하이라이트 반경은 iOS처럼 2미터를 더한다`() {
        val radius = resolveMapCircleHighlightRadius(
            ShapeModel(
                shapeType = ShapeType.CIRCLE,
                radius = 100.0,
            ),
        )

        assertEquals(102.0, radius ?: -1.0, 0.0)
    }

    @Test
    fun `선택 하이라이트는 iOS updateHighlight처럼 shapeType이 아니라 radius 기준으로 만든다`() {
        val radius = resolveMapFocusHighlightRadius(
            ShapeModel(
                shapeType = ShapeType.POLYGON,
                radius = 100.0,
            ),
        )

        assertEquals(102.0, radius ?: -1.0, 0.0)
        assertNull(
            resolveMapCircleHighlightRadius(
                ShapeModel(
                    shapeType = ShapeType.POLYGON,
                    radius = 100.0,
                ),
            ),
        )
    }

    @Test
    fun `반경 없는 원형 도형은 지도 하이라이트를 만들지 않는다`() {
        assertNull(
            resolveMapCircleHighlightRadius(
                ShapeModel(
                    shapeType = ShapeType.CIRCLE,
                    radius = null,
                ),
            ),
        )
        assertNull(
            resolveMapFocusHighlightRadius(
                ShapeModel(
                    shapeType = ShapeType.CIRCLE,
                    radius = null,
                ),
            ),
        )
    }

    @Test
    fun `중복 ID 도형은 iOS처럼 첫 도형만 지도 오버레이 후보로 사용한다`() {
        val shapes = uniqueMapOverlayShapesByFirstId(
            listOf(
                ShapeModel(
                    id = "shape-a",
                    shapeType = ShapeType.POLYGON,
                    radius = 100.0,
                ),
                ShapeModel(
                    id = "shape-a",
                    shapeType = ShapeType.CIRCLE,
                    radius = 100.0,
                ),
            ),
        )

        assertEquals(1, shapes.size)
        assertEquals(ShapeType.POLYGON, shapes.first().shapeType)
        assertEquals(false, shouldRenderMapShapeOverlay(shapes.first()))
    }
}
