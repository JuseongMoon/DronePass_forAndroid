package com.ScienceFiction.DronePassAndroid.feature.map.tap

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * iOS 기준 구현과 같은 공유 fixture(team/fixtures/map-tap-cases.json)로 지도 탭 판정을 확인한다
 * (DATA_CONTRACT "지도 탭 판정", 2026-11-07). 대상과 이유(line/inside/none)가 모두 같아야 한다.
 *
 * 맞춘 fixture: iOS 7d8fa898, 17건, SHA-256 앞 16자 cc2e0e4139b3e9c7.
 * 사례 수가 바뀌면 iOS와 함께 이 테스트도 바꾼다.
 */
class MapTapContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixtureFile(): File? {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "map-tap-cases.json")
        val missing = "Shared map tap fixture is missing: $file"
        if (System.getProperty("dronepass.requireSharedFixtures") == "true") {
            check(file.isFile) { missing }
        } else {
            assumeTrue(missing, file.isFile)
        }
        return file
    }

    private fun Map<*, *>.double(key: String): Double = (this[key] as? Number)?.toDouble() ?: error("Invalid $key")
    private fun Map<*, *>.list(key: String): List<*> = this[key] as? List<*> ?: error("Invalid $key")
    private fun Map<*, *>.map(key: String): Map<*, *> = this[key] as? Map<*, *> ?: error("Invalid $key")

    private fun latLng(any: Any?): MapTapLatLng {
        val m = any as? Map<*, *> ?: error("Invalid coordinate")
        return MapTapLatLng(m.double("latitude"), m.double("longitude"))
    }

    @Test fun `all map tap cases match the shared fixture`() {
        val file = requireNotNull(fixtureFile())
        val bytes = file.readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        println("map-tap-cases.json sha256 ${sha.take(16)}")
        val json = requireNotNull(adapter.fromJson(String(bytes)) as? Map<*, *>)

        val constants = json.map("constants")
        assertEquals(MapTapResolver.LINE_TOLERANCE_PT, constants.double("lineTolerancePt"), 0.0)
        assertEquals(MapTapResolver.TIE_M, constants.double("lineTieM"), 0.0)
        assertEquals(MapTapResolver.AREA_TIE_M2, constants.double("areaTieM2"), 0.0)
        assertEquals(MapTapShape.EARTH_RADIUS_M, constants.double("earthRadiusM"), 0.0)

        val cases = json.list("cases").map { it as Map<*, *> }
        assertEquals("사례 수가 바뀌면 iOS와 함께 바꾼다", 17, cases.size)

        cases.forEach { row ->
            val name = row["name"] as? String ?: error("Invalid name")
            val tap = latLng(row["tap"])
            val tolerance = MapTapResolver.LINE_TOLERANCE_PT * row.double("metersPerPoint")
            val shapes = row.list("candidates").map { it as Map<*, *> }.map { c ->
                val target = MapTapTarget.Shape(c["id"] as? String ?: error("Invalid id in $name"))
                val order = c.double("drawOrder")
                when (c["type"]) {
                    "circle" -> MapTapShape.circle(target, order, latLng(c["center"]), c.double("radiusM"))
                    "polygon" -> {
                        val outer = c.list("outer").map(::latLng)
                        val holes = c.list("holes").map { (it as List<*>).map(::latLng) }
                        requireNotNull(MapTapShape.polygon(target, order, listOf(outer) + holes)) { name }
                    }
                    else -> error("Unknown type in $name")
                }
            }
            val expected = row.map("expected")

            // 색인 경로와 목록 경로가 같은 답
            listOf(
                MapTapResolver.resolve(tap, tolerance, MapTapIndex(shapes)),
                MapTapResolver.resolve(tap, tolerance, null, shapes),
            ).forEach { result ->
                assertEquals(name, expected["id"] as String?, (result.target as MapTapTarget.Shape?)?.id)
                assertEquals(name, expected["reason"] as String, result.reason.raw)
            }
        }
    }
}
