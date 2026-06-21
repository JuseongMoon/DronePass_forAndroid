package com.ScienceFiction.DronePassAndroid.core.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

internal const val AnalyticsEventSketchModeEnter = "sketch_mode_enter"
internal const val AnalyticsEventSketchModeExit = "sketch_mode_exit"
internal const val AnalyticsEventSketchCreated = "sketch_created"
internal const val AnalyticsEventSketchDeleted = "sketch_deleted"
internal const val AnalyticsEventSketchAllCleared = "sketch_all_cleared"
internal const val AnalyticsEventShapeCreated = "shape_created"
internal const val AnalyticsEventShapeDeleted = "shape_deleted"
internal const val AnalyticsEventShapeDuplicated = "shape_duplicated"
internal const val AnalyticsEventDroneCreated = "drone_created"
internal const val AnalyticsEventDroneDeleted = "drone_deleted"
internal const val AnalyticsEventFlightZoneLayerToggled = "flight_zone_layer_toggled"
internal const val AnalyticsEventWeatherViewed = "weather_viewed"
internal const val AnalyticsEventKpViewed = "kp_viewed"
internal const val AnalyticsEventSearchAddress = "search_address"
internal const val AnalyticsEventExternalMapOpened = "external_map_opened"
internal const val AnalyticsEventLogout = "logout"
internal const val AnalyticsParamDurationSeconds = "duration_seconds"
internal const val AnalyticsParamTotalCount = "total_count"
internal const val AnalyticsParamActiveCount = "active_count"
internal const val AnalyticsParamDeletedCount = "deleted_count"
internal const val AnalyticsParamShapeType = "shape_type"
internal const val AnalyticsParamLayerName = "layer_name"
internal const val AnalyticsParamAppName = "app_name"

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
        analytics.logEvent(AnalyticsEventShapeCreated) {
            param(AnalyticsParamShapeType, shapeType)
        }
    }

    /** 도형 삭제 이벤트 */
    fun logShapeDeleted() {
        analytics.logEvent(AnalyticsEventShapeDeleted, null)
    }

    /** 도형 복제 이벤트 */
    fun logShapeDuplicated() {
        analytics.logEvent(AnalyticsEventShapeDuplicated, null)
    }

    /** 스케치 모드 진입 이벤트 */
    fun logSketchModeEntered() {
        analytics.logEvent(AnalyticsEventSketchModeEnter, null)
    }

    /** 스케치 모드 종료 이벤트 */
    fun logSketchModeExited(durationSeconds: Int) {
        analytics.logEvent(AnalyticsEventSketchModeExit) {
            param(AnalyticsParamDurationSeconds, durationSeconds.toLong())
        }
    }

    /** 스케치 저장 이벤트 */
    fun logSketchSaved(totalCount: Int, activeCount: Int) {
        analytics.logEvent(AnalyticsEventSketchCreated) {
            param(AnalyticsParamTotalCount, totalCount.toLong())
            param(AnalyticsParamActiveCount, activeCount.toLong())
        }
    }

    /** 스케치 삭제 이벤트 */
    fun logSketchDeleted(activeCount: Int) {
        analytics.logEvent(AnalyticsEventSketchDeleted) {
            param(AnalyticsParamActiveCount, activeCount.toLong())
        }
    }

    /** 전체 스케치 삭제 이벤트 */
    fun logSketchAllCleared(deletedCount: Int) {
        analytics.logEvent(AnalyticsEventSketchAllCleared) {
            param(AnalyticsParamDeletedCount, deletedCount.toLong())
        }
    }

    /** 드론 생성 이벤트 */
    fun logDroneCreated() {
        analytics.logEvent(AnalyticsEventDroneCreated, null)
    }

    /** 드론 삭제 이벤트 */
    fun logDroneDeleted() {
        analytics.logEvent(AnalyticsEventDroneDeleted, null)
    }

    /** 비행구역 레이어 토글 이벤트 */
    fun logFlightZoneLayerToggled(layerName: String) {
        analytics.logEvent(AnalyticsEventFlightZoneLayerToggled) {
            param(AnalyticsParamLayerName, layerName)
        }
    }

    /** 날씨 화면 조회 이벤트 */
    fun logWeatherViewed() {
        analytics.logEvent(AnalyticsEventWeatherViewed, null)
    }

    /** Kp 지수 화면 조회 이벤트 */
    fun logKpViewed() {
        analytics.logEvent(AnalyticsEventKpViewed, null)
    }

    /** 주소 검색 이벤트 */
    fun logSearchAddress(query: String) {
        analytics.logEvent(AnalyticsEventSearchAddress) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query)
        }
    }

    /** 외부 지도 앱 열기 이벤트 */
    fun logExternalMapOpened(appName: String) {
        analytics.logEvent(AnalyticsEventExternalMapOpened) {
            param(AnalyticsParamAppName, appName)
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
        analytics.logEvent(AnalyticsEventLogout, null)
    }
}
