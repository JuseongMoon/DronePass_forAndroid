package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShapeTypeTest {

    @Test
    fun `Firestore 저장값은 iOS raw value 와 동일한 소문자이다`() {
        assertEquals("circle", ShapeType.CIRCLE.rawValue)
        assertEquals("rectangle", ShapeType.RECTANGLE.rawValue)
        assertEquals("polygon", ShapeType.POLYGON.rawValue)
        assertEquals("polyline", ShapeType.POLYLINE.rawValue)
    }

    @Test
    fun `한국어 표시명은 iOS ShapeType koreanName 과 동일하다`() {
        assertEquals("원", ShapeType.CIRCLE.koreanName)
        assertEquals("사각형", ShapeType.RECTANGLE.koreanName)
        assertEquals("다각형", ShapeType.POLYGON.koreanName)
        assertEquals("선", ShapeType.POLYLINE.koreanName)
    }

    @Test
    fun `iOS raw value 를 파싱한다`() {
        assertEquals(ShapeType.CIRCLE, ShapeType.fromWireValue("circle"))
        assertEquals(ShapeType.RECTANGLE, ShapeType.fromWireValue("rectangle"))
        assertEquals(ShapeType.POLYGON, ShapeType.fromWireValue("polygon"))
        assertEquals(ShapeType.POLYLINE, ShapeType.fromWireValue("polyline"))
    }

    @Test
    fun `Android 레거시 enum name 도 파싱한다`() {
        assertEquals(ShapeType.CIRCLE, ShapeType.fromWireValue("CIRCLE"))
        assertEquals(ShapeType.RECTANGLE, ShapeType.fromWireValue("RECTANGLE"))
        assertEquals(ShapeType.POLYGON, ShapeType.fromWireValue("POLYGON"))
        assertEquals(ShapeType.POLYLINE, ShapeType.fromWireValue("POLYLINE"))
    }

    @Test
    fun `알 수 없는 shapeType 은 원형으로 폴백한다`() {
        assertEquals(ShapeType.CIRCLE, ShapeType.fromWireValue(null))
        assertEquals(ShapeType.CIRCLE, ShapeType.fromWireValue(""))
        assertEquals(ShapeType.CIRCLE, ShapeType.fromWireValue("unknown"))
    }

    @Test
    fun `Firestore 필수 파싱 경로에서는 알 수 없는 shapeType 을 invalid 로 구분한다`() {
        assertNull(ShapeType.parseWireValue(null))
        assertNull(ShapeType.parseWireValue(""))
        assertNull(ShapeType.parseWireValue("unknown"))
        assertEquals(ShapeType.CIRCLE, ShapeType.parseWireValue("circle"))
        assertEquals(ShapeType.CIRCLE, ShapeType.parseWireValue("CIRCLE"))
    }
}
