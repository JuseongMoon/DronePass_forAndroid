package com.ScienceFiction.DronePassAndroid.feature.map.tap

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * 지도 탭 판정(11/7 대표). 지도 위 모든 오버레이(내 도형·비행구역)가 같은 기준이고 종류별 우선순위는 없다.
 * 공유 계약 "지도 탭 판정"과 fixture map-tap-cases.json으로 iOS MapTapResolver와 같은 답을 낸다.
 *  ① 선 우선: 탭 지점에서 테두리까지 거리가 허용값(화면 12pt를 그 줌의 m로) 이하인 것 중 가장 가까운 선.
 *     거리가 0.5m 안에서 같으면 면적이 작은 것.
 *  ② 안쪽: 선 근처가 아니면 탭 지점을 포함하는 것 중 면적이 가장 작은 것. 면적도 1m² 안에서 같으면 위에 그려진 것.
 *  ③ 어느 것도 아니면(none) 지도 강조만 푼다(열린 목록·시트는 그대로, MapTapCoordinator).
 * 보이는 순서(zIndex)는 바꾸지 않고 터치 판정에만 쓴다. 오버레이에는 클릭 리스너를 두지 않는다.
 *
 * 연산: 후보마다 형상(경계 상자·면적·국지 평면 좌표)을 미리 만들고 0.05° 격자 색인으로 이웃 칸만 본다.
 * 탭 1번에 화면 투영은 허용값 환산 1번, 거리 계산은 m 평면, 원은 식 하나.
 */

/** 탭으로 열 대상. 새 오버레이 종류는 하위 타입만 더한다 */
sealed interface MapTapTarget {
    /** VWorld 비행구역(오버레이 하나를 가리키는 키) */
    data class Zone(val key: Any) : MapTapTarget

    /** 내 도형 */
    data class Shape(val id: String) : MapTapTarget
}

data class MapTapLatLng(val lat: Double, val lon: Double)

/** 국지 평면 좌표(m) */
data class MapTapPoint(val x: Double, val y: Double)

