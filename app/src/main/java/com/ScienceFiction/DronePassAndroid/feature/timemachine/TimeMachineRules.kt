package com.ScienceFiction.DronePassAndroid.feature.timemachine

import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/*
 * 타임머신 규칙(D-58 화면, D-59 카드 값, D-83 범위). iOS 타임머신과 같은 칸·같은 값을 고르는 순수 함수들이다.
 * 하루는 KST 00:00~24:00(D-84)이고, 시각은 모두 epoch 밀리초다.
 */

/** 타임머신의 하루 기준 시간대(KST, 일광 절약 없음). */
internal val TimeMachineZone: ZoneId = ZoneId.of("Asia/Seoul")

internal object TimeMachineLimits {
    /** 오늘 + 27일까지 간다(KP 27일 전망 끝, D-83). */
    const val MAX_DAYS = 27
}

/** 타임머신 선택: 고른 날(KST 00:00) + 10분 칸. 기준 시각 t = 그날 00:00 + 칸 × 10분. */
data class TimeMachineSelection(
    val dayStartMillis: Long,
    val slot: Int,
    /** 도형 "이 비행 일정으로 보기"로 들어왔으면 그 도형. */
    val shapeId: String? = null,
) {
    val timeMillis: Long get() = TimeMachineSlot.time(dayStartMillis, slot)
}

/** 10분 칸 규칙: KST 하루를 144칸으로 나눈다. */
internal object TimeMachineSlot {
    const val MINUTES = 10
    const val COUNT = 144
    const val SLOT_MILLIS = MINUTES * 60_000L
    private const val HOUR_MILLIS = 3_600_000L

    data class Resolved(
        val timeMillis: Long,
        /** 실제로 쓴 날(KST 00:00). */
        val dayStartMillis: Long,
        val slot: Int,
        val label: String,
    )

    fun dayStart(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(TimeMachineZone).toLocalDate()
            .atStartOfDay(TimeMachineZone).toInstant().toEpochMilli()

    fun addDays(dayStartMillis: Long, days: Int): Long =
        localDate(dayStartMillis).plusDays(days.toLong()).atStartOfDay(TimeMachineZone).toInstant().toEpochMilli()

    /** KST 날짜 차이(t의 날 − now의 날). */
    fun daysBetween(fromMillis: Long, toMillis: Long): Long =
        ChronoUnit.DAYS.between(localDate(fromMillis), localDate(toMillis))

    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(TimeMachineZone).toLocalDate()

    /** 오늘이면 지금 뒤 첫 칸(올림), 다른 날이면 0. 오늘 남은 칸이 없으면 144. */
    fun firstSlot(dayMillis: Long, nowMillis: Long): Int {
        val today = dayStart(nowMillis)
        if (dayStart(dayMillis) != today) return 0
        val elapsed = nowMillis - today
        return ((elapsed + SLOT_MILLIS - 1) / SLOT_MILLIS).toInt()
    }

    /** 날은 [오늘, 오늘 + 27일], 칸은 [0, 143]. 오늘이면 지금 앞 칸 금지(남은 칸이 없으면 내일 0칸). */
    fun resolve(nowMillis: Long, dayMillis: Long, slot: Int): Resolved {
        val today = dayStart(nowMillis)
        val maxDay = addDays(today, TimeMachineLimits.MAX_DAYS)
        var day = dayStart(dayMillis).coerceIn(today, maxDay)
        var s = slot.coerceIn(0, COUNT - 1)
        if (day == today) {
            val first = firstSlot(day, nowMillis)
            if (first > COUNT - 1) {
                day = addDays(today, 1)
                s = 0
            } else {
                s = max(s, first)
            }
        }
        val t = time(day, s)
        return Resolved(timeMillis = t, dayStartMillis = day, slot = s, label = label(t, nowMillis))
    }

    /** 동그라미 문구: 다른 날 "D+N", 같은 날 Δ ≤ 0이면 "now", 1시간 미만 "+N0m"(최소 10), 아니면 "+Nh". */
    fun label(timeMillis: Long, nowMillis: Long): String {
        val days = daysBetween(nowMillis, timeMillis)
        if (days > 0) return "D+$days"
        val delta = timeMillis - nowMillis
        if (delta <= 0) return "now"
        if (delta < HOUR_MILLIS) return "+${max(10L, (delta / SLOT_MILLIS.toDouble()).roundToLong() * 10)}m"
        return "+${(delta / HOUR_MILLIS.toDouble()).roundToLong()}h"
    }

    /** ‹ › 하루 넘기기: 결과 날이 [오늘, 오늘 + 27일] 안이면 허용. */
    fun canStepDay(nowMillis: Long, dayMillis: Long, delta: Int): Boolean {
        val today = dayStart(nowMillis)
        val target = addDays(dayStart(dayMillis), delta)
        return target in today..addDays(today, TimeMachineLimits.MAX_DAYS)
    }

    fun time(dayMillis: Long, slot: Int): Long = dayStart(dayMillis) + slot * SLOT_MILLIS

    /** 시각이 든 칸(내림). */
    fun slotOf(timeMillis: Long): Int =
        ((timeMillis - dayStart(timeMillis)) / SLOT_MILLIS).toInt().coerceIn(0, COUNT - 1)

    /** 시각을 10분 칸으로 올림. */
    fun slotRoundingUp(timeMillis: Long): Int {
        val elapsed = timeMillis - dayStart(timeMillis)
        return min(COUNT - 1, ((elapsed + SLOT_MILLIS - 1) / SLOT_MILLIS).toInt())
    }
}

/**
 * 일출·일몰(오프라인). iOS가 쓰는 Solar 패키지와 같은 식(Almanac for Computers 1990, 공식 천정 90.83°)이다.
 * 같은 값을 내도록 정규화·날짜 보정까지 그대로 따른다.
 */
internal object SolarCalculator {
    private const val OFFICIAL_ZENITH = 90.83

