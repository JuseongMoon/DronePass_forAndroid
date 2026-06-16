package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ScienceFiction.DronePassAndroid.feature.map.MapScreen
import com.ScienceFiction.DronePassAndroid.feature.map.MapViewModel
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchViewModel

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
        composable(Screen.Map.route) {
            MapScreen(
                focusShapeId = pendingFocusShapeId,
                onFocusConsumed = onPendingShapeConsumed,
                editShapeId = pendingEditShapeId,
                onEditShapeConsumed = onPendingEditShapeConsumed,
                duplicateShapeId = pendingDuplicateShapeId,
                onDuplicateShapeConsumed = onPendingDuplicateShapeConsumed,
                onShapeListFocusRequested = onShapeListFocusRequested,
                viewModel = mapViewModel,
                sketchViewModel = sketchViewModel,
            )
        }
        // 로그인/저장 목록/설정/KP/날씨는 iOS처럼 MainScreen/Settings/MapScreen 의 sheet 또는 overlay 로 표시한다.
        // 별도 route 를 남기면 전체 화면 헤더로 잘못 진입할 수 있어 실제 내비게이션 표면에서 제외한다.
    }
}
