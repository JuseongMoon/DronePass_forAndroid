package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor

/**
 * 드론 생성/편집 BottomSheet
 *
 * @param drone 편집 대상 드론 (null이면 새로 추가 모드)
 * @param suggestedColor 새 드론 생성 시 추천 색상
 * @param isDuplicateName 이름 중복 여부 체크 함수
 * @param onSave 저장 콜백
 * @param onDismiss 닫기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneEditSheet(
    drone: DroneModel? = null,
    suggestedColor: PaletteColor = PaletteColor.BLUE,
    isDuplicateName: (String, String?) -> Boolean,
    onSave: (DroneModel) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = drone != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 상태 초기화
    var name by remember { mutableStateOf(drone?.name ?: "") }
    var selectedColor by remember {
        mutableStateOf(drone?.paletteColor ?: suggestedColor)
    }
    var serialNumber by remember { mutableStateOf(drone?.serialNumber ?: "") }
    var takeoffWeight by remember { mutableStateOf(drone?.takeoffWeight ?: "") }
    var size by remember { mutableStateOf(drone?.size ?: "") }
    var memo by remember { mutableStateOf(drone?.memo ?: "") }

    // 검증 상태 — isDuplicateName 은 List 순회를 동반하므로 name/drone?.id 가 변할 때만 재계산.
    // 이전: 매 recomposition (예: 다른 필드 입력) 마다 List 순회 반복.
    val nameError = name.isBlank()
    val duplicateWarning by remember(name, drone?.id) {
        derivedStateOf { name.isNotBlank() && isDuplicateName(name, drone?.id) }
    }

    // 색상 목록 (GRAY 제외)
    val selectableColors = remember {
        PaletteColor.entries.filter { it != PaletteColor.GRAY }
    }

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
            // 헤더
            Text(
                text = if (isEditMode) stringResource(R.string.drone_edit_title_edit) else stringResource(R.string.drone_edit_title_create),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 이름 =====
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.drone_edit_label_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = duplicateWarning,
                supportingText = if (duplicateWarning) {
                    { Text(stringResource(R.string.drone_edit_duplicate_warning), color = MaterialTheme.colorScheme.error) }
                } else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 색상 선택 =====
            Text(
                text = stringResource(R.string.drone_edit_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            ColorPickerGrid(
                colors = selectableColors,
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 추가 정보 =====
            Text(
                text = stringResource(R.string.drone_edit_additional_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 시리얼 번호
            OutlinedTextField(
                value = serialNumber,
                onValueChange = { serialNumber = it },
                label = { Text(stringResource(R.string.drone_edit_serial_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 이륙 무게. 단위/형식("250g", "1.5kg", "1500" 등) 자유 입력 의도이므로
            // 숫자 strict 검증은 적용하지 않는다. 향후 항공안전법 카테고리 자동 판정이
            // 필요해지면 별도 파서를 두고 입력은 자유 유지 (UX 제약 최소화).
            OutlinedTextField(
                value = takeoffWeight,
                onValueChange = { takeoffWeight = it.take(20) }, // 과도한 입력 길이만 컷
                label = { Text(stringResource(R.string.drone_edit_takeoff_weight)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 크기 ("28cm × 21cm" 등 자유 형식). 동일 정책으로 길이만 제한.
            OutlinedTextField(
                value = size,
                onValueChange = { size = it.take(40) },
                label = { Text(stringResource(R.string.drone_edit_size)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 메모
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text(stringResource(R.string.drone_edit_memo)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text(stringResource(R.string.drone_edit_memo_placeholder)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 하단 버튼 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        val resultDrone = (drone ?: DroneModel()).copy(
                            name = name.trim(),
                            color = selectedColor.hex,
                            serialNumber = serialNumber.ifBlank { null },
                            takeoffWeight = takeoffWeight.ifBlank { null },
                            size = size.ifBlank { null },
                            memo = memo.ifBlank { null },
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(resultDrone)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && !duplicateWarning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 색상 선택 그리드
 */
@Composable
fun ColorPickerGrid(
    colors: List<PaletteColor>,
    selectedColor: PaletteColor,
    onColorSelected: (PaletteColor) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colors) { color ->
            ColorCircle(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

/**
 * 색상 원형 아이템
 */
@Composable
private fun ColorCircle(
    color: PaletteColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.composeColor)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.drone_edit_color_selected, color.koreanName),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
