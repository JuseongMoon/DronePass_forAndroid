package com.ScienceFiction.DronePassAndroid.feature.shape

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShapeDetailSheet(
    shape: ShapeModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit = {},
    drone: DroneModel? = null,
    koreaFeaturesEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // 회전 시 screenHeightDp 가 바뀌어 maxSheetHeight 가 변하면 heightIn 으로 인해
    // 시트 높이가 점프한다. configuration 기준 remember 로 같은 orientation 동안은 안정화하고,
    // 회전 시에는 한 번만 재계산되도록 한다.
    val maxSheetHeight = remember(configuration.orientation, configuration.screenHeightDp) {
        (configuration.screenHeightDp * 0.8f).dp
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExternalMapDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    // Phase 5 에서 사용할 중첩 시트 state — 현재(Phase 1) Menu 의 편집/복제는 기존 콜백 호출 유지.
    // var showEditSheet by remember { mutableStateOf(false) }
    // var showDuplicateSheet by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA) }

    /**
     * 시트 dismiss 애니메이션을 await 한 뒤 외부 액션을 호출한다.
     * Edit 클릭 → 화면 전환 흐름에서 시트가 갑자기 사라지는 대신 매끄럽게 닫히도록 한다.
     */
    fun hideAndThen(action: () -> Unit) {
        coroutineScope.launch {
            runCatching { sheetState.hide() }
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
        ) {
            // iOS NavigationView 정합 — 고정 헤더 TopAppBar.
            // title: inline 제목, actions: ellipsis(MoreVert) → DropdownMenu(편집/복제/삭제).
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shape_detail_navigation_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.shape_detail_more_menu),
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_edit)) },
                            onClick = {
                                showMoreMenu = false
                                hideAndThen(onEdit)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shape_detail_duplicate)) },
                            onClick = {
                                showMoreMenu = false
                                onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.common_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showDeleteConfirmDialog = true
                            },
                        )
                    }
                },
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // iOS Section 1 — 행 순서: 드론 → 제목 → 좌표 → 주소 → 반경 → 고도 → 시작일 → 종료일.
                // 좌측 라벨(bold primary), 우측 값(secondary). StatusBadge 제거.

                // 드론 — iOS connectedDrone 3 상태 분기 (정상/삭제됨/미할당)
                ShapeDetailRow(label = stringResource(R.string.shape_detail_drone_connected)) {
                    DroneStatusValue(drone = drone, droneId = shape.droneId)
                }

                // 제목
                ShapeDetailRow(label = stringResource(R.string.shape_detail_title_label)) {
                    Text(
                        text = shape.title.ifBlank { stringResource(R.string.common_no_title) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 좌표 — DMS 표시 + 길게 누름으로 십진수 복사 (iOS copyableText 정합)
                ShapeDetailRow(
                    label = stringResource(R.string.shape_detail_coordinate),
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            copyToClipboard(context, shape.baseCoordinate.decimalCoordinate)
                        },
                    ),
                ) {
                    Text(
                        text = shape.baseCoordinate.formattedCoordinate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 주소 (조건부) — iOS .blue 링크 + 탭 시 외부 지도 다이얼로그, 길게 누름 시 복사
                if (!shape.address.isNullOrBlank()) {
                    ShapeDetailRow(
                        label = stringResource(R.string.shape_detail_address),
                        modifier = Modifier.combinedClickable(
                            onClick = { showExternalMapDialog = true },
                            onLongClick = {
                                copyToClipboard(context, shape.address)
                            },
                        ),
                    ) {
                        Text(
                            text = shape.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF007AFF), // iOS .blue 정합
                        )
                    }
                }

                // 반경 (조건부)
                if (shape.radius != null) {
                    ShapeDetailRow(label = stringResource(R.string.shape_detail_radius)) {
                        Text(
                            text = "${shape.radius}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 고도 (조건부)
                if (shape.height != null) {
                    ShapeDetailRow(label = stringResource(R.string.shape_detail_altitude)) {
                        Text(
                            text = "${shape.height}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 비행 시작일
                ShapeDetailRow(label = stringResource(R.string.shape_detail_flight_start)) {
                    Text(
                        text = dateFormat.format(Date(shape.flightStartDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 비행 종료일 (조건부)
                if (shape.flightEndDate != null) {
                    ShapeDetailRow(label = stringResource(R.string.shape_detail_flight_end)) {
                        Text(
                            text = dateFormat.format(Date(shape.flightEndDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // iOS Section 2 — 메모 (조건부, HyperlinkTextView 정합)
                // AndroidView(TextView) + Linkify 로 URL/전화/이메일 자동 감지 및 시스템 처리.
                if (!shape.memo.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.common_memo),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MemoLinkifyView(
                        text = shape.memo,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
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

    // 외부 지도 앱 선택 다이얼로그 (주소 행 탭 트리거)
    if (showExternalMapDialog) {
        ExternalMapDialog(
            latitude = shape.baseCoordinate.latitude,
            longitude = shape.baseCoordinate.longitude,
            destinationName = shape.title.ifBlank { stringResource(R.string.common_no_title) },
            koreaFeaturesEnabled = koreaFeaturesEnabled,
            onDismiss = { showExternalMapDialog = false }
        )
    }
}

/**
 * 외부 지도 앱 선택 다이얼로그 — iOS mapButtons 정합.
 *
 * @param koreaFeaturesEnabled ON: Naver/Kakao/TMAP/Google 4개 / OFF: Google 만
 * @param destinationName 도형 제목 — URL 의 goalname/dname 으로 사용
 *
 * 모든 URL 은 **길찾기(route)** 형식 — iOS mapButtons 정합.
 * Google 은 Android 표준 `https://www.google.com/maps/dir/?api=1` 사용 (iOS comgooglemaps:// scheme 미적용).
 */
@Composable
private fun ExternalMapDialog(
    latitude: Double,
    longitude: Double,
    destinationName: String,
    koreaFeaturesEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val encodedName = remember(destinationName) {
        java.net.URLEncoder.encode(destinationName, "UTF-8")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shape_detail_open_external_map)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (koreaFeaturesEnabled) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            openExternalMap(
                                context = context,
                                appUri = "nmap://route/public?dlat=$latitude&dlng=$longitude&dname=$encodedName",
                                webFallback = "https://map.naver.com/v5/?c=$longitude,$latitude,15,0,0,0,dh",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.shape_detail_open_naver_map),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    TextButton(
                        onClick = {
                            onDismiss()
                            openExternalMap(
                                context = context,
                                appUri = "kakaomap://route?ep=$latitude,$longitude&by=CAR",
                                webFallback = "https://map.kakao.com/link/map/$latitude,$longitude",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.shape_detail_open_kakao_map),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    TextButton(
                        onClick = {
                            onDismiss()
                            openExternalMap(
                                context = context,
                                appUri = "tmap://route?goalname=$encodedName&goalx=$longitude&goaly=$latitude",
                                webFallback = null, // TMAP 은 웹 폴백 없음
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.shape_detail_open_tmap),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                // Google 은 한국특화 토글과 무관하게 항상 표시 — iOS 정합.
                TextButton(
                    onClick = {
                        onDismiss()
                        openExternalMap(
                            context = context,
                            appUri = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude",
                            webFallback = null, // 이미 web URL
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.shape_detail_open_google_map),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
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

/**
 * iOS HyperlinkTextView (UITextView dataDetectorTypes = [.link, .phoneNumber]) 정합.
 *
 * AndroidView 로 TextView 호스팅 + autoLinkMask = Linkify.ALL 로 시스템이 URL/전화/이메일 자동 감지.
 * 클릭 시 LinkMovementMethod 가 적절한 Intent 실행 (브라우저/Dialer/메일 앱).
 * 180dp 고정 높이 (iOS minHeight: 180 / maxHeight: 180 정합) + 내부 스크롤.
 */
@Composable
private fun MemoLinkifyView(text: String, textColor: Int) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 14f
                setTextColor(textColor)
                autoLinkMask = Linkify.WEB_URLS or Linkify.PHONE_NUMBERS or Linkify.EMAIL_ADDRESSES
                linksClickable = true
                movementMethod = LinkMovementMethod.getInstance()
                setPadding(0, 8, 0, 8)
            }
        },
        update = { it.text = text },
    )
}

/**
 * iOS ShapeDetailView Section 1 의 HStack { Label.bold + Spacer + Value(.secondary) } 정합.
 *
 * @param label 좌측 라벨 (bold primary)
 * @param modifier 행 전체 Modifier (combinedClickable 등 적용)
 * @param value 우측 슬롯 — Text 또는 DroneStatusValue 등 커스텀 콘텐츠
 */
@Composable
private fun ShapeDetailRow(
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        value()
    }
}

/**
 * iOS connectedDrone 3 상태 분기 (정상 / 삭제됨 / 미할당) 정합.
 *  - 정상: 색상 원(12dp) + 이름(secondary)
 *  - 삭제됨 (droneId != null, drone == null): 경고 아이콘(orange) + "삭제된 드론" italic
 *  - 미할당 (droneId == null): 물음표 아이콘(gray) + "드론 미연결" italic
 */
@Composable
private fun DroneStatusValue(drone: DroneModel?, droneId: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            drone != null -> {
                val colorHex = drone.paletteColor?.hex ?: drone.color
                val droneColor = runCatching {
                    Color(android.graphics.Color.parseColor(colorHex))
                }.getOrDefault(MaterialTheme.colorScheme.primary)
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(droneColor),
                )
                Text(
                    text = drone.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            droneId != null -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9500), // iOS .orange
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.shape_detail_drone_deleted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9500),
                    fontStyle = FontStyle.Italic,
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.shape_detail_drone_unassigned),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.shape_detail_clipboard_label), text)
    clipboardManager.setPrimaryClip(clip)
    Toast.makeText(context, context.getString(R.string.shape_detail_copied), Toast.LENGTH_SHORT).show()
}
