package com.ScienceFiction.DronePassAndroid.feature.shape

import java.text.DateFormat
import java.util.Locale

internal fun shapeEditDateOnlyFormat(locale: Locale = Locale.getDefault()): DateFormat =
    DateFormat.getDateInstance(DateFormat.MEDIUM, locale)

internal fun localizedShapeDateTimeFormat(locale: Locale = Locale.getDefault()): DateFormat =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
