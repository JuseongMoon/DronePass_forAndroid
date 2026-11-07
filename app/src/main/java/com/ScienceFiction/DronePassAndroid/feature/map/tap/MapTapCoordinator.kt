package com.ScienceFiction.DronePassAndroid.feature.map.tap

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 탭 판정 후보를 내는 쪽. 새 오버레이 종류는 이 인터페이스를 구현해 [MapTapCoordinator.register]로 붙인다.
 * iOS MapTapSource와 같다.
 */
interface MapTapSource {
    /** true: 형상이 많아 격자 색인에 넣는다([tapSignature]가 바뀔 때만 다시 만든다). false: 탭마다 [tapShapes]를 본다 */
    val tapIndexed: Boolean

    /** 형상 집합이 바뀌었는지 볼 서명([tapIndexed]일 때만 쓴다) */
    val tapSignature: Int

    /** 지금 지도에 보이는 것의 탭 판정 형상 */
    val tapShapes: List<MapTapShape>

    /** 판정 결과가 이 소스의 대상이면 열고 true */
    fun openTapped(target: MapTapTarget): Boolean

    /** 빈 곳 탭: 지도 강조만 푼다(목록 선택·시트는 그대로) */
    fun clearTapHighlight()
}

/**
 * 지도 탭 하나를 [MapTapResolver]로 판정해 등록된 소스(내 도형·비행구역) 중 하나를 연다(11/7 대표).
 * 해당 없음(none)이면 모든 소스의 지도 강조만 푼다. 저장 목록 선택·열린 시트는 건드리지 않는다.
 * 색인 소스의 형상 색인은 서명이 바뀔 때만 다시 만든다. 색인하지 않는 소스(내 원, 수가 적음)는 탭마다 본다.
 */
class MapTapCoordinator {
    /** 판정 1번이 끝날 때(걸린 시간 ms, 결과, 색인을 다시 만들었는지, 형상 수). 개발 빌드 측정용 */
    data class Stats(val ms: Double, val result: MapTapResult, val rebuilt: Boolean, val shapeCount: Int)

    private val sources = mutableListOf<MapTapSource>()
    private var index: MapTapIndex? = null
    private var signature: List<Int>? = null

    var onResolved: ((Stats) -> Unit)? = null

    /** 소스 등록(같은 객체는 한 번만) */
    fun register(source: MapTapSource) {
        sources.removeAll { it === source }
        sources.add(source)
        signature = null
    }

    /** 화면이 사라질 때 소스와 색인을 놓는다 */
    fun clear() {
        sources.clear()
        index = null
        signature = null
        onResolved = null
    }

    /** 판정하고 반영한다 */
    fun handleTap(p: MapTapLatLng, toleranceM: Double) {
        apply(resolve(p, toleranceM))
    }

    /** 판정 결과 반영: 대상이 있으면 그 소스가 열고, 없으면(none) 모든 소스의 지도 강조만 푼다 */
    fun apply(result: MapTapResult) {
        val target = result.target
        if (target == null) {
            sources.toList().forEach { it.clearTapHighlight() }
            return
        }
        for (source in sources.toList()) {
            if (source.openTapped(target)) break
        }
    }

    /** 판정만(색인 갱신 포함) */
    fun resolve(p: MapTapLatLng, toleranceM: Double): MapTapResult {
        val start = System.nanoTime()
        val indexed = sources.filter { it.tapIndexed }

        var rebuilt = false
        val sig = indexed.map { it.tapSignature }
        if (sig != signature) {
            index = MapTapIndex(indexed.flatMap { it.tapShapes })
            signature = sig
            rebuilt = true
        }
        val extra = sources.filter { !it.tapIndexed }.flatMap { it.tapShapes }
        val result = MapTapResolver.resolve(p, toleranceM, index, extra)

        onResolved?.invoke(
            Stats(
                ms = (System.nanoTime() - start) / 1e6,
                result = result,
                rebuilt = rebuilt,
                shapeCount = (index?.shapes?.size ?: 0) + extra.size,
            ),
        )
        return result
    }

    companion object {
        /**
         * 화면 허용 거리(12pt)를 그 자리·줌의 m로. [offset]은 탭 지점에서 화면 오른쪽으로 12dp 옮긴 곳의 좌표다.
         * iOS MapTapCoordinator.toleranceM과 같은 식.
         */
        fun toleranceMeters(tap: MapTapLatLng, offset: MapTapLatLng): Double {
            val dLat = (offset.lat - tap.lat) * MapTapShape.METERS_PER_DEGREE
            val dLon = (offset.lon - tap.lon) * MapTapShape.METERS_PER_DEGREE * cos(tap.lat * PI / 180)
            return sqrt(dLat * dLat + dLon * dLon)
        }
    }
}
