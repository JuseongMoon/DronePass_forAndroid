package com.ScienceFiction.DronePassAndroid.feature.drone

import com.ScienceFiction.DronePassAndroid.ui.theme.IosPlaceholderText
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRowMinHeight
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import com.ScienceFiction.DronePassAndroid.ui.component.IosNavBarButtonFontSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import androidx.compose.foundation.layout.Arrangement
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet

private const val DRONE_NAME_MAX_WIDTH_DP = 210
internal val DroneEditMemoMinHeight = 100.dp
internal val DroneEditNavigationHeaderHeight = 44.dp
internal val DroneEditNavigationHeaderSideWidth = 88.dp
internal val DroneColorPickerCircleSize = 24.dp
internal val DroneColorPickerCircleTextSpacing = 8.dp
internal val DroneColorPickerRowVerticalPadding = 12.dp
internal val DroneColorPickerListVerticalPadding = 10.dp
internal val DroneEditFirstSectionTopPadding = 24.dp
internal val DroneColorPickerDividerStartPadding = 56.dp
// iOS checkmark .body 글리프 ≈ Material 22dp
internal val DroneColorPickerCheckmarkSize = 22.dp
internal val DroneColorPickerLabelFontSize = 17.sp
internal const val DroneColorPickerCircleBorderAlpha = 0.2f

/**
 * 드론 생성/편집 BottomSheet.
 * iOS DroneEditView 의 NavigationStack + Form 구조에 맞춰 상단 액션과 섹션형 입력으로 구성한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroneEditSheet(
    drone: DroneModel? = null,
    suggestedName: String = "",
    suggestedColor: PaletteColor = PaletteColor.BLUE,
    isDuplicateName: (String, String?) -> Boolean,
    onSave: (DroneModel) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = drone != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val droneNameTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 17.sp,
        letterSpacing = 0.sp,
    )
    val droneNameMaxWidthPx = with(density) { DRONE_NAME_MAX_WIDTH_DP.dp.toPx() }

    var name by remember(drone?.id, suggestedName) {
        mutableStateOf(drone?.name ?: suggestedName)
    }
    var selectedColor by remember(drone?.id, suggestedColor) {
        mutableStateOf(resolveInitialDroneEditColor(drone, suggestedColor))
    }
    var serialNumber by remember(drone?.id) { mutableStateOf(drone?.serialNumber ?: "") }
    var takeoffWeight by remember(drone?.id) { mutableStateOf(drone?.takeoffWeight ?: "") }
    var size by remember(drone?.id) { mutableStateOf(drone?.size ?: "") }
    var memo by remember(drone?.id) { mutableStateOf(drone?.memo ?: "") }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }

    val selectableColors = remember {
        droneEditSelectableColors()
    }
    val saveFailedTitle = stringResource(R.string.drone_edit_alert_save_failed)
    val nameRequiredMessage = stringResource(R.string.drone_edit_alert_name_required)
    val duplicateNameMessage = stringResource(R.string.drone_edit_alert_name_duplicate)
    val canSave = canSaveDroneEditName(name)

    fun saveDrone() {
        val trimmedName = name.trim()

        when {
            trimmedName.isEmpty() -> {
                saveErrorMessage = nameRequiredMessage
            }
            isDuplicateName(trimmedName, drone?.id) -> {
                saveErrorMessage = duplicateNameMessage
            }
            else -> {
                val resultDrone = (drone ?: DroneModel()).copy(
                    name = trimmedName,
                    color = selectedColor.hex,
                    serialNumber = resolveDroneEditOptionalFieldForSave(
                        value = serialNumber,
                        existingValue = drone?.serialNumber,
                    ),
                    takeoffWeight = resolveDroneEditOptionalFieldForSave(
                        value = takeoffWeight,
                        existingValue = drone?.takeoffWeight,
                    ),
                    size = resolveDroneEditOptionalFieldForSave(
                        value = size,
                        existingValue = drone?.size,
                    ),
                    memo = resolveDroneEditOptionalFieldForSave(
                        value = memo,
                        existingValue = drone?.memo,
                    ),
                    updatedAt = System.currentTimeMillis(),
                )
                onSave(resultDrone)
            }
        }
    }

    // iOS DroneEditView 시트에는 끌기 손잡이가 없다.
    DronePassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosSystemGroupedBackground,
        dragHandle = { Spacer(modifier = Modifier.height(DroneSheetTopInset)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            DroneEditNavigationHeader(
                title = if (isEditMode) {
                    stringResource(R.string.drone_edit_title_edit)
                } else {
                    stringResource(R.string.drone_edit_title_create)
                },
                primaryActionText = stringResource(
                    if (isEditMode) R.string.common_save else R.string.drone_edit_add
                ),
                canSave = canSave,
                onDismiss = onDismiss,
                onSave = ::saveDrone,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // iOS: inline 제목 중심 → 첫 섹션 헤더 약 54pt
                    .padding(top = DroneEditFirstSectionTopPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(InsetGroupedSectionSpacing),
            ) {
                DroneEditSection(
                    header = stringResource(R.string.drone_edit_section_basic),
                    footer = stringResource(R.string.drone_edit_section_basic_footer),
                ) {
                    DroneEditInputField(
                        value = name,
                        onValueChange = { candidate ->
                            if (
                                shouldAcceptDroneNameChange(
                                    oldName = name,
                                    newName = candidate,
                                    maxWidthPx = droneNameMaxWidthPx,
                                    measureTextWidth = { text ->
                                        textMeasurer.measure(
                                            text = text,
                                            style = droneNameTextStyle,
                                        ).size.width.toFloat()
                                    },
                                )
                            ) {
                                name = candidate
                            }
                        },
                        placeholder = stringResource(R.string.drone_edit_name_placeholder),
                        singleLine = true,
                        textStyle = droneNameTextStyle,
                    )
                }

                DroneEditSection(
                    header = stringResource(R.string.drone_edit_color),
                ) {
                    ColorPickerGrid(
                        colors = selectableColors,
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it },
                    )
                }

                DroneEditSection(
                    header = stringResource(R.string.drone_edit_section_serial),
                    footer = stringResource(R.string.drone_edit_section_serial_footer),
                ) {
                    DroneEditInputField(
                        value = serialNumber,
                        onValueChange = { serialNumber = it },
                        placeholder = stringResource(R.string.drone_edit_serial_placeholder),
                        singleLine = true,
                    )
                }

                DroneEditSection(
                    header = stringResource(R.string.drone_edit_section_specs),
                    footer = stringResource(R.string.drone_edit_section_specs_footer),
                ) {
                    DroneEditInputField(
                        value = takeoffWeight,
                        onValueChange = { takeoffWeight = it },
                        placeholder = stringResource(R.string.drone_edit_weight_placeholder),
                        singleLine = true,
                    )
                    InsetGroupedDivider()
                    DroneEditInputField(
                        value = size,
                        onValueChange = { size = it },
                        placeholder = stringResource(R.string.drone_edit_size_placeholder),
                        singleLine = true,
                    )
                }

                DroneEditSection(
                    header = stringResource(R.string.drone_edit_section_memo),
                    footer = stringResource(R.string.drone_edit_section_memo_footer),
                ) {
                    DroneEditInputField(
                        value = memo,
                        onValueChange = { memo = it },
                        placeholder = null,
                        singleLine = false,
                        minLines = 4,
                        modifier = Modifier.heightIn(min = DroneEditMemoMinHeight),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    saveErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { saveErrorMessage = null },
            title = { Text(saveFailedTitle) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { saveErrorMessage = null }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }
}

/**
 * iOS DroneEditView NavigationStack inline title + leading/trailing toolbar 정합.
 */
