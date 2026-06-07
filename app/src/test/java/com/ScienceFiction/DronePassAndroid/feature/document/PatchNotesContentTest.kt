package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
        assertEquals(50.dp, PatchNotesEmptyStateIconSize)
        assertEquals(12.dp, PatchNotesEmptyStateSpacing)
    }
}
