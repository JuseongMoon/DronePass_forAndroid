package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeDetailSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedListScreen(
    onNavigateToMapWithShape: (String) -> Unit = {},
    viewModel: SavedListViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
    val notStartedShapes by viewModel.notStartedShapes.collectAsStateWithLifecycle()
    val activeShapes by viewModel.activeFilteredShapes.collectAsStateWithLifecycle()
    val expiredShapes by viewModel.expiredShapes.collectAsStateWithLifecycle()
    val selectedShapeId by viewModel.selectedShapeId.collectAsStateWithLifecycle()
    val selectedShape by viewModel.selectedShape.collectAsStateWithLifecycle()
    val showShapeDetail by viewModel.showShapeDetail.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val activeDrones by viewModel.activeDrones.collectAsStateWithLifecycle()
    val selectedDroneFilter by viewModel.selectedDroneFilter.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // 검색 바
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery
        )

        // 드론 필터 칩
        DroneFilterChips(
            drones = activeDrones,
            selectedDroneFilter = selectedDroneFilter,
            onFilterChange = viewModel::updateDroneFilter
        )

        // 정렬 컨트롤
        SortControls(
            sortOption = sortOption,
            sortDirection = sortDirection,
            totalCount = totalCount,
            onSortOptionChange = viewModel::updateSortOption,
            onToggleDirection = viewModel::toggleSortDirection
        )

        // 도형 리스트
        if (totalCount == 0) {
            EmptyState(hasSearchQuery = searchQuery.isNotBlank())
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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

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

/**
 * 드론 필터 칩 행
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DroneFilterChips(
    drones: List<com.ScienceFiction.DronePassAndroid.domain.model.DroneModel>,
    selectedDroneFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    // 드론이 없으면 필터 칩을 표시하지 않음
    if (drones.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "전체" 칩
        item {
            FilterChip(
                selected = selectedDroneFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text(stringResource(R.string.saved_drone_filter_all)) }
            )
        }

        // 각 드론별 필터 칩
        items(drones, key = { it.id }) { drone ->
            val droneColor = PaletteColor.fromHex(drone.color)?.composeColor
                ?: Color(android.graphics.Color.parseColor(drone.color))

            FilterChip(
                selected = selectedDroneFilter == drone.id,
                onClick = { onFilterChange(drone.id) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(droneColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(drone.name)
                    }
                }
            )
        }

        // "미연결" 칩
        item {
            FilterChip(
                selected = selectedDroneFilter == "",
                onClick = { onFilterChange("") },
                label = { Text(stringResource(R.string.saved_drone_filter_no_drone)) }
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.saved_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.saved_search_icon)
            )
        },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.saved_clear_search)
                    )
                }
            }
        },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortControls(
    sortOption: SortOption,
    sortDirection: SortDirection,
    totalCount: Int,
    onSortOptionChange: (SortOption) -> Unit,
    onToggleDirection: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.saved_total_count, totalCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                FilterChip(
                    selected = true,
                    onClick = { showSortMenu = true },
                    label = { Text(sortOption.label) }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    color = if (option == sortOption)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSortOptionChange(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = onToggleDirection) {
                Icon(
                    imageVector = if (sortDirection == SortDirection.ASCENDING)
                        Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = sortDirection.label,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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
private fun EmptyState(hasSearchQuery: Boolean) {
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
                text = if (hasSearchQuery) stringResource(R.string.saved_empty_with_search) else stringResource(R.string.saved_empty_no_search),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasSearchQuery) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.saved_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
