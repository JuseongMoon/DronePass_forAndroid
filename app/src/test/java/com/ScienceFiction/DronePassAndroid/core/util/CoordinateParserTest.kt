package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * CoordinateParser 단위 테스트
 *
 * Phase 2.1 C-H3 (DMS 분/초 범위 검증) + C-H4 (Decimal anchoring) 회귀 보호.
 */
class CoordinateParserTest {

    // region Decimal anchoring (C-H4)

    @Test
    fun `정상 십진수는 파싱된다`() {
        val c = CoordinateParser.parse("37.5665, 126.9780")
        assertNotNull(c)
        assertEquals(37.5665, c!!.latitude, 0.0001)
        assertEquals(126.9780, c.longitude, 0.0001)
    }

    @Test
    fun `iOS 안내 예시처럼 도 기호가 붙은 십진도도 파싱된다`() {
        val c = CoordinateParser.parse("37.648611°, 126.686667°")
        assertNotNull(c)
        assertEquals(37.648611, c!!.latitude, 0.0001)
        assertEquals(126.686667, c.longitude, 0.0001)
    }

    @Test
    fun `공백 구분자도 파싱된다`() {
        val c = CoordinateParser.parse("37.5665 126.9780")
        assertNotNull(c)
    }

    @Test
    fun `iOS처럼 명시적 양수 부호가 붙은 십진수도 파싱된다`() {
        val c = CoordinateParser.parse("+37.5665 +126.9780")
        assertNotNull(c)
        assertEquals(37.5665, c!!.latitude, 0.0001)
        assertEquals(126.9780, c.longitude, 0.0001)
    }

    @Test
    fun `좌우 공백은 허용`() {
        val c = CoordinateParser.parse("  37.5665, 126.9780  ")
        assertNotNull(c)
    }

    @Test
    fun `입력 앞뒤에 다른 텍스트가 있으면 거부`() {
        // 이전(anchor 없음): "abc 99.9, 99.9" 가 매치되어 잘못된 좌표 생성됐음
        assertNull(CoordinateParser.parse("abc 99.9, 99.9"))
        assertNull(CoordinateParser.parse("99.9, 99.9 xyz"))
        assertNull(CoordinateParser.parse("hello world"))
    }

    // endregion

    // region DMS 분/초 범위 검증 (C-H3)

    @Test
    fun `정상 DMS는 파싱된다`() {
        val c = CoordinateParser.parse("37°33'58.0\"N 126°58'41.0\"E")
        assertNotNull(c)
        // 37 + 33/60 + 58/3600 ≈ 37.566
        assertEquals(37.566, c!!.latitude, 0.001)
    }

    @Test
    fun `iOS 안내 예시의 도분초도 파싱된다`() {
        val c = CoordinateParser.parse("37° 38′ 55″ N 126° 41′ 12″ E")
        assertNotNull(c)
        assertEquals(37.648, c!!.latitude, 0.001)
        assertEquals(126.686, c.longitude, 0.001)
    }

    @Test
    fun `DMS 분이 60 이상이면 거부`() {
        assertNull(CoordinateParser.parse("37°60'00.0\"N 126°58'41.0\"E"))
        assertNull(CoordinateParser.parse("37°33'00.0\"N 126°99'41.0\"E"))
    }

    @Test
    fun `DMS 초가 60 이상이면 거부`() {
        assertNull(CoordinateParser.parse("37°33'60.0\"N 126°58'41.0\"E"))
        assertNull(CoordinateParser.parse("37°33'58.0\"N 126°58'99.9\"E"))
    }

    // endregion

    // region Geo URI

    @Test
    fun `Geo URI 파싱`() {
        val c = CoordinateParser.parse("geo:37.5665,126.9780")
        assertNotNull(c)
    }

    @Test
    fun `iOS처럼 명시적 양수 부호가 붙은 Geo URI도 파싱된다`() {
        val c = CoordinateParser.parse("geo:+37.5665,+126.9780")
        assertNotNull(c)
        assertEquals(37.5665, c!!.latitude, 0.0001)
        assertEquals(126.9780, c.longitude, 0.0001)
    }

    @Test
    fun `Geo URI 위경도 범위 위반 거부`() {
        assertNull(CoordinateParser.parse("geo:91.0,200.0"))
    }

    // endregion

    // region iOS unsupported formats

    @Test
    fun `iOS stub 형식인 MGRS와 Plus Code는 좌표로 파싱하지 않는다`() {
        assertNull(CoordinateParser.parse("52S DF 24174 67282"))
        assertNull(CoordinateParser.parse("8Q98FXC7+M2"))
    }

    // endregion

    // region 위경도 범위 위반

    @Test
    fun `위도가 90을 초과하면 null`() {
        assertNull(CoordinateParser.parse("91.0, 126.9780"))
        assertNull(CoordinateParser.parse("-91.0, 126.9780"))
    }

    @Test
    fun `경도가 180을 초과하면 null`() {
        assertNull(CoordinateParser.parse("37.5665, 181.0"))
        assertNull(CoordinateParser.parse("37.5665, -181.0"))
    }

    // endregion
}
