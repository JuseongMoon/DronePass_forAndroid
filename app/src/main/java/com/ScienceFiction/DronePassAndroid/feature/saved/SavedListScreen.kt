package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeDetailSheet
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedListScreen(
    onNavigateToMapWithShape: (String) -> Unit = {},
    onNavigateToMapForEdit: (String) -> Unit = {},
    onNavigateToMapForDuplicate: (String) -> Unit = {},
    selectionShapeId: String? = null,
    onSelectionConsumed: () -> Unit = {},
    focusShapeId: String? = null,
    onFocusConsumed: () -> Unit = {},
    viewModel: SavedListViewModel = hiltViewModel()
) {
    val notStartedShapes by viewModel.notStartedShapes.collectAsStateWithLifecycle()
    val activeShapes by viewModel.activeFilteredShapes.collectAsStateWithLifecycle()
    val expiredShapes by viewModel.expiredShapes.collectAsStateWithLifecycle()
    val selectedShapeId by viewModel.selectedShapeId.collectAsStateWithLifecycle()
    val selectedShape by viewModel.selectedShape.collectAsStateWithLifecycle()
    val showShapeDetail by viewModel.showShapeDetail.collectAsStateWithLifecycle()
    val activeShapeIds by viewModel.activeShapeIds.collectAsStateWithLifecycle()
    val activeDrones by viewModel.activeDrones.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val allShapeCount by viewModel.allShapeCount.collectAsStateWithLifecycle()
    val selectedDroneCount by viewModel.selectedDroneCount.collectAsStateWithLifecycle()
    val koreaFeaturesEnabled by viewModel.koreaFeaturesEnabled.collectAsStateWithLifecycle()
    val showShapeEdit by viewModel.showShapeEdit.collectAsStateWithLifecycle()
    val isDuplicateMode by viewModel.isDuplicateMode.collectAsStateWithLifecycle()
    val shapeEditDefaults by viewModel.shapeEditDefaults.collectAsStateWithLifecycle()
    val primarySelectedDroneId by viewModel.primarySelectedDroneId.collectAsStateWithLifecycle()
    val defaultShapeColor by viewModel.defaultShapeColor.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var internalFocusShapeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectionShapeId) {
        val targetId = selectionShapeId ?: return@LaunchedEffect
        viewModel.selectShapeForMapFocus(targetId)
        onSelectionConsumed()
    }

    LaunchedEffect(focusShapeId, internalFocusShapeId, activeShapes, notStartedShapes, expiredShapes, activeShapeIds) {
        val targetId = resolveSavedListFocusTarget(
            externalFocusShapeId = focusShapeId,
            internalFocusShapeId = internalFocusShapeId,
        ) ?: return@LaunchedEffect
        val focusConsumption = resolveSavedListFocusConsumption(
            externalFocusShapeId = focusShapeId,
            internalFocusShapeId = internalFocusShapeId,
            targetShapeId = targetId,
        )
        val targetIndex = findLazyListIndex(
            shapeId = targetId,
            activeShapes = activeShapes,
            notStartedShapes = notStartedShapes,
            expiredShapes = expiredShapes,
        )

        if (targetIndex == null) {
            if (shouldConsumeMissingFocus(
                    shapeId = targetId,
                    allShapeCount = allShapeCount,
                    activeShapeIds = activeShapeIds,
                    activeShapes = activeShapes,
                    notStartedShapes = notStartedShapes,
                    expiredShapes = expiredShapes,
                )
            ) {
                if (focusConsumption.clearInternalFocus) {
                    internalFocusShapeId = null
                }
                if (focusConsumption.consumeExternalFocus) {
                    onFocusConsumed()
                }
            }
            return@LaunchedEffect
        }

        delay(SavedListFocusSelectionDelayMs)
        viewModel.selectShapeForMapFocus(targetId)
        delay(SavedListFocusScrollDelayMs)
        listState.animateScrollToItem(targetIndex)
        if (focusConsumption.clearInternalFocus) {
            internalFocusShapeId = null
        }
        if (focusConsumption.consumeExternalFocus) {
            onFocusConsumed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedShapeFocusEvent.collect { shapeId ->
            internalFocusShapeId = shapeId
            onNavigateToMapWithShape(shapeId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = SavedListTopOffset),
    ) {
        // iOS SavedTableListView 동등 — 검색바/드론 필터/정렬 컨트롤은 화면에 없다.
        // 정렬은 SavedListOverlay 헤더의 정렬 칩(파랑/주황)에서, 드론 선택은 글로벌 드론
        // 매니저(지도 드론 드롭다운)에서 처리되며, 검색 기능은 iOS 에도 존재하지 않는다.

        // 도형 리스트
        if (totalCount == 0) {
            EmptyState(
                hasShapes = allShapeCount > 0,
                selectedDroneCount = selectedDroneCount
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = SavedListContentHorizontalPadding,
                    vertical = SavedListContentVerticalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(SavedListRowVerticalSpacing),
            ) {
                // 시작 전 도형 섹션
                if (notStartedShapes.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.saved_section_not_started)
                        )
                    }
                    // 섹션 prefix 를 키에 포함하여 활성/시작전/만료 분류가 흔들리는 경계 시각
                    // (예: flightStartDate == flightEndDate == 현재시각) 등 잠재적 race 에서
                    // 동일 id 가 두 섹션에 동시 등장 시 LazyColumn 이
                    // IllegalStateException("Key was already used") 으로 크래시하는 것을 방지.
                    items(notStartedShapes, key = { "notStarted-${it.id}" }) { shape ->
                        SwipeToDeleteItem(
                            onDelete = { viewModel.deleteShapeFromList(shape) }
                        ) {
                            SavedShapeListItem(
                                shape = shape,
                                isSelected = shape.id == selectedShapeId,
                                onClick = {
                                    viewModel.selectShapeForMapFocus(shape.id)
                                    onNavigateToMapWithShape(shape.id)
                                },
                                onDetailClick = { viewModel.onShapeSelected(shape.id) },
                            )
                        }
                    }
                }

                // 활성 도형 섹션
                if (activeShapes.isNotEmpty()) {
                    item {
                        if (notStartedShapes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(SavedListSectionSpacing))
                        }
                        SectionHeader(
                            title = stringResource(R.string.saved_section_active)
                        )
                    }
                    items(activeShapes, key = { "active-${it.id}" }) { shape ->
                        SwipeToDeleteItem(
                            onDelete = { viewModel.deleteShapeFromList(shape) }
                        ) {
                            SavedShapeListItem(
                                shape = shape,
                                isSelected = shape.id == selectedShapeId,
                                onClick = {
                                    viewModel.selectShapeForMapFocus(shape.id)
                                    onNavigateToMapWithShape(shape.id)
                                },
                                onDetailClick = { viewModel.onShapeSelected(shape.id) },
                            )
                        }
                    }
                }

                // 만료 도형 섹션
                if (expiredShapes.isNotEmpty()) {
                    item {
                        if (activeShapes.isNotEmpty() || notStartedShapes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(SavedListSectionSpacing))
                        }
                        SectionHeader(
                            title = stringResource(R.string.saved_section_expired)
                        )
                    }
                    items(expiredShapes, key = { "expired-${it.id}" }) { shape ->
                        SwipeToDeleteItem(
                            onDelete = { viewModel.deleteShapeFromList(shape) }
                        ) {
                            SavedShapeListItem(
                                shape = shape,
                                isSelected = shape.id == selectedShapeId,
                                onClick = {
                                    viewModel.selectShapeForMapFocus(shape.id)
                                    onNavigateToMapWithShape(shape.id)
                                },
                                onDetailClick = { viewModel.onShapeSelected(shape.id) },
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
                    viewModel.onEditShapeRequested(shape)
                },
                onDelete = {
                    viewModel.deleteShapeFromDetail(shape)
                },
                onDismiss = {
                    viewModel.dismissShapeDetail()
                },
                onDuplicate = {
                    viewModel.onDuplicateRequested(shape)
                },
                drone = viewModel.getDroneById(shape.droneId),
                activeDrones = activeDrones,
                koreaFeaturesEnabled = koreaFeaturesEnabled,
            )
        }
    }

    if (showShapeEdit) {
        selectedShape?.let { shape ->
            ShapeEditScreen(
                shape = shape,
                drones = activeDrones,
                editDefaults = shapeEditDefaults,
                fallbackSelectedDroneId = primarySelectedDroneId,
                defaultShapeColor = defaultShapeColor,
                reverseGeocodedAddress = null,
                geocodingApi = viewModel.naverGeocodingApi,
                isDuplicateMode = isDuplicateMode,
                onPersistEditDefaults = viewModel::saveShapeEditDefaults,
                onDateOnlyModeChanged = viewModel::setShapeEditDateOnlyMode,
                onSave = { updatedShape, originalShapeAtEditStart, onSaveFailed ->
                    viewModel.saveShape(
                        shape = updatedShape,
                        isDuplicate = isDuplicateMode,
                        originalShapeAtEditStart = originalShapeAtEditStart,
                        onFailure = onSaveFailed,
                    )
                },
                onDismiss = {
                    viewModel.dismissShapeEdit()
                },
            )
        }
    }
}

