package com.ScienceFiction.DronePassAndroid.core.util

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 다음 일출/일몰 이벤트 정보. iOS `SettingManager.{sunEventIconName, timeUntilNextSunEvent}` 와 동등.
 *
 * @property isNextSunset true 면 다음 이벤트가 일몰(현재 낮), false 면 일출(현재 밤).
 * @property timeUntilFormatted 남은 시간 `"HH:MM"`(2자리), 계산 불가 시 `"--:--"`.
 */
data class NextSunEvent(
    val isNextSunset: Boolean,
    val timeUntilFormatted: String,
)

/**
 * 현재 시각과 ISO 일출/일몰 문자열로 다음 sun event 를 계산.
 *
 * iOS `SettingManager.updateRemainingTime` 매핑:
 *  - `now < sunrise` → 오늘 일출까지 (isNextSunset=false)
 *  - `sunrise <= now < sunset` → 오늘 일몰까지 (isNextSunset=true)
 *  - `now >= sunset` → 내일 일출까지 (응답에 내일 데이터가 없으므로 동일 시각 가정해 24h 보정)
 *  - 파싱 실패 → `("--:--", true)`
 *
 * 입력 형식: `"2025-10-12T06:15"` 같은 ISO local datetime. "T" 뒤 `HH:mm` 5글자만 사용.
 */
fun nextSunEvent(
    sunriseIso: String?,
    sunsetIso: String?,
    now: LocalDateTime = LocalDateTime.now(),
): NextSunEvent {
    val eventFromDatedValues = nextSunEvent(
        sunriseIsoList = sunriseIso?.let(::listOf),
        sunsetIsoList = sunsetIso?.let(::listOf),
        now = now,
    )
    if (eventFromDatedValues.timeUntilFormatted != "--:--") {
        return eventFromDatedValues
    }

    val sunrise = parseIsoLocalTime(sunriseIso)
    val sunset = parseIsoLocalTime(sunsetIso)
    if (sunrise == null || sunset == null) {
        return NextSunEvent(isNextSunset = true, timeUntilFormatted = "--:--")
    }
    val nowTime = now.toLocalTime()
    return when {
        nowTime.isBefore(sunrise) ->
            NextSunEvent(isNextSunset = false, timeUntilFormatted = formatDuration(nowTime, sunrise))
        nowTime.isBefore(sunset) ->
            NextSunEvent(isNextSunset = true, timeUntilFormatted = formatDuration(nowTime, sunset))
        else ->
            NextSunEvent(
                isNextSunset = false,
                timeUntilFormatted = formatDurationTomorrow(nowTime, sunrise),
            )
    }
}

/** 남은 시간을 세는 대상(DATA_CONTRACT "일출·일몰 표시", D-54). */
enum class SunDisplayTarget { TODAY_SUNSET, TODAY_SUNRISE, TOMORROW_SUNRISE }

/**
 * 일출·일몰 표시 상태. iOS 와 같은 규칙으로 계산하고 공유 fixture(sun-display-cases)로 고정한다.
 *
 * @property isDaytime 오늘 일출·일몰을 모르면 null.
 * @property display 남은 시간 `"HH:MM"`(내림), 대상이 없으면 `"--:--"`.
 * @property nextRefresh 표시가 다음에 바뀌는 시각(이 시각 직후 한 분 줄어든다). 대상이 없으면 null.
 */
data class SunDisplayState(
    val isDaytime: Boolean?,
    val target: SunDisplayTarget?,
    val targetTime: LocalDateTime?,
    val display: String,
    val nextRefresh: LocalDateTime?,
)

/**
 * 기기 시간대의 날짜 붙은 일출·일몰 목록으로 표시 상태를 정한다(D-54).
 *  - 오늘·내일은 [now] 의 날짜로 고른다. 오늘 일출이나 일몰이 없으면 아무것도 보여 주지 않는다.
 *  - 초 단위로 `오늘 일출 <= now < 오늘 일몰` 이면 낮이다(일몰과 같은 시각은 밤, 일출과 같은 시각은 낮).
 *  - 낮이면 오늘 일몰, 밤이면 오늘 일출이 아직이면 오늘 일출, 아니면 내일 일출까지 센다.
 *  - 표시는 남은 초를 내린 "HH:MM", 다음 갱신은 `대상 - floor(남은 초/60)분`.
 */
