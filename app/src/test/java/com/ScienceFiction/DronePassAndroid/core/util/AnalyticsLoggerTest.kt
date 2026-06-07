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
    fun `스케치 analytics 파라미터 이름은 iOS SketchManager 와 동일하다`() {
        assertEquals("duration_seconds", AnalyticsParamDurationSeconds)
        assertEquals("total_count", AnalyticsParamTotalCount)
        assertEquals("active_count", AnalyticsParamActiveCount)
        assertEquals("deleted_count", AnalyticsParamDeletedCount)
    }
}
