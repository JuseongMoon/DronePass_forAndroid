package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalDateTime

internal const val SunEventRefreshIntervalMs = 60_000L

/**
 * iOS `SettingManager.setupTimeUpdateTimer` 와 동일하게 일출/일몰 남은 시간을 1분마다 갱신한다.
 */
@Composable
internal fun rememberSunEventNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(SunEventRefreshIntervalMs)
            now = LocalDateTime.now()
        }
    }

    return now
}
