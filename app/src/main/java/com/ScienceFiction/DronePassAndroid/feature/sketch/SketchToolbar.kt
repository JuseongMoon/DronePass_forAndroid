package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.parseIosOpaqueRgbHexColor
import java.util.Locale

internal val SketchToolbarButtonSize = 32.dp
internal val SketchToolbarIconSize = 18.dp
internal val SketchToolbarContainerCornerRadius = 30.dp
internal val SketchToolbarHorizontalPadding = 16.dp
internal val SketchToolbarVerticalPadding = 12.dp
internal val SketchToolbarItemSpacing = 12.dp
internal val SketchToolbarDividerHeight = 24.dp
internal val SketchToolbarShadowElevation = 10.dp
internal val SketchToolbarDoneHorizontalPadding = 14.dp
internal val SketchToolbarDoneVerticalPadding = 8.dp
internal val SketchToolbarDoneCornerRadius = 16.dp
internal val SketchToolbarDoneFontSize = 14.sp
internal val SketchDeleteBadgeFontSize = 9.sp
@DrawableRes
internal val SketchEraserIconRes = R.drawable.ic_eraser
internal val SketchPenPickerCardCornerRadius = 16.dp
internal val SketchPenPickerCardPadding = 10.dp
internal val SketchPenPickerCardSpacing = 10.dp
internal const val SketchPenPickerAnimationDurationMs = 200
internal const val SketchPenPickerInitialScale = 0.95f
internal const val SketchPenPickerTransformOriginPivotX = 0.5f
internal const val SketchPenPickerTransformOriginPivotY = 1f
internal val SketchPenPickerButtonSize = 44.dp
internal val SketchPenPickerButtonSpacing = 4.dp
internal val SketchPenPickerContentWidth = 236.dp
internal val SketchOpacityCheckerboardSquareSize = 6.dp
internal const val SketchOpacityCheckerboardDarkAlpha = 0.3f
internal val SketchGradientSliderHeight = 30.dp
internal val SketchGradientSliderCornerRadius = 15.dp
internal val SketchGradientSliderThumbSize = 28.dp
internal val SketchGradientSliderThumbInnerSize = 20.dp
internal const val SketchGradientSliderThumbShadowAlpha = 0.25f
internal val SketchGradientSliderThumbShadowOffsetY = 1.dp
internal const val SketchHueSliderMin = 0f
internal const val SketchHueSliderMax = 360f
internal const val SketchOpacitySliderMin = 0.1f
internal const val SketchOpacitySliderMax = 1.0f

internal fun formatSketchDeleteBadgeCount(sketchCount: Int): String {
    return sketchCount.toString()
}

internal fun sketchHueDegreesFromIosHexColor(hex: String): Float {
    val color = parseIosOpaqueRgbHexColor(hex) ?: return 0f
    val red = ((color ushr 16) and 0xFF) / 255f
    val green = ((color ushr 8) and 0xFF) / 255f
    val blue = (color and 0xFF) / 255f
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta == 0f) return 0f

    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return if (hue < 0f) hue + 360f else hue
}

internal fun parseSketchToolbarColorSafe(color: String): Color {
    return Color(parseIosOpaqueRgbHexColor(color) ?: 0xFFFF0000.toInt())
}

internal fun sketchPenButtonDisplayColor(color: String): Color {
    return parseSketchToolbarColorSafe(color)
}

internal fun sketchSliderFractionFromX(
    x: Float,
    width: Float,
    thumbRadius: Float,
): Float {
    val travelWidth = (width - (thumbRadius * 2f)).coerceAtLeast(1f)
    return ((x - thumbRadius) / travelWidth).coerceIn(0f, 1f)
}

internal fun sketchSliderValueFromX(
    x: Float,
    width: Float,
    thumbRadius: Float,
    valueMin: Float,
    valueMax: Float,
): Float {
    val fraction = sketchSliderFractionFromX(
        x = x,
        width = width,
        thumbRadius = thumbRadius,
    )
    return valueMin + (fraction * (valueMax - valueMin))
}

