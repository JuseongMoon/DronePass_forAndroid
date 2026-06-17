package com.ScienceFiction.DronePassAndroid.feature.drone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor

/**
 * 드론 상세 BottomSheet.
 * iOS DroneDetailView 와 동일하게 상단 더보기 메뉴 + 섹션형 상세 정보로 구성한다.
 */
internal val DroneDetailNavigationHeaderHeight = 44.dp
internal val DroneDetailNavigationHeaderSideWidth = 44.dp
internal val DroneDetailMoreCircleSize = 24.dp
internal val DroneDetailMoreCircleStrokeWidth = 1.5.dp
internal val DroneDetailMoreDotsSize = 18.dp
internal val DroneMoveTargetNavigationHeaderHeight = 44.dp
internal val DroneMoveTargetNavigationHeaderSideWidth = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneDetailSheet(
    drone: DroneModel,
    activeDrones: List<DroneModel>,
    getShapeCount: suspend (String) -> Int,
    onEdit: () -> Unit,
    onDelete: (ShapeHandling) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveTargetSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var shapeCount by remember { mutableStateOf<Int?>(null) }
    var showCopyToast by remember { mutableStateOf(false) }
    var copyToastMessage by remember { mutableStateOf("") }
    var copyToastGeneration by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.drone_detail_copied)

    fun copyAndShowToast(text: String) {
        copyToClipboard(context, text)
        hapticFeedback.performHapticFeedback(DroneDetailCopyHapticFeedbackType)
        copyToastMessage = copiedMessage
        showCopyToast = true
        copyToastGeneration += 1
    }

    LaunchedEffect(copyToastGeneration) {
        if (copyToastGeneration == 0) return@LaunchedEffect
        delay(DroneDetailCopyToastDurationMs)
        showCopyToast = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // iOS NavigationView inline title 정합: 좌우 슬롯 폭을 같게 둬 제목을 가운데 고정한다.
                DroneDetailNavigationHeader(
                    menuExpanded = showMoreMenu,
                    onMenuExpandedChange = { showMoreMenu = it },
                    onEdit = {
                        showMoreMenu = false
                        onEdit()
                    },
                    onDelete = {
                        showMoreMenu = false
                        coroutineScope.launch {
                            shapeCount = getShapeCount(drone.id)
                            showDeleteDialog = true
                        }
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    DroneDetailTextRow(
                        label = stringResource(R.string.drone_detail_name),
                        value = drone.name,
                        copyText = drone.name,
                        onCopy = ::copyAndShowToast,
                        valueFontWeight = FontWeight.Medium,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    DroneDetailSectionHeader(text = stringResource(R.string.drone_detail_section_basic))
                    val colorLabel = drone.paletteColor?.localizedLabel()
                    DroneDetailColorRow(
                        label = stringResource(R.string.drone_detail_color),
                        color = drone.paletteColor,
                        colorLabel = colorLabel,
                        onCopy = ::copyAndShowToast,
                    )
                    HorizontalDivider()
                    DroneDetailBlockRow(
                        label = stringResource(R.string.drone_detail_serial_number),
                        value = droneDetailOptionalText(
                            drone.serialNumber,
                            emptyFallback = stringResource(R.string.drone_detail_not_entered),
                        ),
                        copyText = drone.serialNumber,
                        isPlaceholder = isDroneDetailPlaceholder(drone.serialNumber),
                        onCopy = ::copyAndShowToast,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    DroneDetailSectionHeader(text = stringResource(R.string.drone_detail_section_specs))
                    DroneDetailBlockRow(
                        label = stringResource(R.string.drone_detail_takeoff_weight),
                        value = droneDetailOptionalText(
                            drone.takeoffWeight,
                            emptyFallback = stringResource(R.string.drone_detail_not_entered),
                        ),
                        copyText = drone.takeoffWeight,
                        isPlaceholder = isDroneDetailPlaceholder(drone.takeoffWeight),
                        onCopy = ::copyAndShowToast,
                    )
                    HorizontalDivider()
                    DroneDetailBlockRow(
                        label = stringResource(R.string.drone_detail_size),
                        value = droneDetailOptionalText(
                            drone.size,
                            emptyFallback = stringResource(R.string.drone_detail_not_entered),
                        ),
                        copyText = drone.size,
                        isPlaceholder = isDroneDetailPlaceholder(drone.size),
                        onCopy = ::copyAndShowToast,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    DroneDetailSectionHeader(text = stringResource(R.string.common_memo))
                    val memoText = droneDetailOptionalText(
                        drone.memo,
                        emptyFallback = stringResource(R.string.drone_detail_memo_empty),
                    )
                    Text(
                        text = memoText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDroneDetailPlaceholder(drone.memo)) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .copyOnLongPress(drone.memo, ::copyAndShowToast)
                            .padding(vertical = 10.dp),
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            DroneDetailCopyToast(
                visible = showCopyToast,
                message = copyToastMessage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showDeleteDialog) {
        val resolvedShapeCount = shapeCount
        if (resolvedShapeCount != null) {
            when (resolveDroneDeleteDialogType(resolvedShapeCount)) {
                DroneDeleteDialogType.ConfirmDelete -> {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringResource(R.string.drone_detail_delete_title)) },
                        text = {
                            Text(stringResource(R.string.drone_detail_delete_message, drone.name))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    onDelete(ShapeHandling.DeleteAll)
                                }
                            ) {
                                Text(
                                    stringResource(R.string.common_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(stringResource(R.string.common_cancel))
                            }
                        },
                    )
                }
                DroneDeleteDialogType.ShapeHandling -> {
                    DroneDeleteWithShapesActionSheet(
                        drone = drone,
                        shapeCount = resolvedShapeCount,
                        onMoveToOtherDrone = {
                            showDeleteDialog = false
                            showMoveTargetSheet = true
                        },
                        onDeleteAll = {
                            showDeleteDialog = false
                            onDelete(ShapeHandling.DeleteAll)
                        },
                        onDismiss = {
                            showDeleteDialog = false
                        }
                    )
                }
            }
        }
    }

    if (showMoveTargetSheet) {
        DroneMoveTargetSheet(
            drones = activeDrones.filter { it.id != drone.id },
            onConfirm = { targetDroneId ->
                showMoveTargetSheet = false
                onDelete(ShapeHandling.Reassign(targetDroneId))
            },
            onDismiss = { showMoveTargetSheet = false },
        )
    }
}

/**
 * iOS DroneDetailView NavigationView inline title + trailing ellipsis.circle 정합.
 */
@Composable
private fun DroneDetailNavigationHeader(
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DroneDetailNavigationHeaderHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(DroneDetailNavigationHeaderSideWidth))
        Text(
            text = stringResource(R.string.drone_detail_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(DroneDetailNavigationHeaderSideWidth),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = { onMenuExpandedChange(true) },
                modifier = Modifier.size(DroneDetailNavigationHeaderSideWidth),
            ) {
                DroneDetailEllipsisCircleIcon(
                    contentDescription = stringResource(R.string.drone_detail_more_menu),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.common_edit)) },
                    onClick = onEdit,
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.common_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun DroneDetailEllipsisCircleIcon(
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(DroneDetailMoreCircleSize)
            .border(
                BorderStroke(
                    width = DroneDetailMoreCircleStrokeWidth,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DroneDetailMoreDotsSize),
        )
    }
}

internal enum class DroneDeleteDialogType {
    ConfirmDelete,
    ShapeHandling,
}

internal fun resolveDroneDeleteDialogType(shapeCount: Int): DroneDeleteDialogType {
    return when {
        shapeCount == 0 -> DroneDeleteDialogType.ConfirmDelete
        else -> DroneDeleteDialogType.ShapeHandling
    }
}

internal fun shouldShowDroneDeleteShapeHandlingActionSheet(shapeCount: Int?): Boolean =
    shapeCount != null && resolveDroneDeleteDialogType(shapeCount) == DroneDeleteDialogType.ShapeHandling

internal const val DroneDeleteShapeHandlingSkipPartiallyExpanded = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DroneDeleteWithShapesActionSheet(
    drone: DroneModel,
    shapeCount: Int,
    onMoveToOtherDrone: () -> Unit,
    onDeleteAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = DroneDeleteShapeHandlingSkipPartiallyExpanded,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.drone_detail_shape_handling_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = pluralStringResource(
                    R.plurals.drone_detail_delete_with_shapes_message,
                    shapeCount,
                    drone.name,
                    shapeCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            DroneDeleteOptionRow(
                text = stringResource(R.string.drone_detail_reassign, shapeCount),
                onClick = onMoveToOtherDrone,
            )
            HorizontalDivider()
            DroneDeleteOptionRow(
                text = stringResource(R.string.drone_detail_delete_all_shapes, shapeCount),
                onClick = onDeleteAll,
                isDestructive = true,
            )
            HorizontalDivider()
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    }
}

@Composable
private fun DroneDeleteOptionRow(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDestructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DroneMoveTargetSheet(
    drones: List<DroneModel>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDroneId by remember(drones) { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            DroneMoveTargetNavigationHeader(
                canConfirm = selectedDroneId != null,
                onCancel = onDismiss,
                onConfirm = {
                    selectedDroneId?.let(onConfirm)
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DroneDetailSectionHeader(text = stringResource(R.string.drone_select_move_shape))
                drones.forEachIndexed { index, targetDrone ->
                    DroneMoveTargetRow(
                        drone = targetDrone,
                        isSelected = selectedDroneId == targetDrone.id,
                        onClick = { selectedDroneId = targetDrone.id },
                    )
                    if (index != drones.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 40.dp))
                    }
                }
            }
        }
    }
}

