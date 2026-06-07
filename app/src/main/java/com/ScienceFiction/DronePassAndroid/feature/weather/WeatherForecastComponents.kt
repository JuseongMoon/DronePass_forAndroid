package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Storm
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceCalculator
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceLevel
import com.ScienceFiction.DronePassAndroid.core.util.WeatherCodeMapper
import com.ScienceFiction.DronePassAndroid.core.util.interpolateTemperatureColor
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherData
import java.util.Locale

// iOS 색상 (1:1 매핑)
private val WeatherIconBlue = Color(0xFF007AFF)
private val WindArrowTeal = Color(0xFF30B0C0)
private val WindSpeedGreen = Color(0xFF34C759)
private val GustOrange = Color(0xFFFF9500)
private val PrecipitationBlue = Color(0xFF007AFF)
private val VisibilityPurple = Color(0xFFAF52DE)
private val CriCyan = Color(0xFF32ADE6)
private val CautionYellow = Color(0xFFFFCC00)
private val WarningRed = Color(0xFFFF3B30)
private val InfoYellow = Color(0xFFFFCC00)

internal const val IosTemperatureLowCautionC = -10.0
internal const val IosTemperatureHighCautionC = 35.0
internal const val IosPrecipitationDetectionMm = 0.0
internal const val IosVisibilityGoodKm = 10.0
internal const val IosVisibilityPoorKm = 2.0
internal const val IosCriModerate = 40.0
internal const val IosCriHigh = 70.0

// iOS WeatherManager.DroneCategory.windSpeedThresholds 정합 (caution, danger)
internal fun iosWindSpeedThresholds(category: DroneCategory): Pair<Double, Double> = when (category) {
    DroneCategory.TOY -> 7.0 to 9.0
    DroneCategory.CLASS4 -> 8.5 to 10.5
    DroneCategory.CLASS3 -> 10.0 to 12.0
    DroneCategory.CLASS2 -> 12.0 to 15.0
}

// iOS WeatherManager.DroneCategory.gustDifferenceThresholds 정합 (caution, danger)
internal fun iosGustDifferenceThresholds(category: DroneCategory): Pair<Double, Double> = when (category) {
    DroneCategory.TOY -> 5.0 to 7.0
    DroneCategory.CLASS4 -> 6.0 to 8.5
    DroneCategory.CLASS3 -> 7.0 to 9.5
    DroneCategory.CLASS2 -> 9.0 to 12.0
}

enum class WarningIconType { None, Info, Caution, Warning }

internal fun resolveTemperatureWarningIcon(temperatureC: Double): WarningIconType = when {
    temperatureC < IosTemperatureLowCautionC -> WarningIconType.Caution
    temperatureC > IosTemperatureHighCautionC -> WarningIconType.Caution
    else -> WarningIconType.None
}

internal fun resolvePrecipitationWarningIcon(precipitationMm: Double): WarningIconType =
    if (precipitationMm > IosPrecipitationDetectionMm) WarningIconType.Caution else WarningIconType.None

internal fun resolveVisibilityWarningIcon(visibilityKm: Double): WarningIconType = when {
    visibilityKm < IosVisibilityPoorKm -> WarningIconType.Warning
    visibilityKm < IosVisibilityGoodKm -> WarningIconType.Caution
    else -> WarningIconType.None
}

internal fun resolveCriWarningIcon(cri: Double): WarningIconType = when {
    cri >= IosCriHigh -> WarningIconType.Warning
    cri >= IosCriModerate -> WarningIconType.Caution
    else -> WarningIconType.None
}

internal fun resolveGustDifferenceSubTextRes(level: GustDifferenceLevel): Int? =
    if (level == GustDifferenceLevel.LOCALIZED_GUST) R.string.weather_gust_warning else null

