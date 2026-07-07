package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppInfoScreenTest {

    @Test
    fun `앱 버전 행은 iOS처럼 버전명과 빌드 번호를 함께 표시한다`() {
        val value = appInfoVersionValue(versionName = "1.2.3", versionCode = 45)

        assertEquals("1.2.3 (45)", value)
    }

    @Test
    fun `빌드 번호 행은 iOS처럼 빌드 번호만 표시한다`() {
        assertEquals("45", appInfoBuildNumberValue(45))
    }

    @Test
    fun `앱 정보 연락처 행은 iOS처럼 비어있는 회사명과 이메일을 숨긴다`() {
        assertEquals(
            AppInfoContactDisplay(
                showCompany = true,
                showEmail = true,
                showDivider = true,
                emailUri = "mailto:support@sciencefiction.co.kr",
            ),
            appInfoContactDisplay(
                companyName = "Science Fiction Inc.",
                email = "support@sciencefiction.co.kr",
            ),
        )
        assertEquals(
            AppInfoContactDisplay(
                showCompany = false,
                showEmail = true,
                showDivider = false,
                emailUri = "mailto:support@sciencefiction.co.kr",
            ),
            appInfoContactDisplay(
                companyName = "",
                email = "support@sciencefiction.co.kr",
            ),
        )
        assertEquals(
            AppInfoContactDisplay(
                showCompany = true,
                showEmail = false,
                showDivider = false,
                emailUri = null,
            ),
            appInfoContactDisplay(
                companyName = "Science Fiction Inc.",
                email = "",
            ),
        )
    }

    @Test
    fun `앱 정보 이메일 링크는 iOS처럼 mailto 스킴을 사용한다`() {
        assertEquals(
            "mailto:support@sciencefiction.co.kr",
            appInfoEmailUri("support@sciencefiction.co.kr"),
        )
    }

    @Test
    fun `앱 정보 feature row 치수는 iOS AppInfoView 를 따른다`() {
        assertEquals(60.dp, AppInfoIntroIconSize)
        assertEquals(36.dp, AppInfoIntroSymbolSize)
        assertEquals(12.dp, AppInfoIntroSpacing)
        assertEquals(32.dp, AppInfoFeatureIconSize)
        assertEquals(20.dp, AppInfoFeatureCircleSymbolSize)
        assertEquals(12.dp, AppInfoFeatureHorizontalSpacing)
        assertEquals(4.dp, AppInfoFeatureTitleDescriptionSpacing)
        assertEquals(4.dp, AppInfoFeatureVerticalPadding)
        assertEquals(16.dp, AppInfoFeatureHorizontalPadding)
    }

    @Test
    fun `앱 정보 섹션 헤더는 iOS AppInfoView headline 스타일을 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/AppInfoScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/AppInfoScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "private fun AppInfoSectionHeader",
                "style = MaterialTheme.typography.titleSmall",
                "fontWeight = FontWeight.SemiBold",
                "color = MaterialTheme.colorScheme.onSurfaceVariant",
                "modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)",
            ),
        )
    }

    @Test
    fun `앱 정보 섹션과 기능 행 순서는 iOS AppInfoView 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/AppInfoScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/AppInfoScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "AppInfoSectionHeader(title = stringResource(R.string.app_info_section_intro))",
                "R.string.app_info_section_intro",
                "R.string.app_info_description",
                "AppInfoSectionHeader(title = stringResource(R.string.app_info_section_drone_management))",
                "R.string.app_info_section_drone_management",
                "R.string.app_info_feature_multi_drone_title",
                "R.string.app_info_feature_visualization_title",
                "R.string.app_info_feature_expiration_alert_title",
                "AppInfoSectionHeader(title = stringResource(R.string.app_info_section_environmental_info))",
                "R.string.app_info_section_environmental_info",
                "R.string.app_info_feature_weather_title",
                "R.string.app_info_feature_kp_index_title",
                "R.string.app_info_feature_sunrise_sunset_title",
                "AppInfoSectionHeader(title = stringResource(R.string.app_info_section_shapes_and_map))",
                "R.string.app_info_section_shapes_and_map",
                "R.string.app_info_feature_shape_management_title",
                "R.string.app_info_feature_shape_duplicate_title",
                "R.string.app_info_feature_search_title",
                "AppInfoSectionHeader(title = stringResource(R.string.app_info_section_cloud_and_data))",
                "R.string.app_info_section_cloud_and_data",
                "R.string.app_info_feature_cloud_sync_title",
                "R.string.app_info_feature_drone_onestop_title",
                "AppInfoSectionHeader(title = stringResource(R.string.app_info_section_version))",
                "R.string.app_info_section_version",
                "R.string.app_info_version_app",
                "R.string.app_info_version_build",
                "AppInfoSectionHeader(title = stringResource(R.string.app_info_section_contact))",
                "R.string.app_info_section_contact",
                "R.string.app_info_contact_company",
                "R.string.app_info_contact_email",
                "R.string.app_info_contact_message",
            ),
        )
    }

    @Test
    fun `KP 지수 기능 아이콘은 iOS antenna radiowaves 의미와 맞는 안테나 아이콘을 사용한다`() {
        assertEquals("Filled.SettingsInputAntenna", AppInfoKpIndexIcon.name)
    }

    @Test
    fun `일출일몰 기능 아이콘은 iOS sunrise fill 의미와 맞는 수평선 해 아이콘을 사용한다`() {
        assertEquals("Filled.WbTwilight", AppInfoSunriseSunsetIcon.name)
    }

    @Test
    fun `다중 드론 기능 아이콘은 iOS paperplane circle fill 의미와 맞는 종이비행기 아이콘을 사용한다`() {
        assertEquals("AutoMirrored.Filled.Send", AppInfoMultiDroneIcon.name)
    }

    @Test
    fun `circle fill 계열 앱 정보 아이콘은 iOS처럼 색상 원형 배지를 사용한다`() {
        assertEquals(AppInfoFeatureIconStyle.CircleFill, AppInfoMultiDroneIconStyle)
        assertEquals(AppInfoFeatureIconStyle.CircleFill, AppInfoVisualizationIconStyle)
        assertEquals(AppInfoFeatureIconStyle.CircleFill, AppInfoExpirationAlertIconStyle)
        assertEquals(AppInfoFeatureIconStyle.CircleFill, AppInfoSearchIconStyle)
    }

    @Test
    fun `실시간 날씨 기능 아이콘은 iOS cloud sun fill 의미와 맞는 구름 아이콘을 사용한다`() {
        assertEquals("Filled.WbCloudy", AppInfoWeatherIcon.name)
    }

    @Test
    fun `도형 관리 기능 아이콘은 iOS circle circle fill 의미와 맞는 원형 아이콘을 사용한다`() {
        assertEquals("Filled.RadioButtonChecked", AppInfoShapeManagementIcon.name)
    }

    @Test
    fun `클라우드 동기화 기능 아이콘은 iOS icloud fill 의미와 맞는 클라우드 동기화 아이콘을 사용한다`() {
        assertEquals("Filled.CloudSync", AppInfoCloudSyncIcon.name)
    }

    @Test
    fun `드론 원스톱 기능 아이콘은 iOS checkmark seal fill 의미와 맞는 인증 배지 아이콘을 사용한다`() {
        assertEquals("Filled.Verified", AppInfoDroneOnestopIcon.name)
    }

    @Test
    fun `빌드 번호 행 아이콘은 iOS number circle 의미와 맞는 숫자 아이콘을 사용한다`() {
        assertEquals("Filled.Numbers", AppInfoBuildNumberIcon.name)
    }

    @Test
    fun `주소 검색 기능 아이콘 색상은 iOS mint 와 맞춘다`() {
        assertEquals(0xFF00C7BE.toInt(), AppInfoSearchIconColor.toArgb())
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in AppInfoScreen.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
