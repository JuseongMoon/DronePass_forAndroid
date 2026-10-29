package com.ScienceFiction.DronePassAndroid.feature.shape

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
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
        assertFalse(content.period.contains("~"))
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
}
