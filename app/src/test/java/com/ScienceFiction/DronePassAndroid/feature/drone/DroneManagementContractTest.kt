package com.ScienceFiction.DronePassAndroid.feature.drone

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DroneManagementContractTest {

    @Test
    fun `드론 목록 섹션 순서는 iOS DroneListView 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneListScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneListScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.drone_list_section_my",
                "DroneListItem(",
                "DroneAddItem(",
                "R.string.drone_list_section_usage",
                "DroneUsageSection()",
            ),
        )
        assertTrue(
            "DroneListScreen.kt should keep iOS empty section footer text",
            source.contains("R.string.drone_list_empty"),
        )
    }

    @Test
    fun `드론 상세 행 순서는 iOS DroneDetailView 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneDetailSheet.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneDetailSheet.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.drone_detail_name",
                "R.string.drone_detail_section_basic",
                "R.string.drone_detail_color",
                "R.string.drone_detail_serial_number",
                "R.string.drone_detail_section_specs",
                "R.string.drone_detail_takeoff_weight",
                "R.string.drone_detail_size",
                "R.string.drone_detail_section_memo",
                "R.string.drone_detail_memo_empty",
            ),
        )
    }

    @Test
    fun `드론 편집 섹션 순서는 iOS DroneEditView 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneEditSheet.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneEditSheet.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.drone_edit_section_basic",
                "R.string.drone_edit_name_placeholder",
                "R.string.drone_edit_color",
                "R.string.drone_edit_section_serial",
                "R.string.drone_edit_serial_placeholder",
                "R.string.drone_edit_section_specs",
                "R.string.drone_edit_weight_placeholder",
                "R.string.drone_edit_size_placeholder",
                "R.string.drone_edit_section_memo",
            ),
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex",
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
