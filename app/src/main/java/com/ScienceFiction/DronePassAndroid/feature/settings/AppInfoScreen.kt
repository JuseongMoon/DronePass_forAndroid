package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Cloud
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedHeadlineHeaderStyle
import androidx.compose.ui.draw.rotate
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.layout.Arrangement
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleContentGap
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleHeader
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground
import com.ScienceFiction.DronePassAndroid.core.util.openUriSafely

internal val AppInfoIntroIconSize = 60.dp
internal val AppInfoIntroSymbolSize = 36.dp
internal val AppInfoIntroSpacing = 12.dp
// iOS: .title2 SF 심볼(원형 채움 지름 약 26pt)을 32pt 프레임 안에 둔다.
internal val AppInfoFeatureIconSize = 26.dp
internal val AppInfoFeatureIconFrameSize = 32.dp
internal val AppInfoFeatureCircleSymbolSize = 15.dp
internal val AppInfoFeatureHorizontalSpacing = 12.dp
internal val AppInfoFeatureTitleDescriptionSpacing = 4.dp
// iOS List 행 기본 여백(약 8pt) + FeatureRow .padding(.vertical, 4)
internal val AppInfoFeatureVerticalPadding = 12.dp
internal val AppInfoFeatureHorizontalPadding = 16.dp
// iOS 구분선은 글자 시작 위치에 맞춘다: 16 + 아이콘 프레임 32 + 12 / 16 + 아이콘 24 + 12
internal val AppInfoFeatureDividerIndent = 60.dp
internal val AppInfoInfoRowDividerIndent = 52.dp
internal enum class AppInfoFeatureIconStyle {
    Plain,
    CircleFill,
}
internal val AppInfoMultiDroneIcon: ImageVector = Icons.AutoMirrored.Filled.Send
internal val AppInfoMultiDroneIconStyle = AppInfoFeatureIconStyle.CircleFill
internal val AppInfoVisualizationIconStyle = AppInfoFeatureIconStyle.CircleFill
internal val AppInfoExpirationAlertIconStyle = AppInfoFeatureIconStyle.CircleFill
internal val AppInfoWeatherIcon: ImageVector = Icons.Default.WbCloudy
internal val AppInfoKpIndexIcon: ImageVector = Icons.Default.SettingsInputAntenna
internal val AppInfoSunriseSunsetIcon: ImageVector = Icons.Default.WbTwilight
internal val AppInfoShapeManagementIcon: ImageVector = Icons.Default.RadioButtonChecked
internal val AppInfoSearchIconStyle = AppInfoFeatureIconStyle.CircleFill
/** iOS SF Symbols number.circle: 테두리 원 안의 # */
internal val IosNumberCircleIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "IosNumberCircle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes("M12,2.8A9.2,9.2 0,1 1,11.99 2.8Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
        )
        addPath(
            pathData = addPathNodes("M10.4,7.6L9.2,16.4M14.8,7.6L13.6,16.4M7.8,10.2H16.4M7.4,13.8H16"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
        )
    }.build()
}
internal val AppInfoCloudSyncIcon: ImageVector = Icons.Default.Cloud
internal val AppInfoDroneOnestopIcon: ImageVector = Icons.Default.Verified
internal val AppInfoBuildNumberIcon: ImageVector = IosNumberCircleIcon
internal val AppInfoSearchIconColor = Color(0xFF00C7BE)

internal fun appInfoVersionValue(versionName: String, versionCode: Int): String =
    "$versionName ($versionCode)"

internal fun appInfoBuildNumberValue(versionCode: Int): String = versionCode.toString()

internal data class AppInfoContactDisplay(
    val showCompany: Boolean,
    val showEmail: Boolean,
    val showDivider: Boolean,
    val emailUri: String?,
)

internal fun appInfoContactDisplay(companyName: String, email: String): AppInfoContactDisplay {
    val showCompany = companyName.isNotEmpty()
    val showEmail = email.isNotEmpty()
    return AppInfoContactDisplay(
        showCompany = showCompany,
        showEmail = showEmail,
        showDivider = showCompany && showEmail,
        emailUri = email.takeIf { showEmail }?.let(::appInfoEmailUri),
    )
}