internal fun findLazyListIndex(
    shapeId: String,
    activeShapes: List<ShapeModel>,
    notStartedShapes: List<ShapeModel>,
    expiredShapes: List<ShapeModel>,
): Int? {
    var index = 0

    if (notStartedShapes.isNotEmpty()) {
        val shapeIndex = notStartedShapes.indexOfFirst { it.id == shapeId }
        if (shapeIndex >= 0) return index + 1 + shapeIndex
        index += 1 + notStartedShapes.size
    }

    if (activeShapes.isNotEmpty()) {
        val shapeIndex = activeShapes.indexOfFirst { it.id == shapeId }
        if (shapeIndex >= 0) return index + 1 + shapeIndex
        index += 1 + activeShapes.size
    }

    if (expiredShapes.isNotEmpty()) {
        val shapeIndex = expiredShapes.indexOfFirst { it.id == shapeId }
        if (shapeIndex >= 0) return index + 1 + shapeIndex
    }

    return null
}

internal fun resolveSavedListFocusTarget(
    externalFocusShapeId: String?,
    internalFocusShapeId: String?,
): String? = externalFocusShapeId ?: internalFocusShapeId

internal data class SavedListFocusConsumption(
    val consumeExternalFocus: Boolean,
    val clearInternalFocus: Boolean,
)

