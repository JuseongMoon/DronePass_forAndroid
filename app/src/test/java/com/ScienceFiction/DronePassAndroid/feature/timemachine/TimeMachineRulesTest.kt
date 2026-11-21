package com.ScienceFiction.DronePassAndroid.feature.timemachine

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.abs

/** iOS NotamTimeMachineTests·NotamCardRulesTests 와 같은 경우를 같은 값으로 확인한다. */
class TimeMachineRulesTest {

    private fun kst(y: Int, m: Int, d: Int, h: Int = 0, mi: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, mi).atZone(TimeMachineZone).toInstant().toEpochMilli()

    private fun utc(s: String): Long = Instant.parse(s).toEpochMilli()

    private fun minutesOfDay(millis: Long): Int {
        val t = Instant.ofEpochMilli(millis).atZone(TimeMachineZone)
        return t.hour * 60 + t.minute
    }

    @Test
    fun `서울 일출·일몰과 극야·백야`() {
        // 서울 2026-11-09(월) 일출 약 07:04, 일몰 약 17:27 KST(±3분)
        val sun = TimeMachineSun.times(kst(2026, 11, 9), 37.5665, 126.978)
        assertTrue(abs(minutesOfDay(sun.sunriseMillis!!) - (7 * 60 + 4)) <= 3)
        assertTrue(abs(minutesOfDay(sun.sunsetMillis!!) - (17 * 60 + 27)) <= 3)
        assertNull(sun.polar)
        val night = TimeMachineSun.times(kst(2026, 12, 21), 78.2, 15.6)
        assertNull(night.sunriseMillis)
        assertEquals(TimeMachineSun.Polar.NIGHT, night.polar)
        val white = TimeMachineSun.times(kst(2026, 6, 21), 78.2, 15.6)
        assertEquals(TimeMachineSun.Polar.DAY, white.polar)
        assertEquals(0 to TimeMachineSlot.COUNT, TimeMachineSun.slots(white))
        assertEquals(null to null, TimeMachineSun.slots(night))
    }

    @Test
    fun `칸 시각과 동그라미 문구`() {
        val now = kst(2026, 11, 7, 14, 23)
        assertEquals("+10m", TimeMachineSlot.label(kst(2026, 11, 7, 14, 30), now))
        assertEquals("+3h", TimeMachineSlot.label(kst(2026, 11, 7, 17, 0), now))
        assertEquals("D+2", TimeMachineSlot.label(kst(2026, 11, 9, 9, 0), now))
        assertEquals("D+1", TimeMachineSlot.label(kst(2026, 11, 8, 0, 0), now))
        // 지금이 든 칸(14:20)은 now, 지난 칸은 −(U+2212)
        assertEquals("now", TimeMachineSlot.label(kst(2026, 11, 7, 14, 20), now))
        assertEquals("\u221210m", TimeMachineSlot.label(kst(2026, 11, 7, 14, 10), now))
        assertEquals("\u221214h", TimeMachineSlot.label(kst(2026, 11, 7, 0, 0), now))
        // 자정을 넘겨 어제 시각이 남아 있으면 D−1
        assertEquals("D\u22121", TimeMachineSlot.label(kst(2026, 11, 6, 23, 50), kst(2026, 11, 7, 0, 5)))
        assertEquals(TimeMachineSlot.Tense.NOW, TimeMachineSlot.tense(kst(2026, 11, 7, 14, 20), now))
        assertEquals(TimeMachineSlot.Tense.PAST, TimeMachineSlot.tense(kst(2026, 11, 7, 14, 10), now))
        assertEquals(TimeMachineSlot.Tense.FUTURE, TimeMachineSlot.tense(kst(2026, 11, 7, 14, 30), now))
        assertEquals(87, TimeMachineSlot.slotOf(kst(2026, 11, 9, 14, 37)))
        assertEquals(88, TimeMachineSlot.slotRoundingUp(kst(2026, 11, 9, 14, 37)))
        assertEquals(87, TimeMachineSlot.slotRoundingUp(kst(2026, 11, 9, 14, 30)))
        assertEquals(kst(2026, 11, 9, 14, 30), TimeMachineSlot.time(kst(2026, 11, 9, 3, 0), 87))
    }

    @Test
    fun `KST 하루 경계는 UTC 15시다`() {
        // UTC 11/8 15:00 = KST 11/9 00:00
        assertEquals(utc("2026-11-08T15:00:00Z"), TimeMachineSlot.dayStart(utc("2026-11-09T14:59:59Z")))
        assertEquals(utc("2026-11-09T15:00:00Z"), TimeMachineSlot.dayStart(utc("2026-11-09T15:00:00Z")))
    }

