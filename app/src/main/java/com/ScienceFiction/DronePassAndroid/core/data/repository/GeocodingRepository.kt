package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingAddress
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingResult
import javax.inject.Inject
import javax.inject.Singleton

internal fun reverseGeocodingResultsToAddress(results: List<ReverseGeocodingResult>): String {
    val roadAddress = results
        .firstOrNull { it.name == "roadaddr" }
        ?.let(::buildRoadAddress)
        ?.takeIf { it.isNotEmpty() }
    if (roadAddress != null) return roadAddress

    return results
        .firstOrNull { it.name == "addr" }
        ?.let(::buildJibunAddress)
        ?.takeIf { it.isNotEmpty() }
        ?: ""
}

private fun buildRoadAddress(result: ReverseGeocodingResult): String {
    val parts = mutableListOf<String>()

    result.region.area1?.name?.let { parts.add(it) }
    result.region.area2?.name?.let { parts.add(it) }
    result.region.area3?.name?.let { parts.add(it) }

    result.land?.let { land ->
        land.name?.let { roadName ->
            parts.add(roadName)
            val number1 = land.number1.orEmpty()
            val number = if (!land.number2.isNullOrEmpty()) {
                "$number1-${land.number2}"
            } else {
                number1
            }
            parts.add(number)
        }
        land.addition0
            ?.takeIf { it.type == "building" }
            ?.value
            ?.takeIf { it.isNotEmpty() }
            ?.let { parts.add("($it)") }
    }

    return parts.joinToString(" ")
}

private fun buildJibunAddress(result: ReverseGeocodingResult): String {
    val parts = mutableListOf<String>()

    result.region.area1?.name?.let { parts.add(it) }
    result.region.area2?.name?.let { parts.add(it) }
    result.region.area3?.name?.let { parts.add(it) }

    result.land?.let { land ->
        land.number1?.takeIf { it.isNotEmpty() }?.let { number1 ->
            val number = if (!land.number2.isNullOrEmpty()) {
                "$number1-${land.number2}"
            } else {
                number1
            }
            parts.add(number)
        }
    }

    return parts.joinToString(" ")
}

@Singleton
class GeocodingRepository @Inject constructor(
    private val api: NaverGeocodingApi
) {

    /**
     * 주소로 좌표 검색 (Geocoding)
     * @param address 검색할 주소 문자열
     * @return 검색된 주소 목록 또는 에러
     */
    suspend fun geocode(address: String): Result<List<GeocodingAddress>> {
        return try {
            val response = api.geocode(address)
            if (response.status == "OK") {
                // x/y 가 누락된 결과(에러 응답의 부분 결과)는 사용 불가하므로 필터링.
                val valid = response.addresses
                    ?.filter { !it.x.isNullOrBlank() && !it.y.isNullOrBlank() }
                    .orEmpty()
                Result.success(valid)
            } else {
                Result.failure(Exception("Geocoding 실패: status=${response.status ?: "(unknown)"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 좌표로 주소 검색 (Reverse Geocoding)
     * 도로명 주소 우선, 없으면 지번 주소 반환
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 조합된 주소 문자열 또는 에러
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<String> {
        return try {
            // 네이버 API 규격: "경도,위도" 순서
            val coords = "$longitude,$latitude"
            val response = api.reverseGeocode(coords)

            if (response.status.code != 0) {
                return Result.failure(
                    Exception("Reverse Geocoding 실패: ${response.status.message}")
                )
            }

            // 1. "roadaddr" 결과 찾기
            Result.success(reverseGeocodingResultsToAddress(response.results))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
