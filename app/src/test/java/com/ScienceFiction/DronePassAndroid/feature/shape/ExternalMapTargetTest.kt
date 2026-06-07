package com.ScienceFiction.DronePassAndroid.feature.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalMapTargetTest {

    @Test
    fun `한국 특화 기능이 켜져 있으면 iOS처럼 네이버 카카오 티맵 구글 순서로 표시한다`() {
        val targets = buildExternalMapTargets(
            latitude = 37.5,
            longitude = 127.0,
            encodedName = "%EB%AA%A9%EC%A0%81%EC%A7%80",
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
    fun `한국 특화 기능이 꺼져 있으면 iOS처럼 구글 지도만 표시한다`() {
        val targets = buildExternalMapTargets(
            latitude = 37.5,
            longitude = 127.0,
            encodedName = "%EB%AA%A9%EC%A0%81%EC%A7%80",
            koreaFeaturesEnabled = false,
        )

        assertEquals(listOf(ExternalMapProvider.GOOGLE), targets.map { it.provider })
    }

    @Test
    fun `외부 지도 target 은 Android Play Store 폴백 패키지를 가진다`() {
        val targets = buildExternalMapTargets(
            latitude = 37.5,
            longitude = 127.0,
            encodedName = "%EB%AA%A9%EC%A0%81%EC%A7%80",
            koreaFeaturesEnabled = true,
        )

        assertEquals("com.nhn.android.nmap", targets.first { it.provider == ExternalMapProvider.NAVER }.packageName)
        assertEquals("net.daum.android.map", targets.first { it.provider == ExternalMapProvider.KAKAO }.packageName)
        assertEquals("com.skt.tmap.ku", targets.first { it.provider == ExternalMapProvider.TMAP }.packageName)
        assertEquals(
            "com.google.android.apps.maps",
            targets.first { it.provider == ExternalMapProvider.GOOGLE }.packageName,
        )
        targets.forEach { target ->
            assertEquals("market://details?id=${target.packageName}", target.marketUri)
            assertTrue(target.playStoreUri.endsWith("id=${target.packageName}"))
        }
    }

    @Test
    fun `목적지 이름 공백은 iOS URL percent encoding 처럼 퍼센트 이스케이프한다`() {
        val encodedName = encodeExternalMapDestinationName("Drone Zone 1")
        val targets = buildExternalMapTargets(
            latitude = 37.5,
            longitude = 127.0,
            encodedName = encodedName,
            koreaFeaturesEnabled = true,
        )

        assertEquals("Drone%20Zone%201", encodedName)
        assertTrue(
            targets.first { it.provider == ExternalMapProvider.NAVER }
                .appUri
                .contains("dname=Drone%20Zone%201")
        )
        assertTrue(
            targets.first { it.provider == ExternalMapProvider.TMAP }
                .appUri
                .contains("goalname=Drone%20Zone%201")
        )
    }

    @Test
    fun `외부 지도 target 은 iOS와 같은 좌표와 자동차 길찾기 목적을 담는다`() {
        val targets = buildExternalMapTargets(
            latitude = 37.5,
            longitude = 127.0,
            encodedName = "Drone%20Zone%201",
            koreaFeaturesEnabled = true,
        )

        assertEquals(
            "google.navigation:q=37.5,127.0&mode=d",
            targets.first { it.provider == ExternalMapProvider.GOOGLE }.appUri,
        )
        assertEquals(
            "kakaomap://route?ep=37.5,127.0&by=CAR",
            targets.first { it.provider == ExternalMapProvider.KAKAO }.appUri,
        )
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=37.5,127.0",
            targets.first { it.provider == ExternalMapProvider.GOOGLE }.webFallbackUri,
        )
        assertEquals(
            "https://map.kakao.com/link/map/37.5,127.0",
            targets.first { it.provider == ExternalMapProvider.KAKAO }.webFallbackUri,
        )
    }
}
