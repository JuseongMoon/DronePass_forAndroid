package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaletteColorTest {

    @Test
    fun `팔레트 색상 raw value 는 iOS PaletteColor hex 와 동일하다`() {
        assertEquals(
            listOf(
                "#FF3B30",
                "#FF9500",
                "#FFCC00",
                "#34C759",
                "#5AC8FA",
                "#007AFF",
                "#5856D6",
                "#AF52DE",
                "#FF2D55",
                "#8E8E93",
            ),
            PaletteColor.entries.map { it.hex },
        )
    }

    @Test
    fun `드론 선택 가능 팔레트는 iOS처럼 회색을 제외한 순서를 유지한다`() {
        assertEquals(
            PaletteColor.entries.filter { it != PaletteColor.GRAY },
            PaletteColor.droneSelectableEntries,
        )
    }

    @Test
    fun `hex 파싱은 iOS처럼 대소문자를 무시한다`() {
        assertEquals(PaletteColor.BLUE, PaletteColor.fromHex("#007AFF"))
        assertEquals(PaletteColor.BLUE, PaletteColor.fromHex("#007aff"))
        assertNull(PaletteColor.fromHex("007AFF"))
    }
}
