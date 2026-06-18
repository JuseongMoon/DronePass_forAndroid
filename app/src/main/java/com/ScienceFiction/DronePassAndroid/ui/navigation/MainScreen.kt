package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.ui.currentWindowSizeDp
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthViewModel
import com.ScienceFiction.DronePassAndroid.feature.auth.ForegroundSyncDialogState
import com.ScienceFiction.DronePassAndroid.feature.map.MapViewModel
import com.ScienceFiction.DronePassAndroid.feature.saved.SavedListScreen
import com.ScienceFiction.DronePassAndroid.feature.saved.SavedListViewModel
import com.ScienceFiction.DronePassAndroid.feature.saved.SortDirection
import com.ScienceFiction.DronePassAndroid.feature.saved.SortOption
import com.ScienceFiction.DronePassAndroid.feature.saved.nextSavedSortOption
import com.ScienceFiction.DronePassAndroid.feature.settings.SettingsScreen
import com.ScienceFiction.DronePassAndroid.feature.sketch.SketchViewModel
import com.ScienceFiction.DronePassAndroid.service.ForegroundNotification
import com.ScienceFiction.DronePassAndroid.service.ForegroundNotificationBus
import kotlinx.coroutines.delay

// MARK: - iOS MainTabView 와 동등한 시각/치수 토큰
// iOS: width 210, height 60, cornerRadius 30, shadow radius 10, bottom padding phone 15 / iPad 20
private val TabBarWidth = 210.dp
private val TabBarHeight = 60.dp
private val TabBarCornerRadius = 30.dp
internal val TabBarPhoneBottomPadding = 15.dp
internal val TabBarTabletBottomPadding = 20.dp
private val TabBarShadowElevation = 8.dp
private val TabButtonWidth = 60.dp

// iOS .font(.system(size: 20, weight: .medium)) 와 정확히 일치
internal val TabIconSize = 20.dp
internal val TabIconFrameHeight = 24.dp
internal val TabLabelFrameHeight = 16.dp
internal val TabLabelTextSize = 11.sp
internal val TabLabelLineHeight = 11.sp
internal const val TabSelectedScale = 1.1f
internal const val TabUnselectedScale = 1.0f
internal const val TabScaleDampingRatio = 0.7f

// iOS systemBlue / systemGray (라이트 모드)
private val TabSelectedColor = Color(0xFF007AFF)
private val TabUnselectedColor = Color(0xFF8E8E93)

// iOS .ultraThinMaterial 라이트 모드 톤 — minSdk 28 이라 blur 불가, 동일 톤 색으로 흉내
private val OverlayBackgroundColor = Color(0xFFF7F7F8)

// iOS Color(UIColor.systemBackground) — 오버레이 상단 핸들/헤더 영역
internal val OverlayHeaderBackgroundColor = Color.White

// iOS systemGray3 (라이트 모드) — 드래그 핸들 색상
internal val OverlayHandleColor = Color(0xFFC7C7CC)
internal val OverlayDragHandleWidth = 40.dp
internal val OverlayDragHandleHeight = 5.dp
internal val OverlayDragHandleCornerRadius = 2.5.dp
internal val OverlayDragHandleTopPadding = 12.dp
internal val OverlayDragHandleBottomPadding = 8.dp
internal val SavedOverlayHeaderHorizontalPadding = 16.dp
internal val SavedOverlayHeaderBottomPaddingTablet = 4.dp
internal val SavedOverlayHeaderBottomPaddingPhone = 16.dp
internal val SavedOverlayListTopSpacing = 8.dp
internal val SavedOverlaySortChipCornerRadius = 8.dp
internal val SavedOverlaySortChipHorizontalPadding = 8.dp
internal val SavedOverlaySortChipVerticalPadding = 4.dp
internal val SavedOverlaySortChipContentSpacing = 4.dp
internal const val SavedOverlaySortChipBackgroundAlpha = 0.1f
internal val SavedOverlaySortChipPhoneTextSize = 12.sp
internal val SavedOverlaySortChipTabletTextSize = 11.sp
internal val SavedOverlaySortChipPhoneIconSize = 12.dp
internal val SavedOverlaySortChipTabletIconSize = 11.dp
internal val SettingsOverlayHeaderHorizontalPadding = 16.dp
internal val SettingsOverlayHeaderBottomPadding = 4.dp

// iOS SavedListOverlayView 정렬 칩 색상 — .blue / .orange (라이트 모드)
private val SortOptionChipColor = Color(0xFF007AFF)
private val SortDirectionChipColor = Color(0xFFFF9500)

