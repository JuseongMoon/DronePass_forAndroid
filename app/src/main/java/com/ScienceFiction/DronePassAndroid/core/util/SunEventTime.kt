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
        useSameDayFallback = false,
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

/**
 * 여러 날짜의 ISO 일출/일몰 문자열에서 현재 이후의 가장 가까운 이벤트를 고른다.
 *
 * iOS 는 WeatherManager 가 오늘 일출/일몰과 내일 일출을 함께 보관하고, 일몰 이후에는
 * `tomorrowSunriseTime` 까지의 남은 시간을 표시한다. Open-Meteo daily 배열도 같은 정보를
 * 주므로 리스트 기반 계산을 우선 사용한다.
 */
fun nextSunEvent(
    sunriseIsoList: List<String>?,
    sunsetIsoList: List<String>?,
    now: LocalDateTime = LocalDateTime.now(),
): NextSunEvent {
    return nextSunEvent(
        sunriseIsoList = sunriseIsoList,
        sunsetIsoList = sunsetIsoList,
        now = now,
        useSameDayFallback = false,
    )
}

private fun nextSunEvent(
    sunriseIsoList: List<String>?,
    sunsetIsoList: List<String>?,
    now: LocalDateTime,
    useSameDayFallback: Boolean,
): NextSunEvent {
    val datedEvents = buildList {
        sunriseIsoList.orEmpty().forEach { iso ->
            parseIsoLocalDateTime(iso, now)?.let { add(false to it) }
        }
        sunsetIsoList.orEmpty().forEach { iso ->
            parseIsoLocalDateTime(iso, now)?.let { add(true to it) }
        }
    }

    val nextEvent = datedEvents
        .filter { (_, dateTime) -> dateTime.isAfter(now) }
        .minByOrNull { (_, dateTime) -> dateTime }

    if (nextEvent != null) {
        return NextSunEvent(
            isNextSunset = nextEvent.first,
            timeUntilFormatted = formatDuration(now, nextEvent.second),
        )
    }

    if (!useSameDayFallback) {
        return NextSunEvent(isNextSunset = true, timeUntilFormatted = "--:--")
    }

    return nextSunEvent(
        sunriseIso = sunriseIsoList.orEmpty().firstOrNull(),
        sunsetIso = sunsetIsoList.orEmpty().firstOrNull(),
        now = now,
    )
}

private fun parseIsoLocalDateTime(iso: String?, now: LocalDateTime): LocalDateTime? {
    if (iso.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse(iso)
    } catch (_: Exception) {
        parseIsoLocalTime(iso)?.let { time ->
            now.toLocalDate().atTime(time)
        }
    }
}

private fun parseIsoLocalTime(iso: String?): LocalTime? {
    if (iso.isNullOrBlank()) return null
    val timePart = iso.substringAfter('T', missingDelimiterValue = iso)
    return try {
        LocalTime.parse(timePart.take(5))
    } catch (e: Exception) {
        null
    }
}

private fun formatDuration(from: LocalDateTime, to: LocalDateTime): String {
    val totalMinutes = Duration.between(from, to).toMinutes().coerceAtLeast(0).toInt()
    return formatMinutes(totalMinutes)
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
