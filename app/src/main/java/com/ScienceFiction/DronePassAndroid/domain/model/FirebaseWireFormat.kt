package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.Locale

private val FirebaseHexColorPattern = Regex("^#[0-9A-Fa-f]{6}$")

internal fun isValidFirebaseHexColor(color: String): Boolean =
    FirebaseHexColorPattern.matches(color)

internal fun normalizeFirebaseHexColorForWrite(color: String): String =
    if (isValidFirebaseHexColor(color)) {
        color.uppercase(Locale.ROOT)
    } else {
        color
    }

internal fun normalizeFirebaseHexColorForRead(color: String, fallback: String): String =
    if (isValidFirebaseHexColor(color)) {
        color
    } else {
        fallback
    }
