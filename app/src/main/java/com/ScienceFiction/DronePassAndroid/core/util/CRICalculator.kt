package com.ScienceFiction.DronePassAndroid.core.util

import kotlin.math.exp

/**
 * CRI (Condensation Risk Index) 결로위험지수 계산기
 *
 * 채널1: CRI_DeltaT = 100 - 10 * (온도 - 이슬점), 범위 1-100
 * 채널2: CRI_RH = 상대습도 (Magnus 공식)
 * 합성: max(CRI_DeltaT, CRI_RH)
 * 풍속보정: >= 5m/s -> x0.8, >= 2m/s -> x0.9, 하한=CRI_RH
 * 최종: clamp(1, 100)
 */
object CRICalculator {

    /**
     * CRI 계산
     *
     * @param temperature 온도 (Celsius)
     * @param dewPoint 이슬점 (Celsius)
     * @param windSpeed 풍속 (m/s)
     * @return CRI 값 (1-100)
     */
    fun calculate(temperature: Double, dewPoint: Double, windSpeed: Double): Double {
        // 채널1: 온도-이슬점 차이 기반
        val deltaT = temperature - dewPoint
        val criDeltaT = (100.0 - 10.0 * deltaT).coerceIn(1.0, 100.0)

        // 채널2: 상대습도 기반 (Magnus 공식)
        val relativeHumidity = calculateRelativeHumidity(temperature, dewPoint)
        val criRH = relativeHumidity.coerceIn(1.0, 100.0)

        // 합성: 둘 중 큰 값
        val criCombined = maxOf(criDeltaT, criRH)

        // 풍속 보정
        val windFactor = when {
            windSpeed >= 5.0 -> 0.8
            windSpeed >= 2.0 -> 0.9
            else -> 1.0
        }
        val criWindAdjusted = criCombined * windFactor

        // 하한: CRI_RH 이하로 내려가지 않음
        val criFinal = maxOf(criWindAdjusted, criRH)

        // 최종: 1-100 범위로 클램핑
        return criFinal.coerceIn(1.0, 100.0)
    }

    /**
     * Magnus 공식을 사용한 상대습도 계산
     *
     * RH = 100 * exp(17.625 * dewPoint / (243.04 + dewPoint))
     *     / exp(17.625 * temperature / (243.04 + temperature))
     */
    private fun calculateRelativeHumidity(temperature: Double, dewPoint: Double): Double {
        val a = 17.625
        val b = 243.04

        val numerator = exp(a * dewPoint / (b + dewPoint))
        val denominator = exp(a * temperature / (b + temperature))

        return if (denominator > 0) {
            100.0 * numerator / denominator
        } else {
            100.0
        }
    }
}