// MARK: - iOS SavedListOverlayView / SettingsOverlayView 와 동등한 카드 토큰
// 모서리 40pt, 좌우/하단 16pt 마진
private val OverlayCornerRadius = 40.dp
private val OverlaySideMargin = 16.dp
private val OverlayBottomMargin = 16.dp
private val OverlayShadowElevation = 12.dp
private const val TabletBreakpointDp = 600
private val TabletOverlayLeadingMargin = 20.dp
private val TabletOverlayTopMargin = 40.dp
private val TabletOverlayMaxWidth = 400.dp
internal val PhoneNotificationPopupMaxWidth = 320.dp
internal val TabletNotificationPopupMaxWidth = 400.dp
internal val NotificationPopupContentPadding = 24.dp
internal val NotificationPopupSpacing = 20.dp
internal val NotificationPopupIconTopPadding = 8.dp
internal val NotificationPopupButtonTopPadding = 4.dp
internal val NotificationPopupButtonVerticalPadding = 14.dp
internal val NotificationPopupCornerRadius = 20.dp
internal val NotificationPopupButtonCornerRadius = 12.dp
internal const val NotificationPopupDimAlpha = 0.4f
internal val NotificationPopupShadowElevation = 20.dp
internal val NotificationPopupTitleTextSize = 17.sp
internal val NotificationPopupBodyTextSize = 17.sp
internal val NotificationPopupButtonTextSize = 17.sp
internal const val NotificationPopupAnimationDurationMs = 250
internal const val NotificationPopupInitialScale = 0.9f
private val TabletOverlayCornerRadius = 16.dp
private const val TabletOverlayWidthFraction = 0.4f
private const val TabletOverlayHeightFraction = 0.8f

// iOS dismissThreshold = 100, expandThreshold = 50
private val DismissDragThreshold = 100.dp
private val ExpandDragThreshold = 50.dp

// iOS 시트 비율
private const val SheetFractionDefault = 0.5f
private const val SheetFractionExpanded = 0.9f
private const val SheetFractionExpandTrigger = 0.7f
private val SavedOverlayPhoneMaxHeight = 500.dp
internal const val SavedOverlayInitialFocusDelayMs = 500L

internal fun resolveMainStartDestination(): String = Screen.Map.route

internal fun resolveMainSelectedTabRoute(
    currentRoute: String?,
    showSavedListOverlay: Boolean,
    showSettingsOverlay: Boolean,
): String? {
    return when {
        showSavedListOverlay -> Screen.SavedList.route
        showSettingsOverlay -> Screen.Settings.route
        else -> currentRoute
    }
}

internal fun shouldShowFloatingTabBar(
    isSketchMode: Boolean,
): Boolean = !isSketchMode

internal fun resolveFloatingTabButtonScale(isSelected: Boolean): Float {
    return if (isSelected) TabSelectedScale else TabUnselectedScale
}

internal fun resolveTabBarBottomPadding(isTablet: Boolean): Dp {
    return if (isTablet) TabBarTabletBottomPadding else TabBarPhoneBottomPadding
}

internal data class MainOverlayVisibility(
    val showSavedListOverlay: Boolean,
    val showSettingsOverlay: Boolean,
)

internal data class MainTabSelectionResult(
    val overlayVisibility: MainOverlayVisibility,
    val shouldNavigateToMap: Boolean,
)

internal fun resolveMainTabSelection(
    screen: Screen,
    currentRoute: String?,
    showSavedListOverlay: Boolean,
    showSettingsOverlay: Boolean,
): MainTabSelectionResult {
    return when (screen) {
        Screen.SavedList -> {
            if (showSavedListOverlay) {
                MainTabSelectionResult(
                    overlayVisibility = MainOverlayVisibility(
                        showSavedListOverlay = false,
                        showSettingsOverlay = false,
                    ),
                    shouldNavigateToMap = false,
                )
            } else {
                MainTabSelectionResult(
                    overlayVisibility = MainOverlayVisibility(
                        showSavedListOverlay = true,
                        showSettingsOverlay = false,
                    ),
                    shouldNavigateToMap = currentRoute != Screen.Map.route,
                )
            }
        }
        Screen.Settings -> {
            if (showSettingsOverlay) {
                MainTabSelectionResult(
                    overlayVisibility = MainOverlayVisibility(
                        showSavedListOverlay = false,
                        showSettingsOverlay = false,
                    ),
                    shouldNavigateToMap = false,
                )
            } else {
                MainTabSelectionResult(
                    overlayVisibility = MainOverlayVisibility(
                        showSavedListOverlay = false,
                        showSettingsOverlay = true,
                    ),
                    shouldNavigateToMap = currentRoute != Screen.Map.route,
                )
            }
        }
        Screen.Map -> MainTabSelectionResult(
            overlayVisibility = MainOverlayVisibility(
                showSavedListOverlay = false,
                showSettingsOverlay = false,
            ),
            shouldNavigateToMap = true,
        )
    }
}

internal fun resolveMainOverlayVisibilityAfterSketchModeChange(
    isSketchMode: Boolean,
    showSavedListOverlay: Boolean,
    showSettingsOverlay: Boolean,
): MainOverlayVisibility {
    return if (isSketchMode) {
        MainOverlayVisibility(
            showSavedListOverlay = false,
            showSettingsOverlay = false,
        )
    } else {
        MainOverlayVisibility(
            showSavedListOverlay = showSavedListOverlay,
            showSettingsOverlay = showSettingsOverlay,
        )
    }
}

