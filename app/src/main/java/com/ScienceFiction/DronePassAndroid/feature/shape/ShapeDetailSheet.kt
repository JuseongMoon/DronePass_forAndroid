package com.ScienceFiction.DronePassAndroid.feature.shape

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.annotation.StringRes
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.ui.currentWindowSizeDp
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import java.util.Date

internal enum class ExternalMapProvider(@StringRes val labelRes: Int) {
    NAVER(R.string.shape_detail_open_naver_map),
    KAKAO(R.string.shape_detail_open_kakao_map),
    TMAP(R.string.shape_detail_open_tmap),
    GOOGLE(R.string.shape_detail_open_google_map),
}

internal data class ExternalMapTarget(
    val provider: ExternalMapProvider,
    val appUri: String,
    val packageName: String,
    val webFallbackUri: String? = null,
) {
    val marketUri: String = "market://details?id=$packageName"
    val playStoreUri: String = "https://play.google.com/store/apps/details?id=$packageName"
}

internal fun buildExternalMapTargets(
    latitude: Double,
    longitude: Double,
    encodedName: String,
    koreaFeaturesEnabled: Boolean,
): List<ExternalMapTarget> {
    val googleMaps = ExternalMapTarget(
        provider = ExternalMapProvider.GOOGLE,
        appUri = "google.navigation:q=$latitude,$longitude&mode=d",
        packageName = "com.google.android.apps.maps",
        webFallbackUri = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude",
    )
    if (!koreaFeaturesEnabled) return listOf(googleMaps)

    return listOf(
        ExternalMapTarget(
            provider = ExternalMapProvider.NAVER,
            appUri = "nmap://route/public?dlat=$latitude&dlng=$longitude&dname=$encodedName",
            packageName = "com.nhn.android.nmap",
            webFallbackUri = "https://map.naver.com/v5/?c=$longitude,$latitude,15,0,0,0,dh",
        ),
        ExternalMapTarget(
            provider = ExternalMapProvider.KAKAO,
            appUri = "kakaomap://route?ep=$latitude,$longitude&by=CAR",
            packageName = "net.daum.android.map",
            webFallbackUri = "https://map.kakao.com/link/map/$latitude,$longitude",
        ),
        ExternalMapTarget(
            provider = ExternalMapProvider.TMAP,
            appUri = "tmap://route?goalname=$encodedName&goalx=$longitude&goaly=$latitude",
            packageName = "com.skt.tmap.ku",
        ),
        googleMaps,
    )
}

internal fun encodeExternalMapDestinationName(destinationName: String): String =
    java.net.URLEncoder.encode(destinationName, Charsets.UTF_8.name()).replace("+", "%20")

