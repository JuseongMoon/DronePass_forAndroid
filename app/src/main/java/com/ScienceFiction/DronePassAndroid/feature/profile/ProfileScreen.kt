package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.document.PrivacyPolicyScreen
import com.ScienceFiction.DronePassAndroid.feature.document.TermsOfServiceScreen
import com.ScienceFiction.DronePassAndroid.feature.settings.SectionHeader
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsItem
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal const val ProfileDocumentSheetSkipPartiallyExpanded = false

/**
 * iOS `ProfileView` 1:1 정합 시트 콘텐츠.
 * ModalBottomSheet 자식으로 호스팅된다.
 * 3섹션: 동기화 / 약관 및 정책 / 계정 관리.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isCloudBackupEnabled by viewModel.isCloudBackupEnabled.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isAccountActionInProgress by viewModel.isAccountActionInProgress.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastRealtimeSyncTime by viewModel.lastRealtimeSyncTime.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteFinalDialog by remember { mutableStateOf(false) }
    var webDocTarget by remember { mutableStateOf<WebDocTarget?>(null) }
    var resultDialog by remember { mutableStateOf<ProfileResultDialog?>(null) }

    // 동기화 결과 알림 — iOS ProfileView showSyncResult alert 정합.
    LaunchedEffect(viewModel) {
        viewModel.syncResultMessage.collect { result ->
            val message = when (result) {
                is ProfileViewModel.SyncResult.Success ->
                    context.getString(R.string.profile_sync_success, result.shapeCount)
                is ProfileViewModel.SyncResult.Failure ->
                    context.getString(R.string.profile_sync_failed, result.message)
            }
            resultDialog = ProfileResultDialog(
                titleRes = R.string.profile_sync_alert_title,
                message = message,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // 시트 헤더 (iOS NavigationView 제목 정합)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()

        // ===== 1. 동기화 섹션 =====
        SectionHeader(title = stringResource(R.string.profile_section_sync))

        ProfileCloudSyncToggleItem(
            title = stringResource(R.string.profile_sync_cloud),
            subtitle = stringResource(syncStatus.labelRes),
            subtitleColor = syncStatus.color,
            checked = isCloudBackupEnabled,
            enabled = !isSyncing,
            showProgress = shouldShowProfileSyncProgress(isSyncing),
            onCheckedChange = { viewModel.setCloudBackupEnabled(it) },
        )

        // 마지막 동기화 시간 — iOS lastSyncTimeText 라벨 분기 정합.
        val lastSyncDisplay = when {
            lastRealtimeSyncTime.hasSyncTimestamp() ->
                stringResource(R.string.profile_sync_last_sync, formatLastSync(lastRealtimeSyncTime))
            lastBackupTime.hasSyncTimestamp() ->
                stringResource(R.string.profile_backup_last_backup, formatLastSync(lastBackupTime))
            else ->
                stringResource(R.string.profile_sync_no_history)
        }
        Text(
            text = lastSyncDisplay,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // 수동 백업 (로그인 + 토글 ON 시만 표시)
        if (isLoggedIn && isCloudBackupEnabled) {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsItem(
                        title = stringResource(R.string.profile_backup_manual),
                        titleColor = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.syncToCloud() },
                        enabled = shouldEnableProfileManualBackup(isSyncing),
                    )
                }
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 16.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        val syncFooterTextRes = when {
            !isLoggedIn -> R.string.profile_sync_footer_login_required
            !isCloudBackupEnabled -> R.string.profile_sync_footer_enable_info
            else -> null
        }
        syncFooterTextRes?.let { textRes ->
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 2. 약관 및 정책 섹션 =====
        SectionHeader(title = stringResource(R.string.profile_section_terms))

        SettingsItem(
            title = stringResource(R.string.profile_terms_service),
            onClick = { webDocTarget = WebDocTarget.Terms },
            showArrow = true,
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        SettingsItem(
            title = stringResource(R.string.profile_terms_privacy),
            onClick = { webDocTarget = WebDocTarget.Privacy },
            showArrow = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 3. 계정 관리 섹션 (로그인 시만) =====
        if (isLoggedIn) {
            SectionHeader(title = stringResource(R.string.profile_section_account))

            SettingsItem(
                title = stringResource(R.string.profile_account_logout),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { if (!isAccountActionInProgress) showLogoutDialog = true },
                enabled = shouldEnableProfileAccountAction(isAccountActionInProgress),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            SettingsItem(
                title = stringResource(R.string.profile_account_delete),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { if (!isAccountActionInProgress) showDeleteDialog = true },
                enabled = shouldEnableProfileAccountAction(isAccountActionInProgress),
            )

            Text(
                text = stringResource(R.string.profile_account_delete_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 로그아웃 확인 다이얼로그
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.profile_account_logout)) },
            text = { Text(stringResource(R.string.profile_logout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut { onDismiss() }
                }) {
                    Text(
                        stringResource(R.string.profile_account_logout),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // 계정 삭제 1차 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.profile_account_delete)) },
            text = { Text(stringResource(R.string.profile_delete_account_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    showDeleteFinalDialog = true
                }) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // 계정 삭제 2차 최종 확인 (iOS finalConfirm)
    if (showDeleteFinalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFinalDialog = false },
            title = { Text(stringResource(R.string.profile_delete_account_final_title)) },
            text = { Text(stringResource(R.string.profile_delete_account_final_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteFinalDialog = false
                    viewModel.deleteAccount { success, message ->
                        if (success) {
                            onDismiss()
                        } else {
                            resultDialog = ProfileResultDialog(
                                titleRes = R.string.profile_delete_account_error_title,
                                message = message,
                            )
                        }
                    }
                }) {
                    Text(
                        stringResource(R.string.profile_delete_account_final_button),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFinalDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    resultDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { resultDialog = null },
            title = { Text(stringResource(dialog.titleRes)) },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(onClick = { resultDialog = null }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }

    // 약관/개인정보/위치약관 두 번째 시트 (iOS `.sheet(showTerms)` 정합)
    // 자체 서버(sciencefiction.co.kr) 에서 마크다운 fetch → 자체 렌더링.
    webDocTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { webDocTarget = null },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = ProfileDocumentSheetSkipPartiallyExpanded,
            ),
        ) {
            when (target) {
                WebDocTarget.Terms -> TermsOfServiceScreen(onDismiss = { webDocTarget = null })
                WebDocTarget.Privacy -> PrivacyPolicyScreen(onDismiss = { webDocTarget = null })
            }
        }
    }
}

private sealed class WebDocTarget {
    data object Terms : WebDocTarget()
    data object Privacy : WebDocTarget()
}

private data class ProfileResultDialog(
    @StringRes val titleRes: Int,
    val message: String,
)

@Composable
private fun ProfileCloudSyncToggleItem(
    title: String,
    subtitle: String,
    subtitleColor: androidx.compose.ui.graphics.Color,
    checked: Boolean,
    enabled: Boolean,
    showProgress: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable { onCheckedChange(!checked) }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
            )
        }
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

internal fun shouldShowProfileSyncProgress(isSyncing: Boolean): Boolean = isSyncing

internal fun shouldEnableProfileManualBackup(isSyncing: Boolean): Boolean = !isSyncing

internal fun shouldEnableProfileAccountAction(isAccountActionInProgress: Boolean): Boolean =
    !isAccountActionInProgress

private fun Long?.hasSyncTimestamp(): Boolean = this != null && this != 0L

private fun formatLastSync(timestamp: Long?): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    return formatter.format(Date(timestamp ?: 0L))
}
