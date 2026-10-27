package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.content.Context
import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.data.remote.document.DocumentApi
import com.ScienceFiction.DronePassAndroid.core.util.MarkdownParser
import com.ScienceFiction.DronePassAndroid.domain.model.ParsedDocument
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNote
import com.ScienceFiction.DronePassAndroid.feature.settings.resolveCurrentAppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자체 서버 문서 fetch + 메모리 캐시 + 마크다운 파싱.
 *
 * iOS `FetchWebDocuments` 정합:
 *  - 언어별 파일명 (`{base}.txt` 한 / `{base}_en.txt` 영)
 *  - PatchNotes: `Cache-Control: no-cache` 헤더 + 캐시 우회 (iOS `.reloadIgnoringLocalCacheData`)
 *
 * Terms/Privacy 는 같은 언어 경로에서 반복 네트워크 호출을 줄이기 위해 Android 쪽에서만
 * 짧은 메모리 캐시를 둔다. 언어별 경로와 파싱 결과는 iOS와 같은 형식을 유지한다.
 *
 * 사용자가 언어 변경 시 [invalidateCache] 호출 권장.
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val documentApi: DocumentApi,
    @ApplicationContext private val appContext: Context,
) {
    companion object {
        private const val TAG = "DocumentRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30분

        private const val PATH_TERMS = "dronepass/terms/termsofservice"
        private const val PATH_PRIVACY = "dronepass/terms/privacypolicy"
        private const val PATH_LOCATION_TERMS = "dronepass/terms/locationterms"

        /** Android 전용 패치노트(3.6.3부터, L-1). */
        internal const val PATH_PATCH_NOTES = "dronepass/version-patches-android"

        /** 3.6.2 이하 두 앱이 함께 읽는 공용 패치노트. Android 파일이 없을 때(HTTP 404)만 읽는다. */
        internal const val PATH_PATCH_NOTES_SHARED = "dronepass/version-patches"
    }

    @Volatile private var cachedTerms: CacheEntry<ParsedDocument>? = null
    @Volatile private var cachedPrivacy: CacheEntry<ParsedDocument>? = null
    @Volatile private var cachedLocationTerms: CacheEntry<ParsedDocument>? = null
    private val mutex = Mutex()

    /** 현재 앱 언어 기준 파일명 suffix 결정. `ko` → `.txt`, 그 외 → `_en.txt`. */
    private fun localizedPath(base: String): String {
        return localizedDocumentPath(
            base = base,
            languageTag = resolveCurrentAppLanguage(appContext).tag,
        )
    }

    suspend fun fetchTerms(): Result<ParsedDocument> = fetchCachedDocument(
        cacheGet = { cachedTerms },
        cacheSet = { cachedTerms = it },
        path = localizedPath(PATH_TERMS),
        label = "Terms",
    )

    suspend fun fetchPrivacyPolicy(): Result<ParsedDocument> = fetchCachedDocument(
        cacheGet = { cachedPrivacy },
        cacheSet = { cachedPrivacy = it },
        path = localizedPath(PATH_PRIVACY),
        label = "PrivacyPolicy",
    )

    /** 위치기반서비스 이용약관. 이용약관과 같은 방식(`locationterms.txt` / `_en.txt`). */
    suspend fun fetchLocationTerms(): Result<ParsedDocument> = fetchCachedDocument(
        cacheGet = { cachedLocationTerms },
        cacheSet = { cachedLocationTerms = it },
        path = localizedPath(PATH_LOCATION_TERMS),
        label = "LocationTerms",
    )

    /**
     * 패치노트는 캐시 우회 — iOS `.reloadIgnoringLocalCacheData` 정합.
     * 항상 최신 버전 fetch. Android 파일이 서버에 없으면(404) 같은 언어의 공용 파일을 읽는다.
     */
    suspend fun fetchPatchNotes(): Result<List<PatchNote>> {
        return try {
            val content = fetchWithSharedFallback(
                path = localizedPath(PATH_PATCH_NOTES),
                sharedPath = localizedPath(PATH_PATCH_NOTES_SHARED),
                fetch = { path -> documentApi.getDocument(path = path, cacheControl = "no-cache").string() },
                onSharedFallback = { Log.i(TAG, "PatchNotes: Android 파일이 없어(404) 공용 파일을 읽습니다") },
            )
            val notes = MarkdownParser.parsePatchNotes(content)
            Result.success(notes)
        } catch (e: Exception) {
            Log.w(TAG, "PatchNotes 로드 실패: ${e.message}")
            Result.failure(e)
        }
    }

    /** 언어 변경 등 외부 트리거 시 호출 — 모든 캐시 무효화. */
    fun invalidateCache() {
        cachedTerms = null
        cachedPrivacy = null
        cachedLocationTerms = null
    }

    private suspend fun fetchCachedDocument(
        cacheGet: () -> CacheEntry<ParsedDocument>?,
        cacheSet: (CacheEntry<ParsedDocument>?) -> Unit,
        path: String,
        label: String,
    ): Result<ParsedDocument> {
        // 캐시 hit-path (락 없음)
        cacheGet()?.let { entry ->
            if (entry.path == path && System.currentTimeMillis() - entry.fetchedAt < CACHE_DURATION_MS) {
                return Result.success(entry.value)
            }
        }

        // miss-path 직렬화
        return mutex.withLock {
            // 락 획득 후 재확인 (다른 코루틴이 갱신했을 수 있음)
            cacheGet()?.let { entry ->
                if (entry.path == path && System.currentTimeMillis() - entry.fetchedAt < CACHE_DURATION_MS) {
                    return@withLock Result.success(entry.value)
                }
            }

            try {
                val body = documentApi.getDocument(path = path)
                val content = body.string()
                val parsed = requireDisplayableParsedDocument(
                    document = MarkdownParser.parseMarkdown(content),
                    label = label,
                )
                cacheSet(CacheEntry(value = parsed, path = path, fetchedAt = System.currentTimeMillis()))
                Result.success(parsed)
            } catch (e: Exception) {
                Log.w(TAG, "$label 로드 실패 (path=$path): ${e.message}")
                Result.failure(e)
            }
        }
    }

    private data class CacheEntry<T>(
        val value: T,
        val path: String, // 언어 변경 감지용 (path 가 바뀌면 캐시 miss)
        val fetchedAt: Long,
    )
}

