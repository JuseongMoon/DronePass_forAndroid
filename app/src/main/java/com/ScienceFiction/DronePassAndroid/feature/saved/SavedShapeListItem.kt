package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedShapeListItem(
    shape: ShapeModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    droneName: String? = null
) {
    // 한국 시장 타겟이므로 Locale.KOREA 명시. 매 호출 SimpleDateFormat 생성 부담 회피.
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREA) }
    val shapeColor = PaletteColor.fromHex(shape.color)?.composeColor
        ?: Color(android.graphics.Color.parseColor(shape.color))
    val displayColor = if (shape.isExpired) Color.Gray else shapeColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(start = 0.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 색상 인디케이터 바
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(displayColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 도형 정보
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = shape.title.ifBlank { stringResource(R.string.common_no_title) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!shape.address.isNullOrBlank()) {
                    Text(
                        text = shape.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 비행 기간
                val startDate = dateFormat.format(Date(shape.flightStartDate))
                val endDate = shape.flightEndDate?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.common_not_set)
                Text(
                    text = "$startDate ~ $endDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 연결된 드론 이름 (있으면)
                if (droneName != null) {
                    Text(
                        text = droneName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 상태 배지
            StatusChip(shape = shape)
        }
    }
}

@Composable
private fun StatusChip(shape: ShapeModel) {
    val (statusText, statusColor) = when {
        shape.isExpired -> stringResource(R.string.saved_item_status_expired) to MaterialTheme.colorScheme.error
        shape.isNotStarted -> stringResource(R.string.saved_item_status_waiting) to MaterialTheme.colorScheme.tertiary
        else -> stringResource(R.string.saved_item_status_active) to MaterialTheme.colorScheme.primary
    }

    SuggestionChip(
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
