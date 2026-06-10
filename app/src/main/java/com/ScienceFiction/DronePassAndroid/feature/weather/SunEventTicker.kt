package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

internal const val SunEventRefreshIntervalMs = 60_000L

internal fun resolveSunEventNow(
    utcOffsetSeconds: Int?,
    currentTimeMillis: Long = System.currentTimeMillis(),
): LocalDateTime {
    val zone = runCatching {
        utcOffsetSeconds?.let(ZoneOffset::ofTotalSeconds)
    }.getOrNull() ?: ZoneId.systemDefault()
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), zone)
}

/**
 * iOS `SettingManager.setupTimeUpdateTimer` 와 동일하게 일출/일몰 남은 시간을 1분마다 갱신한다.
 */
@Composable
internal fun rememberSunEventNow(utcOffsetSeconds: Int? = null): LocalDateTime {
    var now by remember(utcOffsetSeconds) {
        mutableStateOf(resolveSunEventNow(utcOffsetSeconds))
    }

    LaunchedEffect(utcOffsetSeconds) {
        while (true) {
            delay(SunEventRefreshIntervalMs)
            now = resolveSunEventNow(utcOffsetSeconds)
        }
    }

    return now
}