internal fun localizedDocumentPath(base: String, languageTag: String?): String {
    val primaryLanguage = languageTag
        ?.takeIf { it.isNotBlank() }
        ?.substringBefore('-')
        ?.lowercase(Locale.ROOT)
        ?: "ko"
    return if (primaryLanguage == "ko") "$base.txt" else "${base}_en.txt"
}

/**
 * [path] 를 읽고, 서버에 그 파일이 없을 때(HTTP 404)만 [sharedPath] 를 읽는다(L-1).
 * 네트워크 오류·타임아웃·5xx 같은 다른 실패는 대체하지 않고 그대로 던진다.
 */
internal suspend fun fetchWithSharedFallback(
    path: String,
    sharedPath: String,
    fetch: suspend (path: String) -> String,
    onSharedFallback: () -> Unit = {},
): String {
    return try {
        fetch(path)
    } catch (e: HttpException) {
        if (e.code() != 404) throw e
        onSharedFallback()
        fetch(sharedPath)
    }
}

internal fun parsedDocumentHasDisplayableElements(document: ParsedDocument): Boolean {
    return document.elements.isNotEmpty()
}

internal fun requireDisplayableParsedDocument(
    document: ParsedDocument,
    label: String,
): ParsedDocument {
    if (!parsedDocumentHasDisplayableElements(document)) {
        throw IllegalStateException("$label document is empty")
    }
    return document
}
