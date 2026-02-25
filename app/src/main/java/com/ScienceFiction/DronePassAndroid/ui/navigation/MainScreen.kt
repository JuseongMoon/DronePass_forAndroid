package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthState
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthViewModel
import com.ScienceFiction.DronePassAndroid.feature.saved.SavedListScreen

@Composable
fun MainScreen(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val screens = listOf(Screen.Map, Screen.SavedList, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 로그인 화면에서는 하단 네비게이션 바를 숨김
    val isLoginScreen = currentRoute == Screen.Login.route

    // 인증 상태에 따른 시작 화면 결정
    val startDestination = when (authState) {
        is AuthState.Loading -> Screen.Login.route
        is AuthState.LoggedIn -> Screen.Map.route
        is AuthState.LoggedOut -> Screen.Login.route
        is AuthState.Error -> Screen.Login.route
    }

    // 탭 간 연동: 저장 목록에서 도형 선택 시 지도로 이동
    var pendingFocusShapeId by remember { mutableStateOf<String?>(null) }

    // 저장 목록 오버레이 표시 상태
    var showSavedListOverlay by remember { mutableStateOf(false) }

    // 시각적으로 선택된 탭 결정
    val selectedTab = when {
        showSavedListOverlay -> Screen.SavedList.route
        else -> currentRoute
    }

    Scaffold(
        bottomBar = {
            if (!isLoginScreen) {
                NavigationBar {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleResId)) },
                            label = { Text(stringResource(screen.titleResId)) },
                            selected = selectedTab == screen.route,
                            onClick = {
                                when (screen) {
                                    Screen.SavedList -> {
                                        // 토글 동작: 같은 탭 다시 누르면 닫기
                                        if (showSavedListOverlay) {
                                            showSavedListOverlay = false
                                        } else {
                                            showSavedListOverlay = true
                                            // 현재 라우트가 Map이 아니면 Map으로 이동
                                            if (currentRoute != Screen.Map.route) {
                                                navController.navigate(Screen.Map.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        showSavedListOverlay = false
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // NavHost (지도는 항상 배경에)
            DronePassNavGraph(
                navController = navController,
                startDestination = startDestination,
                onNavigateToMapWithShape = { shapeId ->
                    pendingFocusShapeId = shapeId
                    showSavedListOverlay = false
                    navController.navigate(Screen.Map.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                pendingFocusShapeId = pendingFocusShapeId,
                onPendingShapeConsumed = { pendingFocusShapeId = null }
            )

            // SavedList 오버레이
            AnimatedVisibility(
                visible = showSavedListOverlay,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                SavedListOverlay(
                    onDismiss = { showSavedListOverlay = false },
                    onNavigateToMapWithShape = { shapeId ->
                        pendingFocusShapeId = shapeId
                        showSavedListOverlay = false
                    }
                )
            }
        }
    }
}

/**
 * 저장 목록 오버레이: 하단 50% 높이로 지도 위에 표시
 */
@Composable
private fun SavedListOverlay(
    onDismiss: () -> Unit,
    onNavigateToMapWithShape: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val overlayHeight = (configuration.screenHeightDp * 0.5f).dp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 지도 영역 터치 시 오버레이 닫기 (반투명 스크림)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        // 오버레이 콘텐츠
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(overlayHeight),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column {
                // 드래그 핸들
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                }

                // SavedListScreen 콘텐츠
                SavedListScreen(
                    onNavigateToMapWithShape = onNavigateToMapWithShape
                )
            }
        }
    }
}
