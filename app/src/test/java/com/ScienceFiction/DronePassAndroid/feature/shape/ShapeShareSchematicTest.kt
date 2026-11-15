package com.ScienceFiction.DronePassAndroid.feature.shape

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** 지도 없는 공유 카드(D-71, iOS ShapeShareSchematicTests 와 같은 사례): 구도·배율·회전·색 */
class ShapeShareSchematicTest {
    private val width = ShapeShareCardWidth.value
    private val height = ShapeShareCardMapHeight.value

    private fun circle(radius: Double?) = ShapeModel(id = "S1", shapeType = ShapeType.CIRCLE, radius = radius)
    private fun layout(radius: Double?, zoom: Double = 1.0) = ShapeShareSchematicLayout(circle(radius), width, height, zoom)

    @Test fun `축척 거리는 한도 안의 가장 큰 1·2·5×10ⁿ 이다`() {
        assertEquals(500.0, ShapeShareSchematicLayout.niceDistance(526.3), 0.0)
        assertEquals(200.0, ShapeShareSchematicLayout.niceDistance(499.0), 0.0)
        assertEquals(1.0, ShapeShareSchematicLayout.niceDistance(1.9), 0.0)
        assertEquals(10.0, ShapeShareSchematicLayout.niceDistance(10.0), 0.0)
        assertEquals(50_000.0, ShapeShareSchematicLayout.niceDistance(52_631.0), 0.0)
    }

    @Test fun `원은 칸 높이에 맞고 축척 막대는 반경 비율과 같다`() {
        for (radius in listOf(3.0, 100.0, 500.0, 1234.0, 30_000.0)) {
            for (zoom in listOf(1.0, 0.125, 0.5, 2.0, 4.0)) {
                val layout = layout(radius, zoom)
                val pointRadius = requireNotNull(layout.circleRadius)
                assertEquals("$radius x$zoom", ((height / 2 - ShapeShareSchematicLayout.FIT_PADDING) * zoom).toFloat(), pointRadius, 1e-4f)
                val meters = requireNotNull(layout.scaleMeters)
                val length = requireNotNull(layout.scaleBarLength)
                assertEquals("$radius x$zoom", meters / radius, (length / pointRadius).toDouble(), 1e-6)
                assertTrue(length <= ShapeShareSchematicLayout.MAX_SCALE_BAR_LENGTH + 1e-4f)
                assertTrue(length >= ShapeShareSchematicLayout.MAX_SCALE_BAR_LENGTH / 2.5f - 1e-4f)
            }
        }
    }

    @Test fun `격자는 40 이상이 되는 축척 거리의 가장 작은 정수배다`() {
        for (radius in listOf(3.0, 30.0, 100.0, 500.0, 1234.0, 5000.0, 30_000.0)) {
            for (zoom in listOf(1.0, 0.125, 0.5, 2.0, 4.0)) {
                val layout = layout(radius, zoom)
                val length = requireNotNull(layout.scaleBarLength)
                val multiple = requireNotNull(layout.gridMultiple)
                val spacing = requireNotNull(layout.gridSpacing)
                assertEquals(length * multiple, spacing, 1e-4f)
                assertTrue("$radius x$zoom", spacing >= ShapeShareSchematicLayout.MIN_GRID_SPACING - 1e-4f)
                if (multiple > 1) assertTrue("$radius x$zoom", length * (multiple - 1) < ShapeShareSchematicLayout.MIN_GRID_SPACING)
                assertTrue(layout.isRotatable)
            }
        }
    }

    @Test fun `배율을 바꾸면 축척 값을 다시 고른다`() {
        // 반경 500 m: 1배 200 m, 2배 100 m, 1/2배 500 m
        assertEquals(200.0, layout(500.0).scaleMeters!!, 0.0)
        assertEquals(100.0, layout(500.0, 2.0).scaleMeters!!, 0.0)
        assertEquals(500.0, layout(500.0, 0.5).scaleMeters!!, 0.0)
    }

    @Test fun `배율은 1-8배에서 4배 사이이고 1배 근처 5% 안은 1배로 맞춘다`() {
        assertEquals(0.125, ShapeShareSchematicLayout.clampedZoom(0.01), 0.0)
        assertEquals(4.0, ShapeShareSchematicLayout.clampedZoom(10.0), 0.0)
        assertEquals(1.0, ShapeShareSchematicLayout.clampedZoom(Double.NaN), 0.0)
        assertEquals(1.0, ShapeShareSchematicLayout.clampedZoom(-2.0), 0.0)
        assertEquals(1.0, ShapeShareSchematicLayout.clampedZoom(0.0), 0.0)
        assertEquals(1.0, ShapeShareSchematicLayout.settledZoom(1.04), 0.0)
        assertEquals(1.0, ShapeShareSchematicLayout.settledZoom(0.951), 0.0)
        assertEquals(0.5, ShapeShareSchematicLayout.settledZoom(0.5), 0.0)
        assertEquals(0.125, ShapeShareSchematicLayout.settledZoom(0.1), 0.0)
        assertEquals(1.06, ShapeShareSchematicLayout.settledZoom(1.06), 0.0)
        assertEquals(0.94, ShapeShareSchematicLayout.settledZoom(0.94), 0.0)
        assertEquals(4.0, ShapeShareSchematicLayout.settledZoom(5.0), 0.0)
        // 범위 밖 배율로 만들어도 범위 안으로 그린다
        assertEquals(layout(500.0, 4.0).circleRadius, layout(500.0, 9.0).circleRadius)
    }

