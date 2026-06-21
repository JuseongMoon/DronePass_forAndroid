package com.ScienceFiction.DronePassAndroid.core.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `드론 analytics 이벤트는 성공한 생성 삭제 경로에서 수집한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneViewModel.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/drone/DroneViewModel.kt",
        ).readText()

        assertTrue(source.contains("private val analyticsLogger: AnalyticsLogger"))
        assertTrue(source.contains("analyticsLogger.logDroneCreated()"))
        assertTrue(source.contains("analyticsLogger.logDroneDeleted()"))
    }

    @Test
    fun `비행구역 analytics layer_name 은 현지화 문구가 아닌 VWorld typeName 을 수집한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapViewModel.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapViewModel.kt",
        ).readText()

        assertTrue(source.contains("analyticsLogger.logFlightZoneLayerToggled(layer.typeName)"))
    }

    @Test
    fun `저장 목록 analytics 이벤트는 상세 복제 삭제 경로에서도 수집한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListViewModel.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListViewModel.kt",
        ).readText()

        assertTrue(source.contains("private val analyticsLogger: AnalyticsLogger"))
        assertTrue(source.contains("analyticsLogger.logShapeDuplicated()"))
        assertTrue(source.contains("analyticsLogger.logShapeDeleted()"))
    }

    @Test
    fun `외부 지도 analytics 이벤트는 지도와 저장 목록 상세 경로에서 provider id 로 수집한다`() {
        val shapeDetailSource = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeDetailSheet.kt",
        ).readText()
        val mapSource = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreenLayers.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreenLayers.kt",
        ).readText()
        val savedSource = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListScreen.kt",
        ).readText()

        assertTrue(shapeDetailSource.contains("onExternalMapOpened(target.provider.analyticsName)"))
        assertTrue(shapeDetailSource.contains("if (openExternalMap(context = context, target = target))"))
        assertTrue(mapSource.contains("onExternalMapOpened = viewModel::logExternalMapOpened"))
        assertTrue(savedSource.contains("onExternalMapOpened = viewModel::logExternalMapOpened"))
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val roots = listOf(File("."), File("app"))
        for (root in roots) {
            for (candidate in candidates) {
                val file = File(root, candidate)
                if (file.exists()) return file
            }
        }
        error("Could not resolve any of: ${candidates.joinToString()}")
    }
}
