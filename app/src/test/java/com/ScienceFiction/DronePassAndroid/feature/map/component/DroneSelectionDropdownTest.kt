package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
        assertEquals(DroneDropdownTriggerDiameter, DroneDropdownSelectionButtonHeight)
        assertEquals(DroneDropdownTriggerDiameter, DroneDropdownTriggerSize)
    }

    @Test
    fun `드론 선택 버튼과 오른쪽 드롭다운 원은 같은 상단 정렬을 쓴다`() {
        assertEquals(Alignment.Top, DroneDropdownControlVerticalAlignment)
    }
}
