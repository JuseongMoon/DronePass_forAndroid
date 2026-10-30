package com.ScienceFiction.DronePassAndroid.feature.shape

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** iOS ShapeShareCardContentTests 대응 */
class ShapeShareCardContentTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply { timeZone = seoul }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = seoul }

    /** 서울 기준 2026-10-[day] hh:mm:ss */
    private fun seoul(hour: Int, minute: Int, second: Int = 0, day: Int = 26): Long =
        Calendar.getInstance(seoul).apply {
            clear()
            set(2026, Calendar.OCTOBER, day, hour, minute, second)
        }.timeInMillis

    private val sampleStart = 1_793_000_000_000L

    private fun makeShape(
        address: String? = "서울특별시 중구 세종대로 110",
        radius: Double? = 500.0,
        height: Double? = 120.0,
        start: Long = sampleStart,
        end: Long? = null,
    ) = ShapeModel(
        title = "한강 공원",
        baseCoordinate = Coordinate(37.5665, 126.978),
        radius = radius,
        height = height,
        memo = "긴 메모",
        address = address,
        flightStartDate = start,
        flightEndDate = end,
        color = "#FF3B30",
        droneId = "drone-1",
    )

    private fun content(shape: ShapeModel, drone: DroneModel? = null) = buildShapeShareCardContent(
        shape = shape,
        drone = drone,
        dateTimeFormat = dateTimeFormat,
        dateFormat = dateFormat,
        timeZone = seoul,
        locale = Locale.US,
    )

    @Test
    fun `값이 모두 있으면 모든 행을 채운다`() {
        val drone = DroneModel(id = "drone-1", name = "Mavic 3", color = "#34C759")
        val content = content(makeShape(), drone)

        assertEquals("한강 공원", content.title)
        assertEquals("서울특별시 중구 세종대로 110", content.address)
        assertEquals("500 m", content.radius)
        assertEquals("120 m", content.height)
        assertEquals("Mavic 3", content.droneName)
        assertEquals("#34C759", content.droneColorHex)
        assertEquals(Coordinate(37.5665, 126.978).formattedCoordinate, content.coordinate)
        // 카드 지도는 도형 원래 색을 쓴다
        assertEquals("#FF3B30", content.shapeColorHex)
    }

    @Test
    fun `값이 없거나 주소가 하이픈이면 생략한다`() {
        val content = content(makeShape(address = "-", radius = null, height = null))
        assertNull(content.address)
        assertNull(content.radius)
        assertNull(content.height)
        assertNull(content.droneName)
        assertNull(content.droneColorHex)
    }

    @Test
    fun `빈 주소와 0 이하 반경·고도는 생략한다`() {
        val content = content(makeShape(address = "  ", radius = 0.0, height = -3.0))
        assertNull(content.address)
        assertNull(content.radius)
        assertNull(content.height)
    }

    @Test
    fun `종료일이 없으면 시작만 표시한다`() {
        val content = content(makeShape(end = null))
        assertEquals(dateTimeFormat.format(sampleStart), content.period)
        assertFalse(content.period.orEmpty().contains("~"))
    }

    @Test
    fun `종료일이 있으면 시작과 종료를 잇는다`() {
        val end = sampleStart + 86_400_000L
        val content = content(makeShape(end = end))
        assertEquals("${dateTimeFormat.format(sampleStart)} ~ ${dateTimeFormat.format(end)}", content.period)
    }

    @Test
    fun `반경과 고도는 상세 화면과 같은 정수 미터 표기다`() {
        val content = content(makeShape(radius = 1234.9, height = 80.6))
        assertEquals(formatShapeDetailMeters(1234.9, Locale.US), content.radius)
        assertEquals("1,234 m", content.radius)
        assertEquals("80 m", content.height)
    }

    @Test
    fun `일 단위 도형은 시간 없이 날짜만 표시한다`() {
        val content = content(makeShape(start = seoul(0, 0), end = seoul(23, 59, 59, day = 28)))
        assertEquals("2026-10-26 ~ 2026-10-28", content.period)
    }

    @Test
    fun `일 단위 하루짜리 도형은 날짜 하나만 표시한다`() {
        val content = content(makeShape(start = seoul(0, 0), end = seoul(23, 59)))
        assertEquals("2026-10-26", content.period)
    }

    @Test
    fun `시간 지정 도형은 시간을 유지한다`() {
        val content = content(makeShape(start = seoul(9, 0), end = seoul(18, 0)))
        assertEquals("2026-10-26 09:00 ~ 2026-10-26 18:00", content.period)
    }

    @Test
    fun `일 단위 판정 규칙`() {
        // 일 단위 모드 저장값: 00:00 ~ 23:59 또는 23:59:59
        assertTrue(makeShape(start = seoul(0, 0), end = seoul(23, 59)).isDateOnlyFlight(seoul))
        assertTrue(makeShape(start = seoul(0, 0), end = seoul(23, 59, 59, day = 30)).isDateOnlyFlight(seoul))
        assertTrue(makeShape(start = seoul(0, 0), end = null).isDateOnlyFlight(seoul))
        // 시간 입력 도형
        assertFalse(makeShape(start = seoul(0, 0), end = seoul(18, 0)).isDateOnlyFlight(seoul))
        assertFalse(makeShape(start = seoul(9, 0), end = seoul(23, 59)).isDateOnlyFlight(seoul))
        assertFalse(makeShape(start = seoul(0, 0, 30), end = seoul(23, 59)).isDateOnlyFlight(seoul))
    }

    // ===== 항목 켜기/끄기 (iOS ShapeShareCardContentTests 대응) =====

    private fun content(options: ShapeShareCardOptions, tag: TagModel? = TagModel(name = "촬영")) =
        buildShapeShareCardContent(
            shape = makeShape(),
            drone = DroneModel(id = "drone-1", name = "Mini 4 Pro", color = "#007AFF"),
            tag = tag,
            options = options,
            dateTimeFormat = dateTimeFormat,
            dateFormat = dateFormat,
            timeZone = seoul,
            locale = Locale.US,
        )

    @Test
    fun `기본 옵션은 태그만 빼고 모두 보인다`() {
        val content = content(ShapeShareCardOptions())
        assertEquals("서울특별시 중구 세종대로 110", content.address)
        assertTrue(content.period != null)
        assertTrue(content.coordinate != null)
        assertEquals("Mini 4 Pro", content.droneName)
        assertNull("태그는 기본으로 끈다", content.tagName)
    }

    @Test
    fun `끈 항목은 null 이고 항상 들어가는 항목은 남는다`() {
        val content = content(
            ShapeShareCardOptions(
                showAddress = false,
                showPeriod = false,
                showCoordinate = false,
                showDrone = false,
                showTag = true,
            ),
        )
        assertNull(content.address)
        assertNull(content.period)
        assertNull(content.coordinate)
        assertNull(content.droneName)
        assertNull(content.droneColorHex)
        assertEquals("촬영", content.tagName)
        assertEquals("한강 공원", content.title)
        assertEquals("500 m", content.radius)
        assertEquals("120 m", content.height)
    }

    @Test
    fun `태그를 켜도 태그가 없으면 보이지 않는다`() {
        assertNull(content(ShapeShareCardOptions(showTag = true), tag = null).tagName)
    }

    @Test
    fun `주소 간략히`() {
        assertEquals("서울특별시 중구", content(ShapeShareCardOptions(simplifyAddress = true)).address)
        assertNull(
            "주소를 끄면 간략히와 무관하게 뺀다",
            content(ShapeShareCardOptions(showAddress = false, simplifyAddress = true)).address,
        )
    }

    @Test
    fun `지역 요약 규칙`() {
        assertEquals("경기도 양평군", koreanAddressRegionSummary("경기도 양평군 양서면 양수리 123"))
        assertEquals("경기도 성남시 분당구", koreanAddressRegionSummary("경기도 성남시 분당구 정자동 1"))
        assertEquals("경기도 수원시 팔달구", koreanAddressRegionSummary("경기도 수원시 팔달구 매산로 1"))
        assertEquals("경기도 김포시", koreanAddressRegionSummary("경기도 김포시 고촌읍"))
        assertEquals("서울특별시 강남구", koreanAddressRegionSummary("서울특별시 강남구 역삼동 737"))
        assertEquals("세종특별자치시", koreanAddressRegionSummary("세종특별자치시 한솔동 1"))
        assertEquals("제주특별자치도 제주시", koreanAddressRegionSummary("제주특별자치도 제주시 연동 1"))
        assertEquals("강원특별자치도 평창군", koreanAddressRegionSummary("강원특별자치도 평창군 대관령면"))
        assertEquals("1600 Amphitheatre", koreanAddressRegionSummary("1600 Amphitheatre Pkwy, Mountain View"))
    }

    @Test
    fun `옵션 저장값은 왕복되고 빠진 키는 기본값으로 채운다`() {
        assertEquals(ShapeShareCardOptions(), ShapeShareCardOptions.fromJson(null))
        val options = ShapeShareCardOptions(showCoordinate = false, showTag = true)
        assertEquals(options, ShapeShareCardOptions.fromJson(options.toJson()))
        assertEquals(ShapeShareCardOptions(showAddress = false), ShapeShareCardOptions.fromJson("""{"showAddress":false}"""))
        assertEquals(ShapeShareCardOptions(), ShapeShareCardOptions.fromJson("not json"))
    }
}
