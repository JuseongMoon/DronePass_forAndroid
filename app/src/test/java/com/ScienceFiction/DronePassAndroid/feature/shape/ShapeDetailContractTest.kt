package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShapeDetailContractTest {

    @Test
    fun `도형 상세 행 순서는 iOS ShapeDetailView 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.shape_detail_drone_connected",
                "R.string.shape_detail_title_label",
                "R.string.shape_detail_coordinate",
                "R.string.shape_detail_address",
                "R.string.shape_detail_radius",
                "R.string.shape_detail_altitude",
                "R.string.shape_detail_flight_start",
                "R.string.shape_detail_flight_end",
                "R.string.common_memo",
            ),
        )
    }

    @Test
    fun `도형 상세 더보기 메뉴 순서는 iOS ShapeDetailView 를 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.shape_detail_edit",
                "R.string.shape_detail_duplicate",
                "R.string.common_delete",
            ),
        )
    }

    @Test
    fun `도형 상세 좌표와 주소 상호작용은 iOS copyableText 와 주소 탭 계약을 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.shape_detail_coordinate",
                "copyAndShowToast(shape.baseCoordinate.decimalCoordinate)",
                "shape.baseCoordinate.formattedCoordinate",
                "R.string.shape_detail_address",
                "onClick = { showExternalMapDialog = true }",
                "copyableShapeDetailAddress(shape.address)",
                "formatShapeDetailAddress(shape.address)",
            ),
        )
    }

    @Test
    fun `도형 상세 메모 링크 처리는 iOS HyperlinkTextView 와 SafariView 계약을 따른다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "R.string.common_memo",
                "formatShapeDetailMemo(shape.memo)",
                "resolveShapeDetailMemoLinkAction(url)",
                "ShapeDetailMemoLinkAction.IN_APP_WEB -> memoWebUrl = url",
                "ShapeDetailMemoLinkAction.SYSTEM_INTENT -> openMemoSystemLink(context, url)",
            ),
        )
    }

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex in ShapeDetailSheet.kt",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}
