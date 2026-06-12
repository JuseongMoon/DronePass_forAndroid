package com.ScienceFiction.DronePassAndroid.feature.shape

internal fun formatShapeTitle(title: String, emptyFallback: String): String =
    title.ifEmpty { emptyFallback }