internal fun resolveSavedListFocusConsumption(
    externalFocusShapeId: String?,
    internalFocusShapeId: String?,
    targetShapeId: String,
): SavedListFocusConsumption {
    return SavedListFocusConsumption(
        consumeExternalFocus = externalFocusShapeId == targetShapeId,
        clearInternalFocus = internalFocusShapeId == targetShapeId,
    )
}

internal fun shouldConsumeMissingFocus(
    shapeId: String,
    allShapeCount: Int,
    activeShapeIds: Set<String>,
    activeShapes: List<ShapeModel>,
    notStartedShapes: List<ShapeModel>,
    expiredShapes: List<ShapeModel>,
): Boolean {
    if (allShapeCount == 0) return false
    if (shapeId in activeShapeIds) return true
    return activeShapeIds.isNotEmpty() ||
        activeShapes.isNotEmpty() ||
        notStartedShapes.isNotEmpty() ||
        expiredShapes.isNotEmpty()
}

internal fun shouldDeleteSavedShapeOnSwipe(dismissValue: SwipeToDismissBoxValue): Boolean {
    return dismissValue == SwipeToDismissBoxValue.EndToStart
}

internal fun shouldShowSavedShapeDeleteBackground(dismissDirection: SwipeToDismissBoxValue): Boolean {
    return dismissDirection == SwipeToDismissBoxValue.EndToStart
}

