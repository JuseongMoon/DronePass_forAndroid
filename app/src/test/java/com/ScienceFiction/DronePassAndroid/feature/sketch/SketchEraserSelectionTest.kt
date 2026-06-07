package com.ScienceFiction.DronePassAndroid.feature.sketch

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SketchEraserSelectionTest {

    @Test
    fun `지우개는 iOS처럼 닿은 후보 중 가장 가까운 스케치를 선택한다`() {
        val erasePoint = Coordinate(latitude = 37.0, longitude = 127.0)
        val fartherSketch = horizontalSketch(
            id = "farther",
            latitudeMetersFromPoint = 20.0,
        )
        val closerSketch = horizontalSketch(
            id = "closer",
            latitudeMetersFromPoint = 5.0,
        )

        val selected = findClosestErasableSketch(
            point = erasePoint,
            sketches = listOf(fartherSketch, closerSketch),
        )

        assertEquals("closer", selected?.id)
    }

    @Test
    fun `지우개는 threshold 밖의 스케치를 선택하지 않는다`() {
        val selected = findClosestErasableSketch(
            point = Coordinate(latitude = 37.0, longitude = 127.0),
            sketches = listOf(horizontalSketch(id = "outside", latitudeMetersFromPoint = 40.0)),
        )

        assertNull(selected)
    }

    @Test
    fun `지우개는 iOS처럼 단일 포인트 스케치도 거리로 판정한다`() {
        val selected = findClosestErasableSketch(
            point = Coordinate(latitude = 37.0, longitude = 127.0),
            sketches = listOf(
                SketchModel(
                    id = "single-point",
                    points = listOf(Coordinate(latitude = latitudeOffsetFromBase(5.0), longitude = 127.0)),
                ),
            ),
        )

        assertEquals("single-point", selected?.id)
    }

    private fun horizontalSketch(
        id: String,
        latitudeMetersFromPoint: Double,
    ): SketchModel {
        val latitude = latitudeOffsetFromBase(latitudeMetersFromPoint)
        return SketchModel(
            id = id,
            points = listOf(
                Coordinate(latitude = latitude, longitude = 126.9999),
                Coordinate(latitude = latitude, longitude = 127.0001),
            ),
        )
    }

    private fun latitudeOffsetFromBase(meters: Double): Double {
        return 37.0 + meters / METERS_PER_LATITUDE_DEGREE
    }

    private companion object {
        const val METERS_PER_LATITUDE_DEGREE = 111_320.0
    }
}
