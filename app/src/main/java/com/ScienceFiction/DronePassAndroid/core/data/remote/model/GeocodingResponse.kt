package com.ScienceFiction.DronePassAndroid.core.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 네이버 Geocoding API 응답 모델
 * 주소 -> 좌표 변환 결과
 */
@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val status: String,
    val meta: GeocodingMeta?,
    val addresses: List<GeocodingAddress>
)

@JsonClass(generateAdapter = true)
data class GeocodingMeta(
    val totalCount: Int,
    val page: Int,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class GeocodingAddress(
    val roadAddress: String?,
    val jibunAddress: String?,
    val englishAddress: String?,
    val x: String,  // 경도 (longitude)
    val y: String,  // 위도 (latitude)
    val distance: Double?
)
