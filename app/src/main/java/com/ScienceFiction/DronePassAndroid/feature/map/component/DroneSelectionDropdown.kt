package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.feature.drone.selectedDronesForIosDropdown

internal val DroneDropdownMenuItemHorizontalPadding = 12.dp
internal val DroneDropdownMenuItemVerticalPadding = 8.dp
internal val DroneDropdownShadowElevation = 4.dp
internal val DroneDropdownEmptyIconSize = 12.dp
internal val DroneDropdownChevronIconSize = 10.dp
internal val DroneDropdownTriggerDiameter = 32.dp
internal val DroneDropdownChevronReservedWidth = 40.dp
internal val DroneDropdownChipHorizontalSpacing = 8.dp
internal val DroneDropdownChipLineSpacing = 4.dp
internal val DroneDropdownSelectionButtonHeight = DroneDropdownTriggerDiameter
internal val DroneDropdownTriggerSize = DroneDropdownTriggerDiameter
internal val DroneDropdownControlVerticalAlignment: Alignment.Vertical = Alignment.Top
internal val DroneDropdownTextSize = 14.sp
internal const val DroneDropdownPopupFocusable = false
internal const val DroneDropdownPopupDismissOnClickOutside = false
internal const val DroneDropdownPopupDismissOnBackPress = false
@DrawableRes
internal val DroneDropdownEmptyIconRes = R.drawable.ic_drone
internal fun droneDropdownEmptyTextColor(onSurface: Color): Color = onSurface

