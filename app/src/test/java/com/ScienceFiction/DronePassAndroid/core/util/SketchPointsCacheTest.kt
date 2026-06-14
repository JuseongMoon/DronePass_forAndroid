package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SketchPointsCacheTest {

    @Test
    fun `포인트가 2개 미만이면 iOS처럼 원본을 반환하고 캐시하지 않는다`() = runBlocking {
        SketchPointsCache.clearCache()
        val point = Coordinate(latitude = 37.0, longitude = 127.0)

        val empty = SketchPointsCache.getSmoothedPoints(sketch(id = "empty", points = emptyList()))
        val single = SketchPointsCache.getSmoothedPoints(sketch(id = "single", points = listOf(point)))

        assertEquals(emptyList<Coordinate>(), empty)
        assertEquals(listOf(point), single)
        assertEquals(0, SketchPointsCache.cacheSize())
    }

    @Test
    fun `포인트가 2개 이상이면 스무딩 결과를 캐시한다`() = runBlocking {
        SketchPointsCache.clearCache()
        val source = sketch(id = "line")

        val smoothed = SketchPointsCache.getSmoothedPoints(source)

        assertEquals(11, smoothed.size)
        assertEquals(1, SketchPointsCache.cacheSize())
    }

    @Test
    fun `updatedAt 이 바뀌면 같은 스케치도 새 캐시 항목으로 계산한다`() = runBlocking {
        SketchPointsCache.clearCache()
        val first = sketch(id = "same", updatedAt = 1L)
        val second = sketch(id = "same", updatedAt = 2L)

        SketchPointsCache.getSmoothedPoints(first)
        SketchPointsCache.getSmoothedPoints(second)

        assertEquals(2, SketchPointsCache.cacheSize())
    }

    @Test
    fun `특정 스케치 invalidate 는 updatedAt 별 캐시를 모두 지운다`() = runBlocking {
        SketchPointsCache.clearCache()
        SketchPointsCache.getSmoothedPoints(sketch(id = "target", updatedAt = 1L))
        SketchPointsCache.getSmoothedPoints(sketch(id = "target", updatedAt = 2L))
        SketchPointsCache.getSmoothedPoints(sketch(id = "other", updatedAt = 1L))

        SketchPointsCache.invalidate("target")

        assertEquals(1, SketchPointsCache.cacheSize())
    }

    @Test
    fun `LRU 캐시는 iOS처럼 최대 100개 항목만 유지한다`() = runBlocking {
        SketchPointsCache.clearCache()

        repeat(101) { index ->
            SketchPointsCache.getSmoothedPoints(sketch(id = "sketch-$index"))
        }

        assertEquals(100, SketchPointsCache.cacheSize())
    }

    private fun sketch(
        id: String,
        updatedAt: Long = 1L,
        points: List<Coordinate> = listOf(
            Coordinate(latitude = 0.0, longitude = 0.0),
            Coordinate(latitude = 10.0, longitude = 10.0),
        ),
    ): SketchModel {
        return SketchModel(
            id = id,
            points = points,
            updatedAt = updatedAt,
        )
    }
}
