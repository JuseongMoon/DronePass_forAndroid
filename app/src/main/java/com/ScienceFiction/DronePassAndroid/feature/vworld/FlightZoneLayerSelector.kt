package com.ScienceFiction.DronePassAndroid.feature.vworld

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer

/**
 * 비행구역 레이어 선택 BottomSheet
 *
 * 각 레이어를 토글(체크박스 + 색상 표시 + 레이어명)할 수 있으며,
 * 전체 선택/해제 버튼을 제공한다.
 */
private val InfoBannerBackground = Color(0xFFE3F2FD) // iOS systemBlue.opacity(0.1) 매핑
private val InfoBannerAccent = Color(0xFF007AFF)     // iOS systemBlue

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // 헤더
            Text(
                text = stringResource(R.string.flight_zone_layer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 법적 면책 배너 (iOS LayerSelectionSheet 의 blue.opacity(0.1) 박스 매핑)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = InfoBannerBackground),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = InfoBannerAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.flight_zone_legal_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // 레이어 통계 + 표시 중인 구역 수 (iOS LayerSelectionSheet:97-118 매핑)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.flight_zone_layer_count,
                        visibleLayers.size,
                        FlightZoneLayer.entries.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (displayedZoneCount > 0) {
                    Text(
                        text = "$displayedZoneCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InfoBannerAccent,
                    )
                }
            }

            // 전체 선택/해제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onShowAll) {
                    Text(stringResource(R.string.flight_zone_select_all))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onHideAll) {
                    Text(stringResource(R.string.flight_zone_deselect_all))
                }
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            // 레이어 목록
            LazyColumn {
                items(FlightZoneLayer.entries.toList()) { layer ->
                    LayerItem(
                        layer = layer,
                        isChecked = visibleLayers.contains(layer),
                        onToggle = { onToggleLayer(layer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerItem(
    layer: FlightZoneLayer,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 레이어 색상 표시 (iOS RoundedRectangle(4) 24x24 매핑)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(layer.borderColor.toInt()))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = layer.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = layer.restrictionLevel.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
