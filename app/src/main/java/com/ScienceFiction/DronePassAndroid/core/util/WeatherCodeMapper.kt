package com.ScienceFiction.DronePassAndroid.core.util

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.ScienceFiction.DronePassAndroid.R

/**
 * WMO 날씨 코드 -> 설명/아이콘 매핑
 */
object WeatherCodeMapper {

    @StringRes
    fun weatherCodeToDescriptionRes(code: Int): Int = when (code) {
        0 -> R.string.weather_condition_clear
        1 -> R.string.weather_condition_mostly_clear
        2 -> R.string.weather_condition_partly_cloudy
        3 -> R.string.weather_condition_overcast
        4, 5 -> R.string.weather_condition_haze // WMO 04: smoke / 05: haze
        10 -> R.string.weather_condition_mist
        45, 48 -> R.string.weather_condition_fog
        51, 53, 55 -> R.string.weather_condition_drizzle
        56, 57 -> R.string.weather_condition_freezing_drizzle
        61, 63, 65 -> R.string.weather_condition_rain
        66, 67 -> R.string.weather_condition_freezing_rain
        71, 73, 75 -> R.string.weather_condition_snow
        77 -> R.string.weather_condition_snow_grains
        80, 81, 82 -> R.string.weather_condition_showers
        85, 86 -> R.string.weather_condition_snow_showers
        95 -> R.string.weather_condition_thunderstorm
        96, 99 -> R.string.weather_condition_thunderstorm_hail
        else -> R.string.weather_unknown
    }

    fun weatherCodeToIcon(code: Int): ImageVector = when (code) {
        0 -> Icons.Default.WbSunny
        1, 2 -> Icons.Default.WbCloudy
        3 -> Icons.Default.Cloud
        4, 5, 10 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.Cloud
        51, 53, 55, 56, 57 -> Icons.Default.Grain
        61, 63, 65, 66, 67 -> Icons.Default.Umbrella
        71, 73, 75, 77 -> Icons.Default.AcUnit
        80, 81, 82 -> Icons.Default.WaterDrop
        85, 86 -> Icons.Default.AcUnit
        95 -> Icons.Default.FlashOn
        96, 99 -> Icons.Default.FlashOn
        else -> Icons.Default.Cloud
    }

    /**
     * iOS `WeatherManager.precipitationIconName` 대응.
     *
     * iOS 는 WeatherCondition 이 비/눈이어도 실제 precipitationIntensity 가 0 이면
     * 강수 아이콘 대신 일반 상태 아이콘으로 폴백한다. Open-Meteo 의 WMO code 와
     * 현재 precipitation 값을 함께 사용해 같은 표시 의도를 맞춘다.
     */
    fun weatherCodeToIosPrecipitationIcon(code: Int?, precipitation: Double?): ImageVector {
        if (code == null) return Icons.Default.Cloud

        val hasPrecipitation = (precipitation ?: 0.0) > 0.0
        if (hasPrecipitation) {
            return when (code) {
                51, 53, 55, 56, 57 -> Icons.Default.Grain
                61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Default.Umbrella
                71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
                95, 96, 99 -> Icons.Default.FlashOn
                else -> Icons.Default.Umbrella
            }
        }

        return when (code) {
            0, 1 -> Icons.Default.WbSunny
            2 -> Icons.Default.WbCloudy
            95, 96, 99 -> Icons.Default.FlashOn
            else -> Icons.Default.Cloud
        }
    }
}
