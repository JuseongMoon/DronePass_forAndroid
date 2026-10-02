package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.preferencesOf
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncState
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileViewModelTest {

    @Test
    fun `탈퇴 공급자 인증 오류만 재로그인 안내를 사용한다`() {
        listOf(
            AccountDeletionFailureReason.NOT_AUTHENTICATED,
            AccountDeletionFailureReason.APPLE_REAUTHENTICATION_FAILED,
            AccountDeletionFailureReason.APPLE_ACCESS_TOKEN_MISSING,
            AccountDeletionFailureReason.APPLE_TOKEN_REVOCATION_FAILED,
        ).forEach { reason ->
            assertTrue(accountDeletionRequiresProviderAuthentication(AccountDeletionException(reason)))
        }
        assertTrue(
            !accountDeletionRequiresProviderAuthentication(
                AccountDeletionException(AccountDeletionFailureReason.SERVER_REQUEST_FAILED),
            ),
        )
        assertTrue(!accountDeletionRequiresProviderAuthentication(IllegalStateException("network")))
    }

    @Test
    fun `마지막 백업 시간은 iOS 키를 우선하고 Android 레거시 키를 fallback으로 읽는다`() {
        assertEquals("lastBackupTime", ProfilePreferenceKeys.LAST_BACKUP_TIME.name)
        assertEquals("last_backup_time", ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME.name)
        assertEquals(
            123L,
            storedLastBackupTime(
                preferencesOf(
                    ProfilePreferenceKeys.LAST_BACKUP_TIME to 123L,
                    ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME to 456L,
                ),
            ),
        )
        assertEquals(
            456L,
            storedLastBackupTime(
                preferencesOf(ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME to 456L),
            ),
        )
        assertEquals(null, storedLastBackupTime(preferencesOf()))
    }

    @Test
    fun `프로필 클라우드 동기화 행은 iOS처럼 동기화 중에 진행 표시를 보여준다`() {
        assertTrue(shouldShowProfileSyncProgress(isSyncing = true))
        assertTrue(!shouldShowProfileSyncProgress(isSyncing = false))
    }

    @Test
    fun `프로필 수동 백업은 iOS처럼 동기화 중에 비활성화된다`() {
        assertTrue(!shouldEnableProfileManualBackup(isSyncing = true))
        assertTrue(shouldEnableProfileManualBackup(isSyncing = false))
    }

    @Test
    fun `프로필 동기화 진행 상태는 iOS처럼 수동 또는 실시간 Shape 동기화를 포함한다`() {
        assertTrue(
            isProfileSyncInProgress(
                manualSyncing = true,
                realtimeSyncState = SyncState.Idle,
            ),
        )
        assertTrue(
            isProfileSyncInProgress(
                manualSyncing = false,
                realtimeSyncState = SyncState.Syncing,
            ),
        )
        assertTrue(
            !isProfileSyncInProgress(
                manualSyncing = false,
                realtimeSyncState = SyncState.Success(timestamp = 1_700_000_000_000L),
            ),
        )
    }

    @Test
    fun `프로필 동기화 상태 문구와 색상은 iOS realtimeCloudSyncStatusText 를 따른다`() {
        assertEquals(R.string.profile_sync_in_progress, ProfileSyncStatus.Syncing.labelRes)
        assertEquals(Color(0xFF007AFF), ProfileSyncStatus.Syncing.color)

        assertEquals(R.string.profile_sync_login_required, ProfileSyncStatus.LoginRequired.labelRes)
        assertEquals(Color(0xFFFF9500), ProfileSyncStatus.LoginRequired.color)

        assertEquals(R.string.profile_sync_import_pending, ProfileSyncStatus.ImportPending.labelRes)
        assertEquals(Color(0xFFFF9500), ProfileSyncStatus.ImportPending.color)

        assertEquals(R.string.profile_sync_active, ProfileSyncStatus.Active.labelRes)
        assertEquals(Color(0xFF34C759), ProfileSyncStatus.Active.color)

        assertEquals(R.string.profile_sync_waiting, ProfileSyncStatus.Waiting.labelRes)
        assertEquals(Color(0xFFFF9500), ProfileSyncStatus.Waiting.color)
    }

    @Test
    fun `프로필 클라우드 동기화는 iOS처럼 수동 또는 실시간 동기화 중이면 시작하지 않는다`() {
        assertTrue(
            shouldStartProfileCloudSync(
                manualSyncing = false,
                realtimeSyncState = SyncState.Idle,
            )
        )
        assertTrue(
            !shouldStartProfileCloudSync(
                manualSyncing = true,
                realtimeSyncState = SyncState.Idle,
            )
        )
        assertTrue(
            !shouldStartProfileCloudSync(
                manualSyncing = false,
                realtimeSyncState = SyncState.Syncing,
            )
        )
        assertTrue(
            shouldStartProfileCloudSync(
                manualSyncing = false,
                realtimeSyncState = SyncState.Success(timestamp = 1_700_000_000_000L),
            )
        )
        assertTrue(
            shouldStartProfileCloudSync(
                manualSyncing = false,
                realtimeSyncState = SyncState.Error(message = "network"),
            )
        )
    }

    @Test
    fun `프로필 계정 작업은 iOS처럼 진행 중에 비활성화된다`() {
        assertTrue(!shouldEnableProfileAccountAction(isAccountActionInProgress = true))
        assertTrue(shouldEnableProfileAccountAction(isAccountActionInProgress = false))
    }

    @Test
    fun `프로필 오류 문구는 iOS처럼 null일 때만 fallback을 사용한다`() {
        assertEquals("message", profileErrorDescription("message", fallback = "fallback"))
        assertEquals("", profileErrorDescription("", fallback = "fallback"))
        assertEquals("   ", profileErrorDescription("   ", fallback = "fallback"))
        assertEquals("fallback", profileErrorDescription(null, fallback = "fallback"))
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
            assertTrue("Missing token after $previousIndex: $token", index >= 0)
            previousIndex = index
        }
    }

    @Test
    fun `프로필 가입일은 iOS처럼 Firebase 생성 시각이 있을 때만 표시한다`() {
        assertEquals(1_700_000_000_000L, normalizeProfileJoinDateMillis(1_700_000_000_000L))
        assertEquals(null, normalizeProfileJoinDateMillis(0L))
        assertEquals(null, normalizeProfileJoinDateMillis(null))
    }

    @Test
    fun `프로필 로그인 방식은 iOS처럼 Apple Google 순서로 판별한다`() {
        assertEquals(
            ProfileLoginProvider.APPLE,
            resolveProfileLoginProvider(listOf("firebase", "google.com", "apple.com")),
        )
        assertEquals(
            ProfileLoginProvider.GOOGLE,
            resolveProfileLoginProvider(listOf("firebase", "google.com")),
        )
        assertEquals(
            ProfileLoginProvider.UNKNOWN,
            resolveProfileLoginProvider(listOf("firebase", "password")),
        )
        assertEquals(ProfileLoginProvider.UNKNOWN, resolveProfileLoginProvider(emptyList()))
    }

    @Test
    fun `프로필 만료 도형 수는 종료일이 현재와 같아도 센다`() {
        val shapes = listOf(
            ShapeModel(flightEndDate = 999L),
            ShapeModel(flightEndDate = 1_000L),
            ShapeModel(flightEndDate = 1_001L),
            ShapeModel(flightEndDate = null),
        )

        assertEquals(2, countExpiredProfileShapes(shapes, now = 1_000L))
    }

    @Test
    fun `프로필 동기화 성공 개수는 iOS처럼 동기화 전 로컬 활성 도형 snapshot 기준이다`() {
        val activeLocalShapesBeforeSync = listOf(
            ShapeModel(id = "shape-1"),
            ShapeModel(id = "shape-2"),
        )

        assertEquals(2, profileSyncSuccessCount(activeLocalShapesBeforeSync))
    }


    @Test
    fun `동기화 상태는 토글 없이 로그인과 가져오기 대기와 리스너로 정한다`() {
        assertEquals(ProfileSyncStatus.Syncing, resolveProfileSyncStatus(true, true, false, true))
        assertEquals(ProfileSyncStatus.LoginRequired, resolveProfileSyncStatus(false, false, false, false))
        assertEquals(ProfileSyncStatus.ImportPending, resolveProfileSyncStatus(false, true, true, false))
        assertEquals(ProfileSyncStatus.Active, resolveProfileSyncStatus(false, true, false, true))
        assertEquals(ProfileSyncStatus.Waiting, resolveProfileSyncStatus(false, true, false, false))
    }

    @Test
    fun `프로필 로그아웃과 탈퇴는 화면에 묶이지 않는 기기 데이터 흐름에 맡긴다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileViewModel.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/profile/ProfileViewModel.kt",
        ).readText()

        assertTrue(source.contains("accountSessionFlows.logout(proceedWithoutUpload)"))
        assertTrue(source.contains("accountSessionFlows.deleteAccount(activity)"))
        // 화면 범위에서 직접 로그아웃·삭제하지 않는다(화면을 닫으면 중간에 끊긴다).
        assertTrue(!source.contains("firebaseAuth.signOut()"))
        assertTrue(!source.contains("realtimeSyncManager.forceSyncNow()\n                    saveProfileSyncedShapeBaseline"))
    }
}
