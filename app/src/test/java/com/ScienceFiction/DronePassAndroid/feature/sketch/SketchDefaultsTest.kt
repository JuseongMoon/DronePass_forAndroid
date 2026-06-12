package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import org.junit.Assert.assertEquals
import org.junit.Test

class SketchDefaultsTest {

    @Test
    fun `스케치 설정 키는 iOS UserDefaults 이름과 동일하게 유지한다`() {
        assertEquals("sketchCurrentColor", SketchPreferenceKeys.CURRENT_COLOR.name)
        assertEquals("sketchCurrentStrokeWidth", SketchPreferenceKeys.CURRENT_STROKE_WIDTH.name)
        assertEquals("sketchCurrentOpacity", SketchPreferenceKeys.CURRENT_OPACITY.name)
    }

    @Test
    fun `펜 굵기는 iOS처럼 1에서 20 사이로 제한한다`() {
        assertEquals(1.0, clampSketchStrokeWidth(-3.0), 0.0)
        assertEquals(4.0, clampSketchStrokeWidth(4.0), 0.0)
        assertEquals(20.0, clampSketchStrokeWidth(50.0), 0.0)
    }

    @Test
    fun `펜 투명도는 iOS처럼 0_1에서 1 사이로 제한한다`() {
        assertEquals(0.1, clampSketchOpacity(0.0), 0.0)
        assertEquals(0.5, clampSketchOpacity(0.5), 0.0)
        assertEquals(1.0, clampSketchOpacity(2.0), 0.0)
    }

    @Test
    fun `스케치 Firestore 색상은 저장 전 RRGGBB hex 로 정규화한다`() {
        assertEquals("#123456", normalizeSketchFirestoreColor("#123456"))
        assertEquals("#123456", normalizeSketchFirestoreColor("#AA123456"))
        assertEquals("#FF0000", normalizeSketchFirestoreColor("not-a-color"))
    }

    @Test
    fun `스케치 모드 재진입은 iOS처럼 이미 활성 상태면 무시한다`() {
        assertEquals(true, shouldEnterSketchMode(isSketchModeActive = false))
        assertEquals(false, shouldEnterSketchMode(isSketchModeActive = true))
    }

    @Test
    fun `스케치 모드 진입은 iOS처럼 기존 지우개 모드를 보존한다`() {
        assertEquals(
            SketchModeTransition(
                shouldTransition = true,
                isSketchModeActive = true,
                isEraserModeActive = true,
            ),
            enterSketchModeTransition(
                isSketchModeActive = false,
                isEraserModeActive = true,
            ),
        )
        assertEquals(
            SketchModeTransition(
                shouldTransition = false,
                isSketchModeActive = true,
                isEraserModeActive = true,
            ),
            enterSketchModeTransition(
                isSketchModeActive = true,
                isEraserModeActive = true,
            ),
        )
    }

    @Test
    fun `스케치 모드 종료는 iOS처럼 기존 지우개 모드를 보존한다`() {
        assertEquals(
            SketchModeTransition(
                shouldTransition = true,
                isSketchModeActive = false,
                isEraserModeActive = true,
            ),
            exitSketchModeTransition(
                isSketchModeActive = true,
                isEraserModeActive = true,
            ),
        )
        assertEquals(
            SketchModeTransition(
                shouldTransition = false,
                isSketchModeActive = false,
                isEraserModeActive = true,
            ),
            exitSketchModeTransition(
                isSketchModeActive = false,
                isEraserModeActive = true,
            ),
        )
    }

    @Test
    fun `색상 슬라이더는 iOS처럼 작은 hue 차이는 동기화하지 않는다`() {
        assertEquals(false, shouldSyncSketchHueSlider(currentHue = 10f, newHue = 20f))
    }

    @Test
    fun `색상 슬라이더는 iOS처럼 큰 hue 차이는 외부 색상에 맞춘다`() {
        assertEquals(true, shouldSyncSketchHueSlider(currentHue = 10f, newHue = 40f))
    }

    @Test
    fun `색상 슬라이더 hue 비교는 iOS처럼 0도 경계도 단순 차이로 판단한다`() {
        assertEquals(true, shouldSyncSketchHueSlider(currentHue = 350f, newHue = 5f))
        assertEquals(true, shouldSyncSketchHueSlider(currentHue = 350f, newHue = 40f))
    }

    @Test
    fun `색상 슬라이더는 iOS처럼 hex 하위 RGB 로 hue 를 계산한다`() {
        assertEquals(240f, sketchHueDegreesFromIosHexColor("#AA0000FF"), 0.01f)
    }

    @Test
    fun `스케치 툴바 색상은 iOS처럼 8자리 hex 의 alpha 를 무시한다`() {
        assertEquals(0xFF123456.toInt(), parseSketchToolbarColorSafe("#AA123456").toArgb())
    }

    @Test
    fun `스케치 펜 버튼도 iOS처럼 8자리 hex 의 alpha 를 무시한다`() {
        assertEquals(0xFF123456.toInt(), sketchPenButtonDisplayColor("#AA123456").toArgb())
    }

    @Test
    fun `스케치 툴바 색상 파싱 실패 시 iOS처럼 빨강으로 폴백한다`() {
        assertEquals(0xFFFF0000.toInt(), parseSketchToolbarColorSafe("not-a-color").toArgb())
    }

