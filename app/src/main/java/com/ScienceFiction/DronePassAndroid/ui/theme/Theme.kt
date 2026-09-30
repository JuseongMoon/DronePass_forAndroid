package com.ScienceFiction.DronePassAndroid.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * DronePass Material 3 테마.
 *
 * iOS DronePass 는 `INFOPLIST_KEY_UIUserInterfaceStyle = Light` 로 시스템 다크 모드와
 * 무관하게 항상 라이트(화이트) 톤으로 동작한다. Android 도 동일한 시각 정체성을 유지하기
 * 위해 시스템 다크 모드 여부와 관계 없이 항상 LightColorScheme 만 사용한다.
 *
 *  - Brand / Neutral / Accent 토큰은 [Color] 정의 참고.
 *  - dynamicColor (Android 12+ Material You) 도 화이트 톤 통일을 깨므로 사용하지 않는다.
 *  - statusBar / navigationBar 아이콘은 항상 다크 아이콘(라이트 배경 위)으로 강제한다.
 */
private val LightColorScheme = lightColorScheme(
    primary = Brand40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF004FA8),
    inversePrimary = Color(0xFF0A84FF),
    secondary = Neutral40,
    onSecondary = Color.White,
    secondaryContainer = IosSystemGroupedBackground,
    onSecondaryContainer = IosLabel,
    tertiary = Accent40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF1EA),
    onTertiaryContainer = Color(0xFF7A2E0E),
    error = IosSystemRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEA),
    onErrorContainer = Color(0xFF8E1B14),
    background = Color.White,
    onBackground = IosLabel,
    surface = Color.White,
    onSurface = IosLabel,
    surfaceVariant = IosSystemGroupedBackground,
    // iOS .secondary(#3C3C43 60%) 를 흰 배경에 얹은 색. 보조 글자·아이콘 전반에 쓰인다.
    onSurfaceVariant = IosSecondaryLabel,
    // Material 3 기본 팔레트(보라)가 시트·다이얼로그·메뉴 배경으로 새어 나오지 않도록
    // surface 계열 토큰을 모두 iOS 흰색/회색 계열로 명시한다.
    surfaceTint = Color.White,
    surfaceBright = Color.White,
    surfaceDim = IosSystemGray5,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = IosSystemGray5,
    inverseSurface = IosLabel,
    inverseOnSurface = IosSystemGroupedBackground,
    outline = IosSystemGray2,
    outlineVariant = IosSeparator,
    scrim = Color.Black,
)

@Composable
fun DronePassAndroidTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // 라이트 배경 위 다크 아이콘 강제: status bar / navigation bar 모두 라이트 외관 적용.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
