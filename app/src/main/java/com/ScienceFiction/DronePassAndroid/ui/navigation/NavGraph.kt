package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ScienceFiction.DronePassAndroid.feature.auth.LoginScreen
import com.ScienceFiction.DronePassAndroid.feature.kp.KpForecastScreen
import com.ScienceFiction.DronePassAndroid.feature.map.MapScreen
import com.ScienceFiction.DronePassAndroid.feature.saved.SavedListScreen
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsScreen
import com.ScienceFiction.DronePassAndroid.feature.weather.WeatherForecastScreen

@Composable
fun DronePassNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Map.route,
    onNavigateToMapWithShape: (String) -> Unit = {},
    pendingFocusShapeId: String? = null,
    onPendingShapeConsumed: () -> Unit = {}
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
                onNavigateToWeather = {
                    navController.navigate(Screen.Weather.route)
                }
            )
        }
        composable(Screen.SavedList.route) {
            SavedListScreen(
                onNavigateToMapWithShape = onNavigateToMapWithShape
            )
        }
        composable(Screen.KpForecast.route) { KpForecastScreen() }
        composable(Screen.Weather.route) {
            WeatherForecastScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