/**
 * 스와이프하여 삭제 가능한 아이템 래퍼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (shouldDeleteSavedShapeOnSwipe(dismissValue)) {
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
            val showDeleteBackground = shouldShowSavedShapeDeleteBackground(dismissState.dismissDirection)
            // 삭제 배경 (오른쪽에서 왼쪽 스와이프 시 표시)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (showDeleteBackground) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (showDeleteBackground) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SavedListSectionHeaderMinHeight)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal val SavedListSectionHeaderMinHeight = 40.dp
internal val SavedListTopOffset = (-10).dp
internal val SavedListContentHorizontalPadding = 16.dp
internal val SavedListContentVerticalPadding = 8.dp
internal val SavedListRowVerticalSpacing = 0.dp
internal val SavedListSectionSpacing = 8.dp
internal const val SavedListFocusSelectionDelayMs = 200L
internal const val SavedListFocusScrollDelayMs = 100L

internal enum class SavedListEmptyState {
    NO_SHAPES,
    NO_DRONE_SELECTED,
    NO_MATCHING_SHAPES,
}

internal enum class SavedListEmptyIconStyle {
    INBOX,
    DRONE,
    SEARCH,
}

internal val SavedListEmptyIconSize = 48.dp
internal val SavedListEmptyVerticalSpacing = 16.dp
internal val SavedListEmptySecondaryColor = Color(0xFF8E8E93)
@DrawableRes
internal val SavedListEmptyDroneIconRes = R.drawable.ic_drone

internal fun resolveSavedListEmptyState(
    hasShapes: Boolean,
    selectedDroneCount: Int,
): SavedListEmptyState {
    return when {
        !hasShapes -> SavedListEmptyState.NO_SHAPES
        selectedDroneCount == 0 -> SavedListEmptyState.NO_DRONE_SELECTED
        else -> SavedListEmptyState.NO_MATCHING_SHAPES
    }
}

internal fun resolveSavedListEmptyIconStyle(state: SavedListEmptyState): SavedListEmptyIconStyle {
    return when (state) {
        SavedListEmptyState.NO_SHAPES -> SavedListEmptyIconStyle.INBOX
        SavedListEmptyState.NO_DRONE_SELECTED -> SavedListEmptyIconStyle.DRONE
        SavedListEmptyState.NO_MATCHING_SHAPES -> SavedListEmptyIconStyle.SEARCH
    }
}

@Composable
private fun EmptyState(
    hasShapes: Boolean,
    selectedDroneCount: Int
) {
    val state = resolveSavedListEmptyState(
        hasShapes = hasShapes,
        selectedDroneCount = selectedDroneCount,
    )
    val title = when (state) {
        SavedListEmptyState.NO_SHAPES -> stringResource(R.string.saved_empty_no_search)
        SavedListEmptyState.NO_DRONE_SELECTED -> stringResource(R.string.saved_empty_no_drone_selected)
        SavedListEmptyState.NO_MATCHING_SHAPES -> stringResource(R.string.saved_empty_no_matching_shapes)
    }
    val description = when (state) {
        SavedListEmptyState.NO_SHAPES -> stringResource(R.string.saved_empty_hint)
        SavedListEmptyState.NO_DRONE_SELECTED -> stringResource(R.string.saved_empty_no_drone_selected_hint)
        SavedListEmptyState.NO_MATCHING_SHAPES -> stringResource(R.string.saved_empty_no_matching_shapes_hint)
    }
    val iconStyle = resolveSavedListEmptyIconStyle(state)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SavedListEmptyVerticalSpacing),
        ) {
            when (iconStyle) {
                SavedListEmptyIconStyle.INBOX -> Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(SavedListEmptyIconSize),
                    tint = SavedListEmptySecondaryColor,
                )
                SavedListEmptyIconStyle.DRONE -> Icon(
                    painter = painterResource(SavedListEmptyDroneIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(SavedListEmptyIconSize),
                    tint = SavedListEmptySecondaryColor,
                )
                SavedListEmptyIconStyle.SEARCH -> Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(SavedListEmptyIconSize),
                    tint = SavedListEmptySecondaryColor,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = SavedListEmptySecondaryColor,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = SavedListEmptySecondaryColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
