package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingAddress
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingAddressElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAddressSheetTest {

    @Test
    fun `주소 검색 시트는 iOS처럼 medium large 전환을 위해 부분 확장을 허용한다`() {
        assertFalse(SearchAddressSheetSkipPartiallyExpanded)
    }

    @Test
    fun `주소 검색 시트는 iOS ShapeEditView처럼 외부 인터랙티브 닫기를 막는다`() {
        assertFalse(SearchAddressSheetInteractiveDismissEnabled)
    }

    @Test
    fun `주소 검색 시트는 iOS ShapeEditView처럼 85퍼센트 detent 로 표시한다`() {
        assertEquals(0.85f, SearchAddressSheetHeightFraction)
    }

    @Test
    fun `주소 검색 버튼은 iOS처럼 빈 문자열일 때만 비활성화한다`() {
        assertFalse(shouldEnableSearchAddressSubmit(""))
        assertTrue(shouldEnableSearchAddressSubmit("역"))
        assertTrue(shouldEnableSearchAddressSubmit("서초동"))
    }

    @Test
    fun `주소 검색바는 iOS SearchBar처럼 회색 배경과 8dp 모서리를 사용한다`() {
        assertEquals(8.dp, SearchAddressBarCornerRadius)
        assertEquals(8.dp, SearchAddressBarInnerPadding)
        assertEquals(16.dp, SearchAddressSearchButtonHorizontalPadding)
        assertEquals(8.dp, SearchAddressSearchButtonVerticalPadding)
        assertEquals(0xFFF2F2F7.toInt(), SearchAddressBarBackgroundColor.toArgb())
        assertEquals(0xFF8E8E93.toInt(), SearchAddressBarIconColor.toArgb())
    }

    @Test
    fun `주소 검색 결과 카드는 iOS처럼 8dp 그림자와 6퍼센트 검정 그림자를 사용한다`() {
        assertEquals(8.dp, SearchAddressResultCardShadowElevation)
        assertEquals(0x0F000000, SearchAddressResultCardShadowColor.toArgb())
    }

    @Test
    fun `주소 검색 안내 카드는 iOS처럼 500dp 최대 폭과 20dp 모서리를 사용한다`() {
        assertEquals(500.dp, SearchAddressGuideCardMaxWidth)
        assertEquals(20.dp, SearchAddressGuideCardCornerRadius)
        assertEquals(16.dp, SearchAddressGuideCardPadding)
        assertEquals(8.dp, SearchAddressGuideCardVerticalSpacing)
    }

    @Test
    fun `주소 검색 결과 선택 시 iOS처럼 지번 주소를 우선 저장한다`() {
        val address = GeocodingAddress(
            roadAddress = "서울특별시 서초구 서초대로78길 24",
            jibunAddress = "서울특별시 서초구 서초동 1305-6",
            englishAddress = null,
            x = "127.027",
            y = "37.497",
            distance = null,
        )

        assertEquals(
            "서울특별시 서초구 서초동 1305-6",
            resolveSelectedAddressForShapeEdit(address),
        )
    }

    @Test
    fun `지번 주소가 없을 때만 도로명 주소를 fallback으로 저장한다`() {
        val address = GeocodingAddress(
            roadAddress = "서울특별시 서초구 서초대로78길 24",
            jibunAddress = null,
            englishAddress = null,
            x = "127.027",
            y = "37.497",
            distance = null,
        )

        assertEquals(
            "서울특별시 서초구 서초대로78길 24",
            resolveSelectedAddressForShapeEdit(address),
        )
    }

    @Test
    fun `주소 검색 결과 카드는 iOS처럼 지번 다음 도로명 순서로 표시한다`() {
        val rows = addressDisplayRows(
            GeocodingAddress(
                roadAddress = "서울특별시 서초구 서초대로78길 24",
                jibunAddress = "서울특별시 서초구 서초동 1305-6",
                englishAddress = null,
                x = "127.027",
                y = "37.497",
                distance = null,
            ),
        )

        assertEquals(
            listOf(
                AddressDisplayRow(
                    type = AddressDisplayType.JIBUN,
                    text = "서울특별시 서초구 서초동 1305-6",
                ),
                AddressDisplayRow(
                    type = AddressDisplayType.ROAD,
                    text = "서울특별시 서초구 서초대로78길 24",
                ),
            ),
            rows,
        )
    }

    @Test
    fun `주소 검색 결과 카드는 iOS처럼 BUILDING_NAME 주소 요소를 건물명으로 표시한다`() {
        val address = GeocodingAddress(
            roadAddress = "서울특별시 서초구 서초대로78길 24",
            jibunAddress = "서울특별시 서초구 서초동 1305-6",
            englishAddress = null,
            addressElements = listOf(
                GeocodingAddressElement(
                    types = listOf("BUILDING_NAME"),
                    longName = "GT타워",
                    shortName = "GT타워",
                    code = "",
                ),
            ),
            x = "127.027",
            y = "37.497",
            distance = null,
        )

        assertEquals("GT타워", addressBuildingName(address))
    }

    @Test
    fun `주소 검색 결과 카드는 건물명 요소가 없으면 보조 줄을 표시하지 않는다`() {
        val address = GeocodingAddress(
            roadAddress = "서울특별시 서초구 서초대로78길 24",
            jibunAddress = "서울특별시 서초구 서초동 1305-6",
            englishAddress = null,
            addressElements = listOf(
                GeocodingAddressElement(
                    types = listOf("ROAD_NAME"),
                    longName = "서초대로78길",
                    shortName = "서초대로78길",
                    code = "",
                ),
            ),
            x = "127.027",
            y = "37.497",
            distance = null,
        )

        assertEquals(null, addressBuildingName(address))
    }

    @Test
    fun `주소 검색 안내 카드는 iOS처럼 초기 상태와 무결과 상태에 표시한다`() {
        assertTrue(
            shouldShowSearchAddressGuide(
                isLoading = false,
                errorMessage = null,
                hasResults = false,
            ),
        )
    }

    @Test
    fun `주소 검색 안내 카드는 로딩 에러 결과가 있으면 숨긴다`() {
        assertFalse(
            shouldShowSearchAddressGuide(
                isLoading = true,
                errorMessage = null,
                hasResults = false,
            ),
        )
        assertFalse(
            shouldShowSearchAddressGuide(
                isLoading = false,
                errorMessage = "network",
                hasResults = false,
            ),
        )
        assertFalse(
            shouldShowSearchAddressGuide(
                isLoading = false,
                errorMessage = null,
                hasResults = true,
            ),
        )
    }

    @Test
    fun `주소 검색 에러는 iOS처럼 원인 메시지를 함께 표시한다`() {
        assertEquals(
            "검색 중 오류가 발생했습니다: timeout",
            formatSearchAddressErrorMessage(
                causeMessage = "timeout",
                prefix = "검색 중 오류가 발생했습니다",
                fallback = "주소 검색 중 오류가 발생했습니다",
            ),
        )
        assertEquals(
            "주소 검색 중 오류가 발생했습니다",
            formatSearchAddressErrorMessage(
                causeMessage = "",
                prefix = "검색 중 오류가 발생했습니다",
                fallback = "주소 검색 중 오류가 발생했습니다",
            ),
        )
    }
}
