package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneSelectionDropdownTest {

    @Test
    fun `선택된 드론 칩 색상 원은 iOS처럼 팔레트 색상이 있을 때만 표시한다`() {
        assertTrue(shouldShowSelectedDroneChipColorIndicator(PaletteColor.BLUE))
        assertFalse(shouldShowSelectedDroneChipColorIndicator(null))
    }

    @Test
    fun `드롭다운 메뉴 행 padding은 iOS와 동일하다`() {
        assertEquals(12.dp, DroneDropdownMenuItemHorizontalPadding)
        assertEquals(8.dp, DroneDropdownMenuItemVerticalPadding)
    }

    @Test
    fun `드론 드롭다운 shadow는 iOS 칩과 메뉴 shadow radius를 따른다`() {
        assertEquals(4.dp, DroneDropdownShadowElevation)
    }

    @Test
    fun `선택된 드론이 없을 때 iOS처럼 드론 아이콘을 사용한다`() {
        assertEquals(R.drawable.ic_drone, DroneDropdownEmptyIconRes)
        assertEquals(12.dp, DroneDropdownEmptyIconSize)
    }

    @Test
    fun `선택된 드론이 없을 때 텍스트는 iOS primary 색상을 따른다`() {
        assertEquals(Color.Black, droneDropdownEmptyTextColor(Color.Black))
    }

    @Test
    fun `드론 드롭다운 텍스트와 chevron 크기는 iOS MainFloatingButtonView 와 맞춘다`() {
        assertEquals(14.sp, DroneDropdownTextSize)
        assertEquals(10.dp, DroneDropdownChevronIconSize)
    }

    @Test
    fun `드론 선택 버튼 높이는 오른쪽 드롭다운 원 지름과 같다`() {
        assertEquals(32.dp, DroneDropdownTriggerDiameter)
        assertEquals(40.dp, DroneDropdownChevronReservedWidth)
        assertEquals(DroneDropdownTriggerDiameter, DroneDropdownSelectionButtonHeight)
        assertEquals(DroneDropdownTriggerDiameter, DroneDropdownTriggerSize)
    }

    @Test
    fun `드론 선택 버튼과 오른쪽 드롭다운 원은 같은 상단 정렬을 쓴다`() {
        assertEquals(Alignment.Top, DroneDropdownControlVerticalAlignment)
    }

    @Test
    fun `선택 드론 줄바꿈은 iOS처럼 첫 줄만 chevron 공간을 예약한다`() {
        val layout = computeDroneDropdownChipFlowLayout(
            itemSizes = listOf(
                IntSize(width = 100, height = 32),
                IntSize(width = 100, height = 32),
                IntSize(width = 100, height = 32),
            ),
            maxWidth = 220,
            firstLineReservedWidth = 40,
            horizontalSpacing = 8,
            lineSpacing = 4,
        )

        assertEquals(listOf(100, 208), layout.lineWidths)
        assertEquals(
            listOf(
                DroneDropdownChipPlacement(x = 80, y = 0),
                DroneDropdownChipPlacement(x = 12, y = 36),
                DroneDropdownChipPlacement(x = 120, y = 36),
            ),
            layout.placements,
        )
        assertEquals(68, layout.height)
    }

    @Test
    fun `선택 드론 줄바꿈 계산은 빈 목록을 안전하게 처리한다`() {
        val layout = computeDroneDropdownChipFlowLayout(
            itemSizes = emptyList(),
            maxWidth = 220,
            firstLineReservedWidth = 40,
            horizontalSpacing = 8,
            lineSpacing = 4,
        )

        assertEquals(0, layout.height)
        assertEquals(emptyList<Int>(), layout.lineWidths)
        assertEquals(emptyList<DroneDropdownChipPlacement>(), layout.placements)
    }

    @Test
    fun `첫 줄 칩이 예약 폭보다 길어도 iOS처럼 chevron 앞에 오른쪽 끝을 맞춘다`() {
        val layout = computeDroneDropdownChipFlowLayout(
            itemSizes = listOf(IntSize(width = 210, height = 32)),
            maxWidth = 220,
            firstLineReservedWidth = 40,
            horizontalSpacing = 8,
            lineSpacing = 4,
        )

        assertEquals(listOf(210), layout.lineWidths)
        assertEquals(listOf(DroneDropdownChipPlacement(x = -30, y = 0)), layout.placements)
        assertEquals(180, layout.placements.single().x + layout.lineWidths.single())
        assertEquals(32, layout.height)
    }
}
