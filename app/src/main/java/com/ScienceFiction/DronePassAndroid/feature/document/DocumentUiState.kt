package com.ScienceFiction.DronePassAndroid.feature.document

import com.ScienceFiction.DronePassAndroid.domain.model.ParsedDocument
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNote

/** 약관/개인정보 화면 상태 — iOS TermsOfServiceView 의 isLoading/error/empty 분기 정합. */
sealed class ParsedDocumentUiState {
    data object Loading : ParsedDocumentUiState()
    data class Content(val document: ParsedDocument) : ParsedDocumentUiState()
    data class Error(val message: String?) : ParsedDocumentUiState()
}

/** 패치노트 화면 상태. */
sealed class PatchNotesUiState {
    data object Loading : PatchNotesUiState()
    data class Content(val notes: List<PatchNote>) : PatchNotesUiState()
    data class Error(val message: String?) : PatchNotesUiState()
}
