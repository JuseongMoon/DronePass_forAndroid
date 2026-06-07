package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor

@Composable
fun PaletteColor.localizedLabel(): String =
    stringResource(colorNameRes)

@get:StringRes
private val PaletteColor.colorNameRes: Int
    get() = when (this) {
        PaletteColor.RED -> R.string.palette_color_red
        PaletteColor.ORANGE -> R.string.palette_color_orange
        PaletteColor.YELLOW -> R.string.palette_color_yellow
        PaletteColor.GREEN -> R.string.palette_color_green
        PaletteColor.TEAL -> R.string.palette_color_teal
        PaletteColor.BLUE -> R.string.palette_color_blue
        PaletteColor.INDIGO -> R.string.palette_color_indigo
        PaletteColor.PURPLE -> R.string.palette_color_purple
        PaletteColor.PINK -> R.string.palette_color_pink
        PaletteColor.GRAY -> R.string.palette_color_gray
    }
