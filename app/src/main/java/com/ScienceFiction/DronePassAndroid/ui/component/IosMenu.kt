package com.ScienceFiction.DronePassAndroid.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSeparator

internal val IosMenuMinWidth = 250.dp
internal val IosMenuCornerRadius = 13.dp
internal val IosMenuBackground = Color(0xFFF7F7F7)

/**
 * iOS `Menu` 모양의 드롭다운: 넓은 폭, 둥근 모서리, 항목 사이 구분선([IosMenuDivider]).
 * Material 기본 메뉴는 폭이 좁고 구분선이 없어 iOS 와 톤이 달라 보인다.
 */
@Composable
fun IosDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    // DropdownMenuItem 은 typography.labelLarge(14sp Medium)를 쓴다. iOS 메뉴 항목은 17pt 보통이라 이 메뉴 안에서만 바꾼다.
    val typography = MaterialTheme.typography
    MaterialTheme(typography = typography.copy(labelLarge = typography.bodyLarge)) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.widthIn(min = IosMenuMinWidth),
            offset = offset,
            shape = RoundedCornerShape(IosMenuCornerRadius),
            containerColor = IosMenuBackground,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            content = content,
        )
    }
}

/** iOS 메뉴 항목 사이 구분선. */
@Composable
fun IosMenuDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = IosSeparator)
}
