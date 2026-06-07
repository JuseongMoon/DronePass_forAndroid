package com.ScienceFiction.DronePassAndroid.feature.sketch

import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.math.abs
import kotlin.math.min

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
    val diff = abs(normalizeSketchHue(currentHue) - normalizeSketchHue(newHue))
    val shortestDiff = min(diff, 360f - diff)
    return shortestDiff > thresholdDegrees
}

private fun normalizeSketchHue(hue: Float): Float {
    val normalized = hue % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}