    fun sunrise(dateMillis: Long, latitude: Double, longitude: Double): Long? =
        calculate(isSunrise = true, dateMillis = dateMillis, latitude = latitude, longitude = longitude)

    fun sunset(dateMillis: Long, latitude: Double, longitude: Double): Long? =
        calculate(isSunrise = false, dateMillis = dateMillis, latitude = latitude, longitude = longitude)

    private fun calculate(isSunrise: Boolean, dateMillis: Long, latitude: Double, longitude: Double): Long? {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        val utcDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val day = utcDate.dayOfYear.toDouble()
        val lngHour = longitude / 15
        val hourTime = if (isSunrise) 6.0 else 18.0
        val t = day + ((hourTime - lngHour) / 24)
        val m = (0.9856 * t) - 3.289
        var l = m + 1.916 * sin(m.toRadians()) + 0.020 * sin(2 * m.toRadians()) + 282.634
        l = normalise(l, 360.0)
        var ra = atan(0.91764 * tan(l.toRadians())).toDegrees()
        ra = normalise(ra, 360.0)
        val lQuadrant = floor(l / 90) * 90
        val raQuadrant = floor(ra / 90) * 90
        ra = (ra + (lQuadrant - raQuadrant)) / 15
        val sinDec = 0.39782 * sin(l.toRadians())
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(OFFICIAL_ZENITH.toRadians()) - (sinDec * sin(latitude.toRadians()))) /
            (cosDec * cos(latitude.toRadians()))
        if (!(cosH < 1)) return null  // 해가 뜨지 않음
        if (!(cosH > -1)) return null  // 해가 지지 않음
        val tempH = if (isSunrise) 360 - acos(cosH).toDegrees() else acos(cosH).toDegrees()
        val h = tempH / 15.0
        val localMean = h + ra - (0.06571 * t) - 6.622
        val ut = normalise(localMean - lngHour, 24.0)
        val hour = floor(ut)
        val minute = floor((ut - hour) * 60.0)
        val second = (((ut - hour) * 60) - minute) * 60.0
        val shouldBeYesterday = lngHour > 0 && ut > 12 && isSunrise
        val shouldBeTomorrow = lngHour < 0 && ut < 12 && !isSunrise
        val setDate = when {
            shouldBeYesterday -> utcDate.minusDays(1)
            shouldBeTomorrow -> utcDate.plusDays(1)
            else -> utcDate
        }
        return setDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() +
            hour.toLong() * 3_600_000L + minute.toLong() * 60_000L + second.toLong() * 1_000L
    }

    /** Solar 의 정규화: 한 번만 더하거나 뺀다. */
    private fun normalise(value: Double, maximum: Double): Double {
        var v = value
        if (v < 0) v += maximum
        if (v > maximum) v -= maximum
        return v
    }

