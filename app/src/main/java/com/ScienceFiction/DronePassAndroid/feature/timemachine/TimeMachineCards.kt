package com.ScienceFiction.DronePassAndroid.feature.timemachine

import com.ScienceFiction.DronePassAndroid.core.data.repository.TimeMachineWeather
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 타임머신 시각의 메인 화면 날씨·KP 카드 값(D-59). '예보' 표시는 하지 않는다(사용자가 미래 값임을 안다).
 * 오늘 지난 칸(D-87)도 같은 규칙이다(날씨는 오늘 00:00부터 받은 시간별, KP 는 실측 칸).
 * 지금(now)이면 만들지 않는다 — 카드는 기존 동작 그대로다.
 * 범위 밖이면 값 자리가 '–'(지금 카드의 빈 값과 같은 "-")이고 풍향 화살표는 자리만 남긴다. 해 줄은 늘 계산한다.
 */
data class TimeMachineCardValues(
    /** 날씨 칸이 없으면 null(아이콘은 구름). */
    val condition: WeatherCondition?,
    /** mm — 그 시간 강수량(시간별 예보는 강도 대신 강수량으로 아이콘을 고른다, iOS 와 같음). */
    val precipitation: Double?,
    /** null 이면 풍향 화살표를 숨긴다. */
    val windDirection: Double?,
    val temperature: Double?,
    val sunToSunset: Boolean,
    val sunText: String,
    val kp: Double?,
)

/**
 * 칸마다 부르는 조회(이분 탐색과 상수 시간 계산만). 자료 파싱은 [KpSeries]·[TimeMachineWeather] 를 만들 때 한 번만 한다.
 * @param sunLatitude 해 계산 기준 위치(날씨 카드와 같은 위치, 없으면 지도 중심).
 */
internal fun timeMachineCardValues(
    selection: TimeMachineSelection,
    weather: TimeMachineWeather?,
    kpThreeHour: KpSeries,
    kpLongTerm: KpSeries,
    sunLatitude: Double,
    sunLongitude: Double,
): TimeMachineCardValues {
    val t = selection.timeMillis
    val hour = weather?.let { w ->
        TimeMachineCardRules.bucket(w.starts, TimeMachineCardRules.HOUR_MILLIS, t)?.let { w.hours[it] }
    }
    val kp = TimeMachineCardRules.kp(t, kpThreeHour, kpLongTerm)
    val today = TimeMachineSun.times(selection.dayStartMillis, sunLatitude, sunLongitude)
    val next = TimeMachineSun.times(TimeMachineSlot.addDays(selection.dayStartMillis, 1), sunLatitude, sunLongitude)
    val sun = TimeMachineCardRules.sunEvent(t, today.sunriseMillis, today.sunsetMillis, next.sunriseMillis)
    return TimeMachineCardValues(
        condition = hour?.condition,
        precipitation = hour?.precipitation,
        windDirection = hour?.windDirection,
        temperature = hour?.temperature,
        sunToSunset = sun.toSunset,
        sunText = sun.text,
        kp = kp,
    )
}

private val KpTimeFormats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss")
    .map { DateTimeFormatter.ofPattern(it, Locale.US) }

/** NOAA 3시간 예보 time_tag(UTC). iOS KPIndexData.date 와 같은 형식만 읽는다. */
internal fun kpTimeTagMillis(timeTag: String): Long? {
    for (format in KpTimeFormats) {
        val parsed = runCatching { LocalDateTime.parse(timeTag, format) }.getOrNull() ?: continue
        return parsed.toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    return null
}

private val Kp27DayFormat = DateTimeFormatter.ofPattern("yyyy MMM d", Locale.US)

/** 27일 전망 날짜 "2026 Nov 21"(UTC 00:00). */
internal fun kp27DayMillis(date: String): Long? =
    runCatching { LocalDate.parse(date, Kp27DayFormat).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()

/**
 * 타임머신이 쓰는 3시간 KP 칸: NOAA 자료(지난 7일 실측 observed·estimated + 예보) 중 끝(시작 + 3시간)이 오늘 KST 00:00 뒤인 칸(D-87).
 * KP 그래프 화면의 −6h~+48h 거르기와는 따로다.
 */
internal fun kpThreeHourSeries(forecast: List<KpIndexData>, nowMillis: Long = System.currentTimeMillis()): KpSeries {
    val today = TimeMachineSlot.dayStart(nowMillis)
    return KpSeries.of(
        forecast.mapNotNull { item -> kpTimeTagMillis(item.timeTag)?.let { it to item.kp } }
            .filter { (start, _) -> start + TimeMachineCardRules.THREE_HOURS_MILLIS > today },
    )
}

internal fun kpLongTermSeries(forecast: List<Kp27DayForecast>): KpSeries =
    KpSeries.of(forecast.mapNotNull { item -> kp27DayMillis(item.date)?.let { it to item.kp } })
