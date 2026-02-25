package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ScienceFiction.DronePassAndroid.R

sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector) {
    /** 로그인 화면 */
    data object Login : Screen("login", R.string.screen_login, Icons.AutoMirrored.Filled.Login)
    data object Map : Screen("map", R.string.screen_map, Icons.Default.LocationOn)
    data object SavedList : Screen("saved_list", R.string.screen_saved_list, Icons.AutoMirrored.Filled.List)
    data object KpForecast : Screen("kp_forecast", R.string.screen_kp_forecast, Icons.Default.BrightnessHigh)
    data object Weather : Screen("weather", R.string.screen_weather, Icons.Default.Cloud)
    data object Settings : Screen("settings", R.string.screen_settings, Icons.Default.Settings)
}
