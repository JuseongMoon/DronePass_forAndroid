package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `markdown uri open failure is ignored without crashing`() {
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                throw IllegalArgumentException("bad uri")
            }
        }

        assertFalse(openMarkdownUriSafely(uriHandler, "bad://url"))
    }

    @Test
    fun `markdown uri open success returns true`() {
        var openedUri: String? = null
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                openedUri = uri
            }
        }

        assertTrue(openMarkdownUriSafely(uriHandler, "https://example.com"))
        assertEquals("https://example.com", openedUri)
    }
}
