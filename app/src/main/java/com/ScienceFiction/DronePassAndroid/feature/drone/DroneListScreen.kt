package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

/**
 * 드론 관리 목록 화면
 *
 * @param onBack 뒤로가기 콜백
 * @param droneViewModel 드론 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneListScreen(
    onBack: () -> Unit,
    droneViewModel: DroneViewModel = hiltViewModel()
) {
    val drones by droneViewModel.activeDrones.collectAsStateWithLifecycle()
    val showDroneDetail by droneViewModel.showDroneDetail.collectAsStateWithLifecycle()
    val showDroneEdit by droneViewModel.showDroneEdit.collectAsStateWithLifecycle()
    val selectedDrone by droneViewModel.selectedDrone.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.drone_list_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.drone_list_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    droneViewModel.showEditSheet(null)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.drone_list_add)
                )
            }
        }
    ) { innerPadding ->
        if (drones.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.drone_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.drone_list_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(drones, key = { it.id }) { drone ->
                    DroneListItem(
                        drone = drone,
                        onClick = { droneViewModel.selectDrone(drone.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }

    // 드론 상세 시트
    if (showDroneDetail && selectedDrone != null) {
        DroneDetailSheet(
            drone = selectedDrone!!,
            activeDrones = drones,
            getShapeCount = { droneId ->
                droneViewModel.getShapeCountForDrone(droneId)
            },
            onEdit = {
                droneViewModel.showEditSheet(selectedDrone)
            },
            onDelete = { handling ->
                droneViewModel.deleteDrone(selectedDrone!!, handling)
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
        val duplicateNameChecker = remember(drones) {
            { name: String, excludeId: String? ->
                droneViewModel.isDuplicateName(name, excludeId, drones)
            }
        }
        DroneEditSheet(
            drone = selectedDrone,
            suggestedColor = suggestedColor,
            isDuplicateName = duplicateNameChecker,
            onSave = { drone ->
                if (selectedDrone != null) {
                    droneViewModel.updateDrone(drone)
                } else {
                    droneViewModel.addDrone(
                        name = drone.name,
                        color = drone.color,
                        serialNumber = drone.serialNumber,
                        takeoffWeight = drone.takeoffWeight,
                        size = drone.size,
                        memo = drone.memo
                    )
                }
                droneViewModel.dismissEditSheet()
                droneViewModel.dismissDetailSheet()
            },
            onDismiss = {
                droneViewModel.dismissEditSheet()
            }
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 색상 원형
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    drone.paletteColor?.composeColor
                        ?: MaterialTheme.colorScheme.primary
                )
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 드론 이름
        Text(
            text = drone.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 화살표
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
