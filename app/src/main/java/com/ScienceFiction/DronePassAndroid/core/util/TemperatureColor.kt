package com.ScienceFiction.DronePassAndroid.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * iOS `WeatherManager.temperatureColor` 와 동등한 6단계 그라데이션.
 *
 * 임계값(°C): -10 / 7.5 / 17.5 / 27.5 / 40.
 * 색상: ColdBlue → Cyan → Lime → Orange → HotRed.
 * 구간 사이는 channel-wise linear interpolation.
 */
private val ColdBlue = Color(0xFF0072FF)
private val Cyan = Color(0xFF00C8FF)
private val Lime = Color(0xFFA6E22E)
private val OrangeWarm = Color(0xFFFFA500)
private val HotRed = Color(0xFFFF3B30)

fun interpolateTemperatureColor(temperature: Double?): Color {
    if (temperature == null) return Color.Gray
    return when {
        temperature < -10.0 -> ColdBlue
        temperature < 7.5 -> lerp(ColdBlue, Cyan, ((temperature + 10.0) / 17.5).toFloat())
        temperature < 17.5 -> lerp(Cyan, Lime, ((temperature - 7.5) / 10.0).toFloat())
        temperature < 27.5 -> lerp(Lime, OrangeWarm, ((temperature - 17.5) / 10.0).toFloat())
        temperature < 40.0 -> lerp(OrangeWarm, HotRed, ((temperature - 27.5) / 12.5).toFloat())
        else -> HotRed
    }
}