internal fun shouldDelaySavedOverlayFocus(wasOverlayClosed: Boolean): Boolean = wasOverlayClosed

internal data class SavedOverlayFocusTarget(
    val selectionShapeId: String?,
    val immediateShapeId: String?,
    val delayedShapeId: String?,
)

internal fun resolveSavedOverlayFocusTarget(
    shapeId: String,
    wasOverlayClosed: Boolean,
): SavedOverlayFocusTarget {
    return if (shouldDelaySavedOverlayFocus(wasOverlayClosed)) {
        SavedOverlayFocusTarget(
            selectionShapeId = shapeId,
            immediateShapeId = null,
            delayedShapeId = shapeId,
        )
    } else {
        SavedOverlayFocusTarget(
            selectionShapeId = shapeId,
            immediateShapeId = shapeId,
            delayedShapeId = null,
        )
    }
}

internal fun shouldDismissSavedOverlayAfterShapeTapInOverlay(): Boolean = false

internal fun shouldDismissMainOverlayOnOutsideTap(): Boolean = false

internal fun resolveSavedOverlayPhoneBaseHeight(screenHeight: Dp): Dp {
    return minOf(screenHeight * SheetFractionDefault, SavedOverlayPhoneMaxHeight)
}

internal fun resolveTabletOverlayWidth(screenWidth: Dp): Dp {
    return minOf(screenWidth * TabletOverlayWidthFraction, TabletOverlayMaxWidth)
}

internal fun resolveTabletOverlayHeight(screenHeight: Dp): Dp {
    return screenHeight * TabletOverlayHeightFraction
}

internal fun resolveMainOverlayCornerRadius(isTablet: Boolean): Dp {
    return if (isTablet) TabletOverlayCornerRadius else OverlayCornerRadius
}

internal fun notificationPopupEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(durationMillis = NotificationPopupAnimationDurationMs)) +
        scaleIn(
            initialScale = NotificationPopupInitialScale,
            animationSpec = tween(durationMillis = NotificationPopupAnimationDurationMs),
        )
}

internal fun notificationPopupExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(durationMillis = NotificationPopupAnimationDurationMs)) +
        scaleOut(
            targetScale = NotificationPopupInitialScale,
            animationSpec = tween(durationMillis = NotificationPopupAnimationDurationMs),
        )
}

internal fun resolveNotificationPopupWidth(
    screenWidth: Dp,
    isTablet: Boolean,
): Dp {
    val maxWidth = if (isTablet) {
        TabletNotificationPopupMaxWidth
    } else {
        PhoneNotificationPopupMaxWidth
    }
    return minOf(screenWidth, maxWidth)
}

internal fun mainOverlayEnterTransition(isTablet: Boolean): EnterTransition {
    val moveTransition = if (isTablet) {
        slideInHorizontally(initialOffsetX = { -it })
    } else {
        slideInVertically(initialOffsetY = { it })
    }
    return moveTransition + fadeIn()
}

internal fun mainOverlayExitTransition(isTablet: Boolean): ExitTransition {
    val moveTransition = if (isTablet) {
        slideOutHorizontally(targetOffsetX = { -it })
    } else {
        slideOutVertically(targetOffsetY = { it })
    }
    return moveTransition + fadeOut()
}

internal fun resolveSavedOverlayPhoneDragOffset(
    dragOffset: Dp,
    isDragging: Boolean,
): Dp {
    return if (isDragging && dragOffset > 0.dp) dragOffset else 0.dp
}

internal fun resolveSavedOverlayHeaderBottomPadding(isTablet: Boolean): Dp {
    return if (isTablet) {
        SavedOverlayHeaderBottomPaddingTablet
    } else {
        SavedOverlayHeaderBottomPaddingPhone
    }
}

internal fun resolveSavedOverlaySortChipTextSize(isTablet: Boolean): TextUnit {
    return if (isTablet) {
        SavedOverlaySortChipTabletTextSize
    } else {
        SavedOverlaySortChipPhoneTextSize
    }
}

internal fun resolveSavedOverlaySortChipIconSize(isTablet: Boolean): Dp {
    return if (isTablet) {
        SavedOverlaySortChipTabletIconSize
    } else {
        SavedOverlaySortChipPhoneIconSize
    }
}

internal fun resolveSavedSortOptionIcon(option: SortOption): ImageVector {
    return when (option) {
        SortOption.TITLE,
        SortOption.DATE_CREATED,
        SortOption.FLIGHT_START,
        SortOption.FLIGHT_END -> Icons.Default.SwapVert
    }
}

internal fun resolveSavedSortDirectionIcon(direction: SortDirection): ImageVector {
    return if (direction == SortDirection.ASCENDING) {
        Icons.Default.ArrowDownward
    } else {
        Icons.Default.ArrowUpward
    }
}

