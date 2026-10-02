package com.ScienceFiction.DronePassAndroid.feature.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.sync.encodeAccountSwitchShapeBaseline
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelForegroundSyncTest {

    @Test
    fun `foreground resume requests confirmation only when the sync gate is open and realtime listener is off`() {
        assertEquals(
            ForegroundCloudSyncAction.REQUEST_USER_CONFIRMATION,
            resolveForegroundCloudSyncAction(
                syncGateOpen = true,
                realtimeSyncEnabled = false,
            ),
        )
    }

    @Test
    fun `foreground resume does nothing when realtime listener is already active`() {
        assertEquals(
            ForegroundCloudSyncAction.NO_OP,
            resolveForegroundCloudSyncAction(
                syncGateOpen = true,
                realtimeSyncEnabled = true,
            ),
        )
    }

    @Test
    fun `foreground resume does nothing while the sync gate is closed`() {
        // 비로그인·가져오기 확인 대기·로그아웃 중에는 대체 동기화 경로도 열리지 않는다.
        assertEquals(
            ForegroundCloudSyncAction.NO_OP,
            resolveForegroundCloudSyncAction(
                syncGateOpen = false,
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
    fun `foreground confirmation follows iOS loading then complete or error alert sequence`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
        ).readText()
        val functionBody = source.substringAfter("fun confirmForegroundCloudSync()")
            .substringBefore("/**\n     * Google Sign-In 실행")

        assertAppearsInOrder(
            source = functionBody,
            tokens = listOf(
                "if (isForegroundSyncing) return@launch",
                "isForegroundSyncing = true",
                "realtimeSyncManager.startListening(ticket)",
                "ForegroundSyncDialogState.Loading",
                "performForegroundCloudSync(ticket)",
                "FullSyncResult.Success",
                "ForegroundSyncDialogState.Complete",
                "is FullSyncResult.Failure",
                "ForegroundSyncDialogState.Error(result.message)",
                "isForegroundSyncing = false",
            ),
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
    fun `foreground resume marks check complete only after remote check succeeds like iOS`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
        ).readText()
        val functionBody = source.substringAfter("fun ensureCloudSyncActiveOnForeground()")
            .substringBefore("fun resetForegroundSyncCheckStatus()")

        assertAppearsInOrder(
            source = functionBody,
            tokens = listOf(
                "resolveForegroundCloudSyncAction",
                "syncGateOpen = accountSession.syncTicket() != null",
                "realtimeSyncManager.isRealtimeSyncEnabled.value",
                "ForegroundCloudSyncAction.REQUEST_USER_CONFIRMATION",
                "shouldCheckForegroundRemoteChanges",
                "lastForegroundRemoteChangeCheckTimeMillis = nowMillis",
                "realtimeSyncManager.hasForegroundShapeRemoteChanges()",
                ".onFailure",
                "if (remoteChangesResult.isSuccess)",
                "hasCheckedForegroundRemoteChanges = true",
                "remoteChangesResult.getOrDefault(false)",
                "_foregroundSyncConfirmation.tryEmit(Unit)",
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
    fun `Apple login cancellation suppression uses Firebase web errorCode`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/auth/AuthViewModel.kt",
        ).readText()
        val functionBody = source.substringAfter("internal fun shouldSuppressAppleSignInFailure")
            .substringBefore("internal fun resolveForegroundCloudSyncAction")

        assertTrue(functionBody.contains("exception !is FirebaseAuthWebException"))
        assertTrue(functionBody.contains("isAppleSignInCancellationErrorCode(exception.errorCode)"))
        assertFalse(functionBody.contains("ERROR_WEB_NETWORK_REQUEST_FAILED"))
        assertFalse(functionBody.contains("localizedMessage"))
    }

    @Test
    fun `login error dialog preserves empty and whitespace messages like iOS localizedDescription`() {
        assertEquals("", loginErrorDialogText(""))
        assertEquals("   ", loginErrorDialogText("   "))
        assertEquals("Firebase 인증 실패", loginErrorDialogText("Firebase 인증 실패"))
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Project file not found. Tried: ${candidates.joinToString()}")
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in AuthViewModel.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
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
    fun `synced shape baseline uses iOS UserDefaults json shape`() {
        val encoded = encodeAccountSwitchShapeBaseline(
            mapOf(
                "shape-b" to 200,
                "shape-a" to 100,
            ),
        )

        assertEquals("{\"shape-a\":100, \"shape-b\":200}", encoded)
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
        assertEquals(16.dp, LoginGoogleButtonTopSpacing) // iOS 실측
        assertEquals(3.dp, LoginGoogleButtonShadowElevation)
        assertEquals(8.dp, LoginTermsTopSpacing)
        assertEquals(16.dp, LoginSkipButtonTopSpacing)
        assertEquals(0.dp, LoginTermsLineSpacing) // iOS VStack(spacing: 0)
    }
}
