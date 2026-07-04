package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNote
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNoteFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PatchNotesContentTest {

    @Test
    fun `패치노트 섹션 치수는 iOS inset grouped section 과 맞춘다`() {
        assertEquals(16.dp, PatchNoteSectionHorizontalPadding)
        assertEquals(4.dp, PatchNoteSectionVerticalPadding)
        assertEquals(16.dp, PatchNoteSectionContentPadding)
        assertEquals(12.dp, PatchNoteSectionCornerRadius)
        assertEquals(16.dp, PatchNoteSectionSpacing)
        assertEquals(5.dp, PatchNoteHeaderLeadingPadding)
        assertEquals(10.dp, PatchNoteDateTrailingPadding)
        assertEquals(16.dp, PatchNoteFeatureTitleIconSize)
        assertEquals(20.dp, PatchNoteFeatureDescriptionLeadingPadding)
        assertEquals(12.dp, PatchNoteFeatureGroupSpacing)
        assertEquals(6.dp, PatchNoteFeatureItemSpacing)
        assertEquals(3.dp, PatchNoteFeatureDescriptionBulletSpacing)
        assertEquals(1f, PatchNoteFeatureTextWeight, 0f)
        assertEquals(50.dp, PatchNotesEmptyStateIconSize)
        assertEquals(12.dp, PatchNotesEmptyStateSpacing)
    }

    @Test
    fun `패치노트 feature 표시 여부는 iOS처럼 isEmpty 기준을 사용한다`() {
        assertFalse(
            shouldShowPatchNoteFeatures(
                listOf(
                    PatchNoteFeature(title = "", description = null),
                    PatchNoteFeature(title = "", description = "description"),
                ),
            ),
        )
        assertTrue(
            shouldShowPatchNoteFeatures(
                listOf(PatchNoteFeature(title = " ", description = null)),
            ),
        )
        assertTrue(
            shouldShowPatchNoteFeatures(
                listOf(
                    PatchNoteFeature(title = "", description = null),
                    PatchNoteFeature(title = "Feature", description = null),
                ),
            ),
        )
    }

    @Test
    fun `패치노트 description 표시 여부도 iOS처럼 isEmpty 기준을 사용한다`() {
        assertFalse(shouldShowPatchNoteFeatureDescription(null))
        assertFalse(shouldShowPatchNoteFeatureDescription(""))
        assertTrue(shouldShowPatchNoteFeatureDescription(" "))
        assertTrue(shouldShowPatchNoteFeatureDescription("description"))
    }

    @Test
    fun `패치노트 title 표시 여부도 iOS처럼 isEmpty 기준을 사용한다`() {
        assertFalse(shouldShowPatchNoteTitle(""))
        assertTrue(shouldShowPatchNoteTitle(" "))
        assertTrue(shouldShowPatchNoteTitle("Release title"))
    }

    @Test
    fun `패치노트 리스트 키는 iOS UUID identity 처럼 같은 version date 도 충돌하지 않는다`() {
        val note = PatchNote(
            version = "v1.0.0",
            date = "2025-01-01",
            title = "Release",
            features = emptyList(),
        )

        assertNotEquals(
            patchNoteListKey(0, note),
            patchNoteListKey(1, note),
        )
    }

    @Test
    fun `패치노트 상태 분기는 iOS처럼 loading empty list 순서를 유지한다`() {
        val source = patchNotesContentSource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "is PatchNotesUiState.Loading -> LoadingContent()",
                "is PatchNotesUiState.Error -> ErrorContent()",
                "is PatchNotesUiState.Content ->",
                "if (s.notes.isEmpty())",
                "ErrorContent()",
                "LazyColumn",
                "itemsIndexed(s.notes, key = ::patchNoteListKey)",
                "PatchNoteSection(note = note)",
            ),
        )
    }

    @Test
    fun `패치노트 목록 섹션은 iOS처럼 header feature title description 순서를 유지한다`() {
        val source = patchNotesContentSource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "private fun PatchNoteSection",
                "PatchNoteHeader(note = note)",
                "if (shouldShowPatchNoteFeatures(note.features))",
                "note.features.forEach",
                "FeatureTitleRow(title = feature.title)",
                "FeatureDescription(description = feature.description)",
            ),
        )
    }

    @Test
    fun `패치노트 description 은 iOS처럼 newline 으로 나눈 뒤 dash bullet 을 붙인다`() {
        val source = patchNotesContentSource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "private fun FeatureDescription",
                "?.split(\"\\n\")",
                "?.forEach { line ->",
                "Text(",
                "text = \"-\"",
                "Text(",
                "text = line",
            ),
        )
    }

    @Test
    fun `패치노트 loading 과 empty 상태는 iOS처럼 아이콘 텍스트 순서를 유지한다`() {
        val source = patchNotesContentSource()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "private fun LoadingContent",
                "CircularProgressIndicator()",
                "R.string.patch_notes_loading",
                "private fun ErrorContent",
                "Icons.Outlined.FindInPage",
                "R.string.patch_notes_error_title",
                "R.string.patch_notes_error_message",
            ),
        )
    }

    private fun patchNotesContentSource(): String {
        return resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/PatchNotesContent.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/PatchNotesContent.kt",
        ).readText()
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in PatchNotesContent.kt",
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