internal data class SettingsOverlayPhoneDragEnd(
    val sheetHeightFraction: Float,
    val shouldDismiss: Boolean,
)

internal fun resolveSettingsOverlayPhoneDragEnd(
    translation: Float,
    currentSheetHeightFraction: Float,
    dismissThreshold: Float,
    expandThreshold: Float,
): SettingsOverlayPhoneDragEnd {
    return when {
        translation < -expandThreshold &&
            currentSheetHeightFraction < SheetFractionExpandTrigger -> {
            SettingsOverlayPhoneDragEnd(
                sheetHeightFraction = SheetFractionExpanded,
                shouldDismiss = false,
            )
        }
        translation > dismissThreshold -> {
            SettingsOverlayPhoneDragEnd(
                sheetHeightFraction = SheetFractionDefault,
                shouldDismiss = true,
            )
        }
        translation > 0f &&
            currentSheetHeightFraction > SheetFractionExpandTrigger -> {
            SettingsOverlayPhoneDragEnd(
                sheetHeightFraction = SheetFractionDefault,
                shouldDismiss = false,
            )
        }
        else -> {
            SettingsOverlayPhoneDragEnd(
                sheetHeightFraction = currentSheetHeightFraction,
                shouldDismiss = false,
            )
        }
    }
}

@Composable
internal fun MainScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    sketchViewModel: SketchViewModel = hiltViewModel(),
    mapViewModel: MapViewModel = hiltViewModel(),
    initialFocusShapeId: String? = null,
    initialForegroundNotification: ForegroundNotification? = null,
    onInitialFocusShapeConsumed: () -> Unit = {},
    onInitialForegroundNotificationConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val isSketchMode by sketchViewModel.isSketchMode.collectAsStateWithLifecycle()
    val tabScreens = listOf(Screen.Map, Screen.SavedList, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val windowSize = currentWindowSizeDp()
    val isTablet = windowSize.width >= TabletBreakpointDp.dp
    val lifecycleOwner = LocalLifecycleOwner.current

    var pendingFocusShapeId by remember { mutableStateOf<String?>(null) }
    // 저장 탭 → 편집 시 ShapeDetailSheet 자동 표시 우회용 — focus 와 별도 채널
    var pendingEditShapeId by remember { mutableStateOf<String?>(null) }
    var pendingDuplicateShapeId by remember { mutableStateOf<String?>(null) }
    var savedOverlaySelectionShapeId by remember { mutableStateOf<String?>(null) }
    var savedOverlayFocusShapeId by remember { mutableStateOf<String?>(null) }
    var delayedSavedOverlayFocusShapeId by remember { mutableStateOf<String?>(null) }
    var showSavedListOverlay by remember { mutableStateOf(false) }
    var showSettingsOverlay by remember { mutableStateOf(false) }
    var foregroundNotification by remember { mutableStateOf<ForegroundNotification?>(null) }
    var displayedForegroundNotification by remember { mutableStateOf<ForegroundNotification?>(null) }
    var showForegroundSyncConfirmation by remember { mutableStateOf(false) }
    var foregroundSyncDialog by remember { mutableStateOf<ForegroundSyncDialogState?>(null) }

    fun dismissSavedListOverlay() {
        showSavedListOverlay = false
        savedOverlaySelectionShapeId = null
        savedOverlayFocusShapeId = null
        delayedSavedOverlayFocusShapeId = null
        mapViewModel.clearSelection()
    }

    LaunchedEffect(delayedSavedOverlayFocusShapeId, showSavedListOverlay) {
        val shapeId = delayedSavedOverlayFocusShapeId ?: return@LaunchedEffect
        if (!showSavedListOverlay) return@LaunchedEffect

        delay(SavedOverlayInitialFocusDelayMs)
        if (showSavedListOverlay && delayedSavedOverlayFocusShapeId == shapeId) {
            savedOverlayFocusShapeId = shapeId
            delayedSavedOverlayFocusShapeId = null
        }
    }

    LaunchedEffect(initialFocusShapeId) {
        val shapeId = initialFocusShapeId ?: return@LaunchedEffect
        pendingFocusShapeId = shapeId
        dismissSavedListOverlay()
        showSettingsOverlay = false
        onInitialFocusShapeConsumed()

        if (currentRoute != null &&
            currentRoute != Screen.Map.route
        ) {
            navController.navigate(Screen.Map.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(initialForegroundNotification) {
        val notification = initialForegroundNotification ?: return@LaunchedEffect
        foregroundNotification = notification
        onInitialForegroundNotificationConsumed()
    }

    val selectedTabRoute = resolveMainSelectedTabRoute(
        currentRoute = currentRoute,
        showSavedListOverlay = showSavedListOverlay,
        showSettingsOverlay = showSettingsOverlay,
    )

    LaunchedEffect(isSketchMode) {
        val overlayVisibility = resolveMainOverlayVisibilityAfterSketchModeChange(
            isSketchMode = isSketchMode,
            showSavedListOverlay = showSavedListOverlay,
            showSettingsOverlay = showSettingsOverlay,
        )
        if (showSavedListOverlay != overlayVisibility.showSavedListOverlay) {
            dismissSavedListOverlay()
        }
        if (showSettingsOverlay != overlayVisibility.showSettingsOverlay) {
            showSettingsOverlay = overlayVisibility.showSettingsOverlay
        }
    }

    LaunchedEffect(Unit) {
        ForegroundNotificationBus.events.collect { notification ->
            foregroundNotification = notification
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.foregroundSyncConfirmation.collect {
            showForegroundSyncConfirmation = true
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.foregroundSyncDialogState.collect { dialogState ->
            foregroundSyncDialog = dialogState
        }
    }

    LaunchedEffect(foregroundNotification) {
        foregroundNotification?.let { notification ->
            displayedForegroundNotification = notification
        }
    }

    DisposableEffect(lifecycleOwner, authViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> authViewModel.ensureCloudSyncActiveOnForeground()
                Lifecycle.Event.ON_STOP -> authViewModel.resetForegroundSyncCheckStatus()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // iOS MainTabView 와 동일하게 ZStack(=Box) 구조. 자식 순서가 z-order:
    // 1. NavHost (배경) → 2. SavedList/Settings 오버레이 → 3. FloatingTabBar
    Box(modifier = Modifier.fillMaxSize()) {
        DronePassNavGraph(
            navController = navController,
            sketchViewModel = sketchViewModel,
            mapViewModel = mapViewModel,
            onNavigateToMapWithShape = { shapeId ->
                pendingFocusShapeId = shapeId
                dismissSavedListOverlay()
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
            pendingDuplicateShapeId = pendingDuplicateShapeId,
            onPendingDuplicateShapeConsumed = { pendingDuplicateShapeId = null },
            onShapeListFocusRequested = { shapeId ->
                val wasOverlayClosed = !showSavedListOverlay
                val focusTarget = resolveSavedOverlayFocusTarget(
                    shapeId = shapeId,
                    wasOverlayClosed = wasOverlayClosed,
                )
                savedOverlaySelectionShapeId = focusTarget.selectionShapeId
                savedOverlayFocusShapeId = focusTarget.immediateShapeId
                delayedSavedOverlayFocusShapeId = focusTarget.delayedShapeId
                showSettingsOverlay = false
                showSavedListOverlay = true
            },
        )

        // SavedList 오버레이 — iOS 와 동일하게 태블릿은 좌측 패널, 폰은 하단 시트
        AnimatedVisibility(
            visible = showSavedListOverlay,
            enter = mainOverlayEnterTransition(isTablet),
            exit = mainOverlayExitTransition(isTablet),
        ) {
            SavedListOverlay(
                isTablet = isTablet,
                onDismiss = ::dismissSavedListOverlay,
                selectionShapeId = savedOverlaySelectionShapeId,
                onSelectionConsumed = { savedOverlaySelectionShapeId = null },
                focusShapeId = savedOverlayFocusShapeId,
                onFocusConsumed = { savedOverlayFocusShapeId = null },
                onNavigateToMapWithShape = { shapeId ->
                    pendingFocusShapeId = shapeId
                    showSettingsOverlay = false
                    if (shouldDismissSavedOverlayAfterShapeTapInOverlay()) {
                        dismissSavedListOverlay()
                    }
                },
                onNavigateToMapForEdit = { shapeId ->
                    pendingEditShapeId = shapeId
                    showSettingsOverlay = false
                    dismissSavedListOverlay()
                },
                onNavigateToMapForDuplicate = { shapeId ->
                    pendingDuplicateShapeId = shapeId
                    showSettingsOverlay = false
                    dismissSavedListOverlay()
                },
            )
        }

        // Settings 오버레이 — iOS 와 동일하게 태블릿은 좌측 패널, 폰은 50%/90% 하단 시트
        AnimatedVisibility(
            visible = showSettingsOverlay,
            enter = mainOverlayEnterTransition(isTablet),
            exit = mainOverlayExitTransition(isTablet),
        ) {
            SettingsOverlay(
                isTablet = isTablet,
                onDismiss = { showSettingsOverlay = false },
                onAccountSessionEnded = {
                    mapViewModel.clearMapHighlightForAccountSessionEnd()
                },
            )
        }

        // Floating tab bar — 오버레이가 떠 있어도 탭 전환이 가능해야 하므로
        // SavedList/Settings 오버레이 위에 둔다. 포그라운드 알림 팝업은 아래 블록에서
        // 가장 위에 표시된다.
        if (shouldShowFloatingTabBar(isSketchMode = isSketchMode)) {
            FloatingTabBar(
                tabs = tabScreens,
                selectedRoute = selectedTabRoute,
                onTabClick = { screen ->
                    handleTabSelection(
                        screen = screen,
                        currentRoute = currentRoute,
                        navController = navController,
                        showSavedListOverlay = showSavedListOverlay,
                        onSavedListOverlayChange = { isVisible ->
                            if (isVisible) {
                                showSavedListOverlay = true
                            } else {
                                dismissSavedListOverlay()
                            }
                        },
                        showSettingsOverlay = showSettingsOverlay,
                        onSettingsOverlayChange = { showSettingsOverlay = it },
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = resolveTabBarBottomPadding(isTablet)),
            )
        }

        AnimatedVisibility(
            visible = foregroundNotification != null,
            enter = notificationPopupEnterTransition(),
            exit = notificationPopupExitTransition(),
        ) {
            displayedForegroundNotification?.let { notification ->
                PushNotificationOverlay(
                    notification = notification,
                    isTablet = isTablet,
                    onDismiss = { foregroundNotification = null },
                )
            }
        }

        if (showForegroundSyncConfirmation) {
            AlertDialog(
                onDismissRequest = { showForegroundSyncConfirmation = false },
                title = { Text(stringResource(R.string.sync_alert_detected_title)) },
                text = { Text(stringResource(R.string.sync_alert_detected_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showForegroundSyncConfirmation = false
                            authViewModel.confirmForegroundCloudSync()
                        },
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForegroundSyncConfirmation = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        }

        foregroundSyncDialog?.let { dialogState ->
            ForegroundSyncDialog(
                dialogState = dialogState,
                onDismiss = { foregroundSyncDialog = null },
            )
        }
    }
}

@Composable
private fun ForegroundSyncDialog(
    dialogState: ForegroundSyncDialogState,
    onDismiss: () -> Unit,
) {
    when (dialogState) {
        ForegroundSyncDialogState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.sync_loading_title)) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.sync_loading_message))
                    }
                },
                confirmButton = {},
            )
        }
        ForegroundSyncDialogState.Complete -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.sync_complete_title)) },
                text = { Text(stringResource(R.string.sync_complete_message)) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_confirm))
                    }
                },
            )
        }
        is ForegroundSyncDialogState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.sync_error_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.sync_error_message,
                            dialogState.message,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_confirm))
                    }
                },
            )
        }
    }
}

