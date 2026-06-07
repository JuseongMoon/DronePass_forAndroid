package com.ScienceFiction.DronePassAndroid.feature.drone

import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneListScreenTest {

    @Test
    fun `드론 목록 색상 원은 iOS처럼 팔레트 색상이 있을 때만 표시한다`() {
        assertTrue(shouldShowDroneListColorIndicator(PaletteColor.BLUE))
        assertFalse(shouldShowDroneListColorIndicator(null))
    }
}
