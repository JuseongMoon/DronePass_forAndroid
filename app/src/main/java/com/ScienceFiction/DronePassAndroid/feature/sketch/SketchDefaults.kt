package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ScienceFiction.DronePassAndroid.core.util.parseIosOpaqueRgbHexColor
import java.util.Locale
import kotlin.math.abs

internal const val DefaultSketchColor = "#FF0000"
internal const val DefaultSketchStrokeWidth = 4.0
internal const val DefaultSketchOpacity = 1.0

internal object SketchPreferenceKeys {
    val CURRENT_COLOR = stringPreferencesKey("sketchCurrentColor")
    val CURRENT_STROKE_WIDTH = doublePreferencesKey("sketchCurrentStrokeWidth")
    val CURRENT_OPACITY = doublePreferencesKey("sketchCurrentOpacity")
}

internal fun clampSketchStrokeWidth(width: Double): Double {
    return width.coerceIn(1.0, 20.0)
}

internal fun clampSketchOpacity(opacity: Double): Double {
    return opacity.coerceIn(0.1, 1.0)
}

internal fun normalizeSketchFirestoreColor(color: String): String {
    val parsed = parseIosOpaqueRgbHexColor(color) ?: return DefaultSketchColor
    return String.format(Locale.US, "#%06X", parsed and 0xFFFFFF)
}

internal fun shouldEnterSketchMode(isSketchModeActive: Boolean): Boolean {
    return !isSketchModeActive
}

internal data class SketchModeTransition(
    val shouldTransition: Boolean,
    val isSketchModeActive: Boolean,
    val isEraserModeActive: Boolean,
)

internal fun enterSketchModeTransition(
    isSketchModeActive: Boolean,
    isEraserModeActive: Boolean,
): SketchModeTransition {
    return SketchModeTransition(
        shouldTransition = !isSketchModeActive,
        isSketchModeActive = true,
        isEraserModeActive = isEraserModeActive,
    )
}

internal fun exitSketchModeTransition(
    isSketchModeActive: Boolean,
    isEraserModeActive: Boolean,
): SketchModeTransition {
    return SketchModeTransition(
        shouldTransition = isSketchModeActive,
        isSketchModeActive = false,
        isEraserModeActive = isEraserModeActive,
    )
}

internal fun shouldSyncSketchHueSlider(
    currentHue: Float,
    newHue: Float,
    thresholdDegrees: Float = 18f,
): Boolean {
    return abs(normalizeSketchHue(currentHue) - normalizeSketchHue(newHue)) > thresholdDegrees
}

private fun normalizeSketchHue(hue: Float): Float {
    val normalized = hue % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}