/**
 * iOS `WeatherForecastView.currentWeatherCard` 1:1 정합.
 * 헤더(현재 날씨 + 드론 카테고리) + 큰 미리보기 + 7개 데이터 그리드 + 면책 문구.
 */
@Composable
internal fun CurrentWeatherSection(
    data: WeatherData,
    category: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onWeatherInfoRequested: (WeatherInfoTopic) -> Unit = {},
) {
    val current = data.current ?: return

    val temperatureRange = resolveForecastTemperatureRange(data.hourlyForecast)
    val maxTemp = temperatureRange?.first ?: current.temperature
    val minTemp = temperatureRange?.second ?: current.temperature
    val visibility = data.hourlyForecast.firstOrNull()?.visibility ?: 0.0
    val gustDiff = GustDifferenceCalculator.calculateGustDifference(current.windSpeed, current.windGusts)

    val (windCaution, windDanger) = iosWindSpeedThresholds(category)
    val windWarning = when {
        current.windSpeed >= windDanger -> WarningIconType.Warning
        current.windSpeed >= windCaution -> WarningIconType.Caution
        else -> WarningIconType.None
    }
    val tempWarning = resolveTemperatureWarningIcon(current.temperature)
    val gustWarning = when (current.gustDifferenceLevel) {
        GustDifferenceLevel.SAFE -> WarningIconType.None
        GustDifferenceLevel.LOCALIZED_GUST -> WarningIconType.Info
        GustDifferenceLevel.CAUTION -> WarningIconType.Caution
        GustDifferenceLevel.DANGER -> WarningIconType.Warning
    }
    val gustSubText = resolveGustDifferenceSubTextRes(current.gustDifferenceLevel)
    val precipWarning = resolvePrecipitationWarningIcon(current.precipitation)
    val visibilityWarning = resolveVisibilityWarningIcon(visibility)
    val criWarning = resolveCriWarningIcon(current.cri)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.weather_section_current),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.weather_drone_weight_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                DroneCategoryMenu(category = category, onCategoryChanged = onCategoryChanged)
            }

            Box {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PreviewBlock(
                        weatherCode = current.weatherCode,
                        temperature = current.temperature,
                        maxTemperature = maxTemp,
                        minTemperature = minTemp,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WeatherDataCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Thermostat,
                                iconColor = interpolateTemperatureColor(current.temperature),
                                label = stringResource(R.string.weather_temperature),
                                value = formatIosTemperatureDegrees(current.temperature),
                                warningIcon = tempWarning,
                                onClick = { onWeatherInfoRequested(WeatherInfoTopic.Temperature) },
                            )
                            WeatherDataCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Air,
                                iconColor = WindSpeedGreen,
                                label = stringResource(R.string.weather_wind_speed),
                                value = "%.1f m/s".format(Locale.ROOT, current.windSpeed),
                                warningIcon = windWarning,
                                onClick = { onWeatherInfoRequested(WeatherInfoTopic.WindSpeed) },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WeatherDataCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Navigation,
                                iconColor = WindArrowTeal,
                                label = stringResource(R.string.weather_wind_direction),
                                value = stringResource(resolveWindDirectionLabelRes(current.windDirection)),
                                rotation = current.windDirection.toFloat(),
                            )
                            WeatherDataCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Storm,
                                iconColor = GustOrange,
                                label = stringResource(R.string.weather_gust_difference),
                                value = "%.1f m/s".format(Locale.ROOT, gustDiff),
                                warningIcon = gustWarning,
                                subText = gustSubText?.let { stringResource(it) },
                                onClick = { onWeatherInfoRequested(WeatherInfoTopic.GustDifference) },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WeatherDataCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.WaterDrop,
                                iconColor = PrecipitationBlue,
                                label = stringResource(R.string.weather_precipitation),
                                value = "%.1f mm".format(Locale.ROOT, current.precipitation),
                                warningIcon = precipWarning,
                                onClick = { onWeatherInfoRequested(WeatherInfoTopic.Precipitation) },
                            )
                            WeatherDataCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Visibility,
                                iconColor = VisibilityPurple,
                                label = stringResource(R.string.weather_visibility),
                                value = "%.0f km".format(Locale.ROOT, visibility),
                                warningIcon = visibilityWarning,
                                onClick = { onWeatherInfoRequested(WeatherInfoTopic.Visibility) },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WeatherDataCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Opacity,
                                iconColor = CriCyan,
                                label = stringResource(R.string.weather_cri),
                                value = "${current.cri.toInt()}",
                                warningIcon = criWarning,
                                onClick = { onWeatherInfoRequested(WeatherInfoTopic.Cri) },
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.weather_disclaimer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

