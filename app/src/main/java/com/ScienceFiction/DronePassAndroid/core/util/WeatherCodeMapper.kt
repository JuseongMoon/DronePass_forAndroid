package com.ScienceFiction.DronePassAndroid.core.util

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

/**
 * WMO 날씨 코드 -> 설명/아이콘 매핑
 */
object WeatherCodeMapper {

    fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "맑음"
        1 -> "대체로 맑음"
        2 -> "구름 조금"
        3 -> "흐림"
        4, 5 -> "연무" // WMO 04: smoke / 05: haze (Open-Meteo 외 표준 코드 호환)
        10 -> "옅은 안개" // WMO 10: mist
        45, 48 -> "안개"
        51, 53, 55 -> "이슬비"
        56, 57 -> "어는 이슬비"
        61, 63, 65 -> "비"
        66, 67 -> "어는 비"
        71, 73, 75 -> "눈"
        77 -> "싸라기눈"
        80, 81, 82 -> "소나기"
        85, 86 -> "눈소나기"
        95 -> "뇌우"
        96, 99 -> "우박을 동반한 뇌우"
        else -> "알 수 없음"
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
}
