package com.ScienceFiction.DronePassAndroid.core.util

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * iOS 기준 구현과 같은 공유 fixture(team/fixtures/sun-display-cases.json)로 일출·일몰 표시 규칙을 확인한다
 * (DATA_CONTRACT "일출·일몰 표시", D-54). 앱처럼 일출·일몰을 기기 시간대의 초 단위 현지 문자열로 바꿔 넣는다.
 */
class SunDisplayContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)
    private val localSecond = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "sun-display-cases.json")
        val missing = "Shared sun display fixture is missing: $file"
        if (System.getProperty("dronepass.requireSharedFixtures") == "true") {
            check(file.isFile) { missing }
        } else {
            assumeTrue(missing, file.isFile)
        }
        return requireNotNull(adapter.fromJson(file.readText()) as? Map<*, *>)
    }

    private fun Map<*, *>.string(key: String): String = this[key] as? String ?: error("Invalid $key")
    private fun Map<*, *>.list(key: String): List<*> = this[key] as? List<*> ?: error("Invalid $key")
    private fun Map<*, *>.map(key: String): Map<*, *> = this[key] as? Map<*, *> ?: error("Invalid $key")

    private fun local(iso: String, zone: ZoneId): LocalDateTime =
        OffsetDateTime.parse(iso).atZoneSameInstant(zone).toLocalDateTime()

    private fun target(name: String?): SunDisplayTarget? = when (name) {
        null -> null
        "todaySunset" -> SunDisplayTarget.TODAY_SUNSET
        "todaySunrise" -> SunDisplayTarget.TODAY_SUNRISE
        "tomorrowSunrise" -> SunDisplayTarget.TOMORROW_SUNRISE
        else -> error("Unknown target $name")
    }

    @Test fun `all sun display cases match the shared fixture`() {
        val cases = fixture().list("sunDisplayCases").map { it as Map<*, *> }
        assertEquals(11, cases.size)
        cases.forEach { row ->
            val zone = ZoneId.of(row.string("timeZone"))
            val days = row.list("days").map { it as Map<*, *> }
            val sunrises = days.mapNotNull { (it["sunrise"] as? String)?.let { iso -> local(iso, zone).format(localSecond) } }
            val sunsets = days.mapNotNull { (it["sunset"] as? String)?.let { iso -> local(iso, zone).format(localSecond) } }
            val expected = row.map("expected")

            val state = sunDisplayState(sunrises, sunsets, local(row.string("now"), zone))

            val name = row.string("name")
            assertEquals(name, expected["isDaytime"] as Boolean?, state.isDaytime)
            assertEquals(name, target(expected["target"] as String?), state.target)
            assertEquals(name, (expected["targetTime"] as String?)?.let { local(it, zone) }, state.targetTime)
            assertEquals(name, expected.string("display"), state.display)
            assertEquals(name, (expected["nextRefresh"] as String?)?.let { local(it, zone) }, state.nextRefresh)
        }
    }
}
