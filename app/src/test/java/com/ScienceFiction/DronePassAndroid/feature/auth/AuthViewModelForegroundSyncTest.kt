package com.ScienceFiction.DronePassAndroid.feature.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.buildAccountSwitchLocalChangeState
import com.ScienceFiction.DronePassAndroid.core.data.sync.hasUnsyncedLocalChanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthViewModelForegroundSyncTest {

    @Test
    fun `foreground resume starts sync only when logged in cloud backup is enabled and realtime listener is off`() {
        assertEquals(
            ForegroundCloudSyncAction.START_REALTIME_AND_SYNC,
            resolveForegroundCloudSyncAction(
                isLoggedIn = true,
                cloudBackupEnabled = true,
                realtimeSyncEnabled = false,
            ),
        )
    }

    @Test
    fun `foreground resume does nothing when realtime listener is already active`() {
        assertEquals(
            ForegroundCloudSyncAction.NO_OP,
            resolveForegroundCloudSyncAction(
                isLoggedIn = true,
                cloudBackupEnabled = true,
                realtimeSyncEnabled = true,
            ),
        )
    }

    @Test
    fun `foreground resume does nothing when login or cloud backup condition is missing`() {
        assertEquals(
            ForegroundCloudSyncAction.NO_OP,
            resolveForegroundCloudSyncAction(
                isLoggedIn = false,
                cloudBackupEnabled = true,
                realtimeSyncEnabled = false,
            ),
        )
        assertEquals(
            ForegroundCloudSyncAction.NO_OP,
            resolveForegroundCloudSyncAction(
                isLoggedIn = true,
                cloudBackupEnabled = false,
                realtimeSyncEnabled = false,
            ),
        )
    }

    @Test
    fun `provider login starts only from logged out or error state like iOS retry flow`() {
        assertEquals(
            AuthProviderSignInAction.START,
            resolveAuthProviderSignInAction(AuthState.LoggedOut),
        )
        assertEquals(
            AuthProviderSignInAction.START,
            resolveAuthProviderSignInAction(AuthState.Error("failed")),
        )
    }

    @Test
    fun `provider login ignores duplicate attempts while loading`() {
        assertEquals(
            AuthProviderSignInAction.IGNORE,
            resolveAuthProviderSignInAction(AuthState.Loading),
        )
    }

    @Test
    fun `Google login cancellation is ignored like iOS user cancelled flow`() {
        assertEquals(
            true,
            shouldSuppressGoogleSignInFailure(GetCredentialCancellationException()),
        )
        assertEquals(
            false,
            shouldSuppressGoogleSignInFailure(IllegalStateException("network")),
        )
    }

    @Test
    fun `account switch resets local data before iOS style login sync`() {
        assertEquals(
            true,
            shouldResetLocalDataForAccountChange(AuthAccountChangeAction.RESET_LOCAL_DATA),
        )
        assertEquals(
            false,
            shouldResetLocalDataForAccountChange(AuthAccountChangeAction.KEEP_LOCAL_DATA),
        )
    }

    @Test
    fun `account switch asks confirmation only when local changes are unsynced like iOS`() {
        assertEquals(
            true,
            shouldRequestAccountSwitchConfirmation(hasUnsyncedLocalChanges = true),
        )
        assertEquals(
            false,
            shouldRequestAccountSwitchConfirmation(hasUnsyncedLocalChanges = false),
        )
    }

    @Test
    fun `clean account switch still resets local data before navigation like iOS`() {
        assertEquals(
            true,
            shouldPrepareAccountSwitchBeforeNavigation(AuthAccountChangeAction.RESET_LOCAL_DATA),
        )
        assertEquals(
            false,
            shouldPrepareAccountSwitchBeforeNavigation(AuthAccountChangeAction.KEEP_LOCAL_DATA),
        )
    }

    @Test
    fun `account switch dirty check compares local modification time with last sync time`() {
        assertEquals(
            true,
            hasUnsyncedLocalChanges(
                lastLocalModificationTime = 200,
                lastSyncTime = 100,
                localItemCount = 1,
            ),
        )
        assertEquals(
            false,
            hasUnsyncedLocalChanges(
                lastLocalModificationTime = 100,
                lastSyncTime = 200,
                localItemCount = 1,
            ),
        )
        assertEquals(
            false,
            hasUnsyncedLocalChanges(
                lastLocalModificationTime = 200,
                lastSyncTime = 100,
                localItemCount = 0,
            ),
        )
        assertEquals(
            false,
            hasUnsyncedLocalChanges(
                lastLocalModificationTime = null,
                lastSyncTime = null,
                localItemCount = 1,
            ),
        )
    }

    @Test
    fun `account switch warning counts shapes and sketches when either domain is dirty`() {
        val state = buildAccountSwitchLocalChangeState(
            shapeCount = 2,
            sketchCount = 3,
            lastLocalModificationTime = 200,
            lastSyncTime = 100,
            lastLocalSketchModificationTime = 50,
            lastSketchSyncTime = 100,
        )

        assertEquals(true, state.hasUnsyncedLocalChanges)
        assertEquals(5, state.atRiskCount)
    }

    @Test
    fun `sync preference keys use iOS UserDefaults names`() {
        assertEquals("lastSyncTime", SyncPreferenceKeys.LAST_SYNC_TIME.name)
        assertEquals("lastLocalModificationTime", SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME.name)
        assertEquals("lastSketchSyncTime", SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME.name)
        assertEquals(
            "lastLocalSketchModificationTime",
            SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME.name,
        )
    }

    @Test
    fun `clean local data does not ask account switch confirmation even when items exist`() {
        val state = buildAccountSwitchLocalChangeState(
            shapeCount = 2,
            sketchCount = 3,
            lastLocalModificationTime = 100,
            lastSyncTime = 200,
            lastLocalSketchModificationTime = 100,
            lastSketchSyncTime = 200,
        )

        assertEquals(false, state.hasUnsyncedLocalChanges)
        assertEquals(5, state.atRiskCount)
        assertEquals(
            false,
            shouldRequestAccountSwitchConfirmation(state.hasUnsyncedLocalChanges),
        )
    }

    @Test
    fun `login legal documents use iOS modal sheet presentation`() {
        assertEquals(
            LoginDocumentPresentation.Hidden,
            resolveLoginDocumentPresentation(null),
        )
        assertEquals(
            LoginDocumentPresentation.Sheet,
            resolveLoginDocumentPresentation(LoginDocTarget.Terms),
        )
        assertEquals(
            LoginDocumentPresentation.Sheet,
            resolveLoginDocumentPresentation(LoginDocTarget.Privacy),
        )
        assertFalse(LoginDocumentSheetSkipPartiallyExpanded)
    }

    @Test
    fun `login layout dimensions follow iOS LoginView tokens`() {
        assertEquals(32.dp, LoginScreenHorizontalPadding)
        assertEquals(200.dp, LoginLogoSize)
        assertEquals(24.dp, LoginLogoCornerRadius)
        assertEquals(32.dp, LoginLogoBottomSpacing)
        assertEquals(24.dp, LoginTitleBottomSpacing)
        assertEquals(22.dp, LoginDividerHorizontalPadding)
        assertEquals(10.dp, LoginDividerBottomSpacing)
        assertEquals(350.dp, LoginButtonMaxWidth)
        assertEquals(50.dp, LoginButtonHeight)
        assertEquals(12.dp, LoginButtonCornerRadius)
        assertEquals(18.dp, LoginProviderIconSize)
        assertEquals(8.dp, LoginProviderIconTextSpacing)
        assertEquals(19.sp, LoginProviderTextSize)
        assertEquals(8.dp, LoginGoogleButtonTopSpacing)
        assertEquals(8.dp, LoginTermsTopSpacing)
        assertEquals(16.dp, LoginSkipButtonTopSpacing)
        assertEquals(32.dp, LoginBottomSpacing)
        assertEquals(2.dp, LoginTermsLineSpacing)
    }
}
