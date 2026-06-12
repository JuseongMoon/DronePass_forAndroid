package com.ScienceFiction.DronePassAndroid.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val IosToastMessageDurationMs = 2_000L
internal const val IosToastMessageAnimationDurationMs = 300
internal const val IosToastMessageBackgroundAlpha = 0.7f
internal val IosToastMessageBottomPadding = 100.dp
internal val IosToastMessageCornerRadius = 12.dp
internal val IosToastMessageHorizontalPadding = 16.dp
internal val IosToastMessageVerticalPadding = 10.dp

/**
 * iOS ToastMessageModifier 정합: 화면 내부 하단 토스트.
 */
@Composable
internal fun IosToastMessageOverlay(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.padding(bottom = IosToastMessageBottomPadding),
        enter = slideInVertically(
            animationSpec = tween(IosToastMessageAnimationDurationMs),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(IosToastMessageAnimationDurationMs)),
        exit = slideOutVertically(
            animationSpec = tween(IosToastMessageAnimationDurationMs),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(IosToastMessageAnimationDurationMs)),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = IosToastMessageBackgroundAlpha),
                    shape = RoundedCornerShape(IosToastMessageCornerRadius),
                )
                .padding(
                    horizontal = IosToastMessageHorizontalPadding,
                    vertical = IosToastMessageVerticalPadding,
                ),
        )
    }
}
