package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R

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
            enter = expandVertically(),
            exit = shrinkVertically()
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
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
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
                        }
                        showPenSettings = !showPenSettings
                    }
                )

                // 지우개 버튼
                ToolbarIconButton(
                    icon = Icons.Default.DeleteForever,
                    contentDescription = stringResource(R.string.sketch_eraser),
                    isActive = isEraserMode,
                    onClick = {
                        showPenSettings = false
                        onToggleEraser()
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

                // 전체 삭제 버튼 (뱃지로 개수 표시)
                DeleteAllButton(
                    sketchCount = sketchCount,
                    onClick = { showDeleteAllConfirmDialog = true }
                )

                // 완료 버튼
                ToolbarIconButton(
                    icon = Icons.Default.Check,
                    contentDescription = stringResource(R.string.sketch_done),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onDone
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
    val penColor = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        Color.Red
    }

    val indicatorSize = (strokeWidth * 2).coerceIn(6.0, 20.0).dp

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (isActive) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 펜 아이콘
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.sketch_pen),
            modifier = Modifier.size(22.dp),
            tint = if (isActive) penColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        // 두께 인디케이터 (우하단)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(indicatorSize)
                .background(penColor, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
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
    IconButton(
        onClick = onClick,
        enabled = sketchCount > 0
    ) {
        if (sketchCount > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = if (sketchCount > 99) "99+" else sketchCount.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = stringResource(R.string.sketch_delete_all),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = stringResource(R.string.sketch_delete_all),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }
}

// ──────────────────────────────────────────────
// 툴바 아이콘 버튼
// ──────────────────────────────────────────────

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                isActive -> MaterialTheme.colorScheme.primary
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                else -> tint
            }
        )
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ──── 색상 선택 ────
            Text(
                text = stringResource(R.string.sketch_color),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            ColorSelector(
                currentColor = currentColor,
                onColorChanged = onColorChanged
            )

            // ──── 두께 선택 ────
            Text(
                text = stringResource(R.string.sketch_stroke_width),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            StrokeWidthSelector(
                currentWidth = currentStrokeWidth,
                onWidthChanged = onStrokeWidthChanged
            )

            // ──── 투명도 슬라이더 ────
            Text(
                text = stringResource(R.string.sketch_opacity, (currentOpacity * 100).toInt()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 현재 색상 기반 투명도 그라데이션 슬라이더
            val penColor = try {
                Color(android.graphics.Color.parseColor(currentColor))
            } catch (e: Exception) { Color.Red }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                penColor.copy(alpha = 0.1f),
                                penColor.copy(alpha = 0.5f),
                                penColor
                            )
                        )
                    )
            ) {
                Slider(
                    value = currentOpacity.toFloat(),
                    onValueChange = { onOpacityChanged(it.toDouble()) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 색상 선택기
// ──────────────────────────────────────────────

@Composable
private fun ColorSelector(
    currentColor: String,
    onColorChanged: (String) -> Unit
) {
    // 현재 색상의 Hue 값 계산
    val currentHue = remember(currentColor) { hexToHue(currentColor) }
    var sliderPosition by remember(currentColor) { mutableStateOf(currentHue) }

    Column {
        // 레인보우 그라데이션 슬라이더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = (0..10).map { i ->
                            Color.hsv(i * 36f, 0.85f, 0.9f)
                        }
                    )
                )
        ) {
            // 슬라이더 오버레이
            Slider(
                value = sliderPosition,
                onValueChange = { newHue ->
                    sliderPosition = newHue
                    onColorChanged(hueToHex(newHue))
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 현재 선택된 색상 미리보기
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        try { Color(android.graphics.Color.parseColor(currentColor)) }
                        catch (e: Exception) { Color.Red }
                    )
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
            )
            Text(
                text = currentColor.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        widthOptions.forEach { width ->
            val isSelected = width == currentWidth
            val dotSize = (width * 2.5).coerceIn(5.0, 25.0).dp

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onWidthChanged(width) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            CircleShape
                        )
                )
            }
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
    return try {
        val color = android.graphics.Color.parseColor(hex)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[0] // Hue (0~360)
    } catch (e: Exception) {
        0f
    }
}

/**
 * Hue 값(0~360)을 HEX 문자열로 변환
 * Saturation=0.85, Brightness=0.9 고정
 */
private fun hueToHex(hue: Float): String {
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.9f))
    return String.format("#%06X", 0xFFFFFF and color)
}
