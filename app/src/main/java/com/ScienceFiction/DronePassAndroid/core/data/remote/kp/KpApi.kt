package com.ScienceFiction.DronePassAndroid.core.data.remote.kp

import okhttp3.ResponseBody
import retrofit2.http.GET

/**
 * Kp 지수 API 인터페이스
 *
 * 두 가지 소스를 사용:
 * 1. NOAA SWPC Forecast (JSON)
 * 2. GFZ Potsdam Nowcast (텍스트)
 */
interface KpNoaaApi {
    /**
     * NOAA SWPC Kp 지수 예보 데이터 (JSON 배열)
     */
    @GET("products/noaa-planetary-k-index-forecast.json")
    suspend fun getKpForecast(): ResponseBody
}

interface KpNoaa27DayApi {
    /**
     * NOAA SWPC 27일 장기 예보 (텍스트)
     * 고정폭 테이블 형식: 날짜, Kp, Ap 컬럼
     */
    @GET("text/27-day-outlook.txt")
    suspend fun get27DayOutlook(): ResponseBody
}

interface KpGfzApi {
    /**
     * GFZ Potsdam Kp 지수 실시간 데이터 (텍스트)
     */
    @GET("app/files/Kp_ap_nowcast.txt")
    suspend fun getKpNowcast(): ResponseBody
}
