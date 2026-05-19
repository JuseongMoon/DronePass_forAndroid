package com.ScienceFiction.DronePassAndroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Brand80,
    secondary = Neutral80,
    tertiary = Accent80,
)

private val LightColorScheme = lightColorScheme(
    primary = Brand40,
    secondary = Neutral40,
    tertiary = Accent40,
)

/**
 * DronePass Material 3 테마.
 *
 * dynamicColor 정책:
 *  - 기본값은 false. iOS 와 시각 일관성(systemBlue 브랜드 통일)을 우선 보장한다.
 *  - 사용자가 시스템 강조 색상을 따르길 원하면 호출처에서 true 로 옵트인할 수 있도록 노출.
 *  - true 이면 Android 12+ 에서 monet 동적 색상을 사용 (이전 버전은 정적 팔레트 사용).
 *
 * statusBar / navigationBar:
 *  - Android 15+ 에서 edge-to-edge 가 강제되어 [androidx.activity.enableEdgeToEdge] 호출 후
 *    statusBar/navigationBar 색상 직접 설정은 무시된다. WindowInsetsController 의 light/dark
 *    icon 모드만 조정하면 되며, 색상은 Compose 의 surface/scaffold 가 직접 그린다.
 *  - 따라서 본 함수에서는 색상을 손대지 않는다.
 */
@Composable
fun DronePassAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
