package com.ScienceFiction.DronePassAndroid.feature.map

import org.junit.Assert.assertEquals
import org.junit.Test

class SketchTouchDecisionTest {

    @Test
    fun `1손가락 down 은 펜 모드에서 그리기를 시작하고 터치를 소비한다`() {
        val decision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Down,
            pointerCount = 1,
            isSketchTouchActive = false,
            isEraserMode = false,
        )

        assertEquals(
            SketchTouchDecision(
                consume = true,
                nextIsSketchTouchActive = true,
                action = SketchTouchAction.StartDrawing,
            ),
            decision,
        )
    }

    @Test
    fun `1손가락 move 는 활성 스케치 터치일 때만 그리기를 이어간다`() {
        val decision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Move,
            pointerCount = 1,
            isSketchTouchActive = true,
            isEraserMode = false,
        )

        assertEquals(
            SketchTouchDecision(
                consume = true,
                nextIsSketchTouchActive = true,
                action = SketchTouchAction.ContinueDrawing,
            ),
            decision,
        )
    }

    @Test
    fun `1손가락 move 가 비활성 터치면 iOS처럼 지도에 전달한다`() {
        val decision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Move,
            pointerCount = 1,
            isSketchTouchActive = false,
            isEraserMode = false,
        )

        assertEquals(
            SketchTouchDecision(
                consume = false,
                nextIsSketchTouchActive = false,
                action = null,
            ),
            decision,
        )
    }

    @Test
    fun `1손가락 up 은 활성 펜 터치를 저장하고 터치 상태를 종료한다`() {
        val decision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Up,
            pointerCount = 1,
            isSketchTouchActive = true,
            isEraserMode = false,
        )

        assertEquals(
            SketchTouchDecision(
                consume = true,
                nextIsSketchTouchActive = false,
                action = SketchTouchAction.FinishDrawing,
            ),
            decision,
        )
    }

    @Test
    fun `cancel 은 활성 펜 터치를 저장하지 않고 취소한다`() {
        val decision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Cancel,
            pointerCount = 1,
            isSketchTouchActive = true,
            isEraserMode = false,
        )

        assertEquals(
            SketchTouchDecision(
                consume = true,
                nextIsSketchTouchActive = false,
                action = SketchTouchAction.CancelDrawing,
            ),
            decision,
        )
    }

    @Test
    fun `2손가락 이상으로 전환되면 진행 중인 펜 터치를 취소하고 지도에 전달한다`() {
        val decision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Move,
            pointerCount = 2,
            isSketchTouchActive = true,
            isEraserMode = false,
        )

        assertEquals(
            SketchTouchDecision(
                consume = false,
                nextIsSketchTouchActive = false,
                action = SketchTouchAction.CancelDrawing,
            ),
            decision,
        )
    }

    @Test
    fun `지우개 모드의 1손가락 down 과 move 는 삭제 액션이다`() {
        val downDecision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Down,
            pointerCount = 1,
            isSketchTouchActive = false,
            isEraserMode = true,
        )
        val moveDecision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Move,
            pointerCount = 1,
            isSketchTouchActive = true,
            isEraserMode = true,
        )

        assertEquals(SketchTouchAction.DeleteAtPoint, downDecision.action)
        assertEquals(SketchTouchAction.DeleteAtPoint, moveDecision.action)
    }

    @Test
    fun `지우개 모드의 멀티터치는 취소 액션 없이 지도에 전달한다`() {
        val decision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Move,
            pointerCount = 2,
            isSketchTouchActive = true,
            isEraserMode = true,
        )

        assertEquals(
            SketchTouchDecision(
                consume = false,
                nextIsSketchTouchActive = false,
                action = null,
            ),
            decision,
        )
    }

    @Test
    fun `비활성 up cancel other 이벤트는 iOS처럼 지도에 전달한다`() {
        val upDecision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Up,
            pointerCount = 1,
            isSketchTouchActive = false,
            isEraserMode = false,
        )
        val cancelDecision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Cancel,
            pointerCount = 1,
            isSketchTouchActive = false,
            isEraserMode = false,
        )
        val otherDecision = resolveSketchTouchEvent(
            eventType = SketchTouchEventType.Other,
            pointerCount = 1,
            isSketchTouchActive = false,
            isEraserMode = false,
        )

        assertEquals(
            SketchTouchDecision(
                consume = false,
                nextIsSketchTouchActive = false,
                action = null,
            ),
            upDecision,
        )
        assertEquals(
            SketchTouchDecision(
                consume = false,
                nextIsSketchTouchActive = false,
                action = null,
            ),
            cancelDecision,
        )
        assertEquals(
            SketchTouchDecision(
                consume = false,
                nextIsSketchTouchActive = false,
                action = null,
            ),
            otherDecision,
        )
    }
}