@Composable
private fun DroneEditNavigationHeader(
    title: String,
    primaryActionText: String,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(DroneEditNavigationHeaderHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(DroneEditNavigationHeaderSideWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), fontSize = IosNavBarButtonFontSize, fontWeight = FontWeight.Normal)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(DroneEditNavigationHeaderSideWidth),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(
                onClick = onSave,
                enabled = canSave,
            ) {
                Text(primaryActionText, fontSize = IosNavBarButtonFontSize, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun DroneEditSection(
    header: String,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    InsetGroupedSection(header = header, footer = footer) {
        content()
    }
}

@Composable
private fun DroneEditInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val placeholderText = resolveDroneEditPlaceholderText(placeholder)
    // iOS Form TextField: 44pt 행, 16pt 좌우 여백, placeholderText 색. Material TextField(56dp)는 쓰지 않는다.
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = InsetGroupedRowMinHeight),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholderText != null) {
                    Text(
                        text = placeholderText,
                        style = textStyle,
                        color = IosPlaceholderText,
                    )
                }
                innerTextField()
            }
        },
    )
}

internal fun shouldAcceptDroneNameChange(
    oldName: String,
    newName: String,
    maxWidthPx: Float,
    measureTextWidth: (String) -> Float,
): Boolean {
    return measureTextWidth(newName) <= maxWidthPx
}

internal fun resolveInitialDroneEditColor(
    drone: DroneModel?,
    suggestedColor: PaletteColor,
): PaletteColor {
    return if (drone != null) {
        drone.paletteColor ?: PaletteColor.BLUE
    } else {
        suggestedColor
    }
}

internal fun droneEditSelectableColors(): List<PaletteColor> =
    PaletteColor.droneSelectableEntries

internal fun canSaveDroneEditName(name: String): Boolean =
    name.trim().isNotEmpty()

internal fun normalizeDroneEditOptionalField(value: String): String? =
    value.takeUnless { it.isBlank() }

internal fun resolveDroneEditOptionalFieldForSave(
    value: String,
    existingValue: String?,
): String? {
    return normalizeDroneEditOptionalField(value) ?: existingValue
}

internal fun resolveDroneEditPlaceholderText(placeholder: String?): String? =
    placeholder?.takeIf { it.isNotEmpty() }

@Composable
fun ColorPickerGrid(
    colors: List<PaletteColor>,
    selectedColor: PaletteColor,
    onColorSelected: (PaletteColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    // iOS ColorPickerGrid 는 목록 위아래에 여백이 있어 카드 안쪽이 약 10pt 더 넓다.
    Column(modifier = modifier.fillMaxWidth().padding(vertical = DroneColorPickerListVerticalPadding)) {
        colors.forEachIndexed { index, color ->
            ColorPickerRow(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) },
            )
            if (index != colors.lastIndex) {
                // iOS 색상 목록은 한 행 안의 커스텀 목록이라 구분선이 오른쪽 여백 앞에서 끝난다.
                InsetGroupedDivider(startIndent = DroneColorPickerDividerStartPadding, endIndent = 16.dp)
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    color: PaletteColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = DroneColorPickerRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DroneColorPickerCircleSize)
                .clip(CircleShape)
                .background(color.composeColor)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = DroneColorPickerCircleBorderAlpha),
                    shape = CircleShape,
                ),
        )
        Spacer(modifier = Modifier.width(DroneColorPickerCircleTextSpacing))
        Text(
            text = color.localizedLabel(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = DroneColorPickerLabelFontSize,
                letterSpacing = 0.sp,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(
                    R.string.drone_edit_color_selected,
                    color.localizedLabel(),
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DroneColorPickerCheckmarkSize),
            )
        }
    }
}
