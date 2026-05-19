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
     * Jetpack Compose Color로 변환.
     *
     * 정의된 enum 항목의 hex 는 빌드 타임 상수이지만, 외부에서 hex 문자열이
     * 손상된 채로 들어와 [fromHex] 등을 거치지 않고 직접 [PaletteColor.composeColor]
     * 가 호출될 일은 없다. 그래도 [android.graphics.Color.parseColor] 가 던지는
     * [IllegalArgumentException] 을 방어하여 UI 크래시를 차단한다.
     */
    val composeColor: Color
        get() = try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: IllegalArgumentException) {
            DEFAULT_FALLBACK
        }

    companion object {
        /** parseColor 실패 시 사용할 폴백 (#007AFF — iOS DronePass 기본 파랑) */
        private val DEFAULT_FALLBACK = Color(0xFF007AFF)

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
