package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DroneDeleteValidationTest {

    @Test
    fun `마지막 드론 삭제는 iOS처럼 차단한다`() {
        assertEquals(
            DroneDeleteValidationError.CANNOT_DELETE_LAST_DRONE,
            validateDroneDeleteRequest(
                activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
                connectedShapeCount = 0,
                shapeHandling = ShapeHandling.DeleteAll,
            ),
        )
    }

    @Test
    fun `연결 도형 재할당 대상이 없으면 iOS처럼 삭제를 중단한다`() {
        assertEquals(
            DroneDeleteValidationError.TARGET_DRONE_NOT_FOUND,
            validateDroneDeleteRequest(
                activeDrones = listOf(
                    DroneModel(id = "drone-a", name = "A"),
                    DroneModel(id = "drone-b", name = "B"),
                ),
                connectedShapeCount = 2,
                shapeHandling = ShapeHandling.Reassign("missing-drone"),
            ),
        )
    }

    @Test
    fun `연결 도형 재할당 대상이 삭제할 드론 자신이면 삭제를 중단한다`() {
        assertEquals(
            DroneDeleteValidationError.TARGET_DRONE_NOT_FOUND,
            validateDroneDeleteRequest(
                activeDrones = listOf(
                    DroneModel(id = "drone-a", name = "A"),
                    DroneModel(id = "drone-b", name = "B"),
                ),
                connectedShapeCount = 2,
                shapeHandling = ShapeHandling.Reassign("drone-a"),
                deletingDroneId = "drone-a",
            ),
        )
    }

    @Test
    fun `연결 도형이 없으면 iOS처럼 재할당 대상 검증 없이 통과한다`() {
        assertNull(
            validateDroneDeleteRequest(
                activeDrones = listOf(
                    DroneModel(id = "drone-a", name = "A"),
                    DroneModel(id = "drone-b", name = "B"),
                ),
                connectedShapeCount = 0,
                shapeHandling = ShapeHandling.Reassign("missing-drone"),
            ),
        )
    }

    @Test
    fun `연결 도형 재할당 대상이 있으면 삭제 요청이 유효하다`() {
        assertNull(
            validateDroneDeleteRequest(
                activeDrones = listOf(
                    DroneModel(id = "drone-a", name = "A"),
                    DroneModel(id = "drone-b", name = "B"),
                ),
                connectedShapeCount = 2,
                shapeHandling = ShapeHandling.Reassign("drone-b"),
            ),
        )
    }

    @Test
    fun `삭제 다이얼로그는 연결 도형 확인 중이면 로딩 상태다`() {
        assertEquals(
            DroneDeleteDialogType.Loading,
            resolveDroneDeleteDialogType(shapeCount = null),
        )
    }

    @Test
    fun `삭제 메뉴를 누르면 iOS처럼 연결 도형 수를 새로 확인할 때까지 로딩 상태로 시작한다`() {
        assertEquals(
            DroneDeleteDialogType.Loading,
            resolveDroneDeleteDialogType(resetDroneDeleteShapeCountForPrompt()),
        )
    }

    @Test
    fun `연결 도형이 없으면 iOS처럼 일반 삭제 확인을 보여준다`() {
        assertEquals(
            DroneDeleteDialogType.ConfirmDelete,
            resolveDroneDeleteDialogType(shapeCount = 0),
        )
    }

    @Test
    fun `연결 도형이 있으면 iOS처럼 처리 방식 선택을 보여준다`() {
        assertEquals(
            DroneDeleteDialogType.ShapeHandling,
            resolveDroneDeleteDialogType(shapeCount = 3),
        )
    }

    @Test
    fun `연결 도형 처리 선택은 iOS confirmationDialog처럼 하단 액션 시트로 표시한다`() {
        assertTrue(shouldShowDroneDeleteShapeHandlingActionSheet(shapeCount = 3))
        assertFalse(shouldShowDroneDeleteShapeHandlingActionSheet(shapeCount = 0))
        assertFalse(shouldShowDroneDeleteShapeHandlingActionSheet(shapeCount = null))
        assertTrue(DroneDeleteShapeHandlingSkipPartiallyExpanded)
    }

    @Test
    fun `드론 상세 스냅샷은 iOS처럼 삭제 포함 전체 목록에서 같은 id 변경을 반영한다`() {
        val current = DroneModel(id = "drone-a", name = "Before", updatedAt = 1L)
        val updated = current.copy(name = "After", deletedAt = 10L, updatedAt = 11L)

        assertEquals(
            updated,
            resolveDroneDetailSnapshot(
                selectedDrone = current,
                allDrones = listOf(updated),
            ),
        )
    }

    @Test
    fun `드론 상세 스냅샷은 대상이 목록에서 사라져도 iOS처럼 현재 값을 유지한다`() {
        val current = DroneModel(id = "drone-a", name = "Current")

        assertEquals(
            current,
            resolveDroneDetailSnapshot(
                selectedDrone = current,
                allDrones = emptyList(),
            ),
        )
    }

    @Test
    fun `선택 드론이 없으면 드론 상세 스냅샷도 없다`() {
        assertNull(
            resolveDroneDetailSnapshot(
                selectedDrone = null,
                allDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            ),
        )
    }

    @Test
    fun `편집 시트 취소는 기존 드론 편집일 때만 iOS처럼 상세로 돌아간다`() {
        assertTrue(shouldReturnToDroneDetailAfterEditDismiss(DroneModel(id = "drone-a", name = "A")))
        assertFalse(shouldReturnToDroneDetailAfterEditDismiss(null))
    }

    @Test
    fun `드론 상세 복사 토스트는 iOS CopyToastOverlay 표시 타이밍과 위치를 따른다`() {
        assertEquals(1_500L, DroneDetailCopyToastDurationMs)
        assertEquals(300, DroneDetailCopyToastAnimationDurationMs)
        assertEquals(0.75f, DroneDetailCopyToastBackgroundAlpha, 0f)
        assertEquals(50f, DroneDetailCopyToastBottomPadding.value, 0f)
        assertEquals(HapticFeedbackType.LongPress, DroneDetailCopyHapticFeedbackType)
    }

    @Test
    fun `재할당 대상 드론 색상 원은 iOS처럼 팔레트 색상이 있을 때만 표시한다`() {
        assertTrue(shouldShowDroneMoveTargetColorIndicator(PaletteColor.BLUE))
        assertFalse(shouldShowDroneMoveTargetColorIndicator(null))
    }
}
