package com.ScienceFiction.DronePassAndroid.feature.timemachine

import com.ScienceFiction.DronePassAndroid.core.data.repository.TimeMachineHour
import com.ScienceFiction.DronePassAndroid.core.data.repository.TimeMachineWeather
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

/** iOS NotamTimeMachineTests.testSetSlotStepDayAndReturn·NotamCardRulesTests.testNowInjectsNothing 와 같은 흐름. */
class TimeMachineClockTest {

    private fun kst(y: Int, m: Int, d: Int, h: Int = 0, mi: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, mi).atZone(TimeMachineZone).toInstant().toEpochMilli()

    private val now = kst(2026, 11, 7, 14, 23)
    private val today = TimeMachineSlot.dayStart(now)

    @Test
    fun `칸 고르기 하루 넘기기 접기 현재로`() {
        val clock = TimeMachineClock { now }
        assertFalse(clock.isFuture)
        assertEquals(87, clock.slot())
        clock.expand()
        assertEquals(TimeMachineClock.Panel.FULL, clock.panel.value)
        // 내일 14:00
        clock.pickDay(TimeMachineSlot.addDays(today, 1))
        clock.setSlot(84)
        assertTrue(clock.isFuture)
        assertEquals(84, clock.selection.value?.slot)
        assertEquals(kst(2026, 11, 8, 14, 0), clock.time())
        assertEquals("D+1", clock.badge())
        // 하루 뒤로 넘기면 시각 유지
        clock.stepDay(1)
        assertEquals(84, clock.selection.value?.slot)
        assertEquals(TimeMachineSlot.addDays(today, 2), clock.selection.value?.dayStartMillis)
        // 오늘보다 앞으로는 못 간다. 오늘로 오면 지금 뒤 첫 칸보다 앞이 아니다
        clock.stepDay(-1)
        clock.stepDay(-1)
        assertFalse(clock.canStepDay(-1))
        clock.stepDay(-1)
        assertEquals(today, clock.selection.value?.dayStartMillis)
        assertTrue(clock.time() >= now)
        // 오늘 지난 칸으로는 가지 않는다
        clock.setSlot(10)
        assertEquals(87, clock.selection.value?.slot)
        // 빈 곳 탭: 미래면 1줄, 다시 동그라미 → 펼침
        clock.collapse()
        assertEquals(TimeMachineClock.Panel.DATE_ONLY, clock.panel.value)
        clock.tapCircle()
        assertEquals(TimeMachineClock.Panel.FULL, clock.panel.value)
        // 현재로
        clock.returnToNow()
        assertFalse(clock.isFuture)
        assertEquals(TimeMachineClock.Panel.HIDDEN, clock.panel.value)
        assertNull(clock.badge())
        // 지금 상태로 펼쳤다가 빈 곳 탭 → 모두 접힘, 두 줄에서 동그라미 → 모두 접힘
        clock.expand()
        clock.collapse()
        assertEquals(TimeMachineClock.Panel.HIDDEN, clock.panel.value)
        clock.tapCircle()
        clock.tapCircle()
        assertEquals(TimeMachineClock.Panel.HIDDEN, clock.panel.value)
    }

    @Test
    fun `27일 끝에서는 하루 뒤로 못 간다`() {
        val clock = TimeMachineClock { now }
        clock.pickDay(TimeMachineSlot.addDays(today, 40))
        assertEquals(TimeMachineSlot.addDays(today, TimeMachineLimits.MAX_DAYS), clock.selection.value?.dayStartMillis)
        assertFalse(clock.canStepDay(1))
        assertTrue(clock.canStepDay(-1))
    }

    private fun shape(start: Long, end: Long?) = ShapeModel(
        id = "s1",
        title = "t",
        baseCoordinate = Coordinate(37.5665, 126.978),
        radius = 100.0,
        flightStartDate = start,
        flightEndDate = end,
    )

