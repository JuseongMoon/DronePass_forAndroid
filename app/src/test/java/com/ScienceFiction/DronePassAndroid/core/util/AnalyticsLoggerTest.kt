package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsLoggerTest {

    @Test
    fun `스케치 analytics 이벤트 이름은 iOS SketchManager 와 동일하다`() {
        assertEquals("sketch_mode_enter", AnalyticsEventSketchModeEnter)
        assertEquals("sketch_mode_exit", AnalyticsEventSketchModeExit)
        assertEquals("sketch_created", AnalyticsEventSketchCreated)
        assertEquals("sketch_deleted", AnalyticsEventSketchDeleted)
        assertEquals("sketch_all_cleared", AnalyticsEventSketchAllCleared)
    }

    @Test
    fun `Android analytics 이벤트 이름은 GA4 보고서에서 안정적으로 유지한다`() {
        assertEquals("shape_created", AnalyticsEventShapeCreated)
        assertEquals("shape_deleted", AnalyticsEventShapeDeleted)
        assertEquals("shape_duplicated", AnalyticsEventShapeDuplicated)
        assertEquals("drone_created", AnalyticsEventDroneCreated)
        assertEquals("drone_deleted", AnalyticsEventDroneDeleted)
        assertEquals("flight_zone_layer_toggled", AnalyticsEventFlightZoneLayerToggled)
        assertEquals("weather_viewed", AnalyticsEventWeatherViewed)
        assertEquals("kp_viewed", AnalyticsEventKpViewed)
        assertEquals("search_address", AnalyticsEventSearchAddress)
        assertEquals("external_map_opened", AnalyticsEventExternalMapOpened)
        assertEquals("logout", AnalyticsEventLogout)
    }

    @Test
    fun `스케치 analytics 파라미터 이름은 iOS SketchManager 와 동일하다`() {
        assertEquals("duration_seconds", AnalyticsParamDurationSeconds)
        assertEquals("total_count", AnalyticsParamTotalCount)
        assertEquals("active_count", AnalyticsParamActiveCount)
        assertEquals("deleted_count", AnalyticsParamDeletedCount)
    }

    @Test
    fun `Android analytics 파라미터 이름은 GA4 보고서에서 안정적으로 유지한다`() {
        assertEquals("shape_type", AnalyticsParamShapeType)
        assertEquals("layer_name", AnalyticsParamLayerName)
        assertEquals("app_name", AnalyticsParamAppName)
    }
}
