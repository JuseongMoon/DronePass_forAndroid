package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class ProfileSheetParityTest {

    @Test
    fun `약관 문서 시트는 iOS medium large detent 처럼 부분 확장을 허용한다`() {
        assertFalse(ProfileDocumentSheetSkipPartiallyExpanded)
    }

    @Test
    fun `프로필 시트는 iOS NavigationView 처럼 large title 을 사용한다`() {
        assertEquals(true, ProfileSheetUsesLargeNavigationTitle)
    }

    @Test
    fun `로그아웃 행은 iOS처럼 내 정보 섹션과 분리한다`() {
        assertEquals(10.dp, ProfileInfoToLogoutSectionSpacing)
        assertEquals(16.dp, ProfileLogoutToSyncSectionSpacing)
    }

    @Test
    fun `내 정보 섹션은 iOS ProfileView처럼 컴팩트 묶음 간격을 사용한다`() {
        assertEquals(12.dp, ProfileInfoSectionVerticalPadding)
        assertEquals(8.dp, ProfileInfoRowSpacing)
        assertEquals(2.dp, ProfileInfoDividerVerticalPadding)
        assertEquals(16.dp, ProfileInfoValueLeadingSpacing)
    }

    @Test
    fun `프로필 가입일은 iOS DateFormatter long date style 을 사용한다`() {
        val timestamp = 1_700_000_000_000L

        assertEquals(
            DateFormat.getDateInstance(DateFormat.LONG, Locale.KOREA).format(Date(timestamp)),
            formatProfileJoinDate(timestamp, Locale.KOREA),
        )
        assertEquals(
            DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(Date(timestamp)),
            formatProfileJoinDate(timestamp, Locale.US),
        )
    }

    @Test
    fun `프로필 동기화 시간은 iOS localizedDateTime 처럼 medium date 와 short time 을 사용한다`() {
        val timestamp = 1_700_000_000_000L

        assertEquals(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.KOREA).format(Date(timestamp)),
            formatProfileSyncDateTime(timestamp, Locale.KOREA),
        )
        assertEquals(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(Date(timestamp)),
            formatProfileSyncDateTime(timestamp, Locale.US),
        )
    }

    @Test
    fun `계정 관리 섹션은 iOS ProfileView처럼 로그인 상태와 무관하게 표시한다`() {
        assertEquals(true, shouldShowProfileAccountSection(isLoggedIn = true))
        assertEquals(true, shouldShowProfileAccountSection(isLoggedIn = false))
    }

    @Test
    fun `프로필 화면 섹션 순서는 iOS ProfileView 를 따른다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()

        assertSourceOrder(
            source,
            listOf(
                "R.string.profile_section_my_info",
                "R.string.profile_account_logout",
                "R.string.profile_section_sync",
                "R.string.profile_sync_cloud",
                "R.string.profile_backup_manual",
                "R.string.profile_section_terms",
                "R.string.profile_terms_service",
                "R.string.profile_terms_privacy",
                "R.string.profile_section_account",
                "R.string.profile_account_delete",
            ),
        )
    }

    @Test
    fun `프로필 동기화 토글 행은 iOS처럼 headline caption progress switch 구조를 유지한다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()

        assertSourceOrder(
            source,
            listOf(
                "ProfileCloudSyncToggleItem(",
                "title = stringResource(R.string.profile_sync_cloud)",
                "subtitle = stringResource(syncStatus.labelRes)",
                "subtitleColor = syncStatus.color",
                "showProgress = shouldShowProfileSyncProgress(isSyncing)",
                "onCheckedChange = { viewModel.setCloudBackupEnabled(it) }",
            ),
        )

        val toggleItemSource = source.substring(source.indexOf("private fun ProfileCloudSyncToggleItem"))
        assertSourceOrder(
            toggleItemSource,
            listOf(
                "style = MaterialTheme.typography.bodyLarge",
                "fontWeight = FontWeight.SemiBold",
                "style = MaterialTheme.typography.bodySmall",
                "color = subtitleColor",
                "if (showProgress)",
                "CircularProgressIndicator",
                "Switch(",
            ),
        )
    }

    @Test
    fun `프로필 마지막 동기화 문구는 iOS처럼 실시간 동기화 백업 기록없음 순서로 고른다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()
        val lastSyncSource = source.substring(source.indexOf("val lastSyncDisplay = when"))

        assertSourceOrder(
            lastSyncSource,
            listOf(
                "lastRealtimeSyncTime.hasSyncTimestamp()",
                "R.string.profile_sync_last_sync",
                "lastBackupTime.hasSyncTimestamp()",
                "R.string.profile_backup_last_backup",
                "R.string.profile_sync_no_history",
            ),
        )
    }

    @Test
    fun `프로필 수동 백업과 동기화 footer 조건은 iOS ProfileView 를 따른다`() {
        assertTrue(shouldShowProfileManualBackup(isLoggedIn = true, isCloudBackupEnabled = true))
        assertFalse(shouldShowProfileManualBackup(isLoggedIn = false, isCloudBackupEnabled = true))
        assertFalse(shouldShowProfileManualBackup(isLoggedIn = true, isCloudBackupEnabled = false))

        assertEquals(
            R.string.profile_sync_footer_login_required,
            profileSyncFooterTextRes(isLoggedIn = false, isCloudBackupEnabled = true),
        )
        assertEquals(
            R.string.profile_sync_footer_login_required,
            profileSyncFooterTextRes(isLoggedIn = false, isCloudBackupEnabled = false),
        )
        assertEquals(
            R.string.profile_sync_footer_enable_info,
            profileSyncFooterTextRes(isLoggedIn = true, isCloudBackupEnabled = false),
        )
        assertEquals(null, profileSyncFooterTextRes(isLoggedIn = true, isCloudBackupEnabled = true))
    }

    @Test
    fun `프로필 동기화 섹션은 iOS처럼 마지막 동기화 수동 백업 footer 순서로 배치한다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()
        val syncSectionSource = source.substring(
            source.indexOf("// ===== 2. 동기화 섹션 ====="),
            source.indexOf("// ===== 3. 약관 및 정책 섹션 ====="),
        )
        val footerHelperSource = source.substring(source.indexOf("internal fun profileSyncFooterTextRes"))

        assertSourceOrder(
            syncSectionSource,
            listOf(
                "ProfileCloudSyncToggleItem(",
                "val lastSyncDisplay = when",
                "Text(",
                "text = lastSyncDisplay",
                "if (shouldShowProfileManualBackup(isLoggedIn, isCloudBackupEnabled))",
                "R.string.profile_backup_manual",
                "val syncFooterTextRes = profileSyncFooterTextRes(isLoggedIn, isCloudBackupEnabled)",
            ),
        )
        assertSourceOrder(
            footerHelperSource,
            listOf(
                "R.string.profile_sync_footer_login_required",
                "R.string.profile_sync_footer_enable_info",
            ),
        )
    }

    @Test
    fun `프로필 약관 행은 iOS처럼 chevron 행으로 약관 개인정보 시트를 연다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()
        val termsSectionSource = source.substring(
            source.indexOf("// ===== 3. 약관 및 정책 섹션 ====="),
            source.indexOf("// ===== 4. 계정 관리 섹션"),
        )
        val documentSheetSource = source.substring(source.indexOf("// 약관/개인정보 두 번째 시트"))

        assertSourceOrder(
            termsSectionSource,
            listOf(
                "R.string.profile_section_terms",
                "R.string.profile_terms_service",
                "webDocTarget = WebDocTarget.Terms",
                "showArrow = true",
                "HorizontalDivider",
                "R.string.profile_terms_privacy",
                "webDocTarget = WebDocTarget.Privacy",
                "showArrow = true",
            ),
        )
        assertSourceOrder(
            documentSheetSource,
            listOf(
                "webDocTarget?.let { target ->",
                "ModalBottomSheet(",
                "onDismissRequest = { webDocTarget = null }",
                "skipPartiallyExpanded = ProfileDocumentSheetSkipPartiallyExpanded",
                "WebDocTarget.Terms -> TermsOfServiceScreen(onDismiss = { webDocTarget = null })",
                "WebDocTarget.Privacy -> PrivacyPolicyScreen(onDismiss = { webDocTarget = null })",
            ),
        )
    }

    @Test
    fun `프로필 로그아웃 알림은 iOS처럼 취소와 destructive 로그아웃을 제공한다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()
        val logoutDialogSource = source.substring(
            source.indexOf("// 로그아웃 확인 다이얼로그"),
            source.indexOf("// 계정 삭제 1차 다이얼로그"),
        )

        assertSourceOrder(
            logoutDialogSource,
            listOf(
                "AlertDialog(",
                "onDismissRequest = { showLogoutDialog = false }",
                "R.string.profile_account_logout",
                "R.string.profile_logout_message",
                "confirmButton",
                "viewModel.signOut",
                "onAccountSessionEnded()",
                "onDismiss()",
                "MaterialTheme.colorScheme.error",
                "dismissButton",
                "R.string.common_cancel",
            ),
        )
    }

    @Test
    fun `프로필 탈퇴 흐름은 iOS처럼 1차 확인 후 최종 확인에서 deleteAccount 를 호출한다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()
        val deleteDialogSource = source.substring(source.indexOf("// 계정 삭제 1차 다이얼로그"))

        assertTrue(deleteDialogSource.contains("R.string.profile_delete_account_button"))
        assertTrue(deleteDialogSource.contains("R.string.profile_delete_account_final_button"))
        assertSourceOrder(
            deleteDialogSource,
            listOf(
                "R.string.profile_account_delete",
                "R.string.profile_delete_account_message",
                "showDeleteFinalDialog = true",
                "R.string.profile_delete_account_final_title",
                "R.string.profile_delete_account_final_message",
                "viewModel.deleteAccount",
            ),
        )
    }

    @Test
    fun `프로필 결과 알림은 iOS처럼 확인 버튼 하나로 동기화 결과와 탈퇴 실패를 닫는다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt",
        ).readText()
        val syncResultSource = source.substring(
            source.indexOf("// 동기화 결과 알림"),
            source.indexOf("Scaffold("),
        )
        val deleteFinalSource = source.substring(
            source.indexOf("// 계정 삭제 2차 최종 확인"),
            source.indexOf("resultDialog?.let { dialog ->"),
        )
        val resultDialogSource = source.substring(
            source.indexOf("resultDialog?.let { dialog ->"),
            source.indexOf("// 약관/개인정보 두 번째 시트"),
        )

        assertSourceOrder(
            syncResultSource,
            listOf(
                "viewModel.syncResultMessage.collect",
                "ProfileResultDialog(",
                "titleRes = R.string.profile_sync_alert_title",
            ),
        )
        assertSourceOrder(
            deleteFinalSource,
            listOf(
                "viewModel.deleteAccount",
                "ProfileResultDialog(",
                "titleRes = R.string.profile_delete_account_error_title",
                "message = message",
            ),
        )
        assertSourceOrder(
            resultDialogSource,
            listOf(
                "AlertDialog(",
                "onDismissRequest = { resultDialog = null }",
                "title = { Text(stringResource(dialog.titleRes)) }",
                "text = { Text(dialog.message) }",
                "R.string.common_confirm",
            ),
        )
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Project file not found. Tried: ${candidates.joinToString()}")
    }

    private fun assertSourceOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        tokens.forEach { token ->
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue("Missing token: $token", index >= 0)
            assertTrue("Token out of order: $token", index > previousIndex)
            previousIndex = index
        }
    }
}
