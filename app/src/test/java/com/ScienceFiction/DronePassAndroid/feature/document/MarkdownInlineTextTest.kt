package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.ui.graphics.Color
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
}
