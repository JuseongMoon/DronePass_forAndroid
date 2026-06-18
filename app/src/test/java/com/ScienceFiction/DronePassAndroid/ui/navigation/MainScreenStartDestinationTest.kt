package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.feature.saved.SortDirection
import com.ScienceFiction.DronePassAndroid.feature.saved.SortOption
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenStartDestinationTest {

    @Test
    fun `앱 시작 화면은 iOS처럼 인증 상태와 무관하게 지도이다`() {
        assertEquals(Screen.Map.route, resolveMainStartDestination())
    }

    @Test
    fun `하단 탭 선택 상태는 iOS처럼 저장과 설정 오버레이를 우선한다`() {
        assertEquals(
            Screen.SavedList.route,
            resolveMainSelectedTabRoute(
                currentRoute = Screen.Map.route,
                showSavedListOverlay = true,
                showSettingsOverlay = false,
            ),
        )
        assertEquals(
            Screen.Settings.route,
            resolveMainSelectedTabRoute(
                currentRoute = Screen.Map.route,
                showSavedListOverlay = false,
                showSettingsOverlay = true,
            ),
        )
        assertEquals(
            Screen.Map.route,
            resolveMainSelectedTabRoute(
                currentRoute = Screen.Map.route,
                showSavedListOverlay = false,
                showSettingsOverlay = false,
            ),
        )
    }

    @Test
    fun `저장 탭 선택은 iOS처럼 설정 오버레이를 닫고 저장 오버레이를 연다`() {
        assertEquals(
            MainTabSelectionResult(
                overlayVisibility = MainOverlayVisibility(
                    showSavedListOverlay = true,
                    showSettingsOverlay = false,
                ),
                shouldNavigateToMap = false,
            ),
            resolveMainTabSelection(
                screen = Screen.SavedList,
                currentRoute = Screen.Map.route,
                showSavedListOverlay = false,
                showSettingsOverlay = true,
            ),
        )
    }

    @Test
    fun `저장 탭을 다시 선택하면 iOS처럼 저장 오버레이를 닫는다`() {
        assertEquals(
            MainTabSelectionResult(
                overlayVisibility = MainOverlayVisibility(
                    showSavedListOverlay = false,
                    showSettingsOverlay = false,
                ),
                shouldNavigateToMap = false,
            ),
            resolveMainTabSelection(
                screen = Screen.SavedList,
                currentRoute = Screen.Map.route,
                showSavedListOverlay = true,
                showSettingsOverlay = false,
            ),
        )
    }

    @Test
    fun `설정 탭 선택은 iOS처럼 저장 오버레이를 닫고 설정 오버레이를 연다`() {
        assertEquals(
            MainTabSelectionResult(
                overlayVisibility = MainOverlayVisibility(
                    showSavedListOverlay = false,
                    showSettingsOverlay = true,
                ),
                shouldNavigateToMap = false,
            ),
            resolveMainTabSelection(
                screen = Screen.Settings,
                currentRoute = Screen.Map.route,
                showSavedListOverlay = true,
                showSettingsOverlay = false,
            ),
        )
    }

    @Test
    fun `설정 탭을 다시 선택하면 iOS처럼 설정 오버레이를 닫는다`() {
        assertEquals(
            MainTabSelectionResult(
                overlayVisibility = MainOverlayVisibility(
                    showSavedListOverlay = false,
                    showSettingsOverlay = false,
                ),
                shouldNavigateToMap = false,
            ),
            resolveMainTabSelection(
                screen = Screen.Settings,
                currentRoute = Screen.Map.route,
                showSavedListOverlay = false,
                showSettingsOverlay = true,
            ),
        )
    }

    @Test
    fun `지도 탭 선택은 iOS처럼 모든 오버레이를 닫고 지도로 이동한다`() {
        assertEquals(
            MainTabSelectionResult(
                overlayVisibility = MainOverlayVisibility(
                    showSavedListOverlay = false,
                    showSettingsOverlay = false,
                ),
                shouldNavigateToMap = true,
            ),
            resolveMainTabSelection(
                screen = Screen.Map,
                currentRoute = Screen.SavedList.route,
                showSavedListOverlay = true,
                showSettingsOverlay = true,
            ),
        )
    }

    @Test
    fun `저장 또는 설정 탭을 맵 밖에서 열 때는 iOS처럼 배경을 지도 경로로 되돌린다`() {
        assertEquals(
            true,
            resolveMainTabSelection(
                screen = Screen.SavedList,
                currentRoute = Screen.Settings.route,
                showSavedListOverlay = false,
                showSettingsOverlay = false,
            ).shouldNavigateToMap,
        )
        assertEquals(
            true,
            resolveMainTabSelection(
                screen = Screen.Settings,
                currentRoute = Screen.SavedList.route,
                showSavedListOverlay = false,
                showSettingsOverlay = false,
            ).shouldNavigateToMap,
        )
    }

    @Test
    fun `하단 탭바는 iOS처럼 스케치 모드에서 숨긴다`() {
        assertEquals(true, shouldShowFloatingTabBar(isSketchMode = false))
        assertEquals(false, shouldShowFloatingTabBar(isSketchMode = true))
    }

    @Test
    fun `하단 탭바 bottom padding은 Android 폰에서 낮게 두고 tablet은 iOS처럼 20dp 이다`() {
        assertEquals(8.dp, TabBarPhoneBottomPadding)
        assertEquals(20.dp, TabBarTabletBottomPadding)
        assertEquals(8.dp, resolveTabBarBottomPadding(isTablet = false))
        assertEquals(20.dp, resolveTabBarBottomPadding(isTablet = true))
    }

    @Test
    fun `하단 탭 버튼 토큰은 iOS CustomTabButton 과 일치한다`() {
        assertEquals(1.1f, TabSelectedScale, 0f)
        assertEquals(1.0f, TabUnselectedScale, 0f)
        assertEquals(0.7f, TabScaleDampingRatio, 0f)
        assertEquals(1.1f, resolveFloatingTabButtonScale(isSelected = true), 0f)
        assertEquals(1.0f, resolveFloatingTabButtonScale(isSelected = false), 0f)
        assertEquals(20.dp, TabIconSize)
        assertEquals(24.dp, TabIconFrameHeight)
        assertEquals(16.dp, TabLabelFrameHeight)
        assertEquals(11.sp, TabLabelTextSize)
        assertEquals(11.sp, TabLabelLineHeight)
    }

    @Test
    fun `스케치 모드 진입 시 iOS처럼 저장과 설정 오버레이를 모두 닫는다`() {
        assertEquals(
            MainOverlayVisibility(showSavedListOverlay = false, showSettingsOverlay = false),
            resolveMainOverlayVisibilityAfterSketchModeChange(
                isSketchMode = true,
                showSavedListOverlay = true,
                showSettingsOverlay = true,
            ),
        )
        assertEquals(
            MainOverlayVisibility(showSavedListOverlay = true, showSettingsOverlay = false),
            resolveMainOverlayVisibilityAfterSketchModeChange(
                isSketchMode = false,
                showSavedListOverlay = true,
                showSettingsOverlay = false,
            ),
        )
    }

    @Test
    fun `저장 오버레이가 닫힌 상태의 도형 포커스 요청은 iOS처럼 선택은 즉시 스크롤은 500ms 지연한다`() {
        assertEquals(500L, SavedOverlayInitialFocusDelayMs)
        assertEquals(true, shouldDelaySavedOverlayFocus(wasOverlayClosed = true))
        assertEquals(
            SavedOverlayFocusTarget(
                selectionShapeId = "shape-1",
                immediateShapeId = null,
                delayedShapeId = "shape-1",
            ),
            resolveSavedOverlayFocusTarget(shapeId = "shape-1", wasOverlayClosed = true),
        )
    }

    @Test
    fun `저장 오버레이가 이미 열린 상태의 도형 포커스 요청은 즉시 처리한다`() {
        assertEquals(false, shouldDelaySavedOverlayFocus(wasOverlayClosed = false))
        assertEquals(
            SavedOverlayFocusTarget(
                selectionShapeId = "shape-1",
                immediateShapeId = "shape-1",
                delayedShapeId = null,
            ),
            resolveSavedOverlayFocusTarget(shapeId = "shape-1", wasOverlayClosed = false),
        )
    }

    @Test
    fun `저장 오버레이 안에서 도형을 탭하면 iOS처럼 오버레이를 닫지 않는다`() {
        assertEquals(false, shouldDismissSavedOverlayAfterShapeTapInOverlay())
    }

    @Test
    fun `저장과 설정 오버레이는 iOS처럼 바깥 영역 탭으로 닫히지 않는다`() {
        assertEquals(false, shouldDismissMainOverlayOnOutsideTap())
    }

    @Test
    fun `저장 목록 폰 오버레이 높이는 지도 컨트롤을 덜 가리도록 화면 30퍼센트와 500dp 중 작은 값이다`() {
        assertEquals(240f, resolveSavedOverlayPhoneBaseHeight(800.dp).value, 0.001f)
        assertEquals(360f, resolveSavedOverlayPhoneBaseHeight(1_200.dp).value, 0.001f)
        assertEquals(420f, resolveSavedOverlayPhoneBaseHeight(1_400.dp).value, 0.001f)
        assertEquals(450f, resolveSavedOverlayPhoneBaseHeight(1_500.dp).value, 0.001f)
        assertEquals(500f, resolveSavedOverlayPhoneBaseHeight(1_800.dp).value, 0.001f)
    }

    @Test
    fun `저장과 설정 태블릿 오버레이 크기는 iOS처럼 폭 40퍼센트 최대 400dp 높이 80퍼센트이다`() {
        assertEquals(320.dp, resolveTabletOverlayWidth(800.dp))
        assertEquals(400.dp, resolveTabletOverlayWidth(1_200.dp))
        assertEquals(640.dp, resolveTabletOverlayHeight(800.dp))
    }

    @Test
    fun `저장과 설정 오버레이 모서리는 iOS처럼 phone 40dp tablet 16dp 이다`() {
        assertEquals(40.dp, resolveMainOverlayCornerRadius(isTablet = false))
        assertEquals(16.dp, resolveMainOverlayCornerRadius(isTablet = true))
    }

    @Test
    fun `저장과 설정 폰 오버레이는 iOS처럼 화면 하단에 붙는다`() {
        assertEquals(0.dp, OverlayBottomMargin)
    }

    @Test
    fun `저장 목록 폰 오버레이는 iOS처럼 드래그 중 높이를 줄이지 않고 아래로 이동한다`() {
        assertEquals(120.dp, resolveSavedOverlayPhoneDragOffset(120.dp, isDragging = true))
        assertEquals(0.dp, resolveSavedOverlayPhoneDragOffset((-40).dp, isDragging = true))
        assertEquals(0.dp, resolveSavedOverlayPhoneDragOffset(120.dp, isDragging = false))
    }

    @Test
    fun `설정 폰 오버레이는 위로 드래그하면 iOS처럼 90퍼센트로 확장한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = -51f,
            currentSheetHeightFraction = 0.3f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.9f, result.sheetHeightFraction, 0f)
        assertEquals(false, result.shouldDismiss)
    }

    @Test
    fun `설정 폰 오버레이는 아래로 크게 드래그하면 닫고 다음 높이를 초기화한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = 101f,
            currentSheetHeightFraction = 0.9f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.3f, result.sheetHeightFraction, 0f)
        assertEquals(true, result.shouldDismiss)
    }

    @Test
    fun `설정 폰 오버레이는 확장 상태에서 아래로 조금 드래그하면 기본 높이로 축소한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = 20f,
            currentSheetHeightFraction = 0.9f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.3f, result.sheetHeightFraction, 0f)
        assertEquals(false, result.shouldDismiss)
    }

    @Test
    fun `설정 폰 오버레이는 iOS처럼 임계값 미만 드래그는 현재 높이를 유지한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = -20f,
            currentSheetHeightFraction = 0.3f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.3f, result.sheetHeightFraction, 0f)
        assertEquals(false, result.shouldDismiss)
    }

    @Test
    fun `저장과 설정 오버레이 드래그 핸들은 iOS MainTabView 토큰을 따른다`() {
        assertEquals(0xFFFFFFFF.toInt(), OverlayHeaderBackgroundColor.toArgb())
        assertEquals(0xFFC7C7CC.toInt(), OverlayHandleColor.toArgb())
        assertEquals(40.dp, OverlayDragHandleWidth)
        assertEquals(5.dp, OverlayDragHandleHeight)
        assertEquals(2.5.dp, OverlayDragHandleCornerRadius)
        assertEquals(12.dp, OverlayDragHandleTopPadding)
        assertEquals(8.dp, OverlayDragHandleBottomPadding)
        assertEquals(16.dp, SavedOverlayHeaderHorizontalPadding)
        assertEquals(4.dp, SavedOverlayHeaderBottomPaddingTablet)
        assertEquals(16.dp, SavedOverlayHeaderBottomPaddingPhone)
        assertEquals(4.dp, resolveSavedOverlayHeaderBottomPadding(isTablet = true))
        assertEquals(16.dp, resolveSavedOverlayHeaderBottomPadding(isTablet = false))
        assertEquals(8.dp, SavedOverlayListTopSpacing)
        assertEquals(8.dp, SavedOverlaySortChipCornerRadius)
        assertEquals(8.dp, SavedOverlaySortChipHorizontalPadding)
        assertEquals(4.dp, SavedOverlaySortChipVerticalPadding)
        assertEquals(4.dp, SavedOverlaySortChipContentSpacing)
        assertEquals(0.1f, SavedOverlaySortChipBackgroundAlpha, 0f)
        assertEquals(12.sp, SavedOverlaySortChipPhoneTextSize)
        assertEquals(11.sp, SavedOverlaySortChipTabletTextSize)
        assertEquals(12.dp, SavedOverlaySortChipPhoneIconSize)
        assertEquals(11.dp, SavedOverlaySortChipTabletIconSize)
        assertEquals(12.sp, resolveSavedOverlaySortChipTextSize(isTablet = false))
        assertEquals(11.sp, resolveSavedOverlaySortChipTextSize(isTablet = true))
        assertEquals(12.dp, resolveSavedOverlaySortChipIconSize(isTablet = false))
        assertEquals(11.dp, resolveSavedOverlaySortChipIconSize(isTablet = true))
        assertEquals(16.dp, SettingsOverlayHeaderHorizontalPadding)
        assertEquals(4.dp, SettingsOverlayHeaderBottomPadding)
    }

    @Test
    fun `저장 목록 정렬 기준 칩 아이콘은 iOS처럼 옵션과 무관하게 고정된다`() {
        assertEquals(Icons.Default.SwapVert, resolveSavedSortOptionIcon(SortOption.TITLE))
        assertEquals(Icons.Default.SwapVert, resolveSavedSortOptionIcon(SortOption.DATE_CREATED))
        assertEquals(Icons.Default.SwapVert, resolveSavedSortOptionIcon(SortOption.FLIGHT_START))
        assertEquals(Icons.Default.SwapVert, resolveSavedSortOptionIcon(SortOption.FLIGHT_END))
    }

    @Test
    fun `저장 목록 정렬 방향 칩 아이콘은 iOS SortDirection icon 분기를 따른다`() {
        assertEquals(Icons.Default.ArrowDownward, resolveSavedSortDirectionIcon(SortDirection.ASCENDING))
        assertEquals(Icons.Default.ArrowUpward, resolveSavedSortDirectionIcon(SortDirection.DESCENDING))
    }

    @Test
    fun `포그라운드 알림 팝업 치수와 전환값은 iOS PushNotificationPopupView 를 따른다`() {
        assertEquals(320.dp, PhoneNotificationPopupMaxWidth)
        assertEquals(400.dp, TabletNotificationPopupMaxWidth)
        assertEquals(300.dp, resolveNotificationPopupWidth(screenWidth = 300.dp, isTablet = false))
        assertEquals(320.dp, resolveNotificationPopupWidth(screenWidth = 360.dp, isTablet = false))
        assertEquals(400.dp, resolveNotificationPopupWidth(screenWidth = 900.dp, isTablet = true))
        assertEquals(24.dp, NotificationPopupContentPadding)
        assertEquals(20.dp, NotificationPopupSpacing)
        assertEquals(8.dp, NotificationPopupIconTopPadding)
        assertEquals(4.dp, NotificationPopupButtonTopPadding)
        assertEquals(14.dp, NotificationPopupButtonVerticalPadding)
        assertEquals(20.dp, NotificationPopupCornerRadius)
        assertEquals(12.dp, NotificationPopupButtonCornerRadius)
        assertEquals(0.4f, NotificationPopupDimAlpha, 0f)
        assertEquals(20.dp, NotificationPopupShadowElevation)
        assertEquals(17.sp, NotificationPopupTitleTextSize)
        assertEquals(17.sp, NotificationPopupBodyTextSize)
        assertEquals(17.sp, NotificationPopupButtonTextSize)
        assertEquals(250, NotificationPopupAnimationDurationMs)
        assertEquals(0.9f, NotificationPopupInitialScale)
    }
}
