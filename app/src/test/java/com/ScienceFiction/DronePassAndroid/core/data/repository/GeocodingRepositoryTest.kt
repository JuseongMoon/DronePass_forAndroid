package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingResponse
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.LandAddition
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingLand
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingRegion
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingResponse
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingResult
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingStatus
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.RegionArea
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingRepositoryTest {

    @Test
    fun `reverse geocode 주소는 iOS처럼 도로명 주소를 우선 사용한다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(
                result(name = "addr", land = land(number1 = "12")),
                result(name = "roadaddr", land = land(name = "테헤란로", number1 = "1")),
            ),
        )

        assertEquals("서울특별시 강남구 역삼동 테헤란로 1", address)
    }

    @Test
    fun `도로명 주소에 건물명이 있으면 iOS처럼 괄호로 붙인다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(
                result(
                    name = "roadaddr",
                    land = land(
                        name = "테헤란로",
                        number1 = "1",
                        buildingName = "GT타워",
                    ),
                ),
            ),
        )

        assertEquals("서울특별시 강남구 역삼동 테헤란로 1 (GT타워)", address)
    }

    @Test
    fun `도로명 주소가 없으면 지번 주소를 사용한다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(result(name = "addr", land = land(number1 = "12", number2 = "3"))),
        )

        assertEquals("서울특별시 강남구 역삼동 12-3", address)
    }

    @Test
    fun `주소 구성 요소가 없으면 빈 문자열을 반환한다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(
                result(
                    name = "addr",
                    region = ReverseGeocodingRegion(
                        area0 = null,
                        area1 = null,
                        area2 = null,
                        area3 = null,
                        area4 = null,
                    ),
                    land = null,
                ),
            ),
        )

        assertEquals("", address)
    }

    @Test
    fun `도로명 주소 빈 문자열 구성 요소도 iOS처럼 제거하지 않는다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(
                result(
                    name = "roadaddr",
                    region = region(area1 = "", area2 = " ", area3 = "역삼동"),
                    land = land(name = "", number1 = "12", number2 = " "),
                ),
            ),
        )

        assertEquals("   역삼동  12- ", address)
    }

    @Test
    fun `도로명이 nil 이면 iOS처럼 도로명 건물번호를 붙이지 않는다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(
                result(
                    name = "roadaddr",
                    land = land(name = null, number1 = "12", number2 = "3"),
                ),
            ),
        )

        assertEquals("서울특별시 강남구 역삼동", address)
    }

    @Test
    fun `도로명 건물명은 iOS처럼 빈 문자열만 제외하고 공백은 보존한다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(
                result(
                    name = "roadaddr",
                    land = land(name = "테헤란로", number1 = "1", buildingName = " "),
                ),
            ),
        )

        assertEquals("서울특별시 강남구 역삼동 테헤란로 1 ( )", address)
    }

    @Test
    fun `지번 주소도 iOS처럼 빈 문자열이 아닌 공백 지번을 보존한다`() {
        val address = reverseGeocodingResultsToAddress(
            listOf(
                result(
                    name = "addr",
                    land = land(number1 = " ", number2 = "2"),
                ),
            ),
        )

        assertEquals("서울특별시 강남구 역삼동  -2", address)
    }

    @Test
    fun `역지오코딩 성공 응답에 주소 후보가 없으면 iOS처럼 실패로 처리한다`() = runBlocking {
        val api = FakeNaverGeocodingApi(
            reverseResponse = ReverseGeocodingResponse(
                status = ReverseGeocodingStatus(code = 0, name = "ok", message = "done"),
                results = listOf(result(name = "admcode")),
            ),
        )
        val repository = GeocodingRepository(api = api)

        val result = repository.reverseGeocode(latitude = 37.5665, longitude = 126.9780)

        assertTrue(result.isFailure)
        assertEquals("126.978,37.5665", api.lastReverseCoords)
    }

    @Test
    fun `역지오코딩 주소 후보 판정은 iOS roadaddr addr 결과만 허용한다`() {
        assertEquals(true, hasReverseGeocodingAddressResult(listOf(result(name = "roadaddr"))))
        assertEquals(true, hasReverseGeocodingAddressResult(listOf(result(name = "addr"))))
        assertEquals(false, hasReverseGeocodingAddressResult(emptyList()))
        assertEquals(false, hasReverseGeocodingAddressResult(listOf(result(name = "admcode"))))
    }

    private fun result(
        name: String,
        region: ReverseGeocodingRegion = region(),
        land: ReverseGeocodingLand? = null,
    ): ReverseGeocodingResult {
        return ReverseGeocodingResult(
            name = name,
            code = null,
            region = region,
            land = land,
        )
    }

    private fun region(
        area1: String = "서울특별시",
        area2: String = "강남구",
        area3: String = "역삼동",
    ): ReverseGeocodingRegion {
        return ReverseGeocodingRegion(
            area0 = null,
            area1 = RegionArea(name = area1, coords = null),
            area2 = RegionArea(name = area2, coords = null),
            area3 = RegionArea(name = area3, coords = null),
            area4 = null,
        )
    }

    private fun land(
        name: String? = null,
        number1: String? = null,
        number2: String? = null,
        buildingName: String? = null,
    ): ReverseGeocodingLand {
        return ReverseGeocodingLand(
            type = null,
            number1 = number1,
            number2 = number2,
            name = name,
            addition0 = buildingName?.let { LandAddition(type = "building", value = it) },
            addition1 = null,
            addition2 = null,
        )
    }

    private class FakeNaverGeocodingApi(
        private val reverseResponse: ReverseGeocodingResponse,
    ) : NaverGeocodingApi {
        var lastReverseCoords: String? = null
            private set

        override suspend fun geocode(address: String): GeocodingResponse {
            return GeocodingResponse(status = "OK", meta = null, addresses = emptyList())
        }

        override suspend fun reverseGeocode(
            coords: String,
            orders: String,
            output: String,
        ): ReverseGeocodingResponse {
            lastReverseCoords = coords
            return reverseResponse
        }
    }
}