/**
 * iOS DroneSelectionSheet inline title + leading/trailing toolbar 정합.
 */
@Composable
private fun DroneMoveTargetNavigationHeader(
    canConfirm: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DroneMoveTargetNavigationHeaderHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(DroneMoveTargetNavigationHeaderSideWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.common_cancel))
            }
        }
        Text(
            text = stringResource(R.string.drone_select_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(DroneMoveTargetNavigationHeaderSideWidth),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(
                onClick = onConfirm,
                enabled = canConfirm,
            ) {
                Text(stringResource(R.string.drone_select_confirm))
            }
        }
    }
}

@Composable
private fun DroneMoveTargetRow(
    drone: DroneModel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        drone.paletteColor?.takeIf(::shouldShowDroneMoveTargetColorIndicator)?.let { paletteColor ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(paletteColor.composeColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape,
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = drone.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.drone_select_selected, drone.name),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal fun shouldShowDroneMoveTargetColorIndicator(color: PaletteColor?): Boolean = color != null

internal fun droneDetailOptionalText(value: String?, emptyFallback: String): String =
    value ?: emptyFallback

internal fun isDroneDetailPlaceholder(value: String?): Boolean = value == null

internal fun copyableDroneDetailText(text: String?): String? =
    text?.takeIf { it.isNotEmpty() }

@Composable
private fun DroneDetailSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun DroneDetailTextRow(
    label: String,
    value: String,
    copyText: String? = null,
    onCopy: (String) -> Unit,
    valueFontWeight: FontWeight = FontWeight.Normal,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .copyOnLongPress(copyText, onCopy)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueFontWeight,
        )
    }
}

