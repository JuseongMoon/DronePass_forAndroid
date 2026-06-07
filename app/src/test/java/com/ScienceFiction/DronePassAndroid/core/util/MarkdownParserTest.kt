package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownParserTest {

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
}