/** 미리 만든 탭 판정 형상 */
class MapTapShape private constructor(
    val target: MapTapTarget,
    /** 위에 그려진 정도(클수록 위). 면적까지 같을 때만 쓴다 */
    val drawOrder: Double,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val areaM2: Double,
    val origin: MapTapLatLng,
    /** 원이면 반지름, 다각형이면 null */
    private val radiusM: Double?,
    /** 다각형 고리들(바깥 + 구멍, 짝홀 규칙). 원이면 비어 있다 */
    private val rings: List<List<MapTapPoint>>,
) {
    /** 탭 지점까지 테두리 거리(m)와 안쪽 여부 */
    data class Measure(val lineM: Double, val inside: Boolean)

    fun measure(p: MapTapLatLng): Measure {
        val q = project(p, origin)
        if (radiusM != null) {
            val d = sqrt(q.x * q.x + q.y * q.y)
            return Measure(abs(d - radiusM), d <= radiusM)
        }
        var best = Double.POSITIVE_INFINITY
        var inside = false
        for (ring in rings) {
            var j = ring.size - 1
            for (i in ring.indices) {
                val a = ring[j]
                val b = ring[i]
                if ((b.y > q.y) != (a.y > q.y) &&
                    q.x < (a.x - b.x) * (q.y - b.y) / (a.y - b.y) + b.x
                ) {
                    inside = !inside
                }
                best = min(best, segmentDistance(q, a, b))
                j = i
            }
        }
        return Measure(best, inside)
    }

    companion object {
        const val EARTH_RADIUS_M = 6_371_008.8
        const val METERS_PER_DEGREE = EARTH_RADIUS_M * PI / 180

        /** 원: 중심(국지 평면 원점)과 반지름 */
        fun circle(target: MapTapTarget, drawOrder: Double, center: MapTapLatLng, radiusM: Double): MapTapShape {
            val dLat = radiusM / METERS_PER_DEGREE
            val dLon = dLat / max(cos(center.lat * PI / 180), 0.01)
            return MapTapShape(
                target = target,
                drawOrder = drawOrder,
                minLat = center.lat - dLat,
                minLon = center.lon - dLon,
                maxLat = center.lat + dLat,
                maxLon = center.lon + dLon,
                areaM2 = PI * radiusM * radiusM,
                origin = center,
                radiusM = radiusM,
                rings = emptyList(),
            )
        }

        /** 첫 고리 = 바깥, 나머지 = 구멍. 점이 3개 미만인 고리는 버린다. 쓸 고리가 없으면 null */
        fun polygon(target: MapTapTarget, drawOrder: Double, rings: List<List<MapTapLatLng>>): MapTapShape? {
            val usable = rings.filter { it.size >= 3 }
            val outer = usable.firstOrNull() ?: return null
            val minLat = outer.minOf { it.lat }
            val maxLat = outer.maxOf { it.lat }
            val minLon = outer.minOf { it.lon }
            val maxLon = outer.maxOf { it.lon }
            val origin = MapTapLatLng((minLat + maxLat) / 2, (minLon + maxLon) / 2)
            val projected = usable.map { ring -> ring.map { project(it, origin) } }
            val signed = projected.map(::shoelace)
            val area = max(0.0, abs(signed[0]) - signed.drop(1).sumOf { abs(it) })
            return MapTapShape(
                target = target,
                drawOrder = drawOrder,
                minLat = minLat,
                minLon = minLon,
                maxLat = maxLat,
                maxLon = maxLon,
                areaM2 = area,
                origin = origin,
                radiusM = null,
                rings = projected,
            )
        }

        fun project(p: MapTapLatLng, origin: MapTapLatLng): MapTapPoint = MapTapPoint(
            (p.lon - origin.lon) * METERS_PER_DEGREE * cos(origin.lat * PI / 180),
            (p.lat - origin.lat) * METERS_PER_DEGREE,
        )

        private fun shoelace(ring: List<MapTapPoint>): Double {
            var sum = 0.0
            for (i in ring.indices) {
                val a = ring[i]
                val b = ring[(i + 1) % ring.size]
                sum += a.x * b.y - b.x * a.y
            }
            return sum / 2
        }

        fun segmentDistance(p: MapTapPoint, a: MapTapPoint, b: MapTapPoint): Double {
            val abX = b.x - a.x
            val abY = b.y - a.y
            val len2 = abX * abX + abY * abY
            var t = if (len2 > 0) ((p.x - a.x) * abX + (p.y - a.y) * abY) / len2 else 0.0
            t = min(1.0, max(0.0, t))
            val dx = p.x - (a.x + abX * t)
            val dy = p.y - (a.y + abY * t)
            return sqrt(dx * dx + dy * dy)
        }
    }
}

/** 0.05° 격자 색인. 형상 집합이 바뀔 때만 만든다 */
class MapTapIndex(val shapes: List<MapTapShape>) {
    private val cells = HashMap<Long, MutableList<Int>>()
    private val wide = mutableListOf<Int>()

    init {
        shapes.forEachIndexed { i, s ->
            val r0 = cell(s.minLat)
            val r1 = cell(s.maxLat)
            val c0 = cell(s.minLon)
            val c1 = cell(s.maxLon)
            if ((r1 - r0 + 1) * (c1 - c0 + 1) > MAX_CELLS_PER_SHAPE) {
                wide.add(i)
            } else {
                for (r in r0..r1) {
                    for (c in c0..c1) cells.getOrPut(key(r, c)) { mutableListOf() }.add(i)
                }
            }
        }
    }

    /** 탭 지점 ± 여유(도)가 걸치는 칸의 후보(중복 없음) */
    fun candidates(near: MapTapLatLng, padLat: Double, padLon: Double): List<Int> {
        val seen = HashSet<Int>(wide)
        val out = ArrayList<Int>(wide)
        for (r in cell(near.lat - padLat)..cell(near.lat + padLat)) {
            for (c in cell(near.lon - padLon)..cell(near.lon + padLon)) {
                cells[key(r, c)]?.forEach { if (seen.add(it)) out.add(it) }
            }
        }
        return out
    }

