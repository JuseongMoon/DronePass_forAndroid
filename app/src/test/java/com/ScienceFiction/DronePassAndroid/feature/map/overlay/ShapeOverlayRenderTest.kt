package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShapeOverlayRenderTest {

    @Test
    fun `지도 오버레이는 계약 도형 타입을 렌더링한다`() {
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

        assertEquals(
            true,
            shouldRenderMapShapeOverlay(
                ShapeModel(
                    shapeType = ShapeType.CIRCLE,
                    radius = 100.0,
                ),
            ),
        )
        assertEquals(true, shouldRenderMapShapeOverlay(rectangle))
        assertEquals(true, shouldRenderMapShapeOverlay(polygon))
        assertEquals(true, shouldRenderMapShapeOverlay(polyline))
    }

    @Test
    fun `원형 도형이어도 반경이 없으면 지도 오버레이를 만들지 않는다`() {
        assertEquals(
            false,
            shouldRenderMapCircleOverlay(
                ShapeModel(
                    shapeType = ShapeType.CIRCLE,
                    radius = null,
                ),
            ),
        )
    }

    @Test
    fun `좌표가 부족한 계약 도형은 지도 오버레이를 만들지 않는다`() {
        assertEquals(
            false,
            shouldRenderMapShapeOverlay(
                ShapeModel(
                    shapeType = ShapeType.RECTANGLE,
                    secondCoordinate = null,
                ),
            ),
        )
        assertEquals(
            false,
            shouldRenderMapShapeOverlay(
                ShapeModel(
                    shapeType = ShapeType.POLYGON,
                    polygonCoordinates = listOf(
                        Coordinate(37.0, 127.0),
                        Coordinate(37.1, 127.1),
                    ),
                ),
            ),
        )
        assertEquals(
            false,
            shouldRenderMapShapeOverlay(
                ShapeModel(
                    shapeType = ShapeType.POLYLINE,
                    polylineCoordinates = listOf(Coordinate(37.0, 127.0)),
                ),
            ),
        )
    }

    @Test
    fun `사각형 지도 오버레이는 두 좌표에서 네 모서리를 만든다`() {
        val shape = ShapeModel(
            shapeType = ShapeType.RECTANGLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            secondCoordinate = Coordinate(37.1, 127.2),
        )

        assertEquals(
            listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.2),
                Coordinate(37.1, 127.2),
                Coordinate(37.1, 127.0),
            ),
            rectangleOverlayCoordinates(shape),
        )
    }

    @Test
    fun `닫힌 도형 하이라이트는 마지막 좌표에 첫 좌표를 추가한다`() {
        val shape = ShapeModel(
            shapeType = ShapeType.POLYGON,
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.1),
                Coordinate(37.1, 127.1),
            ),
        )

        assertEquals(
            listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.1),
                Coordinate(37.1, 127.1),
                Coordinate(37.0, 127.0),
            ),
            resolveMapShapeHighlightCoordinates(shape),
        )
    }

    @Test
    fun `선 도형 하이라이트는 열린 좌표를 그대로 사용한다`() {
        val coordinates = listOf(
            Coordinate(37.0, 127.0),
            Coordinate(37.1, 127.1),
        )
        val shape = ShapeModel(
            shapeType = ShapeType.POLYLINE,
            polylineCoordinates = coordinates,
        )

        assertEquals(coordinates, resolveMapShapeHighlightCoordinates(shape))
    }

    @Test
    fun `지도 하이라이트는 원형 오버레이 후보에만 적용한다`() {
        val radius = resolveMapCircleHighlightRadius(
            ShapeModel(
                shapeType = ShapeType.CIRCLE,
                radius = 100.0,
            ),
        )

        assertEquals(102.0, radius ?: -1.0, 0.0)
    }

    @Test
    fun `비원형 도형은 반경 값이 있어도 지도 하이라이트를 만들지 않는다`() {
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
