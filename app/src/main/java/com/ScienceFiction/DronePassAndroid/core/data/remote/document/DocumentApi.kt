package com.ScienceFiction.DronePassAndroid.core.data.remote.document

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * 자체 서버 문서 fetch API (iOS `FetchWebDocuments` 정합).
 *
 * baseUrl: `https://sciencefiction.co.kr/`
 * 응답은 plain text (마크다운/패치노트 형식) — Moshi 변환 없이 [ResponseBody] 로 받아 직접 디코딩.
 *
 * 경로 예:
 *  - `dronepass/terms/termsofservice.txt`
 *  - `dronepass/terms/privacypolicy.txt`
 *  - `dronepass/terms/locationservice.txt`
 *  - `dronepass/version-patches.txt`
 */
interface DocumentApi {
    /**
     * 임의 경로의 plain text 문서를 가져온다.
     *
     * @param path "dronepass/..." 형식의 경로. `/` 가 포함되므로 `encoded=true` 로 retrofit 의
     *   기본 인코딩 우회.
     * @param cacheControl 옵션 — 패치노트는 `"no-cache"` 로 항상 최신 fetch.
     */
    @GET("{path}")
    suspend fun getDocument(
        @Path("path", encoded = true) path: String,
        @Header("Cache-Control") cacheControl: String? = null,
    ): ResponseBody
}
