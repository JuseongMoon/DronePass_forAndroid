package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
}