internal const val ShapeDetailSheetHeightFraction = 0.8f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShapeDetailSheet(
    shape: ShapeModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit = {},
    drone: DroneModel? = null,
    activeDrones: List<DroneModel> = emptyList(),
    koreaFeaturesEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val sheetHeight = currentWindowSizeDp().height * ShapeDetailSheetHeightFraction
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExternalMapDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCopyToast by remember { mutableStateOf(false) }
    var copyToastMessage by remember { mutableStateOf("") }
    var copyToastGeneration by remember { mutableIntStateOf(0) }
    var memoWebUrl by remember { mutableStateOf<String?>(null) }
    val copiedMessage = stringResource(R.string.shape_detail_copied)

    val dateFormat = remember { localizedShapeDateTimeFormat() }
    val detailDrone = resolveShapeDetailDrone(
        shapeDroneId = shape.droneId,
        matchedDrone = drone,
        activeDrones = activeDrones,
    )

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

    fun copyAndShowToast(text: String) {
        copyToClipboard(context, text)
        hapticFeedback.performHapticFeedback(ShapeDetailCopyHapticFeedbackType)
        copyToastMessage = copiedMessage
        showCopyToast = true
        copyToastGeneration += 1
    }

    LaunchedEffect(copyToastGeneration) {
        if (copyToastGeneration == 0) return@LaunchedEffect
        delay(ShapeDetailCopyToastDurationMs)
        showCopyToast = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            // iOS NavigationView 정합 — 고정 헤더 TopAppBar.
            // title: inline 제목, actions: ellipsis.circle → DropdownMenu(편집/복제/삭제).
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
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = stringResource(R.string.shape_detail_more_menu),
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shape_detail_edit)) },
                            onClick = {
                                showMoreMenu = false
                                hideAndThen(onEdit)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shape_detail_duplicate)) },
                            onClick = {
                                showMoreMenu = false
                                hideAndThen(onDuplicate)
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

                // 드론 — iOS connectedDrone 상태 분기 (정상/삭제됨/레거시 fallback/미할당)
                ShapeDetailRow(label = stringResource(R.string.shape_detail_drone_connected)) {
                    DroneStatusValue(drone = detailDrone, droneId = shape.droneId)
                }
                ShapeDetailRowDivider()

                // 제목
                ShapeDetailRow(label = stringResource(R.string.shape_detail_title_label)) {
                    Text(
                        text = shapeDetailTitleText(shape.title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ShapeDetailRowDivider()

                // 좌표 — DMS 표시 + 길게 누름으로 십진수 복사 (iOS copyableText 정합)
                ShapeDetailRow(
                    label = stringResource(R.string.shape_detail_coordinate),
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            copyAndShowToast(shape.baseCoordinate.decimalCoordinate)
                        },
                    ),
                ) {
                    Text(
                        text = shape.baseCoordinate.formattedCoordinate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ShapeDetailRowDivider()

                // 주소 — iOS 는 nil 도 "-" 로 표시하며 탭 시 좌표 기반 외부 지도 다이얼로그를 연다.
                ShapeDetailRow(
                    label = stringResource(R.string.shape_detail_address),
                    modifier = Modifier.combinedClickable(
                        onClick = { showExternalMapDialog = true },
                        onLongClick = {
                            copyableShapeDetailAddress(shape.address)?.let { address ->
                                copyAndShowToast(address)
                            }
                        },
                    ),
                ) {
                    Text(
                        text = formatShapeDetailAddress(shape.address),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF007AFF), // iOS .blue 정합
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 반경 (조건부)
                if (shape.radius != null) {
                    ShapeDetailRowDivider()
                    ShapeDetailRow(label = stringResource(R.string.shape_detail_radius)) {
                        Text(
                            text = formatMeters(shape.radius),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 고도 (조건부)
                if (shape.height != null) {
                    ShapeDetailRowDivider()
                    ShapeDetailRow(label = stringResource(R.string.shape_detail_altitude)) {
                        Text(
                            text = formatMeters(shape.height),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 비행 시작일
                ShapeDetailRowDivider()
                ShapeDetailRow(label = stringResource(R.string.shape_detail_flight_start)) {
                    Text(
                        text = dateFormat.format(Date(shape.flightStartDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 비행 종료일 (조건부)
                if (shape.flightEndDate != null) {
                    ShapeDetailRowDivider()
                    ShapeDetailRow(label = stringResource(R.string.shape_detail_flight_end)) {
                        Text(
                            text = dateFormat.format(Date(shape.flightEndDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // iOS Section 2 — 메모. 값이 없어도 "-" 로 180dp 섹션을 항상 표시한다.
                // AndroidView(TextView) + Linkify 로 URL/전화/이메일 자동 감지.
                // 웹 링크는 iOS SafariView처럼 앱 내부 시트로 열고, 전화/메일은 시스템 앱으로 전달한다.
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.common_memo),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MemoLinkifyView(
                    text = formatShapeDetailMemo(shape.memo),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
                    onUrlClick = { url ->
                        when (resolveShapeDetailMemoLinkAction(url)) {
                            ShapeDetailMemoLinkAction.IN_APP_WEB -> memoWebUrl = url
                            ShapeDetailMemoLinkAction.SYSTEM_INTENT -> openMemoSystemLink(context, url)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
            }
            ShapeDetailCopyToast(
                visible = showCopyToast,
                message = copyToastMessage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.shape_detail_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.shape_detail_delete_message,
                        shapeDetailTitleText(shape.title),
                    )
                )
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
        ExternalMapActionSheet(
            latitude = shape.baseCoordinate.latitude,
            longitude = shape.baseCoordinate.longitude,
            destinationName = shapeDetailTitleText(shape.title),
            koreaFeaturesEnabled = koreaFeaturesEnabled,
            onDismiss = { showExternalMapDialog = false }
        )
    }

    memoWebUrl?.let { url ->
        ShapeDetailMemoWebSheet(
            url = url,
            onDismiss = { memoWebUrl = null },
        )
    }
}

/**
 * 외부 지도 앱 선택 액션 시트 — iOS confirmationDialog + mapButtons 정합.
 *
 * @param koreaFeaturesEnabled ON: Naver/Kakao/TMAP/Google 4개 / OFF: Google 만
 * @param destinationName 도형 제목 — URL 의 goalname/dname 으로 사용
 *
 * 모든 URL 은 **길찾기(route)** 형식 — iOS mapButtons 정합.
 * Google 은 Android 표준 `https://www.google.com/maps/dir/?api=1` 사용 (iOS comgooglemaps:// scheme 미적용).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalMapActionSheet(
    latitude: Double,
    longitude: Double,
    destinationName: String,
    koreaFeaturesEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = ShapeDetailExternalMapSkipPartiallyExpanded,
    )
    val encodedName = remember(destinationName) {
        encodeExternalMapDestinationName(destinationName)
    }
    val targets = remember(latitude, longitude, encodedName, koreaFeaturesEnabled) {
        buildExternalMapTargets(
            latitude = latitude,
            longitude = longitude,
            encodedName = encodedName,
            koreaFeaturesEnabled = koreaFeaturesEnabled,
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.shape_detail_open_external_map),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.shape_detail_navigation_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            targets.forEachIndexed { index, target ->
                TextButton(
                    onClick = {
                        onDismiss()
                        openExternalMap(context = context, target = target)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(target.provider.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (index != targets.lastIndex) {
                    HorizontalDivider()
                }
            }

            HorizontalDivider()
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    }
}

/**
 * 외부 지도 앱을 열거나, 앱이 없으면 iOS App Store fallback 과 동등하게 Play Store 로 이동한다.
 */
private fun openExternalMap(
    context: Context,
    target: ExternalMapTarget,
) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target.appUri)).apply {
        setPackage(target.packageName)
    }
    if (tryStartActivity(context, appIntent)) return

    if (tryStartActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(target.marketUri)))) return
    if (tryStartActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(target.playStoreUri)))) return

    target.webFallbackUri?.let { fallback ->
        tryStartActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
    }
}

private fun tryStartActivity(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

private fun formatMeters(value: Double): String = "${value.toInt()} m"

internal fun formatShapeDetailAddress(address: String?): String {
    return address ?: "-"
}

internal fun formatShapeDetailMemo(memo: String?): String {
    return memo ?: "-"
}

internal fun copyableShapeDetailAddress(address: String?): String? {
    return address?.takeIf { it.isNotEmpty() }
}

internal const val ShapeDetailCopyToastDurationMs = 1_500L
internal const val ShapeDetailCopyToastAnimationDurationMs = 300
internal const val ShapeDetailCopyToastBackgroundAlpha = 0.75f
internal val ShapeDetailCopyToastBottomPadding = 50.dp
internal val ShapeDetailCopyHapticFeedbackType = HapticFeedbackType.LongPress
internal val ShapeDetailRowMinHeight = 44.dp
internal val ShapeDetailRowDividerThickness = 0.5.dp
internal val ShapeDetailMemoHeight = 180.dp
internal const val ShapeDetailExternalMapSkipPartiallyExpanded = true
internal const val ShapeDetailMemoWebSheetSkipPartiallyExpanded = true
internal const val ShapeDetailMemoWebSheetHeightFraction = 0.92f
internal const val ShapeDetailMemoWebJavaScriptEnabled = false
internal const val ShapeDetailMemoWebDomStorageEnabled = true
internal const val ShapeDetailMemoWebJavaScriptCanOpenWindowsAutomatically = false
internal const val ShapeDetailMemoWebAllowFileAccess = false
internal const val ShapeDetailMemoWebAllowContentAccess = false
internal const val ShapeDetailMemoWebAllowFileAccessFromFileUrls = false
internal const val ShapeDetailMemoWebAllowUniversalAccessFromFileUrls = false
internal const val ShapeDetailMemoWebSafeBrowsingEnabled = true
internal const val ShapeDetailMemoWebMixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

@Composable
private fun ShapeDetailCopyToast(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.padding(bottom = ShapeDetailCopyToastBottomPadding),
        enter = slideInVertically(
            animationSpec = tween(ShapeDetailCopyToastAnimationDurationMs),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(ShapeDetailCopyToastAnimationDurationMs)),
        exit = slideOutVertically(
            animationSpec = tween(ShapeDetailCopyToastAnimationDurationMs),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(ShapeDetailCopyToastAnimationDurationMs)),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = ShapeDetailCopyToastBackgroundAlpha),
                    shape = CircleShape,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

internal fun resolveShapeDetailDrone(
    shapeDroneId: String?,
    matchedDrone: DroneModel?,
    activeDrones: List<DroneModel>,
): DroneModel? {
    return if (shapeDroneId == null) {
        activeDrones.firstOrNull()
    } else {
        matchedDrone?.takeIf { it.id == shapeDroneId }
    }
}

/**
 * iOS HyperlinkTextView + SafariView 정합.
 *
 * AndroidView 로 TextView 호스팅 + autoLinkMask 로 URL/전화번호/이메일을 자동 감지한다.
 * 웹 링크는 앱 내부 웹 시트로, 전화/메일 등은 시스템 앱으로 전달한다.
 * 180dp 고정 높이 (iOS minHeight: 180 / maxHeight: 180 정합) + 내부 스크롤.
 */
@Composable
private fun MemoLinkifyView(
    text: String,
    textColor: Int,
    onUrlClick: (String) -> Unit,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(ShapeDetailMemoHeight),
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 16f
                setTextColor(textColor)
                autoLinkMask = ShapeDetailMemoAutoLinkMask
                linksClickable = true
                movementMethod = LinkMovementMethod.getInstance()
                setPadding(0, 8, 0, 8)
            }
        },
        update = { textView ->
            textView.text = buildShapeDetailMemoLinkText(
                text = text,
                onUrlClick = onUrlClick,
            )
        },
    )
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShapeDetailMemoWebSheet(
    url: String,
    onDismiss: () -> Unit,
) {
    val sheetHeight = currentWindowSizeDp().height * ShapeDetailMemoWebSheetHeightFraction
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = ShapeDetailMemoWebSheetSkipPartiallyExpanded,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .navigationBarsPadding(),
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_close))
                    }
                },
            )
            HorizontalDivider()
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = ShapeDetailMemoWebJavaScriptEnabled
                        settings.domStorageEnabled = ShapeDetailMemoWebDomStorageEnabled
                        settings.javaScriptCanOpenWindowsAutomatically =
                            ShapeDetailMemoWebJavaScriptCanOpenWindowsAutomatically
                        settings.allowFileAccess = ShapeDetailMemoWebAllowFileAccess
                        settings.allowContentAccess = ShapeDetailMemoWebAllowContentAccess
                        settings.allowFileAccessFromFileURLs = ShapeDetailMemoWebAllowFileAccessFromFileUrls
                        settings.allowUniversalAccessFromFileURLs =
                            ShapeDetailMemoWebAllowUniversalAccessFromFileUrls
                        settings.safeBrowsingEnabled = ShapeDetailMemoWebSafeBrowsingEnabled
                        settings.mixedContentMode = ShapeDetailMemoWebMixedContentMode
                        loadUrl(url)
                    }
                },
                update = {},
            )
        }
    }
}

