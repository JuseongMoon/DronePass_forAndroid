package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 새 도형에서 시작일을 고른 뒤 일단위 토글을 바꾸면 시작일이 저장된 기본값으로 돌아갔다(3.6.1 에뮬레이터 재현).
 * 토글은 기본값에 바로 저장되므로, 그 값이 초기 기간의 remember 키에 있으면 고른 날짜가 다시 계산된다.
 */
class ShapeEditDateOnlyToggleTest {

    @Test
    fun `toggling date only mode does not recompute the initial flight period`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val screen = File(root, "com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt").readText()
        val keys = screen.substringAfter("val initialFlightPeriod = remember(").substringBefore(") {")
        assertTrue(keys, keys.contains("editDefaults.startDate"))
        assertFalse(keys, keys.contains("editDefaults.isDateOnly"))
    }
}
