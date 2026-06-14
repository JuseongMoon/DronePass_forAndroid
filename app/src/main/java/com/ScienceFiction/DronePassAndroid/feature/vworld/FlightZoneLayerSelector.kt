package com.ScienceFiction.DronePassAndroid.feature.vworld

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer

/**
 * 비행구역 레이어 선택 BottomSheet (iOS LayerSelectionSheet 정합)
 *
 * iOS 의 NavigationView + (배너 / 통계 박스 / 액션 2버튼 / 레이어 목록) 구조를
 * Compose 의 ModalBottomSheet 위에 재현한다.
 */
private val InfoBannerAccent = Color(0xFF007AFF)     // iOS systemBlue
private val SelectAllButtonBackground = Color(0x1A007AFF) // iOS Color.blue.opacity(0.1)
private val DeselectAllButtonBackground = Color(0x1AFF3B30) // iOS Color.red.opacity(0.1)
private val DeselectAllButtonForeground = Color(0xFFFF3B30) // iOS .red
private val SelectedRowBackground = Color(0x0D007AFF) // iOS .blue.opacity(0.05)
internal val FlightZoneLayerSelectorLegalNoticeBackgroundColor = Color(0x1A007AFF) // iOS .blue.opacity(0.1)
internal val FlightZoneLayerSelectorLegalNoticeTopPadding = 16.dp
internal val FlightZoneLayerSelectorLegalNoticeBottomPadding = 8.dp
internal val FlightZoneLayerSelectorLegalNoticeBorderWidth = 1.dp
internal val FlightZoneLayerSelectorLegalNoticeBorderColor = Color(0x4D007AFF) // iOS .blue.opacity(0.3)
internal val FlightZoneLayerSelectorStatHeaderOuterHorizontalPadding = 0.dp
internal val FlightZoneLayerSelectorStatHeaderOuterVerticalPadding = 0.dp
internal val FlightZoneLayerSelectorDividerLeadingPadding = 60.dp

internal fun sortedFlightZoneLayersForSelector(
    displayNameOf: (FlightZoneLayer) -> String,
): List<FlightZoneLayer> = FlightZoneLayer.entries.sortedBy(displayNameOf)

internal fun shouldShowFlightZoneLayerDividerAfterItem(
    index: Int,
    lastIndex: Int,
): Boolean = index in 0..lastIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightZoneLayerSelector(
    visibleLayers: Set<FlightZoneLayer>,
    displayedZoneCount: Int,
    onToggleLayer: (FlightZoneLayer) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Localized displayName 가나다순 정렬 (iOS sorted(by: $0.displayName < $1.displayName))
    val sortedLayers = sortedFlightZoneLayersForSelector { context.getString(it.displayNameRes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // NavigationHeader (iOS NavigationView 흉내)
            NavigationHeader(
                title = stringResource(R.string.flight_zone_navigation_title),
                trailingText = stringResource(R.string.flight_zone_navigation_done),
                onTrailingClick = onDismiss
            )

            // 법적 면책 배너 (iOS LayerSelectionSheet 의 blue.opacity(0.1) 박스)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(
                        top = FlightZoneLayerSelectorLegalNoticeTopPadding,
                        bottom = FlightZoneLayerSelectorLegalNoticeBottomPadding,
                    ),
                colors = CardDefaults.cardColors(containerColor = FlightZoneLayerSelectorLegalNoticeBackgroundColor),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = FlightZoneLayerSelectorLegalNoticeBorderWidth,
                    color = FlightZoneLayerSelectorLegalNoticeBorderColor,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = InfoBannerAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.flight_zone_legal_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // 통계 헤더 (iOS LayerSelectionSheet:97-118)
            StatHeader(
                selectedCount = visibleLayers.size,
                totalCount = FlightZoneLayer.entries.size,
                displayedCount = displayedZoneCount
            )

            // 전체 선택/해제 액션 버튼 (iOS LayerSelectionSheet:123-153)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BigActionButton(
                    text = stringResource(R.string.flight_zone_select_all),
                    icon = Icons.Default.CheckCircle,
                    containerColor = SelectAllButtonBackground,
                    contentColor = InfoBannerAccent,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShowAll()
                    },
                    modifier = Modifier.weight(1f)
                )
                BigActionButton(
                    text = stringResource(R.string.flight_zone_deselect_all),
                    icon = Icons.Default.Cancel,
                    containerColor = DeselectAllButtonBackground,
                    contentColor = DeselectAllButtonForeground,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHideAll()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // 레이어 목록
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(sortedLayers) { index, layer ->
                    LayerItem(
                        layer = layer,
                        isChecked = visibleLayers.contains(layer),
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleLayer(layer)
                        }
                    )
                    if (shouldShowFlightZoneLayerDividerAfterItem(index, sortedLayers.lastIndex)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = FlightZoneLayerSelectorDividerLeadingPadding,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 시트 상단 네비게이션 헤더 (iOS NavigationView inline title + trailing button 매핑)
 */
@Composable
private fun NavigationHeader(
    title: String,
    trailingText: String,
    onTrailingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(60.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onTrailingClick,
            modifier = Modifier.width(60.dp)
        ) {
            Text(
                text = trailingText,
                color = InfoBannerAccent
            )
        }
    }
}

/**
 * 통계 헤더 (좌: 선택된 레이어 N/13, 우: 표시된 구역 M)
 */
@Composable
private fun StatHeader(
    selectedCount: Int,
    totalCount: Int,
    displayedCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = FlightZoneLayerSelectorStatHeaderOuterHorizontalPadding,
                vertical = FlightZoneLayerSelectorStatHeaderOuterVerticalPadding,
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.flight_zone_stats_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.flight_zone_stats_count_format,
                        selectedCount,
                        totalCount
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.flight_zone_stats_displayed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayedCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = InfoBannerAccent
                )
            }
        }
    }
}

/**
 * 큰 액션 버튼 (전체 선택 / 전체 해제 — iOS Label + frame(maxWidth: .infinity) 매핑)
 */
@Composable
private fun BigActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(
                8.dp,
                alignment = Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 단일 레이어 행 (iOS LayerRow 매핑: 체크 아이콘 + 색상박스(fill+border) + displayName)
 */
@Composable
private fun LayerItem(
    layer: FlightZoneLayer,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        color = if (isChecked) SelectedRowBackground else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isChecked) InfoBannerAccent else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // iOS RoundedRectangle 24x24 (fill=overlayColor, border 2pt=borderColor)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(layer.fillColor.toInt()))
                    .border(
                        width = 2.dp,
                        color = Color(layer.borderColor.toInt()),
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(layer.displayNameRes),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
