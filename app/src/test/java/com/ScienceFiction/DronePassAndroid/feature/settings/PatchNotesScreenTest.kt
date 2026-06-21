package com.ScienceFiction.DronePassAndroid.feature.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PatchNotesScreenTest {

    @Test
    fun `패치노트 화면은 iOS PatchNotesView 처럼 large title close action content 순서를 유지한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/PatchNotesScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/PatchNotesScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "LargeTopAppBar",
                "R.string.patch_notes_title",
                "R.string.common_close",
                "PatchNotesContent()",
            ),
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in PatchNotesScreen.kt",
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
