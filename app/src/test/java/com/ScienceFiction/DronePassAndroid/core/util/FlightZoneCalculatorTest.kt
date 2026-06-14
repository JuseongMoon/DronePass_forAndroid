package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightRestrictionLevel
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FlightZoneCalculator 단위 테스트
 *
 * Ray Casting 알고리즘의 정확성을 검증한다.
 * iOS 원본(FlightZoneCalculator.swift) 결과와 일치해야 한다.
 *
 * 안전 기준: 비행구역 오판 0건. 본 테스트가 실패하면 출시 금지.
 */
class FlightZoneCalculatorTest {

    @Test
    fun `비행구역 거리 계산은 iOS FlightZoneCalculator WGS84 반지름을 유지한다`() {
        assertEquals(
            111.31949079327357,
            FlightZoneCalculator.distance(
                lat1 = 37.0,
                lon1 = 127.0,
                lat2 = 37.001,
                lon2 = 127.0,
            ),
            0.000001,
        )
    }

    // region 정사각형 폴리곤 (위도 37~38, 경도 126~127) ─ 가장 단순한 경계 검증

    /** 정사각형 4꼭짓점: (lat, lon) = (37,126), (37,127), (38,127), (38,126) */
    private val unitSquare = listOf(
        Pair(37.0, 126.0),
        Pair(37.0, 127.0),
        Pair(38.0, 127.0),
        Pair(38.0, 126.0)
    )

    @Test
    fun `정사각형 중앙 점은 내부로 판정된다`() {
        val inside = FlightZoneCalculator.isPointInPolygon(
            lat = 37.5,
            lon = 126.5,
            polygon = unitSquare
        )
        assertTrue("정사각형 중앙 (37.5, 126.5)은 내부여야 함", inside)
    }

    @Test
    fun `정사각형 외부의 점은 외부로 판정된다 - 북쪽`() {
        assertFalse(
            FlightZoneCalculator.isPointInPolygon(
                lat = 39.0,
                lon = 126.5,
                polygon = unitSquare
            )
        )
    }

    @Test
    fun `정사각형 외부의 점은 외부로 판정된다 - 남쪽`() {
        assertFalse(
            FlightZoneCalculator.isPointInPolygon(
                lat = 36.0,
                lon = 126.5,
                polygon = unitSquare
            )
        )
    }

