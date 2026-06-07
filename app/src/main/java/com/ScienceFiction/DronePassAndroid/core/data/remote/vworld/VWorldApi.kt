package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * VWorld WFS (Web Feature Service) API 인터페이스
 *
 * 비행구역 GeoJSON 데이터를 조회한다.
 * Base URL: https://api.vworld.kr/
 *
 * 응답을 [ResponseBody] 로 받아 호출자(VWorldRepository) 가 본문을 직접 검사한다.
 * VWorld 는 인증키가 유효하지 않거나 서버 에러 시 JSON 이 아닌 **XML** (`<ServiceException>`) 을
 * 반환하므로 Moshi 자동 디코딩으로는 사용자가 INVALID_KEY 같은 구체 오류를 알 수 없다.
 */
interface VWorldApi {

    /**
     * WFS GetFeature 요청
     *
     * @param service WFS 고정
     * @param version WFS 버전 (1.1.0)
     * @param request GetFeature 고정
     * @param typeName 레이어 typename 파라미터 값 (예: lt_c_aisprhc)
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
        @Query("typename") typeName: String,
        @Query("outputFormat") outputFormat: String = "application/json",
        @Query("srsname") srsName: String = "EPSG:4326",
        @Query("bbox") bbox: String,
        @Query("key") key: String,
        @Query("domain") domain: String = "드론패스",
        @Query("maxFeatures") maxFeatures: Int = 1000
    ): ResponseBody
}

/**
 * VWorld WFS 에러. 카테고리별 sealed 로 호출자가 사용자 메시지를 차별화하도록 한다.
 */
sealed class VWorldServiceException(message: String) : RuntimeException(message) {
    /** 인증키가 등록되지 않았거나 만료. `local.properties` 의 `VWORLD_API_KEY` 점검 필요. */
    class InvalidKey(detail: String) :
        VWorldServiceException("VWorld INVALID_KEY: $detail")

    /** 기타 OGC 서비스 예외 (도메인 미등록, 일별 호출 초과 등). 본문 앞 200자를 메시지로 보관. */
    class Other(detail: String) :
        VWorldServiceException("VWorld ServiceException: $detail")
}
