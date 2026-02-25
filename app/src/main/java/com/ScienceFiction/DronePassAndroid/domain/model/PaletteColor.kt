package com.ScienceFiction.DronePassAndroid.domain.model

import androidx.compose.ui.graphics.Color

enum class PaletteColor(val hex: String, val koreanName: String) {
    RED("#FF3B30", "빨강"),
    ORANGE("#FF9500", "주황"),
    YELLOW("#FFCC00", "노랑"),
    GREEN("#34C759", "초록"),
    TEAL("#5AC8FA", "청록"),
    BLUE("#007AFF", "파랑"),
    INDIGO("#5856D6", "남색"),
    PURPLE("#AF52DE", "보라"),
    PINK("#FF2D55", "분홍"),
    GRAY("#8E8E93", "회색");

    /**
     * Jetpack Compose Color로 변환
     */
    val composeColor: Color
        get() = Color(android.graphics.Color.parseColor(hex))

    companion object {
        /**
         * HEX 문자열로부터 PaletteColor 검색
         */
        fun fromHex(hex: String): PaletteColor? =
            entries.find { it.hex.equals(hex, ignoreCase = true) }

        /**
         * 인덱스에 따라 순환적으로 색상 반환
         */
        fun colorAtIndex(index: Int): PaletteColor =
            entries[index % entries.size]
    }
}
