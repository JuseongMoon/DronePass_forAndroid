package com.ScienceFiction.DronePassAndroid.ui.navigation

import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.feature.auth.AuthState
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenStartDestinationTest {

    @Test
    fun `앱 시작 화면은 iOS처럼 로그인 상태 확인 중에도 지도이다`() {
        assertEquals(Screen.Map.route, resolveMainStartDestination(AuthState.Loading))
    }

    @Test
    fun `앱 시작 화면은 iOS처럼 로그아웃 상태에도 지도이다`() {
        assertEquals(Screen.Map.route, resolveMainStartDestination(AuthState.LoggedOut))
    }

    @Test
    fun `앱 시작 화면은 인증 오류 상태에도 지도이다`() {
        assertEquals(Screen.Map.route, resolveMainStartDestination(AuthState.Error("error")))
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
    fun `하단 탭바는 iOS처럼 스케치 모드에서 숨긴다`() {
        assertEquals(true, shouldShowFloatingTabBar(isLoginScreen = false, isSketchMode = false))
        assertEquals(false, shouldShowFloatingTabBar(isLoginScreen = false, isSketchMode = true))
        assertEquals(false, shouldShowFloatingTabBar(isLoginScreen = true, isSketchMode = false))
    }

    @Test
    fun `하단 탭바 bottom padding은 iOS처럼 phone 15dp tablet 20dp 이다`() {
        assertEquals(15.dp, TabBarPhoneBottomPadding)
        assertEquals(20.dp, TabBarTabletBottomPadding)
        assertEquals(15.dp, resolveTabBarBottomPadding(isTablet = false))
        assertEquals(20.dp, resolveTabBarBottomPadding(isTablet = true))
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
    fun `저장 목록 폰 오버레이 높이는 iOS처럼 화면 절반과 500dp 중 작은 값이다`() {
        assertEquals(400.dp, resolveSavedOverlayPhoneBaseHeight(800.dp))
        assertEquals(500.dp, resolveSavedOverlayPhoneBaseHeight(1_200.dp))
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
    fun `저장 목록 폰 오버레이는 iOS처럼 드래그 중 높이를 줄이지 않고 아래로 이동한다`() {
        assertEquals(120.dp, resolveSavedOverlayPhoneDragOffset(120.dp, isDragging = true))
        assertEquals(0.dp, resolveSavedOverlayPhoneDragOffset((-40).dp, isDragging = true))
        assertEquals(0.dp, resolveSavedOverlayPhoneDragOffset(120.dp, isDragging = false))
    }

    @Test
    fun `설정 폰 오버레이는 iOS처럼 위로 드래그하면 90퍼센트로 확장한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = -51f,
            currentSheetHeightFraction = 0.5f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.9f, result.sheetHeightFraction, 0f)
        assertEquals(false, result.shouldDismiss)
    }

    @Test
    fun `설정 폰 오버레이는 iOS처럼 아래로 크게 드래그하면 닫고 다음 높이를 초기화한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = 101f,
            currentSheetHeightFraction = 0.9f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.5f, result.sheetHeightFraction, 0f)
        assertEquals(true, result.shouldDismiss)
    }

    @Test
    fun `설정 폰 오버레이는 iOS처럼 확장 상태에서 아래로 조금 드래그하면 50퍼센트로 축소한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = 20f,
            currentSheetHeightFraction = 0.9f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.5f, result.sheetHeightFraction, 0f)
        assertEquals(false, result.shouldDismiss)
    }

    @Test
    fun `설정 폰 오버레이는 iOS처럼 임계값 미만 드래그는 현재 높이를 유지한다`() {
        val result = resolveSettingsOverlayPhoneDragEnd(
            translation = -20f,
            currentSheetHeightFraction = 0.5f,
            dismissThreshold = 100f,
            expandThreshold = 50f,
        )

        assertEquals(0.5f, result.sheetHeightFraction, 0f)
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
    fun `포그라운드 알림 팝업 치수와 전환값은 iOS PushNotificationPopupView 를 따른다`() {
        assertEquals(320.dp, PhoneNotificationPopupMaxWidth)
        assertEquals(400.dp, TabletNotificationPopupMaxWidth)
        assertEquals(24.dp, NotificationPopupContentPadding)
        assertEquals(20.dp, NotificationPopupSpacing)
        assertEquals(8.dp, NotificationPopupIconTopPadding)
        assertEquals(4.dp, NotificationPopupButtonTopPadding)
        assertEquals(14.dp, NotificationPopupButtonVerticalPadding)
        assertEquals(20.dp, NotificationPopupCornerRadius)
        assertEquals(12.dp, NotificationPopupButtonCornerRadius)
        assertEquals(0.4f, NotificationPopupDimAlpha, 0f)
        assertEquals(20.dp, NotificationPopupShadowElevation)
        assertEquals(250, NotificationPopupAnimationDurationMs)
        assertEquals(0.9f, NotificationPopupInitialScale)
    }
}
