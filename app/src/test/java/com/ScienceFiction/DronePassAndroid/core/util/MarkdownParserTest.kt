package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parseMarkdownPreservesIosDocumentElementStructure() {
        val content = """
            # Title
            Paragraph with **bold** text
            | Header A | Header B |
            |---|---|
            | Value A | Value B |
            | Only A |
            ---
            - Dash item
            • Bullet item
        """.trimIndent()

        val parsed = MarkdownParser.parseMarkdown(content)

        assertEquals(
            listOf(
                MarkdownElementType.Header,
                MarkdownElementType.Paragraph,
                MarkdownElementType.Table,
                MarkdownElementType.Separator,
                MarkdownElementType.ListItem,
                MarkdownElementType.ListItem,
            ),
            parsed.elements.map { it.type },
        )
        assertEquals("Title", parsed.elements[0].content)
        assertEquals(1, parsed.elements[0].level)
        assertEquals("Paragraph with **bold** text", parsed.elements[1].content)
        assertEquals(parsed.tables.single().id, parsed.elements[2].content)
        assertEquals("Dash item", parsed.elements[4].content)
        assertEquals("Bullet item", parsed.elements[5].content)

        val table = parsed.tables.single()
        assertEquals(listOf("Header A", "Header B"), table.headers)
        assertEquals(
            listOf(
                listOf("Value A", "Value B"),
                listOf("Only A"),
            ),
            table.rows,
        )
    }

    @Test
    fun parseMarkdownTreatsSinglePipeLineAsParagraphLikeIos() {
        val parsed = MarkdownParser.parseMarkdown("Use A | B as text")

        assertEquals(1, parsed.elements.size)
        assertEquals(MarkdownElementType.Paragraph, parsed.elements.single().type)
        assertEquals("Use A | B as text", parsed.elements.single().content)
        assertTrue(parsed.tables.isEmpty())
    }

    @Test
    fun parseMarkdownHandlesWindowsNewlinesLikeIosNewlines() {
        val parsed = MarkdownParser.parseMarkdown(
            "# Title\r\n" +
                "Paragraph\r\n" +
                "| Header A | Header B |\r\n" +
                "|---|---|\r\n" +
                "| Value A | Value B |\r\n" +
                "- Dash item",
        )

        assertEquals(
            listOf(
                MarkdownElementType.Header,
                MarkdownElementType.Paragraph,
                MarkdownElementType.Table,
                MarkdownElementType.ListItem,
            ),
            parsed.elements.map { it.type },
        )
        assertEquals("Title", parsed.elements[0].content)
        assertEquals("Paragraph", parsed.elements[1].content)
        assertEquals(listOf("Header A", "Header B"), parsed.tables.single().headers)
        assertEquals(listOf("Value A", "Value B"), parsed.tables.single().rows.single())
        assertEquals("Dash item", parsed.elements[3].content)
    }

    @Test
    fun parsePatchNotesPreservesIosFeatureStructure() {
        val content = """
            v1.2.3 (2025-07-01): Title: with colon
            - Feature A
                - First description line
                - Second description line
            - Feature B

            v1.2.2 (2025-06-01): Previous release
            - Previous feature
        """.trimIndent()

        val notes = MarkdownParser.parsePatchNotes(content)

        assertEquals(2, notes.size)
        assertEquals("v1.2.3", notes[0].version)
        assertEquals("2025-07-01", notes[0].date)
        assertEquals("Title: with colon", notes[0].title)
        assertEquals(2, notes[0].features.size)
        assertEquals("Feature A", notes[0].features[0].title)
        assertEquals("First description line\nSecond description line", notes[0].features[0].description)
        assertEquals("Feature B", notes[0].features[1].title)
        assertNull(notes[0].features[1].description)
        assertEquals("v1.2.2", notes[1].version)
    }

    @Test
    fun parsePatchNotesRestoresLeadingVersionPrefixLikeIos() {
        val content = """
            1.0.0 (2025-01-01): Legacy first block
            - Initial feature
        """.trimIndent()

        val notes = MarkdownParser.parsePatchNotes(content)

        assertEquals(1, notes.size)
        assertEquals("v1.0.0", notes[0].version)
        assertEquals("Initial feature", notes[0].features.single().title)
    }

    @Test
    fun parsePatchNotesHandlesWindowsNewlinesLikeIosNewlines() {
        val notes = MarkdownParser.parsePatchNotes(
            "v1.2.3 (2025-07-01): Release title\r\n" +
                "- Feature A\r\n" +
                "    - First description line\r\n" +
                "    - Second description line\r\n" +
                "\r\n" +
                "v1.2.2 (2025-06-01): Previous release\r\n" +
                "- Previous feature",
        )

        assertEquals(2, notes.size)
        assertEquals("v1.2.3", notes[0].version)
        assertEquals("2025-07-01", notes[0].date)
        assertEquals("Release title", notes[0].title)
        assertEquals("Feature A", notes[0].features.single().title)
        assertEquals("First description line\nSecond description line", notes[0].features.single().description)
        assertEquals("Previous release", notes[1].title)
        assertEquals("Previous feature", notes[1].features.single().title)
    }
}
