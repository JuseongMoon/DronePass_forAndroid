package com.ScienceFiction.DronePassAndroid.feature.profile

import android.widget.Toast
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
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsToggleItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // 동기화 결과 토스트
    LaunchedEffect(viewModel) {
        viewModel.syncResultMessage.collect { result ->
            val message = when (result) {
                is ProfileViewModel.SyncResult.Success ->
                    context.getString(R.string.profile_sync_success, result.shapeCount)
                is ProfileViewModel.SyncResult.Failure ->
                    context.getString(R.string.profile_sync_failed, result.message)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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

        SettingsToggleItem(
            title = stringResource(R.string.profile_sync_cloud),
            subtitle = stringResource(syncStatus.labelRes),
            checked = isCloudBackupEnabled,
            enabled = !isSyncing && isLoggedIn,
            onCheckedChange = { viewModel.setCloudBackupEnabled(it) },
        )

        // 마지막 동기화 시간
        val lastSyncDisplay = formatLastSync(
            lastRealtimeSyncTime ?: lastBackupTime,
            context.getString(R.string.profile_sync_no_history),
        )
        Text(
            text = stringResource(R.string.profile_sync_last_sync, lastSyncDisplay),
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

        // Footer
        Text(
            text = stringResource(
                if (!isLoggedIn) R.string.profile_sync_footer_login_required
                else R.string.profile_sync_footer_enable_info
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

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
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            SettingsItem(
                title = stringResource(R.string.profile_account_delete),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { if (!isAccountActionInProgress) showDeleteDialog = true },
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
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success) onDismiss()
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

    // 약관/개인정보/위치약관 두 번째 시트 (iOS `.sheet(showTerms)` 정합)
    // 자체 서버(sciencefiction.co.kr) 에서 마크다운 fetch → 자체 렌더링.
    webDocTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { webDocTarget = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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

private fun formatLastSync(timestamp: Long?, fallback: String): String {
    if (timestamp == null || timestamp == 0L) return fallback
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
