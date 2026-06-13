package com.ScienceFiction.DronePassAndroid.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.document.PrivacyPolicyScreen
import com.ScienceFiction.DronePassAndroid.feature.document.TermsOfServiceScreen

internal const val LoginTabletBreakpointDp = 600
internal val LoginScreenHorizontalPadding = 0.dp
internal val LoginButtonHorizontalPadding = 24.dp
internal val LoginVerticalPaddingPhone = 24.dp
internal val LoginVerticalPaddingTablet = 32.dp
internal val LoginTopSpacerPhone = 40.dp
internal val LoginTopSpacerTablet = 72.dp
internal val LoginTermsBottomPaddingPhone = 24.dp
internal val LoginTermsBottomPaddingTablet = 40.dp
internal val LoginLogoSize = 200.dp
internal val LoginLogoCornerRadius = 24.dp
internal val LoginLogoBottomSpacing = 32.dp
internal val LoginTitleBottomSpacing = 24.dp
internal val LoginDividerHorizontalPadding = 22.dp
internal val LoginDividerBottomSpacing = 10.dp
internal val LoginButtonMaxWidth = 350.dp
internal val LoginButtonHeight = 50.dp
internal val LoginButtonCornerRadius = 12.dp
internal val LoginProviderIconSize = 18.dp
internal val LoginProviderIconTextSpacing = 8.dp
internal val LoginProviderTextSize = 19.sp
internal val LoginGoogleButtonTopSpacing = 8.dp
internal val LoginGoogleButtonShadowElevation = 3.dp
internal val LoginTermsTopSpacing = 8.dp
internal val LoginSkipButtonTopSpacing = 16.dp
internal val LoginTermsLineSpacing = 2.dp
internal const val LoginDocumentSheetSkipPartiallyExpanded = false

internal fun resolveLoginVerticalPadding(isTablet: Boolean) =
    if (isTablet) LoginVerticalPaddingTablet else LoginVerticalPaddingPhone

internal fun resolveLoginTopSpacer(isTablet: Boolean) =
    if (isTablet) LoginTopSpacerTablet else LoginTopSpacerPhone

internal fun resolveLoginTermsBottomPadding(isTablet: Boolean) =
    if (isTablet) LoginTermsBottomPaddingTablet else LoginTermsBottomPaddingPhone

