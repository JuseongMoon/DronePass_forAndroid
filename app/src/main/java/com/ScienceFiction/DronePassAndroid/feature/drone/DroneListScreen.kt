package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import androidx.compose.foundation.layout.heightIn
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRowMinHeight
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleHeader
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor

internal val DroneListAddIconSize = 20.dp
internal val DroneListAddIconTextSpacing = 8.dp

/**
 * 드론 관리 목록 화면
 * @param droneViewModel 드론 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneListScreen(
    droneViewModel: DroneViewModel = hiltViewModel()
) {
    val drones by droneViewModel.activeDrones.collectAsStateWithLifecycle()
    val showDroneDetail by droneViewModel.showDroneDetail.collectAsStateWithLifecycle()
    val showDroneEdit by droneViewModel.showDroneEdit.collectAsStateWithLifecycle()
    val selectedDrone by droneViewModel.selectedDrone.collectAsStateWithLifecycle()
    val deleteError by droneViewModel.deleteError.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(InsetGroupedSectionSpacing),
    ) {
        item {
            // iOS DroneListView 는 닫기 버튼 없이 시트를 끌어 내려 닫는다.
            SheetLargeTitleHeader(title = stringResource(R.string.drone_list_title))
        }

        item {
            InsetGroupedSection(
                header = stringResource(R.string.drone_list_section_my),
                footer = if (drones.isEmpty()) stringResource(R.string.drone_list_empty) else null,
            ) {
                drones.forEachIndexed { index, drone ->
                    DroneListItem(
                        drone = drone,
                        onClick = { droneViewModel.selectDrone(drone.id) }
                    )
                    if (index != drones.lastIndex) {
                        InsetGroupedDivider(startIndent = 52.dp)
                    }
                }
            }
        }

        item {
            InsetGroupedSection {
                DroneAddItem(onClick = droneViewModel::requestAddDrone)
            }
        }

        item {
            InsetGroupedSection(header = stringResource(R.string.drone_list_section_usage)) {
                DroneUsageSection()
            }
        }
    }

    // 드론 상세 시트
    val detailDrone = selectedDrone
    if (showDroneDetail && detailDrone != null) {
        DroneDetailSheet(
            drone = detailDrone,
            activeDrones = drones,
            getShapeCount = { droneId ->
                droneViewModel.getShapeCountForDrone(droneId)
            },
            onEdit = {
                droneViewModel.showEditSheet(detailDrone)
            },
            onDelete = { handling ->
                droneViewModel.deleteDrone(detailDrone, handling)
            },
            onDismiss = {
                droneViewModel.dismissDetailSheet()
            }
        )
    }

    // 드론 편집/추가 시트
    if (showDroneEdit) {
        // 매 recomposition 마다 suggestNextColor() / isDuplicateName 람다가 새로 생성되어
        // 내부 List 순회를 반복하던 문제를 메모이즈로 차단. drones 목록이 변할 때만 재계산.
        val suggestedColor = remember(drones) {
            droneViewModel.suggestNextColor(drones)
        }
        val suggestedName = stringResource(R.string.drone_edit_default_name, drones.size + 1)
        val duplicateNameChecker = remember(drones) {
            { name: String, excludeId: String? ->
                droneViewModel.isDuplicateName(name, excludeId, drones)
            }
        }
        DroneEditSheet(
            drone = selectedDrone,
            suggestedName = suggestedName,
            suggestedColor = suggestedColor,
            isDuplicateName = duplicateNameChecker,
            onSave = { drone ->
                if (selectedDrone != null) {
                    droneViewModel.updateDrone(drone)
                    droneViewModel.showDetailSheet(drone)
                } else {
                    droneViewModel.addDrone(
                        name = drone.name,
                        color = drone.color,
                        serialNumber = drone.serialNumber,
                        takeoffWeight = drone.takeoffWeight,
                        size = drone.size,
                        memo = drone.memo
                    )
                    droneViewModel.dismissEditSheet()
                }
            },
            onDismiss = {
                droneViewModel.dismissEditSheet()
            }
        )
    }

    deleteError?.let { error ->
        val message = when (error) {
            is DroneDeleteError.Validation -> when (error.error) {
                DroneDeleteValidationError.CANNOT_DELETE_LAST_DRONE -> {
                    stringResource(R.string.drone_detail_delete_last_drone_error)
                }
                DroneDeleteValidationError.TARGET_DRONE_NOT_FOUND -> {
                    stringResource(R.string.drone_detail_target_drone_not_found_error)
                }
            }
            is DroneDeleteError.Failure -> {
                droneDeleteFailureMessage(error.message)
            }
        }
        AlertDialog(
            onDismissRequest = { droneViewModel.clearDeleteError() },
            title = { Text(stringResource(R.string.common_error)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { droneViewModel.clearDeleteError() }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }
}

/**
 * 드론 목록 아이템
 */
@Composable
private fun DroneListItem(
    drone: DroneModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .heightIn(min = InsetGroupedRowMinHeight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        drone.paletteColor?.takeIf(::shouldShowDroneListColorIndicator)?.let { paletteColor ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(paletteColor.composeColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = drone.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun shouldShowDroneListColorIndicator(color: PaletteColor?): Boolean = color != null

internal fun droneDeleteFailureMessage(message: String): String = message

@Composable
private fun DroneAddItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DroneListAddIconSize),
        )
        Spacer(modifier = Modifier.width(DroneListAddIconTextSpacing))
        Text(
            text = stringResource(R.string.drone_list_add),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DroneUsageSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.drone_list_usage_color_customization),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.drone_list_usage_last_drone),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
