package com.ScienceFiction.DronePassAndroid.core.util

import kotlin.math.roundToInt

/** 결로위험지수: 기온과 이슬점의 차를 곡선에 대입하고 저시정 안개를 보정한다. */
object CRICalculator {
    internal const val MIN_STABILIZED_TEMPERATURE_C = -80.0
    internal const val MAX_STABILIZED_TEMPERATURE_C = 60.0
    internal const val FOG_VISIBILITY_KM_BELOW = 1.0
    internal const val FOG_MAX_SPREAD_C = 3.0
    internal const val FOG_MINIMUM_CRI = 90.0
    internal const val MIN_CRI = 1.0
    internal const val MAX_CRI = 100.0
    internal val CURVE = listOf(
        0.0 to 100.0,
        2.0 to 70.0,
        3.0 to 40.0,
        5.0 to 20.0,
        10.0 to 1.0,
    )

    /** 유한하지 않은 기온이나 이슬점은 NaN, 정상 입력은 반올림한 1..100 값을 반환한다. */
    fun calculate(temperature: Double, dewPoint: Double, visibilityKm: Double?): Double {
        val cri = calculateUnrounded(temperature, dewPoint, visibilityKm)
        return if (cri.isFinite()) cri.roundToInt().toDouble() else cri
    }

    internal fun calculateUnrounded(temperature: Double, dewPoint: Double, visibilityKm: Double?): Double {
        if (!temperature.isFinite() || !dewPoint.isFinite()) return Double.NaN
        val spread = (
            temperature.coerceIn(MIN_STABILIZED_TEMPERATURE_C, MAX_STABILIZED_TEMPERATURE_C) -
                dewPoint.coerceIn(MIN_STABILIZED_TEMPERATURE_C, MAX_STABILIZED_TEMPERATURE_C)
            ).coerceAtLeast(0.0)
        var cri = interpolate(spread)
        if (visibilityKm != null && visibilityKm < FOG_VISIBILITY_KM_BELOW && spread <= FOG_MAX_SPREAD_C) {
            cri = maxOf(cri, FOG_MINIMUM_CRI)
        }
        return cri.coerceIn(MIN_CRI, MAX_CRI)
    }

    private fun interpolate(spread: Double): Double {
        if (spread <= CURVE.first().first) return CURVE.first().second
        for (index in 1 until CURVE.size) {
            val (x1, y1) = CURVE[index]
            if (spread <= x1) {
                val (x0, y0) = CURVE[index - 1]
                return y0 + (spread - x0) * (y1 - y0) / (x1 - x0)
            }
        }
        return CURVE.last().second
    }
}

/** 평균 창(5샘플 × 3분). 직전 샘플과 이 간격을 넘겨 벌어지면 평균을 새로 시작한다. */
internal const val CRI_SMOOTHER_RESET_GAP_MS = 15L * 60L * 1000L

/**
 * 현재 CRI 이동 평균. iOS `CRISmoother` 와 같은 규칙이다.
 * - 최근 [maxSamples] 개 평균을 반올림한다.
 * - 새 샘플이 직전 샘플보다 [resetGapMs] 를 **초과**해서 늦으면(백그라운드·잠금 뒤 복귀) 버퍼를 비우고 새로 쌓는다.
 *   정확히 [resetGapMs] 이면 유지한다. 기기 시계가 거꾸로 가면 간격을 0으로 보고 유지한다.
 * - 샘플 시각은 샘플을 넣는 순간의 기기 시각이다(서버 fetchedAt 아님).
 */
internal class CurrentCriSmoother(
    private val maxSamples: Int = 5,
    private val resetGapMs: Long = CRI_SMOOTHER_RESET_GAP_MS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val samples = ArrayDeque<Double>()
    private var lastSampleAtMillis: Long? = null

    fun smooth(nextCri: Double): Double = smooth(nextCri, nowMillis())

    fun smooth(nextCri: Double, atMillis: Long): Double {
        if (!nextCri.isFinite()) return nextCri

        val previous = lastSampleAtMillis
        if (previous != null && (atMillis - previous).coerceAtLeast(0L) > resetGapMs) {
            samples.clear()
        }
        lastSampleAtMillis = atMillis

        samples.addLast(nextCri.coerceIn(1.0, 100.0))
        while (samples.size > maxSamples) {
            samples.removeFirst()
        }

        return (samples.sum() / samples.size).roundToInt().toDouble()
    }

    /** 현재 버퍼 크기. 테스트에서 초기화 여부를 확인하는 데 쓴다. */
    internal val sampleCount: Int get() = samples.size

    fun reset() {
        samples.clear()
        lastSampleAtMillis = null
    }
}
