package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedShapeListItemTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    @Test
    fun `저장 목록 행 날짜 formatter 는 iOS DateFormatter 처럼 호출 Locale 을 따른다`() {
        val formatter = savedShapeListDateFormat(Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }

        assertEquals("2025-01-01", formatter.format(1_735_657_200_000L))
    }

    @Test
    fun `저장 목록 행은 날짜 Locale 을 한국어로 강제하지 않는다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedShapeListItem.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedShapeListItem.kt",
        ).readText()

        assertTrue(source.contains("savedShapeListDateFormat(currentLocale)"))
        assertFalse(source.contains("Locale.KOREA"))
    }

    @Test
    fun `저장 목록 행 날짜는 iOS처럼 하이픈 형식으로 표시한다`() {
        val text = formatSavedShapeDateRange(
            startDateMillis = 1_735_657_200_000L,
            endDateMillis = 1_735_743_600_000L,
            dateFormat = dateFormat,
        )

        assertEquals("2025-01-01 ~ 2025-01-02", text)
    }

    @Test
    fun `저장 목록 행 종료일이 없으면 iOS처럼 시작일만 표시한다`() {
        val text = formatSavedShapeDateRange(
            startDateMillis = 1_735_657_200_000L,
            endDateMillis = null,
            dateFormat = dateFormat,
        )

        assertEquals("2025-01-01", text)
    }

    @Test
    fun `저장 목록 행 색상은 손상된 HEX 를 iOS처럼 기본 파랑으로 폴백한다`() {
        assertEquals(0xFF007AFF.toInt(), resolveSavedShapeDisplayColor("not-a-color").toArgb())
    }

    @Test
    fun `저장 목록 행 색상은 팔레트 외 유효 HEX 를 그대로 사용한다`() {
        assertEquals(0xFF123456.toInt(), resolveSavedShapeDisplayColor("#123456").toArgb())
    }

    @Test
    fun `저장 목록 행 색상은 iOS처럼 8자리 HEX 의 하위 RGB 만 사용한다`() {
        assertEquals(0xFF123456.toInt(), resolveSavedShapeDisplayColor("#AA123456").toArgb())
    }

    @Test
    fun `저장 목록 행 색상은 iOS처럼 짧은 HEX 도 스캐너 결과를 사용한다`() {
        assertEquals(0xFF000001.toInt(), resolveSavedShapeDisplayColor("#1").toArgb())
    }

    @Test
    fun `저장 목록 주소 행은 iOS처럼 nil 일 때만 숨기고 빈 문자열은 유지한다`() {
        assertEquals(false, shouldShowSavedShapeAddress(null))
        assertEquals(true, shouldShowSavedShapeAddress(""))
        assertEquals(true, shouldShowSavedShapeAddress("서울특별시 중구 세종대로 110"))
    }

    @Test
    fun `저장 목록 제목은 iOS ShapeInfoContent처럼 빈 문자열도 원문 그대로 표시한다`() {
        assertEquals("", savedShapeListTitleText(""))
        assertEquals("   ", savedShapeListTitleText("   "))
        assertEquals("Flight Area", savedShapeListTitleText("Flight Area"))
    }

    @Test
    fun `저장 목록 색상 인디케이터 그림자는 iOS radius 1 과 맞춘다`() {
        assertEquals(1f, SavedShapeColorIndicatorShadowElevation.value, 0f)
    }

    @Test
    fun `저장 목록 행 레이아웃 토큰은 iOS ShapeListRow 값을 따른다`() {
        assertEquals(55.dp, SavedShapeRowMinHeight)
        assertEquals(4.dp, SavedShapeRowHorizontalPadding)
        assertEquals(12.dp, SavedShapeInfoLeadingSpacing)
        assertEquals(8.dp, SavedShapeDetailLeadingSpacing)
        assertEquals(30.dp, SavedShapeDetailButtonWidth)
        assertEquals(12.dp, SavedShapeDetailChevronSize)
    }

    @Test
    fun `저장 목록 색상 인디케이터 크기는 iOS ShapeColorIndicator 값을 따른다`() {
        assertEquals(4.dp, SavedShapeColorIndicatorWidth)
        assertEquals(35.dp, SavedShapeColorIndicatorHeight)
        assertEquals(15.dp, SavedShapeColorIndicatorCornerRadius)
    }

    @Test
    fun `저장 목록 만료 도형 색상 인디케이터는 iOS systemGray 를 사용한다`() {
        assertEquals(0xFF8E8E93.toInt(), SavedShapeExpiredIndicatorColor.toArgb())
    }

    @Test
    fun `저장 목록 행 만료 색상 판정은 iOS SavedTableListView처럼 종료 시각과 현재가 같으면 만료로 본다`() {
        assertEquals(true, isSavedShapeListItemExpired(flightEndDateMillis = 1_000L, now = 1_000L))
        assertEquals(true, isSavedShapeListItemExpired(flightEndDateMillis = 999L, now = 1_000L))
        assertEquals(false, isSavedShapeListItemExpired(flightEndDateMillis = 1_001L, now = 1_000L))
        assertEquals(false, isSavedShapeListItemExpired(flightEndDateMillis = null, now = 1_000L))
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