    @Test
    fun `오늘 0시부터 27일 뒤까지 고른다`() {
        val now = kst(2026, 11, 7, 14, 23)
        val today = TimeMachineSlot.dayStart(now)
        // 오늘 지난 칸도 그대로 고른다(D-87)
        val past = TimeMachineSlot.resolve(now, today, 10)
        assertEquals(10, past.slot)
        assertEquals(today, past.dayStartMillis)
        assertEquals("\u221213h", past.label)
        // 어제 → 오늘 같은 칸
        val yesterday = TimeMachineSlot.resolve(now, TimeMachineSlot.addDays(today, -1), 100)
        assertEquals(today, yesterday.dayStartMillis)
        assertEquals(100, yesterday.slot)
        // +27일까지, 그 뒤는 +27일로 자른다
        val last = TimeMachineSlot.addDays(today, TimeMachineLimits.MAX_DAYS)
        assertEquals(last, TimeMachineSlot.resolve(now, TimeMachineSlot.addDays(today, 40), 5).dayStartMillis)
        assertEquals("D+27", TimeMachineSlot.resolve(now, last, 0).label)
        // 칸 범위
        assertEquals(TimeMachineSlot.COUNT - 1, TimeMachineSlot.resolve(now, last, 999).slot)
        assertEquals(0, TimeMachineSlot.resolve(now, last, -3).slot)
        // 오늘 23:55에도 오늘 23:50 칸은 지금이 든 칸(내일로 넘기지 않는다)
        val late = kst(2026, 11, 7, 23, 55)
        val r = TimeMachineSlot.resolve(late, late, 143)
        assertEquals(today, r.dayStartMillis)
        assertEquals("now", r.label)
        // 하루 넘기기는 오늘에서 어제로 가지 않는다
        assertFalse(TimeMachineSlot.canStepDay(now, today, -1))
        assertTrue(TimeMachineSlot.canStepDay(now, today, 1))
    }

    @Test
    fun `KP 3시간 칸은 끝이 오늘 0시 뒤인 것만 쓴다`() {
        val now = kst(2026, 11, 21, 14, 23)
        val rows = listOf(
            com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData(timeTag = "2026-11-20 12:00:00", kp = 5.0, observed = "observed"),
            com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData(timeTag = "2026-11-20 15:00:00", kp = 1.67, observed = "observed"),
            com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData(timeTag = "2026-11-21 06:00:00", kp = 3.67, observed = "predicted"),
        )
        val series = kpThreeHourSeries(rows, now)
        // UTC 11/20 12:00~15:00 칸은 KST 11/20 21:00~24:00 — 끝이 오늘 0시와 같아 뺀다
        assertEquals(listOf(utc("2026-11-20T15:00:00Z"), utc("2026-11-21T06:00:00Z")), series.starts.toList())
        assertEquals(1.67, TimeMachineCardRules.kp(kst(2026, 11, 21, 0, 0), series, KpSeries.Empty)!!, 0.0)
    }

    @Test
    fun `정시 칸 경계`() {
        val starts = LongArray(3) { utc("2026-11-09T0$it:00:00Z") }
        val hour = TimeMachineCardRules.HOUR_MILLIS
        assertEquals(0, TimeMachineCardRules.bucket(starts, hour, utc("2026-11-09T00:00:00Z")))
        assertEquals(0, TimeMachineCardRules.bucket(starts, hour, utc("2026-11-09T00:59:59Z")))
        assertEquals(1, TimeMachineCardRules.bucket(starts, hour, utc("2026-11-09T01:00:00Z")))
        assertNull("범위 밖(마지막 칸 끝)", TimeMachineCardRules.bucket(starts, hour, utc("2026-11-09T03:00:00Z")))
        assertNull("범위 앞", TimeMachineCardRules.bucket(starts, hour, utc("2026-11-08T23:59:00Z")))
        assertNull(TimeMachineCardRules.bucket(LongArray(0), hour, utc("2026-11-09T00:00:00Z")))
    }

