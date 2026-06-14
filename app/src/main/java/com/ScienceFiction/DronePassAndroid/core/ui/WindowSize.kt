package com.ScienceFiction.DronePassAndroid.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize

@Composable
fun currentWindowSizeDp(): DpSize {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    return with(density) {
        DpSize(
            width = containerSize.width.toDp(),
            height = containerSize.height.toDp(),
        )
    }
}
