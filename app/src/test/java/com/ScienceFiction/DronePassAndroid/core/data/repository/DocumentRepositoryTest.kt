package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElement
import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElementType
import com.ScienceFiction.DronePassAndroid.domain.model.ParsedDocument
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class DocumentRepositoryTest {

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    /** 경로별 응답을 정해 두고, 요청한 경로를 순서대로 기록한다. */
    private class FakeServer(private val responses: Map<String, () -> String>) {
        val requested = mutableListOf<String>()
        var fallbackCount = 0

        suspend fun fetch(path: String, sharedPath: String): String = fetchWithSharedFallback(
            path = path,
            sharedPath = sharedPath,
            fetch = { requested += it; responses.getValue(it)() },
            onSharedFallback = { fallbackCount++ },
        )
    }

    @Test
    fun `패치노트는 Android 전용 파일을 언어별 이름으로 읽고 대체는 같은 언어의 공용 파일이다`() {
        val platform = DocumentRepository.PATH_PATCH_NOTES
        val shared = DocumentRepository.PATH_PATCH_NOTES_SHARED

        assertEquals("dronepass/version-patches-android.txt", localizedDocumentPath(platform, "ko"))
        assertEquals("dronepass/version-patches-android.txt", localizedDocumentPath(platform, "ko-KR"))
        assertEquals("dronepass/version-patches-android.txt", localizedDocumentPath(platform, null))
        assertEquals("dronepass/version-patches-android_en.txt", localizedDocumentPath(platform, "en"))
        assertEquals("dronepass/version-patches-android_en.txt", localizedDocumentPath(platform, "ja-JP"))
        assertEquals("dronepass/version-patches.txt", localizedDocumentPath(shared, "ko"))
        assertEquals("dronepass/version-patches_en.txt", localizedDocumentPath(shared, "en"))
    }

    @Test
    fun `Android 파일이 있으면 그 파일을 쓰고 공용 파일은 읽지 않는다`() = runBlocking {
        val server = FakeServer(
            mapOf(
                "dronepass/version-patches-android.txt" to { "android" },
                "dronepass/version-patches.txt" to { "shared" },
            ),
        )

        val content = server.fetch("dronepass/version-patches-android.txt", "dronepass/version-patches.txt")

        assertEquals("android", content)
        assertEquals(listOf("dronepass/version-patches-android.txt"), server.requested)
        assertEquals(0, server.fallbackCount)
    }

    @Test
    fun `Android 파일이 없으면(404) 같은 언어의 공용 파일로 대체한다`() = runBlocking {
        val server = FakeServer(
            mapOf(
                "dronepass/version-patches-android_en.txt" to { throw httpError(404) },
                "dronepass/version-patches_en.txt" to { "shared en" },
            ),
        )

        val content = server.fetch("dronepass/version-patches-android_en.txt", "dronepass/version-patches_en.txt")

        assertEquals("shared en", content)
        assertEquals(
            listOf("dronepass/version-patches-android_en.txt", "dronepass/version-patches_en.txt"),
            server.requested,
        )
        assertEquals(1, server.fallbackCount)
    }

    @Test
    fun `404가 아닌 실패는 공용 파일로 대체하지 않고 그대로 던진다`() = runBlocking {
        val failures = listOf(
            IOException("network"),
            SocketTimeoutException("timeout"),
            httpError(500),
            httpError(503),
            httpError(403),
        )
        for (failure in failures) {
            val server = FakeServer(
                mapOf(
                    "dronepass/version-patches-android.txt" to { throw failure },
                    "dronepass/version-patches.txt" to { "shared" },
                ),
            )

            val result = runCatching {
                server.fetch("dronepass/version-patches-android.txt", "dronepass/version-patches.txt")
            }

            assertSame(failure, result.exceptionOrNull())
            assertEquals(listOf("dronepass/version-patches-android.txt"), server.requested)
            assertEquals(0, server.fallbackCount)
        }
    }

    @Test
    fun `공용 파일도 실패하면 그 실패를 그대로 돌려준다`() = runBlocking {
        val sharedFailure = httpError(404)
        val server = FakeServer(
            mapOf(
                "dronepass/version-patches-android.txt" to { throw httpError(404) },
                "dronepass/version-patches.txt" to { throw sharedFailure },
            ),
        )

        val result = runCatching {
            server.fetch("dronepass/version-patches-android.txt", "dronepass/version-patches.txt")
        }

        assertSame(sharedFailure, result.exceptionOrNull())
        assertEquals(
            listOf("dronepass/version-patches-android.txt", "dronepass/version-patches.txt"),
            server.requested,
        )
    }

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
        assertEquals(
            "dronepass/version-patches.txt",
            localizedDocumentPath("dronepass/version-patches", null),
        )
        assertEquals(
            "dronepass/version-patches.txt",
            localizedDocumentPath("dronepass/version-patches", ""),
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
