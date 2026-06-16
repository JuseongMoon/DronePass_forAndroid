package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ScienceFiction.DronePassAndroid.R

sealed class Screen(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    @StringRes val tabLabelResId: Int? = null,
) {
    /** 지도 — iOS: map / map.fill */
    data object Map : Screen(
        route = "map",
        titleResId = R.string.screen_map,
        icon = Icons.Outlined.Map,
        selectedIcon = Icons.Filled.Map,
        tabLabelResId = R.string.tab_label_map,
    )

    /** 저장 목록 — iOS: tray.full / tray.full.fill */
    data object SavedList : Screen(
        route = "saved_list",
        titleResId = R.string.screen_saved_list,
        icon = Icons.Outlined.Inventory2,
        selectedIcon = Icons.Filled.Inventory2,
        tabLabelResId = R.string.tab_label_saved,
    )

    /** 설정 — iOS: gearshape / gearshape.fill */
    data object Settings : Screen(
        route = "settings",
        titleResId = R.string.screen_settings,
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        tabLabelResId = R.string.tab_label_settings,
    )
}
