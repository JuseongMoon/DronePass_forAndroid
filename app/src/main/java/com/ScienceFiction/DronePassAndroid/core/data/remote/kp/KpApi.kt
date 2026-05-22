package com.ScienceFiction.DronePassAndroid.core.data.remote.kp

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
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
     * NOAA SWPC Kp 지수 예보 데이터.
     *
     * 응답 형식: 객체 배열 `[{"time_tag":"...","kp":1.0,"observed":"observed","noaa_scale":"0"}, ...]`
     * (옛 응답은 2D 배열 `[["time_tag","kp",...],[...]]` 이었으나 NOAA 가 객체 배열로 전환).
     */
    @GET("products/noaa-planetary-k-index-forecast.json")
    suspend fun getKpForecast(): List<KpForecastItemDto>
}

/**
 * NOAA Kp 예보 응답의 단일 항목 DTO.
 *
 * 필드는 iOS [ForecastAPIResponse](`/DronePass/Core/Models/KPIndexModel.swift`) 와 동일.
 * `observed` / `noaa_scale` 은 응답에 따라 누락될 수 있어 nullable.
 */
@JsonClass(generateAdapter = true)
data class KpForecastItemDto(
    @Json(name = "time_tag") val timeTag: String,
    @Json(name = "kp") val kp: Double,
    @Json(name = "observed") val observed: String? = null,
    @Json(name = "noaa_scale") val noaaScale: String? = null,
)

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