    private fun Double.toRadians(): Double = this * (Math.PI / 180.0)
    private fun Double.toDegrees(): Double = (this * 180.0) / Math.PI
}

/** 그날(KST) 기준 위치의 일출·일몰. 극야·백야면 null 이고, KST 하루 밖이면 자른다. */
internal object TimeMachineSun {
    enum class Polar { DAY, NIGHT }

    data class Times(
        val sunriseMillis: Long?,
        val sunsetMillis: Long?,
        /** 해가 뜨지 않거나 지지 않는 날. */
        val polar: Polar?,
    )

    fun times(dayMillis: Long, latitude: Double, longitude: Double): Times {
        val start = TimeMachineSlot.dayStart(dayMillis)
        val end = start + 86_400_000L
        val noon = start + 12 * 3_600_000L  // KST 정오로 그날을 고른다
        val rise = SolarCalculator.sunrise(noon, latitude, longitude)
        val set = SolarCalculator.sunset(noon, latitude, longitude)
        if (rise == null || set == null || rise >= set) {
            // 해가 뜨거나 지지 않는 날: 태양 적위와 위도가 같은 쪽이면 백야, 아니면 극야(한국에서는 일어나지 않는다)
            val doy = TimeMachineSlot.localDate(start).dayOfYear.toDouble()
            val declination = -23.44 * cos(2 * Math.PI / 365 * (doy + 10))
            return Times(null, null, if (latitude * declination > 0) Polar.DAY else Polar.NIGHT)
        }
        return Times(rise.coerceIn(start, end), set.coerceIn(start, end), null)
    }

    /** 슬라이더에 그릴 일출·일몰 칸. 백야는 (0, 144), 극야는 (null, null). */
    fun slots(times: Times): Pair<Int?, Int?> = when (times.polar) {
        Polar.DAY -> 0 to TimeMachineSlot.COUNT
        Polar.NIGHT -> null to null
        null -> times.sunriseMillis?.let(TimeMachineSlot::slotOf) to times.sunsetMillis?.let(TimeMachineSlot::slotOf)
    }
}

/** 카드 선택 규칙(D-59). */
internal object TimeMachineCardRules {
    const val HOUR_MILLIS = 3_600_000L
    const val THREE_HOURS_MILLIS = 3 * HOUR_MILLIS
    private const val DAY_MILLIS = 86_400_000L

    /** 정렬된 시작 시각들에서 t가 든 칸(시작 ≤ t < 시작 + 길이). 없으면 null. */
    fun bucket(starts: LongArray, lengthMillis: Long, t: Long): Int? {
        var lo = 0
        var hi = starts.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (starts[mid] <= t) lo = mid + 1 else hi = mid
        }
        val i = lo - 1
        if (i < 0 || t >= starts[i] + lengthMillis) return null
        return i
    }

    /** KP: 3시간 예보 칸이 있으면 그 값, 없으면 27일 전망의 그날(UTC) 최대. */
    fun kp(t: Long, threeHour: KpSeries, longTerm: KpSeries): Double? {
        bucket(threeHour.starts, THREE_HOURS_MILLIS, t)?.let { return threeHour.values[it] }
        val day = utcDay(t)
        var best: Double? = null
        for (i in longTerm.starts.indices) {
            if (utcDay(longTerm.starts[i]) == day) best = max(best ?: longTerm.values[i], longTerm.values[i])
        }
        return best
    }

    fun utcDay(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS)

    data class SunEvent(val toSunset: Boolean, val text: String)

    /** 해 다음 이벤트: 일출 전이면 일출까지, 낮이면 일몰까지, 일몰 뒤면 다음 날 일출까지. 시각이 없으면 "--:--". */
    fun sunEvent(t: Long, sunrise: Long?, sunset: Long?, nextSunrise: Long?): SunEvent {
        val daytime: Boolean
        val target: Long?
        if (sunrise != null && sunset != null && sunrise <= t && t < sunset) {
            daytime = true
            target = sunset
        } else if (sunrise != null && t < sunrise) {
            daytime = false
            target = sunrise
        } else {
            daytime = false
            target = nextSunrise
        }
        if (target == null || target <= t) return SunEvent(daytime, "--:--")
        val s = (target - t) / 1000
        return SunEvent(daytime, String.format(Locale.ROOT, "%02d:%02d", s / 3600, (s % 3600) / 60))
    }
}

