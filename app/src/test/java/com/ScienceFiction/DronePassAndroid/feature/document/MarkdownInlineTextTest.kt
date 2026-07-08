package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownInlineTextTest {

    @Test
    fun `markdown links keep iOS AttributedString URL annotations`() {
        val text = parseInlineMarkdown(
            text = "Open [NOAA](https://www.swpc.noaa.gov/) data",
            linkColor = Color.Blue,
        )

        assertEquals("Open NOAA data", text.text)
        val annotations = text.getStringAnnotations(
            tag = MarkdownUrlAnnotationTag,
            start = "Open ".length,
            end = "Open NOAA".length,
        )
        assertEquals("https://www.swpc.noaa.gov/", annotations.single().item)
    }

    @Test
    fun `plain markdown text has no URL annotations`() {
        val text = parseInlineMarkdown(
            text = "No link here",
            linkColor = Color.Blue,
        )

        assertEquals("No link here", text.text)
        assertTrue(
            text.getStringAnnotations(
                tag = MarkdownUrlAnnotationTag,
                start = 0,
                end = text.length,
            ).isEmpty(),
        )
    }

    @Test
    fun `link text parses nested bold like iOS AttributedString inline markdown`() {
        val text = parseInlineMarkdown(
            text = "Open [**NOAA**](https://www.swpc.noaa.gov/) data",
            linkColor = Color.Blue,
        )

        assertEquals("Open NOAA data", text.text)
        val start = "Open ".length
        val end = "Open NOAA".length
        assertEquals(
            "https://www.swpc.noaa.gov/",
            text.getStringAnnotations(MarkdownUrlAnnotationTag, start, end).single().item,
        )
        assertTrue(
            text.spanStyles.any { range ->
                range.start == start &&
                    range.end == end &&
                    range.item.fontWeight == FontWeight.Bold
            },
        )
    }

    @Test
    fun `bold text parses nested link like iOS AttributedString inline markdown`() {
        val text = parseInlineMarkdown(
            text = "**[NOAA](https://www.swpc.noaa.gov/)**",
            linkColor = Color.Blue,
        )

        assertEquals("NOAA", text.text)
        assertEquals(
            "https://www.swpc.noaa.gov/",
            text.getStringAnnotations(MarkdownUrlAnnotationTag, 0, text.length).single().item,
        )
        assertTrue(
            text.spanStyles.any { range ->
                range.start == 0 &&
                    range.end == text.length &&
                    range.item.fontWeight == FontWeight.Bold
            },
        )
    }

    @Test
    fun `markdown table separators match iOS table overlays`() {
        assertTrue(shouldShowMarkdownTableVerticalDividerAfterCell())
        assertTrue(shouldShowMarkdownTableHorizontalDividerAfterDataRow())
    }

    @Test
    fun `markdown view renders iOS element switch order`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/MarkdownView.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/MarkdownView.kt",
        ).readText()
        val markdownViewSource = source.substring(
            source.indexOf("fun MarkdownView("),
            source.indexOf("@Composable\nprivate fun HeaderView"),
        )

        assertSourceOrder(
            markdownViewSource,
            listOf(
                "Column(",
                ".padding(horizontal = 16.dp)",
                "Arrangement.spacedBy(16.dp)",
                "elements.forEach { element ->",
                "MarkdownElementType.Header -> HeaderView",
                "MarkdownElementType.Paragraph -> ParagraphView",
                "MarkdownElementType.Table -> tableMap[element.content]?.let { TableView(it) }",
                "MarkdownElementType.Separator -> SeparatorView()",
                "MarkdownElementType.ListItem -> ListItemView",
            ),
        )
    }

    @Test
    fun `markdown block element tokens match iOS MarkdownView`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/MarkdownView.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/MarkdownView.kt",
        ).readText()
        val headerSource = source.substring(
            source.indexOf("@Composable\nprivate fun HeaderView"),
            source.indexOf("private fun headerPadding"),
        )
        val paragraphSource = source.substring(
            source.indexOf("@Composable\nprivate fun ParagraphView"),
            source.indexOf("@Composable\nprivate fun ListItemView"),
        )
        val listSource = source.substring(
            source.indexOf("@Composable\nprivate fun ListItemView"),
            source.indexOf("@Composable\nprivate fun SeparatorView"),
        )
        val separatorSource = source.substring(
            source.indexOf("@Composable\nprivate fun SeparatorView"),
            source.indexOf("@Composable\nprivate fun TableView"),
        )

        assertSourceOrder(
            headerSource,
            listOf(
                "Arrangement.spacedBy(8.dp)",
                "markdownHeaderFontSize(level)",
                "markdownHeaderFontWeight(level)",
                "if (level <= 2)",
                ".height(if (level == 1) 3.dp else 2.dp)",
                ".copy(alpha = 0.3f)",
            ),
        )
        assertTrue(paragraphSource.contains("MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)"))
        assertSourceOrder(
            listSource,
            listOf(
                "Modifier.padding(start = 8.dp)",
                "Arrangement.spacedBy(8.dp)",
                "text = \"•\"",
                "fontWeight = FontWeight.Bold",
                "modifier = Modifier.padding(top = 2.dp)",
                "MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp)",
            ),
        )
        assertSourceOrder(
            separatorSource,
            listOf(
                "Modifier.padding(vertical = 8.dp)",
                "thickness = 1.dp",
            ),
        )
    }

    @Test
    fun `markdown table tokens match iOS table view`() {
        val source = resolveProjectFile(
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/MarkdownView.kt",
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/document/MarkdownView.kt",
        ).readText()
        val tableSource = source.substring(
            source.indexOf("@Composable\nprivate fun TableView"),
            source.indexOf("/**\n * 인라인 마크다운 파서"),
        )

        assertSourceOrder(
            tableSource,
            listOf(
                ".padding(vertical = 4.dp)",
                ".clip(RoundedCornerShape(8.dp))",
                ".border(1.dp, border, RoundedCornerShape(8.dp))",
                "TableRow(",
                "isHeader = true",
                "HorizontalDivider(thickness = 0.5.dp, color = border)",
                "table.rows.forEach { row ->",
                "isHeader = false",
                "shouldShowMarkdownTableHorizontalDividerAfterDataRow()",
                "if (cells.size < totalColumns)",
                "repeat(totalColumns - cells.size)",
                "horizontal = 12.dp",
                "vertical = if (isHeader) 10.dp else 8.dp",
                ".width(0.5.dp)",
            ),
        )
    }

    @Test
    fun `markdown header font tokens match iOS title styles`() {
        assertEquals(28.sp, markdownHeaderFontSize(1))
        assertEquals(22.sp, markdownHeaderFontSize(2))
        assertEquals(20.sp, markdownHeaderFontSize(3))
        assertEquals(17.sp, markdownHeaderFontSize(4))
        assertEquals(FontWeight.Bold, markdownHeaderFontWeight(1))
        assertEquals(FontWeight.Bold, markdownHeaderFontWeight(2))
        assertEquals(FontWeight.SemiBold, markdownHeaderFontWeight(3))
        assertEquals(FontWeight.SemiBold, markdownHeaderFontWeight(4))
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Project file not found. Tried: ${candidates.joinToString()}")
    }

    private fun assertSourceOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        tokens.forEach { token ->
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue("Missing token: $token", index >= 0)
            assertTrue("Token out of order: $token", index > previousIndex)
            previousIndex = index
        }
    }
}
