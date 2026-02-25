package com.ScienceFiction.DronePassAndroid.core.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 네이버 Reverse Geocoding API 응답 모델
 * 좌표 -> 주소 변환 결과
 */
@JsonClass(generateAdapter = true)
data class ReverseGeocodingResponse(
    val status: ReverseGeocodingStatus,
    val results: List<ReverseGeocodingResult>
)

@JsonClass(generateAdapter = true)
data class ReverseGeocodingStatus(
    val code: Int,
    val name: String,
    val message: String
)

@JsonClass(generateAdapter = true)
data class ReverseGeocodingResult(
    val name: String,  // "roadaddr" 또는 "addr"
    val code: ReverseGeocodingCode?,
    val region: ReverseGeocodingRegion,
    val land: ReverseGeocodingLand?
)

@JsonClass(generateAdapter = true)
data class ReverseGeocodingCode(
    val id: String?,
    val type: String?,
    val mappingId: String?
)

@JsonClass(generateAdapter = true)
data class ReverseGeocodingRegion(
    val area0: RegionArea?,  // 국가
    val area1: RegionArea?,  // 시/도
    val area2: RegionArea?,  // 구/군
    val area3: RegionArea?,  // 동/읍/면
    val area4: RegionArea?   // 리
)

@JsonClass(generateAdapter = true)
data class RegionArea(
    val name: String?,
    val coords: AreaCoords?
)

@JsonClass(generateAdapter = true)
data class AreaCoords(
    val center: AreaPoint?
)

@JsonClass(generateAdapter = true)
data class AreaPoint(
    val crs: String?,
    val x: Double?,
    val y: Double?
)

@JsonClass(generateAdapter = true)
data class ReverseGeocodingLand(
    val type: String?,
    val number1: String?,  // 번지
    val number2: String?,  // 호
    val name: String?,     // 도로명
    val addition0: LandAddition?,  // 건물명
    val addition1: LandAddition?,
    val addition2: LandAddition?
)

@JsonClass(generateAdapter = true)
data class LandAddition(
    val type: String?,
    val value: String?
)
