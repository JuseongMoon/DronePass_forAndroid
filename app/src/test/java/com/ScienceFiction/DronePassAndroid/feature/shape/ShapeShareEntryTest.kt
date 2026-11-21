package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D-73: 3.7.0 은 공유 카드 입구를 숨긴다. 지도 허락 뒤 상수만 true 로 바꾼다. */
class ShapeShareEntryTest {

    @Test fun `3_7_0 출시 빌드는 공유 입구를 숨긴다`() {
        assertFalse(ShapeShareEntry.isEnabledInRelease)
        assertFalse(ShapeShareEntry.isVisible(debugForce = ""))
    }

    @Test fun `개발 빌드 강제 값이 상수보다 앞선다`() {
        assertTrue(ShapeShareEntry.isVisible(debugForce = "on", enabledInRelease = false))
        assertFalse(ShapeShareEntry.isVisible(debugForce = "off", enabledInRelease = true))
        assertTrue(ShapeShareEntry.isVisible(debugForce = "", enabledInRelease = true))
        assertFalse(ShapeShareEntry.isVisible(debugForce = "yes", enabledInRelease = false))
    }
}
