package com.ScienceFiction.DronePassAndroid.feature.drone

import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneListScreenTest {

    @Test
    fun `드론 목록 색상 원은 iOS처럼 팔레트 색상이 있을 때만 표시한다`() {
        assertTrue(shouldShowDroneListColorIndicator(PaletteColor.BLUE))
        assertFalse(shouldShowDroneListColorIndicator(null))
    }

    @Test
    fun `드론 삭제 실패 메시지는 iOS처럼 원문을 보존한다`() {
        assertEquals("network", droneDeleteFailureMessage("network"))
        assertEquals("", droneDeleteFailureMessage(""))
        assertEquals(" ", droneDeleteFailureMessage(" "))
    }
}