@Composable
fun DroneSelectionDropdown(
    activeDrones: List<DroneModel>,
    selectedDroneIds: Set<String>,
    highlightedDroneIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onToggleHighlight: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    var triggerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val selectedDrones = selectedDronesForIosDropdown(activeDrones, selectedDroneIds)

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            triggerHeight = coordinates.size.height
        }
    ) {
        if (selectedDrones.isEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = DroneDropdownControlVerticalAlignment,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = DroneDropdownShadowElevation,
                    modifier = Modifier
                        .requiredHeight(DroneDropdownSelectionButtonHeight)
                        .align(DroneDropdownControlVerticalAlignment),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(DroneDropdownEmptyIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(DroneDropdownEmptyIconSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Text(
                            text = stringResource(R.string.drone_dropdown_select),
                            fontSize = DroneDropdownTextSize,
                            fontWeight = FontWeight.Medium,
                            color = droneDropdownEmptyTextColor(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                DroneDropdownChevronButton(
                    showDropdown = showDropdown,
                    onClick = { showDropdown = !showDropdown },
                )
            }
        } else {
            DroneChipFlowLayout(
                drones = selectedDrones,
                highlightedDroneIds = highlightedDroneIds,
                onToggleHighlight = onToggleHighlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd),
            )
            DroneDropdownChevronButton(
                showDropdown = showDropdown,
                onClick = { showDropdown = !showDropdown },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        // 드롭다운 메뉴 (Popup)
        if (showDropdown) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, triggerHeight + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = { showDropdown = false },
                // iOS overlay 정합: 바깥 탭으로 자동 dismiss 되지 않고 chevron 재탭으로 닫힌다.
                // Popup 을 focusable/modal 로 만들면 지도 탭이 드롭다운을 먼저 닫는 Android 고유
                // 동작이 생기므로, 외부 dismiss 를 끄고 터치는 뒤 화면으로 통과시킨다.
                properties = PopupProperties(
                    focusable = DroneDropdownPopupFocusable,
                    dismissOnBackPress = DroneDropdownPopupDismissOnBackPress,
                    dismissOnClickOutside = DroneDropdownPopupDismissOnClickOutside,
                )
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
                        // iOS `.fixedSize(horizontal: true, vertical: false)` 정합:
                        // 드롭다운 폭은 가장 긴 드론 이름 + 패딩에 맞게 wrap 한다.
                        // - `wrapContentWidth()` 만으로는 내부 `HorizontalDivider` 의 fillMaxWidth
                        //   기본 동작 때문에 화면 좌우를 다 차지하게 된다.
                        // - `width(IntrinsicSize.Max)` 로 자식의 maxIntrinsicWidth(가장 긴 드론 이름 행)
                        //   에 맞춰 강제 wrap → divider 도 자동으로 그 너비를 따른다.
                        // 우측 정렬과 우측 16dp 패딩은 Popup(alignment = TopEnd) 가 부모 Box(=chevron
                        // 우측에 맞춤) 의 우측 상단에 정렬해 주므로 자동으로 chevron 우측과 일치한다.
                        modifier = Modifier.width(IntrinsicSize.Max),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = DroneDropdownShadowElevation
                    ) {
                        Column {
                            activeDrones.forEachIndexed { index, drone ->
                                val isSelected = drone.id in selectedDroneIds
                                Row(
                                    modifier = Modifier
                                        .clickable { onToggleSelection(drone.id) }
                                        .padding(
                                            horizontal = DroneDropdownMenuItemHorizontalPadding,
                                            vertical = DroneDropdownMenuItemVerticalPadding,
                                        ),
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
                                    val droneColor = droneDropdownMenuColor(drone.paletteColor)
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(droneColor, CircleShape)
                                    )

                                    // 드론 이름
                                    Text(
                                        text = drone.name,
                                        fontSize = DroneDropdownTextSize,
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
private fun DroneDropdownChevronButton(
    showDropdown: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = DroneDropdownShadowElevation,
        modifier = modifier.requiredSize(DroneDropdownTriggerSize),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
        ) {
            Icon(
                imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = if (showDropdown)
                    stringResource(R.string.drone_dropdown_collapse)
                else
                    stringResource(R.string.drone_dropdown_toggle),
                modifier = Modifier.size(DroneDropdownChevronIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DroneChipFlowLayout(
    drones: List<DroneModel>,
    highlightedDroneIds: Set<String>,
    onToggleHighlight: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val firstLineReservedWidth = with(density) { DroneDropdownChevronReservedWidth.roundToPx() }
    val horizontalSpacing = with(density) { DroneDropdownChipHorizontalSpacing.roundToPx() }
    val lineSpacing = with(density) { DroneDropdownChipLineSpacing.roundToPx() }

    Layout(
        modifier = modifier,
        content = {
            drones.forEach { drone ->
                DroneChip(
                    drone = drone,
                    isHighlighted = drone.id in highlightedDroneIds,
                    onClick = { onToggleHighlight(drone.id) },
                )
            }
        },
    ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable ->
            measurable.measure(looseConstraints)
        }
        val maxWidth = constraints.maxWidth
        val layout = computeDroneDropdownChipFlowLayout(
            itemSizes = placeables.map { IntSize(it.width, it.height) },
            maxWidth = maxWidth,
            firstLineReservedWidth = firstLineReservedWidth,
            horizontalSpacing = horizontalSpacing,
            lineSpacing = lineSpacing,
        )

        layout(
            width = maxWidth.coerceIn(constraints.minWidth, constraints.maxWidth),
            height = layout.height.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {
            placeables.forEachIndexed { index, placeable ->
                val placement = layout.placements[index]
                placeable.placeRelative(placement.x, placement.y)
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
    // iOS 정합: "드론 선택" 빈 상태 버튼과 동일한 외형/크기.
    // Surface(onClick=...) 를 쓰면 Material3 의 최소 터치영역(48dp) 이 강제되어
    // 빈 상태 버튼(약 32dp) 보다 크게 그려진다. 따라서 onClick 은 내부 Row 의
    // Modifier.clickable 로 처리하여 외형 높이를 빈 상태 버튼과 일치시킨다.
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = DroneDropdownShadowElevation,
        modifier = Modifier.requiredHeight(DroneDropdownSelectionButtonHeight),
        border = if (isHighlighted)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 드론 색상 원
            drone.paletteColor?.takeIf(::shouldShowSelectedDroneChipColorIndicator)?.let { paletteColor ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(paletteColor.composeColor, CircleShape)
                )
            }

            // 드론 이름
            Text(
                text = drone.name,
                fontSize = DroneDropdownTextSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

internal fun shouldShowSelectedDroneChipColorIndicator(color: PaletteColor?): Boolean = color != null

internal fun droneDropdownMenuColor(color: PaletteColor?): Color =
    color?.composeColor ?: Color.Gray

internal data class DroneDropdownChipPlacement(
    val x: Int,
    val y: Int,
)

internal data class DroneDropdownChipFlowLayoutResult(
    val height: Int,
    val placements: List<DroneDropdownChipPlacement>,
    val lineWidths: List<Int>,
)

internal fun computeDroneDropdownChipFlowLayout(
    itemSizes: List<IntSize>,
    maxWidth: Int,
    firstLineReservedWidth: Int,
    horizontalSpacing: Int,
    lineSpacing: Int,
): DroneDropdownChipFlowLayoutResult {
    if (itemSizes.isEmpty()) {
        return DroneDropdownChipFlowLayoutResult(
            height = 0,
            placements = emptyList(),
            lineWidths = emptyList(),
        )
    }

    val safeMaxWidth = maxWidth.coerceAtLeast(0)
    val safeReservedWidth = firstLineReservedWidth.coerceAtLeast(0)
    val safeHorizontalSpacing = horizontalSpacing.coerceAtLeast(0)
    val safeLineSpacing = lineSpacing.coerceAtLeast(0)
    val lineItems = mutableListOf<MutableList<Int>>(mutableListOf())
    val lineWidths = mutableListOf(0)
    val lineHeights = mutableListOf(0)

    itemSizes.forEachIndexed { index, rawSize ->
        val width = rawSize.width.coerceAtLeast(0)
        val height = rawSize.height.coerceAtLeast(0)
        val lineIndex = lineItems.lastIndex
        val currentItems = lineItems[lineIndex]
        val currentWidth = lineWidths[lineIndex]
        val candidateWidth = if (currentItems.isEmpty()) {
            width
        } else {
            currentWidth + safeHorizontalSpacing + width
        }
        val availableWidth = if (lineIndex == 0) {
            (safeMaxWidth - safeReservedWidth).coerceAtLeast(0)
        } else {
            safeMaxWidth
        }

        val targetLineIndex = if (currentItems.isNotEmpty() && candidateWidth > availableWidth) {
            lineItems.add(mutableListOf())
            lineWidths.add(0)
            lineHeights.add(0)
            lineItems.lastIndex
        } else {
            lineIndex
        }

        val targetItems = lineItems[targetLineIndex]
        lineWidths[targetLineIndex] = if (targetItems.isEmpty()) {
            width
        } else {
            lineWidths[targetLineIndex] + safeHorizontalSpacing + width
        }
        lineHeights[targetLineIndex] = maxOf(lineHeights[targetLineIndex], height)
        targetItems.add(index)
    }

    val placements = MutableList(itemSizes.size) { DroneDropdownChipPlacement(0, 0) }
    var currentY = 0
    lineItems.forEachIndexed { lineIndex, indices ->
        if (indices.isEmpty()) return@forEachIndexed
        val availableWidth = if (lineIndex == 0) {
            (safeMaxWidth - safeReservedWidth).coerceAtLeast(0)
        } else {
            safeMaxWidth
        }
        var currentX = availableWidth - lineWidths[lineIndex]

        indices.forEach { itemIndex ->
            placements[itemIndex] = DroneDropdownChipPlacement(currentX, currentY)
            currentX += itemSizes[itemIndex].width.coerceAtLeast(0) + safeHorizontalSpacing
        }
        currentY += lineHeights[lineIndex] + safeLineSpacing
    }

    return DroneDropdownChipFlowLayoutResult(
        height = (currentY - safeLineSpacing).coerceAtLeast(0),
        placements = placements,
        lineWidths = lineWidths,
    )
}
