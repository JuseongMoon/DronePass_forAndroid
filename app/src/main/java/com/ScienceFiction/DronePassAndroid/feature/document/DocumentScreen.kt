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
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.ScienceFiction.DronePassAndroid.R

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
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 고정 헤더 (iOS VStack { HStack { title + Close } + Divider } 정합)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // 본문 — 상태별 분기
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is ParsedDocumentUiState.Loading -> LoadingContent()
                is ParsedDocumentUiState.Error -> ErrorContent(
                    message = state.message,
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

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.document_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.document_error_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message ?: stringResource(R.string.document_error_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
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
        if (state is ParsedDocumentUiState.Loading) viewModel.loadTerms()
    }
    DocumentScreen(
        title = stringResource(R.string.profile_terms_service),
        state = state,
        onRetry = { viewModel.loadTerms() },
        onDismiss = onDismiss,
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
        if (state is ParsedDocumentUiState.Loading) viewModel.loadPrivacy()
    }
    DocumentScreen(
        title = stringResource(R.string.profile_terms_privacy),
        state = state,
        onRetry = { viewModel.loadPrivacy() },
        onDismiss = onDismiss,
    )
}