    @Test
    fun `KP는 3시간 칸 다음 27일 전망 UTC 날짜`() {
        val three = KpSeries.of(listOf(utc("2026-11-09T00:00:00Z") to 2.0, utc("2026-11-09T03:00:00Z") to 4.33))
        val long = KpSeries.of(
            listOf(
                utc("2026-11-09T00:00:00Z") to 3.0,
                utc("2026-11-12T00:00:00Z") to 5.0,
                utc("2026-11-12T12:00:00Z") to 6.0,
            ),
        )
        assertEquals(2.0, TimeMachineCardRules.kp(utc("2026-11-09T02:59:00Z"), three, long)!!, 0.0)
        assertEquals("3시간 칸 경계", 4.33, TimeMachineCardRules.kp(utc("2026-11-09T03:00:00Z"), three, long)!!, 0.0)
        assertEquals(6.0, TimeMachineCardRules.kp(utc("2026-11-12T23:59:00Z"), three, long)!!, 0.0)
        // KST 11/13 08:00 = UTC 11/12 23:00 → UTC 날짜 기준 11/12
        assertEquals(6.0, TimeMachineCardRules.kp(utc("2026-11-12T23:00:00Z"), three, long)!!, 0.0)
        assertNull("27일 뒤 → –", TimeMachineCardRules.kp(utc("2026-12-31T00:00:00Z"), three, long))
        assertNull(TimeMachineCardRules.kp(utc("2026-11-09T00:00:00Z"), KpSeries.Empty, KpSeries.Empty))
    }

    @Test
    fun `해 다음 이벤트`() {
        val rise = utc("2026-11-08T22:04:00Z")
        val set = utc("2026-11-09T08:27:00Z")
        val next = utc("2026-11-09T22:05:00Z")
        val before = TimeMachineCardRules.sunEvent(utc("2026-11-08T20:00:00Z"), rise, set, next)
        assertFalse(before.toSunset)
        assertEquals("일출 전 → 일출까지", "02:04", before.text)
        val day = TimeMachineCardRules.sunEvent(utc("2026-11-09T05:00:00Z"), rise, set, next)
        assertTrue(day.toSunset)
        assertEquals("낮 → 일몰까지", "03:27", day.text)
        val night = TimeMachineCardRules.sunEvent(utc("2026-11-09T10:00:00Z"), rise, set, next)
        assertFalse(night.toSunset)
        assertEquals("일몰 뒤 → 다음 날 일출까지", "12:05", night.text)
        assertFalse("일몰 시각 = 밤", TimeMachineCardRules.sunEvent(set, rise, set, next).toSunset)
        assertEquals("--:--", TimeMachineCardRules.sunEvent(set, rise, set, null).text)
    }

    private fun shape(start: Long, end: Long?) = ShapeModel(
        title = "t",
        baseCoordinate = Coordinate(37.5665, 126.978),
        radius = 100.0,
        flightStartDate = start,
        flightEndDate = end,
    )

    @Test
    fun `도형 일정 시작 시각`() {
        val now = kst(2026, 11, 7, 14, 23)
        fun at(shape: ShapeModel): Long =
            (TimeMachineShapeSchedule.resolve(shape, now, TimeMachineZone) as TimeMachineShapeSchedule.Result.At).resolved.timeMillis
        // 시각까지 있는 도형 → 그 시각
        assertEquals(kst(2026, 11, 9, 13, 0), at(shape(kst(2026, 11, 9, 13, 0), kst(2026, 11, 9, 15, 0))))
        // 날짜만 → 그날 일출(서울 11/9 약 07:04) → 칸 올림 07:10
        assertEquals(kst(2026, 11, 9, 7, 10), at(shape(kst(2026, 11, 9), kst(2026, 11, 9, 23, 59))))
        // 진행 중 → 지금, 끝난 도형·27일 넘는 도형은 버튼 없음
        assertEquals(
            TimeMachineShapeSchedule.Result.Now,
            TimeMachineShapeSchedule.resolve(shape(kst(2026, 11, 5), kst(2026, 11, 20)), now, TimeMachineZone),
        )
        assertEquals(
            TimeMachineShapeSchedule.Result.Unavailable,
            TimeMachineShapeSchedule.resolve(shape(kst(2026, 11, 1), kst(2026, 11, 2)), now, TimeMachineZone),
        )
        assertEquals(
            TimeMachineShapeSchedule.Result.Unavailable,
            TimeMachineShapeSchedule.resolve(shape(kst(2026, 12, 5, 9, 0), kst(2026, 12, 5, 10, 0)), now, TimeMachineZone),
        )
        assertEquals(kst(2026, 12, 4, 9, 0), at(shape(kst(2026, 12, 4, 9, 0), kst(2026, 12, 4, 10, 0))))
        // 종료가 없는 도형은 만료되지 않는다(지난달 시작이어도 진행 중)
        assertEquals(
            TimeMachineShapeSchedule.Result.Now,
            TimeMachineShapeSchedule.resolve(shape(kst(2026, 10, 1, 9, 0), null), now, TimeMachineZone),
        )
    }
}