internal fun sketchSliderThumbCenterX(
    value: Float,
    width: Float,
    thumbRadius: Float,
    valueMin: Float,
    valueMax: Float,
): Float {
    val valueRange = valueMax - valueMin
    val fraction = if (valueRange <= 0f) {
        0f
    } else {
        ((value - valueMin) / valueRange).coerceIn(0f, 1f)
    }
    val travelWidth = (width - (thumbRadius * 2f)).coerceAtLeast(1f)
    return thumbRadius + (fraction * travelWidth)
}

/**
 * 스케치 모드 메인 툴바.
 *
 * 펜 설정, 지우개, Undo/Redo, 전체 삭제, 완료 버튼을 제공한다.
 * 펜 버튼을 탭하면 색상/두께/투명도 설정 카드가 토글된다.
 *
 * @param currentColor 현재 펜 색상 (HEX)
 * @param currentStrokeWidth 현재 펜 두께
 * @param currentOpacity 현재 펜 투명도
 * @param isEraserMode 지우개 모드 여부
 * @param canUndo Undo 가능 여부
 * @param canRedo Redo 가능 여부
 * @param sketchCount 현재 활성 스케치 수
 * @param onColorChanged 색상 변경 콜백
 * @param onStrokeWidthChanged 두께 변경 콜백
 * @param onOpacityChanged 투명도 변경 콜백
 * @param onToggleEraser 지우개 토글 콜백
 * @param onUndo Undo 콜백
 * @param onRedo Redo 콜백
 * @param onDeleteAll 전체 삭제 콜백
 * @param onDone 완료 콜백
 */