    @Test
    fun `정사각형 외부의 점은 외부로 판정된다 - 동쪽`() {
        assertFalse(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.5,
                lon = 128.0,
                polygon = unitSquare
            )
        )
    }

    @Test
    fun `정사각형 외부의 점은 외부로 판정된다 - 서쪽`() {
        assertFalse(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.5,
                lon = 125.0,
                polygon = unitSquare
            )
        )
    }

    @Test
    fun `정사각형 내부 - 우측 가장자리 근처`() {
        assertTrue(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.5,
                lon = 126.9999,
                polygon = unitSquare
            )
        )
    }

    @Test
    fun `정사각형 내부 - 좌측 가장자리 근처`() {
        assertTrue(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.5,
                lon = 126.0001,
                polygon = unitSquare
            )
        )
    }

    // endregion

    // region 비정상 입력 방어

    @Test
    fun `점이 3개 미만이면 항상 false`() {
        val emptyPolygon = emptyList<Pair<Double, Double>>()
        val singlePoint = listOf(Pair(37.5, 126.5))
        val twoPoints = listOf(Pair(37.0, 126.0), Pair(38.0, 127.0))

        assertFalse(FlightZoneCalculator.isPointInPolygon(37.5, 126.5, emptyPolygon))
        assertFalse(FlightZoneCalculator.isPointInPolygon(37.5, 126.5, singlePoint))
        assertFalse(FlightZoneCalculator.isPointInPolygon(37.5, 126.5, twoPoints))
    }

    // endregion

    // region 볼록 폴리곤 (오각형)

    /**
     * 볼록 오각형: 서울 강남구 인근 가상 영역
     * (37.5, 127.0), (37.51, 127.02), (37.5, 127.04), (37.49, 127.03), (37.49, 127.01)
     */
    private val pentagon = listOf(
        Pair(37.500, 127.000),
        Pair(37.510, 127.020),
        Pair(37.500, 127.040),
        Pair(37.490, 127.030),
        Pair(37.490, 127.010)
    )

    @Test
    fun `오각형 중앙 점은 내부`() {
        assertTrue(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.500,
                lon = 127.020,
                polygon = pentagon
            )
        )
    }

    @Test
    fun `오각형 바깥 점은 외부`() {
        assertFalse(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.520,
                lon = 127.020,
                polygon = pentagon
            )
        )
    }

    // endregion

    // region 비볼록(concave) 폴리곤 - L자 모양

    /**
     * L자 모양 폴리곤 (반시계방향):
     *   (37,126) → (37,128) → (37.5,128) → (37.5,127) → (38,127) → (38,126) → (37,126)
     *
     * L자의 안쪽 빈 영역(우측 상단)이 외부로 판정되어야 한다.
     */
    private val lShape = listOf(
        Pair(37.0, 126.0),
        Pair(37.0, 128.0),
        Pair(37.5, 128.0),
        Pair(37.5, 127.0),
        Pair(38.0, 127.0),
        Pair(38.0, 126.0)
    )

    @Test
    fun `L자 폴리곤 - 하단 영역은 내부`() {
        assertTrue(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.25,
                lon = 127.0,
                polygon = lShape
            )
        )
    }

    @Test
    fun `L자 폴리곤 - 좌측 영역은 내부`() {
        assertTrue(
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.75,
                lon = 126.5,
                polygon = lShape
            )
        )
    }

    @Test
    fun `L자 폴리곤 - 오목한 빈 영역(우측 상단)은 외부`() {
        assertFalse(
            "L자의 오목한 빈 영역은 외부로 판정되어야 함",
            FlightZoneCalculator.isPointInPolygon(
                lat = 37.75,
                lon = 127.5,
                polygon = lShape
            )
        )
    }

    // endregion

    // region 실제 비행 안전 시나리오 (잠실 인근 가상 비행금지구역)

    /**
     * 가상 비행금지 구역 (잠실 종합운동장 일대):
     * 위도 37.510~37.520, 경도 127.060~127.080 사각형
     */
    private val jamsilProhibitedZone = listOf(
        Pair(37.510, 127.060),
        Pair(37.510, 127.080),
        Pair(37.520, 127.080),
        Pair(37.520, 127.060)
    )

    @Test
    fun `잠실 비행금지 가상 구역 - 중심점은 내부(비행불가)`() {
        // 비행금지구역 정중앙
        val inside = FlightZoneCalculator.isPointInPolygon(
            lat = 37.515,
            lon = 127.070,
            polygon = jamsilProhibitedZone
        )
        assertTrue("비행금지 구역 중앙이 외부로 판정되면 안전 기준 위반(false negative)", inside)
    }

    @Test
    fun `잠실 비행금지 가상 구역 - 충분히 떨어진 외부점은 외부(비행가능)`() {
        // 구역에서 약 10km 떨어진 강남
        val outside = FlightZoneCalculator.isPointInPolygon(
            lat = 37.500,
            lon = 127.040,
            polygon = jamsilProhibitedZone
        )
        assertFalse("비행금지 구역 외부가 내부로 판정되면 false positive", outside)
    }

    // endregion

    // region iOS 비행 제한 수준 정합

    @Test
    fun `경계 교통 장애물 구역은 iOS처럼 비행제한으로 분류된다`() {
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.ALERT.restrictionLevel)
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.ATZ.restrictionLevel)
        assertEquals(FlightRestrictionLevel.RESTRICTED, FlightZoneLayer.OBSTACLE.restrictionLevel)
    }

    @Test
    fun `경계구역 내부는 허가 없는 비행 불가로 판정된다`() {
        val zone = DroneZoneFeature(
            id = "alert-zone",
            layer = FlightZoneLayer.ALERT,
            polygons = listOf(unitSquare),
            zoneCode = "A1",
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "경계구역"
        )

        val result = FlightZoneCalculator.checkFlightPermission(
            lat = 37.5,
            lon = 126.5,
            zones = listOf(zone)
        )

        assertFalse(result.canFly)
        assertEquals(FlightRestrictionLevel.RESTRICTED, result.level)
        assertEquals("경계구역 구역입니다. 비행 승인이 필요합니다.", result.message)
        assertEquals(listOf("경계구역: A1"), result.details)
    }

    @Test
    fun `사전협의구역 내부는 iOS처럼 비행 승인이 필요한 비행불가로 판정된다`() {
        val zone = DroneZoneFeature(
            id = "consultation-zone",
            layer = FlightZoneLayer.PRIOR_CONSULTATION,
            polygons = listOf(unitSquare),
            zoneCode = "서울지방항공청",
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "사전협의구역"
        )

        val result = FlightZoneCalculator.checkFlightPermission(
            lat = 37.5,
            lon = 126.5,
            zones = listOf(zone)
        )

        assertFalse(result.canFly)
        assertEquals(FlightRestrictionLevel.RESTRICTED, result.level)
        assertEquals("사전협의구역 구역입니다. 비행 승인이 필요합니다.", result.message)
        assertEquals(listOf("사전협의구역: 서울지방항공청"), result.details)
    }

    @Test
    fun `비행 가능 판정 details 는 iOS처럼 비어 있다`() {
        val result = FlightZoneCalculator.checkFlightPermission(
            lat = 39.0,
            lon = 126.5,
            zones = emptyList(),
        )

        assertTrue(result.canFly)
        assertEquals(FlightRestrictionLevel.ADVISORY, result.level)
        assertEquals("해당 지역은 비행 가능합니다.", result.message)
        assertEquals(emptyList<String>(), result.details)
    }

    @Test
    fun `주의 구역 details 는 iOS처럼 레이어명과 zoneCode fallback 이름을 포함한다`() {
        val zone = DroneZoneFeature(
            id = "park-zone",
            layer = FlightZoneLayer.NATIONAL_PARK,
            polygons = listOf(unitSquare),
            zoneCode = null,
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = "VWorld 개별 공원명",
        )

        val result = FlightZoneCalculator.checkFlightPermission(
            lat = 37.5,
            lon = 126.5,
            zones = listOf(zone),
        )

        assertTrue(result.canFly)
        assertEquals(FlightRestrictionLevel.ADVISORY, result.level)
        assertEquals("비행 가능하나 주의가 필요한 지역입니다.", result.message)
        assertEquals(listOf("국립자연공원: 국립자연공원"), result.details)
    }

    // endregion

    // region distance 메서드 회귀 (Haversine)

    @Test
    fun `같은 좌표의 거리는 0`() {
        val d = FlightZoneCalculator.distance(37.5665, 126.9780, 37.5665, 126.9780)
        assertEquals(0.0, d, 0.001)
    }

    @Test
    fun `서울시청에서 부산시청까지 약 325km`() {
        // 서울시청: 37.5665, 126.9780
        // 부산시청: 35.1796, 129.0756
        // 실제 직선거리 약 325km (Haversine 기준)
        val d = FlightZoneCalculator.distance(37.5665, 126.9780, 35.1796, 129.0756)
        val km = d / 1000.0
        assertTrue("서울-부산 거리는 320~330km 범위여야 함 (실제: ${km}km)", km in 320.0..335.0)
    }

    // endregion
}
