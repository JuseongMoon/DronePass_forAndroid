package com.ScienceFiction.DronePassAndroid.feature.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneListScreen
import com.ScienceFiction.DronePassAndroid.feature.kp.KpForecastContent
import com.ScienceFiction.DronePassAndroid.feature.profile.ProfileScreen
import com.ScienceFiction.DronePassAndroid.feature.kp.KpSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.kp.KpViewModel
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastContent
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherViewModel

/**
 * 설정 화면의 서브 스크린 상태.
 *
 * 약관/개인정보/위치약관은 ProfileView 시트 안의 두 번째 ModalBottomSheet 로 이동했으므로
 * 여기에는 더 이상 WebDoc case 가 필요 없다. 패치노트는 자체 서버 마크다운 파일을 fetch 해
 * 네이티브 카드로 렌더링하는 PatchNotesScreen 으로 표시.
 */
private sealed class SettingsSubScreen {
    data object Main : SettingsSubScreen()
    data object DroneList : SettingsSubScreen()
    data object AppInfo : SettingsSubScreen()
    data object PatchNotes : SettingsSubScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    // iOS 동등 오버레이로 호스팅할 때 자체 TopAppBar 를 숨김.
    // 오버레이가 자체 큰 "설정" 헤더(.title .bold) 를 그리므로 이중 헤더 방지.
    showTopAppBar: Boolean = true,
) {
    var currentScreen: SettingsSubScreen by remember { mutableStateOf(SettingsSubScreen.Main) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState !is SettingsSubScreen.Main) {
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
            is SettingsSubScreen.Main -> {
                SettingsMainContent(
                    settingsViewModel = settingsViewModel,
                    showTopAppBar = showTopAppBar,
                    onNavigateToDroneList = { currentScreen = SettingsSubScreen.DroneList },
                    onNavigateToAppInfo = { currentScreen = SettingsSubScreen.AppInfo },
                    onNavigateToPatchNotes = { currentScreen = SettingsSubScreen.PatchNotes },
                )
            }
            is SettingsSubScreen.DroneList -> {
                DroneListScreen(
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
            is SettingsSubScreen.AppInfo -> {
                AppInfoScreen(
                    onBack = { currentScreen = SettingsSubScreen.Main }
                )
            }
            is SettingsSubScreen.PatchNotes -> {
                PatchNotesScreen(
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
    showTopAppBar: Boolean,
    onNavigateToDroneList: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
    onNavigateToPatchNotes: () -> Unit,
) {
    val context = LocalContext.current
    val hideExpiredShapes by settingsViewModel.hideExpiredShapes.collectAsStateWithLifecycle()
    val hideNotStartedShapes by settingsViewModel.hideNotStartedShapes.collectAsStateWithLifecycle()
    val keepScreenAwake by settingsViewModel.keepScreenAwake.collectAsStateWithLifecycle()
    val sunriseAlarmEnabled by settingsViewModel.sunriseAlarmEnabled.collectAsStateWithLifecycle()
    val sunsetAlarmEnabled by settingsViewModel.sunsetAlarmEnabled.collectAsStateWithLifecycle()
    val endDateAlarmEnabled by settingsViewModel.endDateAlarmEnabled.collectAsStateWithLifecycle()
    val currentKpString by settingsViewModel.currentKpString.collectAsStateWithLifecycle()

    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    val koreaFeaturesEnabled by settingsViewModel.koreaFeaturesEnabled.collectAsStateWithLifecycle()

    var showDeleteExpiredDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showKpForecastSheet by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var pendingLanguageChange by remember { mutableStateOf<AppLanguage?>(null) }
    var koreaFeaturesAlertOn by remember { mutableStateOf<Boolean?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        if (showTopAppBar) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_settings),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ===== 1. 내 정보 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_my_info))

            // 프로필 (iOS: 로그인 시 "My Profile" → ProfileView 시트, 비로그인 시 "Sign In / Sign Up" → LoginView)
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.settings_profile),
                onClick = { showProfileSheet = true },
                showArrow = true,
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

            // KP 지수 (iOS settings.kp.current 정합 — "현재 Kp 지수: 4.5" + 클릭 시 시트)
            SettingsItem(
                icon = Icons.Default.Sensors,
                title = stringResource(R.string.settings_kp_index_current, currentKpString),
                onClick = { showKpForecastSheet = true },
                showArrow = true,
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 날씨 정보 (iOS settings.weather.current 정합 — 클릭 시 시트)
            SettingsItem(
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.settings_weather_current),
                onClick = { showWeatherSheet = true },
                showArrow = true,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 3. 알림 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_notifications))

            // POST_NOTIFICATIONS (Android 13+) + SCHEDULE_EXACT_ALARM (Android 12+)
            // 권한 부재 시 안내 카드 표시 + 요청/설정 진입 흐름 제공
            NotificationPermissionRequest()

            // 도형 만료 알림 (iOS settings.notification.shapeExpiry — 알림 섹션 첫 행)
            SettingsToggleItem(
                title = stringResource(R.string.settings_end_date_alarm),
                subtitle = stringResource(R.string.settings_end_date_alarm_subtitle),
                checked = endDateAlarmEnabled,
                onCheckedChange = { settingsViewModel.toggleEndDateAlarm(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            // 일출 알림
            SettingsToggleItem(
                title = stringResource(R.string.settings_sunrise_alarm),
                subtitle = stringResource(R.string.settings_sunrise_alarm_subtitle),
                checked = sunriseAlarmEnabled,
                onCheckedChange = { settingsViewModel.toggleSunriseAlarm(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            // 일몰 알림
            SettingsToggleItem(
                title = stringResource(R.string.settings_sunset_alarm),
                subtitle = stringResource(R.string.settings_sunset_alarm_subtitle),
                checked = sunsetAlarmEnabled,
                onCheckedChange = { settingsViewModel.toggleSunsetAlarm(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 4. 지도 표시 섹션 (iOS settings.section.mapDisplay 정합 순서) =====
            SectionHeader(title = stringResource(R.string.settings_section_map_display))

            // 화면 항상 켜기
            SettingsToggleItem(
                title = stringResource(R.string.settings_keep_screen_awake),
                subtitle = stringResource(R.string.settings_keep_screen_awake_subtitle),
                checked = keepScreenAwake,
                onCheckedChange = { settingsViewModel.toggleKeepScreenAwake(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            // 시작 전 도형 숨기기
            SettingsToggleItem(
                title = stringResource(R.string.settings_hide_not_started),
                subtitle = stringResource(R.string.settings_hide_not_started_subtitle),
                checked = hideNotStartedShapes,
                onCheckedChange = { settingsViewModel.toggleHideNotStartedShapes(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            // 만료 도형 숨기기
            SettingsToggleItem(
                title = stringResource(R.string.settings_hide_expired),
                subtitle = stringResource(R.string.settings_hide_expired_subtitle),
                checked = hideExpiredShapes,
                onCheckedChange = { settingsViewModel.toggleHideExpiredShapes(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            // 만료된 도형 전체 삭제 (destructive — iOS settings.shape.deleteExpired Role.destructive 정합)
            SettingsItem(
                title = stringResource(R.string.settings_delete_expired_shapes),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteExpiredDialog = true },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 6. 앱 섹션 =====
            SectionHeader(title = stringResource(R.string.settings_section_app_info))

            // 언어 (iOS settings.language Picker 정합 — Material DropdownMenu)
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageMenu = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(currentLanguage.displayNameRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false },
                ) {
                    AppLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(stringResource(lang.displayNameRes)) },
                            trailingIcon = if (lang == currentLanguage) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else null,
                            onClick = {
                                showLanguageMenu = false
                                if (lang != currentLanguage) {
                                    pendingLanguageChange = lang
                                }
                            },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            // 한국 특화 기능 (iOS settings.koreaFeatures.toggle 정합)
            SettingsToggleItem(
                title = stringResource(R.string.settings_korea_features),
                subtitle = stringResource(R.string.settings_korea_features_subtitle),
                checked = koreaFeaturesEnabled,
                onCheckedChange = { newValue ->
                    settingsViewModel.toggleKoreaFeatures(newValue)
                    koreaFeaturesAlertOn = newValue
                },
            )

            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 프로필 시트 (iOS ProfileView 정합)
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ProfileScreen(onDismiss = { showProfileSheet = false })
        }
    }

    // 언어 변경 안내 다이얼로그 (iOS settings.language onChange showLanguageChangeAlert 정합)
    pendingLanguageChange?.let { lang ->
        AlertDialog(
            onDismissRequest = { pendingLanguageChange = null },
            title = { Text(stringResource(R.string.settings_language_restart_title)) },
            text = { Text(stringResource(R.string.settings_language_restart_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingLanguageChange = null
                    // dialog dismiss animation 후 setLanguage 호출 (Activity recreate 직전 안정성)
                    scope.launch {
                        kotlinx.coroutines.delay(300)
                        settingsViewModel.setLanguage(lang)
                    }
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLanguageChange = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // 한국 특화 기능 ON/OFF 안내 다이얼로그 (iOS showKoreaFeaturesOnAlert / OffAlert 정합)
    koreaFeaturesAlertOn?.let { isOn ->
        AlertDialog(
            onDismissRequest = { koreaFeaturesAlertOn = null },
            title = {
                Text(
                    stringResource(
                        if (isOn) R.string.settings_korea_features_on_title
                        else R.string.settings_korea_features_off_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (isOn) R.string.settings_korea_features_on_message
                        else R.string.settings_korea_features_off_message
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { koreaFeaturesAlertOn = null }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }

    // KP 예보 시트 (iOS .sheet showKPForecastSheet 정합)
    if (showKpForecastSheet) {
        val kpViewModel: KpViewModel = hiltViewModel()
        val kpIsLoading by kpViewModel.isLoading.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showKpForecastSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                KpSheetHeader(
                    isLoading = kpIsLoading,
                    onRefresh = { kpViewModel.loadKpData() },
                    onInfo = null,
                )
                KpForecastContent(
                    viewModel = kpViewModel,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }

    // 날씨 예보 시트 (iOS .sheet showWeatherInfoSheet 정합)
    if (showWeatherSheet) {
        val weatherViewModel: WeatherViewModel = hiltViewModel()
        val weatherIsLoading by weatherViewModel.isLoading.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showWeatherSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                WeatherSheetHeader(
                    isLoading = weatherIsLoading,
                    onRefresh = { weatherViewModel.refreshWeather() },
                    onInfo = null,
                )
                WeatherForecastContent(
                    viewModel = weatherViewModel,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
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

// 재사용 컴포넌트(SectionHeader / SettingsItem / SettingsToggleItem)는
// SettingsComponents.kt 에 internal 로 이전 — ProfileScreen 등에서 동일 시각 정합으로 재사용.
