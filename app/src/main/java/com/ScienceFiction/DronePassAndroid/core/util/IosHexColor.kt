package com.ScienceFiction.DronePassAndroid.core.util

/**
 * iOS `UIColor(hex:)` 와 같은 HEX 스캔 규칙.
 *
 * `#` 제거 후 선행 HEX 토큰을 UInt64 로 읽고, 하위 24비트 RGB만 사용하며 alpha 는 1.0으로 고정한다.
 */
internal fun parseIosOpaqueRgbHexColor(colorString: String): Int? {
    val hex = colorString.trim()
        .replace("#", "")
        .takeWhile { char -> char.isDigit() || char.lowercaseChar() in 'a'..'f' }
    if (hex.isEmpty()) return null

    val rgb = hex.toLongOrNull(radix = 16) ?: return null
    val red = ((rgb and 0xFF0000) shr 16).toInt()
    val green = ((rgb and 0x00FF00) shr 8).toInt()
    val blue = (rgb and 0x0000FF).toInt()
    return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
}
