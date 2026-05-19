package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.DroneCategory
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceCalculator
import com.ScienceFiction.DronePassAndroid.core.util.GustDifferenceLevel
import com.ScienceFiction.DronePassAndroid.core.util.WeatherCodeMapper
import com.ScienceFiction.DronePassAndroid.domain.model.CurrentWeatherData
import com.ScienceFiction.DronePassAndroid.domain.model.HourlyWeatherData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * 비행 적합도 요약 카드
 */
@Composable
internal fun FlightSuitabilityCard(
    current: CurrentWeatherData,
    category: DroneCategory
) {
    val gustLevel = GustDifferenceCalculator.evaluate(
        current.windSpeed, current.windGusts, category
    )

    data class SuitabilityResult(
        val label: String,
        val color: Color,
        val icon: ImageVector
    )

    val windIssue = gustLevel == GustDifferenceLevel.DANGER
    val gustIssue = gustLevel == GustDifferenceLevel.CAUTION || gustLevel == GustDifferenceLevel.DANGER
    val precipitationIssue = current.precipitation > 0.5

    val suitability = when {
        windIssue || (precipitationIssue && gustIssue) -> SuitabilityResult(
            label = stringResource(R.string.weather_suitability_danger),
            color = Color(0xFFF44336),
            icon = Icons.Default.Warning
        )
        gustIssue || precipitationIssue -> SuitabilityResult(
            label = stringResource(R.string.weather_suitability_caution),
            color = Color(0xFFFF9800),
            icon = Icons.Default.Warning
        )
        else -> SuitabilityResult(
            label = stringResource(R.string.weather_suitability_safe),
            color = Color(0xFF4CAF50),
            icon = Icons.Default.CheckCircle
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = suitability.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = suitability.icon,
                    contentDescription = suitability.label,
                    tint = suitability.color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.weather_flight_suitability),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = suitability.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = suitability.color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SuitabilityCheckItem(
                label = stringResource(R.string.weather_wind_speed),
                value = "${String.format(Locale.ROOT, "%.1f", current.windSpeed)} m/s",
                isGood = gustLevel == GustDifferenceLevel.SAFE || gustLevel == GustDifferenceLevel.LOCALIZED_GUST
            )
            Spacer(modifier = Modifier.height(4.dp))
            SuitabilityCheckItem(
                label = stringResource(R.string.weather_gust),
                value = current.windGusts?.let { "${String.format(Locale.ROOT, "%.1f", it)} m/s" } ?: "-",
                isGood = gustLevel == GustDifferenceLevel.SAFE
            )
            Spacer(modifier = Modifier.height(4.dp))
            SuitabilityCheckItem(
                label = stringResource(R.string.weather_precipitation),
                value = "${String.format(Locale.ROOT, "%.1f", current.precipitation)} mm",
                isGood = current.precipitation <= 0.5
            )
        }
    }
}

@Composable
internal fun SuitabilityCheckItem(
    label: String,
    value: String,
    isGood: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isGood) Color(0xFF4CAF50) else Color(0xFFF44336))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 풍향 나침반 카드
 */