@Composable
fun SketchToolbar(
    currentColor: String,
    currentStrokeWidth: Double,
    currentOpacity: Double,
    isEraserMode: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    sketchCount: Int,
    onColorChanged: (String) -> Unit,
    onStrokeWidthChanged: (Double) -> Unit,
    onOpacityChanged: (Double) -> Unit,
    onToggleEraser: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDeleteAll: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPenSettings by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 펜 설정 카드 (토글)
        AnimatedVisibility(
            visible = showPenSettings && !isEraserMode,
            enter = fadeIn(
                animationSpec = tween(SketchPenPickerAnimationDurationMs),
            ) + scaleIn(
                animationSpec = tween(SketchPenPickerAnimationDurationMs),
                initialScale = SketchPenPickerInitialScale,
                transformOrigin = SketchPenPickerTransformOrigin,
            ),
            exit = fadeOut(
                animationSpec = tween(SketchPenPickerAnimationDurationMs),
            ) + scaleOut(
                animationSpec = tween(SketchPenPickerAnimationDurationMs),
                targetScale = SketchPenPickerInitialScale,
                transformOrigin = SketchPenPickerTransformOrigin,
            ),
        ) {
            PenSettingsCard(
                currentColor = currentColor,
                currentStrokeWidth = currentStrokeWidth,
                currentOpacity = currentOpacity,
                onColorChanged = onColorChanged,
                onStrokeWidthChanged = onStrokeWidthChanged,
                onOpacityChanged = onOpacityChanged,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // 메인 툴바
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(SketchToolbarContainerCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = SketchToolbarShadowElevation,
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = SketchToolbarHorizontalPadding,
                        vertical = SketchToolbarVerticalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(SketchToolbarItemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 펜 버튼 (현재 색/두께 표시)
                PenButton(
                    color = currentColor,
                    strokeWidth = currentStrokeWidth,
                    isActive = !isEraserMode,
                    onClick = {
                        if (isEraserMode) {
                            onToggleEraser()
                            showPenSettings = false
                        } else {
                            showPenSettings = !showPenSettings
                        }
                    }
                )

                // 지우개 버튼
                ToolbarIconButton(
                    painter = painterResource(SketchEraserIconRes),
                    contentDescription = stringResource(R.string.sketch_eraser),
                    isActive = isEraserMode,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        showPenSettings = false
                        if (!isEraserMode) {
                            onToggleEraser()
                        }
                    }
                )

                // Undo 버튼
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.sketch_undo),
                    enabled = canUndo,
                    onClick = onUndo
                )

                // Redo 버튼
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.sketch_redo),
                    enabled = canRedo,
                    onClick = onRedo
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(SketchToolbarDividerHeight)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // 전체 삭제 버튼 (뱃지로 개수 표시)
                DeleteAllButton(
                    sketchCount = sketchCount,
                    onClick = { showDeleteAllConfirmDialog = true }
                )

                // 완료 버튼
                DoneButton(
                    text = stringResource(R.string.sketch_done),
                    onClick = onDone,
                )
            }
        }

        // 전체 삭제 확인 다이얼로그
        if (showDeleteAllConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirmDialog = false },
                title = { Text(stringResource(R.string.sketch_delete_all_title)) },
                text = { Text(stringResource(R.string.sketch_delete_all_confirm, sketchCount)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAllConfirmDialog = false
                            onDeleteAll()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.common_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

internal val SketchPenPickerTransformOrigin = TransformOrigin(
    pivotFractionX = SketchPenPickerTransformOriginPivotX,
    pivotFractionY = SketchPenPickerTransformOriginPivotY,
)

// ──────────────────────────────────────────────
// 펜 버튼
// ──────────────────────────────────────────────

@Composable
private fun PenButton(
    color: String,
    strokeWidth: Double,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val penColor = sketchPenButtonDisplayColor(color)

    Box(
        modifier = Modifier
            .size(SketchToolbarButtonSize)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(strokeWidth.coerceAtLeast(3.0).dp)
                .height(20.dp)
                .rotate(-45f)
                .clip(RoundedCornerShape((strokeWidth / 2).dp))
                .background(penColor)
        )
    }
}

// ──────────────────────────────────────────────
// 전체 삭제 버튼 (뱃지 포함)
// ──────────────────────────────────────────────

@Composable
private fun DeleteAllButton(
    sketchCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(SketchToolbarButtonSize)
            .then(
                if (sketchCount > 0) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (sketchCount > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = formatSketchDeleteBadgeCount(sketchCount),
                            fontSize = SketchDeleteBadgeFontSize,
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.sketch_delete_all),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(SketchToolbarIconSize),
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.sketch_delete_all),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(SketchToolbarIconSize),
            )
        }
    }
}

@Composable
private fun DoneButton(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        fontSize = SketchToolbarDoneFontSize,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(SketchToolbarDoneCornerRadius))
            .background(Color(0xFF007AFF))
            .clickable(onClick = onClick)
            .padding(
                horizontal = SketchToolbarDoneHorizontalPadding,
                vertical = SketchToolbarDoneVerticalPadding,
            ),
    )
}

// ──────────────────────────────────────────────
// 툴바 아이콘 버튼
// ──────────────────────────────────────────────

@Composable
private fun ToolbarIconButton(
    icon: ImageVector? = null,
    painter: Painter? = null,
    contentDescription: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(SketchToolbarButtonSize)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) {
                    Color(0xFFFF9500).copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                }
            )
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        val iconTint = when {
            isActive -> Color(0xFFFF9500)
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            else -> tint
        }
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(SketchToolbarIconSize),
                tint = iconTint,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(SketchToolbarIconSize),
                tint = iconTint,
            )
        }
    }
}

// ──────────────────────────────────────────────
// 펜 설정 카드
// ──────────────────────────────────────────────

