package com.ScienceFiction.DronePassAndroid.feature.map.overlay

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShapeOverlayRenderTest {

    @Test
    fun `지도 오버레이는 iOS처럼 원형 도형만 렌더링한다`() {
        assertEquals(
            true,
            shouldRenderMapCircleOverlay(
                ShapeModel(
                    shapeType = ShapeType.CIRCLE,
                    radius = 100.0,
                ),
            ),
        )
        assertEquals(
            false,
            shouldRenderMapCircleOverlay(
                ShapeModel(
                    shapeType = ShapeType.POLYGON,
                    radius = 100.0,
                ),
            ),
        )
    }

    @Test
    fun `원형 도형이어도 반경이 없으면 iOS처럼 지도 오버레이를 만들지 않는다`() {
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
        assertEquals(false, shouldRenderMapCircleOverlay(shapes.first()))
    }
}
