package com.ScienceFiction.DronePassAndroid.core.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics 이벤트 로깅 헬퍼
 *
 * 앱 내 주요 사용자 액션을 Firebase Analytics에 기록한다.
 * Hilt를 통해 싱글톤으로 주입되며, 각 ViewModel에서 간단히 호출할 수 있다.
 */
@Singleton
class AnalyticsLogger @Inject constructor() {

    private val analytics: FirebaseAnalytics = Firebase.analytics

    /** 도형 생성 이벤트 */
    fun logShapeCreated(shapeType: String) {
        analytics.logEvent("shape_created") {
            param("shape_type", shapeType)
        }
    }

    /** 도형 삭제 이벤트 */
    fun logShapeDeleted() {
        analytics.logEvent("shape_deleted", null)
    }

    /** 도형 복제 이벤트 */
    fun logShapeDuplicated() {
        analytics.logEvent("shape_duplicated", null)
    }

    /** 스케치 모드 진입 이벤트 */
    fun logSketchModeEntered() {
        analytics.logEvent("sketch_mode_entered", null)
    }

    /** 스케치 저장 이벤트 */
    fun logSketchSaved() {
        analytics.logEvent("sketch_saved", null)
    }

    /** 드론 생성 이벤트 */
    fun logDroneCreated() {
        analytics.logEvent("drone_created", null)
    }

    /** 드론 삭제 이벤트 */
    fun logDroneDeleted() {
        analytics.logEvent("drone_deleted", null)
    }

    /** 비행구역 레이어 토글 이벤트 */
    fun logFlightZoneLayerToggled(layerName: String) {
        analytics.logEvent("flight_zone_layer_toggled") {
            param("layer_name", layerName)
        }
    }

    /** 날씨 화면 조회 이벤트 */
    fun logWeatherViewed() {
        analytics.logEvent("weather_viewed", null)
    }

    /** Kp 지수 화면 조회 이벤트 */
    fun logKpViewed() {
        analytics.logEvent("kp_viewed", null)
    }

    /** 주소 검색 이벤트 */
    fun logSearchAddress(query: String) {
        analytics.logEvent("search_address") {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query)
        }
    }

    /** 외부 지도 앱 열기 이벤트 */
    fun logExternalMapOpened(appName: String) {
        analytics.logEvent("external_map_opened") {
            param("app_name", appName)
        }
    }

    /** 로그인 이벤트 */
    fun logLogin(method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    /** 로그아웃 이벤트 */
    fun logLogout() {
        analytics.logEvent("logout", null)
    }
}