@Composable
private fun PenSettingsCard(
    currentColor: String,
    currentStrokeWidth: Double,
    currentOpacity: Double,
    onColorChanged: (String) -> Unit,
    onStrokeWidthChanged: (Double) -> Unit,
    onOpacityChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentWidth(),
        shape = RoundedCornerShape(SketchPenPickerCardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(SketchPenPickerCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SketchPenPickerCardSpacing)
        ) {
            StrokeWidthSelector(
                currentWidth = currentStrokeWidth,
                onWidthChanged = onStrokeWidthChanged,
            )

            Box(
                modifier = Modifier
                    .width(SketchPenPickerContentWidth)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            ColorSelector(
                currentColor = currentColor,
                onColorChanged = onColorChanged,
                modifier = Modifier.width(SketchPenPickerContentWidth),
            )

            Box(
                modifier = Modifier
                    .width(SketchPenPickerContentWidth)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            OpacitySelector(
                currentColor = currentColor,
                currentOpacity = currentOpacity,
                onOpacityChanged = onOpacityChanged,
                modifier = Modifier.width(SketchPenPickerContentWidth),
            )
        }
    }
}

// ──────────────────────────────────────────────
// 색상 선택기
// ──────────────────────────────────────────────

@Composable
private fun ColorSelector(
    currentColor: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 현재 색상의 Hue 값으로 초기화하되, 이후 슬라이더 자체가 SSOT.
    // currentColor 가 외부에서 변경되어도 sliderPosition 을 강제 동기화하지 않는다.
    // 이전 `remember(currentColor)` 방식은 onColorChanged → currentColor 갱신 →
    // hexToHue 재계산 round-trip 으로 부동소수점 오차가 누적되어 thumb 가 미세하게 점프했다.
    var sliderPosition by remember { mutableFloatStateOf(hexToHue(currentColor)) }

    LaunchedEffect(currentColor) {
        val newPosition = hexToHue(currentColor)
        if (shouldSyncSketchHueSlider(sliderPosition, newPosition)) {
            sliderPosition = newPosition
        }
    }

    Box(
        modifier = modifier
            .height(SketchGradientSliderHeight)
    ) {
        SketchGradientSlider(
            value = sliderPosition,
            valueMin = SketchHueSliderMin,
            valueMax = SketchHueSliderMax,
            thumbColor = parseSketchColor(currentColor),
            onValueChanged = { newHue ->
                sliderPosition = newHue
                onColorChanged(hueToHex(newHue))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = (0..10).map { i ->
                                Color.hsv(i * 36f, 0.85f, 0.9f)
                            }
                        )
                    )
            )
        }
    }
}

