package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.ScienceFiction.DronePassAndroid.R

internal val DocumentErrorIcon = Icons.Outlined.FindInPage
internal val DocumentHeaderHorizontalPadding = 16.dp
internal val DocumentHeaderVerticalPadding = 16.dp
internal val DocumentDividerThickness = 0.5.dp
internal val DocumentErrorIconSize = 50.dp
internal val DocumentEmptyStateSpacing = 12.dp
internal val DocumentErrorRetryTopSpacing = 20.dp

/**
 * iOS `TermsOfServiceView` / `PrivacyPolicyView` 정합 공통 시트 콘텐츠.
 *
 * 상단 고정 헤더(제목 + 닫기) + 0.5dp Divider + ScrollView 분기(Loading/Content/Error).
 *
 * @param title 헤더 제목 텍스트
 * @param state 현재 UI 상태
 * @param onRetry 에러 상태에서 "다시 시도" 클릭 시 호출
 * @param onDismiss 헤더 "닫기" 버튼 호출 (ModalBottomSheet 호스트가 시트 닫기)
 */
@Composable
fun DocumentScreen(
    title: String,
    state: ParsedDocumentUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes loadingTextResId: Int = R.string.document_loading,
    @StringRes errorTitleResId: Int = R.string.document_error_title,
    @StringRes errorMessageResId: Int = R.string.document_error_message,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 고정 헤더 (iOS VStack { HStack { title + Close } + Divider } 정합)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DocumentHeaderHorizontalPadding,
                    vertical = DocumentHeaderVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
        HorizontalDivider(
            thickness = DocumentDividerThickness,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // 본문 — 상태별 분기
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is ParsedDocumentUiState.Loading -> LoadingContent(loadingTextResId = loadingTextResId)
                is ParsedDocumentUiState.Error -> ErrorContent(
                    titleResId = errorTitleResId,
                    messageResId = errorMessageResId,
                    onRetry = onRetry,
                )
                is ParsedDocumentUiState.Content -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                ) {
                    MarkdownView(
                        elements = state.document.elements,
                        tables = state.document.tables,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

internal fun shouldAutoLoadParsedDocumentOnEnter(state: ParsedDocumentUiState): Boolean {
    return state !is ParsedDocumentUiState.Content
}

@Composable
private fun LoadingContent(@StringRes loadingTextResId: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(DocumentEmptyStateSpacing))
        Text(
            text = stringResource(loadingTextResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(
    @StringRes titleResId: Int,
    @StringRes messageResId: Int,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = DocumentErrorIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DocumentErrorIconSize),
        )
        Spacer(modifier = Modifier.height(DocumentEmptyStateSpacing))
        Text(
            text = stringResource(titleResId),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(DocumentEmptyStateSpacing))
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(DocumentErrorRetryTopSpacing))
        OutlinedButton(onClick = onRetry) {
            Text(stringResource(R.string.common_retry))
        }
    }
}

/**
 * 이용약관 화면 (iOS `TermsOfServiceView` 1:1 정합).
 */
@Composable
fun TermsOfServiceScreen(
    onDismiss: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel(),
) {
    val state by viewModel.termsState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (shouldAutoLoadParsedDocumentOnEnter(state)) viewModel.loadTerms()
    }
    DocumentScreen(
        title = stringResource(R.string.profile_terms_service),
        state = state,
        onRetry = { viewModel.loadTerms() },
        onDismiss = onDismiss,
        loadingTextResId = R.string.document_terms_loading,
        errorTitleResId = R.string.document_terms_service_error_title,
        errorMessageResId = R.string.document_terms_service_error_message,
    )
}

/**
 * 개인정보 처리방침 화면 (iOS `PrivacyPolicyView` 1:1 정합).
 */
@Composable
fun PrivacyPolicyScreen(
    onDismiss: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel(),
) {
    val state by viewModel.privacyState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (shouldAutoLoadParsedDocumentOnEnter(state)) viewModel.loadPrivacy()
    }
    DocumentScreen(
        title = stringResource(R.string.profile_terms_privacy),
        state = state,
        onRetry = { viewModel.loadPrivacy() },
        onDismiss = onDismiss,
        loadingTextResId = R.string.document_terms_loading,
        errorTitleResId = R.string.document_terms_privacy_error_title,
        errorMessageResId = R.string.document_terms_privacy_error_message,
    )
}