@Composable
internal fun WindCompassCard(
    windDirection: Double,
    windSpeed: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.weather_wind_compass),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val compassColor = MaterialTheme.colorScheme.onSurfaceVariant
                val arrowColor = MaterialTheme.colorScheme.primary
                val circleColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(
                    modifier = Modifier.size(120.dp)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 8f

                    drawCircle(
                        color = circleColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    drawCircle(
                        color = circleColor.copy(alpha = 0.3f),
                        radius = radius * 0.6f,
                        center = center,
                        style = Stroke(width = 1f)
                    )

                    for (i in 0 until 8) {
                        val angle = Math.toRadians((i * 45.0) - 90.0)
                        val innerR = radius * 0.85f
                        val outerR = radius
                        val startX = center.x + innerR * cos(angle).toFloat()
                        val startY = center.y + innerR * sin(angle).toFloat()
                        val endX = center.x + outerR * cos(angle).toFloat()
                        val endY = center.y + outerR * sin(angle).toFloat()
                        drawLine(
                            color = compassColor.copy(alpha = 0.5f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (i % 2 == 0) 2f else 1f
                        )
                    }

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val directions = listOf("N", "E", "S", "W")
                    val angles = listOf(-90.0, 0.0, 90.0, 180.0)
                    for (i in directions.indices) {
                        val angle = Math.toRadians(angles[i])
                        val textR = radius + 2f
                        if (i == 0) {
                            textPaint.color = android.graphics.Color.RED
                        } else {
                            textPaint.color = android.graphics.Color.GRAY
                        }
                        val textX = center.x + textR * cos(angle).toFloat()
                        val textY = center.y + textR * sin(angle).toFloat() + 8f
                        drawContext.canvas.nativeCanvas.drawText(
                            directions[i],
                            textX,
                            textY,
                            textPaint
                        )
                    }

                    val windAngle = Math.toRadians(windDirection - 90.0)
                    val arrowLength = radius * 0.65f
                    val arrowEndX = center.x + arrowLength * cos(windAngle).toFloat()
                    val arrowEndY = center.y + arrowLength * sin(windAngle).toFloat()

                    drawLine(
                        color = arrowColor,
                        start = center,
                        end = Offset(arrowEndX, arrowEndY),
                        strokeWidth = 3f
                    )

                    val headLength = 12f
                    val headAngle1 = windAngle + Math.toRadians(150.0)
                    val headAngle2 = windAngle - Math.toRadians(150.0)
                    val head1X = arrowEndX + headLength * cos(headAngle1).toFloat()
                    val head1Y = arrowEndY + headLength * sin(headAngle1).toFloat()
                    val head2X = arrowEndX + headLength * cos(headAngle2).toFloat()
                    val head2Y = arrowEndY + headLength * sin(headAngle2).toFloat()

                    val arrowPath = Path().apply {
                        moveTo(arrowEndX, arrowEndY)
                        lineTo(head1X, head1Y)
                        lineTo(head2X, head2Y)
                        close()
                    }
                    drawPath(arrowPath, arrowColor)

                    drawCircle(
                        color = arrowColor,
                        radius = 4f,
                        center = center
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = windDirectionToText(windDirection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${windDirection.toInt()}\u00B0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.ROOT, "%.1f", windSpeed)} m/s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 현재 날씨 카드
 */
@Composable
internal fun CurrentWeatherCard(
    current: CurrentWeatherData,
    sunrise: String?,
    sunset: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = WeatherCodeMapper.weatherCodeToIcon(current.weatherCode),
                contentDescription = WeatherCodeMapper.weatherCodeToDescription(current.weatherCode),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${String.format(Locale.ROOT, "%.1f", current.temperature)}\u00B0C",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = WeatherCodeMapper.weatherCodeToDescription(current.weatherCode),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherInfoChip(
                    icon = Icons.Default.Air,
                    label = stringResource(R.string.weather_wind_speed),
                    value = "${String.format(Locale.ROOT, "%.1f", current.windSpeed)} m/s"
                )
                WeatherInfoChip(
                    icon = Icons.Default.Navigation,
                    label = stringResource(R.string.weather_wind_direction),
                    value = windDirectionToText(current.windDirection),
                    iconRotation = current.windDirection.toFloat()
                )
                current.windGusts?.let { gusts ->
                    WeatherInfoChip(
                        icon = Icons.Default.Air,
                        label = stringResource(R.string.weather_gust),
                        value = "${String.format(Locale.ROOT, "%.1f", gusts)} m/s"
                    )
                }
            }

            if (sunrise != null || sunset != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    sunrise?.let {
                        val time = formatSunTime(it)
                        Text(
                            text = "${stringResource(R.string.weather_sunrise)} $time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    sunset?.let {
                        val time = formatSunTime(it)
                        Text(
                            text = "${stringResource(R.string.weather_sunset)} $time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WeatherInfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    iconRotation: Float = 0f
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(iconRotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 드론 카테고리 선택기
 */
@Composable
internal fun DroneCategorySelector(
    selectedCategory: DroneCategory,
    onCategoryChanged: (DroneCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.weather_drone_category),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(text = selectedCategory.label)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DroneCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(category.label)
                                    Text(
                                        text = "${category.minWeight} ~ ${category.maxWeight}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onCategoryChanged(category)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 날씨 데이터 그리드
 */
@Composable
internal fun WeatherDataGrid(
    current: CurrentWeatherData,
    category: DroneCategory
) {
    val gustDiff = GustDifferenceCalculator.calculateGustDifference(
        current.windSpeed, current.windGusts
    )
    val gustLevel = GustDifferenceCalculator.evaluate(
        current.windSpeed, current.windGusts, category
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GustLevelBanner(level = gustLevel)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeatherGridItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Thermostat,
                label = stringResource(R.string.weather_temperature),
                value = "${String.format(Locale.ROOT, "%.1f", current.temperature)}\u00B0C"
            )
            WeatherGridItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Air,
                label = stringResource(R.string.weather_wind_speed),
                value = "${String.format(Locale.ROOT, "%.1f", current.windSpeed)} m/s",
                level = gustLevel
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeatherGridItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Explore,
                label = stringResource(R.string.weather_wind_direction),
                value = "${windDirectionToText(current.windDirection)} (${current.windDirection.toInt()}\u00B0)"
            )
            WeatherGridItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                label = stringResource(R.string.weather_gust_difference),
                value = "${String.format(Locale.ROOT, "%.1f", gustDiff)} m/s",
                level = gustLevel
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeatherGridItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WaterDrop,
                label = stringResource(R.string.weather_precipitation),
                value = "${String.format(Locale.ROOT, "%.1f", current.precipitation)} mm",
                level = if (current.precipitation > 0) GustDifferenceLevel.CAUTION else GustDifferenceLevel.SAFE
            )
            WeatherGridItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Opacity,
                label = stringResource(R.string.weather_cri),
                value = "${current.cri.toInt()}",
                level = when {
                    current.cri >= 80 -> GustDifferenceLevel.DANGER
                    current.cri >= 60 -> GustDifferenceLevel.CAUTION
                    else -> GustDifferenceLevel.SAFE
                }
            )
        }
    }
}

@Composable
internal fun GustLevelBanner(level: GustDifferenceLevel) {
    val backgroundColor = Color(level.colorLong).copy(alpha = 0.15f)
    val textColor = Color(level.colorLong)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (level) {
                    GustDifferenceLevel.SAFE -> Icons.Default.Air
                    else -> Icons.Default.Warning
                },
                contentDescription = level.label,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.weather_flight_safety_level, level.label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = when (level) {
                        GustDifferenceLevel.SAFE -> stringResource(R.string.weather_gust_safe)
                        GustDifferenceLevel.LOCALIZED_GUST -> stringResource(R.string.weather_gust_localized)
                        GustDifferenceLevel.CAUTION -> stringResource(R.string.weather_gust_caution)
                        GustDifferenceLevel.DANGER -> stringResource(R.string.weather_gust_danger)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
internal fun WeatherGridItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    level: GustDifferenceLevel = GustDifferenceLevel.SAFE
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            if (level != GustDifferenceLevel.SAFE) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(level.colorLong),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

/**
 * 시간별 날씨 아이템
 */
@Composable
internal fun HourlyWeatherItem(
    hourly: HourlyWeatherData,
    category: DroneCategory
) {
    val gustLevel = GustDifferenceCalculator.evaluate(
        hourly.windSpeed, hourly.windGusts, category
    )
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
    val timeStr = timeFormat.format(Date(hourly.time))
    val dateStr = dateFormat.format(Date(hourly.time))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(hourly.windDirection.toFloat()),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                Text(
                    text = stringResource(R.string.weather_temperature),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "${String.format(Locale.ROOT, "%.0f", hourly.temperature)}\u00B0",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.weather_wind_speed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "${String.format(Locale.ROOT, "%.1f", hourly.windSpeed)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.weather_gust),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = hourly.windGusts?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(gustLevel.colorLong)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp)
            ) {
                Text(
                    text = stringResource(R.string.weather_hourly_precipitation),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = if (hourly.precipitation > 0) "${String.format(Locale.ROOT, "%.1f", hourly.precipitation)}" else "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hourly.precipitation > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 24.dp)
                    .background(
                        Color(gustLevel.colorLong),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/**
 * 풍향(도) -> 방위 텍스트 변환
 */
internal fun windDirectionToText(degrees: Double): String {
    val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((degrees + 11.25) / 22.5).toInt() % 16
    return directions[index]
}

/**
 * ISO 날짜시간 문자열에서 시간만 추출
 */
internal fun formatSunTime(isoTime: String): String {
    return try {
        val parts = isoTime.split("T")
        if (parts.size == 2) parts[1] else isoTime
    } catch (e: Exception) {
        isoTime
    }
}
