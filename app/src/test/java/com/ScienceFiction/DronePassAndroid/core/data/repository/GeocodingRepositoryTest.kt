package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.core.data.remote.model.LandAddition
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingLand
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingRegion
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingResult
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.RegionArea
import org.junit.Assert.assertEquals
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

    private fun region(): ReverseGeocodingRegion {
        return ReverseGeocodingRegion(
            area0 = null,
            area1 = RegionArea(name = "서울특별시", coords = null),
            area2 = RegionArea(name = "강남구", coords = null),
            area3 = RegionArea(name = "역삼동", coords = null),
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
}
