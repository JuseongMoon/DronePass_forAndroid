package com.ScienceFiction.DronePassAndroid.feature.shape

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeDetailSheet(
    shape: ShapeModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit = {},
    droneName: String? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.8f).dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExternalMapDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 헤더: 제목 + 상태
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shape.title.ifBlank { stringResource(R.string.common_no_title) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(shape = shape)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 도형 타입
            DetailRow(
                label = stringResource(R.string.shape_detail_type),
                value = shape.shapeType.koreanName
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 좌표 (DMS 형식 + 복사 버튼)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.shape_detail_coordinate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = shape.baseCoordinate.formattedCoordinate,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(
                    onClick = {
                        copyToClipboard(
                            context,
                            shape.baseCoordinate.decimalCoordinate
                        )
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.shape_detail_copy_coordinate),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 주소 (있으면)
            if (!shape.address.isNullOrBlank()) {
                DetailRow(
                    label = stringResource(R.string.shape_detail_address),
                    value = shape.address
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 반경
            if (shape.radius != null) {
                DetailRow(
                    label = stringResource(R.string.shape_detail_radius),
                    value = "${shape.radius}m"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 고도 (있으면)
            if (shape.height != null) {
                DetailRow(
                    label = stringResource(R.string.shape_detail_altitude),
                    value = "${shape.height}m"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 연결된 드론 정보
            DetailRow(
                label = stringResource(R.string.shape_detail_drone_connected),
                value = droneName ?: stringResource(R.string.shape_detail_no_drone)
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // 비행 기간
            DetailRow(
                label = stringResource(R.string.shape_detail_flight_start),
                value = dateFormat.format(Date(shape.flightStartDate))
            )
            Spacer(modifier = Modifier.height(8.dp))

            DetailRow(
                label = stringResource(R.string.shape_detail_flight_end),
                value = shape.flightEndDate?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.common_not_set)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 메모 (있으면)
            if (!shape.memo.isNullOrBlank()) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow(
                    label = stringResource(R.string.common_memo),
                    value = shape.memo
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // 액션 버튼 행 1: 삭제 / 편집
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
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
                        contentDescription = stringResource(R.string.common_edit),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.common_edit))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 액션 버튼 행 2: 복제 / 외부 지도 앱 열기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CopyAll,
                        contentDescription = stringResource(R.string.shape_detail_duplicate),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.shape_detail_duplicate))
                }
                OutlinedButton(
                    onClick = { showExternalMapDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = stringResource(R.string.shape_detail_open_external_map),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.shape_detail_open_external_map))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.shape_detail_delete_title)) },
            text = {
                Text(stringResource(R.string.shape_detail_delete_message, shape.title.ifBlank { stringResource(R.string.common_no_title) }))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 외부 지도 앱 선택 다이얼로그
    if (showExternalMapDialog) {
        ExternalMapDialog(
            latitude = shape.baseCoordinate.latitude,
            longitude = shape.baseCoordinate.longitude,
            onDismiss = { showExternalMapDialog = false }
        )
    }
}

/**
 * 외부 지도 앱 선택 다이얼로그
 */
@Composable
private fun ExternalMapDialog(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shape_detail_open_external_map)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        onDismiss()
                        openExternalMap(
                            context = context,
                            appUri = "nmap://map?lat=$latitude&lng=$longitude&zoom=15",
                            webFallback = "https://map.naver.com/v5/?c=$longitude,$latitude,15,0,0,0,dh"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.shape_detail_open_naver_map),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        openExternalMap(
                            context = context,
                            appUri = "kakaomap://look?p=$latitude,$longitude",
                            webFallback = "https://map.kakao.com/link/map/$latitude,$longitude"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.shape_detail_open_kakao_map),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        openExternalMap(
                            context = context,
                            appUri = "tmap://route?goalx=$longitude&goaly=$latitude",
                            webFallback = null // TMAP은 웹 폴백 없음
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.shape_detail_open_tmap),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        openExternalMap(
                            context = context,
                            appUri = "geo:$latitude,$longitude?z=15",
                            webFallback = "https://www.google.com/maps/@$latitude,$longitude,15z"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.shape_detail_open_google_map),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/**
 * 외부 지도 앱을 열거나 웹 폴백으로 이동
 */
private fun openExternalMap(
    context: Context,
    appUri: String,
    webFallback: String?
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUri))
    val resolveInfo = context.packageManager.resolveActivity(intent, 0)
    if (resolveInfo != null) {
        context.startActivity(intent)
    } else if (webFallback != null) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback))
        context.startActivity(webIntent)
    }
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

@Composable
private fun StatusBadge(shape: ShapeModel) {
    val (statusText, statusColor) = when {
        shape.isExpired -> stringResource(R.string.shape_detail_status_expired) to MaterialTheme.colorScheme.error
        shape.isNotStarted -> stringResource(R.string.shape_detail_status_not_started) to MaterialTheme.colorScheme.tertiary
        else -> stringResource(R.string.shape_detail_status_active) to MaterialTheme.colorScheme.primary
    }

    androidx.compose.material3.SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.shape_detail_clipboard_label), text)
    clipboardManager.setPrimaryClip(clip)
    Toast.makeText(context, context.getString(R.string.shape_detail_copied), Toast.LENGTH_SHORT).show()
}
