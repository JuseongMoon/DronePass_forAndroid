package com.ScienceFiction.DronePassAndroid.feature.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthState
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneListScreen

/**
 * 설정 화면의 서브 스크린 상태
 */
private enum class SettingsSubScreen {
    Main,
    DroneList,
    AppInfo,
    PatchNotes,
    Terms,
    Privacy,
    LocationTerms
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf(SettingsSubScreen.Main) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState != SettingsSubScreen.Main) {
                // 서브 화면으로 진입: 오른쪽에서 슬라이드인
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                // 설정으로 돌아옴: 왼쪽에서 슬라이드인
                (slideInHorizontally { -it } + fadeIn())
                    .togetherWith(slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "settings_screen_transition"
    ) { screen ->
        when (screen) {
            SettingsSubScreen.Main -> {
                SettingsMainContent(
                    settingsViewModel = settingsViewModel,
                    onNavigateToDroneList = { currentScreen = SettingsSubScreen.DroneList },
                    onNavigateToAppInfo = { currentScreen = SettingsSubScreen.AppInfo },
                    onNavigateToPatchNotes = { currentScreen = SettingsSubScreen.PatchNotes },
                    onNavigateToTerms = { currentScreen = SettingsSubScreen.Terms },
                    onNavigateToPrivacy = { currentScreen = SettingsSubScreen.Privacy },
                    onNavigateToLocationTerms = { currentScreen = SettingsSubScreen.LocationTerms }
                )
            }
            SettingsSubScreen.DroneList -> {
                DroneListScreen(
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
            SettingsSubScreen.AppInfo -> {
                AppInfoScreen(
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
            SettingsSubScreen.PatchNotes -> {
                PatchNotesScreen(
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
            SettingsSubScreen.Terms -> {
                WebDocumentScreen(
                    title = stringResource(R.string.settings_terms),
                    url = "https://dronepass.notion.site/terms",
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
            SettingsSubScreen.Privacy -> {
                WebDocumentScreen(
                    title = stringResource(R.string.settings_privacy),
                    url = "https://dronepass.notion.site/privacy",
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
            SettingsSubScreen.LocationTerms -> {
                WebDocumentScreen(
                    title = stringResource(R.string.settings_location_terms),
                    url = "https://dronepass.notion.site/location-terms",
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainContent(
    settingsViewModel: SettingsViewModel,
    onNavigateToDroneList: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
    onNavigateToPatchNotes: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToLocationTerms: () -> Unit
) {
    val context = LocalContext.current
    val authState by settingsViewModel.authState.collectAsStateWithLifecycle()
    val hideExpiredShapes by settingsViewModel.hideExpiredShapes.collectAsStateWithLifecycle()
    val hideNotStartedShapes by settingsViewModel.hideNotStartedShapes.collectAsStateWithLifecycle()
    val keepScreenAwake by settingsViewModel.keepScreenAwake.collectAsStateWithLifecycle()
    val showFlightZoneLayers by settingsViewModel.showFlightZoneLayers.collectAsStateWithLifecycle()
    val sunriseAlarmEnabled by settingsViewModel.sunriseAlarmEnabled.collectAsStateWithLifecycle()
    val sunsetAlarmEnabled by settingsViewModel.sunsetAlarmEnabled.collectAsStateWithLifecycle()
    val endDateAlarmEnabled by settingsViewModel.endDateAlarmEnabled.collectAsStateWithLifecycle()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeleteExpiredDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.screen_settings),
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ===== 1. 내 정보 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_my_info))

            // 프로필
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.settings_profile),
                subtitle = when (authState) {
                    is AuthState.LoggedIn -> (authState as AuthState.LoggedIn).user.email ?: stringResource(R.string.settings_profile_logged_in)
                    is AuthState.LoggedOut -> stringResource(R.string.settings_profile_logged_out)
                    is AuthState.Loading -> stringResource(R.string.settings_profile_loading)
                    is AuthState.Error -> stringResource(R.string.settings_profile_error)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 드론 관리
            SettingsItem(
                icon = Icons.Default.AirplanemodeActive,
                title = stringResource(R.string.settings_drone_manage),
                subtitle = stringResource(R.string.settings_drone_manage_subtitle),
                onClick = onNavigateToDroneList,
                showArrow = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 2. 비행 환경 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_flight_environment))

            // KP 지수
            SettingsItem(
                icon = Icons.Default.Sensors,
                title = stringResource(R.string.settings_kp_index),
                subtitle = stringResource(R.string.settings_kp_subtitle)
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 날씨 정보
            SettingsItem(
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.settings_weather_info),
                subtitle = stringResource(R.string.settings_weather_subtitle)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 3. 알림 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_notifications))

            // POST_NOTIFICATIONS (Android 13+) + SCHEDULE_EXACT_ALARM (Android 12+)
            // 권한 부재 시 안내 카드 표시 + 요청/설정 진입 흐름 제공
            NotificationPermissionRequest()

            // 일출 알림
            SettingsToggleItem(
                icon = Icons.Default.WbSunny,
                title = stringResource(R.string.settings_sunrise_alarm),
                subtitle = stringResource(R.string.settings_sunrise_alarm_subtitle),
                checked = sunriseAlarmEnabled,
                onCheckedChange = { settingsViewModel.toggleSunriseAlarm(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 일몰 알림
            SettingsToggleItem(
                icon = Icons.Default.WbTwilight,
                title = stringResource(R.string.settings_sunset_alarm),
                subtitle = stringResource(R.string.settings_sunset_alarm_subtitle),
                checked = sunsetAlarmEnabled,
                onCheckedChange = { settingsViewModel.toggleSunsetAlarm(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 종료일 알림
            SettingsToggleItem(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.settings_end_date_alarm),
                subtitle = stringResource(R.string.settings_end_date_alarm_subtitle),
                checked = endDateAlarmEnabled,
                onCheckedChange = { settingsViewModel.toggleEndDateAlarm(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 4. 지도 표시 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_map_display))

            // 만료 도형 숨기기
            SettingsToggleItem(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.settings_hide_expired),
                subtitle = stringResource(R.string.settings_hide_expired_subtitle),
                checked = hideExpiredShapes,
                onCheckedChange = { settingsViewModel.toggleHideExpiredShapes(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 시작 전 도형 숨기기
            SettingsToggleItem(
                icon = Icons.Default.Visibility,
                title = stringResource(R.string.settings_hide_not_started),
                subtitle = stringResource(R.string.settings_hide_not_started_subtitle),
                checked = hideNotStartedShapes,
                onCheckedChange = { settingsViewModel.toggleHideNotStartedShapes(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 화면 항상 켜기
            SettingsToggleItem(
                icon = Icons.Default.ScreenLockPortrait,
                title = stringResource(R.string.settings_keep_screen_awake),
                subtitle = stringResource(R.string.settings_keep_screen_awake_subtitle),
                checked = keepScreenAwake,
                onCheckedChange = { settingsViewModel.toggleKeepScreenAwake(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 비행구역 레이어 표시
            SettingsToggleItem(
                icon = Icons.Default.Map,
                title = stringResource(R.string.settings_flight_zone_layers),
                subtitle = stringResource(R.string.settings_flight_zone_layers_subtitle),
                checked = showFlightZoneLayers,
                onCheckedChange = { settingsViewModel.toggleShowFlightZoneLayers(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 5. 데이터 관리 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_data_management))

            // 만료된 도형 전체 삭제
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = stringResource(R.string.settings_delete_expired_shapes),
                subtitle = stringResource(R.string.settings_delete_expired_shapes_subtitle),
                onClick = { showDeleteExpiredDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 6. 앱 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_app_info))

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_app_version),
                subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 앱 소개
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_app_intro),
                onClick = onNavigateToAppInfo,
                showArrow = true
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 패치노트
            SettingsItem(
                icon = Icons.Default.NewReleases,
                title = stringResource(R.string.settings_patch_notes),
                onClick = onNavigateToPatchNotes,
                showArrow = true
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 이용약관
            SettingsItem(
                icon = Icons.Default.Description,
                title = stringResource(R.string.settings_terms),
                onClick = onNavigateToTerms,
                showArrow = true
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 개인정보 처리방침
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.settings_privacy),
                onClick = onNavigateToPrivacy,
                showArrow = true
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 위치기반서비스 이용약관
            SettingsItem(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.settings_location_terms),
                onClick = onNavigateToLocationTerms,
                showArrow = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 7. 계정 섹션 (로그인 시만) =====
            if (authState is AuthState.LoggedIn) {
                SectionHeader(title = stringResource(R.string.settings_section_account))

                // 로그아웃
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.settings_sign_out),
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showSignOutDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                // 회원탈퇴
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.settings_delete_account),
                    titleColor = MaterialTheme.colorScheme.error,
                    subtitle = stringResource(R.string.settings_delete_account_subtitle),
                    onClick = { showDeleteAccountDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 로그아웃 확인 다이얼로그
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.settings_sign_out)) },
            text = { Text(stringResource(R.string.settings_sign_out_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.signOut()
                        showSignOutDialog = false
                        Toast.makeText(context, context.getString(R.string.settings_sign_out_success), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.settings_sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 회원탈퇴 확인 다이얼로그
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = {
                Text(stringResource(R.string.settings_delete_account_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        settingsViewModel.deleteAccount { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_delete_account_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 만료된 도형 전체 삭제 확인 다이얼로그
    if (showDeleteExpiredDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteExpiredDialog = false },
            title = { Text(stringResource(R.string.settings_delete_expired_shapes)) },
            text = {
                Text(stringResource(R.string.settings_delete_expired_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteExpiredDialog = false
                        settingsViewModel.deleteAllExpiredShapes { deletedCount ->
                            val message = if (deletedCount > 0) {
                                context.getString(R.string.settings_delete_expired_success, deletedCount)
                            } else {
                                context.getString(R.string.settings_delete_expired_none)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteExpiredDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

// ===== 재사용 컴포넌트 =====

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
