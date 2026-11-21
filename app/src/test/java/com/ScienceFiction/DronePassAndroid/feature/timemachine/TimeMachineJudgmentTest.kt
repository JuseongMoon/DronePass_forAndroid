package com.ScienceFiction.DronePassAndroid.feature.timemachine

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.feature.map.filterShapesByMapVisibilitySettings
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.MapOverlaySystemGrayHex
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.calculateMapOverlayFillColor
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.calculateMapOverlayOutlineColor
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.calculateMapOverlayOutlineWidth
import com.ScienceFiction.DronePassAndroid.feature.map.overlay.parseMapOverlayColorSafe
import com.ScienceFiction.DronePassAndroid.feature.saved.SavedShapeVisibilitySettings
import com.ScienceFiction.DronePassAndroid.feature.saved.SortDirection
import com.ScienceFiction.DronePassAndroid.feature.saved.SortOption
import com.ScienceFiction.DronePassAndroid.feature.saved.buildSavedShapeSections
import com.ScienceFiction.DronePassAndroid.feature.saved.isSavedShapeListItemExpired
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * D-85: 타임머신이 미래 시각 t 를 가리키면 저장 목록 분류, 지도 만료 회색, 만료 숨김을 t 로 판정한다.
 * 'now'(선택 없음)는 지금과 똑같다.
 */
class TimeMachineJudgmentTest {

    private fun kst(y: Int, m: Int, d: Int, h: Int = 0, mi: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, mi).atZone(TimeMachineZone).toInstant().toEpochMilli()

    private val now = kst(2026, 11, 21, 14, 23)
    /** D+2 10:00 */
    private val t = TimeMachineSelection(TimeMachineSlot.dayStart(kst(2026, 11, 23)), 60)

    /** 지금 진행 중이고 내일 끝난다 → t 에서는 만료 */
    private val endsTomorrow = ShapeModel(id = "a", color = "#FF0000", flightStartDate = kst(2026, 11, 20, 9), flightEndDate = kst(2026, 11, 22, 18))
    /** 지금은 시작 전, t 에서는 진행 중 */
    private val startsTomorrow = ShapeModel(id = "b", color = "#00FF00", flightStartDate = kst(2026, 11, 22, 9), flightEndDate = kst(2026, 11, 25, 18))
    /** 지금도 t 에서도 시작 전 */
    private val startsLater = ShapeModel(id = "c", color = "#0000FF", flightStartDate = kst(2026, 11, 30, 9), flightEndDate = kst(2026, 11, 30, 18))
    private val shapes = listOf(endsTomorrow, startsTomorrow, startsLater)

    @Test
    fun `now 는 지금과 같다`() {
        assertEquals(now, timeMachineJudgmentTime(null, now))
        assertEquals(t.timeMillis, timeMachineJudgmentTime(t, now))
    }

    @Test
    fun `저장 목록 분류는 t 로 나눈다`() {
        fun sections(at: Long, hideExpired: Boolean = false) = buildSavedShapeSections(
            shapes = shapes,
            activeDrones = listOf(DroneModel(id = "d1")),
            selectedDroneIds = setOf("d1"),
            sortOption = SortOption.TITLE,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(hideExpired = hideExpired),
            now = at,
        )
        val current = sections(now)
        assertEquals(listOf("a"), current.activeFiltered.map { it.id })
        assertEquals(setOf("b", "c"), current.notStarted.map { it.id }.toSet())
        assertTrue(current.expired.isEmpty())

        val future = sections(timeMachineJudgmentTime(t, now))
        assertEquals(listOf("b"), future.activeFiltered.map { it.id })
        assertEquals(listOf("c"), future.notStarted.map { it.id })
        assertEquals(listOf("a"), future.expired.map { it.id })
        // 만료 숨김도 t 로
        assertTrue(sections(t.timeMillis, hideExpired = true).expired.isEmpty())
        assertEquals(1, sections(now, hideExpired = true).activeFiltered.size)
        // 행의 회색 표시도 같은 시각
        assertFalse(isSavedShapeListItemExpired(endsTomorrow.flightEndDate, now))
        assertTrue(isSavedShapeListItemExpired(endsTomorrow.flightEndDate, t.timeMillis))
    }

    @Test
    fun `지도 만료 숨김은 t 로 거른다`() {
        val current = filterShapesByMapVisibilitySettings(shapes, hideExpired = true, hideNotStarted = false, now = now)
        assertEquals(listOf("a", "b", "c"), current.map { it.id })
        val future = filterShapesByMapVisibilitySettings(shapes, hideExpired = true, hideNotStarted = false, now = t.timeMillis)
        assertEquals(listOf("b", "c"), future.map { it.id })
    }

    @Test
    fun `지도 만료 회색과 시작 전 모양은 t 로 정한다`() {
        val gray = parseMapOverlayColorSafe(MapOverlaySystemGrayHex)
        val red = parseMapOverlayColorSafe(endsTomorrow.color)
        // 지금: 진행 중 → 원래 색, t: 만료 → 회색
        assertEquals(red, calculateMapOverlayOutlineColor(endsTomorrow, emptySet(), now))
        assertEquals(gray, calculateMapOverlayOutlineColor(endsTomorrow, emptySet(), t.timeMillis))
        assertEquals(gray and 0x00FFFFFF, calculateMapOverlayFillColor(endsTomorrow, emptySet(), t.timeMillis) and 0x00FFFFFF)
        // 지금 시작 전(얇은 선) → t 에서는 진행 중(보통 선)
        assertEquals(1, calculateMapOverlayOutlineWidth(startsTomorrow, now))
        assertEquals(2, calculateMapOverlayOutlineWidth(startsTomorrow, t.timeMillis))
    }
}