    @Test
    fun `스케치 툴바 치수는 iOS SketchToolbarView 와 맞춘다`() {
        assertEquals(32.dp, SketchToolbarButtonSize)
        assertEquals(18.dp, SketchToolbarIconSize)
        assertEquals(R.drawable.ic_eraser, SketchEraserIconRes)
        assertEquals(30.dp, SketchToolbarContainerCornerRadius)
        assertEquals(16.dp, SketchToolbarHorizontalPadding)
        assertEquals(12.dp, SketchToolbarVerticalPadding)
        assertEquals(12.dp, SketchToolbarItemSpacing)
        assertEquals(24.dp, SketchToolbarDividerHeight)
        assertEquals(10.dp, SketchToolbarShadowElevation)
        assertEquals(14.dp, SketchToolbarDoneHorizontalPadding)
        assertEquals(8.dp, SketchToolbarDoneVerticalPadding)
        assertEquals(16.dp, SketchToolbarDoneCornerRadius)
        assertEquals(14.sp, SketchToolbarDoneFontSize)
        assertEquals(9.sp, SketchDeleteBadgeFontSize)
    }

    @Test
    fun `전체 삭제 뱃지는 iOS처럼 실제 스케치 개수를 그대로 표시한다`() {
        assertEquals("3", formatSketchDeleteBadgeCount(3))
        assertEquals("120", formatSketchDeleteBadgeCount(120))
    }

    @Test
    fun `스케치 펜 선택 카드 치수는 iOS PenPickerCard 와 맞춘다`() {
        assertEquals(16.dp, SketchPenPickerCardCornerRadius)
        assertEquals(10.dp, SketchPenPickerCardPadding)
        assertEquals(10.dp, SketchPenPickerCardSpacing)
        assertEquals(44.dp, SketchPenPickerButtonSize)
        assertEquals(4.dp, SketchPenPickerButtonSpacing)
        assertEquals(236.dp, SketchPenPickerContentWidth)
    }

    @Test
    fun `스케치 펜 선택 카드 전환은 iOS opacity scale bottom anchor 를 따른다`() {
        assertEquals(200, SketchPenPickerAnimationDurationMs)
        assertEquals(0.95f, SketchPenPickerInitialScale, 0f)
        assertEquals(0.5f, SketchPenPickerTransformOrigin.pivotFractionX, 0f)
        assertEquals(1f, SketchPenPickerTransformOrigin.pivotFractionY, 0f)
    }

    @Test
    fun `투명도 슬라이더 체크무늬 배경은 iOS CheckerboardPattern 과 맞춘다`() {
        assertEquals(6.dp, SketchOpacityCheckerboardSquareSize)
        assertEquals(0.3f, SketchOpacityCheckerboardDarkAlpha, 0f)
    }

    @Test
    fun `색상과 투명도 슬라이더 thumb 치수는 iOS GradientSlider 와 맞춘다`() {
        assertEquals(30.dp, SketchGradientSliderHeight)
        assertEquals(15.dp, SketchGradientSliderCornerRadius)
        assertEquals(28.dp, SketchGradientSliderThumbSize)
        assertEquals(20.dp, SketchGradientSliderThumbInnerSize)
        assertEquals(0.25f, SketchGradientSliderThumbShadowAlpha, 0f)
        assertEquals(1.dp, SketchGradientSliderThumbShadowOffsetY)
        assertEquals(0f, SketchHueSliderMin, 0f)
        assertEquals(360f, SketchHueSliderMax, 0f)
        assertEquals(0.1f, SketchOpacitySliderMin, 0f)
        assertEquals(1.0f, SketchOpacitySliderMax, 0f)
    }

    @Test
    fun `슬라이더 터치 좌표는 iOS처럼 thumb 반지름을 제외한 영역으로 환산한다`() {
        val width = 236f
        val thumbRadius = 14f

        assertEquals(
            0f,
            sketchSliderValueFromX(
                x = 14f,
                width = width,
                thumbRadius = thumbRadius,
                valueMin = 0f,
                valueMax = 360f,
            ),
            0f,
        )
        assertEquals(
            360f,
            sketchSliderValueFromX(
                x = 222f,
                width = width,
                thumbRadius = thumbRadius,
                valueMin = 0f,
                valueMax = 360f,
            ),
            0f,
        )
        assertEquals(
            0.55f,
            sketchSliderValueFromX(
                x = 118f,
                width = width,
                thumbRadius = thumbRadius,
                valueMin = 0.1f,
                valueMax = 1.0f,
            ),
            0.0001f,
        )
    }

    @Test
    fun `슬라이더 thumb 중심은 iOS position 공식과 동일하다`() {
        val width = 236f
        val thumbRadius = 14f

        assertEquals(
            14f,
            sketchSliderThumbCenterX(
                value = 0.1f,
                width = width,
                thumbRadius = thumbRadius,
                valueMin = 0.1f,
                valueMax = 1.0f,
            ),
            0f,
        )
        assertEquals(
            222f,
            sketchSliderThumbCenterX(
                value = 1.0f,
                width = width,
                thumbRadius = thumbRadius,
                valueMin = 0.1f,
                valueMax = 1.0f,
            ),
            0f,
        )
    }
}
