package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneListScreenTest {

    @Test
    fun `드론 추가 행은 iOS plus circle 행 토큰을 따른다`() {
        assertEquals(20.dp, DroneListAddIconSize)
        assertEquals(8.dp, DroneListAddIconTextSpacing)
    }

    @Test
    fun `빈 드론 목록 안내는 iOS section footer 처럼 작은 보조 텍스트 여백을 따른다`() {
        assertEquals(16.dp, DroneListEmptyFooterHorizontalPadding)
        assertEquals(8.dp, DroneListEmptyFooterVerticalPadding)
    }

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

    @Test
    fun `드론 상세 placeholder 는 iOS처럼 nil 값에만 적용한다`() {
        assertEquals("미입력", droneDetailOptionalText(null, emptyFallback = "미입력"))
        assertEquals("", droneDetailOptionalText("", emptyFallback = "미입력"))
        assertEquals(" ", droneDetailOptionalText(" ", emptyFallback = "미입력"))
        assertTrue(isDroneDetailPlaceholder(null))
        assertFalse(isDroneDetailPlaceholder(""))
        assertFalse(isDroneDetailPlaceholder(" "))
    }

    @Test
    fun `드론 상세 복사는 iOS CopyableTextModifier 처럼 nil 과 빈 문자열을 무시한다`() {
        assertNull(copyableDroneDetailText(null))
        assertNull(copyableDroneDetailText(""))
        assertEquals(" ", copyableDroneDetailText(" "))
        assertEquals("SN-01", copyableDroneDetailText("SN-01"))
    }
}
