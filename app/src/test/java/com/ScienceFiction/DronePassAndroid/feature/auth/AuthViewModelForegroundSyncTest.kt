package com.ScienceFiction.DronePassAndroid.feature.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.buildAccountSwitchLocalChangeState
import com.ScienceFiction.DronePassAndroid.core.data.sync.countAccountSwitchShapeBaselineChanges
import com.ScienceFiction.DronePassAndroid.core.data.sync.decodeAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.encodeAccountSwitchShapeBaseline
import com.ScienceFiction.DronePassAndroid.core.data.sync.hasUnsyncedLocalChanges
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelForegroundSyncTest {

    @Test
    fun `foreground resume requests confirmation only when logged in cloud backup is enabled and realtime listener is off`() {
        assertEquals(
            ForegroundCloudSyncAction.REQUEST_USER_CONFIRMATION,
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
    fun `foreground confirmation syncs only shape domain like iOS change detection prompt`() {
        assertEquals(
            listOf(ForegroundCloudSyncDomain.Shape),
            foregroundCloudSyncDomains(),
        )
    }

    @Test
    fun `foreground remote change check is skipped after a completed check in the same session`() {
        assertEquals(
            false,
            shouldCheckForegroundRemoteChanges(
                hasCheckedForChanges = true,
                isSyncing = false,
                nowMillis = 40_000L,
                lastCheckTimeMillis = 10_000L,
            ),
        )
    }

    @Test
    fun `foreground remote change check is skipped while sync is already running`() {
        assertEquals(
            false,
            shouldCheckForegroundRemoteChanges(
                hasCheckedForChanges = false,
                isSyncing = true,
                nowMillis = 40_000L,
                lastCheckTimeMillis = null,
            ),
        )
    }

    @Test
    fun `foreground remote change check follows iOS thirty second throttle`() {
        assertEquals(
            true,
            shouldCheckForegroundRemoteChanges(
                hasCheckedForChanges = false,
                isSyncing = false,
                nowMillis = 10_000L,
                lastCheckTimeMillis = null,
            ),
        )
        assertEquals(
            false,
            shouldCheckForegroundRemoteChanges(
                hasCheckedForChanges = false,
                isSyncing = false,
                nowMillis = 39_999L,
                lastCheckTimeMillis = 10_000L,
            ),
        )
        assertEquals(
            true,
            shouldCheckForegroundRemoteChanges(
                hasCheckedForChanges = false,
                isSyncing = false,
                nowMillis = 40_000L,
                lastCheckTimeMillis = 10_000L,
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
    fun `login screen keeps provider buttons visible but disables them while loading like iOS`() {
        assertEquals(true, areLoginProviderButtonsEnabled(AuthState.LoggedOut))
        assertEquals(true, areLoginProviderButtonsEnabled(AuthState.Error("failed")))
        assertEquals(false, areLoginProviderButtonsEnabled(AuthState.Loading))
    }

    @Test
    fun `로그아웃은 FCM 비활성화 완료 후 리스너 중단과 Firebase 로그아웃을 수행한다`() {
        assertEquals(
            listOf(
                AuthSignOutStep.DEACTIVATE_FCM_TOKEN,
                AuthSignOutStep.STOP_REALTIME_SYNC,
                AuthSignOutStep.SIGN_OUT,
            ),
            authSignOutSteps(),
        )
    }

    @Test
    fun `Auth 로그아웃은 Firestore FCM 비활성화 쓰기를 기다린다`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
        ).readText()

        assertTrue(source.contains("authSignOutSteps().forEach"))
        assertTrue(source.contains("FcmService.deactivateTokenAndWait(appContext)"))
    }

    @Test
    fun `Google login cancellation is ignored like iOS user cancelled flow`() {
        assertEquals(
            true,
            shouldSuppressGoogleSignInFailure(GetCredentialCancellationException()),
        )
        assertEquals(
            true,
            shouldSuppressGoogleSignInFailure(NoCredentialException()),
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
    fun `login error dialog preserves empty and whitespace messages like iOS localizedDescription`() {
        assertEquals("", loginErrorDialogText(""))
        assertEquals("   ", loginErrorDialogText("   "))
        assertEquals("Firebase 인증 실패", loginErrorDialogText("Firebase 인증 실패"))
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

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Project file not found. Tried: ${candidates.joinToString()}")
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
    fun `account switch resets local data before finalizing provider recovery keys like iOS`() {
        assertEquals(
            listOf(
                ProviderLoginPreparationStep.RESET_LOCAL_DATA,
                ProviderLoginPreparationStep.FINALIZE_SIGN_IN,
            ),
            resolveProviderLoginPreparationSteps(prepareAccountSwitchBeforeNavigation = true),
        )
        assertEquals(
            listOf(ProviderLoginPreparationStep.FINALIZE_SIGN_IN),
            resolveProviderLoginPreparationSteps(prepareAccountSwitchBeforeNavigation = false),
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
    fun `account switch malformed shape baseline falls back to current active shape count`() {
        val decodedBaseline = decodeAccountSwitchShapeBaseline("{\"shape-a\":not-a-timestamp}")
        val state = buildAccountSwitchLocalChangeState(
            currentShapeUpdatedAtById = mapOf(
                "shape-a" to 100,
                "shape-b" to 200,
            ),
            syncedShapeBaseline = decodedBaseline,
            sketchCount = 0,
            lastLocalSketchModificationTime = null,
            lastSketchSyncTime = null,
        )

        assertEquals(null, decodedBaseline)
        assertEquals(true, state.hasUnsyncedLocalChanges)
        assertEquals(2, state.atRiskCount)
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
        assertEquals(20.sp, LoginTitleTextSize)
        assertEquals(13.sp, LoginTermsTextSize)
        assertEquals(8.dp, LoginGoogleButtonTopSpacing)
        assertEquals(3.dp, LoginGoogleButtonShadowElevation)
        assertEquals(8.dp, LoginTermsTopSpacing)
        assertEquals(16.dp, LoginSkipButtonTopSpacing)
        assertEquals(2.dp, LoginTermsLineSpacing)
    }
}
