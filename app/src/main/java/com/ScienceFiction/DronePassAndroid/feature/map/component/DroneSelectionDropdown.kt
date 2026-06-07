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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
internal val DroneDropdownTextSize = 14.sp
@DrawableRes
internal val DroneDropdownEmptyIconRes = R.drawable.ic_drone

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
    var triggerHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val selectedDrones = selectedDronesForIosDropdown(activeDrones, selectedDroneIds)

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            triggerHeight = coordinates.size.height
        }
    ) {
        // iOS `WrappingHStack(alignment: .trailing, spacing: 8, lineSpacing: 4, firstLineReservedWidth: 40)` 정합:
        // - 첫째줄은 chevron(32dp) + spacing(8dp) = 40dp 만큼 우측 공간을 예약 → chevron 좌측에 칩 배치
        // - 둘째줄부터는 전체 너비를 사용 → chevron 아래까지 칩이 확장됨
        // - 각 줄은 우측 정렬, 줄 내부는 좌→우 순서로 배치
        // 빈 상태 "드론 선택" 버튼도 동일 Layout 의 단일 자식으로 두어 위치 로직을 통일.
        FirstLineReservedFlowLayout(
            spacing = 8.dp,
            lineSpacing = 4.dp,
            firstLineReservedWidth = 40.dp,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            if (selectedDrones.isEmpty()) {
                // 빈 상태: "드론 선택" 버튼
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = DroneDropdownShadowElevation
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                selectedDrones.forEach { drone ->
                    DroneChip(
                        drone = drone,
                        isHighlighted = drone.id in highlightedDroneIds,
                        onClick = { onToggleHighlight(drone.id) }
                    )
                }
            }
        }

        // Chevron 버튼 - 우측 상단 절대 위치 (iOS `ZStack.topTrailing` 정합)
        Surface(
            onClick = { showDropdown = !showDropdown },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = DroneDropdownShadowElevation,
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.TopEnd)
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
                    modifier = Modifier.size(DroneDropdownChevronIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 드롭다운 메뉴 (Popup)
        if (showDropdown) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, triggerHeight + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = { showDropdown = false },
                // focusable=true 로 외부 터치 이벤트를 Popup 이 흡수하도록 한다.
                // 기본값(false) 에서는 chevron 재클릭 시 외부 터치로 인식되어 onDismissRequest 가
                // showDropdown=false 로 만든 직후, 같은 터치가 chevron 까지 전달되어 onClick 이
                // 다시 호출되며 토글 결과가 true 가 되어 "닫혔다 바로 열림" 으로 보였다.
                // focusable=true 면 외부 터치는 chevron 으로 전달되지 않고 onDismissRequest 만 호출 → 한 번에 닫힘.
                properties = PopupProperties(focusable = true)
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
                                    val droneColor = drone.paletteColor?.composeColor ?: Color.Gray
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
        border = if (isHighlighted)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
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

/**
 * iOS `FlowLayout(spacing:, lineSpacing:, firstLineReservedWidth:)` 정합 Custom Layout.
 *
 * - 첫째줄에는 우측에 [firstLineReservedWidth] 만큼 공간을 예약 (chevron 자리)
 * - 둘째줄부터는 전체 너비를 사용
 * - 각 줄은 우측 정렬, 줄 내부는 좌→우 순서로 배치
 * - 자식은 무한 제약(`Constraints()`) 으로 측정해 자기 ideal size 를 가짐 (iOS `sizeThatFits(.unspecified)` 정합)
 */
@Composable
private fun FirstLineReservedFlowLayout(
    spacing: Dp,
    lineSpacing: Dp,
    firstLineReservedWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val lineSpacingPx = lineSpacing.roundToPx()
        val reservedPx = firstLineReservedWidth.roundToPx()
        val proposalWidth = constraints.maxWidth

        // 각 자식 측정 (iOS `sizeThatFits(.unspecified)` 정합)
        val placeables = measurables.map { it.measure(Constraints()) }

        // 1단계: 자식들을 줄에 그룹화
        // (originalIndex, placeable) 쌍을 줄별 리스트로 관리
        val lines = mutableListOf<MutableList<Pair<Int, Placeable>>>()
        lines.add(mutableListOf())
        var currentLineWidth = 0

        placeables.forEachIndexed { index, placeable ->
            val currentLineIndex = lines.lastIndex
            val availableWidth = if (currentLineIndex == 0)
                proposalWidth - reservedPx
            else
                proposalWidth

            // 줄바꿈 조건: 현재 줄에 이미 항목이 있고, 새 항목 추가 시 가용 너비 초과
            if (currentLineWidth + placeable.width > availableWidth && currentLineWidth > 0) {
                lines.add(mutableListOf())
                currentLineWidth = 0
            }

            lines.last().add(index to placeable)
            // iOS 와 동일하게 spacing 포함 누적 (보수적 줄바꿈)
            currentLineWidth += placeable.width + spacingPx
        }

        // 2단계: 위치 계산 (각 줄 우측 정렬, 줄 내부는 좌→우)
        val positions = arrayOfNulls<IntOffset>(placeables.size)
        var currentY = 0

        lines.forEachIndexed { lineIndex, line ->
            if (line.isEmpty()) return@forEachIndexed

            val lineWidth = line.sumOf { it.second.width } + (line.size - 1) * spacingPx
            val lineHeight = line.maxOf { it.second.height }

            val availableWidth = if (lineIndex == 0)
                proposalWidth - reservedPx
            else
                proposalWidth

            // 줄의 시작 x: 가용 너비에서 줄 너비를 뺀 위치 (우측 정렬)
            var currentX = availableWidth - lineWidth

            line.forEach { (originalIndex, placeable) ->
                positions[originalIndex] = IntOffset(currentX, currentY)
                currentX += placeable.width + spacingPx
            }

            currentY += lineHeight + lineSpacingPx
        }

        val totalHeight = if (lines.any { it.isNotEmpty() })
            currentY - lineSpacingPx
        else
            0

        layout(proposalWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val pos = positions[index] ?: IntOffset.Zero
                placeable.place(pos.x, pos.y)
            }
        }
    }
}
