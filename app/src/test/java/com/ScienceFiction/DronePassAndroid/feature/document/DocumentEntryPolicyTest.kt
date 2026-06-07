package com.ScienceFiction.DronePassAndroid.feature.document

import com.ScienceFiction.DronePassAndroid.domain.model.ParsedDocument
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
}
