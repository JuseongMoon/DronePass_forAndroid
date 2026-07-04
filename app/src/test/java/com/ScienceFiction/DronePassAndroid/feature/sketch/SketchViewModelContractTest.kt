package com.ScienceFiction.DronePassAndroid.feature.sketch

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SketchViewModelContractTest {

    @Test
    fun `스케치 모드 진입은 iOS SketchManager 처럼 세션 상태를 초기화한 뒤 편집 세션을 시작한다`() {
        val source = sketchViewModelSource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "fun enterSketchMode()",
                "drawingBuffer.clear()",
                "_currentDrawingPoints.value = emptyList()",
                "lastSampledPoint = null",
                "undoStack.clear()",
                "redoStack.clear()",
                "updateUndoRedoState()",
                "_isSketchMode.value = transition.isSketchModeActive",
                "_isEraserMode.value = transition.isEraserModeActive",
                "sketchRepository.beginSketchEditSession()",
                "sketchModeEnterTimeMillis = System.currentTimeMillis()",
                "analyticsLogger.logSketchModeEntered()",
            ),
        )
    }

    @Test
    fun `스케치 모드 종료는 iOS처럼 진행 중인 선 저장 후 모드 종료와 완료 동기화를 수행한다`() {
        val source = sketchViewModelSource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "fun exitSketchMode()",
                "if (drawingBuffer.isNotEmpty())",
                "finishDrawing(recordUndo = RecordSketchUndoWhenExitingMode)",
                "_isSketchMode.value = transition.isSketchModeActive",
                "_isEraserMode.value = transition.isEraserModeActive",
                "syncSketchesOnModeComplete()",
                "analyticsLogger.logSketchModeExited(durationSeconds)",
                "sketchModeEnterTimeMillis = null",
            ),
        )
    }

    @Test
    fun `전체 스케치 삭제는 iOS처럼 하나의 undo 액션으로 저장하고 redo 를 초기화한다`() {
        val source = sketchViewModelSource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "fun deleteAllSketches()",
                "val deletedSketches = sketchRepository.deleteAllSketches()",
                "if (deletedSketches.isNotEmpty())",
                "undoStack.addLast(SketchAction.Delete(deletedSketches))",
                "redoStack.clear()",
                "updateUndoRedoState()",
                "analyticsLogger.logSketchAllCleared(deletedSketches.size)",
            ),
        )
    }

    private fun sketchViewModelSource(): String {
        return resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchViewModel.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchViewModel.kt",
        ).readText()
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
