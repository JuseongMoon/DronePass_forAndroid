package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SketchDefaultsTest {

    @Test
    fun `스케치 기본 펜 설정은 iOS SketchManager 초기값과 맞춘다`() {
        assertEquals("#FF0000", DefaultSketchColor)
        assertEquals(4.0, DefaultSketchStrokeWidth, 0.0)
        assertEquals(1.0, DefaultSketchOpacity, 0.0)
    }

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
    fun `스케치 모드 종료 중 저장된 선은 iOS처럼 undo 액션으로 기록한다`() {
        assertEquals(true, RecordSketchUndoWhenExitingMode)
    }

    @Test
    fun `지우개 토글은 iOS처럼 진행 중인 선 버퍼를 취소하지 않는다`() {
        assertEquals(false, CancelSketchDrawingWhenTogglingEraser)
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
    fun `색상 슬라이더 hue to hex 는 iOS처럼 RGB 성분을 버림 처리한다`() {
        assertEquals("#E52222", sketchHueToIosHexColor(0f))
        assertEquals("#22E522", sketchHueToIosHexColor(120f))
        assertEquals("#2222E5", sketchHueToIosHexColor(240f))
        assertEquals("#E59722", sketchHueToIosHexColor(36f))
        assertEquals("#E52222", sketchHueToIosHexColor(360f))
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
        assertEquals(14.dp, SketchDeleteBadgeMinSize)
        assertEquals(4.dp, SketchDeleteBadgeOffsetX)
        assertEquals((-4).dp, SketchDeleteBadgeOffsetY)
    }

    @Test
    fun `스케치 툴바 버튼 순서는 iOS SketchToolbarView 와 맞춘다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchToolbar.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchToolbar.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "PenButton(",
                "painter = painterResource(SketchEraserIconRes)",
                "icon = Icons.AutoMirrored.Filled.Undo",
                "icon = Icons.AutoMirrored.Filled.Redo",
                ".height(SketchToolbarDividerHeight)",
                "DeleteAllButton(",
                "DoneButton(",
            ),
        )
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
    fun `스케치 펜 굵기 선택지는 iOS PenPickerCard 와 맞춘다`() {
        assertEquals(listOf(2.0, 4.0, 6.0, 8.0, 10.0), SketchPenStrokeWidthOptions)
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

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        tokens.forEach { token ->
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "Expected token '$token' after index $previousIndex",
                index >= 0,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Could not resolve project file from: ${candidates.joinToString()}")
    }
}
