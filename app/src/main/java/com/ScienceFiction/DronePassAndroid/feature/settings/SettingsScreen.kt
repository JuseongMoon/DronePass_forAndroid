package com.ScienceFiction.DronePassAndroid.feature.settings
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.ScienceFiction.DronePassAndroid.subscription.EntitlementState
import com.ScienceFiction.DronePassAndroid.subscription.LegacyKind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthState
import com.ScienceFiction.DronePassAndroid.feature.auth.LoginScreen
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneListScreen
import com.ScienceFiction.DronePassAndroid.feature.kp.KpForecastContent
import com.ScienceFiction.DronePassAndroid.feature.profile.ProfileScreen
import com.ScienceFiction.DronePassAndroid.feature.kp.KpSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.kp.KpViewModel
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastContent
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherInfoTopic
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherSheetHeader
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherViewModel
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.UnfoldMore
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRow
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedToggleRow
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGreen

internal data class LanguageSelectionAction(
    val languageToApply: AppLanguage?,
    val showRestartAlert: Boolean,
)

internal const val SettingsLoginSheetShowSkipLogin = false

internal fun resolveLanguageSelectionAction(
    selectedLanguage: AppLanguage,
    currentLanguage: AppLanguage,
): LanguageSelectionAction {
    return if (selectedLanguage == currentLanguage) {
        LanguageSelectionAction(languageToApply = null, showRestartAlert = false)
    } else {
        LanguageSelectionAction(languageToApply = selectedLanguage, showRestartAlert = true)
    }
}

