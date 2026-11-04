package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ScienceFiction.DronePassAndroid.core.util.sunDisplayState
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** 남은 시간 표시 단위(1분). 다음 이벤트를 모를 때의 갱신 간격이기도 하다. */
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
 * 남은 시간 표시("HH:MM", 분 내림)가 다음에 바뀌는 순간까지의 밀리초.
 * 남은 시간이 분 경계를 넘거나 일출·일몰이 지나는 바로 그 순간이다. 다음 이벤트를 모르면 1분 뒤.
 * 1분 간격 타이머는 분 경계와 어긋나 표시가 최대 59초 늦게 바뀌었다(iOS 와 맞춘 규칙, 11/4 결정).
 */
internal fun millisUntilSunDisplayChange(
    now: LocalDateTime,
    sunriseIsoList: List<String>,
    sunsetIsoList: List<String>,
): Long {
    val refresh = sunDisplayState(sunriseIsoList, sunsetIsoList, now).nextRefresh
        ?: return SunEventRefreshIntervalMs
    // nextRefresh 직후에 한 분 줄어든다(정확히 분 경계면 nextRefresh 는 now).
    return Duration.between(now, refresh).toMillis().coerceAtLeast(0) + 1
}

/**
 * 일출/일몰 남은 시간을 표시 값이 바뀌는 순간마다 갱신한다([millisUntilSunDisplayChange]).
 */
@Composable
internal fun rememberSunEventNow(
    utcOffsetSeconds: Int? = null,
    sunriseIsoList: List<String> = emptyList(),
    sunsetIsoList: List<String> = emptyList(),
): LocalDateTime {
    var now by remember(utcOffsetSeconds) {
        mutableStateOf(resolveSunEventNow(utcOffsetSeconds))
    }

    LaunchedEffect(utcOffsetSeconds, sunriseIsoList, sunsetIsoList) {
        while (true) {
            val current = resolveSunEventNow(utcOffsetSeconds)
            now = current
            delay(millisUntilSunDisplayChange(current, sunriseIsoList, sunsetIsoList))
        }
    }

    return now
}
