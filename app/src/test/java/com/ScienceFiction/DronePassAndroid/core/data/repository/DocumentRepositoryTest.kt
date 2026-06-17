package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElement
import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElementType
import com.ScienceFiction.DronePassAndroid.domain.model.ParsedDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentRepositoryTest {

    @Test
    fun `문서 경로는 iOS처럼 한국어만 원본 txt 파일을 사용한다`() {
        assertEquals(
            "dronepass/version-patches.txt",
            localizedDocumentPath("dronepass/version-patches", "ko"),
        )
        assertEquals(
            "dronepass/version-patches.txt",
            localizedDocumentPath("dronepass/version-patches", "ko-KR"),
        )
    }

    @Test
    fun `문서 경로는 iOS처럼 한국어가 아닌 언어에서 영어 파일을 사용한다`() {
        assertEquals(
            "dronepass/version-patches_en.txt",
            localizedDocumentPath("dronepass/version-patches", "en"),
        )
        assertEquals(
            "dronepass/version-patches_en.txt",
            localizedDocumentPath("dronepass/version-patches", "ja-JP"),
        )
        assertEquals(
            "dronepass/version-patches_en.txt",
            localizedDocumentPath("dronepass/version-patches", null),
        )
    }

    @Test
    fun `빈 약관 문서는 iOS처럼 로드 실패 대상으로 취급한다`() {
        val emptyDocument = ParsedDocument(elements = emptyList(), tables = emptyList())

        assertFalse(parsedDocumentHasDisplayableElements(emptyDocument))
        assertThrows(IllegalStateException::class.java) {
            requireDisplayableParsedDocument(emptyDocument, label = "Terms")
        }
    }

    @Test
    fun `표시 가능한 약관 문서는 성공 캐시 대상으로 유지한다`() {
        val document = ParsedDocument(
            elements = listOf(
                MarkdownElement(
                    type = MarkdownElementType.Paragraph,
                    content = "terms",
                ),
            ),
            tables = emptyList(),
        )

        assertTrue(parsedDocumentHasDisplayableElements(document))
        assertSame(document, requireDisplayableParsedDocument(document, label = "Terms"))
    }
}
