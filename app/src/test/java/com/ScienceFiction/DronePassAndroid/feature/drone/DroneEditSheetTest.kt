package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneEditSheetTest {

    @Test
    fun `드론 편집 헤더는 iOS inline navigation title처럼 좌우 액션 슬롯 폭을 맞춘다`() {
        assertEquals(44.dp, DroneEditNavigationHeaderHeight)
        assertEquals(88.dp, DroneEditNavigationHeaderSideWidth)
    }

    @Test
    fun `추가 모드 초기 색상은 iOS처럼 추천 색상을 사용한다`() {
        assertEquals(
            PaletteColor.ORANGE,
            resolveInitialDroneEditColor(
                drone = null,
                suggestedColor = PaletteColor.ORANGE,
            ),
        )
    }

    @Test
    fun `편집 모드 초기 색상은 iOS처럼 드론의 팔레트 색상을 사용한다`() {
        assertEquals(
            PaletteColor.GREEN,
            resolveInitialDroneEditColor(
                drone = DroneModel(color = PaletteColor.GREEN.hex),
                suggestedColor = PaletteColor.ORANGE,
            ),
        )
    }

    @Test
    fun `편집 모드에서 드론 색상이 팔레트가 아니면 iOS처럼 파랑으로 초기화한다`() {
        assertEquals(
            PaletteColor.BLUE,
            resolveInitialDroneEditColor(
                drone = DroneModel(color = "#123456"),
                suggestedColor = PaletteColor.ORANGE,
            ),
        )
    }

    @Test
    fun `색상 선택 목록은 iOS ColorPickerGrid처럼 회색을 제외하고 팔레트 순서를 유지한다`() {
        assertEquals(
            listOf(
                PaletteColor.RED,
                PaletteColor.ORANGE,
                PaletteColor.YELLOW,
                PaletteColor.GREEN,
                PaletteColor.TEAL,
                PaletteColor.BLUE,
                PaletteColor.INDIGO,
                PaletteColor.PURPLE,
                PaletteColor.PINK,
            ),
            droneEditSelectableColors(),
        )
    }

    @Test
    fun `저장 버튼은 iOS처럼 공백 제거 후 빈 이름이면 비활성화된다`() {
        assertFalse(canSaveDroneEditName(""))
        assertFalse(canSaveDroneEditName("   "))
        assertTrue(canSaveDroneEditName(" 드론 1 "))
    }

    @Test
    fun `선택 사양 필드는 iOS처럼 공백뿐이면 null이고 입력값은 원문을 보존한다`() {
        assertNull(normalizeDroneEditOptionalField(""))
        assertNull(normalizeDroneEditOptionalField(" \n\t "))
        assertEquals("  SN-01  ", normalizeDroneEditOptionalField("  SN-01  "))
    }

    @Test
    fun `추가 모드 선택 사양 필드는 iOS처럼 공백뿐이면 null 로 저장한다`() {
        assertNull(resolveDroneEditOptionalFieldForSave(value = "", existingValue = null))
        assertNull(resolveDroneEditOptionalFieldForSave(value = " \n\t ", existingValue = null))
        assertEquals("  SN-01  ", resolveDroneEditOptionalFieldForSave(value = "  SN-01  ", existingValue = null))
    }

    @Test
    fun `편집 모드 선택 사양 필드는 iOS처럼 공백으로 기존 값을 지우지 않는다`() {
        assertEquals(
            "SN-OLD",
            resolveDroneEditOptionalFieldForSave(value = "", existingValue = "SN-OLD"),
        )
        assertEquals(
            "memo old",
            resolveDroneEditOptionalFieldForSave(value = " \n\t ", existingValue = "memo old"),
        )
    }

    @Test
    fun `편집 모드 선택 사양 필드는 새 입력값이 있으면 iOS처럼 원문으로 덮어쓴다`() {
        assertEquals(
            "  SN-NEW  ",
            resolveDroneEditOptionalFieldForSave(value = "  SN-NEW  ", existingValue = "SN-OLD"),
        )
    }

    @Test
    fun `메모 입력 영역은 iOS TextEditor처럼 최소 100dp 높이를 가진다`() {
        assertEquals(100.dp, DroneEditMemoMinHeight)
    }

    @Test
    fun `빈 placeholder는 iOS처럼 렌더링하지 않는다`() {
        assertNull(resolveDroneEditPlaceholderText(null))
        assertNull(resolveDroneEditPlaceholderText(""))
        assertEquals(
            "드론 이름",
            resolveDroneEditPlaceholderText("드론 이름"),
        )
    }
}
