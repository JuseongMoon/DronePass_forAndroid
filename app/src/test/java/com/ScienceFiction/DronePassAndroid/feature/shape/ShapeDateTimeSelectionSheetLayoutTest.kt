package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 날짜+시간 선택 시트는 달력과 시계를 함께 띄워 휴대폰 높이를 넘는다.
 * 스크롤이 없으면 시계가 눌려 찌그러지고 [선택 완료] 가 화면 밖으로 밀린다(3.6.1 에뮬레이터 확인).
 */
class ShapeDateTimeSelectionSheetLayoutTest {

    @Test
    fun `date time sheet content scrolls`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val sheet = File(root, "com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt").readText()
            .substringAfter("private fun ShapeDateTimeSelectionSheet(")
            .substringAfter("DronePassModalBottomSheet(")
        val column = sheet.substringAfter("Column(").substringBefore(") {")
        assertTrue(column, column.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(sheet.indexOf("Column(") < sheet.indexOf("DatePicker(state = datePickerState)"))
    }

    @Test
    fun `cancel and outside dismiss discard the picked value`() {
        // 취소 버튼까지 값을 적용해 바꾼 날짜를 되돌릴 방법이 없었다. "선택 완료"만 적용한다.
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val sheet = File(root, "com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt").readText()
            .substringAfter("private fun ShapeDateTimeSelectionSheet(")
        assertTrue(sheet.contains("onDismissRequest = onCancel,"))
        assertTrue(sheet.contains("onClick = onCancel,"))
        assertTrue(Regex("""applyCurrentSelectionAndDismiss\(\)""").findAll(sheet).count() == 2) // 정의 + 선택 완료 버튼
    }
}
