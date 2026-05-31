package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthState
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthViewModel
import com.ScienceFiction.DronePassAndroid.feature.saved.SavedListScreen
import com.ScienceFiction.DronePassAndroid.feature.saved.SavedListViewModel
import com.ScienceFiction.DronePassAndroid.feature.saved.SortDirection
import com.ScienceFiction.DronePassAndroid.feature.saved.SortOption
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsScreen

// MARK: - iOS MainTabView 와 동등한 시각/치수 토큰
// iOS: width 210, height 60, cornerRadius 30, shadow radius 10, bottom padding 15
private val TabBarWidth = 210.dp
private val TabBarHeight = 60.dp
private val TabBarCornerRadius = 30.dp
private val TabBarBottomPadding = 15.dp
private val TabBarShadowElevation = 8.dp
private val TabButtonWidth = 60.dp

// iOS .font(.system(size: 20, weight: .medium)) 와 정확히 일치
private val TabIconSize = 20.dp

// iOS systemBlue / systemGray (라이트 모드)
private val TabSelectedColor = Color(0xFF007AFF)
private val TabUnselectedColor = Color(0xFF8E8E93)

// iOS .ultraThinMaterial 라이트 모드 톤 — minSdk 28 이라 blur 불가, 동일 톤 색으로 흉내
private val OverlayBackgroundColor = Color(0xFFF7F7F8)

// iOS systemGray3 (라이트 모드) — 드래그 핸들 색상
private val OverlayHandleColor = Color(0xFFC7C7CC)

// iOS SavedListOverlayView 정렬 칩 색상 — .blue / .orange (라이트 모드)
private val SortOptionChipColor = Color(0xFF007AFF)
private val SortDirectionChipColor = Color(0xFFFF9500)

// MARK: - iOS SavedListOverlayView / SettingsOverlayView 와 동등한 카드 토큰
// 모서리 40pt, 좌우/하단 16pt 마진
private val OverlayCornerRadius = 40.dp
private val OverlaySideMargin = 16.dp
private val OverlayBottomMargin = 16.dp
private val OverlayShadowElevation = 12.dp

// iOS dismissThreshold = 100, expandThreshold = 50
private val DismissDragThreshold = 100.dp
private val ExpandDragThreshold = 50.dp

// iOS 시트 비율
private const val SheetFractionDefault = 0.5f
private const val SheetFractionExpanded = 0.9f
private const val SheetFractionExpandTrigger = 0.7f

