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
        private const val PATH_PATCH_NOTES = "dronepass/version-patches"
    }

    @Volatile private var cachedTerms: CacheEntry<ParsedDocument>? = null
    @Volatile private var cachedPrivacy: CacheEntry<ParsedDocument>? = null
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

    /**
     * 패치노트는 캐시 우회 — iOS `.reloadIgnoringLocalCacheData` 정합.
     * 항상 최신 버전 fetch.
     */
    suspend fun fetchPatchNotes(): Result<List<PatchNote>> {
        return try {
            val body = documentApi.getDocument(
                path = localizedPath(PATH_PATCH_NOTES),
                cacheControl = "no-cache",
            )
            val content = body.string()
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
                val parsed = MarkdownParser.parseMarkdown(content)
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
        ?.substringBefore('-')
        ?.lowercase(Locale.ROOT)
    return if (primaryLanguage == "ko") "$base.txt" else "${base}_en.txt"
}