internal fun resolveForecastTemperatureRange(
    hourlyForecast: List<HourlyWeatherData>,
): Pair<Double, Double>? {
    if (hourlyForecast.isEmpty()) return null
    return hourlyForecast.maxOf { it.temperature } to hourlyForecast.minOf { it.temperature }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DroneCategoryMenu(
    category: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = WeatherIconBlue.copy(alpha = 0.1f),
            onClick = { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = WeatherIconBlue,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(category.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = WeatherIconBlue,
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = WeatherIconBlue,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DroneCategory.entries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(entry.labelRes),
                                    fontWeight = FontWeight.Medium,
                                )
                                if (entry == category) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = WeatherIconBlue,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Text(
                                text = stringResource(entry.examplesRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onCategoryChanged(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewBlock(
    weatherCode: Int,
    temperature: Double,
    maxTemperature: Double,
    minTemperature: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = WeatherCodeMapper.weatherCodeToIcon(weatherCode),
                contentDescription = WeatherCodeMapper.weatherCodeToDescription(weatherCode),
                tint = WeatherIconBlue,
                modifier = Modifier.size(64.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatIosTemperatureDegrees(temperature),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = interpolateTemperatureColor(temperature),
                    )
                    Text(
                        text = WeatherCodeMapper.weatherCodeToDescription(weatherCode),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.weather_temperature_high_prefix) +
                            formatIosTemperatureDegrees(maxTemperature),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "~",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.weather_temperature_low_prefix) +
                            formatIosTemperatureDegrees(minTemperature),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherDataCell(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    rotation: Float? = null,
    warningIcon: WarningIconType = WarningIconType.None,
    subText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val hasSubText = !subText.isNullOrBlank()
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation?.plus(180f) ?: 0f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (hasSubText) 1.dp else 2.dp),
            ) {
                Text(
                    text = label,
                    style = if (hasSubText) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = value,
                    style = if (hasSubText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (hasSubText) {
                    Text(
                        text = subText.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (warningIcon != WarningIconType.None) {
                Icon(
                    imageVector = when (warningIcon) {
                        WarningIconType.Info -> Icons.Default.Info
                        WarningIconType.Caution -> Icons.Default.Warning
                        WarningIconType.Warning -> Icons.Default.Error
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (warningIcon) {
                        WarningIconType.Info -> InfoYellow
                        WarningIconType.Caution -> CautionYellow
                        WarningIconType.Warning -> WarningRed
                        else -> Color.Transparent
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

internal fun formatIosTemperatureDegrees(temperatureC: Double): String =
    "%.0f°".format(Locale.ROOT, temperatureC)

@StringRes
internal fun resolveWindDirectionLabelRes(degrees: Double): Int {
    val directions = listOf(
        R.string.weather_direction_n,
        R.string.weather_direction_ne,
        R.string.weather_direction_e,
        R.string.weather_direction_se,
        R.string.weather_direction_s,
        R.string.weather_direction_sw,
        R.string.weather_direction_w,
        R.string.weather_direction_nw,
    )
    val normalized = ((degrees % 360.0) + 360.0) % 360.0
    val index = ((normalized + 22.5) / 45.0).toInt() % directions.size
    return directions[index]
}