/**
 * iOS PushNotificationPopupView 와 동등한 포그라운드 푸시 팝업.
 */
@Composable
private fun PushNotificationOverlay(
    notification: ForegroundNotification,
    isTablet: Boolean,
    onDismiss: () -> Unit,
) {
    val popupWidth = resolveNotificationPopupWidth(
        screenWidth = currentWindowSizeDp().width,
        isTablet = isTablet,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = NotificationPopupDimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .width(popupWidth)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            shape = RoundedCornerShape(NotificationPopupCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = NotificationPopupShadowElevation,
        ) {
            Column(
                modifier = Modifier.padding(NotificationPopupContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NotificationPopupSpacing),
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = TabSelectedColor,
                    modifier = Modifier
                        .padding(top = NotificationPopupIconTopPadding)
                        .size(40.dp),
                )
                Text(
                    text = notification.title,
                    fontSize = NotificationPopupTitleTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = notification.body,
                    fontSize = NotificationPopupBodyTextSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = NotificationPopupButtonTopPadding),
                    shape = RoundedCornerShape(NotificationPopupButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelectedColor),
                    contentPadding = PaddingValues(
                        vertical = NotificationPopupButtonVerticalPadding,
                        horizontal = 16.dp,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.common_confirm),
                        fontSize = NotificationPopupButtonTextSize,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
        targetValue = resolveFloatingTabButtonScale(isSelected),
        animationSpec = spring(
            dampingRatio = TabScaleDampingRatio,
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
                modifier = Modifier.height(TabIconFrameHeight),
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
                modifier = Modifier.height(TabLabelFrameHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelText,
                    color = color,
                    fontSize = TabLabelTextSize,
                    lineHeight = TabLabelLineHeight,
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
    val result = resolveMainTabSelection(
        screen = screen,
        currentRoute = currentRoute,
        showSavedListOverlay = showSavedListOverlay,
        showSettingsOverlay = showSettingsOverlay,
    )
    onSavedListOverlayChange(result.overlayVisibility.showSavedListOverlay)
    onSettingsOverlayChange(result.overlayVisibility.showSettingsOverlay)

    if (result.shouldNavigateToMap) {
        navController.navigate(Screen.Map.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

/**
 * iOS SavedListOverlayView 와 동등.
 * - 태블릿: 좌측 패널, 화면 폭 40%/최대 400dp, 높이 80%, 오른쪽 드래그 닫기
 * - 폰: 50% 하단 시트, 아래 드래그 닫기
 */
@Composable
private fun SavedListOverlay(
    isTablet: Boolean,
    onDismiss: () -> Unit,
    selectionShapeId: String? = null,
    onSelectionConsumed: () -> Unit = {},
    focusShapeId: String? = null,
    onFocusConsumed: () -> Unit = {},
    onNavigateToMapWithShape: (String) -> Unit,
    onNavigateToMapForEdit: (String) -> Unit = {},
    onNavigateToMapForDuplicate: (String) -> Unit = {},
) {
    val windowSize = currentWindowSizeDp()
    val density = LocalDensity.current
    val screenHeight = windowSize.height
    val baseHeight = resolveSavedOverlayPhoneBaseHeight(screenHeight)
    val tabletPanelWidth = resolveTabletOverlayWidth(windowSize.width)
    val tabletPanelHeight = resolveTabletOverlayHeight(screenHeight)
    val dismissThresholdPx = with(density) { DismissDragThreshold.toPx() }

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val visibleDragDp = with(density) { dragOffsetPx.coerceAtLeast(0f).toDp() }
    val phoneDragOffset = resolveSavedOverlayPhoneDragOffset(
        dragOffset = visibleDragDp,
        isDragging = isDragging,
    )
    val animatedVerticalOffset by animateDpAsState(
        targetValue = if (!isTablet) phoneDragOffset else 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "saved_overlay_vertical_offset",
    )
    val animatedHorizontalOffset by animateDpAsState(
        targetValue = if (isTablet && isDragging) visibleDragDp else 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "saved_overlay_horizontal_offset",
    )

    // 헤더 칩 ↔ 콘텐츠 리스트가 같은 상태를 보도록 ViewModel 을 오버레이에서 호스팅
    val savedListViewModel: SavedListViewModel = hiltViewModel()
    val sortOption by savedListViewModel.sortOption.collectAsStateWithLifecycle()
    val sortDirection by savedListViewModel.sortDirection.collectAsStateWithLifecycle()
    val dismissOverlay = {
        savedListViewModel.dismissShapeDetail()
        onDismiss()
    }
    DisposableEffect(savedListViewModel) {
        onDispose {
            savedListViewModel.dismissShapeDetail()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (shouldDismissMainOverlayOnOutsideTap()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = dismissOverlay,
                    )
            )
        }

        Surface(
            modifier = if (isTablet) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = TabletOverlayLeadingMargin,
                        top = TabletOverlayTopMargin,
                    )
                    .offset {
                        IntOffset(
                            x = with(density) { animatedHorizontalOffset.roundToPx() },
                            y = 0,
                        )
                    }
                    .width(tabletPanelWidth)
                    .height(tabletPanelHeight)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = OverlaySideMargin,
                        end = OverlaySideMargin,
                        bottom = OverlayBottomMargin,
                    )
                    .offset {
                        IntOffset(
                            x = 0,
                            y = with(density) { animatedVerticalOffset.roundToPx() },
                        )
                    }
                    .fillMaxWidth()
                    .height(baseHeight)
            },
            shape = RoundedCornerShape(resolveMainOverlayCornerRadius(isTablet)),
            color = OverlayBackgroundColor,
            shadowElevation = OverlayShadowElevation,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 헤더 영역에만 드래그 부착 — SavedListScreen 내부 LazyColumn 과 충돌 회피
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OverlayHeaderBackgroundColor)
                        .pointerInput(isTablet) {
                            if (isTablet) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        dragOffsetPx = 0f
                                    },
                                    onDragEnd = {
                                        val translation = dragOffsetPx
                                        if (translation > dismissThresholdPx) {
                                            dismissOverlay()
                                        }
                                        dragOffsetPx = 0f
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        dragOffsetPx = 0f
                                        isDragging = false
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetPx = (dragOffsetPx + dragAmount)
                                            .coerceAtLeast(0f)
                                    },
                                )
                            } else {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        dragOffsetPx = 0f
                                    },
                                    onDragEnd = {
                                        val translation = dragOffsetPx
                                        if (translation > dismissThresholdPx) {
                                            dismissOverlay()
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
                                        dragOffsetPx = (dragOffsetPx + dragAmount)
                                            .coerceAtLeast(0f)
                                    },
                                )
                            }
                        },
                ) {
                    OverlayDragHandle()
                    SavedListHeaderRow(
                        isTablet = isTablet,
                        sortOption = sortOption,
                        sortDirection = sortDirection,
                        onCycleSortOption = {
                            savedListViewModel.updateSortOption(nextSavedSortOption(sortOption))
                        },
                        onToggleSortDirection = savedListViewModel::toggleSortDirection,
                    )
                }
                Spacer(modifier = Modifier.height(SavedOverlayListTopSpacing))
                SavedListScreen(
                    onNavigateToMapWithShape = onNavigateToMapWithShape,
                    onNavigateToMapForEdit = onNavigateToMapForEdit,
                    onNavigateToMapForDuplicate = onNavigateToMapForDuplicate,
                    selectionShapeId = selectionShapeId,
                    onSelectionConsumed = onSelectionConsumed,
                    focusShapeId = focusShapeId,
                    onFocusConsumed = onFocusConsumed,
                    viewModel = savedListViewModel,
                )
            }
        }
    }
}