    @Test fun `북쪽에서 5도 안이면 북쪽 위로 맞춘다`() {
        val snap = ShapeShareSchematicLayout::snappedDegrees
        assertEquals(0.0, snap(0.0), 0.0)
        assertEquals(0.0, snap(4.9), 0.0)
        assertEquals(0.0, snap(-4.9), 0.0)
        assertEquals(0.0, snap(355.5), 0.0)
        assertEquals(0.0, snap(720.0), 0.0)
        assertEquals(5.0, snap(5.0), 0.0)
        assertEquals(330.0, snap(-30.0), 0.0)
        assertEquals(135.0, snap(495.0), 0.0)
        assertEquals(0.0, snap(Double.NaN), 0.0)
        assertFalse(ShapeShareSchematicLayout.showsCompass(3.0))
        assertFalse(ShapeShareSchematicLayout.showsCompass(360.0))
        assertTrue(ShapeShareSchematicLayout.showsCompass(30.0))
    }

    @Test fun `손을 떼면 같은 방향의 가까운 각도로 맞춘다`() {
        assertEquals(360.0, ShapeShareSchematicLayout.settledDegrees(357.0), 0.0)
        assertEquals(0.0, ShapeShareSchematicLayout.settledDegrees(-3.0), 0.0)
        assertEquals(390.0, ShapeShareSchematicLayout.settledDegrees(390.0), 0.0)
        assertEquals(-30.0, ShapeShareSchematicLayout.settledDegrees(-30.0), 0.0)
        assertEquals(360.0, ShapeShareSchematicLayout.northUpDegrees(300.0), 0.0)
        assertEquals(0.0, ShapeShareSchematicLayout.northUpDegrees(100.0), 0.0)
        assertEquals(-360.0, ShapeShareSchematicLayout.northUpDegrees(-200.0), 0.0)
    }

    @Test fun `색은 배경 밝기를 따른다`() {
        val light = ShapeShareSchematicPalette(ShapeShareSchematicPalette.lightBackground)
        val dark = ShapeShareSchematicPalette(ShapeShareSchematicPalette.darkBackground)
        assertEquals(ShapeShareSchematicPalette.Tone.LIGHT, light.tone)
        assertEquals(ShapeShareSchematicPalette.Tone.DARK, dark.tone)
        for (palette in listOf(light, dark)) {
            assertTrue(contrast(palette.ink, palette.background) > 4.5)
            assertTrue(contrast(palette.ink, palette.halo) > 4.5)
            assertTrue(palette.southNeedle.luminance > palette.northNeedle.luminance * 1.8)
            assertTrue(palette.southNeedle.b > palette.southNeedle.r)
        }
        // 나침반 바늘은 확정 색(빨강 230,56,54 / 옅은 청회색 168,189,214)
        assertEquals(listOf(230, 56, 54), light.northNeedle.rgb255())
        assertEquals(listOf(168, 189, 214), light.southNeedle.rgb255())
    }

    private fun ShapeShareSchematicPalette.Rgb.rgb255() = listOf(r, g, b).map { Math.round(it * 255).toInt() }

    private fun contrast(a: ShapeShareSchematicPalette.Rgb, b: ShapeShareSchematicPalette.Rgb): Double {
        val hi = maxOf(a.luminance, b.luminance)
        val lo = minOf(a.luminance, b.luminance)
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test fun `원이 아니거나 반경이 없으면 축척·회전·배율 없는 점이다`() {
        val polygon = circle(500.0).copy(shapeType = ShapeType.POLYGON)
        for (shape in listOf(polygon, circle(null), circle(0.0))) {
            for (zoom in listOf(1.0, 2.0)) {
                val layout = ShapeShareSchematicLayout(shape, width, height, zoom)
                assertNull(layout.circleRadius)
                assertNull(layout.scaleMeters)
                assertNull(layout.scaleBarLength)
                assertNull(layout.gridSpacing)
                assertFalse(layout.isRotatable)
            }
        }
        assertNotNull(layout(500.0).circleRadius)
    }

    @Test fun `축척 표기`() {
        val ko = Locale.KOREA
        assertEquals("500 m", ShapeShareSchematicLayout.distanceText(500.0, ko))
        assertEquals("1 km", ShapeShareSchematicLayout.distanceText(1000.0, ko))
        assertEquals("20 km", ShapeShareSchematicLayout.distanceText(20_000.0, ko))
        assertEquals("0.5 m", ShapeShareSchematicLayout.distanceText(0.5, ko))
    }
}
