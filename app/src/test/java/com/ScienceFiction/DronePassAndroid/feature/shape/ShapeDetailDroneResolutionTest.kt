package com.ScienceFiction.DronePassAndroid.feature.shape

import android.text.util.Linkify
import android.webkit.WebSettings
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeDetailDroneResolutionTest {

    @Test
    fun `연결된 드론이 있으면 해당 드론을 표시한다`() {
        val matchedDrone = DroneModel(id = "drone-a", name = "Alpha")
        val fallbackDrone = DroneModel(id = "drone-b", name = "Bravo")

        val result = resolveShapeDetailDrone(
            shapeDroneId = "drone-a",
            matchedDrone = matchedDrone,
            activeDrones = listOf(fallbackDrone),
        )

        assertEquals(matchedDrone, result)
    }

    @Test
    fun `droneId 가 없는 레거시 도형은 iOS처럼 첫 번째 활성 드론을 표시한다`() {
        val firstDrone = DroneModel(id = "drone-first", name = "First")
        val secondDrone = DroneModel(id = "drone-second", name = "Second")

        val result = resolveShapeDetailDrone(
            shapeDroneId = null,
            matchedDrone = null,
            activeDrones = listOf(firstDrone, secondDrone),
        )

        assertEquals(firstDrone, result)
    }

    @Test
    fun `droneId 는 있지만 드론을 찾지 못하면 삭제된 드론 상태를 유지한다`() {
        val result = resolveShapeDetailDrone(
            shapeDroneId = "deleted-drone",
            matchedDrone = null,
            activeDrones = listOf(DroneModel(id = "other-drone", name = "Other")),
        )

        assertNull(result)
    }

    @Test
    fun `droneId 와 다른 드론이 전달되면 iOS처럼 연결된 드론으로 표시하지 않는다`() {
        val result = resolveShapeDetailDrone(
            shapeDroneId = "drone-a",
            matchedDrone = DroneModel(id = "drone-b", name = "Wrong"),
            activeDrones = listOf(DroneModel(id = "drone-a", name = "Fallback")),
        )

        assertNull(result)
    }

    @Test
    fun `droneId 가 없는 레거시 도형은 전달된 드론보다 첫 번째 활성 드론을 우선한다`() {
        val firstDrone = DroneModel(id = "drone-first", name = "First")
        val result = resolveShapeDetailDrone(
            shapeDroneId = null,
            matchedDrone = DroneModel(id = "drone-passed", name = "Passed"),
            activeDrones = listOf(firstDrone),
        )

        assertEquals(firstDrone, result)
    }

    @Test
    fun `복사 토스트는 iOS CopyToastOverlay 표시 타이밍과 위치를 따른다`() {
        assertEquals(1_500L, ShapeDetailCopyToastDurationMs)
        assertEquals(300, ShapeDetailCopyToastAnimationDurationMs)
        assertEquals(0.75f, ShapeDetailCopyToastBackgroundAlpha, 0f)
        assertEquals(50f, ShapeDetailCopyToastBottomPadding.value, 0f)
        assertEquals(HapticFeedbackType.LongPress, ShapeDetailCopyHapticFeedbackType)
    }

    @Test
    fun `상세 시트 높이는 iOS presentationDetents fraction 0_8 과 맞춘다`() {
        assertEquals(0.8f, ShapeDetailSheetHeightFraction, 0f)
    }

    @Test
    fun `상세 시트 헤더는 iOS inline navigation title처럼 좌우 슬롯 폭을 맞춘다`() {
        assertEquals(44.dp, ShapeDetailNavigationHeaderHeight)
        assertEquals(44.dp, ShapeDetailNavigationHeaderSideWidth)
    }

    @Test
    fun `상세 시트 더보기 버튼은 iOS ellipsis circle 심볼 치수를 따른다`() {
        assertEquals(24.dp, ShapeDetailMoreCircleSize)
        assertEquals(1.5.dp, ShapeDetailMoreCircleStrokeWidth)
        assertEquals(18.dp, ShapeDetailMoreDotsSize)
    }

    @Test
    fun `상세 정보 행 최소 높이는 iOS defaultMinListRowHeight 44와 맞춘다`() {
        assertEquals(44.dp, ShapeDetailRowMinHeight)
    }

    @Test
    fun `상세 정보 행 구분선은 iOS List separator처럼 얇게 표시한다`() {
        assertEquals(0.5.dp, ShapeDetailRowDividerThickness)
    }

    @Test
    fun `메모 영역 높이는 iOS ShapeDetailView의 180pt 고정 높이와 맞춘다`() {
        assertEquals(180.dp, ShapeDetailMemoHeight)
    }

    @Test
    fun `외부 지도 앱 선택은 iOS confirmationDialog처럼 하단 액션 시트로 표시한다`() {
        assertTrue(ShapeDetailExternalMapSkipPartiallyExpanded)
    }

    @Test
    fun `메모 웹 링크는 iOS SafariView처럼 앱 내부 웹 시트로 연다`() {
        assertEquals(ShapeDetailMemoLinkAction.IN_APP_WEB, resolveShapeDetailMemoLinkAction("https://example.com"))
        assertEquals(ShapeDetailMemoLinkAction.IN_APP_WEB, resolveShapeDetailMemoLinkAction("HTTP://example.com"))
        assertEquals(ShapeDetailMemoLinkAction.SYSTEM_INTENT, resolveShapeDetailMemoLinkAction("tel:0312909221"))
        assertEquals(ShapeDetailMemoLinkAction.SYSTEM_INTENT, resolveShapeDetailMemoLinkAction("mailto:pilot@example.com"))
        assertTrue(ShapeDetailMemoWebSheetSkipPartiallyExpanded)
        assertEquals(0.92f, ShapeDetailMemoWebSheetHeightFraction, 0f)
    }

    @Test
    fun `메모 웹 링크 WebView는 임의 URL에 대한 JavaScript 로컬 접근과 혼합 콘텐츠를 차단한다`() {
        assertFalse(ShapeDetailMemoWebJavaScriptEnabled)
        assertTrue(ShapeDetailMemoWebDomStorageEnabled)
        assertFalse(ShapeDetailMemoWebJavaScriptCanOpenWindowsAutomatically)
        assertFalse(ShapeDetailMemoWebAllowFileAccess)
        assertFalse(ShapeDetailMemoWebAllowContentAccess)
        assertFalse(ShapeDetailMemoWebAllowFileAccessFromFileUrls)
        assertFalse(ShapeDetailMemoWebAllowUniversalAccessFromFileUrls)
        assertTrue(ShapeDetailMemoWebSafeBrowsingEnabled)
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, ShapeDetailMemoWebMixedContentMode)
    }

    @Test
    fun `드론 색상 원은 iOS처럼 팔레트 색상이 있을 때만 표시한다`() {
        assertTrue(shouldShowShapeDetailDroneColorIndicator(PaletteColor.BLUE))
        assertFalse(shouldShowShapeDetailDroneColorIndicator(null))
    }

    @Test
    fun `메모 자동 감지는 iOS처럼 웹 링크와 전화번호만 사용한다`() {
        assertEquals(
            Linkify.WEB_URLS or Linkify.PHONE_NUMBERS,
            ShapeDetailMemoAutoLinkMask,
        )
        assertFalse(ShapeDetailMemoAutoLinkMask and Linkify.EMAIL_ADDRESSES != 0)
    }

    @Test
    fun `상세 주소 표시는 iOS처럼 nil 일 때만 대시로 대체한다`() {
        assertEquals("-", formatShapeDetailAddress(null))
        assertEquals("", formatShapeDetailAddress(""))
        assertEquals("서울특별시 중구 세종대로 110", formatShapeDetailAddress("서울특별시 중구 세종대로 110"))
    }

    @Test
    fun `상세 제목은 iOS ShapeDetailView처럼 빈 문자열도 원문 그대로 표시한다`() {
        assertEquals("", shapeDetailTitleText(""))
        assertEquals("   ", shapeDetailTitleText("   "))
        assertEquals("Flight Area", shapeDetailTitleText("Flight Area"))
    }

    @Test
    fun `상세 메모 표시는 iOS처럼 nil 일 때만 대시로 대체한다`() {
        assertEquals("-", formatShapeDetailMemo(null))
        assertEquals("", formatShapeDetailMemo(""))
        assertEquals("   ", formatShapeDetailMemo("   "))
        assertEquals("메모", formatShapeDetailMemo("메모"))
    }

    @Test
    fun `상세 주소 복사는 iOS copyableText처럼 nil 과 빈 문자열만 제외한다`() {
        assertNull(copyableShapeDetailAddress(null))
        assertNull(copyableShapeDetailAddress(""))
        assertEquals("   ", copyableShapeDetailAddress("   "))
        assertEquals("서울특별시 중구 세종대로 110", copyableShapeDetailAddress("서울특별시 중구 세종대로 110"))
    }

    @Test
    fun `한국 기능이 켜져 있으면 외부 지도 앱은 iOS처럼 네이버 카카오 티맵 구글 순서이다`() {
        val targets = buildExternalMapTargets(
            latitude = 37.5,
            longitude = 127.0,
            encodedName = "Flight%20Area",
            koreaFeaturesEnabled = true,
        )

        assertEquals(
            listOf(
                ExternalMapProvider.NAVER,
                ExternalMapProvider.KAKAO,
                ExternalMapProvider.TMAP,
                ExternalMapProvider.GOOGLE,
            ),
            targets.map { it.provider },
        )
    }

    @Test
    fun `한국 기능이 꺼져 있으면 외부 지도 앱은 iOS처럼 구글만 표시한다`() {
        val targets = buildExternalMapTargets(
            latitude = 37.5,
            longitude = 127.0,
            encodedName = "Flight%20Area",
            koreaFeaturesEnabled = false,
        )

        assertEquals(listOf(ExternalMapProvider.GOOGLE), targets.map { it.provider })
    }

    @Test
    fun `외부 지도 목적지 이름은 iOS처럼 URL query 값으로 인코딩한다`() {
        assertEquals("Flight%20Area", encodeExternalMapDestinationName("Flight Area"))
        assertEquals("%EB%B9%84%ED%96%89%20%EA%B5%AC%EC%97%AD", encodeExternalMapDestinationName("비행 구역"))
    }
}