/**
 * iOS SettingsOverlayView 와 동등.
 * - 태블릿: 좌측 패널, 화면 폭 40%/최대 400dp, 높이 80%, 오른쪽 드래그 닫기
 * - 폰: 50% 시작 / 위로 드래그시 90% 확장 / 아래 드래그 닫기 또는 축소
 */
@Composable
private fun SettingsOverlay(
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onAccountSessionEnded: () -> Unit = {},
) {
    val windowSize = currentWindowSizeDp()
    val density = LocalDensity.current
    val screenHeight = windowSize.height
    val tabletPanelWidth = resolveTabletOverlayWidth(windowSize.width)
    val tabletPanelHeight = resolveTabletOverlayHeight(screenHeight)
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
    val animatedHorizontalOffset by animateDpAsState(
        targetValue = if (isTablet && isDragging) visibleDragDp else 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "settings_overlay_horizontal_offset",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (shouldDismissMainOverlayOnOutsideTap()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    )
            )
        }

        Surface(
            modifier = if (isTablet) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = TabletOverlayLeadingMargin,
                        top = TabletOverlayTopMargin,
                    )
                    .offset {
                        IntOffset(
                            x = with(density) { animatedHorizontalOffset.roundToPx() },
                            y = 0,
                        )
                    }
                    .width(tabletPanelWidth)
                    .height(tabletPanelHeight)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = OverlaySideMargin,
                        end = OverlaySideMargin,
                        bottom = OverlayBottomMargin,
                    )
                    .fillMaxWidth()
                    .height(animatedHeight)
            },
            shape = RoundedCornerShape(resolveMainOverlayCornerRadius(isTablet)),
            color = OverlayBackgroundColor,
            shadowElevation = OverlayShadowElevation,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 헤더 영역에만 드래그 부착 — SettingsScreen 내부 verticalScroll 과 충돌 회피
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OverlayHeaderBackgroundColor)
                        .pointerInput(isTablet, sheetHeightFraction) {
                            if (isTablet) {
                                detectHorizontalDragGestures(
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
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetPx = (dragOffsetPx + dragAmount)
                                            .coerceAtLeast(0f)
                                    },
                                )
                            } else {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        dragOffsetPx = 0f
                                    },
                                    onDragEnd = {
                                        val dragEnd = resolveSettingsOverlayPhoneDragEnd(
                                            translation = dragOffsetPx,
                                            currentSheetHeightFraction = sheetHeightFraction,
                                            dismissThreshold = dismissThresholdPx,
                                            expandThreshold = expandThresholdPx,
                                        )
                                        sheetHeightFraction = dragEnd.sheetHeightFraction
                                        if (dragEnd.shouldDismiss) {
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
                                        // 위/아래 모두 누적 (방향 판단은 onDragEnd 에서)
                                        dragOffsetPx += dragAmount
                                    },
                                )
                            }
                        },
                ) {
                    OverlayDragHandle()
                    // 헤더 — iOS .font(.title) .bold "설정"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SettingsOverlayHeaderHorizontalPadding)
                            .padding(bottom = SettingsOverlayHeaderBottomPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.screen_settings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                SettingsScreen(
                    onAccountSessionEnded = onAccountSessionEnded,
                )
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
            .padding(
                top = OverlayDragHandleTopPadding,
                bottom = OverlayDragHandleBottomPadding,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(OverlayDragHandleWidth)
                .height(OverlayDragHandleHeight)
                .clip(RoundedCornerShape(OverlayDragHandleCornerRadius))
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
    isTablet: Boolean,
    sortOption: SortOption,
    sortDirection: SortDirection,
    onCycleSortOption: () -> Unit,
    onToggleSortDirection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SavedOverlayHeaderHorizontalPadding)
            .padding(bottom = resolveSavedOverlayHeaderBottomPadding(isTablet)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.screen_saved_list),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        SortChip(
            isTablet = isTablet,
            text = stringResource(sortOption.labelRes),
            iconVector = resolveSavedSortOptionIcon(sortOption),
            color = SortOptionChipColor,
            onClick = onCycleSortOption,
        )
        Spacer(modifier = Modifier.width(8.dp))
        SortChip(
            isTablet = isTablet,
            text = stringResource(sortDirection.labelRes),
            iconVector = resolveSavedSortDirectionIcon(sortDirection),
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
    isTablet: Boolean,
    text: String,
    iconVector: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SavedOverlaySortChipCornerRadius))
            .background(color.copy(alpha = SavedOverlaySortChipBackgroundAlpha))
            .clickable(onClick = onClick)
            .padding(
                horizontal = SavedOverlaySortChipHorizontalPadding,
                vertical = SavedOverlaySortChipVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SavedOverlaySortChipContentSpacing),
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(resolveSavedOverlaySortChipIconSize(isTablet)),
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontSize = resolveSavedOverlaySortChipTextSize(isTablet),
            fontWeight = FontWeight.Medium,
        )
    }
}
