package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 스케치 스무딩 결과를 LRU 캐시하는 싱글턴.
 *
 * 동일한 스케치에 대한 반복적인 스무딩 계산을 방지하여 성능을 향상시킨다.
 * Thread-safe 하며, 캐시 키는 "sketchId:updatedAt" 형식을 사용한다.
 *
 * 동시 호출 처리: 동일 cacheKey에 대해 여러 코루틴이 동시에 [getSmoothedPoints]를
 * 호출하더라도 무거운 Catmull-Rom 계산은 단 1회만 수행된다. in-flight 작업의
 * [CompletableDeferred]를 공유하여 나머지 호출자는 동일 결과를 await로 받는다.
 */
object SketchPointsCache {

    /** 최대 캐시 항목 수 */
    private const val MAX_CACHE_SIZE = 100

    /**
     * LRU 캐시.
     *
     * LinkedHashMap 의 세 번째 인자 `accessOrder=true` 를 사용하면 get/put 호출 시
     * 해당 엔트리가 tail 로 이동(MRU)되고, [removeEldestEntry] 가 head(LRU)를 자동 축출한다.
     * 초기 용량 `MAX_CACHE_SIZE + 1` 은 add 후 즉시 축출 직전 1회 rehash 를 피하기 위함.
     */
    private val cache = object : LinkedHashMap<String, List<Coordinate>>(
        MAX_CACHE_SIZE + 1, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Coordinate>>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    /**
     * 현재 진행 중인 스무딩 계산 추적 맵.
     * 동일 cacheKey 에 대한 중복 계산을 방지한다.
     */
    private val inFlight = mutableMapOf<String, CompletableDeferred<List<Coordinate>>>()

    /** Thread-safety를 위한 Mutex */
    private val mutex = Mutex()

    /**
     * 캐시 조회의 결과를 표현하는 봉인 클래스 (file-local).
     */
    private sealed class LookupResult {
        /** 이미 캐시에 결과가 있음 */
        data class Cached(val data: List<Coordinate>) : LookupResult()
        /** 다른 코루틴이 동일 키에 대해 이미 계산 중 */
        data class Pending(val deferred: CompletableDeferred<List<Coordinate>>) : LookupResult()
        /** 새 계산 시작 — 결과를 [deferred]에 채워야 함 */
        data class Compute(val deferred: CompletableDeferred<List<Coordinate>>) : LookupResult()
    }

    /**
     * 스케치의 스무딩된 포인트를 반환한다.
     *
     * 캐시에 존재하면 캐시된 결과를 반환하고,
     * 미스 시 [SketchSmoothingAlgorithm.smoothUsingCatmullRom]을 호출하여 계산 후 캐시에 저장한다.
     * 동일 키에 대한 동시 호출은 단 1번만 계산하고 나머지는 그 결과를 공유받는다.
     *
     * @param sketch 스무딩할 스케치 모델
     * @return 스무딩된 좌표 리스트
     */
    suspend fun getSmoothedPoints(sketch: SketchModel): List<Coordinate> {
        val cacheKey = "${sketch.id}:${sketch.updatedAt}"

        val lookup = mutex.withLock {
            cache[cacheKey]?.let { return@withLock LookupResult.Cached(it) }
            inFlight[cacheKey]?.let { return@withLock LookupResult.Pending(it) }
            val deferred = CompletableDeferred<List<Coordinate>>()
            inFlight[cacheKey] = deferred
            LookupResult.Compute(deferred)
        }

        return when (lookup) {
            is LookupResult.Cached -> lookup.data
            is LookupResult.Pending -> lookup.deferred.await()
            is LookupResult.Compute -> {
                try {
                    // 캐시 미스: 스무딩 계산 (lock 밖에서 수행하여 병렬성 확보)
                    val smoothed = SketchSmoothingAlgorithm.smoothUsingCatmullRom(sketch.points)
                    mutex.withLock {
                        cache[cacheKey] = smoothed
                        inFlight.remove(cacheKey)
                    }
                    lookup.deferred.complete(smoothed)
                    smoothed
                } catch (e: Throwable) {
                    mutex.withLock { inFlight.remove(cacheKey) }
                    lookup.deferred.completeExceptionally(e)
                    throw e
                }
            }
        }
    }

    /**
     * 캐시를 전체 비운다. in-flight 작업은 그대로 두어 await 중인 호출자가 결과를 받게 한다.
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
