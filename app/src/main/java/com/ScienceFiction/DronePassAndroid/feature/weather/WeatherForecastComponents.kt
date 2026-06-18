package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.annotation.StringRes
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

internal val IosWeatherForecastCardCornerRadius = 16.dp
internal val IosWeatherForecastCardPadding = 16.dp
internal val IosWeatherForecastCardSpacing = 12.dp
internal val IosWeatherForecastCardContainerColor = Color(0xFFF2F2F7)
internal val IosCurrentWeatherHeaderTitleFontSize = 20.sp
internal val IosCurrentWeatherHeaderDroneWeightFontSize = 15.sp
internal val IosCurrentWeatherHeaderTitleFontWeight = FontWeight.SemiBold
internal val IosCurrentWeatherHeaderRegularFontWeight = FontWeight.Normal
internal val IosWeatherDroneCategoryButtonCornerRadius = 8.dp
internal val IosWeatherDroneCategoryButtonHorizontalPadding = 10.dp
internal val IosWeatherDroneCategoryButtonVerticalPadding = 6.dp
internal val IosWeatherDroneCategoryButtonSpacing = 4.dp
internal const val IosWeatherDroneCategoryButtonBackgroundAlpha = 0.1f
internal val IosWeatherDroneCategoryLeadingIconSize = 12.dp
internal val IosWeatherDroneCategoryChevronIconSize = 11.dp
internal val IosWeatherDroneCategoryLabelFontSize = 15.sp
internal val IosWeatherDroneCategoryLabelFontWeight = FontWeight.Medium
internal val IosWeatherDroneCategoryCheckmarkSize = 15.dp
internal val IosWeatherDroneCategoryExampleFontSize = 11.sp
internal val IosCurrentWeatherEmptyStateHeight = 200.dp
internal val IosCurrentWeatherPreviewCornerRadius = 12.dp
internal val IosCurrentWeatherPreviewPadding = 16.dp
internal val IosCurrentWeatherPreviewIconSize = 64.dp
internal val IosCurrentWeatherPreviewIconSlotWidth = 94.dp
internal val IosCurrentWeatherPreviewContainerColor = Color(0xFFFFFFFF)
internal val IosCurrentWeatherPreviewTemperatureFontSize = 32.sp
internal val IosCurrentWeatherPreviewConditionFontSize = 20.sp
internal val IosCurrentWeatherPreviewTemperatureRangeFontSize = 15.sp
internal val IosCurrentWeatherPreviewTemperatureFontWeight = FontWeight.SemiBold
internal val IosCurrentWeatherPreviewRegularFontWeight = FontWeight.Normal
internal val IosCurrentWeatherPreviewTemperatureRangeSpacing = 8.dp
internal val IosWeatherDataCellCornerRadius = 12.dp
internal val IosWeatherDataCellHorizontalPadding = 12.dp
internal val IosWeatherDataCellVerticalPadding = 12.dp
internal val IosWeatherDataCellMinHeight = 62.dp
internal val IosWeatherDataCellIconSize = 24.dp
internal val IosWeatherDataCellIconSlotWidth = 32.dp
internal val IosWeatherDataCellWarningIconSize = 16.dp
internal val IosWeatherDataCellContainerColor = Color(0xFFFFFFFF)
internal val IosWeatherDataCellLabelFontSize = 17.sp
internal val IosWeatherDataCellLabelWithSubTextFontSize = 15.sp
internal val IosWeatherDataCellValueFontSize = 20.sp
internal val IosWeatherDataCellValueWithSubTextFontSize = 17.sp
internal val IosWeatherDataCellSubTextFontSize = 11.sp
internal val IosWeatherDataCellHeadlineFontWeight = FontWeight.SemiBold
internal val IosWeatherDataCellRegularFontWeight = FontWeight.Normal
internal val IosWeatherDataCellValueFontWeight = FontWeight.SemiBold
internal val IosWeatherDisclaimerTopPadding = 8.dp
internal val IosWeatherDisclaimerIconSize = 12.dp
internal val IosWeatherDisclaimerSpacing = 4.dp
internal const val IosWeatherDisclaimerColorAlpha = 1f
internal val IosWeatherReloadingIndicatorCornerRadius = 8.dp
internal val IosWeatherReloadingIndicatorPadding = 8.dp
internal val IosWeatherReloadingIndicatorSize = 16.dp
internal val IosWeatherReloadingIndicatorStrokeWidth = 2.dp
internal const val IosWeatherReloadingIndicatorBackgroundAlpha = 0.9f
internal const val MissingWeatherValueText = "-"
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

