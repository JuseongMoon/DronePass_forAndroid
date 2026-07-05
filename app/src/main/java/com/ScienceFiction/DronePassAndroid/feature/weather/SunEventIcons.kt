package com.ScienceFiction.DronePassAndroid.feature.weather

import androidx.annotation.DrawableRes
import com.ScienceFiction.DronePassAndroid.R

internal const val IosSunriseSymbolName = "sunrise.fill"
internal const val IosSunsetSymbolName = "sunset.fill"

internal fun iosSunEventSymbolName(isSunrise: Boolean): String =
    if (isSunrise) IosSunriseSymbolName else IosSunsetSymbolName

@DrawableRes
internal fun iosSunEventDrawableRes(isSunrise: Boolean): Int =
    if (isSunrise) {
        R.drawable.ic_sunrise_fill_ios_like
    } else {
        R.drawable.ic_sunset_fill_ios_like
    }
