package com.ScienceFiction.DronePassAndroid.domain.model

private val FirebaseHexColorPattern = Regex("^#[0-9A-Fa-f]{6}$")

internal fun isValidFirebaseHexColor(color: String): Boolean =
    FirebaseHexColorPattern.matches(color)