internal enum class CurrentWeatherContentState { Loading, Error, Data }

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

internal fun resolveNullableVisibilityWarningIcon(visibilityKm: Double?): WarningIconType =
    visibilityKm?.let(::resolveVisibilityWarningIcon) ?: WarningIconType.Caution

internal fun resolveCriWarningIcon(cri: Double): WarningIconType = when {
    cri >= IosCriHigh -> WarningIconType.Warning
    cri >= IosCriModerate -> WarningIconType.Caution
    else -> WarningIconType.None
}

internal fun resolveGustDifferenceSubTextRes(level: GustDifferenceLevel): Int? =
    if (level == GustDifferenceLevel.LOCALIZED_GUST) R.string.weather_gust_warning else null

internal fun resolveCurrentWeatherVisibility(data: WeatherData): Double? = data.current?.visibility

internal fun isSnowingWeatherCode(weatherCode: Int?): Boolean = when (weatherCode) {
    71, 73, 75, 77, 85, 86 -> true
    else -> false
}

internal fun resolvePrecipitationLabelRes(weatherCode: Int?): Int =
    if (isSnowingWeatherCode(weatherCode)) R.string.weather_snowfall else R.string.weather_precipitation

internal fun resolveCurrentWeatherContentState(
    hourlyForecast: List<HourlyWeatherData>,
    isLoading: Boolean,
    hasError: Boolean,
): CurrentWeatherContentState = when {
    hourlyForecast.isEmpty() && isLoading -> CurrentWeatherContentState.Loading
    hourlyForecast.isEmpty() && hasError -> CurrentWeatherContentState.Error
    else -> CurrentWeatherContentState.Data
}

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
    error: WeatherError? = null,
    onWeatherInfoRequested: (WeatherInfoTopic) -> Unit = {},
) {
    val current = data.current

    val temperatureRange = resolveForecastTemperatureRange(data.hourlyForecast)
    val maxTemperatureText = formatNullableIosTemperatureDegrees(temperatureRange?.first ?: current?.temperature)
    val minTemperatureText = formatNullableIosTemperatureDegrees(temperatureRange?.second ?: current?.temperature)
    val visibility = resolveCurrentWeatherVisibility(data)
    val gustDiff = current?.let { GustDifferenceCalculator.calculateGustDifference(it.windSpeed, it.windGusts) }

    val (windCaution, windDanger) = iosWindSpeedThresholds(category)
    val windWarning = when {
        current == null -> WarningIconType.None
        current.windSpeed >= windDanger -> WarningIconType.Warning
        current.windSpeed >= windCaution -> WarningIconType.Caution
        else -> WarningIconType.None
    }
    val tempWarning = current?.temperature?.let(::resolveTemperatureWarningIcon) ?: WarningIconType.None
    val gustWarning = when (current?.gustDifferenceLevel) {
        GustDifferenceLevel.SAFE -> WarningIconType.None
        GustDifferenceLevel.LOCALIZED_GUST -> WarningIconType.Info
        GustDifferenceLevel.CAUTION -> WarningIconType.Caution
        GustDifferenceLevel.DANGER -> WarningIconType.Warning
        null -> WarningIconType.None
    }
    val gustSubText = current?.gustDifferenceLevel?.let(::resolveGustDifferenceSubTextRes)
    val precipWarning = current?.precipitation?.let(::resolvePrecipitationWarningIcon) ?: WarningIconType.None
    val visibilityWarning = resolveNullableVisibilityWarningIcon(visibility)
    val criWarning = current?.cri?.let(::resolveCriWarningIcon) ?: WarningIconType.None
    val shouldShowReloadingIndicator = shouldShowWeatherReloadingIndicator(isLoading, data.hourlyForecast)
    val contentState = resolveCurrentWeatherContentState(
        hourlyForecast = data.hourlyForecast,
        isLoading = isLoading,
        hasError = error != null,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IosWeatherForecastCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = IosWeatherForecastCardContainerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IosWeatherForecastCardPadding),
            verticalArrangement = Arrangement.spacedBy(IosWeatherForecastCardSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.weather_section_current),
                    fontSize = IosCurrentWeatherHeaderTitleFontSize,
                    fontWeight = IosCurrentWeatherHeaderTitleFontWeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.weather_drone_weight_label),
                    fontSize = IosCurrentWeatherHeaderDroneWeightFontSize,
                    fontWeight = IosCurrentWeatherHeaderRegularFontWeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                DroneCategoryMenu(category = category, onCategoryChanged = onCategoryChanged)
            }

            when (contentState) {
                CurrentWeatherContentState.Loading -> CurrentWeatherLoadingState()
                CurrentWeatherContentState.Error -> {
                    val message = error?.formatArg?.let { formatArg ->
                        stringResource(error.messageRes, formatArg)
                    } ?: error?.let {
                        stringResource(it.messageRes)
                    }.orEmpty()
                    CurrentWeatherErrorState(message = message)
                }
                CurrentWeatherContentState.Data -> {
                    Box {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PreviewBlock(
                                weatherIcon = WeatherCodeMapper.weatherCodeToIosPrecipitationIcon(
                                    code = current?.weatherCode,
                                    precipitation = current?.precipitation,
                                ),
                                conditionText = current?.weatherCode
                                    ?.let { stringResource(WeatherCodeMapper.weatherCodeToDescriptionRes(it)) }
                                    ?: stringResource(R.string.weather_unknown),
                                temperatureText = formatNullableIosTemperatureDegrees(current?.temperature),
                                temperatureColor = current?.temperature?.let(::interpolateTemperatureColor) ?: Color.Gray,
                                maxTemperatureText = maxTemperatureText,
                                minTemperatureText = minTemperatureText,
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    WeatherDataCell(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Thermostat,
                                        iconColor = current?.temperature?.let(::interpolateTemperatureColor) ?: Color.Gray,
                                        label = stringResource(R.string.weather_temperature),
                                        value = formatNullableIosTemperatureDegrees(current?.temperature),
                                        warningIcon = tempWarning,
                                        onClick = { onWeatherInfoRequested(WeatherInfoTopic.Temperature) },
                                    )
                                    WeatherDataCell(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Air,
                                        iconColor = WindSpeedGreen,
                                        label = stringResource(R.string.weather_wind_speed),
                                        value = formatIosMetersPerSecond(current?.windSpeed),
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
                                        value = current?.windDirection
                                            ?.let { stringResource(resolveWindDirectionLabelRes(it)) }
                                            ?: MissingWeatherValueText,
                                        rotation = current?.windDirection?.toFloat(),
                                    )
                                    WeatherDataCell(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Storm,
                                        iconColor = GustOrange,
                                        label = stringResource(R.string.weather_gust_difference),
                                        value = formatIosMetersPerSecond(gustDiff),
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
                                        label = stringResource(resolvePrecipitationLabelRes(current?.weatherCode)),
                                        value = formatIosPrecipitationIntensity(
                                            precipitation = current?.precipitation,
                                            isSnowing = isSnowingWeatherCode(current?.weatherCode),
                                        ),
                                        warningIcon = precipWarning,
                                        onClick = { onWeatherInfoRequested(WeatherInfoTopic.Precipitation) },
                                    )
                                    WeatherDataCell(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Visibility,
                                        iconColor = VisibilityPurple,
                                        label = stringResource(R.string.weather_visibility),
                                        value = formatNullableIosVisibilityKilometers(visibility),
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
                                        value = formatIosCri(current?.cri),
                                        warningIcon = criWarning,
                                        onClick = { onWeatherInfoRequested(WeatherInfoTopic.Cri) },
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = IosWeatherDisclaimerTopPadding),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = IosWeatherDisclaimerColorAlpha,
                                    ),
                                    modifier = Modifier.size(IosWeatherDisclaimerIconSize),
                                )
                                Spacer(modifier = Modifier.width(IosWeatherDisclaimerSpacing))
                                Text(
                                    text = stringResource(R.string.weather_disclaimer),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = IosWeatherDisclaimerColorAlpha,
                                    ),
                                    textAlign = TextAlign.End,
                                )
                            }
                        }

                        if (shouldShowReloadingIndicator) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(IosWeatherReloadingIndicatorCornerRadius))
                                    .background(
                                        IosWeatherForecastCardContainerColor.copy(
                                            alpha = IosWeatherReloadingIndicatorBackgroundAlpha,
                                        ),
                                    )
                                    .padding(IosWeatherReloadingIndicatorPadding),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(IosWeatherReloadingIndicatorSize),
                                    strokeWidth = IosWeatherReloadingIndicatorStrokeWidth,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentWeatherLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IosCurrentWeatherEmptyStateHeight),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CurrentWeatherErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(IosCurrentWeatherEmptyStateHeight)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = GustOrange,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

