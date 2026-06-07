package com.ScienceFiction.DronePassAndroid.feature.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
    fun `login legal documents use iOS navigation push presentation`() {
        assertEquals(
            LoginDocumentPresentation.Hidden,
            resolveLoginDocumentPresentation(null),
        )
        assertEquals(
            LoginDocumentPresentation.Pushed,
            resolveLoginDocumentPresentation(LoginDocTarget.Terms),
        )
        assertEquals(
            LoginDocumentPresentation.Pushed,
            resolveLoginDocumentPresentation(LoginDocTarget.Privacy),
        )
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
        assertEquals(20.dp, LoginProviderIconSize)
        assertEquals(8.dp, LoginProviderIconTextSpacing)
        assertEquals(12.dp, LoginGoogleButtonTopSpacing)
        assertEquals(12.dp, LoginTermsTopSpacing)
        assertEquals(16.dp, LoginSkipButtonTopSpacing)
        assertEquals(32.dp, LoginBottomSpacing)
        assertEquals(2.dp, LoginTermsLineSpacing)
    }
}
