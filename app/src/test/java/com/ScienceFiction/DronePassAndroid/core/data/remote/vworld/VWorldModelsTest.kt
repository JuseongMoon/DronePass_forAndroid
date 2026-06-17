package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class VWorldModelsTest {

    @Test
    fun `레이어 표시명은 iOS Localizable 값과 일치한다`() {
        val expected = mapOf(
            FlightZoneLayer.PROHIBITED to "비행금지구역",
            FlightZoneLayer.TEMPORARY_PROHIBITED to "임시비행금지구역",
            FlightZoneLayer.CONTROL_ZONE to "관제권",
            FlightZoneLayer.RESTRICTED to "비행제한구역",
            FlightZoneLayer.DANGER to "위험지역",
            FlightZoneLayer.ALERT to "경계구역",
            FlightZoneLayer.ATZ to "비행장교통구역",
            FlightZoneLayer.ULTRALIGHT to "초경량비행장치공역",
            FlightZoneLayer.LANDING_FIELD to "경량항공기 이착륙장",
            FlightZoneLayer.OBSTACLE to "장애물공역",
            FlightZoneLayer.PRIOR_CONSULTATION to "사전협의구역",
            FlightZoneLayer.CULTURAL_HERITAGE to "문화재보호구역",
            FlightZoneLayer.NATIONAL_PARK to "국립자연공원"
        )

        assertEquals(expected, FlightZoneLayer.entries.associateWith { it.displayName })
    }

    @Test
    fun `레이어 loadingPriority는 iOS 로딩 순서와 일치한다`() {
        val expected = listOf(
            FlightZoneLayer.PROHIBITED,
            FlightZoneLayer.TEMPORARY_PROHIBITED,
            FlightZoneLayer.CONTROL_ZONE,
            FlightZoneLayer.RESTRICTED,
            FlightZoneLayer.DANGER,
            FlightZoneLayer.ALERT,
            FlightZoneLayer.ATZ,
            FlightZoneLayer.ULTRALIGHT,
            FlightZoneLayer.LANDING_FIELD,
            FlightZoneLayer.OBSTACLE,
            FlightZoneLayer.PRIOR_CONSULTATION,
            FlightZoneLayer.CULTURAL_HERITAGE,
            FlightZoneLayer.NATIONAL_PARK
        )

        assertEquals(expected, FlightZoneLayer.entries.sortedByLoadingPriority())
        assertEquals(expected.mapIndexed { index, _ -> index + 1 }, expected.map { it.loadingPriority })
    }

    @Test
    fun `오버레이 priority는 iOS zIndex와 충돌 판정 우선순위를 유지한다`() {
        assertEquals(1, FlightZoneLayer.PROHIBITED.priority)
        assertEquals(1, FlightZoneLayer.TEMPORARY_PROHIBITED.priority)
        assertEquals(2, FlightZoneLayer.CONTROL_ZONE.priority)
        assertEquals(2, FlightZoneLayer.RESTRICTED.priority)
        assertEquals(2, FlightZoneLayer.DANGER.priority)
        assertEquals(3, FlightZoneLayer.ALERT.priority)
        assertEquals(3, FlightZoneLayer.ATZ.priority)
        assertEquals(3, FlightZoneLayer.OBSTACLE.priority)
        assertEquals(4, FlightZoneLayer.CULTURAL_HERITAGE.priority)
        assertEquals(4, FlightZoneLayer.NATIONAL_PARK.priority)
    }

    @Test
    fun `오버레이 색상은 iOS FlightZoneLayer overlayColor와 borderUIColor를 따른다`() {
        assertEquals(0x4DFF3B30, FlightZoneLayer.PROHIBITED.fillColor)
        assertEquals(0xFFFF3B30, FlightZoneLayer.PROHIBITED.borderColor)
        assertEquals(0x4DFF3B30, FlightZoneLayer.TEMPORARY_PROHIBITED.fillColor)
        assertEquals(0xFFFF3B30, FlightZoneLayer.TEMPORARY_PROHIBITED.borderColor)

        assertEquals(0x4DFF9500, FlightZoneLayer.CONTROL_ZONE.fillColor)
        assertEquals(0xFFFF9500, FlightZoneLayer.CONTROL_ZONE.borderColor)
        assertEquals(0x4DFF9500, FlightZoneLayer.RESTRICTED.fillColor)
        assertEquals(0xFFFF9500, FlightZoneLayer.RESTRICTED.borderColor)
        assertEquals(0x4DFF9500, FlightZoneLayer.DANGER.fillColor)
        assertEquals(0xFFFF9500, FlightZoneLayer.DANGER.borderColor)

        assertEquals(0x4DFFCC00, FlightZoneLayer.ALERT.fillColor)
        assertEquals(0xFFFFCC00, FlightZoneLayer.ALERT.borderColor)
        assertEquals(0x4DFFCC00, FlightZoneLayer.ATZ.fillColor)
        assertEquals(0xFFFFCC00, FlightZoneLayer.ATZ.borderColor)
        assertEquals(0x4DFFCC00, FlightZoneLayer.OBSTACLE.fillColor)
        assertEquals(0xFFFFCC00, FlightZoneLayer.OBSTACLE.borderColor)

        assertEquals(0x4D007AFF, FlightZoneLayer.ULTRALIGHT.fillColor)
        assertEquals(0xFF007AFF, FlightZoneLayer.ULTRALIGHT.borderColor)
        assertEquals(0x4D007AFF, FlightZoneLayer.LANDING_FIELD.fillColor)
        assertEquals(0xFF007AFF, FlightZoneLayer.LANDING_FIELD.borderColor)
        assertEquals(0x4D007AFF, FlightZoneLayer.PRIOR_CONSULTATION.fillColor)
        assertEquals(0xFF007AFF, FlightZoneLayer.PRIOR_CONSULTATION.borderColor)

        assertEquals(0x4D34C759, FlightZoneLayer.CULTURAL_HERITAGE.fillColor)
        assertEquals(0xFF34C759, FlightZoneLayer.CULTURAL_HERITAGE.borderColor)
        assertEquals(0x4D34C759, FlightZoneLayer.NATIONAL_PARK.fillColor)
        assertEquals(0xFF34C759, FlightZoneLayer.NATIONAL_PARK.borderColor)
    }

    @Test
    fun `비행 제한 수준은 iOS FlightZoneLayer restrictionLevel과 일치한다`() {
        assertEquals(FlightRestrictionLevel.PROHIBITED, FlightZoneLayer.PROHIBITED.restrictionLevel)
        assertEquals(FlightRestrictionLevel.PROHIBITED, FlightZoneLayer.TEMPORARY_PROHIBITED.restrictionLevel)

        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.CONTROL_ZONE.restrictionLevel)
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.RESTRICTED.restrictionLevel)
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.DANGER.restrictionLevel)
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.ALERT.restrictionLevel)
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.ATZ.restrictionLevel)
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.OBSTACLE.restrictionLevel)

        assertEquals(FlightRestrictionLevel.CONSULTATION, FlightZoneLayer.ULTRALIGHT.restrictionLevel)
        assertEquals(FlightRestrictionLevel.CONSULTATION, FlightZoneLayer.LANDING_FIELD.restrictionLevel)
        assertEquals(FlightRestrictionLevel.CONSULTATION, FlightZoneLayer.PRIOR_CONSULTATION.restrictionLevel)

        assertEquals(FlightRestrictionLevel.ADVISORY, FlightZoneLayer.CULTURAL_HERITAGE.restrictionLevel)
        assertEquals(FlightRestrictionLevel.ADVISORY, FlightZoneLayer.NATIONAL_PARK.restrictionLevel)
    }

    @Test
    fun `구역 코드는 iOS와 같은 레이어별 필드에서 추출된다`() {
        assertEquals(
            "RK P73A",
            FlightZoneLayer.PROHIBITED.resolveZoneCode(
                featureId = "lt_c_aisprhc.1",
                properties = mapOf("prh_lbl_1" to "RK P73A")
            )
        )
        assertEquals(
            "김포 CTR",
            FlightZoneLayer.CONTROL_ZONE.resolveZoneCode(
                featureId = "lt_c_aisctrc.1",
                properties = mapOf("ctr_lbl_1" to "김포 CTR")
            )
        )
        assertEquals(
            "R75",
            FlightZoneLayer.RESTRICTED.resolveZoneCode(
                featureId = "lt_c_aisresc.1",
                properties = mapOf("res_lbl_1" to "R75")
            )
        )
        assertEquals(
            "A1",
            FlightZoneLayer.ALERT.resolveZoneCode(
                featureId = "lt_c_aisaltc.1",
                properties = mapOf("alt_lbl_1" to "A1")
            )
        )
        assertEquals(
            "초경량 공역",
            FlightZoneLayer.ULTRALIGHT.resolveZoneCode(
                featureId = "lt_c_aisuac.1",
                properties = mapOf("name_txt" to "초경량 공역")
            )
        )
        assertEquals(
            "서울지방항공청",
            FlightZoneLayer.PRIOR_CONSULTATION.resolveZoneCode(
                featureId = "lt_c_aispca.1",
                properties = mapOf("nm_kor" to "서울지방항공청")
            )
        )
        assertEquals(
            "경복궁",
            FlightZoneLayer.CULTURAL_HERITAGE.resolveZoneCode(
                featureId = "lt_c_uo301.1",
                properties = mapOf("alias" to "경복궁")
            )
        )
    }

    @Test
    fun `구역 코드 문자열 파싱은 iOS as String 처럼 빈 문자열을 보존하고 숫자를 거부한다`() {
        assertEquals(
            "",
            FlightZoneLayer.ULTRALIGHT.resolveZoneCode(
                featureId = "lt_c_aisuac.1",
                properties = mapOf(
                    "uac_lbl_1" to "",
                    "name_txt" to "fallback name",
                )
            )
        )
        assertNull(
            FlightZoneLayer.PROHIBITED.resolveZoneCode(
                featureId = "lt_c_aisprhc.1",
                properties = mapOf("prh_lbl_1" to 73)
            )
        )
    }

    @Test
    fun `국립공원은 iOS처럼 Feature ID로 이름을 매핑한다`() {
        assertEquals(
            "북한산국립공원사무소",
            FlightZoneLayer.NATIONAL_PARK.resolveZoneCode(
                featureId = "lt_c_wgisnpgug.30",
                properties = emptyMap()
            )
        )
        assertEquals(
            "한려해상국립공원사무소",
            FlightZoneLayer.NATIONAL_PARK.resolveZoneCode(
                featureId = "lt_c_wgisnpgug.93",
                properties = emptyMap()
            )
        )
        assertNull(
            FlightZoneLayer.NATIONAL_PARK.resolveZoneCode(
                featureId = "lt_c_wgisnpgug.999",
                properties = emptyMap()
            )
        )
    }

    @Test
    fun `iOS가 코드 행을 표시하지 않는 기본 레이어는 null을 유지한다`() {
        assertNull(
            FlightZoneLayer.ATZ.resolveZoneCode(
                featureId = "lt_c_aisatzc.1",
                properties = mapOf("atz_lbl_1" to "ATZ")
            )
        )
        assertNull(
            FlightZoneLayer.LANDING_FIELD.resolveZoneCode(
                featureId = "lt_c_aisfldc.1",
                properties = mapOf("fld_lbl_1" to "이착륙장")
            )
        )
        assertNull(
            FlightZoneLayer.OBSTACLE.resolveZoneCode(
                featureId = "lt_c_aisobls.1",
                properties = mapOf("obs_lbl_1" to "장애물")
            )
        )
    }

    @Test
    fun `상세 중심 좌표는 렌더 가능한 폴리곤이 없어도 iOS geometry center를 사용한다`() {
        val zone = DroneZoneFeature(
            id = "line.1",
            layer = FlightZoneLayer.ATZ,
            polygons = emptyList(),
            zoneCode = null,
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "LineString 구역",
            geometryCenterCoordinate = 37.25 to 126.75,
        )

        assertEquals(37.25 to 126.75, zone.centerCoordinate)
        assertEquals("37.2500° N, 126.7500° E", formatCoordinate(37.25, 126.75))
        assertEquals("37.2500° S, 126.7500° W", formatCoordinate(-37.25, -126.75))
    }

    @Test
    fun `NOTAM two digit years are parsed as 2000 based years like iOS`() {
        val zone = DroneZoneFeature(
            id = "notam.1",
            layer = FlightZoneLayer.TEMPORARY_PROHIBITED,
            polygons = emptyList(),
            zoneCode = "RK TEST",
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "임시비행금지구역",
            properties = mapOf("notam" to "A)RKRR B)9912311500 C)9912311459")
        )

        val start = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = zone.notamStartDate!!
        }
        val end = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = zone.notamEndDate!!
        }

        assertEquals(2099, start.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, start.get(Calendar.MONTH))
        assertEquals(31, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(2099, end.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, end.get(Calendar.MONTH))
        assertEquals(31, end.get(Calendar.DAY_OF_MONTH))
    }
}