@Composable
private fun OpacitySelector(
    currentColor: String,
    currentOpacity: Double,
    onOpacityChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val penColor = parseSketchColor(currentColor)

    Box(
        modifier = modifier
            .height(SketchGradientSliderHeight)
    ) {
        val opacity = currentOpacity.toFloat()
        SketchGradientSlider(
            value = opacity,
            valueMin = SketchOpacitySliderMin,
            valueMax = SketchOpacitySliderMax,
            thumbColor = penColor.copy(alpha = opacity.coerceIn(SketchOpacitySliderMin, SketchOpacitySliderMax)),
            onValueChanged = { onOpacityChanged(it.toDouble()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OpacityCheckerboardPattern(modifier = Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                penColor.copy(alpha = 0.1f),
                                penColor.copy(alpha = 0.25f),
                                penColor.copy(alpha = 0.5f),
                                penColor.copy(alpha = 0.75f),
                                penColor,
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun SketchGradientSlider(
    value: Float,
    valueMin: Float,
    valueMax: Float,
    thumbColor: Color,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackContent: @Composable BoxScope.() -> Unit,
) {
    val currentOnValueChanged by rememberUpdatedState(onValueChanged)

    Box(
        modifier = modifier
            .height(SketchGradientSliderHeight)
            .pointerInput(valueMin, valueMax) {
                val thumbRadiusPx = SketchGradientSliderThumbSize.toPx() / 2f

                fun updateValue(x: Float) {
                    currentOnValueChanged(
                        sketchSliderValueFromX(
                            x = x,
                            width = size.width.toFloat(),
                            thumbRadius = thumbRadiusPx,
                            valueMin = valueMin,
                            valueMax = valueMax,
                        )
                    )
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    updateValue(down.position.x)
                    drag(down.id) { change ->
                        updateValue(change.position.x)
                        change.consume()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(SketchGradientSliderCornerRadius)),
            content = trackContent,
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            val thumbRadiusPx = SketchGradientSliderThumbSize.toPx() / 2f
            val innerRadiusPx = SketchGradientSliderThumbInnerSize.toPx() / 2f
            val centerX = sketchSliderThumbCenterX(
                value = value,
                width = size.width,
                thumbRadius = thumbRadiusPx,
                valueMin = valueMin,
                valueMax = valueMax,
            )
            val center = Offset(centerX, size.height / 2f)
            val shadowCenter = center.copy(
                y = center.y + SketchGradientSliderThumbShadowOffsetY.toPx(),
            )

            drawCircle(
                color = Color.Black.copy(alpha = SketchGradientSliderThumbShadowAlpha),
                radius = thumbRadiusPx,
                center = shadowCenter,
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadiusPx,
                center = center,
            )
            drawCircle(
                color = thumbColor,
                radius = innerRadiusPx,
                center = center,
            )
        }
    }
}

@Composable
private fun OpacityCheckerboardPattern(
    modifier: Modifier = Modifier,
) {
    val squareSize = SketchOpacityCheckerboardSquareSize
    Canvas(modifier = modifier) {
        val squarePx = squareSize.toPx()
        val columns = kotlin.math.ceil(size.width / squarePx).toInt()
        val rows = kotlin.math.ceil(size.height / squarePx).toInt()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val color = if ((row + column) % 2 == 0) {
                    Color.White
                } else {
                    Color.Gray.copy(alpha = SketchOpacityCheckerboardDarkAlpha)
                }
                drawRect(
                    color = color,
                    topLeft = Offset(column * squarePx, row * squarePx),
                    size = Size(squarePx, squarePx),
                )
            }
        }
    }
}

@Composable
private fun PenStrokeOptionButton(
    width: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SketchPenPickerButtonSize)
            .clip(CircleShape)
            .background(if (isSelected) Color.Black else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(30.dp)
                .rotate(-45f)
                .clip(RoundedCornerShape((width / 2).dp))
                .background(if (isSelected) Color.White else Color.Gray)
        ) {
        }
    }
}

// ──────────────────────────────────────────────
// 두께 선택기
// ──────────────────────────────────────────────

@Composable
private fun StrokeWidthSelector(
    currentWidth: Double,
    onWidthChanged: (Double) -> Unit
) {
    val widthOptions = listOf(2.0, 4.0, 6.0, 8.0, 10.0)

    Row(
        modifier = Modifier.width(SketchPenPickerContentWidth),
        horizontalArrangement = Arrangement.spacedBy(SketchPenPickerButtonSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        widthOptions.forEach { width ->
            val isSelected = width == currentWidth
            PenStrokeOptionButton(
                width = width,
                isSelected = isSelected,
                onClick = { onWidthChanged(width) },
            )
        }
    }
}

// ──────────────────────────────────────────────
// HEX ↔ Hue 변환 유틸
// ──────────────────────────────────────────────

/**
 * HEX 문자열을 HSV의 Hue 값(0~360)으로 변환
 */
private fun hexToHue(hex: String): Float {
    return sketchHueDegreesFromIosHexColor(hex)
}

/**
 * Hue 값(0~360)을 HEX 문자열로 변환
 * Saturation=0.85, Brightness=0.9 고정
 */
private fun hueToHex(hue: Float): String {
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.9f))
    return String.format(Locale.ROOT, "#%06X", 0xFFFFFF and color)
}

private fun parseSketchColor(color: String): Color {
    return parseSketchToolbarColorSafe(color)
}
