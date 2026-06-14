package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class ShapeDateFormatsTest {

    private val sampleDate = Date(1_735_714_800_000L)

    @Test
    fun `도형 편집 날짜 전용 표시는 iOS DateSection처럼 medium date만 사용한다`() {
        val locale = Locale.KOREA

        val actual = shapeEditDateOnlyFormat(locale).format(sampleDate)
        val expected = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(sampleDate)

        assertEquals(expected, actual)
    }

    @Test
    fun `도형 편집 날짜 시간 표시는 iOS DateSection처럼 medium date와 short time을 사용한다`() {
        val locale = Locale.KOREA

        val actual = localizedShapeDateTimeFormat(locale).format(sampleDate)
        val expected = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(sampleDate)

        assertEquals(expected, actual)
    }

    @Test
    fun `도형 상세 날짜 시간 표시는 iOS localizedDateTime처럼 현재 로케일 스타일을 따른다`() {
        val locale = Locale.US

        val actual = localizedShapeDateTimeFormat(locale).format(sampleDate)
        val expected = DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(sampleDate)

        assertEquals(expected, actual)
    }
}
