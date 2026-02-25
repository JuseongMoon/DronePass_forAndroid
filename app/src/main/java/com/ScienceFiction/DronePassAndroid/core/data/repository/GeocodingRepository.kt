package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingAddress
import javax.inject.Inject
import javax.inject.Singleton

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
                Result.success(response.addresses)
            } else {
                Result.failure(Exception("Geocoding 실패: status=${response.status}"))
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
            val roadResult = response.results.find { it.name == "roadaddr" }
            if (roadResult != null) {
                val address = buildRoadAddress(roadResult)
                if (address.isNotBlank()) {
                    return Result.success(address)
                }
            }

            // 2. "addr" 결과에서 region 정보 조합
            val addrResult = response.results.find { it.name == "addr" }
            if (addrResult != null) {
                val address = buildJibunAddress(addrResult)
                if (address.isNotBlank()) {
                    return Result.success(address)
                }
            }

            // 3. 둘 다 없으면 빈 문자열 반환 (UI에서 처리)
            Result.success("")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 도로명 주소 조합
     * region.area1.name + region.area2.name + region.area3.name + land.name + land.number1
     */
    private fun buildRoadAddress(
        result: com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingResult
    ): String {
        val parts = mutableListOf<String>()

        result.region.area1?.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        result.region.area2?.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        result.region.area3?.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        result.land?.let { land ->
            land.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            land.number1?.takeIf { it.isNotBlank() }?.let { number1 ->
                val number = if (!land.number2.isNullOrBlank()) {
                    "$number1-${land.number2}"
                } else {
                    number1
                }
                parts.add(number)
            }
        }

        return parts.joinToString(" ")
    }

    /**
     * 지번 주소 조합
     * region.area1.name + region.area2.name + region.area3.name + land.number1
     */
    private fun buildJibunAddress(
        result: com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingResult
    ): String {
        val parts = mutableListOf<String>()

        result.region.area1?.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        result.region.area2?.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        result.region.area3?.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        result.land?.let { land ->
            land.number1?.takeIf { it.isNotBlank() }?.let { number1 ->
                val number = if (!land.number2.isNullOrBlank()) {
                    "$number1-${land.number2}"
                } else {
                    number1
                }
                parts.add(number)
            }
        }

        return parts.joinToString(" ")
    }
}