/**
 * 설정 화면.
 *
 * iOS `SettingView` 와 동일하게 드론 관리, 앱 정보, 패치노트는 설정 목록을 push 하지 않고
 * 각각 sheet 로 표시한다. 약관/개인정보는 ProfileView 시트 안의 두 번째
 * ModalBottomSheet 에서 처리한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onAccountSessionEnded: () -> Unit = {},
    contentBottomPadding: Dp = 0.dp,
) {
    var showDroneListSheet by remember { mutableStateOf(false) }
    var showAppInfoSheet by remember { mutableStateOf(false) }
    var showPatchNotesSheet by remember { mutableStateOf(false) }

    SettingsMainContent(
        settingsViewModel = settingsViewModel,
        onNavigateToDroneList = { showDroneListSheet = true },
        onNavigateToAppInfo = { showAppInfoSheet = true },
        onNavigateToPatchNotes = { showPatchNotesSheet = true },
        onAccountSessionEnded = onAccountSessionEnded,
        contentBottomPadding = contentBottomPadding,
    )

    if (showDroneListSheet) {
        DronePassModalBottomSheet(
            onDismissRequest = { showDroneListSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = IosSystemGroupedBackground,
        ) {
            DroneListScreen(onClose = { showDroneListSheet = false })
        }
    }

    if (showAppInfoSheet) {
        DronePassModalBottomSheet(
            onDismissRequest = { showAppInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = IosSystemGroupedBackground,
        ) {
            AppInfoScreen(onBack = { showAppInfoSheet = false })
        }
    }

    if (showPatchNotesSheet) {
        DronePassModalBottomSheet(
            onDismissRequest = { showPatchNotesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = IosSystemGroupedBackground,
        ) {
            PatchNotesScreen(onBack = { showPatchNotesSheet = false })
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
    onAccountSessionEnded: () -> Unit,
    contentBottomPadding: Dp,
) {
    val hideExpiredShapes by settingsViewModel.hideExpiredShapes.collectAsStateWithLifecycle()
    val hideNotStartedShapes by settingsViewModel.hideNotStartedShapes.collectAsStateWithLifecycle()
    val keepScreenAwake by settingsViewModel.keepScreenAwake.collectAsStateWithLifecycle()
    val sunriseAlarmEnabled by settingsViewModel.sunriseAlarmEnabled.collectAsStateWithLifecycle()
    val sunsetAlarmEnabled by settingsViewModel.sunsetAlarmEnabled.collectAsStateWithLifecycle()
    val endDateAlarmEnabled by settingsViewModel.endDateAlarmEnabled.collectAsStateWithLifecycle()
    val currentKpString by settingsViewModel.currentKpString.collectAsStateWithLifecycle()
    val authState by settingsViewModel.authState.collectAsStateWithLifecycle()
    val plan by settingsViewModel.subscriptionManager.status.collectAsStateWithLifecycle()
    val quotaLimits by settingsViewModel.subscriptionManager.limits.collectAsStateWithLifecycle()
    val quotaUsage by settingsViewModel.subscriptionManager.usage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    val koreaFeaturesEnabled by settingsViewModel.koreaFeaturesEnabled.collectAsStateWithLifecycle()
    val isLoggedIn = authState is AuthState.LoggedIn

    var showDeleteExpiredDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var showKpForecastSheet by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }
    var showKpInfoSheet by remember { mutableStateOf(false) }
    var showWeatherInfoSheet by remember { mutableStateOf(false) }
    var selectedWeatherInfoTopic by remember { mutableStateOf<WeatherInfoTopic?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showLanguageChangeAlert by rememberSaveable { mutableStateOf(false) }
    var koreaFeaturesAlertOn by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(InsetGroupedSectionSpacing),
        ) {
            // ===== 1. 내 정보 (iOS: 내 프로필 / 로그인 + 내 드론 관리하기) =====
            InsetGroupedSection(header = stringResource(R.string.settings_section_my_info)) {
                InsetGroupedRow(
                    title = if (isLoggedIn) {
                        stringResource(R.string.settings_profile_my)
                    } else {
                        stringResource(R.string.settings_profile_login)
                    },
                    showChevron = true,
                    onClick = {
                        if (isLoggedIn) showProfileSheet = true else showLoginSheet = true
                    },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.settings_drone_manage),
                    showChevron = true,
                    onClick = onNavigateToDroneList,
                )
            }

            // ===== 2. 구독 (iOS subscription.settings.*: 플랜 / 도형 / 스케치 선 / 드론 / 알아보기 / 구매 복원) =====
            val isPro = plan.entitlement == EntitlementState.PRO
            InsetGroupedSection(
                header = stringResource(R.string.subscription_settings_section),
                footer = buildList {
                    if (plan.paymentIssue) add(stringResource(R.string.subscription_payment_issue))
                    add(stringResource(R.string.subscription_cross_platform_notice))
                }.joinToString("\n"),
            ) {
                InsetGroupedRow(
                    title = stringResource(R.string.subscription_plan),
                    value = when {
                        plan.isPaidSubscriber && plan.legacyKind == LegacyKind.EARLY_ACCESS -> stringResource(R.string.subscription_pro) + " · " + stringResource(R.string.subscription_early_badge)
                        plan.legacyKind == LegacyKind.EARLY_ACCESS -> stringResource(R.string.subscription_early_lifetime)
                        isPro -> stringResource(R.string.subscription_pro)
                        else -> stringResource(R.string.subscription_free)
                    },
                    valueColor = if (isPro) IosSystemGreen else IosSecondaryLabel,
                )
                if (!isPro) {
                    SubscriptionUsageRow(
                        title = stringResource(R.string.subscription_settings_shapes),
                        used = quotaUsage.shapes,
                        limit = quotaLimits.freeShapes,
                    )
                    SubscriptionUsageRow(
                        title = stringResource(R.string.subscription_settings_sketches),
                        used = quotaUsage.sketches,
                        limit = quotaLimits.freeSketches,
                    )
                    SubscriptionUsageRow(
                        title = stringResource(R.string.subscription_settings_drones),
                        used = quotaUsage.drones,
                        limit = quotaLimits.freeDrones,
                    )
                }
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = if (isPro) {
                        stringResource(R.string.subscription_benefits)
                    } else {
                        stringResource(R.string.subscription_learn_more)
                    },
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = { settingsViewModel.subscriptionManager.showPaywall("settings") },
                )
                if (plan.isPaidSubscriber) {
                    InsetGroupedDivider()
                    InsetGroupedRow(
                        title = stringResource(R.string.subscription_manage),
                        titleColor = MaterialTheme.colorScheme.primary,
                        onClick = { settingsViewModel.subscriptionManager.openManagement(context) },
                    )
                }
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.subscription_restore),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = { settingsViewModel.subscriptionManager.restore() },
                )
            }

            // ===== 3. 비행 환경 =====
            InsetGroupedSection(header = stringResource(R.string.settings_section_flight_environment)) {
                InsetGroupedRow(
                    title = stringResource(R.string.settings_kp_index_current, currentKpString),
                    showChevron = true,
                    onClick = { showKpForecastSheet = true },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.settings_weather_current),
                    showChevron = true,
                    onClick = { showWeatherSheet = true },
                )
            }

            // ===== 4. 알림 =====
            InsetGroupedSection(header = stringResource(R.string.settings_section_notifications)) {
                    // POST_NOTIFICATIONS (Android 13+) + SCHEDULE_EXACT_ALARM (Android 12+)
                    // 권한이 없을 때만 섹션 첫 행에 안내를 표시한다.
                    NotificationPermissionRequest()
                    InsetGroupedToggleRow(
                        title = stringResource(R.string.settings_end_date_alarm),
                        subtitle = stringResource(R.string.settings_end_date_alarm_subtitle),
                        checked = endDateAlarmEnabled,
                        onCheckedChange = { settingsViewModel.toggleEndDateAlarm(it) },
                    )
                    InsetGroupedDivider()
                    InsetGroupedToggleRow(
                        title = stringResource(R.string.settings_sunrise_alarm),
                        subtitle = stringResource(R.string.settings_sunrise_alarm_subtitle),
                        checked = sunriseAlarmEnabled,
                        onCheckedChange = { settingsViewModel.toggleSunriseAlarm(it) },
                    )
                    InsetGroupedDivider()
                    InsetGroupedToggleRow(
                        title = stringResource(R.string.settings_sunset_alarm),
                        subtitle = stringResource(R.string.settings_sunset_alarm_subtitle),
                        checked = sunsetAlarmEnabled,
                        onCheckedChange = { settingsViewModel.toggleSunsetAlarm(it) },
                    )
            }

            // ===== 5. 지도 표시 =====
            InsetGroupedSection(header = stringResource(R.string.settings_section_map_display)) {
                InsetGroupedToggleRow(
                    title = stringResource(R.string.settings_keep_screen_awake),
                    subtitle = stringResource(R.string.settings_keep_screen_awake_subtitle),
                    checked = keepScreenAwake,
                    onCheckedChange = { settingsViewModel.toggleKeepScreenAwake(it) },
                )
                InsetGroupedDivider()
                InsetGroupedToggleRow(
                    title = stringResource(R.string.settings_hide_not_started),
                    subtitle = stringResource(R.string.settings_hide_not_started_subtitle),
                    checked = hideNotStartedShapes,
                    onCheckedChange = { settingsViewModel.toggleHideNotStartedShapes(it) },
                )
                InsetGroupedDivider()
                InsetGroupedToggleRow(
                    title = stringResource(R.string.settings_hide_expired),
                    subtitle = stringResource(R.string.settings_hide_expired_subtitle),
                    checked = hideExpiredShapes,
                    onCheckedChange = { settingsViewModel.toggleHideExpiredShapes(it) },
                )
                InsetGroupedDivider()
                // destructive — iOS settings.shape.deleteExpired Role.destructive 정합
                InsetGroupedRow(
                    title = stringResource(R.string.settings_delete_expired_shapes),
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteExpiredDialog = true },
                )
            }

            // ===== 6. 앱 =====
            InsetGroupedSection(header = stringResource(R.string.settings_section_app_info)) {
                // 언어 (iOS Picker 메뉴 정합 — Material DropdownMenu)
                Box {
                    InsetGroupedRow(
                        title = stringResource(R.string.settings_language),
                        value = stringResource(currentLanguage.displayNameRes),
                        onClick = { showLanguageMenu = true },
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = null,
                                tint = IosSecondaryLabel,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        modifier = Modifier.align(Alignment.TopEnd),
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
                                    val action = resolveLanguageSelectionAction(
                                        selectedLanguage = lang,
                                        currentLanguage = currentLanguage,
                                    )
                                    action.languageToApply?.let { language ->
                                        showLanguageChangeAlert = action.showRestartAlert
                                        settingsViewModel.setLanguage(language)
                                    }
                                },
                            )
                        }
                    }
                }
                InsetGroupedDivider()
                InsetGroupedToggleRow(
                    title = stringResource(R.string.settings_korea_features),
                    checked = koreaFeaturesEnabled,
                    onCheckedChange = { newValue ->
                        settingsViewModel.toggleKoreaFeatures(newValue)
                        koreaFeaturesAlertOn = newValue
                    },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.settings_app_intro),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToAppInfo,
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.settings_patch_notes),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToPatchNotes,
                )
            }

            Spacer(modifier = Modifier.height(12.dp + contentBottomPadding))
        }
    }

    // 프로필 시트 (iOS ProfileView 정합)
    if (showProfileSheet) {
        DronePassModalBottomSheet(
            onDismissRequest = {
                showProfileSheet = false
                settingsViewModel.checkAuthState()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = IosSystemGroupedBackground,
        ) {
            ProfileScreen(
                onDismiss = {
                    showProfileSheet = false
                    settingsViewModel.checkAuthState()
                },
                onAccountSessionEnded = onAccountSessionEnded,
            )
        }
    }

    // 로그인 시트 (iOS SettingView: 비로그인 시 LoginView sheet)
    if (showLoginSheet) {
        DronePassModalBottomSheet(
            onDismissRequest = {
                showLoginSheet = false
                settingsViewModel.checkAuthState()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            LoginScreen(
                onLoginSuccess = {
                    showLoginSheet = false
                    settingsViewModel.checkAuthState()
                },
                onSkipLogin = {
                    showLoginSheet = false
                    settingsViewModel.checkAuthState()
                },
                showSkipLogin = SettingsLoginSheetShowSkipLogin,
            )
        }
    }

    // 언어 변경 안내 다이얼로그 (iOS settings.language onChange showLanguageChangeAlert 정합)
    if (showLanguageChangeAlert) {
        AlertDialog(
            onDismissRequest = { showLanguageChangeAlert = false },
            title = { Text(stringResource(R.string.settings_language_restart_title)) },
            text = { Text(stringResource(R.string.settings_language_restart_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLanguageChangeAlert = false
                }) {
                    Text(stringResource(R.string.common_confirm))
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
        DronePassModalBottomSheet(
            onDismissRequest = { showKpForecastSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                KpSheetHeader(
                    isLoading = kpIsLoading,
                    onRefresh = { kpViewModel.loadKpData() },
                    onInfo = { showKpInfoSheet = true },
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
        DronePassModalBottomSheet(
            onDismissRequest = { showWeatherSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                WeatherSheetHeader(
                    isLoading = weatherIsLoading,
                    onRefresh = { weatherViewModel.refreshWeather() },
                    onInfo = {
                        selectedWeatherInfoTopic = null
                        showWeatherInfoSheet = true
                    },
                )
                WeatherForecastContent(
                    viewModel = weatherViewModel,
                    modifier = Modifier.padding(bottom = 16.dp),
                    onWeatherInfoRequested = { topic ->
                        selectedWeatherInfoTopic = topic
                        showWeatherInfoSheet = true
                    },
                )
            }
        }
    }

    // KP 정보 가이드 (iOS KPInfoView 정합)
    if (showKpInfoSheet) {
        DronePassModalBottomSheet(
            onDismissRequest = { showKpInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            KpInfoGuideSheet(onDismiss = { showKpInfoSheet = false })
        }
    }

    // 날씨 정보 가이드 (iOS WeatherInfoView 정합)
    if (showWeatherInfoSheet) {
        val weatherViewModel: WeatherViewModel = hiltViewModel()
        val selectedCategory by weatherViewModel.selectedCategory.collectAsStateWithLifecycle()
        val isUsingGps by weatherViewModel.isUsingGps.collectAsStateWithLifecycle()
        val locationAccuracyMeters by weatherViewModel.locationAccuracyMeters.collectAsStateWithLifecycle()
        DronePassModalBottomSheet(
            onDismissRequest = { showWeatherInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            WeatherInfoGuideSheet(
                onDismiss = { showWeatherInfoSheet = false },
                initialTopic = selectedWeatherInfoTopic,
                category = selectedCategory,
                onCategoryChanged = { weatherViewModel.setCategory(it, refreshWeather = false) },
                isUsingGps = isUsingGps,
                locationAccuracyMeters = locationAccuracyMeters,
            )
        }
    }

    // 만료된 도형 전체 삭제 확인 다이얼로그
    if (showDeleteExpiredDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteExpiredDialog = false },
            title = { Text(stringResource(R.string.settings_delete_expired_alert_title)) },
            text = {
                Text(stringResource(R.string.settings_delete_expired_alert_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteExpiredDialog = false
                        settingsViewModel.deleteAllExpiredShapes()
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


/** 무료 플랜 사용량 행. iOS 처럼 한도에 도달하면 값을 빨간색으로 표시한다. */
@Composable
private fun SubscriptionUsageRow(title: String, used: Int, limit: Int) {
    InsetGroupedDivider()
    InsetGroupedRow(
        title = title,
        value = stringResource(R.string.subscription_usage_value, used, limit),
        valueColor = if (used >= limit) MaterialTheme.colorScheme.error else IosSecondaryLabel,
    )
}