internal fun shouldShowWeatherReloadingIndicator(
    isLoading: Boolean,
    hourlyForecast: List<HourlyWeatherData>,
): Boolean = isLoading && hourlyForecast.isNotEmpty()

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
            shape = RoundedCornerShape(IosWeatherDroneCategoryButtonCornerRadius),
            color = WeatherIconBlue.copy(alpha = IosWeatherDroneCategoryButtonBackgroundAlpha),
            onClick = { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = IosWeatherDroneCategoryButtonHorizontalPadding,
                    vertical = IosWeatherDroneCategoryButtonVerticalPadding,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IosWeatherDroneCategoryButtonSpacing),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = WeatherIconBlue,
                    modifier = Modifier.size(IosWeatherDroneCategoryLeadingIconSize),
                )
                Text(
                    text = stringResource(category.labelRes),
                    fontSize = IosWeatherDroneCategoryLabelFontSize,
                    fontWeight = IosWeatherDroneCategoryLabelFontWeight,
                    color = WeatherIconBlue,
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = WeatherIconBlue,
                    modifier = Modifier.size(IosWeatherDroneCategoryChevronIconSize),
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
                                    fontSize = IosWeatherDroneCategoryLabelFontSize,
                                    fontWeight = IosWeatherDroneCategoryLabelFontWeight,
                                )
                                if (entry == category) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = WeatherIconBlue,
                                        modifier = Modifier.size(IosWeatherDroneCategoryCheckmarkSize),
                                    )
                                }
                            }
                            Text(
                                text = stringResource(entry.examplesRes),
                                fontSize = IosWeatherDroneCategoryExampleFontSize,
                                fontWeight = IosCurrentWeatherHeaderRegularFontWeight,
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
    weatherIcon: ImageVector,
    conditionText: String,
    temperatureText: String,
    temperatureColor: Color,
    maxTemperatureText: String,
    minTemperatureText: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IosCurrentWeatherPreviewCornerRadius),
        colors = CardDefaults.cardColors(containerColor = IosCurrentWeatherPreviewContainerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IosCurrentWeatherPreviewPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(IosCurrentWeatherPreviewIconSlotWidth)
                    .height(IosCurrentWeatherPreviewIconSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = conditionText,
                    tint = WeatherIconBlue,
                    modifier = Modifier.size(IosCurrentWeatherPreviewIconSize),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = temperatureText,
                        fontSize = IosCurrentWeatherPreviewTemperatureFontSize,
                        fontWeight = IosCurrentWeatherPreviewTemperatureFontWeight,
                        color = temperatureColor,
                    )
                    Text(
                        text = conditionText,
                        fontSize = IosCurrentWeatherPreviewConditionFontSize,
                        fontWeight = IosCurrentWeatherPreviewRegularFontWeight,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(IosCurrentWeatherPreviewTemperatureRangeSpacing)) {
                    Text(
                        text = stringResource(R.string.weather_temperature_high_prefix) + maxTemperatureText,
                        fontSize = IosCurrentWeatherPreviewTemperatureRangeFontSize,
                        fontWeight = IosCurrentWeatherPreviewRegularFontWeight,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "~",
                        fontSize = IosCurrentWeatherPreviewTemperatureRangeFontSize,
                        fontWeight = IosCurrentWeatherPreviewRegularFontWeight,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.weather_temperature_low_prefix) + minTemperatureText,
                        fontSize = IosCurrentWeatherPreviewTemperatureRangeFontSize,
                        fontWeight = IosCurrentWeatherPreviewRegularFontWeight,
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
    val hasSubText = weatherDataCellHasSubText(subText)
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        ),
        shape = RoundedCornerShape(IosWeatherDataCellCornerRadius),
        colors = CardDefaults.cardColors(containerColor = IosWeatherDataCellContainerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = IosWeatherDataCellMinHeight)
                .padding(
                    vertical = IosWeatherDataCellVerticalPadding,
                    horizontal = IosWeatherDataCellHorizontalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.width(IosWeatherDataCellIconSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(IosWeatherDataCellIconSize)
                        .rotate(rotation?.plus(180f) ?: 0f),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (hasSubText) 1.dp else 2.dp),
            ) {
                Text(
                    text = label,
                    fontSize = if (hasSubText) {
                        IosWeatherDataCellLabelWithSubTextFontSize
                    } else {
                        IosWeatherDataCellLabelFontSize
                    },
                    fontWeight = if (hasSubText) {
                        IosWeatherDataCellRegularFontWeight
                    } else {
                        IosWeatherDataCellHeadlineFontWeight
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = value,
                    fontSize = if (hasSubText) {
                        IosWeatherDataCellValueWithSubTextFontSize
                    } else {
                        IosWeatherDataCellValueFontSize
                    },
                    fontWeight = IosWeatherDataCellValueFontWeight,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (hasSubText) {
                    Text(
                        text = subText.orEmpty(),
                        fontSize = IosWeatherDataCellSubTextFontSize,
                        fontWeight = IosWeatherDataCellRegularFontWeight,
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
                    modifier = Modifier.size(IosWeatherDataCellWarningIconSize),
                )
            }
        }
    }
}

internal fun weatherDataCellHasSubText(subText: String?): Boolean = subText != null

internal fun formatNullableIosTemperatureDegrees(temperatureC: Double?): String =
    temperatureC?.let(::formatIosTemperatureDegrees) ?: MissingWeatherValueText

internal fun formatIosTemperatureDegrees(temperatureC: Double): String =
    "%.0f°".format(Locale.ROOT, temperatureC)

internal fun formatNullableIosVisibilityKilometers(visibilityKm: Double?): String =
    visibilityKm?.let(::formatIosVisibilityKilometers) ?: MissingWeatherValueText

internal fun formatIosVisibilityKilometers(visibilityKm: Double): String =
    "%.1f km".format(Locale.ROOT, visibilityKm)

internal fun formatIosMetersPerSecond(value: Double?): String =
    value?.let { "%.1f m/s".format(Locale.ROOT, it) } ?: MissingWeatherValueText

internal fun formatIosPrecipitationIntensity(
    precipitation: Double?,
    isSnowing: Boolean = false,
): String = precipitation?.let {
    val value = if (isSnowing) it / 10.0 else it
    val unit = if (isSnowing) "cm/h" else "mm/h"
    "%.1f %s".format(Locale.ROOT, value, unit)
} ?: MissingWeatherValueText

internal fun formatIosCri(cri: Double?): String =
    cri?.let { "%.0f".format(Locale.ROOT, it) } ?: MissingWeatherValueText

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
