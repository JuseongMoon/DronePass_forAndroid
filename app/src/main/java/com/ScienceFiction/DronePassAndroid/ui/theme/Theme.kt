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
    secondary = Neutral40,
    onSecondary = Color.White,
    tertiary = Accent40,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF6B7280),
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
