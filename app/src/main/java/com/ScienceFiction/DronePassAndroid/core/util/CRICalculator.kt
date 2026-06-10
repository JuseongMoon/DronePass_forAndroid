package com.ScienceFiction.DronePassAndroid.core.util

import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * CRI (Condensation Risk Index) 결로위험지수 계산기
 *
 * 채널1: CRI_DeltaT = 100 - 10 * (온도 - 이슬점), 범위 1-100
 * 채널2: CRI_RH = 상대습도 (Magnus 공식)
 * 합성: max(CRI_DeltaT, CRI_RH)
 * 풍속보정: >= 5m/s -> x0.8, >= 2m/s -> x0.9, 하한=CRI_RH
 * 유한한 온도/이슬점 극값은 iOS WeatherManager처럼 -80..60°C 로 안정화한다.
 * 최종: clamp(1, 100), iOS WeatherManager처럼 정수 반올림
 */
object CRICalculator {

    /**
     * CRI 계산
     *
     * 입력이 NaN/Infinity 이면 [Double.NaN] 을 반환한다. 유한한 극단값은 iOS와 동일하게
     * 클램프해서 계산을 계속한다.
     *
     * @param temperature 온도 (Celsius)
     * @param dewPoint 이슬점 (Celsius)
     * @param windSpeed 풍속 (m/s)
     * @return iOS WeatherManager처럼 반올림된 CRI 값 (1-100) 또는 비정상 입력 시 NaN
     */
    fun calculate(temperature: Double, dewPoint: Double, windSpeed: Double): Double {
        if (!temperature.isFinite() || !dewPoint.isFinite() || !windSpeed.isFinite()) {
            return Double.NaN
        }
        // 풍속은 음수가 될 수 없음 (Open-Meteo 데이터 오류 방어)
        val safeWindSpeed = windSpeed.coerceAtLeast(0.0)
        val safeTemperature = temperature.coerceIn(MIN_STABILIZED_TEMPERATURE_C, MAX_STABILIZED_TEMPERATURE_C)
        val safeDewPoint = dewPoint.coerceIn(MIN_STABILIZED_TEMPERATURE_C, MAX_STABILIZED_TEMPERATURE_C)

        // 채널1: 온도-이슬점 차이 기반
        val deltaT = (safeTemperature - safeDewPoint).coerceAtLeast(0.0)
        val criDeltaT = (100.0 - 10.0 * deltaT).coerceIn(1.0, 100.0)

        // 채널2: 상대습도 기반 (Magnus 공식).
        val relativeHumidity = calculateRelativeHumidity(safeTemperature, safeDewPoint)
        if (!relativeHumidity.isFinite()) return Double.NaN
        val criRH = relativeHumidity.coerceIn(0.0, 100.0)

        // 합성: 둘 중 큰 값
        val criCombined = maxOf(criDeltaT, criRH)

        // 풍속 보정 (>=5m/s 강풍 -20%, >=2m/s 중풍 -10%, 미풍은 무보정).
        // 미풍(<2m/s) 구간에서 windFactor=1.0 은 의도된 동작으로 결로 위험을 깎지 않는다.
        val windFactor = when {
            safeWindSpeed >= 5.0 -> 0.8
            safeWindSpeed >= 2.0 -> 0.9
            else -> 1.0
        }
        val criWindAdjusted = criCombined * windFactor

        // 하한: CRI_RH 이하로 내려가지 않음
        val criFinal = maxOf(criWindAdjusted, criRH)

        // 최종: 1-100 범위로 클램핑 후 iOS WeatherManager.finalCRI처럼 반올림
        return criFinal.coerceIn(1.0, 100.0).roundToInt().toDouble()
    }

    /**
     * Magnus 공식을 사용한 상대습도 계산
     *
     * RH = 100 * exp(17.625 * dewPoint / (243.04 + dewPoint))
     *     / exp(17.625 * temperature / (243.04 + temperature))
     *
     * 분모가 0 이하가 되는 비물리적 입력(temperature ≤ -243.04°C 등)이나
     * exp 결과가 Infinity가 되는 극단값에 대해 [Double.NaN]을 반환한다.
     */
    private fun calculateRelativeHumidity(temperature: Double, dewPoint: Double): Double {
        val a = 17.625
        val b = 243.04

        // Magnus 공식 분모가 0 이하가 되면 의미 없는 결과 → NaN
        val denomT = b + temperature
        val denomDP = b + dewPoint
        if (denomT <= 0.0 || denomDP <= 0.0) {
            return Double.NaN
        }

        val numerator = exp(a * dewPoint / denomDP)
        val denominator = exp(a * temperature / denomT)

        // 극단값에서 exp가 Infinity가 될 수 있음
        if (!numerator.isFinite() || !denominator.isFinite() || denominator <= 0.0) {
            return Double.NaN
        }
        return 100.0 * numerator / denominator
    }

    private const val MIN_STABILIZED_TEMPERATURE_C = -80.0
    private const val MAX_STABILIZED_TEMPERATURE_C = 60.0
}
