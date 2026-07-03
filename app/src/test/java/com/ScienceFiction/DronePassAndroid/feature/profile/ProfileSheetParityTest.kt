package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.compose.ui.unit.dp
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

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Project file not found. Tried: ${candidates.joinToString()}")
    }

    private fun assertSourceOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        tokens.forEach { token ->
            val index = source.indexOf(token)
            assertTrue("Missing token: $token", index >= 0)
            assertTrue("Token out of order: $token", index > previousIndex)
            previousIndex = index
        }
    }
}
