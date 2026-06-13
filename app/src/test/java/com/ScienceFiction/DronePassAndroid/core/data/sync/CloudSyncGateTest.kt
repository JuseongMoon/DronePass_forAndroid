package com.ScienceFiction.DronePassAndroid.core.data.sync

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncGateTest {

    @Test
    fun `클라우드 백업 저장 키는 프로필 토글과 동일하다`() {
        assertEquals("cloudBackupEnabled", CloudSyncPreferenceKeys.CLOUD_BACKUP_ENABLED.name)
        assertEquals("cloud_backup_enabled", CloudSyncPreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED.name)
    }

    @Test
    fun `클라우드 백업 저장값은 현재 키를 레거시 키보다 우선한다`() {
        assertTrue(
            storedCloudSyncEnabled(
                preferencesOf(CloudSyncPreferenceKeys.CLOUD_BACKUP_ENABLED to true),
            ),
        )
        assertTrue(
            storedCloudSyncEnabled(
                preferencesOf(CloudSyncPreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED to true),
            ),
        )
        assertFalse(
            storedCloudSyncEnabled(
                preferencesOf(
                    CloudSyncPreferenceKeys.CLOUD_BACKUP_ENABLED to false,
                    CloudSyncPreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED to true,
                ),
            ),
        )
        assertFalse(storedCloudSyncEnabled(preferencesOf()))
    }

    @Test
    fun `즉시 클라우드 푸시는 iOS처럼 로그인과 클라우드 백업 ON이 모두 필요하다`() {
        assertTrue(shouldRunImmediateCloudSync(isLoggedIn = true, cloudSyncEnabled = true))
        assertFalse(shouldRunImmediateCloudSync(isLoggedIn = true, cloudSyncEnabled = false))
        assertFalse(shouldRunImmediateCloudSync(isLoggedIn = false, cloudSyncEnabled = true))
        assertFalse(shouldRunImmediateCloudSync(isLoggedIn = false, cloudSyncEnabled = false))
    }
}