internal enum class ShapeDetailMemoLinkAction {
    IN_APP_WEB,
    SYSTEM_INTENT,
}

internal fun resolveShapeDetailMemoLinkAction(url: String): ShapeDetailMemoLinkAction {
    val normalized = url.trim()
    return if (
        normalized.startsWith("https://", ignoreCase = true) ||
        normalized.startsWith("http://", ignoreCase = true)
    ) {
        ShapeDetailMemoLinkAction.IN_APP_WEB
    } else {
        ShapeDetailMemoLinkAction.SYSTEM_INTENT
    }
}

private fun buildShapeDetailMemoLinkText(
    text: String,
    onUrlClick: (String) -> Unit,
): SpannableString {
    val spannable = SpannableString(text)
    Linkify.addLinks(spannable, ShapeDetailMemoAutoLinkMask)
    val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
    spans.forEach { span ->
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val flags = spannable.getSpanFlags(span)
        val url = span.url
        spannable.removeSpan(span)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onUrlClick(url)
                }
            },
            start,
            end,
            flags.takeIf { it != 0 } ?: Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    return spannable
}

private fun openMemoSystemLink(context: Context, url: String) {
    tryStartActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
            .heightIn(min = ShapeDetailRowMinHeight)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            value()
        }
    }
}

@Composable
private fun ShapeDetailRowDivider() {
    HorizontalDivider(
        thickness = ShapeDetailRowDividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * iOS connectedDrone 상태 분기 (정상 / 삭제됨 / 레거시 fallback / 미할당) 정합.
 *  - 정상: 색상 원(12dp) + 이름(secondary)
 *  - 삭제됨 (droneId != null, drone == null): 경고 아이콘(orange) + "삭제된 드론" italic
 *  - 레거시 fallback (droneId == null, activeDrones.first != null): 첫 번째 활성 드론 표시
 *  - 미할당 (droneId == null, activeDrones.isEmpty): 물음표 아이콘(gray) + "드론 미연결" italic
 */
@Composable
private fun DroneStatusValue(drone: DroneModel?, droneId: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            drone != null -> {
                drone.paletteColor?.takeIf(::shouldShowShapeDetailDroneColorIndicator)?.let { paletteColor ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(paletteColor.composeColor),
                    )
                }
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
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
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

internal fun shouldShowShapeDetailDroneColorIndicator(color: PaletteColor?): Boolean = color != null

internal const val ShapeDetailMemoAutoLinkMask =
    Linkify.WEB_URLS or Linkify.PHONE_NUMBERS or Linkify.EMAIL_ADDRESSES

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    val clip = ClipData.newPlainText(context.getString(R.string.shape_detail_clipboard_label), text)
    clipboardManager.setPrimaryClip(clip)
}
