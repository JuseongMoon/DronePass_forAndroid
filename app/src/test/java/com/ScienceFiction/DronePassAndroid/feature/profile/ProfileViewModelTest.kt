package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.datastore.preferences.core.preferencesOf
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileViewModelTest {

    @Test
    fun `탈퇴 시 iOS 사용자 데이터와 Android FCM devices 컬렉션을 삭제 대상으로 포함한다`() {
        assertEquals(
            listOf("shapes", "drones", "sketches", "metadata", "devices"),
            FIRESTORE_USER_SUBCOLLECTIONS_TO_DELETE,
        )
    }

    @Test
    fun `삭제 대상 컬렉션은 중복이 없다`() {
        assertTrue(
            FIRESTORE_USER_SUBCOLLECTIONS_TO_DELETE.toSet().size ==
                FIRESTORE_USER_SUBCOLLECTIONS_TO_DELETE.size
        )
    }

    @Test
    fun `탈퇴 Firestore 문서 삭제는 iOS처럼 500개 단위로 batch 분할한다`() {
        val documentIds = (1..501).map { "doc-$it" }

        val chunks = chunkFirestoreDocumentIdsForBatchDelete(documentIds)

        assertEquals(listOf(500, 1), chunks.map { it.size })
        assertEquals("doc-1", chunks.first().first())
        assertEquals("doc-501", chunks.last().single())
    }

    @Test
    fun `탈퇴 Firestore 데이터 삭제는 iOS처럼 최대 3회까지 시도한다`() {
        assertEquals(3, FIRESTORE_USER_DELETE_MAX_ATTEMPTS)
        assertTrue(shouldRetryFirestoreUserDelete(completedAttempts = 0))
        assertTrue(shouldRetryFirestoreUserDelete(completedAttempts = 2))
        assertTrue(!shouldRetryFirestoreUserDelete(completedAttempts = 3))
    }

    @Test
    fun `탈퇴 Firestore 데이터 삭제가 최종 실패해도 iOS처럼 Auth 계정 삭제는 계속 진행한다`() {
        assertTrue(shouldContinueAccountDeletionAfterFirestoreDeleteFailure())
    }

    @Test
    fun `탈퇴 시 iOS처럼 클라우드 동기화 추적 키를 정리한다`() {
        assertEquals(
            listOf(
                SyncPreferenceKeys.LAST_SYNC_TIME,
                SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME,
                SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME,
                SyncPreferenceKeys.SYNCED_SHAPE_BASELINE,
                SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME,
                SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME,
            ),
            ACCOUNT_DELETION_SYNC_PREFERENCE_KEYS_TO_CLEAR,
        )
    }

    @Test
    fun `클라우드 백업 설정은 iOS 키를 우선하고 Android 레거시 키를 fallback으로 읽는다`() {
        assertEquals("cloudBackupEnabled", ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED.name)
        assertEquals("cloud_backup_enabled", ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED.name)
        assertTrue(
            storedCloudBackupEnabled(
                preferencesOf(ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED to true),
            ),
        )
        assertTrue(
            storedCloudBackupEnabled(
                preferencesOf(ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED to true),
            ),
        )
        assertTrue(
            !storedCloudBackupEnabled(
                preferencesOf(
                    ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED to false,
                    ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED to true,
                ),
            ),
        )
        assertTrue(!storedCloudBackupEnabled(preferencesOf()))
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
    fun `프로필 계정 작업은 iOS처럼 진행 중에 비활성화된다`() {
        assertTrue(!shouldEnableProfileAccountAction(isAccountActionInProgress = true))
        assertTrue(shouldEnableProfileAccountAction(isAccountActionInProgress = false))
    }

    @Test
    fun `클라우드 동기화 토글 ON은 iOS처럼 로그인 상태에서 백업 결과 알림을 표시한다`() {
        assertTrue(
            shouldNotifyProfileSyncResultForCloudToggle(
                enabled = true,
                isLoggedIn = true,
            )
        )
        assertTrue(
            !shouldNotifyProfileSyncResultForCloudToggle(
                enabled = true,
                isLoggedIn = false,
            )
        )
        assertTrue(
            !shouldNotifyProfileSyncResultForCloudToggle(
                enabled = false,
                isLoggedIn = true,
            )
        )
    }

    @Test
    fun `프로필 오류 문구는 iOS처럼 null일 때만 fallback을 사용한다`() {
        assertEquals("message", profileErrorDescription("message", fallback = "fallback"))
        assertEquals("", profileErrorDescription("", fallback = "fallback"))
        assertEquals("   ", profileErrorDescription("   ", fallback = "fallback"))
        assertEquals("fallback", profileErrorDescription(null, fallback = "fallback"))
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
    fun `프로필 만료 도형 수는 iOS처럼 종료일이 현재보다 과거인 도형만 센다`() {
        val shapes = listOf(
            ShapeModel(flightEndDate = 999L),
            ShapeModel(flightEndDate = 1_000L),
            ShapeModel(flightEndDate = 1_001L),
            ShapeModel(flightEndDate = null),
        )

        assertEquals(1, countExpiredProfileShapes(shapes, now = 1_000L))
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
    fun `로그아웃 baseline은 iOS처럼 활성 도형 updatedAt만 저장한다`() {
        val baseline = buildProfileSyncedShapeBaseline(
            listOf(
                ShapeModel(id = "active", updatedAt = 100L, deletedAt = null),
                ShapeModel(id = "deleted", updatedAt = 200L, deletedAt = 300L),
            ),
        )

        assertEquals(mapOf("active" to 100L), baseline)
    }
}
