package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShapeEditContractTest {

    @Test
    fun `도형 편집 행 순서는 iOS ShapeEditView 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.shape_edit_drone_label",
                "R.string.shape_edit_title_label",
                "R.string.shape_edit_coordinate_label",
                "R.string.shape_edit_label_address",
                "R.string.shape_edit_radius_label",
                "R.string.shape_edit_altitude_label",
                "R.string.shape_edit_start_date",
                "R.string.shape_edit_end_date",
                "R.string.shape_edit_date_only_mode",
                "R.string.shape_edit_label_memo",
            ),
        )
    }

    @Test
    fun `도형 편집 내비게이션 액션 순서는 iOS leading cancel trailing save 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.shape_edit_navigation_cancel",
                "R.string.shape_edit_navigation_save",
            ),
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in ShapeEditScreen.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
