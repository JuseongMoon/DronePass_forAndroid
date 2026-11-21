package com.ScienceFiction.DronePassAndroid.feature.timemachine

import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.weatherGridCoordinate
import com.ScienceFiction.DronePassAndroid.core.data.repository.TIME_MACHINE_WEATHER_CACHE_MS
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * iOS 공유 fixture `team/fixtures/time-machine-cases.json`(DATA_CONTRACT '타임머신')으로 같은 칸·같은 값을 고르는지 본다.
 * -PrequireCrossPlatformFixtures=true 이면 fixture 가 없을 때 실패한다(타임머신 fixture 는 iOS 타임머신 브랜치에 있다).
 */
class TimeMachineFixtureContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixture(): Map<*, *> {
        val required = System.getProperty("dronepass.requireSharedFixtures") == "true"
        val directory = System.getProperty("dronepass.iosFixtureDirectory")
        val file = directory?.let { File(it, "time-machine-cases.json") }
        if (file == null || !file.isFile) {
            check(!required) { "Shared time machine fixture is missing: $file" }
            assumeTrue("time-machine-cases.json 없음(IOS_PROJECT_DIR 가 iOS 타임머신 브랜치가 아님)", false)
        }
        return requireNotNull(adapter.fromJson(file!!.readText()) as? Map<*, *>)
    }

    private fun Map<*, *>.required(key: String): Any = requireNotNull(this[key]) { "Missing $key" }
    private fun Map<*, *>.string(key: String): String = required(key) as? String ?: error("Invalid $key")
    private fun Map<*, *>.optionalString(key: String): String? = this[key] as? String
    private fun Map<*, *>.number(key: String): Double = (required(key) as? Number)?.toDouble() ?: error("Invalid $key")
    private fun Map<*, *>.int(key: String): Int = number(key).toInt()
    private fun Map<*, *>.map(key: String): Map<*, *> = required(key) as? Map<*, *> ?: error("Invalid $key")
    private fun Map<*, *>.cases(key: String): List<Map<*, *>> =
        (required(key) as? List<*>)?.map { it as? Map<*, *> ?: error("Invalid $key row") } ?: error("Invalid $key")

    private fun instant(value: String): Long = Instant.parse(value).toEpochMilli()
    private fun kstDay(value: String): Long = LocalDate.parse(value).atStartOfDay(TimeMachineZone).toInstant().toEpochMilli()
    private fun kstDate(millis: Long): String = TimeMachineSlot.localDate(millis).toString()
    private fun Map<*, *>.name(): String = string("name")

    @Test
    fun `constants match`() {
        val c = fixture().map("constants")
        assertEquals(TimeMachineZone.id, c.string("dayTimeZone"))
        assertEquals(TimeMachineSlot.MINUTES, c.int("slotMinutes"))
        assertEquals(TimeMachineSlot.COUNT, c.int("slotCount"))
        assertEquals(TimeMachineLimits.MAX_DAYS, c.int("maxDays"))
        assertEquals(TIME_MACHINE_WEATHER_CACHE_MS / 1000, c.int("weatherCacheSeconds").toLong())
        assertEquals(TimeMachineCardRules.HOUR_MILLIS / 1000, c.int("weatherBucketSeconds").toLong())
        assertEquals(TimeMachineCardRules.THREE_HOURS_MILLIS / 1000, c.int("kpThreeHourSeconds").toLong())
        assertEquals(2, c.int("weatherGridDecimals"))
    }

    @Test
    fun `slot cases`() {
        val cases = fixture().cases("slotCases")
        assertTrue(cases.isNotEmpty())
        for (case in cases) {
            val input = case.map("input")
            val expected = case.map("expected")
            // 기기 시간대는 결과에 영향이 없어야 한다(하루는 늘 KST)
            withDefaultZone(input.optionalString("deviceTimeZone")) {
                val r = TimeMachineSlot.resolve(instant(input.string("now")), kstDay(input.string("day")), input.int("slot"))
                assertEquals(case.name(), instant(expected.string("t")), r.timeMillis)
                assertEquals(case.name(), expected.string("day"), kstDate(r.dayStartMillis))
                assertEquals(case.name(), expected.int("slot"), r.slot)
                assertEquals(case.name(), expected.string("label"), r.label)
            }
        }
    }

    @Test
    fun `step day cases`() {
        val cases = fixture().cases("stepDayCases")
        assertTrue(cases.isNotEmpty())
        for (case in cases) {
            val input = case.map("input")
            val allowed = case.map("expected").required("allowed") as Boolean
            assertEquals(
                case.name(),
                allowed,
                TimeMachineSlot.canStepDay(instant(input.string("now")), kstDay(input.string("day")), input.int("delta")),
            )
        }
    }

    @Test
    fun `weather hour cases`() {
        val cases = fixture().cases("weatherHourCases")
        assertTrue(cases.isNotEmpty())
        for (case in cases) {
            val input = case.map("input")
            val starts = (input.required("hourStarts") as List<*>).map { instant(it as String) }.toLongArray()
            val expected = (case.map("expected")["index"] as? Number)?.toInt()
            assertEquals(
                case.name(),
                expected,
                TimeMachineCardRules.bucket(starts, TimeMachineCardRules.HOUR_MILLIS, instant(input.string("t"))),
            )
        }
    }

    private fun series(rows: Any): KpSeries =
        KpSeries.of((rows as List<*>).map { row -> (row as List<*>).let { instant(it[0] as String) to (it[1] as Number).toDouble() } })

    @Test
    fun `kp cases`() {
        val cases = fixture().cases("kpCases")
        assertTrue(cases.isNotEmpty())
        for (case in cases) {
            val input = case.map("input")
            val expected = (case.map("expected")["kp"] as? Number)?.toDouble()
            val actual = TimeMachineCardRules.kp(
                instant(input.string("t")),
                series(input.required("threeHour")),
                series(input.required("longTerm")),
            )
            assertEquals(case.name(), expected, actual)
        }
    }

    @Test
    fun `sun event cases`() {
        val cases = fixture().cases("sunEventCases")
        assertTrue(cases.isNotEmpty())
        for (case in cases) {
            val input = case.map("input")
            val expected = case.map("expected")
            val event = TimeMachineCardRules.sunEvent(
                instant(input.string("t")),
                input.optionalString("sunrise")?.let(::instant),
                input.optionalString("sunset")?.let(::instant),
                input.optionalString("nextSunrise")?.let(::instant),
            )
            assertEquals(case.name(), expected.required("toSunset") as Boolean, event.toSunset)
            assertEquals(case.name(), expected.string("text"), event.text)
        }
    }

    @Test
    fun `schedule cases`() {
        val cases = fixture().cases("scheduleCases")
        assertTrue(cases.isNotEmpty())
        for (case in cases) {
            val input = case.map("input")
            val expected = case.map("expected")
            val result = TimeMachineShapeSchedule.resolve(
                nowMillis = instant(input.string("now")),
                startMillis = instant(input.string("start")),
                endMillis = input.optionalString("end")?.let(::instant),
                dateOnly = input.required("dateOnly") as Boolean,
                sunriseMillis = input.optionalString("sunrise")?.let(::instant),
                zone = ZoneId.of(input.string("deviceTimeZone")),
            )
            when (val kind = expected.string("kind")) {
                "unavailable" -> assertEquals(case.name(), TimeMachineShapeSchedule.Result.Unavailable, result)
                "now" -> assertEquals(case.name(), TimeMachineShapeSchedule.Result.Now, result)
                "at" -> {
                    val r = (result as? TimeMachineShapeSchedule.Result.At)?.resolved ?: error("${case.name()}: $result")
                    assertEquals(case.name(), instant(expected.string("t")), r.timeMillis)
                    assertEquals(case.name(), expected.string("day"), kstDate(r.dayStartMillis))
                    assertEquals(case.name(), expected.int("slot"), r.slot)
                    assertEquals(case.name(), expected.string("label"), r.label)
                }
                else -> error("Unknown kind $kind")
            }
        }
    }

    @Test
    fun `grid cases`() {
        val cases = fixture().cases("gridCases")
        assertTrue(cases.isNotEmpty())
        for (case in cases) {
            val input = case.map("input")
            val expected = case.map("expected")
            val lat = weatherGridCoordinate(input.number("lat"))
            val lon = weatherGridCoordinate(input.number("lon"))
            assertEquals(case.name(), expected.number("requestLat"), lat, 0.0)
            assertEquals(case.name(), expected.number("requestLon"), lon, 0.0)
            assertEquals(case.name(), expected.int("gridLat").toLong(), (lat * 100).roundToLong())
            assertEquals(case.name(), expected.int("gridLon").toLong(), (lon * 100).roundToLong())
        }
    }

    private inline fun withDefaultZone(zone: String?, block: () -> Unit) {
        val saved = java.util.TimeZone.getDefault()
        try {
            zone?.let { java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(it)) }
            block()
        } finally {
            java.util.TimeZone.setDefault(saved)
        }
    }
}
