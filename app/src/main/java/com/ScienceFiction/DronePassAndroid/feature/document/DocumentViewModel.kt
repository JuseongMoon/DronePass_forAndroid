package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 4개 문서 화면(Terms/Privacy/LocationTerms/PatchNotes) 공통 ViewModel.
 *
 * 단일 ViewModel 에 4개 StateFlow 를 두어 같은 인스턴스 내 fetch 중복 방지.
 * iOS `FetchWebDocuments` 가 4개 published 속성을 가진 것과 동일한 패턴.
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val repository: DocumentRepository,
) : ViewModel() {

    private val _termsState = MutableStateFlow<ParsedDocumentUiState>(ParsedDocumentUiState.Loading)
    val termsState: StateFlow<ParsedDocumentUiState> = _termsState.asStateFlow()

    private val _privacyState = MutableStateFlow<ParsedDocumentUiState>(ParsedDocumentUiState.Loading)
    val privacyState: StateFlow<ParsedDocumentUiState> = _privacyState.asStateFlow()

    private val _locationTermsState = MutableStateFlow<ParsedDocumentUiState>(ParsedDocumentUiState.Loading)
    val locationTermsState: StateFlow<ParsedDocumentUiState> = _locationTermsState.asStateFlow()

    private val _patchNotesState = MutableStateFlow<PatchNotesUiState>(PatchNotesUiState.Loading)
    val patchNotesState: StateFlow<PatchNotesUiState> = _patchNotesState.asStateFlow()

    fun loadTerms() {
        viewModelScope.launch {
            _termsState.value = ParsedDocumentUiState.Loading
            repository.fetchTerms().fold(
                onSuccess = { _termsState.value = ParsedDocumentUiState.Content(it) },
                onFailure = { _termsState.value = ParsedDocumentUiState.Error(it.localizedMessage) },
            )
        }
    }

    fun loadPrivacy() {
        viewModelScope.launch {
            _privacyState.value = ParsedDocumentUiState.Loading
            repository.fetchPrivacyPolicy().fold(
                onSuccess = { _privacyState.value = ParsedDocumentUiState.Content(it) },
                onFailure = { _privacyState.value = ParsedDocumentUiState.Error(it.localizedMessage) },
            )
        }
    }

    fun loadLocationTerms() {
        viewModelScope.launch {
            _locationTermsState.value = ParsedDocumentUiState.Loading
            repository.fetchLocationTerms().fold(
                onSuccess = { _locationTermsState.value = ParsedDocumentUiState.Content(it) },
                onFailure = { _locationTermsState.value = ParsedDocumentUiState.Error(it.localizedMessage) },
            )
        }
    }

    fun loadPatchNotes() {
        viewModelScope.launch {
            _patchNotesState.value = PatchNotesUiState.Loading
            repository.fetchPatchNotes().fold(
                onSuccess = { _patchNotesState.value = PatchNotesUiState.Content(it) },
                onFailure = { _patchNotesState.value = PatchNotesUiState.Error(it.localizedMessage) },
            )
        }
    }
}
