package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeDetailSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedListScreen(
    onNavigateToMapWithShape: (String) -> Unit = {},
    viewModel: SavedListViewModel = hiltViewModel()
) {
    val notStartedShapes by viewModel.notStartedShapes.collectAsStateWithLifecycle()
    val activeShapes by viewModel.activeFilteredShapes.collectAsStateWithLifecycle()
    val expiredShapes by viewModel.expiredShapes.collectAsStateWithLifecycle()
    val selectedShapeId by viewModel.selectedShapeId.collectAsStateWithLifecycle()
    val selectedShape by viewModel.selectedShape.collectAsStateWithLifecycle()
    val showShapeDetail by viewModel.showShapeDetail.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // iOS SavedTableListView 동등 — 검색바/드론 필터/정렬 컨트롤은 화면에 없다.
        // 정렬은 SavedListOverlay 헤더의 정렬 칩(파랑/주황)에서, 드론 선택은 글로벌 드론
        // 매니저(지도 드론 드롭다운)에서 처리되며, 검색 기능은 iOS 에도 존재하지 않는다.

        // 도형 리스트
        if (totalCount == 0) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 활성 도형 섹션
                if (activeShapes.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.saved_section_active),
                            count = activeShapes.size,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 섹션 prefix 를 키에 포함하여 활성/시작전/만료 분류가 흔들리는 경계 시각
                    // (예: flightStartDate == flightEndDate == 현재시각) 등 잠재적 race 에서
                    // 동일 id 가 두 섹션에 동시 등장 시 LazyColumn 이
                    // IllegalStateException("Key was already used") 으로 크래시하는 것을 방지.
                    items(activeShapes, key = { "active-${it.id}" }) { shape ->
                        SwipeToDeleteItem(
                            shape = shape,
                            onDelete = { viewModel.deleteShape(shape) }
                        ) {
                            SavedShapeListItem(
                                shape = shape,
                                isSelected = shape.id == selectedShapeId,
                                onClick = { viewModel.onShapeSelected(shape.id) },
                                droneName = viewModel.getDroneName(shape.droneId)
                            )
                        }
                    }
                }

                // 시작 전 도형 섹션
                if (notStartedShapes.isNotEmpty()) {
                    item {
                        if (activeShapes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        SectionHeader(
                            title = stringResource(R.string.saved_section_not_started),
                            count = notStartedShapes.size,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    items(notStartedShapes, key = { "notStarted-${it.id}" }) { shape ->
                        SwipeToDeleteItem(
                            shape = shape,
                            onDelete = { viewModel.deleteShape(shape) }
                        ) {
                            SavedShapeListItem(
                                shape = shape,
                                isSelected = shape.id == selectedShapeId,
                                onClick = { viewModel.onShapeSelected(shape.id) },
                                droneName = viewModel.getDroneName(shape.droneId)
                            )
                        }
                    }
                }

                // 만료 도형 섹션
                if (expiredShapes.isNotEmpty()) {
                    item {
                        if (activeShapes.isNotEmpty() || notStartedShapes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        SectionHeader(
                            title = stringResource(R.string.saved_section_expired),
                            count = expiredShapes.size,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    items(expiredShapes, key = { "expired-${it.id}" }) { shape ->
                        SwipeToDeleteItem(
                            shape = shape,
                            onDelete = { viewModel.deleteShape(shape) }
                        ) {
                            SavedShapeListItem(
                                shape = shape,
                                isSelected = shape.id == selectedShapeId,
                                onClick = { viewModel.onShapeSelected(shape.id) },
                                droneName = viewModel.getDroneName(shape.droneId)
                            )
                        }
                    }
                }
            }
        }
    }

    // 도형 상세 BottomSheet
    if (showShapeDetail) {
        selectedShape?.let { shape ->
            ShapeDetailSheet(
                shape = shape,
                onEdit = {
                    viewModel.dismissShapeDetail()
                    onNavigateToMapWithShape(shape.id)
                },
                onDelete = {
                    viewModel.deleteShape(shape)
                },
                onDismiss = {
                    viewModel.dismissShapeDetail()
                },
                droneName = viewModel.getDroneName(shape.droneId)
            )
        }
    }
}

/**
 * 스와이프하여 삭제 가능한 아이템 래퍼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    shape: ShapeModel,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    // 스와이프 → 확인 다이얼로그 → 삭제 흐름. 이전엔 즉시 onDelete 호출되어 실수 삭제 위험.
    var pendingDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                pendingDelete = true
                false  // 다이얼로그에서 확인할 때까지 실제 dismiss 보류
            } else {
                false
            }
        }
    )

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = {
                pendingDelete = false
                scope.launch { dismissState.reset() }
            },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.shape_delete_confirm_message, shape.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDelete = false
                        scope.launch { dismissState.reset() }
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // 삭제 배경 (오른쪽에서 왼쪽 스와이프 시 표시)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = color
        )
        Text(
            text = " ($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.saved_empty_no_search),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.saved_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
