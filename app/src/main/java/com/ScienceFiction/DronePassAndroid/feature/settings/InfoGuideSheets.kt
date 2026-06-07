package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherInfoTopic
import kotlinx.coroutines.delay

@Composable
internal fun KpInfoGuideSheet(onDismiss: () -> Unit) {
    InfoGuideScaffold(
        titleRes = R.string.kp_info_title,
        onDismiss = onDismiss,
    ) {
        item {
            SectionTitle(titleRes = R.string.kp_info_what_title)
        }
        item {
            KpOverviewCard()
        }
        item {
            SectionTitle(
                titleRes = R.string.kp_info_levels_title,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        items(kpLevelGuideItems()) { item ->
            KpLevelGuideCard(item = item)
        }
    }
}

@Composable
internal fun WeatherInfoGuideSheet(
    onDismiss: () -> Unit,
    initialTopic: WeatherInfoTopic? = null,
    category: DroneCategory = DroneCategory.IosDefault,
    onCategoryChanged: (DroneCategory) -> Unit = {},
    isUsingGps: Boolean = false,
    locationAccuracyMeters: Double? = null,
) {
    InfoGuideScaffold(
        titleRes = R.string.weather_info_title,
        onDismiss = onDismiss,
        initialScrollIndex = weatherInfoScrollIndex(initialTopic),
    ) {
        item {
            SectionTitle(titleRes = R.string.weather_info_importance_title)
        }
        item {
            WeatherOverviewCard(
                isUsingGps = isUsingGps,
                locationAccuracyMeters = locationAccuracyMeters,
            )
        }
        item {
            SectionTitle(
                titleRes = R.string.weather_info_elements_title,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        items(weatherElementGuideItems(category)) { item ->
            WeatherElementGuideCard(
                item = item,
                category = category,
                onCategoryChanged = onCategoryChanged,
            )
        }
    }
}

@Composable
private fun InfoGuideScaffold(
    @StringRes titleRes: Int,
    onDismiss: () -> Unit,
    initialScrollIndex: Int? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(initialScrollIndex) {
        val targetIndex = initialScrollIndex ?: return@LaunchedEffect
        delay(100)
        listState.animateScrollToItem(targetIndex)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun GuideSection(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp),
    )
}

@Composable
private fun KpOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.kp_info_what_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                GuideBullet(textRes = R.string.kp_info_what_bullet_range)
                GuideBullet(textRes = R.string.kp_info_what_bullet_measure)
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.kp_info_drone_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.kp_info_drone_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GuideBullet(textRes = R.string.kp_info_relation_bullet_gps_accuracy)
                GuideBullet(textRes = R.string.kp_info_relation_bullet_tracking_error)
                GuideBullet(textRes = R.string.kp_info_relation_bullet_rth)
                GuideBullet(
                    textRes = R.string.kp_info_relation_bullet_kp5,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.kp_info_sources_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.kp_info_sources_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GuideBullet(textRes = R.string.kp_info_source_gfz)
                GuideBullet(textRes = R.string.kp_info_source_noaa)
                Text(
                    text = stringResource(R.string.kp_info_source_difference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                GuideBullet(textRes = R.string.kp_info_source_reason_station)
                GuideBullet(textRes = R.string.kp_info_source_reason_interval)
                GuideBullet(textRes = R.string.kp_info_source_reason_forecast)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = "💡",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = stringResource(R.string.kp_info_source_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9500),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherOverviewCard(
    isUsingGps: Boolean,
    locationAccuracyMeters: Double?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.weather_info_importance_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                GuideBullet(textRes = R.string.weather_info_importance_bullet_factors)
                GuideBullet(textRes = R.string.weather_info_importance_bullet_stop)
                GuideBullet(textRes = R.string.weather_info_importance_bullet_forecast)
            }

            HorizontalDivider()

            if (!isUsingGps && locationAccuracyMeters != null) {
                WeatherLocationAccuracyCard(accuracyMeters = locationAccuracyMeters)

                HorizontalDivider()
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.weather_info_safety_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.weather_info_safety_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GuideBullet(textRes = R.string.weather_info_safety_bullet_comprehensive)
                GuideBullet(textRes = R.string.weather_info_safety_bullet_forecast_gap)
                GuideBullet(
                    textRes = R.string.weather_info_safety_bullet_postpone,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun WeatherLocationAccuracyCard(accuracyMeters: Double) {
    val warningColor = Color(0xFFFF9500)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = warningColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.weather_info_location_title),
                style = MaterialTheme.typography.bodyMedium,
                color = warningColor,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = stringResource(
                R.string.weather_info_location_wifi,
                accuracyMeters.toInt(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GuideBullet(
            textRes = R.string.weather_info_location_bullet_weather_difference,
            color = warningColor,
        )
        GuideBullet(
            textRes = R.string.weather_info_location_bullet_cellular,
            color = warningColor,
        )
    }
}

@Composable
private fun WeatherElementGuideCard(
    item: WeatherElementGuideItem,
    category: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconColor,
                    modifier = Modifier.size(32.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.weather_info_element_safe_range,
                            formattedStringResource(item.safeRangeRes, item.safeRangeArgs),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.showCategorySelector) {
                    WeatherCategoryMenu(
                        category = category,
                        onCategoryChanged = onCategoryChanged,
                    )
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.levels.forEach { level ->
                    WeatherElementDetailRow(level = level)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.weather_info_element_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                FormattedGuideNoteText(
                    note = formattedStringResource(item.noteRes, item.noteArgs),
                )
            }
        }
    }
}

@Composable
private fun WeatherCategoryMenu(
    category: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(category.labelRes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "⌄",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DroneCategory.entries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(entry.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = stringResource(entry.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onCategoryChanged(entry)
                    },
                )
            }
        }
    }
}

@Composable
private fun WeatherElementDetailRow(level: WeatherElementLevelItem) {
    val color = level.level.color
    val adviceColor = if (level.level == WeatherSafetyLevel.SAFE) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        level.level.textColor
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            Text(
                text = formattedStringResource(level.rangeRes, level.rangeArgs),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
        GuideBullet(textRes = level.descriptionRes)
        GuideBullet(
            textRes = level.adviceRes,
            color = adviceColor,
            fontWeight = if (level.level == WeatherSafetyLevel.DANGER) {
                FontWeight.Medium
            } else {
                FontWeight.Normal
            },
        )
    }
}

@Composable
private fun formattedStringResource(
    @StringRes resId: Int,
    args: List<Any>,
): String {
    val resolvedArgs = args.map { arg ->
        when (arg) {
            is StringResourceArg -> stringResource(arg.resId)
            else -> arg
        }
    }
    return if (args.isEmpty()) {
        stringResource(resId)
    } else {
        stringResource(resId, *resolvedArgs.toTypedArray())
    }
}

@Composable
private fun KpLevelGuideCard(item: KpLevelGuideItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(item.color, CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(item.nameRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = item.color,
                    )
                    Text(
                        text = stringResource(item.rangeRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GuideBullet(textRes = item.descriptionRes)
                GuideBullet(
                    textRes = item.adviceRes,
                    color = if (isKpLevelAdviceDanger(item.level)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isKpLevelAdviceDanger(item.level)) {
                        FontWeight.Medium
                    } else {
                        FontWeight.Normal
                    },
                )
            }
        }
    }
}

@Composable
private fun GuideBullet(
    @StringRes textRes: Int,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = fontWeight,
        )
    }
}

@Composable
private fun FormattedGuideNoteText(note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        parseFormattedGuideNoteLines(note).forEach { line ->
            when (line.style) {
                FormattedGuideNoteLineStyle.Blank -> Text(
                    text = "",
                    style = MaterialTheme.typography.bodySmall,
                )

                FormattedGuideNoteLineStyle.Bullet -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                FormattedGuideNoteLineStyle.Header -> Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )

                FormattedGuideNoteLineStyle.Indented -> Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

internal enum class FormattedGuideNoteLineStyle {
    Blank,
    Bullet,
    Header,
    Indented,
}

internal data class FormattedGuideNoteLine(
    val text: String,
    val style: FormattedGuideNoteLineStyle,
)

internal fun parseFormattedGuideNoteLines(note: String): List<FormattedGuideNoteLine> {
    return note.split('\n').map { rawLine ->
        val trimmed = rawLine.trim()
        when {
            trimmed.isEmpty() -> FormattedGuideNoteLine(
                text = "",
                style = FormattedGuideNoteLineStyle.Blank,
            )

            trimmed.startsWith("• ") -> FormattedGuideNoteLine(
                text = trimmed.drop(2),
                style = FormattedGuideNoteLineStyle.Bullet,
            )

            isFormattedGuideNoteHeader(trimmed) -> FormattedGuideNoteLine(
                text = trimmed,
                style = FormattedGuideNoteLineStyle.Header,
            )

            trimmed.startsWith("→") -> FormattedGuideNoteLine(
                text = trimmed,
                style = FormattedGuideNoteLineStyle.Indented,
            )

            else -> FormattedGuideNoteLine(
                text = trimmed,
                style = FormattedGuideNoteLineStyle.Bullet,
            )
        }
    }
}

private fun isFormattedGuideNoteHeader(text: String): Boolean {
    return text.startsWith("①") ||
        text.startsWith("②") ||
        text.startsWith("③") ||
        text.startsWith("④") ||
        text.startsWith("⑤") ||
        text.startsWith("⚠️")
}

@Composable
private fun LevelGuideCard(item: GuideItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .background(item.color, CircleShape),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(item.bodyRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class GuideItem(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val color: Color,
)

internal enum class WeatherSafetyLevel(
    val color: Color,
    val textColor: Color,
) {
    SAFE(
        color = Color(0xFF34C759),
        textColor = Color.Unspecified,
    ),
    CAUTION(
        color = Color(0xFFFF9500),
        textColor = Color(0xFFFF9500),
    ),
    DANGER(
        color = Color(0xFFFF3B30),
        textColor = Color(0xFFFF3B30),
    ),
}

internal data class WeatherElementLevelItem(
    @StringRes val rangeRes: Int,
    val rangeArgs: List<Any> = emptyList(),
    val level: WeatherSafetyLevel,
    @StringRes val descriptionRes: Int,
    @StringRes val adviceRes: Int,
)

internal data class WeatherElementGuideItem(
    val topic: WeatherInfoTopic,
    @StringRes val titleRes: Int,
    @StringRes val safeRangeRes: Int,
    val safeRangeArgs: List<Any> = emptyList(),
    val icon: ImageVector,
    val iconColor: Color,
    val levels: List<WeatherElementLevelItem>,
    @StringRes val noteRes: Int,
    val noteArgs: List<Any> = emptyList(),
    val showCategorySelector: Boolean = false,
)

internal data class StringResourceArg(
    @StringRes val resId: Int,
)

internal data class KpLevelGuideItem(
    val level: KpLevel,
    @StringRes val nameRes: Int,
    @StringRes val rangeRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val adviceRes: Int,
    val color: Color,
)

internal fun kpLevelGuideItems(): List<KpLevelGuideItem> = listOf(
    KpLevelGuideItem(
        level = KpLevel.NORMAL,
        nameRes = R.string.kp_info_level_normal_name,
        rangeRes = R.string.kp_info_level_normal_range,
        descriptionRes = R.string.kp_info_level_normal_desc,
        adviceRes = R.string.kp_info_level_normal_advice,
        color = Color(KpLevel.NORMAL.color.toInt()),
    ),
    KpLevelGuideItem(
        level = KpLevel.G1,
        nameRes = R.string.kp_info_level_g1_name,
        rangeRes = R.string.kp_info_level_g1_range,
        descriptionRes = R.string.kp_info_level_g1_desc,
        adviceRes = R.string.kp_info_level_g1_advice,
        color = Color(KpLevel.G1.color.toInt()),
    ),
    KpLevelGuideItem(
        level = KpLevel.G2,
        nameRes = R.string.kp_info_level_g2_name,
        rangeRes = R.string.kp_info_level_g2_range,
        descriptionRes = R.string.kp_info_level_g2_desc,
        adviceRes = R.string.kp_info_level_g2_advice,
        color = Color(KpLevel.G2.color.toInt()),
    ),
    KpLevelGuideItem(
        level = KpLevel.G3,
        nameRes = R.string.kp_info_level_g3_name,
        rangeRes = R.string.kp_info_level_g3_range,
        descriptionRes = R.string.kp_info_level_g3_desc,
        adviceRes = R.string.kp_info_level_g3_advice,
        color = Color(KpLevel.G3.color.toInt()),
    ),
    KpLevelGuideItem(
        level = KpLevel.G4,
        nameRes = R.string.kp_info_level_g4_name,
        rangeRes = R.string.kp_info_level_g4_range,
        descriptionRes = R.string.kp_info_level_g4_desc,
        adviceRes = R.string.kp_info_level_g4_advice,
        color = Color(KpLevel.G4.color.toInt()),
    ),
    KpLevelGuideItem(
        level = KpLevel.G5,
        nameRes = R.string.kp_info_level_g5_name,
        rangeRes = R.string.kp_info_level_g5_range,
        descriptionRes = R.string.kp_info_level_g5_desc,
        adviceRes = R.string.kp_info_level_g5_advice,
        color = Color(KpLevel.G5.color.toInt()),
    ),
)

internal fun isKpLevelAdviceDanger(level: KpLevel): Boolean {
    return level == KpLevel.G3 || level == KpLevel.G4 || level == KpLevel.G5
}

internal fun weatherElementGuideItems(category: DroneCategory): List<WeatherElementGuideItem> {
    val (windCaution, windDanger) = weatherGuideWindSpeedThresholds(category)
    val (gustCaution, gustDanger) = weatherGuideGustDifferenceThresholds(category)
    val maxWindResistance = weatherGuideMaxWindResistance(category)
    val visibilityGood = 10
    val visibilityPoor = 2
    val temperatureLowCaution = -10
    val temperatureHighCaution = 35

    return listOf(
        WeatherElementGuideItem(
            topic = WeatherInfoTopic.WindSpeed,
            titleRes = R.string.weather_info_wind_title,
            safeRangeRes = R.string.weather_info_wind_safe_range,
            safeRangeArgs = listOf(windCaution),
            icon = Icons.Default.Air,
            iconColor = Color(0xFF007AFF),
            levels = listOf(
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_wind_range_safe,
                    rangeArgs = listOf(windCaution),
                    level = WeatherSafetyLevel.SAFE,
                    descriptionRes = R.string.weather_info_wind_safe_desc,
                    adviceRes = R.string.weather_info_wind_safe_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_wind_range_caution,
                    rangeArgs = listOf(windCaution + 0.1, windDanger - 0.1),
                    level = WeatherSafetyLevel.CAUTION,
                    descriptionRes = R.string.weather_info_wind_caution_desc,
                    adviceRes = R.string.weather_info_wind_caution_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_wind_range_danger,
                    rangeArgs = listOf(windDanger),
                    level = WeatherSafetyLevel.DANGER,
                    descriptionRes = R.string.weather_info_wind_danger_desc,
                    adviceRes = R.string.weather_info_wind_danger_advice,
                ),
            ),
            noteRes = R.string.weather_info_wind_note,
            noteArgs = listOf(StringResourceArg(category.labelRes), maxWindResistance, windDanger),
            showCategorySelector = true,
        ),
        WeatherElementGuideItem(
            topic = WeatherInfoTopic.GustDifference,
            titleRes = R.string.weather_info_gust_title,
            safeRangeRes = R.string.weather_info_gust_safe_range,
            safeRangeArgs = listOf(gustCaution),
            icon = Icons.Default.Air,
            iconColor = Color(0xFFFF9500),
            levels = listOf(
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_gust_range_safe,
                    rangeArgs = listOf(gustCaution),
                    level = WeatherSafetyLevel.SAFE,
                    descriptionRes = R.string.weather_info_gust_safe_desc,
                    adviceRes = R.string.weather_info_gust_safe_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_gust_range_caution,
                    rangeArgs = listOf(gustCaution, gustDanger - 0.1),
                    level = WeatherSafetyLevel.CAUTION,
                    descriptionRes = R.string.weather_info_gust_caution_desc,
                    adviceRes = R.string.weather_info_gust_caution_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_gust_range_danger,
                    rangeArgs = listOf(gustDanger),
                    level = WeatherSafetyLevel.DANGER,
                    descriptionRes = R.string.weather_info_gust_danger_desc,
                    adviceRes = R.string.weather_info_gust_danger_advice,
                ),
            ),
            noteRes = R.string.weather_info_gust_note,
            noteArgs = listOf(StringResourceArg(category.labelRes), gustCaution, gustDanger),
            showCategorySelector = true,
        ),
        WeatherElementGuideItem(
            topic = WeatherInfoTopic.Precipitation,
            titleRes = R.string.weather_info_precipitation_title,
            safeRangeRes = R.string.weather_info_precipitation_safe_range,
            icon = Icons.Default.WaterDrop,
            iconColor = Color(0xFF32ADE6),
            levels = listOf(
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_precipitation_range_safe,
                    level = WeatherSafetyLevel.SAFE,
                    descriptionRes = R.string.weather_info_precipitation_safe_desc,
                    adviceRes = R.string.weather_info_precipitation_safe_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_precipitation_range_caution,
                    level = WeatherSafetyLevel.CAUTION,
                    descriptionRes = R.string.weather_info_precipitation_caution_desc,
                    adviceRes = R.string.weather_info_precipitation_caution_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_precipitation_range_danger,
                    level = WeatherSafetyLevel.DANGER,
                    descriptionRes = R.string.weather_info_precipitation_danger_desc,
                    adviceRes = R.string.weather_info_precipitation_danger_advice,
                ),
            ),
            noteRes = R.string.weather_info_precipitation_note,
        ),
        WeatherElementGuideItem(
            topic = WeatherInfoTopic.Visibility,
            titleRes = R.string.weather_info_visibility_title,
            safeRangeRes = R.string.weather_info_visibility_safe_range,
            safeRangeArgs = listOf(visibilityGood),
            icon = Icons.Default.Visibility,
            iconColor = Color(0xFF5856D6),
            levels = listOf(
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_visibility_range_safe,
                    rangeArgs = listOf(visibilityGood),
                    level = WeatherSafetyLevel.SAFE,
                    descriptionRes = R.string.weather_info_visibility_safe_desc,
                    adviceRes = R.string.weather_info_visibility_safe_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_visibility_range_caution,
                    rangeArgs = listOf(visibilityPoor, visibilityGood),
                    level = WeatherSafetyLevel.CAUTION,
                    descriptionRes = R.string.weather_info_visibility_caution_desc,
                    adviceRes = R.string.weather_info_visibility_caution_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_visibility_range_danger,
                    rangeArgs = listOf(visibilityPoor),
                    level = WeatherSafetyLevel.DANGER,
                    descriptionRes = R.string.weather_info_visibility_danger_desc,
                    adviceRes = R.string.weather_info_visibility_danger_advice,
                ),
            ),
            noteRes = R.string.weather_info_visibility_note,
        ),
        WeatherElementGuideItem(
            topic = WeatherInfoTopic.Temperature,
            titleRes = R.string.weather_info_temperature_title,
            safeRangeRes = R.string.weather_info_temperature_safe_range,
            safeRangeArgs = listOf(temperatureHighCaution),
            icon = Icons.Default.Thermostat,
            iconColor = Color(0xFFFF9500),
            levels = listOf(
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_temperature_range_safe,
                    rangeArgs = listOf(temperatureHighCaution),
                    level = WeatherSafetyLevel.SAFE,
                    descriptionRes = R.string.weather_info_temperature_safe_desc,
                    adviceRes = R.string.weather_info_temperature_safe_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_temperature_range_caution,
                    rangeArgs = listOf(temperatureLowCaution, temperatureHighCaution),
                    level = WeatherSafetyLevel.CAUTION,
                    descriptionRes = R.string.weather_info_temperature_caution_desc,
                    adviceRes = R.string.weather_info_temperature_caution_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_temperature_range_danger,
                    rangeArgs = listOf(temperatureLowCaution),
                    level = WeatherSafetyLevel.DANGER,
                    descriptionRes = R.string.weather_info_temperature_danger_desc,
                    adviceRes = R.string.weather_info_temperature_danger_advice,
                ),
            ),
            noteRes = R.string.weather_info_temperature_note,
        ),
        WeatherElementGuideItem(
            topic = WeatherInfoTopic.Cri,
            titleRes = R.string.weather_info_cri_title,
            safeRangeRes = R.string.weather_info_cri_safe_range,
            icon = Icons.Default.Opacity,
            iconColor = Color(0xFF5E5CE6),
            levels = listOf(
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_cri_range_safe,
                    level = WeatherSafetyLevel.SAFE,
                    descriptionRes = R.string.weather_info_cri_safe_desc,
                    adviceRes = R.string.weather_info_cri_safe_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_cri_range_caution,
                    level = WeatherSafetyLevel.CAUTION,
                    descriptionRes = R.string.weather_info_cri_caution_desc,
                    adviceRes = R.string.weather_info_cri_caution_advice,
                ),
                WeatherElementLevelItem(
                    rangeRes = R.string.weather_info_cri_range_danger,
                    level = WeatherSafetyLevel.DANGER,
                    descriptionRes = R.string.weather_info_cri_danger_desc,
                    adviceRes = R.string.weather_info_cri_danger_advice,
                ),
            ),
            noteRes = R.string.weather_info_cri_note,
        ),
    )
}

internal fun weatherGuideWindSpeedThresholds(category: DroneCategory): Pair<Double, Double> = when (category) {
    DroneCategory.TOY -> 7.0 to 9.0
    DroneCategory.CLASS4 -> 8.5 to 10.5
    DroneCategory.CLASS3 -> 10.0 to 12.0
    DroneCategory.CLASS2 -> 12.0 to 15.0
}

internal fun weatherGuideGustDifferenceThresholds(category: DroneCategory): Pair<Double, Double> = when (category) {
    DroneCategory.TOY -> 5.0 to 7.0
    DroneCategory.CLASS4 -> 6.0 to 8.5
    DroneCategory.CLASS3 -> 7.0 to 9.5
    DroneCategory.CLASS2 -> 9.0 to 12.0
}

internal fun weatherGuideMaxWindResistance(category: DroneCategory): Double = when (category) {
    DroneCategory.TOY -> 10.7
    DroneCategory.CLASS4 -> 12.0
    DroneCategory.CLASS3 -> 12.0
    DroneCategory.CLASS2 -> 15.0
}

internal fun weatherInfoScrollIndex(topic: WeatherInfoTopic?): Int? {
    if (topic == null) return null
    val topicOrder = listOf(
        WeatherInfoTopic.WindSpeed,
        WeatherInfoTopic.GustDifference,
        WeatherInfoTopic.Precipitation,
        WeatherInfoTopic.Visibility,
        WeatherInfoTopic.Temperature,
        WeatherInfoTopic.Cri,
    )
    val topicIndex = topicOrder.indexOf(topic).takeIf { it >= 0 } ?: return null
    // WeatherInfoGuideSheet: importance, safety, "elements" header, then element cards.
    return 3 + topicIndex
}
