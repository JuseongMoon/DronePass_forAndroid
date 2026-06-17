package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNote
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNoteFeature

internal val PatchNoteSectionHorizontalPadding = 16.dp
internal val PatchNoteSectionVerticalPadding = 4.dp
internal val PatchNoteSectionContentPadding = 16.dp
internal val PatchNoteSectionCornerRadius = 12.dp
internal val PatchNoteSectionSpacing = 16.dp
internal val PatchNoteHeaderLeadingPadding = 5.dp
internal val PatchNoteDateTrailingPadding = 10.dp
internal val PatchNoteFeatureTitleIconSize = 16.dp
internal val PatchNoteFeatureDescriptionLeadingPadding = 20.dp
internal val PatchNoteFeatureGroupSpacing = 12.dp
internal val PatchNoteFeatureItemSpacing = 6.dp
internal val PatchNoteFeatureDescriptionBulletSpacing = 3.dp
internal val PatchNotesEmptyStateIconSize = 50.dp
internal val PatchNotesEmptyStateSpacing = 12.dp

/**
 * iOS `PatchNotesView` 1:1 정합 콘텐츠.
 *
 * List(insetGrouped) → LazyColumn 섹션형 콘텐츠.
 * 각 PatchNote: Header(version + date + title) + Features("✓ feature title" + 들여쓰기 "- description" 멀티라인).
 *
 * 헤더 없는 순수 콘텐츠. 호스팅 화면(PatchNotesScreen 또는 ModalBottomSheet)이 TopAppBar/시트 헤더 제공.
 */
@Composable
fun PatchNotesContent(
    modifier: Modifier = Modifier,
    viewModel: DocumentViewModel = hiltViewModel(),
) {
    val state by viewModel.patchNotesState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (shouldAutoLoadPatchNotesOnEnter(state)) viewModel.loadPatchNotes()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is PatchNotesUiState.Loading -> LoadingContent()
            is PatchNotesUiState.Error -> ErrorContent()
            is PatchNotesUiState.Content -> {
                if (s.notes.isEmpty()) {
                    ErrorContent()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(s.notes, key = ::patchNoteListKey) { _, note ->
                            PatchNoteSection(note = note)
                        }
                    }
                }
            }
        }
    }
}

internal fun shouldAutoLoadPatchNotesOnEnter(state: PatchNotesUiState): Boolean {
    return when (state) {
        is PatchNotesUiState.Loading,
        is PatchNotesUiState.Error,
        is PatchNotesUiState.Content -> true
    }
}

internal fun shouldShowPatchNoteFeatures(features: List<PatchNoteFeature>): Boolean {
    return !features.all { it.title.isEmpty() }
}

internal fun shouldShowPatchNoteFeatureDescription(description: String?): Boolean {
    return description != null && description.isNotEmpty()
}

internal fun shouldShowPatchNoteTitle(title: String): Boolean {
    return title.isNotEmpty()
}

internal fun patchNoteListKey(index: Int, note: PatchNote): String {
    return "$index-${note.version}-${note.date}"
}

@Composable
private fun PatchNoteSection(note: PatchNote) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PatchNoteSectionHorizontalPadding,
                vertical = PatchNoteSectionVerticalPadding,
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(PatchNoteSectionCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(PatchNoteSectionContentPadding),
            verticalArrangement = Arrangement.spacedBy(PatchNoteSectionSpacing),
        ) {
            PatchNoteHeader(note = note)

            if (shouldShowPatchNoteFeatures(note.features)) {
                Column(verticalArrangement = Arrangement.spacedBy(PatchNoteFeatureGroupSpacing)) {
                    note.features.forEach { feature ->
                        Column(verticalArrangement = Arrangement.spacedBy(PatchNoteFeatureItemSpacing)) {
                            FeatureTitleRow(title = feature.title)
                            FeatureDescription(description = feature.description)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatchNoteHeader(note: PatchNote) {
    Column(
        modifier = Modifier.padding(start = PatchNoteHeaderLeadingPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = note.version,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = note.date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = PatchNoteDateTrailingPadding),
            )
        }
        if (shouldShowPatchNoteTitle(note.title)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun FeatureTitleRow(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(PatchNoteFeatureTitleIconSize),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FeatureDescription(description: String?) {
    description
        ?.takeIf(::shouldShowPatchNoteFeatureDescription)
        ?.split("\n")
        ?.forEach { line ->
            Row(
                modifier = Modifier.padding(start = PatchNoteFeatureDescriptionLeadingPadding),
                horizontalArrangement = Arrangement.spacedBy(PatchNoteFeatureDescriptionBulletSpacing),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        Spacer(modifier = Modifier.height(PatchNotesEmptyStateSpacing))
        Text(
            text = stringResource(R.string.patch_notes_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.FindInPage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(PatchNotesEmptyStateIconSize),
        )
        Spacer(modifier = Modifier.height(PatchNotesEmptyStateSpacing))
        Text(
            text = stringResource(R.string.patch_notes_error_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(PatchNotesEmptyStateSpacing))
        Text(
            text = stringResource(R.string.patch_notes_error_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
