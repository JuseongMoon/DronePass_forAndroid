package com.ScienceFiction.DronePassAndroid.feature.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.buildAccountSwitchLocalChangeState
import com.ScienceFiction.DronePassAndroid.core.data.sync.countAccountSwitchShapeBaselineChanges
import com.ScienceFiction.DronePassAndroid.core.data.sync.decodeAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.encodeAccountSwitchShapeBaseline
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
    fun `Apple login web cancellation is ignored like iOS user cancelled flow`() {
        assertEquals(
            true,
            isAppleSignInCancellationErrorCode("ERROR_WEB_CONTEXT_CANCELED"),
        )
        assertEquals(
            true,
            isAppleSignInCancellationErrorCode("ERROR_WEB_CONTEXT_CANCELLED"),
        )
        assertEquals(
            false,
            isAppleSignInCancellationErrorCode("ERROR_WEB_NETWORK_REQUEST_FAILED"),
        )
        assertEquals(
            false,
            shouldSuppressAppleSignInFailure(IllegalStateException("network")),
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
    fun `account switch warning counts only shape changes since iOS baseline`() {
        val state = buildAccountSwitchLocalChangeState(
            currentShapeUpdatedAtById = mapOf(
                "same" to 100,
                "modified" to 2_500,
                "added" to 300,
            ),
            syncedShapeBaseline = mapOf(
                "same" to 100,
                "modified" to 1_000,
                "removed" to 700,
            ),
            sketchCount = 0,
            lastLocalSketchModificationTime = null,
            lastSketchSyncTime = null,
        )

        assertEquals(true, state.hasUnsyncedLocalChanges)
        assertEquals(3, state.atRiskCount)
    }

    @Test
    fun `account switch warning still includes dirty sketches because Android can lose them too`() {
        val state = buildAccountSwitchLocalChangeState(
            currentShapeUpdatedAtById = mapOf("same" to 100),
            syncedShapeBaseline = mapOf("same" to 100),
            sketchCount = 3,
            lastLocalSketchModificationTime = 200,
            lastSketchSyncTime = 100,
        )

        assertEquals(true, state.hasUnsyncedLocalChanges)
        assertEquals(3, state.atRiskCount)
    }

    @Test
    fun `account switch warning includes dirty drones because Android clears them too`() {
        val state = buildAccountSwitchLocalChangeState(
            currentShapeUpdatedAtById = mapOf("same" to 100),
            syncedShapeBaseline = mapOf("same" to 100),
            droneCount = 2,
            lastLocalDroneModificationTime = 200,
            lastSyncTime = 100,
            sketchCount = 0,
            lastLocalSketchModificationTime = null,
            lastSketchSyncTime = null,
        )

        assertEquals(true, state.hasUnsyncedLocalChanges)
        assertEquals(2, state.atRiskCount)
    }

    @Test
    fun `sync preference keys use iOS UserDefaults names`() {
        assertEquals("lastSyncTime", SyncPreferenceKeys.LAST_SYNC_TIME.name)
        assertEquals("lastLocalModificationTime", SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME.name)
        assertEquals("lastLocalDroneModificationTime", SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME.name)
        assertEquals("lastSketchSyncTime", SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME.name)
        assertEquals(
            "lastLocalSketchModificationTime",
            SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME.name,
        )
        assertEquals("syncedShapeBaseline", SyncPreferenceKeys.SYNCED_SHAPE_BASELINE.name)
    }

    @Test
    fun `clean local data does not ask account switch confirmation even when items exist`() {
        val state = buildAccountSwitchLocalChangeState(
            currentShapeUpdatedAtById = mapOf(
                "shape-a" to 100,
                "shape-b" to 200,
            ),
            syncedShapeBaseline = mapOf(
                "shape-a" to 100,
                "shape-b" to 200,
            ),
            droneCount = 2,
            lastLocalDroneModificationTime = 100,
            lastSyncTime = 200,
            sketchCount = 3,
            lastLocalSketchModificationTime = 100,
            lastSketchSyncTime = 200,
        )

        assertEquals(false, state.hasUnsyncedLocalChanges)
        assertEquals(0, state.atRiskCount)
        assertEquals(
            false,
            shouldRequestAccountSwitchConfirmation(state.hasUnsyncedLocalChanges),
        )
    }

    @Test
    fun `account switch shape baseline missing falls back to current active shape count`() {
        assertEquals(
            2,
            countAccountSwitchShapeBaselineChanges(
                currentShapeUpdatedAtById = mapOf(
                    "shape-a" to 100,
                    "shape-b" to 200,
                ),
                syncedShapeBaseline = null,
            ),
        )
    }

    @Test
    fun `account switch shape baseline ignores timestamp differences within iOS one second tolerance`() {
        assertEquals(
            0,
            countAccountSwitchShapeBaselineChanges(
                currentShapeUpdatedAtById = mapOf("shape-a" to 1_900),
                syncedShapeBaseline = mapOf("shape-a" to 1_000),
            ),
        )
        assertEquals(
            1,
            countAccountSwitchShapeBaselineChanges(
                currentShapeUpdatedAtById = mapOf("shape-a" to 2_001),
                syncedShapeBaseline = mapOf("shape-a" to 1_000),
            ),
        )
    }

    @Test
    fun `account switch shape baseline uses iOS UserDefaults json shape`() {
        val encoded = encodeAccountSwitchShapeBaseline(
            mapOf(
                "shape-a" to 100,
                "shape-b" to 200,
            ),
        )

        assertEquals(
            mapOf(
                "shape-a" to 100L,
                "shape-b" to 200L,
            ),
            decodeAccountSwitchShapeBaseline(encoded),
        )
        assertEquals(null, decodeAccountSwitchShapeBaseline(""))
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
        assertEquals(600, LoginTabletBreakpointDp)
        assertEquals(0.dp, LoginScreenHorizontalPadding)
        assertEquals(24.dp, LoginButtonHorizontalPadding)
        assertEquals(24.dp, LoginVerticalPaddingPhone)
        assertEquals(32.dp, LoginVerticalPaddingTablet)
        assertEquals(40.dp, LoginTopSpacerPhone)
        assertEquals(72.dp, LoginTopSpacerTablet)
        assertEquals(24.dp, LoginTermsBottomPaddingPhone)
        assertEquals(40.dp, LoginTermsBottomPaddingTablet)
        assertEquals(24.dp, resolveLoginVerticalPadding(isTablet = false))
        assertEquals(32.dp, resolveLoginVerticalPadding(isTablet = true))
        assertEquals(40.dp, resolveLoginTopSpacer(isTablet = false))
        assertEquals(72.dp, resolveLoginTopSpacer(isTablet = true))
        assertEquals(24.dp, resolveLoginTermsBottomPadding(isTablet = false))
        assertEquals(40.dp, resolveLoginTermsBottomPadding(isTablet = true))
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
        assertEquals(2.dp, LoginTermsLineSpacing)
    }
}
