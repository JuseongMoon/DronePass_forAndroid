package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DroneSelectionDropdown(
    activeDrones: List<DroneModel>,
    selectedDroneIds: Set<String>,
    highlightedDroneId: String?,
    onToggleSelection: (String) -> Unit,
    onToggleHighlight: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    var triggerHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val selectedDrones = activeDrones
        .filter { it.id in selectedDroneIds }
        .sortedBy { it.name }

    Box(modifier = modifier) {
        // 트리거 영역
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                triggerHeight = coordinates.size.height
            }
        ) {
            // 선택된 드론들 또는 빈 상태
            if (selectedDrones.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.drone_dropdown_select),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                // 선택된 드론 칩들 (FlowRow로 래핑)
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selectedDrones.forEach { drone ->
                        DroneChip(
                            drone = drone,
                            isHighlighted = drone.id == highlightedDroneId,
                            onClick = { onToggleHighlight(drone.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Chevron 버튼
            Surface(
                onClick = { showDropdown = !showDropdown },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showDropdown)
                            stringResource(R.string.drone_dropdown_collapse)
                        else
                            stringResource(R.string.drone_dropdown_toggle),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 드롭다운 메뉴 (Popup)
        if (showDropdown) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, triggerHeight + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = { showDropdown = false }
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        animationSpec = tween(200),
                        transformOrigin = TransformOrigin(1f, 0f)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(
                        animationSpec = tween(150),
                        transformOrigin = TransformOrigin(1f, 0f)
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.widthIn(min = 160.dp)
                        ) {
                            activeDrones.forEachIndexed { index, drone ->
                                val isSelected = drone.id in selectedDroneIds
                                Row(
                                    modifier = Modifier
                                        .clickable { onToggleSelection(drone.id) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 체크/미체크 아이콘
                                    Icon(
                                        imageVector = if (isSelected)
                                            Icons.Default.CheckCircle
                                        else
                                            Icons.Outlined.Circle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )

                                    // 드론 색상 원
                                    val droneColor = drone.paletteColor?.composeColor ?: Color.Gray
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(droneColor, CircleShape)
                                    )

                                    // 드론 이름
                                    Text(
                                        text = drone.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // 구분선 (마지막 항목 제외)
                                if (index < activeDrones.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DroneChip(
    drone: DroneModel,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = if (isHighlighted)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 드론 색상 원
            val droneColor = drone.paletteColor?.composeColor ?: Color.Gray
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(droneColor, CircleShape)
            )

            // 드론 이름
            Text(
                text = drone.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
