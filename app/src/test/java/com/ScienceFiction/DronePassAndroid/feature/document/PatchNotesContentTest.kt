package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNote
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNoteFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