@Composable
private fun DroneDetailColorRow(
    label: String,
    color: PaletteColor?,
    colorLabel: String?,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .copyOnLongPress(colorLabel, onCopy)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        color?.let { paletteColor ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(paletteColor.composeColor)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = colorLabel.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DroneDetailBlockRow(
    label: String,
    value: String,
    copyText: String? = null,
    isPlaceholder: Boolean,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .copyOnLongPress(copyText, onCopy)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaceholder) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.copyOnLongPress(
    text: String?,
    onCopy: (String) -> Unit,
): Modifier {
    val copyText = copyableDroneDetailText(text) ?: return this
    return combinedClickable(
        onClick = {},
        onLongClick = { onCopy(copyText) },
    )
}

internal const val DroneDetailCopyToastDurationMs = 1_500L
internal const val DroneDetailCopyToastAnimationDurationMs = 300
internal const val DroneDetailCopyToastBackgroundAlpha = 0.75f
internal val DroneDetailCopyToastBottomPadding = 50.dp
internal val DroneDetailCopyHapticFeedbackType = HapticFeedbackType.LongPress

@Composable
private fun DroneDetailCopyToast(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.padding(bottom = DroneDetailCopyToastBottomPadding),
        enter = slideInVertically(
            animationSpec = tween(DroneDetailCopyToastAnimationDurationMs),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(DroneDetailCopyToastAnimationDurationMs)),
        exit = slideOutVertically(
            animationSpec = tween(DroneDetailCopyToastAnimationDurationMs),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(DroneDetailCopyToastAnimationDurationMs)),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = DroneDetailCopyToastBackgroundAlpha),
                    shape = CircleShape,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    val clip = ClipData.newPlainText(context.getString(R.string.drone_detail_clipboard_label), text)
    clipboardManager.setPrimaryClip(clip)
}