    companion object {
        const val CELL_DEG = 0.05

        /** 한 형상이 이보다 많은 칸을 덮으면 격자에 넣지 않고 늘 본다(넓은 경계구역 등) */
        const val MAX_CELLS_PER_SHAPE = 4096L

        fun cell(deg: Double): Long = floor(deg / CELL_DEG).toLong()
        fun key(r: Long, c: Long): Long = (r shl 32) xor (c and 0xFFFF_FFFFL)
    }
}

/** 판정 결과와 이유(fixture의 reason과 같은 값) */
data class MapTapResult(val target: MapTapTarget?, val reason: Reason) {
    enum class Reason(val raw: String) {
        /** 선 근처(허용값 안)에서 가장 가까운 선 */
        LINE("line"),

        /** 탭 지점을 포함하는 것 중 가장 작은 것 */
        INSIDE("inside"),

        /** 해당 없음: 지도 강조만 푼다 */
        NONE("none"),
    }

    companion object {
        val None = MapTapResult(null, Reason.NONE)
    }
}

object MapTapResolver {
    /** 화면 12pt(Android는 12dp, 그 줌의 m로 환산) */
    const val LINE_TOLERANCE_PT = 12.0

    /** 선 거리 동률로 보는 차이(m) */
    const val TIE_M = 0.5

    /** 면적 동률로 보는 차이(m²) */
    const val AREA_TIE_M2 = 1.0

    /** 판정. 형상은 색인(비행구역 등 많은 것)과 그때그때 만든 목록(내 원) 둘 다 받는다 */
    fun resolve(
        p: MapTapLatLng,
        toleranceM: Double,
        index: MapTapIndex?,
        extra: List<MapTapShape> = emptyList(),
    ): MapTapResult {
        val padLat = toleranceM / MapTapShape.METERS_PER_DEGREE
        val padLon = padLat / max(cos(p.lat * PI / 180), 0.01)
        val pool = ArrayList<MapTapShape>(extra)
        if (index != null) {
            index.candidates(p, padLat, padLon).mapTo(pool) { index.shapes[it] }
        }

        var line: MapTapShape? = null
        var lineD = 0.0
        var inside: MapTapShape? = null
        for (s in pool) {
            if (p.lat < s.minLat - padLat || p.lat > s.maxLat + padLat ||
                p.lon < s.minLon - padLon || p.lon > s.maxLon + padLon
            ) {
                continue
            }
            val m = s.measure(p)
            if (m.lineM <= toleranceM) {
                val cur = line
                if (cur == null ||
                    m.lineM < lineD - TIE_M ||
                    (abs(m.lineM - lineD) <= TIE_M && smaller(s, cur))
                ) {
                    line = s
                    lineD = m.lineM
                }
            }
            if (m.inside && (inside == null || smaller(s, inside))) {
                inside = s
            }
        }
        line?.let { return MapTapResult(it.target, MapTapResult.Reason.LINE) }
        inside?.let { return MapTapResult(it.target, MapTapResult.Reason.INSIDE) }
        return MapTapResult.None
    }

    /** 면적이 작은 쪽, 같으면(1m² 안) 위에 그려진 쪽 */
    fun smaller(a: MapTapShape, than: MapTapShape): Boolean {
        if (abs(a.areaM2 - than.areaM2) > AREA_TIE_M2) return a.areaM2 < than.areaM2
        return a.drawOrder > than.drawOrder
    }

    /**
     * 오버레이의 보이는 순서를 판정용 값으로. 위에 그려진 것이 크다(iOS와 같은 식).
     * zIndex는 |zIndex| < 500일 때만 globalZIndex 층을 넘지 않는다. 앱의 zIndex는 0~100이다.
     */
    fun drawOrder(globalZIndex: Int, zIndex: Int): Double = globalZIndex + zIndex / 1000.0
}
