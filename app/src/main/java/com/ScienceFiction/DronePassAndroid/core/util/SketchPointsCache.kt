package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 스케치 스무딩 결과를 LRU 캐시하는 싱글턴.
 *
 * 동일한 스케치에 대한 반복적인 스무딩 계산을 방지하여 성능을 향상시킨다.
 * Thread-safe 하며, 캐시 키는 "sketchId:updatedAt" 형식을 사용한다.
 */
object SketchPointsCache {

    /** 최대 캐시 항목 수 */
    private const val MAX_CACHE_SIZE = 100

    /** LRU 캐시 (LinkedHashMap의 accessOrder=true 활용) */
    private val cache = object : LinkedHashMap<String, List<Coordinate>>(
        MAX_CACHE_SIZE + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Coordinate>>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    /** Thread-safety를 위한 Mutex */
    private val mutex = Mutex()

    /**
     * 스케치의 스무딩된 포인트를 반환한다.
     *
     * 캐시에 존재하면 캐시된 결과를 반환하고,
     * 미스 시 [SketchSmoothingAlgorithm.smoothUsingCatmullRom]을 호출하여 계산 후 캐시에 저장한다.
     *
     * @param sketch 스무딩할 스케치 모델
     * @return 스무딩된 좌표 리스트
     */
    suspend fun getSmoothedPoints(sketch: SketchModel): List<Coordinate> {
        val cacheKey = "${sketch.id}:${sketch.updatedAt}"

        mutex.withLock {
            cache[cacheKey]?.let { return it }
        }

        // 캐시 미스: 스무딩 계산 (lock 밖에서 수행하여 병렬성 확보)
        val smoothed = SketchSmoothingAlgorithm.smoothUsingCatmullRom(sketch.points)

        mutex.withLock {
            cache[cacheKey] = smoothed
        }

        return smoothed
    }

    /**
     * 캐시를 전체 비운다.
     */
    suspend fun clearCache() {
        mutex.withLock {
            cache.clear()
        }
    }

    /**
     * 특정 스케치의 캐시를 무효화한다.
     *
     * @param sketchId 무효화할 스케치의 ID
     */
    suspend fun invalidate(sketchId: String) {
        mutex.withLock {
            val keysToRemove = cache.keys.filter { it.startsWith("$sketchId:") }
            keysToRemove.forEach { cache.remove(it) }
        }
    }

    /**
     * 현재 캐시 크기를 반환한다 (테스트/디버그용).
     */
    suspend fun cacheSize(): Int {
        mutex.withLock {
            return cache.size
        }
    }
}
