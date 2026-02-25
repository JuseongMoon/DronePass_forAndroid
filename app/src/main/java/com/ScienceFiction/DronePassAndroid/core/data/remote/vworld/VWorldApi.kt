package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * VWorld WFS (Web Feature Service) API 인터페이스
 *
 * 비행구역 GeoJSON 데이터를 조회한다.
 * Base URL: https://api.vworld.kr/
 */
interface VWorldApi {

    /**
     * WFS GetFeature 요청
     *
     * @param service WFS 고정
     * @param version WFS 버전 (1.1.0)
     * @param request GetFeature 고정
     * @param typeName 레이어 typename (예: lt_c_aisprhc)
     * @param outputFormat 응답 형식 (application/json)
     * @param srsName 좌표 체계 (EPSG:4326)
     * @param bbox 바운딩 박스 (minLon,minLat,maxLon,maxLat)
     * @param key VWorld API 키
     * @param domain 요청 도메인
     * @param maxFeatures 최대 반환 피처 수
     */
    @GET("req/wfs")
    suspend fun getFeatures(
        @Query("service") service: String = "WFS",
        @Query("version") version: String = "1.1.0",
        @Query("request") request: String = "GetFeature",
        @Query("typeName") typeName: String,
        @Query("outputFormat") outputFormat: String = "application/json",
        @Query("srsName") srsName: String = "EPSG:4326",
        @Query("bbox") bbox: String,
        @Query("key") key: String,
        @Query("domain") domain: String = "드론패스",
        @Query("maxFeatures") maxFeatures: Int = 1000
    ): VWorldWfsResponse
}
