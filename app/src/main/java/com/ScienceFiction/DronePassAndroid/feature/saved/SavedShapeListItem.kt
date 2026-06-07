package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.parseIosOpaqueRgbHexColor
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
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 한국 시장 타겟이므로 Locale.KOREA 명시. 매 호출 SimpleDateFormat 생성 부담 회피.
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }
    val shapeColor = resolveSavedShapeDisplayColor(shape.color)
    val isExpired = isSavedShapeListItemExpired(shape.flightEndDate, now = System.currentTimeMillis())
    val displayColor = if (isExpired) SavedShapeExpiredIndicatorColor else shapeColor
    val dateRangeText = formatSavedShapeDateRange(
        startDateMillis = shape.flightStartDate,
        endDateMillis = shape.flightEndDate,
        dateFormat = dateFormat,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SavedShapeRowMinHeight)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(
                start = SavedShapeRowHorizontalPadding,
                end = SavedShapeRowHorizontalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // iOS ShapeColorIndicator: width 4, height 35, cornerRadius 15, shadow radius 1.
        Box(
            modifier = Modifier
                .width(SavedShapeColorIndicatorWidth)
                .height(SavedShapeColorIndicatorHeight)
                .shadow(
                    elevation = SavedShapeColorIndicatorShadowElevation,
                    shape = RoundedCornerShape(SavedShapeColorIndicatorCornerRadius),
                    clip = false,
                )
                .clip(RoundedCornerShape(SavedShapeColorIndicatorCornerRadius))
                .background(displayColor)
        )

        Spacer(modifier = Modifier.width(SavedShapeInfoLeadingSpacing))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = shape.title.ifBlank { stringResource(R.string.common_no_title) },
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!shape.address.isNullOrBlank()) {
                Text(
                    text = shape.address,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = dateRangeText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(SavedShapeDetailLeadingSpacing))

        Box(
            modifier = Modifier
                .width(SavedShapeDetailButtonWidth)
                .clickable(onClick = onDetailClick),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.shape_detail_navigation_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(SavedShapeDetailChevronSize)
                    .height(SavedShapeDetailChevronSize)
            )
        }
    }
}

internal val SavedShapeRowMinHeight = 55.dp
internal val SavedShapeRowHorizontalPadding = 4.dp
internal val SavedShapeInfoLeadingSpacing = 12.dp
internal val SavedShapeDetailLeadingSpacing = 8.dp
internal val SavedShapeDetailButtonWidth = 30.dp
internal val SavedShapeDetailChevronSize = 12.dp
internal val SavedShapeColorIndicatorWidth = 4.dp
internal val SavedShapeColorIndicatorHeight = 35.dp
internal val SavedShapeColorIndicatorCornerRadius = 15.dp
internal val SavedShapeColorIndicatorShadowElevation = 1.dp
internal val SavedShapeExpiredIndicatorColor = Color(0xFF8E8E93)

internal fun formatSavedShapeDateRange(
    startDateMillis: Long,
    endDateMillis: Long?,
    dateFormat: SimpleDateFormat,
): String {
    val startDate = dateFormat.format(Date(startDateMillis))
    return endDateMillis?.let { end ->
        "$startDate ~ ${dateFormat.format(Date(end))}"
    } ?: startDate
}

internal fun resolveSavedShapeDisplayColor(colorHex: String): Color {
    PaletteColor.fromHex(colorHex)?.let { return it.composeColor }
    return parseIosOpaqueRgbHexColor(colorHex)?.let(::Color) ?: Color(0xFF007AFF)
}

internal fun isSavedShapeListItemExpired(
    flightEndDateMillis: Long?,
    now: Long = System.currentTimeMillis(),
): Boolean {
    return flightEndDateMillis?.let { it <= now } ?: false
}