/**
 * 로그인 화면 Composable.
 * iOS LoginView 의 로고/타이틀/약관 흐름을 기준으로 Apple, Google 로그인을 제공한다.
 *
 * @param viewModel 인증 ViewModel (Hilt로 주입)
 * @param onLoginSuccess 로그인 성공 시 콜백
 * @param onSkipLogin 비로그인으로 계속하기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onSkipLogin: () -> Unit,
    showSkipLogin: Boolean = false,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val accountSwitchConfirmation by viewModel.accountSwitchConfirmation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= LoginTabletBreakpointDp
    val topSpacer = resolveLoginTopSpacer(isTablet)
    val termsBottomPadding = resolveLoginTermsBottomPadding(isTablet)
    var docTarget by remember { mutableStateOf<LoginDocTarget?>(null) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }

    // authState 전이 처리는 단일 LaunchedEffect 로 통합한다.
    // 이전: 두 개의 LaunchedEffect(authState) 가 동시 등록되어 LoggedIn → Error 빠른 전이 시
    //   화면 전환과 스낵바가 경쟁할 수 있었음.
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.LoggedIn -> onLoginSuccess()
            is AuthState.Error -> loginErrorMessage = state.message
            else -> Unit // Loading, LoggedOut 은 동작 없음 (UI 상태만 변경)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = LoginScreenHorizontalPadding,
                    vertical = resolveLoginVerticalPadding(isTablet),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = topSpacer)
            )

            Image(
                painter = painterResource(R.drawable.login_logo),
                contentDescription = stringResource(R.string.login_logo_description),
                modifier = Modifier
                    .size(LoginLogoSize)
                    .clip(RoundedCornerShape(LoginLogoCornerRadius)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(LoginLogoBottomSpacing))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(LoginTitleBottomSpacing))

            HorizontalDivider(modifier = Modifier.padding(horizontal = LoginDividerHorizontalPadding))

            Spacer(modifier = Modifier.height(LoginDividerBottomSpacing))

            // 로딩 상태
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.login_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Apple Sign-In 버튼 — Apple HIG: 검정 배경 + 흰 텍스트/로고, Google 위에 배치.
                // iOS DronePass 와 같은 Apple ID 로 로그인 시 동일 Firebase UID → 데이터 자동 호환.
                Button(
                    onClick = {
                        val activity = context.findActivity()
                        if (activity != null) {
                            viewModel.signInWithApple(activity)
                        } else {
                            loginErrorMessage = context.getString(R.string.login_apple_error)
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = LoginButtonHorizontalPadding)
                        .widthIn(max = LoginButtonMaxWidth)
                        .fillMaxWidth()
                        .height(LoginButtonHeight),
                    shape = RoundedCornerShape(LoginButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_apple_logo),
                        contentDescription = stringResource(R.string.login_apple_logo_description),
                        tint = Color.White,
                        modifier = Modifier.size(LoginProviderIconSize),
                    )
                    Spacer(modifier = Modifier.size(LoginProviderIconTextSpacing))
                    Text(
                        text = stringResource(R.string.login_apple),
                        fontSize = LoginProviderTextSize,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(LoginGoogleButtonTopSpacing))

                Button(
                    onClick = { viewModel.signInWithGoogle(context) },
                    modifier = Modifier
                        .padding(horizontal = LoginButtonHorizontalPadding)
                        .widthIn(max = LoginButtonMaxWidth)
                        .fillMaxWidth()
                        .height(LoginButtonHeight),
                    shape = RoundedCornerShape(LoginButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = LoginGoogleButtonShadowElevation,
                        pressedElevation = LoginGoogleButtonShadowElevation,
                        focusedElevation = LoginGoogleButtonShadowElevation,
                        hoveredElevation = LoginGoogleButtonShadowElevation,
                    ),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = stringResource(R.string.login_google_logo_description),
                        modifier = Modifier.size(LoginProviderIconSize),
                    )
                    Spacer(modifier = Modifier.size(LoginProviderIconTextSpacing))
                    Text(
                        text = stringResource(R.string.login_google),
                        fontSize = LoginProviderTextSize,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                LoginTermsNotice(
                    onTermsClick = { docTarget = LoginDocTarget.Terms },
                    onPrivacyClick = { docTarget = LoginDocTarget.Privacy },
                    modifier = Modifier.padding(
                        top = LoginTermsTopSpacing,
                        bottom = if (showSkipLogin) 0.dp else termsBottomPadding,
                    ),
                )

                if (showSkipLogin) {
                    Spacer(modifier = Modifier.height(LoginSkipButtonTopSpacing))

                    OutlinedButton(
                        onClick = onSkipLogin,
                        modifier = Modifier
                            .padding(horizontal = LoginButtonHorizontalPadding)
                            .widthIn(max = LoginButtonMaxWidth)
                            .fillMaxWidth()
                            .height(LoginButtonHeight),
                        shape = RoundedCornerShape(LoginButtonCornerRadius),
                    ) {
                        Text(
                            text = stringResource(R.string.login_skip),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = topSpacer)
            )
        }
    }

    docTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { docTarget = null },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = LoginDocumentSheetSkipPartiallyExpanded,
            ),
        ) {
            when (target) {
                LoginDocTarget.Terms -> TermsOfServiceScreen(onDismiss = { docTarget = null })
                LoginDocTarget.Privacy -> PrivacyPolicyScreen(onDismiss = { docTarget = null })
            }
        }
    }

    loginErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { loginErrorMessage = null },
            title = {
                Text(text = stringResource(R.string.login_error_title))
            },
            text = {
                Text(text = message.ifBlank { stringResource(R.string.login_error_unknown) })
            },
            confirmButton = {
                TextButton(onClick = { loginErrorMessage = null }) {
                    Text(text = stringResource(R.string.common_confirm))
                }
            },
        )
    }

    accountSwitchConfirmation?.let { request ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelAccountSwitch() },
            title = {
                Text(text = stringResource(R.string.login_account_switch_title))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.login_account_switch_message,
                        request.localDataCount,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmAccountSwitch() }) {
                    Text(text = stringResource(R.string.login_account_switch_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelAccountSwitch() }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

internal sealed class LoginDocTarget {
    data object Terms : LoginDocTarget()
    data object Privacy : LoginDocTarget()
}

internal enum class LoginDocumentPresentation {
    Hidden,
    Sheet,
}

internal fun resolveLoginDocumentPresentation(target: LoginDocTarget?): LoginDocumentPresentation {
    return if (target == null) {
        LoginDocumentPresentation.Hidden
    } else {
        LoginDocumentPresentation.Sheet
    }
}

@Composable
private fun LoginTermsNotice(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LoginTermsLineSpacing),
    ) {
        Text(
            text = stringResource(R.string.login_terms_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.login_terms_service),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onTermsClick),
            )
            Text(
                text = ", ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.login_terms_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onPrivacyClick),
            )
            Text(
                text = stringResource(R.string.login_terms_middle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.login_terms_agree),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