internal fun appInfoEmailUri(email: String): String = "mailto:$email"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(
            title = stringResource(R.string.app_info_title),
            closeText = stringResource(R.string.common_close),
            onClose = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = SheetLargeTitleContentGap, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(InsetGroupedSectionSpacing),
        ) {
            InsetGroupedSection(
                header = stringResource(R.string.app_info_section_intro),
                headerStyle = InsetGroupedHeadlineHeaderStyle,
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // iOS VStack(spacing: 12).padding(.vertical, 8) + List 행 기본 여백
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppInfoIntroIcon()
                Spacer(modifier = Modifier.height(AppInfoIntroSpacing))
                Text(
                    text = stringResource(R.string.app_info_description),
                    // iOS .body(17), .secondary
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            }
            InsetGroupedSection(
                header = stringResource(R.string.app_info_section_drone_management),
                headerStyle = InsetGroupedHeadlineHeaderStyle,
            ) {
            FeatureRow(
                icon = AppInfoMultiDroneIcon,
                iconColor = Color(0xFF007AFF),
                iconStyle = AppInfoMultiDroneIconStyle,
                title = stringResource(R.string.app_info_feature_multi_drone_title),
                description = stringResource(R.string.app_info_feature_multi_drone_desc),
            )
            InsetGroupedDivider(startIndent = AppInfoFeatureDividerIndent)
            FeatureRow(
                icon = Icons.Default.Map,
                iconColor = Color(0xFF34C759),
                iconStyle = AppInfoVisualizationIconStyle,
                title = stringResource(R.string.app_info_feature_visualization_title),
                description = stringResource(R.string.app_info_feature_visualization_desc),
            )
            InsetGroupedDivider(startIndent = AppInfoFeatureDividerIndent)
            FeatureRow(
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFFFF9500),
                iconStyle = AppInfoExpirationAlertIconStyle,
                title = stringResource(R.string.app_info_feature_expiration_alert_title),
                description = stringResource(R.string.app_info_feature_expiration_alert_desc),
            )

            }
            InsetGroupedSection(
                header = stringResource(R.string.app_info_section_environmental_info),
                headerStyle = InsetGroupedHeadlineHeaderStyle,
            ) {
            FeatureRow(
                icon = AppInfoWeatherIcon,
                iconColor = Color(0xFF32ADE6), // iOS .cyan
                title = stringResource(R.string.app_info_feature_weather_title),
                description = stringResource(R.string.app_info_feature_weather_desc),
            )
            InsetGroupedDivider(startIndent = AppInfoFeatureDividerIndent)
            FeatureRow(
                icon = AppInfoKpIndexIcon,
                iconColor = Color(0xFFAF52DE),
                title = stringResource(R.string.app_info_feature_kp_index_title),
                description = stringResource(R.string.app_info_feature_kp_index_desc),
            )
            InsetGroupedDivider(startIndent = AppInfoFeatureDividerIndent)
            FeatureRow(
                icon = AppInfoSunriseSunsetIcon,
                iconColor = Color(0xFFFF2D55),
                title = stringResource(R.string.app_info_feature_sunrise_sunset_title),
                description = stringResource(R.string.app_info_feature_sunrise_sunset_desc),
            )

            }
            InsetGroupedSection(
                header = stringResource(R.string.app_info_section_shapes_and_map),
                headerStyle = InsetGroupedHeadlineHeaderStyle,
            ) {
            FeatureRow(
                icon = AppInfoShapeManagementIcon,
                iconColor = Color(0xFF5856D6),
                title = stringResource(R.string.app_info_feature_shape_management_title),
                description = stringResource(R.string.app_info_feature_shape_management_desc),
            )
            InsetGroupedDivider(startIndent = AppInfoFeatureDividerIndent)
            FeatureRow(
                icon = Icons.Default.FileCopy, // iOS doc.on.doc.fill
                iconColor = Color(0xFF30B0C7), // iOS .teal
                title = stringResource(R.string.app_info_feature_shape_duplicate_title),
                description = stringResource(R.string.app_info_feature_shape_duplicate_desc),
            )
            InsetGroupedDivider(startIndent = AppInfoFeatureDividerIndent)
            FeatureRow(
                icon = Icons.Default.Search,
                iconColor = AppInfoSearchIconColor,
                iconStyle = AppInfoSearchIconStyle,
                title = stringResource(R.string.app_info_feature_search_title),
                description = stringResource(R.string.app_info_feature_search_desc),
            )

            }
            InsetGroupedSection(
                header = stringResource(R.string.app_info_section_cloud_and_data),
                headerStyle = InsetGroupedHeadlineHeaderStyle,
            ) {
            FeatureRow(
                icon = AppInfoCloudSyncIcon,
                iconColor = Color(0xFF007AFF),
                title = stringResource(R.string.app_info_feature_cloud_sync_title),
                description = stringResource(R.string.app_info_feature_cloud_sync_desc),
            )
            InsetGroupedDivider(startIndent = AppInfoFeatureDividerIndent)
            FeatureRow(
                icon = AppInfoDroneOnestopIcon,
                iconColor = Color(0xFF34C759),
                title = stringResource(R.string.app_info_feature_drone_onestop_title),
                description = stringResource(R.string.app_info_feature_drone_onestop_desc),
            )

            }
            InsetGroupedSection(
                header = stringResource(R.string.app_info_section_version),
                headerStyle = InsetGroupedHeadlineHeaderStyle,
            ) {
            InfoRow(
                icon = Icons.Outlined.Info, // iOS info.circle
                title = stringResource(R.string.app_info_version_app),
                value = appInfoVersionValue(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            )
            InsetGroupedDivider(startIndent = AppInfoInfoRowDividerIndent)
            InfoRow(
                icon = AppInfoBuildNumberIcon,
                title = stringResource(R.string.app_info_version_build),
                value = appInfoBuildNumberValue(BuildConfig.VERSION_CODE),
            )

            }
            InsetGroupedSection(
                header = stringResource(R.string.app_info_section_contact),
                headerStyle = InsetGroupedHeadlineHeaderStyle,
                // iOS 는 문의 안내를 섹션 footer 로 붙인다.
                footer = stringResource(R.string.app_info_contact_message),
            ) {
            val contactCompany = stringResource(R.string.app_info_contact_company)
            val contactEmail = stringResource(R.string.app_info_contact_email)
            val contactDisplay = appInfoContactDisplay(contactCompany, contactEmail)
            if (contactDisplay.showCompany) {
                InfoRow(
                    icon = Icons.Default.Business,
                    title = contactCompany,
                )
            }
            if (contactDisplay.showDivider) {
                InsetGroupedDivider(startIndent = AppInfoInfoRowDividerIndent)
            }
            if (contactDisplay.showEmail) {
                ContactEmailRow(email = contactEmail)
            }
            }
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
                    colors = listOf(Color(0xFF007AFF), Color(0xFF32ADE6)), // iOS .blue → .cyan
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.AirplanemodeActive,
            contentDescription = null,
            tint = Color.White,
            // iOS airplane 심볼은 오른쪽을 향한다.
            modifier = Modifier
                .size(AppInfoIntroSymbolSize)
                .rotate(90f),
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconColor: Color,
    iconStyle: AppInfoFeatureIconStyle = AppInfoFeatureIconStyle.Plain,
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
        Box(
            modifier = Modifier.size(AppInfoFeatureIconFrameSize),
            contentAlignment = Alignment.Center,
        ) {
            FeatureIcon(
                icon = icon,
                iconColor = iconColor,
                iconStyle = iconStyle,
            )
        }
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
private fun FeatureIcon(
    icon: ImageVector,
    iconColor: Color,
    iconStyle: AppInfoFeatureIconStyle,
) {
    when (iconStyle) {
        AppInfoFeatureIconStyle.Plain -> {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(AppInfoFeatureIconSize),
            )
        }
        AppInfoFeatureIconStyle.CircleFill -> {
            Box(
                modifier = Modifier
                    .size(AppInfoFeatureIconSize)
                    .clip(CircleShape)
                    .background(iconColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(AppInfoFeatureCircleSymbolSize),
                )
            }
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
                // iOS .body .medium, .secondary
                style = MaterialTheme.typography.bodyLarge,
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
            .clickable { openUriSafely(uriHandler, appInfoEmailUri(email)) }
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
