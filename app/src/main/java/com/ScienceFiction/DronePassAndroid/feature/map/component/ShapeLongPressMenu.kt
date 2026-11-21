package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.map.MapViewModel
import com.ScienceFiction.DronePassAndroid.feature.map.ShapeLongPressState
import com.ScienceFiction.DronePassAndroid.feature.shape.shapeDetailTitleText
import com.ScienceFiction.DronePassAndroid.ui.component.IosDropdownMenu
import com.ScienceFiction.DronePassAndroid.ui.component.IosMenuDivider
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import kotlin.math.roundToInt

/**
 * 지도 도형 길게 누르기 메뉴와 삭제 확인 창(D-83 (2)). 지도를 채운 Box 안에 둔다.
 * 메뉴는 누른 자리에 붙는 팝업 메뉴다(Android 문맥 메뉴 모양, 앱의 다른 메뉴와 같은 [IosDropdownMenu]).
 * 바깥을 누르거나 뒤로 가면 닫히므로 '취소' 항목은 두지 않는다.
 */
@Composable
internal fun ShapeLongPressLayer(viewModel: MapViewModel) {
    val state by viewModel.shapeLongPressState.collectAsStateWithLifecycle()
    val shapes by viewModel.activeShapes.collectAsStateWithLifecycle()
    val current = state ?: return
    val shape = shapes.firstOrNull { it.id == current.shapeId }
    if (shape == null) {
        // 메뉴를 띄운 사이 다른 기기에서 지워졌다
        LaunchedEffect(current) { viewModel.dismissShapeLongPress() }
        return
    }
    val title = shapeDetailTitleText(shape.title)
    when (current) {
        is ShapeLongPressState.Menu -> ShapeLongPressMenu(
            title = title,
            anchor = IntOffset(current.anchorX.roundToInt(), current.anchorY.roundToInt()),
            onEdit = viewModel::editFromShapeLongPress,
            onDelete = viewModel::requestDeleteFromShapeLongPress,
            onNewShapeHere = viewModel::newShapeHereFromShapeLongPress,
            onDismiss = viewModel::dismissShapeLongPress,
        )
        is ShapeLongPressState.ConfirmDelete -> ShapeLongPressDeleteDialog(
            title = title,
            onConfirm = viewModel::confirmDeleteFromShapeLongPress,
            onDismiss = viewModel::dismissShapeLongPress,
        )
    }
}

@Composable
private fun ShapeLongPressMenu(
    title: String,
    anchor: IntOffset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNewShapeHere: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 크기 0인 앵커를 누른 자리에 두고 메뉴를 붙인다. 화면 끝에서는 메뉴가 안쪽으로 접혀 열린다.
    Box(modifier = Modifier.offset { anchor }) {
        IosDropdownMenu(expanded = true, onDismissRequest = onDismiss) {
            Text(
                text = title,
                color = IosSecondaryLabel,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            IosMenuDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.map_shape_menu_edit)) },
                onClick = onEdit,
            )
            IosMenuDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = onDelete,
            )
            IosMenuDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.map_shape_menu_new_shape_here)) },
                onClick = onNewShapeHere,
            )
        }
    }
}

/** 도형 상세의 삭제 확인 창과 같은 문구·모양 */
@Composable
private fun ShapeLongPressDeleteDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shape_detail_delete_title)) },
        text = { Text(stringResource(R.string.shape_detail_delete_message, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.common_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