@Composable
fun MainScreen(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val tabScreens = listOf(Screen.Map, Screen.SavedList, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isLoginScreen = currentRoute == Screen.Login.route

    val startDestination = when (authState) {
        is AuthState.Loading -> Screen.Login.route
        is AuthState.LoggedIn -> Screen.Map.route
        is AuthState.LoggedOut -> Screen.Login.route
        is AuthState.Error -> Screen.Login.route
    }

    var pendingFocusShapeId by remember { mutableStateOf<String?>(null) }
    // 저장 탭 → 편집 시 ShapeDetailSheet 자동 표시 우회용 — focus 와 별도 채널
    var pendingEditShapeId by remember { mutableStateOf<String?>(null) }
    var showSavedListOverlay by remember { mutableStateOf(false) }
    var showSettingsOverlay by remember { mutableStateOf(false) }

    val selectedTabRoute = when {
        showSavedListOverlay -> Screen.SavedList.route
        showSettingsOverlay -> Screen.Settings.route
        else -> currentRoute
    }

    // iOS MainTabView 와 동일하게 ZStack(=Box) 구조. 자식 순서가 z-order:
    // 1. NavHost (배경) → 2. FloatingTabBar → 3. SavedList/Settings 오버레이 (탭바 위)
    Box(modifier = Modifier.fillMaxSize()) {
        DronePassNavGraph(
            navController = navController,
            startDestination = startDestination,
            onNavigateToMapWithShape = { shapeId ->
                pendingFocusShapeId = shapeId
                showSavedListOverlay = false
                showSettingsOverlay = false
                navController.navigate(Screen.Map.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            pendingFocusShapeId = pendingFocusShapeId,
            onPendingShapeConsumed = { pendingFocusShapeId = null },
            pendingEditShapeId = pendingEditShapeId,
            onPendingEditShapeConsumed = { pendingEditShapeId = null },
        )

        // Floating tab bar — 오버레이가 떠 있으면 시각적으로 가려지지만 클릭 영역은 살아 있음.
        // (iOS 의 .overlay() 가 탭바를 덮는 구조와 동등)
        if (!isLoginScreen) {
            FloatingTabBar(
                tabs = tabScreens,
                selectedRoute = selectedTabRoute,
                onTabClick = { screen ->
                    handleTabSelection(
                        screen = screen,
                        currentRoute = currentRoute,
                        navController = navController,
                        showSavedListOverlay = showSavedListOverlay,
                        onSavedListOverlayChange = { showSavedListOverlay = it },
                        showSettingsOverlay = showSettingsOverlay,
                        onSettingsOverlayChange = { showSettingsOverlay = it },
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = TabBarBottomPadding),
            )
        }

        // SavedList 오버레이 — iOS SavedListOverlayView 와 동등 (카드형, 50% 고정)
        AnimatedVisibility(
            visible = showSavedListOverlay,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            SavedListOverlay(
                onDismiss = { showSavedListOverlay = false },
                onNavigateToMapWithShape = { shapeId ->
                    pendingFocusShapeId = shapeId
                    showSavedListOverlay = false
                },
                onNavigateToMapForEdit = { shapeId ->
                    pendingEditShapeId = shapeId
                    showSavedListOverlay = false
                },
            )
        }

        // Settings 오버레이 — iOS SettingsOverlayView 와 동등 (카드형, 50%/90% 드래그 확장)
        AnimatedVisibility(
            visible = showSettingsOverlay,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            SettingsOverlay(
                onDismiss = { showSettingsOverlay = false },
            )
        }
    }
}

/**
 * iOS MainTabView 의 floating pill tab bar 와 동등한 Composable.
 * 너비 210dp · 높이 60dp · cornerRadius 30dp · shadow elevation 8dp.
 */
@Composable
private fun FloatingTabBar(
    tabs: List<Screen>,
    selectedRoute: String?,
    onTabClick: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(TabBarWidth)
            .height(TabBarHeight),
        shape = RoundedCornerShape(TabBarCornerRadius),
        color = OverlayBackgroundColor,
        shadowElevation = TabBarShadowElevation,
    ) {
        // iOS HStack 은 자식 너비 합(180pt) 만큼만 차지하고, 그걸 감싸는
        // .frame(width: 210) 의 기본 alignment(.center) 로 가운데 정렬됨 → 좌우 15pt 여유.
        // Android Row 는 fillMaxSize + 기본 Arrangement.Start 라서 좌측에 몰리므로
        // 동일 효과를 위해 spacedBy(0, CenterHorizontally) 로 가운데 정렬 강제.
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
        ) {
            tabs.forEach { screen ->
                FloatingTabButton(
                    screen = screen,
                    isSelected = selectedRoute == screen.route,
                    onClick = { onTabClick(screen) },
                    modifier = Modifier.width(TabButtonWidth),
                )
            }
        }
    }
}

/**
 * iOS CustomTabButton 와 동등한 개별 탭 버튼.
 * 선택 시 selectedIcon · 파란색 · scale 1.1 · SemiBold.
 */
@Composable
private fun FloatingTabButton(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "floating_tab_scale",
    )
    val color = if (isSelected) TabSelectedColor else TabUnselectedColor
    val labelText = stringResource(screen.tabLabelResId ?: screen.titleResId)
    val icon = if (isSelected) screen.selectedIcon else screen.icon
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(TabBarCornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // iOS CustomTabButton 와 동일하게 아이콘·텍스트의 frame 을 명시적으로 고정 →
        // 폰트 측정 높이에 따라 베이스라인이 흔들리지 않아 셋 다 균일 정렬.
        Column(
            modifier = Modifier.scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            // iOS .frame(height: 24)
            Box(
                modifier = Modifier.height(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = labelText,
                    tint = color,
                    modifier = Modifier.size(TabIconSize),
                )
            }
            // iOS .frame(height: 16)
            Box(
                modifier = Modifier.height(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelText,
                    color = color,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * iOS MainTabView.handleTabSelection 와 동등한 탭 선택 처리.
 * - Map 탭: 두 오버레이 닫고 지도로 이동
 * - SavedList 탭: 토글. 열 때 Settings 오버레이는 닫고, 필요시 지도로 복귀
 * - Settings 탭: 토글. 열 때 SavedList 오버레이는 닫고, 필요시 지도로 복귀
 */
private fun handleTabSelection(
    screen: Screen,
    currentRoute: String?,
    navController: NavHostController,
    showSavedListOverlay: Boolean,
    onSavedListOverlayChange: (Boolean) -> Unit,
    showSettingsOverlay: Boolean,
    onSettingsOverlayChange: (Boolean) -> Unit,
) {
    when (screen) {
        Screen.SavedList -> {
            if (showSavedListOverlay) {
                onSavedListOverlayChange(false)
            } else {
                onSettingsOverlayChange(false)
                onSavedListOverlayChange(true)
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
        Screen.Settings -> {
            if (showSettingsOverlay) {
                onSettingsOverlayChange(false)
            } else {
                onSavedListOverlayChange(false)
                onSettingsOverlayChange(true)
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
            onSavedListOverlayChange(false)
            onSettingsOverlayChange(false)
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

/**
 * iOS SavedListOverlayView (iPhone) 과 동등.
 * - 50% 고정 높이 카드 / 좌우·하단 16dp / 모서리 40dp
 * - 드래그 핸들 + "저장 목록" + 정렬 옵션/방향 칩(파랑/주황) 한 줄 헤더 (iOS IMG_0392 동등)
 * - 아래로 100dp 이상 드래그 시 닫힘 / 외부 영역 터치 시 닫힘
 *
 * SavedListViewModel 은 이 오버레이에서 호스팅해 SavedListScreen 과 동일 인스턴스 공유.
 * 헤더 정렬 칩 ↔ SavedListScreen 내부 리스트가 같은 sortOption/sortDirection 을 보도록 보장.
 */
@Composable
private fun SavedListOverlay(
    onDismiss: () -> Unit,
    onNavigateToMapWithShape: (String) -> Unit,
    onNavigateToMapForEdit: (String) -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val baseHeight = (configuration.screenHeightDp * SheetFractionDefault).dp
    val dismissThresholdPx = with(density) { DismissDragThreshold.toPx() }

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val dragOffsetDp = with(density) { dragOffsetPx.toDp() }
    val targetHeight = (baseHeight - dragOffsetDp).coerceAtLeast(0.dp)
    val animatedHeight by animateDpAsState(
        targetValue = if (isDragging) targetHeight else baseHeight,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "saved_overlay_height",
    )

    // 헤더 칩 ↔ 콘텐츠 리스트가 같은 상태를 보도록 ViewModel 을 오버레이에서 호스팅
    val savedListViewModel: SavedListViewModel = hiltViewModel()
    val sortOption by savedListViewModel.sortOption.collectAsStateWithLifecycle()
    val sortDirection by savedListViewModel.sortDirection.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // 외부 영역 터치 시 닫힘 (iOS 동등 — 별도 시각 스크림 없음)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = OverlaySideMargin,
                    end = OverlaySideMargin,
                    bottom = OverlayBottomMargin,
                )
                .fillMaxWidth()
                .height(animatedHeight),
            shape = RoundedCornerShape(OverlayCornerRadius),
            color = OverlayBackgroundColor,
            shadowElevation = OverlayShadowElevation,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 헤더 영역에만 드래그 부착 — SavedListScreen 내부 LazyColumn 과 충돌 회피
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragOffsetPx = 0f
                                },
                                onDragEnd = {
                                    val translation = dragOffsetPx
                                    if (translation > dismissThresholdPx) {
                                        onDismiss()
                                    }
                                    dragOffsetPx = 0f
                                    isDragging = false
                                },
                                onDragCancel = {
                                    dragOffsetPx = 0f
                                    isDragging = false
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    // iOS 와 동일하게 아래로만 시각 변화 (양수)
                                    dragOffsetPx = (dragOffsetPx + dragAmount)
                                        .coerceAtLeast(0f)
                                },
                            )
                        },
                ) {
                    OverlayDragHandle()
                    // 헤더 — iOS IMG_0392 동등: "저장 목록" + 정렬 옵션 칩(파랑) + 정렬 방향 칩(주황)
                    SavedListHeaderRow(
                        sortOption = sortOption,
                        sortDirection = sortDirection,
                        onCycleSortOption = {
                            savedListViewModel.updateSortOption(sortOption.next())
                        },
                        onToggleSortDirection = savedListViewModel::toggleSortDirection,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SavedListScreen(
                    onNavigateToMapWithShape = onNavigateToMapWithShape,
                    onNavigateToMapForEdit = onNavigateToMapForEdit,
                    viewModel = savedListViewModel,
                )
            }
        }
    }
}

/**
 * iOS SettingsOverlayView (iPhone) 과 동등.
 * - 50% 시작 / 위로 -50dp 이상 드래그시 90% 확장
 * - 아래로 100dp 이상 → 닫힘
 * - 90% 상태에서 아래로 드래그시 50% 축소
 * - 큰 "설정" 제목 (titleLarge bold)
 */
@Composable
private fun SettingsOverlay(
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    val dismissThresholdPx = with(density) { DismissDragThreshold.toPx() }
    val expandThresholdPx = with(density) { ExpandDragThreshold.toPx() }

    var sheetHeightFraction by remember { mutableFloatStateOf(SheetFractionDefault) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // iOS 와 동일: dragOffset 은 양수일 때만 시각 변화 반영 (높이 감소)
    val visibleDragDp = with(density) { dragOffsetPx.coerceAtLeast(0f).toDp() }
    val baseHeight = screenHeight * sheetHeightFraction
    val targetHeight = (baseHeight - visibleDragDp).coerceAtLeast(0.dp)
    val animatedHeight by animateDpAsState(
        targetValue = if (isDragging) targetHeight else baseHeight,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "settings_overlay_height",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = OverlaySideMargin,
                    end = OverlaySideMargin,
                    bottom = OverlayBottomMargin,
                )
                .fillMaxWidth()
                .height(animatedHeight),
            shape = RoundedCornerShape(OverlayCornerRadius),
            color = OverlayBackgroundColor,
            shadowElevation = OverlayShadowElevation,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 헤더 영역에만 드래그 부착 — SettingsScreen 내부 verticalScroll 과 충돌 회피
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragOffsetPx = 0f
                                },
                                onDragEnd = {
                                    val translation = dragOffsetPx
                                    when {
                                        // 경우 1: 위로 expandThreshold 이상 + 현재 < 70% → 90%로 확장
                                        translation < -expandThresholdPx &&
                                            sheetHeightFraction < SheetFractionExpandTrigger -> {
                                            sheetHeightFraction = SheetFractionExpanded
                                        }
                                        // 경우 2: 아래로 dismissThreshold 이상 → 닫기
                                        translation > dismissThresholdPx -> {
                                            sheetHeightFraction = SheetFractionDefault
                                            onDismiss()
                                        }
                                        // 경우 3: 아래로 + 현재 > 70% → 50%로 축소
                                        translation > 0f &&
                                            sheetHeightFraction > SheetFractionExpandTrigger -> {
                                            sheetHeightFraction = SheetFractionDefault
                                        }
                                        // else: 원위치 (애니메이션이 자동 처리)
                                    }
                                    dragOffsetPx = 0f
                                    isDragging = false
                                },
                                onDragCancel = {
                                    dragOffsetPx = 0f
                                    isDragging = false
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    // 위/아래 모두 누적 (방향 판단은 onDragEnd 에서)
                                    dragOffsetPx += dragAmount
                                },
                            )
                        },
                ) {
                    OverlayDragHandle()
                    // 헤더 — iOS .font(.title) .bold "설정"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.screen_settings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                SettingsScreen(showTopAppBar = false)
            }
        }
    }
}

/**
 * iOS SavedListOverlay / SettingsOverlay 상단의 RoundedRectangle 드래그 핸들.
 * 너비 40dp · 높이 5dp · 모서리 2.5dp · systemGray3 색상.
 */
@Composable
private fun OverlayDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(OverlayHandleColor),
        )
    }
}