fun sunDisplayState(
    sunriseIsoList: List<String>?,
    sunsetIsoList: List<String>?,
    now: LocalDateTime,
): SunDisplayState {
    val sunrises = sunriseIsoList.orEmpty().mapNotNull(::parseDatedIso)
    val sunsets = sunsetIsoList.orEmpty().mapNotNull(::parseDatedIso)
    val today = now.toLocalDate()
    val todaySunrise = sunrises.firstOrNull { it.toLocalDate() == today }
    val todaySunset = sunsets.firstOrNull { it.toLocalDate() == today }
    if (todaySunrise == null || todaySunset == null) {
        return SunDisplayState(null, null, null, SunDisplayPlaceholder, null)
    }
    val isDaytime = !now.isBefore(todaySunrise) && now.isBefore(todaySunset)
    val (target, targetTime) = when {
        isDaytime -> SunDisplayTarget.TODAY_SUNSET to todaySunset
        todaySunrise.isAfter(now) -> SunDisplayTarget.TODAY_SUNRISE to todaySunrise
        else -> SunDisplayTarget.TOMORROW_SUNRISE to sunrises.firstOrNull { it.toLocalDate() == today.plusDays(1) }
    }
    if (targetTime == null || !targetTime.isAfter(now)) {
        return SunDisplayState(isDaytime, null, null, SunDisplayPlaceholder, null)
    }
    val remainingSeconds = Duration.between(now, targetTime).seconds
    val remainingMinutes = remainingSeconds / 60
    return SunDisplayState(
        isDaytime = isDaytime,
        target = target,
        targetTime = targetTime,
        display = formatMinutes(remainingMinutes.toInt()),
        nextRefresh = targetTime.minusMinutes(remainingMinutes),
    )
}

private const val SunDisplayPlaceholder = "--:--"

/**
 * 여러 날짜의 ISO 일출/일몰 문자열로 다음 이벤트를 정한다. 규칙은 [sunDisplayState] 다.
 * 오늘 값을 모르면 `("--:--", true)`, 밤인데 내일 일출을 모르면 `("--:--", false)`.
 */
fun nextSunEvent(
    sunriseIsoList: List<String>?,
    sunsetIsoList: List<String>?,
    now: LocalDateTime = LocalDateTime.now(),
): NextSunEvent {
    val state = sunDisplayState(sunriseIsoList, sunsetIsoList, now)
    return NextSunEvent(isNextSunset = state.isDaytime ?: true, timeUntilFormatted = state.display)
}

private fun parseDatedIso(iso: String): LocalDateTime? =
    runCatching { LocalDateTime.parse(iso) }.getOrNull()

private fun parseIsoLocalTime(iso: String?): LocalTime? {
    if (iso.isNullOrBlank()) return null
    val timePart = iso.substringAfter('T', missingDelimiterValue = iso)
    return try {
        LocalTime.parse(timePart.take(5))
    } catch (e: Exception) {
        null
    }
}

private fun formatDuration(from: LocalTime, to: LocalTime): String {
    val totalMinutes = ((to.toSecondOfDay() - from.toSecondOfDay()) / 60).coerceAtLeast(0)
    return formatMinutes(totalMinutes)
}

private fun formatDurationTomorrow(now: LocalTime, tomorrowSunrise: LocalTime): String {
    val nowSec = now.toSecondOfDay()
    val tomorrowSec = tomorrowSunrise.toSecondOfDay() + 24 * 3600
    val totalMinutes = ((tomorrowSec - nowSec) / 60).coerceAtLeast(0)
    return formatMinutes(totalMinutes)
}

private fun formatMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "%02d:%02d".format(h, m)
}
