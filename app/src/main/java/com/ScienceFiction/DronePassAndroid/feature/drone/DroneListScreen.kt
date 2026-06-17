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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.drone_list_title),
                        fontWeight = FontWeight.Bold
                    )
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                DroneListSectionHeader(text = stringResource(R.string.drone_list_section_my))
            }

            if (drones.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.drone_list_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            } else {
                itemsIndexed(drones, key = { _, drone -> drone.id }) { index, drone ->
                    DroneListItem(
                        drone = drone,
                        onClick = { droneViewModel.selectDrone(drone.id) }
                    )
                    if (index != drones.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                DroneAddItem(onClick = { droneViewModel.showEditSheet(null) })
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                DroneListSectionHeader(text = stringResource(R.string.drone_list_section_usage))
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

            Spacer(modifier = Modifier.width(8.dp))
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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

@Composable
private fun DroneListSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}