/** 미리 정렬·파싱한 KP 자료(칸마다 다시 파싱하지 않는다). */
internal class KpSeries(val starts: LongArray, val values: DoubleArray) {
    companion object {
        val Empty = KpSeries(LongArray(0), DoubleArray(0))

        fun of(points: List<Pair<Long, Double>>): KpSeries {
            val sorted = points.sortedBy { it.first }
            return KpSeries(LongArray(sorted.size) { sorted[it].first }, DoubleArray(sorted.size) { sorted[it].second })
        }
    }
}

/**
 * 도형 "이 비행 일정으로 보기" 규칙(DATA_CONTRACT '타임머신' 도형 일정).
 * 비행 기간의 날짜(일 단위 입력, 종료 없음)는 기기 시간대로 읽고, 보는 날·칸은 KST 로 정한다.
 */
internal object TimeMachineShapeSchedule {
    sealed interface Result {
        /** 버튼을 숨긴다(끝났거나 +27일 밖에서 시작). */
        data object Unavailable : Result
        /** 진행 중: 지금 그대로 패널만 펼친다. */
        data object Now : Result
        data class At(val resolved: TimeMachineSlot.Resolved) : Result
    }

    /** 비행 끝: 종료가 없으면 시작 날 다음 날 00:00, 일 단위 입력이면 종료 날 다음 날 00:00, 아니면 종료(날짜는 [zone]). */
    fun flightEnd(startMillis: Long, endMillis: Long?, dateOnly: Boolean, zone: ZoneId): Long {
        fun nextDayStart(millis: Long): Long =
            Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = when {
            endMillis == null -> nextDayStart(startMillis)
            dateOnly -> nextDayStart(endMillis)
            else -> endMillis
        }
        return max(end, startMillis)
    }

    /**
     * @param sunriseMillis 시작이 든 KST 날의 일출(도형 위치). 시작이 기기 시간대 00:00:00이고 일출이 시작 뒤면 일출부터 본다.
     */
    fun resolve(
        nowMillis: Long,
        startMillis: Long,
        endMillis: Long?,
        dateOnly: Boolean,
        sunriseMillis: Long?,
        zone: ZoneId,
    ): Result {
        val end = flightEnd(startMillis, endMillis, dateOnly, zone)
        val today = TimeMachineSlot.dayStart(nowMillis)
        val beyond = TimeMachineSlot.addDays(today, TimeMachineLimits.MAX_DAYS + 1)
        if (end <= nowMillis || startMillis >= beyond) return Result.Unavailable
        if (startMillis <= nowMillis) return Result.Now
        val startLocal = Instant.ofEpochMilli(startMillis).atZone(zone)
        val atMidnight = startLocal.hour == 0 && startLocal.minute == 0 && startLocal.second == 0
        val view = if (atMidnight && sunriseMillis != null && sunriseMillis > startMillis) sunriseMillis else startMillis
        // 10분 칸으로 올림: 그날 칸을 넘으면 다음 날 0칸, 마지막 날을 넘으면 마지막 날 마지막 칸
        val rounded = Math.floorDiv(view + TimeMachineSlot.SLOT_MILLIS - 1, TimeMachineSlot.SLOT_MILLIS) * TimeMachineSlot.SLOT_MILLIS
        val lastDay = TimeMachineSlot.addDays(today, TimeMachineLimits.MAX_DAYS)
        val roundedDay = TimeMachineSlot.dayStart(rounded)
        val (day, slot) = if (roundedDay > lastDay) {
            lastDay to TimeMachineSlot.COUNT - 1
        } else {
            roundedDay to TimeMachineSlot.slotOf(rounded)
        }
        return Result.At(TimeMachineSlot.resolve(nowMillis, day, slot))
    }

    /** 앱의 도형으로 판정한다. 일출은 시작이 든 KST 날, 도형 기준 좌표로 기기 안에서 계산한다. */
    fun resolve(shape: ShapeModel, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Result {
        val sunrise = TimeMachineSun.times(
            shape.flightStartDate,
            shape.baseCoordinate.latitude,
            shape.baseCoordinate.longitude,
        ).sunriseMillis
        return resolve(
            nowMillis = nowMillis,
            startMillis = shape.flightStartDate,
            endMillis = shape.flightEndDate,
            dateOnly = shape.isDateOnlyFlight(TimeZone.getTimeZone(zone)),
            sunriseMillis = sunrise,
            zone = zone,
        )
    }
}
