package com.ScienceFiction.DronePassAndroid.feature.map.tap

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * iOS 기준 구현과 같은 공유 fixture(team/fixtures/map-long-press-cases.json)로 지도 길게 누르기 판정을 확인한다
 * (DATA_CONTRACT "지도 길게 누르기", D-83 (2)). 결과(ignore/shapeMenu/newShape)와 도형·이유가 모두 같아야 한다.
 * 메뉴 문구(menuStrings)도 Android 문구와 같은 뜻인지 본다.
 *
 * 맞춘 fixture: iOS dev 30432b46(처음 나온 커밋 04670354), 14건, SHA-256 앞 16자 e4c21ff36040f152.
 * 사례 수나 내용이 바뀌면 iOS와 함께 이 테스트도 바꾼다.
 */
class MapLongPressContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixtureFile(): File {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "map-long-press-cases.json")
        val missing = "Shared map long press fixture is missing: $file"
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
    private fun Map<*, *>.string(key: String): String = this[key] as? String ?: error("Invalid $key")

    private fun latLng(any: Any?): MapTapLatLng {
        val m = any as? Map<*, *> ?: error("Invalid coordinate")
        return MapTapLatLng(m.double("latitude"), m.double("longitude"))
    }

    private fun fixture(): Map<*, *> {
        val bytes = fixtureFile().readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        println("map-long-press-cases.json sha256 ${sha.take(16)}")
        return requireNotNull(adapter.fromJson(String(bytes)) as? Map<*, *>)
    }

    @Test fun `all map long press cases match the shared fixture`() {
        val json = fixture()
        val constants = json.map("constants")
        assertEquals(MapTapResolver.LINE_TOLERANCE_PT, constants.double("lineTolerancePt"), 0.0)
        assertEquals(MapTapResolver.TIE_M, constants.double("lineTieM"), 0.0)
        assertEquals(MapTapResolver.AREA_TIE_M2, constants.double("areaTieM2"), 0.0)
        assertEquals(MapTapShape.EARTH_RADIUS_M, constants.double("earthRadiusM"), 0.0)
        assertEquals(
            setOf(MapLongPressAction.Ignore.raw, MapLongPressAction.NewShape.raw, "shapeMenu"),
            json.map("actions").keys,
        )

        val cases = json.list("cases").map { it as Map<*, *> }
        assertEquals("사례 수가 바뀌면 iOS와 함께 바꾼다", 14, cases.size)

        cases.forEach { row ->
            val name = row.string("name")
            val press = latLng(row["press"])
            val tolerance = MapTapResolver.LINE_TOLERANCE_PT * row.double("metersPerPoint")
            val candidates = row.list("candidates").map { it as Map<*, *> }.map { c ->
                val id = c.string("id")
                val target = when (c["kind"]) {
                    "shape" -> MapTapTarget.Shape(id)
                    "zone" -> MapTapTarget.Zone(id)
                    else -> error("Unknown kind in $name")
                }
                val order = c.double("drawOrder")
                val shape = when (c["type"]) {
                    "circle" -> MapTapShape.circle(target, order, latLng(c["center"]), c.double("radiusM"))
                    "polygon" -> {
                        val outer = c.list("outer").map(::latLng)
                        val holes = (c["holes"] as? List<*>).orEmpty().map { (it as List<*>).map(::latLng) }
                        requireNotNull(MapTapShape.polygon(target, order, listOf(outer) + holes)) { name }
                    }
                    else -> error("Unknown type in $name")
                }
                MapLongPressCandidate(shape, isDrawn = c["drawn"] as? Boolean ?: error("Invalid drawn in $name"))
            }
            val sketchMode = row["sketchMode"] as? Boolean ?: error("Invalid sketchMode in $name")

            val action = MapLongPressResolver.resolve(press, tolerance, candidates, sketchMode)

            val expected = row.map("expected")
            assertEquals(name, expected.string("action"), action.raw)
            val menu = action as? MapLongPressAction.ShapeMenu
            assertEquals(name, expected["id"] as String?, menu?.shapeId)
            assertEquals(name, expected["reason"] as String?, menu?.reason?.raw)
        }
    }

    @Test fun `menu strings match the shared fixture`() {
        val strings = json(fixture().map("menuStrings"))
        val ko = androidStrings("values")
        val en = androidStrings("values-en")
        // fixture 키 → Android 문구. 제목은 도형 이름 그대로라 새 문구가 없다.
        // '취소'는 확인 창 버튼으로 쓴다(메뉴는 바깥을 누르거나 뒤로 가면 닫혀 취소 항목을 두지 않는다).
        val keys = mapOf(
            "edit" to "map_shape_menu_edit",
            "delete" to "common_delete",
            "newShapeHere" to "map_shape_menu_new_shape_here",
            "cancel" to "common_cancel",
            "deleteConfirmTitle" to "shape_detail_delete_title",
            "deleteConfirmMessage" to "shape_detail_delete_message",
        )
        keys.forEach { (key, resource) ->
            assertEquals("$key ko", strings.getValue(key).getValue("ko"), ko.getValue(resource))
            assertEquals("$key en", strings.getValue(key).getValue("en"), en.getValue(resource))
        }
    }

    /** iOS 서식 문자 %@ 를 Android %s 로 바꿔 비교한다 */
    private fun json(menuStrings: Map<*, *>): Map<String, Map<String, String>> =
        menuStrings.entries.associate { (key, value) ->
            key as String to (value as Map<*, *>).entries.associate { (lang, text) ->
                lang as String to (text as String).replace("%@", "%s")
            }
        }

    /** strings.xml의 name → 값(따옴표 이스케이프를 푼다) */
    private fun androidStrings(folder: String): Map<String, String> {
        val file = listOf("src/main/res/$folder/strings.xml", "app/src/main/res/$folder/strings.xml")
            .map(::File).first { it.isFile }
        return Regex("""<string name="([^"]+)">(.*?)</string>""").findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'").replace("\\\"", "\"") }
    }
}