    @Test
    fun `도형 일정으로 보기`() {
        val clock = TimeMachineClock { now }
        // 미래 도형 → 그 시각(10분 올림), 패널 펼침
        clock.showShapeSchedule(shape(kst(2026, 11, 9, 13, 5), kst(2026, 11, 9, 15, 0)))
        assertEquals(kst(2026, 11, 9, 13, 10), clock.time())
        assertEquals("s1", clock.selection.value?.shapeId)
        assertEquals(TimeMachineClock.Panel.FULL, clock.panel.value)
        // 진행 중 도형 → 지금 그대로(미래 아님) 패널만 펼침
        clock.collapse()
        clock.showShapeSchedule(shape(kst(2026, 11, 6), kst(2026, 11, 20)))
        assertFalse(clock.isFuture)
        assertEquals(TimeMachineClock.Panel.FULL, clock.panel.value)
        assertFalse(clock.canShowSchedule(shape(kst(2026, 11, 1), kst(2026, 11, 2))))
    }

    @Test
    fun `카드 값 범위 안과 밖`() {
        val start = kst(2026, 11, 8, 9, 0)
        val weather = TimeMachineWeather(
            gridLatitude = 37.57,
            gridLongitude = 126.98,
            hours = (0 until 3).map {
                TimeMachineHour(
                    startMillis = start + it * 3_600_000L,
                    temperature = 10.0 + it,
                    windDirection = 90.0,
                    condition = WeatherCondition.CLEAR,
                    precipitation = 0.0,
                )
            },
        )
        val three = kpThreeHourSeries(listOf(KpIndexData(timeTag = "2026-11-08 00:00:00", kp = 3.33, observed = "predicted")))
        val long = kpLongTermSeries(listOf(Kp27DayForecast(date = "2026 Nov 20", kp = 5.0, ap = 20)))
        // 내일 10:30 KST → 두 번째 칸(10:00), KP 3시간 칸(UTC 00:00~03:00 = KST 09:00~12:00)
        val inside = timeMachineCardValues(
            TimeMachineSelection(TimeMachineSlot.dayStart(start), 63),
            weather, three, long, 37.5665, 126.978,
        )
        assertEquals(11.0, inside.temperature!!, 0.0)
        assertEquals(90.0, inside.windDirection!!, 0.0)
        assertEquals(3.33, inside.kp!!, 0.0)
        assertTrue(inside.sunToSunset)
        // 11/20 → 날씨 범위 밖('–'·화살표 숨김), KP 는 27일 전망, 해 줄은 늘 계산
        val outside = timeMachineCardValues(
            TimeMachineSelection(kst(2026, 11, 20), 60),
            weather, three, long, 37.5665, 126.978,
        )
        assertNull(outside.temperature)
        assertNull(outside.windDirection)
        assertNull(outside.condition)
        assertEquals(5.0, outside.kp!!, 0.0)
        assertNotEquals("--:--", outside.sunText)
        // 자료가 하나도 없어도 해는 계산한다
        val empty = timeMachineCardValues(
            TimeMachineSelection(kst(2026, 12, 1), 30),
            null, KpSeries.Empty, KpSeries.Empty, 37.5665, 126.978,
        )
        assertNull(empty.kp)
        assertNotEquals("--:--", empty.sunText)
    }

    @Test
    fun `KP 시각 형식은 iOS 와 같은 것만 읽는다`() {
        val expected = Instant.parse("2026-11-08T03:00:00Z").toEpochMilli()
        assertEquals(expected, kpTimeTagMillis("2026-11-08 03:00:00"))
        assertEquals(expected, kpTimeTagMillis("2026-11-08 03:00:00.000"))
        assertEquals(expected, kpTimeTagMillis("2026-11-08T03:00:00"))
        assertNull(kpTimeTagMillis("2026-11-08T03:00:00Z"))
        assertEquals(Instant.parse("2026-11-21T00:00:00Z").toEpochMilli(), kp27DayMillis("2026 Nov 21"))
        assertEquals(Instant.parse("2026-12-05T00:00:00Z").toEpochMilli(), kp27DayMillis("2026 Dec 5"))
        assertNull(kp27DayMillis("2026 Foo 5"))
    }
}
