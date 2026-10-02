package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 무료 한도에서 도형 복제 → 페이월 → 닫기 뒤 입력이 막히던 회귀(105 점검)를 막는다.
 * 상세 시트가 먼저 숨으면, 한도에 막혀 상세 표시 상태가 남은 VM 과 어긋나 보이지 않는 시트 창이 입력을 가로챈다.
 */
class ShapeDuplicateQuotaContractTest {

    @Test
    fun `상세 시트는 복제를 숨기지 않고 바로 넘긴다`() {
        val source = projectSource("feature/shape/ShapeDetailSheet.kt")
        val onDuplicate = source.blockAfter("onDuplicate = {")

        assertTrue(onDuplicate.contains("onDuplicate()"))
        assertFalse(onDuplicate.contains("hideAndThen"))
        assertFalse(onDuplicate.contains("sheetState.hide"))
    }

    @Test
    fun `저장 탭 복제는 차단되면 상세를 그대로 두고 허용될 때만 상세를 닫고 편집을 연다`() {
        val body = projectSource("feature/saved/SavedListViewModel.kt").functionBody("fun onDuplicateRequested(")

        assertAppearsInOrder(
            body,
            listOf(
                "if (!subscriptionManager.allow(QuotaAction.DUPLICATE_SHAPE, \"shape_duplicate\")) return@launch",
                "_isDuplicateMode.value = true",
                "_showShapeDetail.value = false",
                "_showShapeEdit.value = true",
            ),
        )
    }

    @Test
    fun `지도 탭 복제는 차단되면 상세를 그대로 두고 허용될 때만 상세를 닫고 편집을 연다`() {
        val source = projectSource("feature/map/MapViewModel.kt")

        assertAppearsInOrder(
            source.functionBody("fun onDuplicateRequested("),
            listOf(
                "if (!subscriptionManager.allow(QuotaAction.DUPLICATE_SHAPE, \"shape_duplicate\")) return@launch",
                "openDuplicateShape(shape, returnToDetailAfterDismiss)",
            ),
        )
        assertAppearsInOrder(
            source.functionBody("private fun openDuplicateShape("),
            listOf("_isDuplicateMode.value = true", "_showShapeDetail.value = false", "_showShapeEdit.value = true"),
        )
    }

    private fun projectSource(path: String): String {
        val relative = "src/main/java/com/ScienceFiction/DronePassAndroid/$path"
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return listOf(relative, "app/$relative")
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Project file not found: $relative")
    }

    /** [marker] 뒤의 중괄호 블록(짝이 맞는 닫는 괄호까지). */
    private fun String.blockAfter(marker: String): String {
        val start = indexOf(marker).also { assertTrue("$marker not found", it >= 0) } + marker.length
        var depth = 1
        var index = start
        while (depth > 0) {
            when (this[index]) {
                '{' -> depth++
                '}' -> depth--
            }
            index++
        }
        return substring(start, index - 1)
    }

    private fun String.functionBody(signature: String): String {
        val start = indexOf(signature).also { assertTrue("$signature not found", it >= 0) }
        return substring(start).blockAfter("{")
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue("$token should appear after index $previousIndex", index > previousIndex)
            previousIndex = index
        }
    }
}
