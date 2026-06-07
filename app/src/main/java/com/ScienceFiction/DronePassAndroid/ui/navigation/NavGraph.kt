package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ScienceFiction.DronePassAndroid.feature.auth.LoginScreen
import com.ScienceFiction.DronePassAndroid.feature.map.MapScreen
import com.ScienceFiction.DronePassAndroid.feature.map.MapViewModel
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsScreen
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchViewModel
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastScreen

@Composable
fun DronePassNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Map.route,
    sketchViewModel: SketchViewModel,
    mapViewModel: MapViewModel,
    onNavigateToMapWithShape: (String) -> Unit = {},
    pendingFocusShapeId: String? = null,
    onPendingShapeConsumed: () -> Unit = {},
    pendingEditShapeId: String? = null,
    onPendingEditShapeConsumed: () -> Unit = {},
    pendingDuplicateShapeId: String? = null,
    onPendingDuplicateShapeConsumed: () -> Unit = {},
    onShapeListFocusRequested: (String) -> Unit = {},
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSkipLogin = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                focusShapeId = pendingFocusShapeId,
                onFocusConsumed = onPendingShapeConsumed,
                editShapeId = pendingEditShapeId,
                onEditShapeConsumed = onPendingEditShapeConsumed,
                duplicateShapeId = pendingDuplicateShapeId,
                onDuplicateShapeConsumed = onPendingDuplicateShapeConsumed,
                onShapeListFocusRequested = onShapeListFocusRequested,
                onNavigateToWeather = {
                    navController.navigate(Screen.Weather.route)
                },
                viewModel = mapViewModel,
                sketchViewModel = sketchViewModel,
            )
        }
        // 저장 목록은 MainScreen 의 오버레이로 표시되므로 NavGraph 라우트가 불필요.
        // KP 예보는 MapScreen 의 ModalBottomSheet 로 표시되므로 마찬가지로 라우트 미사용.
        // (이전에는 dead route 가 남아 있어 진입 경로 혼란을 일으켰음)
        composable(Screen.Weather.route) {
            WeatherForecastScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
