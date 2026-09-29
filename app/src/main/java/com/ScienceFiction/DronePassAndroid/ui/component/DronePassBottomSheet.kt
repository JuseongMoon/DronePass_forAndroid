package com.ScienceFiction.DronePassAndroid.ui.component

import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGray3

/** 크게 열린 시트와 상태바 사이 간격. iOS large detent 처럼 뒤 화면이 살짝 보이게 한다. */
internal val DronePassSheetTopGap = 10.dp
internal val DronePassSheetCornerRadius = 20.dp
internal val DronePassSheetGrabberWidth = 36.dp
internal val DronePassSheetGrabberHeight = 5.dp
internal val DronePassSheetGrabberVerticalPadding = 8.dp

/**
 * 앱 공통 모달 시트.
 *
 * Material 기본 시트는 최대로 열리면 상태바 뒤까지 올라가 핸들이 시스템 아이콘과 겹친다.
 * 상태바 높이 + [DronePassSheetTopGap] 만큼 위를 비우고, iOS 크기의 작은 그래버를 쓴다.
 * 배경은 흰색이 기본이며, 회색 목록 배경이 필요한 화면은 [containerColor] 로 바꾼다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DronePassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    dragHandle: (@Composable () -> Unit)? = { DronePassSheetGrabber() },
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = DronePassSheetTopGap),
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = DronePassSheetCornerRadius,
            topEnd = DronePassSheetCornerRadius,
        ),
        containerColor = containerColor,
        dragHandle = dragHandle,
    ) {
        DisableSheetNavigationBarScrim()
        content()
    }
}

/**
 * 시트는 별도 Dialog 창이라, 3버튼 내비게이션에서 시스템이 내비게이션 바 뒤에 회색 대비 스크림을 깐다.
 * 시트 배경이 화면 끝까지 이어지도록 스크림을 끄고 밝은 배경용 버튼 색을 쓴다.
 */
@Composable
private fun DisableSheetNavigationBarScrim() {
    val view = LocalView.current
    SideEffect {
        val window = view.findDialogWindow() ?: return@SideEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
    }
}

private fun View.findDialogWindow(): Window? {
    var current: ViewParent? = parent
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent
    }
    return null
}

/**
 * 바깥 탭·아래로 끌기·뒤로가기로 닫히기 전에 [canDismiss] 로 먼저 확인하는 시트 상태.
 *
 * Material 시트는 onDismissRequest 를 무시해도 스스로 숨겨져, 화면에서는 사라졌는데
 * 열린 상태로 남는 문제가 생긴다. 닫기를 막아야 하는 시트는 이 상태를 쓴다.
 * [canDismiss] 가 false 를 돌려주면 시트는 그대로 남고 onDismissRequest 도 불리지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberGuardedSheetState(
    skipPartiallyExpanded: Boolean = true,
    canDismiss: () -> Boolean,
): SheetState {
    val latestCanDismiss by rememberUpdatedState(canDismiss)
    return rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = { target -> target != SheetValue.Hidden || latestCanDismiss() },
    )
}

@Composable
fun DronePassSheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DronePassSheetGrabberVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(DronePassSheetGrabberWidth, DronePassSheetGrabberHeight)
                .background(IosSystemGray3, RoundedCornerShape(50)),
        )
    }
}
