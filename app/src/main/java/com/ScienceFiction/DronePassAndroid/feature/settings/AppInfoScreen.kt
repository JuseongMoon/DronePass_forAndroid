package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.openUriSafely

internal val AppInfoIntroIconSize = 60.dp
internal val AppInfoIntroSymbolSize = 36.dp
internal val AppInfoIntroSpacing = 12.dp
internal val AppInfoFeatureIconSize = 32.dp
internal val AppInfoFeatureHorizontalSpacing = 12.dp
internal val AppInfoFeatureTitleDescriptionSpacing = 4.dp
internal val AppInfoFeatureVerticalPadding = 4.dp
internal val AppInfoFeatureHorizontalPadding = 16.dp
internal val AppInfoMultiDroneIcon: ImageVector = Icons.AutoMirrored.Filled.Send
internal val AppInfoWeatherIcon: ImageVector = Icons.Default.WbCloudy
internal val AppInfoKpIndexIcon: ImageVector = Icons.Default.SettingsInputAntenna
internal val AppInfoSunriseSunsetIcon: ImageVector = Icons.Default.WbTwilight
internal val AppInfoShapeManagementIcon: ImageVector = Icons.Default.RadioButtonChecked
internal val AppInfoCloudSyncIcon: ImageVector = Icons.Default.CloudSync
internal val AppInfoDroneOnestopIcon: ImageVector = Icons.Default.Verified
internal val AppInfoBuildNumberIcon: ImageVector = Icons.Default.Numbers
internal val AppInfoSearchIconColor = Color(0xFF00C7BE)

internal fun appInfoVersionValue(versionName: String, versionCode: Int): String {
    return "$versionName ($versionCode)"
}

internal fun appInfoBuildNumberValue(versionCode: Int): String = versionCode.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LargeTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_info_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            actions = {
                TextButton(onClick = onBack) {
                    Text(
                        text = stringResource(R.string.common_close),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            SectionHeader(title = stringResource(R.string.app_info_section_intro))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppInfoIntroIcon()
                Spacer(modifier = Modifier.height(AppInfoIntroSpacing))
                Text(
                    text = stringResource(R.string.app_info_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = stringResource(R.string.app_info_section_drone_management))
            FeatureRow(
                icon = AppInfoMultiDroneIcon,
                iconColor = Color(0xFF007AFF),
                title = stringResource(R.string.app_info_feature_multi_drone_title),
                description = stringResource(R.string.app_info_feature_multi_drone_desc),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            FeatureRow(
                icon = Icons.Default.Map,
                iconColor = Color(0xFF34C759),
                title = stringResource(R.string.app_info_feature_visualization_title),
                description = stringResource(R.string.app_info_feature_visualization_desc),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            FeatureRow(
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFFFF9500),
                title = stringResource(R.string.app_info_feature_expiration_alert_title),
                description = stringResource(R.string.app_info_feature_expiration_alert_desc),
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = stringResource(R.string.app_info_section_environmental_info))
            FeatureRow(
                icon = AppInfoWeatherIcon,
                iconColor = Color(0xFF5AC8FA),
                title = stringResource(R.string.app_info_feature_weather_title),
                description = stringResource(R.string.app_info_feature_weather_desc),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            FeatureRow(
                icon = AppInfoKpIndexIcon,
                iconColor = Color(0xFFAF52DE),
                title = stringResource(R.string.app_info_feature_kp_index_title),
                description = stringResource(R.string.app_info_feature_kp_index_desc),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            FeatureRow(
                icon = AppInfoSunriseSunsetIcon,
                iconColor = Color(0xFFFF2D55),
                title = stringResource(R.string.app_info_feature_sunrise_sunset_title),
                description = stringResource(R.string.app_info_feature_sunrise_sunset_desc),
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = stringResource(R.string.app_info_section_shapes_and_map))
            FeatureRow(
                icon = AppInfoShapeManagementIcon,
                iconColor = Color(0xFF5856D6),
                title = stringResource(R.string.app_info_feature_shape_management_title),
                description = stringResource(R.string.app_info_feature_shape_management_desc),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            FeatureRow(
                icon = Icons.Default.ContentCopy,
                iconColor = Color(0xFF5AC8FA),
                title = stringResource(R.string.app_info_feature_shape_duplicate_title),
                description = stringResource(R.string.app_info_feature_shape_duplicate_desc),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            FeatureRow(
                icon = Icons.Default.Search,
                iconColor = AppInfoSearchIconColor,
                title = stringResource(R.string.app_info_feature_search_title),
                description = stringResource(R.string.app_info_feature_search_desc),
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = stringResource(R.string.app_info_section_cloud_and_data))
            FeatureRow(
                icon = AppInfoCloudSyncIcon,
                iconColor = Color(0xFF007AFF),
                title = stringResource(R.string.app_info_feature_cloud_sync_title),
                description = stringResource(R.string.app_info_feature_cloud_sync_desc),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            FeatureRow(
                icon = AppInfoDroneOnestopIcon,
                iconColor = Color(0xFF34C759),
                title = stringResource(R.string.app_info_feature_drone_onestop_title),
                description = stringResource(R.string.app_info_feature_drone_onestop_desc),
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = stringResource(R.string.app_info_section_version))
            InfoRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.app_info_version_app),
                value = appInfoVersionValue(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            InfoRow(
                icon = AppInfoBuildNumberIcon,
                title = stringResource(R.string.app_info_version_build),
                value = appInfoBuildNumberValue(BuildConfig.VERSION_CODE),
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = stringResource(R.string.app_info_section_contact))
            InfoRow(
                icon = Icons.Default.Business,
                title = stringResource(R.string.app_info_contact_company),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            ContactEmailRow(email = stringResource(R.string.app_info_contact_email))
            Text(
                text = stringResource(R.string.app_info_contact_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AppInfoIntroIcon() {
    Box(
        modifier = Modifier
            .size(AppInfoIntroIconSize)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF007AFF), Color(0xFF5AC8FA)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.AirplanemodeActive,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(AppInfoIntroSymbolSize),
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppInfoFeatureHorizontalPadding,
                vertical = AppInfoFeatureVerticalPadding,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(AppInfoFeatureIconSize),
        )
        Spacer(modifier = Modifier.width(AppInfoFeatureHorizontalSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppInfoFeatureTitleDescriptionSpacing))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContactEmailRow(email: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openUriSafely(uriHandler, "mailto:$email") }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}
