package com.ScienceFiction.DronePassAndroid.feature.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRowMinHeight
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleContentGap
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleHeader
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondarySystemGroupedBackground
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

/**
 * 설정 > 태그 관리. 활성 태그 목록(도형 수), 새 태그 추가, 사용법. iOS TagListView 정합.
 * 행을 누르면 이름을 바꾸고, 왼쪽으로 밀면 삭제 확인을 띄운다(태그를 단 도형은 태그 없음이 된다).
 */
@Composable
fun TagListScreen(viewModel: TagViewModel = hiltViewModel()) {
    val tags by viewModel.activeTags.collectAsStateWithLifecycle()
    val shapeCounts by viewModel.shapeCounts.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<TagModel?>(null) }
    var deletingTag by remember { mutableStateOf<TagModel?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(title = stringResource(R.string.tag_list_title))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = SheetLargeTitleContentGap, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(InsetGroupedSectionSpacing),
        ) {
            item {
                InsetGroupedSection(
                    header = stringResource(R.string.tag_list_section_my),
                    footer = if (tags.isEmpty()) stringResource(R.string.tag_list_empty) else null,
                ) {
                    tags.forEachIndexed { index, tag ->
                        // 행마다 스와이프 상태를 따로 둔다(삭제 뒤 같은 자리의 다음 행이 밀린 채 남지 않게)
                        androidx.compose.runtime.key(tag.id) {
                            TagSwipeRow(onDeleteRequest = { deletingTag = tag }) {
                                TagListItem(
                                    tag = tag,
                                    shapeCount = shapeCounts[tag.id] ?: 0,
                                    onClick = { editingTag = tag },
                                )
                            }
                        }
                        if (index != tags.lastIndex) InsetGroupedDivider(startIndent = 46.dp)
                    }
                }
            }
            item {
                InsetGroupedSection {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.requestAdd { showAddSheet = true } }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tag_list_add_new),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            item {
                InsetGroupedSection(header = stringResource(R.string.drone_list_section_usage)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(R.string.tag_list_usage_one, R.string.tag_list_usage_filter, R.string.tag_list_usage_delete)
                            .forEach { res ->
                                Text(
                                    text = stringResource(res),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        TagEditSheet(tag = null, viewModel = viewModel, onDismiss = { showAddSheet = false })
    }
    editingTag?.let { tag ->
        TagEditSheet(tag = tag, viewModel = viewModel, onDismiss = { editingTag = null })
    }
    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text(stringResource(R.string.tag_delete_title, tag.name)) },
            text = { Text(stringResource(R.string.tag_delete_message, shapeCounts[tag.id] ?: 0)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag.id)
                        deletingTag = null
                    },
                ) {
                    Text(stringResource(R.string.tag_list_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun TagListItem(tag: TagModel, shapeCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IosSecondarySystemGroupedBackground)
            .clickable(onClick = onClick)
            .heightIn(min = InsetGroupedRowMinHeight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Sell,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.tag_list_shape_count, shapeCount),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 왼쪽으로 밀면 삭제 확인을 요청한다. 행은 제자리로 돌아온다(iOS swipeActions, allowsFullSwipe=false 대응) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSwipeRow(onDeleteRequest: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.4f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val active = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (active) MaterialTheme.colorScheme.error else Color.Transparent)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (active) {
                    Text(text = stringResource(R.string.tag_list_delete), color = MaterialTheme.colorScheme.onError)
                }
            }
        },
    ) {
        content()
    }
}
