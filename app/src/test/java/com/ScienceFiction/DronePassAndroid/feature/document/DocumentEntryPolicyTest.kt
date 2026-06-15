package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.ParsedDocument
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNote
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentEntryPolicyTest {

    @Test
    fun `약관 문서는 iOS처럼 로딩 또는 오류 상태로 재진입하면 자동 로드한다`() {
        assertEquals(
            true,
            shouldAutoLoadParsedDocumentOnEnter(ParsedDocumentUiState.Loading),
        )
        assertEquals(
            true,
            shouldAutoLoadParsedDocumentOnEnter(ParsedDocumentUiState.Error("network")),
        )
    }

    @Test
    fun `약관 문서는 캐시된 콘텐츠가 있으면 iOS 메모리 캐시처럼 자동 로드를 건너뛴다`() {
        assertEquals(
            false,
            shouldAutoLoadParsedDocumentOnEnter(
                ParsedDocumentUiState.Content(
                    ParsedDocument(elements = emptyList(), tables = emptyList()),
                ),
            ),
        )
    }

    @Test
    fun `약관 오류 아이콘은 iOS doc text magnifyingglass 처럼 문서 검색 아이콘을 사용한다`() {
        assertEquals(Icons.Outlined.FindInPage, DocumentErrorIcon)
    }

    @Test
    fun `약관 문서 시트 헤더와 상태 토큰은 iOS Terms views 와 맞춘다`() {
        assertEquals(16.dp, DocumentHeaderHorizontalPadding)
        assertEquals(16.dp, DocumentHeaderVerticalPadding)
        assertEquals(0.5.dp, DocumentDividerThickness)
        assertEquals(50.dp, DocumentErrorIconSize)
        assertEquals(12.dp, DocumentEmptyStateSpacing)
        assertEquals(20.dp, DocumentErrorRetryTopSpacing)
    }

    @Test
    fun `패치노트는 iOS reloadIgnoringLocalCacheData 처럼 진입할 때마다 자동 로드한다`() {
        assertEquals(
            true,
            shouldAutoLoadPatchNotesOnEnter(PatchNotesUiState.Loading),
        )
        assertEquals(
            true,
            shouldAutoLoadPatchNotesOnEnter(PatchNotesUiState.Error("network")),
        )
        assertEquals(
            true,
            shouldAutoLoadPatchNotesOnEnter(PatchNotesUiState.Content(emptyList())),
        )
    }

    @Test
    fun `패치노트 reload 는 이전 콘텐츠가 있으면 iOS처럼 리스트를 유지한다`() {
        val existingContent = PatchNotesUiState.Content(
            listOf(
                PatchNote(
                    version = "v1.0.0",
                    date = "2025-01-01",
                    title = "Release",
                    features = emptyList(),
                ),
            ),
        )

        assertEquals(existingContent, patchNotesStateBeforeReload(existingContent))
        assertEquals(
            existingContent,
            patchNotesStateAfterReloadFailure(previousState = existingContent, message = "network"),
        )
    }

    @Test
    fun `패치노트 reload 는 이전 콘텐츠가 없을 때만 loading 과 error 를 보여준다`() {
        assertEquals(PatchNotesUiState.Loading, patchNotesStateBeforeReload(PatchNotesUiState.Loading))
        assertEquals(
            PatchNotesUiState.Loading,
            patchNotesStateBeforeReload(PatchNotesUiState.Content(emptyList())),
        )
        assertEquals(
            PatchNotesUiState.Error("network"),
            patchNotesStateAfterReloadFailure(
                previousState = PatchNotesUiState.Loading,
                message = "network",
            ),
        )
        assertEquals(
            PatchNotesUiState.Error("network"),
            patchNotesStateAfterReloadFailure(
                previousState = PatchNotesUiState.Content(emptyList()),
                message = "network",
            ),
        )
    }
}
