package com.ScienceFiction.DronePassAndroid.feature.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.ScienceFiction.DronePassAndroid.feature.drone.shouldAcceptDroneNameChange
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRowMinHeight
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.IosNavBarButtonFontSize
import com.ScienceFiction.DronePassAndroid.ui.theme.IosPlaceholderText
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

private val TagEditHeaderHeight = 44.dp
private val TagEditHeaderSideWidth = 72.dp

/**
 * 태그 추가·이름 변경 (이름 하나만 입력). iOS TagEditView 정합.
 * [tag] 가 null 이면 추가 모드이며, 무료 한도는 저장할 때 다시 판정한다(막히면 입력 화면에 머문다).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagEditSheet(
    tag: TagModel?,
    viewModel: TagViewModel,
    onDismiss: () -> Unit,
    /** 추가 모드에서 저장한 태그 (도형 편집이 바로 선택하도록) */
    onSaved: (TagModel) -> Unit = {},
) {
    val isAddMode = tag == null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val nameTextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, letterSpacing = 0.sp)
    val maxWidthPx = with(density) { TAG_NAME_MAX_WIDTH_DP.dp.toPx() }
    var name by remember(tag?.id) { mutableStateOf(tag?.name.orEmpty()) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val duplicateMessage = stringResource(R.string.tag_edit_alert_duplicate)
    val trimmedName = name.trim()

    fun save() {
        if (trimmedName.isEmpty() || isSaving) return
        if (viewModel.isDuplicateName(trimmedName, tag?.id)) {
            alertMessage = duplicateMessage
            return
        }
        if (tag != null) {
            if (trimmedName != tag.name) viewModel.renameTag(tag.id, trimmedName)
            onDismiss()
            return
        }
        isSaving = true
        viewModel.addTag(
            name = trimmedName,
            onAdded = { created ->
                onSaved(created)
                onDismiss()
            },
            onBlocked = { isSaving = false },
        )
    }

    // 입력란이 하나뿐이라 바로 키보드를 띄운다
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    DronePassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosSystemGroupedBackground,
        dragHandle = { Spacer(modifier = Modifier.height(5.dp)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TagEditHeaderHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(TagEditHeaderSideWidth), contentAlignment = Alignment.CenterStart) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel), fontSize = IosNavBarButtonFontSize, fontWeight = FontWeight.Normal)
                    }
                }
                Text(
                    text = stringResource(if (isAddMode) R.string.tag_edit_title_add else R.string.tag_edit_title_edit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.width(TagEditHeaderSideWidth), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = ::save, enabled = trimmedName.isNotEmpty() && !isSaving) {
                        Text(
                            stringResource(if (isAddMode) R.string.drone_edit_add else R.string.common_save),
                            fontSize = IosNavBarButtonFontSize,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }

            InsetGroupedSection(footer = stringResource(R.string.tag_edit_footer)) {
                BasicTextField(
                    value = name,
                    onValueChange = { candidate ->
                        val accept = shouldAcceptDroneNameChange(
                            oldName = name,
                            newName = candidate,
                            maxWidthPx = maxWidthPx,
                            measureTextWidth = { text -> textMeasurer.measure(text, nameTextStyle).size.width.toFloat() },
                        )
                        if (accept) name = candidate
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = InsetGroupedRowMinHeight)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = nameTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.tag_edit_name_placeholder),
                                    style = nameTextStyle,
                                    color = IosPlaceholderText,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }

    alertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text(stringResource(R.string.tag_edit_alert_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { alertMessage = null }) { Text(stringResource(R.string.common_confirm)) }
            },
        )
    }
}
