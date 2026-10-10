package com.ScienceFiction.DronePassAndroid.ui.theme

import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 도형 편집의 날짜 선택에서 펜 아이콘(직접 입력)을 누르면 앱이 종료되던 버그.
 * 직접 입력 필드의 라벨 애니메이션이 bodyLarge 와 bodySmall 을 보간하는데 줄 높이 단위(em/sp)가 달랐다.
 */
class LabelSafeTypographyTest {

    @Test
    fun `app typography cannot interpolate the text field label styles`() {
        // bodyLarge 줄 높이를 글자 크기 비율(em)로 둔 이유는 Type.kt 참고. 그대로는 라벨 보간이 실패한다.
        assertThrows(IllegalArgumentException::class.java) {
            lerp(Typography.bodyLarge, Typography.bodySmall, 0.5f)
        }
    }

    @Test
    fun `label safe typography interpolates and keeps the same line height`() {
        val safe = Typography.withLabelSafeLineHeights()
        listOf(0f, 0.5f, 1f).forEach { fraction -> lerp(safe.bodyLarge, safe.bodySmall, fraction) }
        // 17sp 글자 × 22/17 = 22sp 로 보이는 줄 높이는 그대로다.
        assertEquals(22.sp, safe.bodyLarge.lineHeight)
        assertEquals(Typography.bodyLarge.fontSize, safe.bodyLarge.fontSize)
    }

    @Test
    fun `date pickers and labeled text fields run under the label safe typography`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val labeled = Regex("""\b(DatePicker|DateRangePicker|DatePickerDialog)\(|\blabel = \{""")
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            labeled.findAll(text).forEach { match ->
                val before = text.substring(0, match.range.first).takeLast(400)
                assertTrue("${file.name}: ${match.value}", before.contains("withLabelSafeLineHeights()"))
            }
        }
    }
}
