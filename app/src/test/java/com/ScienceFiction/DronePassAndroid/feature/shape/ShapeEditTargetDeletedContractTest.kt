package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 편집·복제 중인 도형이 다른 기기에서 삭제되면 지도에서는 시트가 빈 "새 도형" 폼으로 바뀌어 새 도형이 생길 수 있었고,
 * 저장 목록에서는 시트가 조용히 사라진 채 플래그가 남았다. 두 화면 모두 시트를 닫고 알린다.
 */
class ShapeEditTargetDeletedContractTest {

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }

    private fun assertOrder(text: String, tokens: List<String>) {
        var previous = -1
        tokens.forEach { token ->
            val index = text.indexOf(token, previous + 1)
            assertTrue("$token should appear after index $previous", index > previous)
            previous = index
        }
    }

    @Test
    fun `map and saved list close the edit sheet when the edited shape is deleted elsewhere`() {
        assertOrder(
            source("feature/map/MapViewModel.kt"),
            listOf(
                "combine(_showShapeEdit, _newShapeCoordinate, _selectedShapeId, activeShapes)",
                "shapeRepository.getShapeById(missingId)",
                "dismissShapeEdit()",
                "_shapeEditTargetDeleted.tryEmit(Unit)",
            ),
        )
        assertOrder(
            source("feature/saved/SavedListViewModel.kt"),
            listOf(
                "combine(_showShapeEdit, _selectedShapeId, activeShapes)",
                "shapeRepository.getShapeById(missingId)",
                "_showShapeEdit.value = false",
                "_shapeEditTargetDeleted.tryEmit(Unit)",
            ),
        )
        assertTrue(source("feature/map/MapScreen.kt").contains("viewModel.shapeEditTargetDeleted.collect"))
        assertTrue(source("feature/saved/SavedListScreen.kt").contains("viewModel.shapeEditTargetDeleted.collect"))
    }
}
