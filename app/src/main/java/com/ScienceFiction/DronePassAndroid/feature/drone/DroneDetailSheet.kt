package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor

/**
 * 드론 상세 BottomSheet
 *
 * @param drone 표시할 드론
 * @param activeDrones 활성 드론 목록 (삭제 시 재할당 대상)
 * @param getShapeCount 드론별 도형 수 조회 함수
 * @param onEdit 편집 요청 콜백
 * @param onDelete 삭제 요청 콜백
 * @param onDismiss 닫기 콜백
 */
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteDialog by remember { mutableStateOf(false) }
    // null = 로딩 중 (UI 에서 "..." 또는 빈 문자열). 0 = 도형 없음. 이전(Int = 0 초기값)
    // 에서는 시트가 열리자마자 잠시 "0개" 로 표시됐다가 실제 값으로 바뀌는 깜빡임이 있었음.
    var shapeCount by remember { mutableStateOf<Int?>(null) }

    // 도형 수 조회
    LaunchedEffect(drone.id) {
        shapeCount = null  // drone 변경 시 로딩 상태로 리셋
        shapeCount = getShapeCount(drone.id)
    }

    val isLastDrone = activeDrones.size <= 1
    val droneColor = drone.paletteColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 헤더: 색상 원형 + 이름
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(droneColor?.composeColor ?: MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = drone.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    droneColor?.let {
                        Text(
                            text = it.koreanName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 연결된 도형 수 — 로딩 중(null)이면 "..." 로 표시하여 깜빡임 차단
            DetailRow(
                label = stringResource(R.string.drone_detail_linked_shapes),
                value = shapeCount?.let { stringResource(R.string.drone_detail_shape_count, it) }
                    ?: "..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 시리얼 번호
            if (!drone.serialNumber.isNullOrBlank()) {
                DetailRow(label = stringResource(R.string.drone_detail_serial_number), value = drone.serialNumber)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 이륙 무게
            if (!drone.takeoffWeight.isNullOrBlank()) {
                DetailRow(label = stringResource(R.string.drone_detail_takeoff_weight), value = drone.takeoffWeight)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 크기
            if (!drone.size.isNullOrBlank()) {
                DetailRow(label = stringResource(R.string.drone_detail_size), value = drone.size)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 메모
            if (!drone.memo.isNullOrBlank()) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow(label = stringResource(R.string.common_memo), value = drone.memo)
                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // 액션 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.common_edit))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 삭제 다이얼로그
    if (showDeleteDialog) {
        if (isLastDrone) {
            // 마지막 드론은 삭제 불가
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.drone_detail_delete_impossible_title)) },
                text = { Text(stringResource(R.string.drone_detail_delete_impossible_message)) },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            )
        } else if (shapeCount == 0 || shapeCount == null) {
            // 연결된 도형이 0개거나 아직 로딩 중이면 바로 삭제 확인 (보수적으로 단순 흐름).
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
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        } else {
            // 연결된 도형이 있으면 처리 방법 선택 (shapeCount 가 1 이상이라는 의미)
            DroneDeleteWithShapesDialog(
                drone = drone,
                shapeCount = shapeCount ?: 0,
                otherDrones = activeDrones.filter { it.id != drone.id },
                onConfirm = { handling ->
                    showDeleteDialog = false
                    onDelete(handling)
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

/**
 * 연결된 도형이 있는 드론 삭제 다이얼로그
 */
@Composable
private fun DroneDeleteWithShapesDialog(
    drone: DroneModel,
    shapeCount: Int,
    otherDrones: List<DroneModel>,
    onConfirm: (ShapeHandling) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<ShapeHandling?>(null) }
    var selectedTargetDrone by remember { mutableStateOf(otherDrones.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drone_detail_delete_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.drone_detail_delete_with_shapes_message, drone.name, shapeCount),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 옵션 1: 다른 드론으로 이동
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedOption = ShapeHandling.Reassign(
                                selectedTargetDrone?.id ?: ""
                            )
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption is ShapeHandling.Reassign,
                        onClick = {
                            selectedOption = ShapeHandling.Reassign(
                                selectedTargetDrone?.id ?: ""
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.drone_detail_reassign), style = MaterialTheme.typography.bodyMedium)
                }

                // 재할당 대상 드론 선택
                if (selectedOption is ShapeHandling.Reassign) {
                    Column(
                        modifier = Modifier.padding(start = 48.dp)
                    ) {
                        otherDrones.forEach { targetDrone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTargetDrone = targetDrone
                                        selectedOption = ShapeHandling.Reassign(targetDrone.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTargetDrone?.id == targetDrone.id,
                                    onClick = {
                                        selectedTargetDrone = targetDrone
                                        selectedOption = ShapeHandling.Reassign(targetDrone.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            targetDrone.paletteColor?.composeColor
                                                ?: MaterialTheme.colorScheme.primary
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = targetDrone.name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 옵션 2: 도형도 함께 삭제
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = ShapeHandling.DeleteAll }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption is ShapeHandling.DeleteAll,
                        onClick = { selectedOption = ShapeHandling.DeleteAll }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.drone_detail_delete_all_shapes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedOption?.let { onConfirm(it) }
                },
                enabled = selectedOption != null
            ) {
                Text(stringResource(R.string.common_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
