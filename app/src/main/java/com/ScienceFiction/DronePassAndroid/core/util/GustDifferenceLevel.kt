package com.ScienceFiction.DronePassAndroid.core.util

enum class GustDifferenceLevel(val label: String, val colorLong: Long) {
    SAFE("안전", 0xFF4CAF50),
    LOCALIZED_GUST("국지 돌풍", 0xFFFFC107),
    CAUTION("주의", 0xFFFF9800),
    DANGER("위험", 0xFFF44336)
}