/**
 * iOS SavedListOverlayView 헤더 한 줄 — "저장 목록" + 정렬 옵션 칩 + 정렬 방향 칩.
 * iOS IMG_0392 의 헤더 정렬을 그대로 재현 (제목 좌측, 칩 우측 정렬).
 */
@Composable
private fun SavedListHeaderRow(
    sortOption: SortOption,
    sortDirection: SortDirection,
    onCycleSortOption: () -> Unit,
    onToggleSortDirection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.screen_saved_list),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        SortChip(
            text = stringResource(sortOption.labelRes),
            iconVector = Icons.Default.SwapVert,
            color = SortOptionChipColor,
            onClick = onCycleSortOption,
        )
        Spacer(modifier = Modifier.width(8.dp))
        SortChip(
            text = stringResource(sortDirection.labelRes),
            iconVector = if (sortDirection == SortDirection.ASCENDING) {
                Icons.Default.ArrowUpward
            } else {
                Icons.Default.ArrowDownward
            },
            color = SortDirectionChipColor,
            onClick = onToggleSortDirection,
        )
    }
}

/**
 * iOS 정렬 칩 — color.opacity(0.1) 배경 + 같은 색의 아이콘/텍스트 + cornerRadius 8.
 */
@Composable
private fun SortChip(
    text: String,
    iconVector: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** iOS cycleSortOption 동등 — enum 다음 값으로 cycle. */
private fun SortOption.next(): SortOption {
    val values = SortOption.entries
    return values[(ordinal + 1) % values.size]
}
