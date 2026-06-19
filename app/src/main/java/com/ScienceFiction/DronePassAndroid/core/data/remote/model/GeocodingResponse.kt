package com.ScienceFiction.DronePassAndroid.core.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * 네이버 Geocoding API 응답 모델
 * 주소 -> 좌표 변환 결과
 *
 * 모든 필드를 nullable 로 정의한다. Naver API 가 에러 응답을 반환하거나
 * 부분 결과(addresses 없음, x/y 누락)를 반환하는 경우 Moshi 디코딩이
 * JsonDataException 으로 실패하는 것을 방지한다. 호출자는 malformed 응답을
 * 에러로 노출하거나, 주소 결과의 nullable 좌표를 명시적으로 처리한다.
 */
@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val status: String?,
    val meta: GeocodingMeta?,
    val addresses: List<GeocodingAddress>?
)

@JsonClass(generateAdapter = true)
data class GeocodingMeta(
    val totalCount: Int?,
    val page: Int?,
    val count: Int?
)

@JsonClass(generateAdapter = true)
data class GeocodingAddress(
    val roadAddress: String?,
    val jibunAddress: String?,
    val englishAddress: String?,
    val addressElements: List<GeocodingAddressElement>? = null,
    val x: String?,  // 경도 (longitude) - 에러 응답 시 누락 가능
    val y: String?,  // 위도 (latitude) - 에러 응답 시 누락 가능
    val distance: Double?
)

@JsonClass(generateAdapter = true)
data class GeocodingAddressElement(
    val types: List<String>?,
    val longName: String?,
    val shortName: String?,
    val code: String?
)
