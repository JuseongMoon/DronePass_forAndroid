package com.ScienceFiction.DronePassAndroid.core.util

import androidx.compose.ui.platform.UriHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeUriHandlerTest {

    @Test
    fun `uri open failure is ignored without crashing`() {
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                throw IllegalArgumentException("bad uri")
            }
        }

        assertFalse(openUriSafely(uriHandler, "bad://url"))
    }

    @Test
    fun `uri open success returns true`() {
        var openedUri: String? = null
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                openedUri = uri
            }
        }

        assertTrue(openUriSafely(uriHandler, "https://example.com"))
        assertEquals("https://example.com", openedUri)
    }
}
