package com.ScienceFiction.DronePassAndroid.ui.component

import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.ui.theme.IosLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondarySystemGroupedBackground
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSeparator
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGray3
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGray4

/*
 * iOS `List(.insetGrouped)` / `Form` 톤을 Compose 로 옮긴 공통 컴포넌트.
 * 회색 배경(IosSystemGroupedBackground) 위에 흰 카드 섹션을 쌓는다.
 */

internal val InsetGroupedHorizontalMargin = 16.dp
internal val InsetGroupedCornerRadius = 12.dp
internal val InsetGroupedRowHorizontalPadding = 16.dp
internal val InsetGroupedRowValueMaxWidth = 220.dp
internal val InsetGroupedRowMinHeight = 48.dp
internal val InsetGroupedSectionSpacing = 20.dp

@Composable
fun InsetGroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                text = header,
                fontSize = 13.sp,
                color = IosSecondaryLabel,
                modifier = Modifier.padding(
                    start = InsetGroupedHorizontalMargin + InsetGroupedRowHorizontalPadding,
                    end = InsetGroupedHorizontalMargin + InsetGroupedRowHorizontalPadding,
                    bottom = 6.dp,
                ),
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = InsetGroupedHorizontalMargin)
                .fillMaxWidth()
                .clip(RoundedCornerShape(InsetGroupedCornerRadius))
                .background(IosSecondarySystemGroupedBackground),
            content = content,
        )
        if (footer != null) {
            Text(
                text = footer,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = IosSecondaryLabel,
                modifier = Modifier.padding(
                    start = InsetGroupedHorizontalMargin + InsetGroupedRowHorizontalPadding,
                    end = InsetGroupedHorizontalMargin + InsetGroupedRowHorizontalPadding,
                    top = 6.dp,
                ),
            )
        }
    }
}

/** 섹션 안 행 사이 구분선. iOS 처럼 왼쪽을 들여 쓴다. */
@Composable
fun InsetGroupedDivider(startIndent: androidx.compose.ui.unit.Dp = InsetGroupedRowHorizontalPadding) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent),
        thickness = 0.5.dp,
        color = IosSeparator,
    )
}

/**
 * 기본 행. 제목 왼쪽, 값/보조 요소 오른쪽.
 * [titleColor] 로 파란 동작 행·빨간 파괴적 행을 표현한다.
 */
@Composable
fun InsetGroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    titleColor: Color = IosLabel,
    valueColor: Color = IosSecondaryLabel,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    valueAccessory: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = InsetGroupedRowMinHeight)
            .then(
                if (enabled && onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = InsetGroupedRowHorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    // iOS 설정 행 설명은 .caption(12pt)
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = IosSecondaryLabel,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (valueAccessory != null) {
            // iOS HStack(제목, Spacer, 배지, 값) 처럼 값 바로 앞에 붙는 요소.
            Spacer(modifier = Modifier.width(12.dp))
            valueAccessory()
        }
        if (value != null) {
            Spacer(modifier = Modifier.width(if (valueAccessory != null) 6.dp else 12.dp))
            // 값은 오른쪽에 붙고, 길면 제목을 밀어 줄바꿈시키지 않도록 최대 폭에서 말줄임한다.
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = InsetGroupedRowValueMaxWidth),
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = IosSystemGray3,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun InsetGroupedToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    InsetGroupedRow(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        trailing = {
            DronePassSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

/** 테두리 없는 밝은 트랙의 스위치. Material 기본의 진한 외곽선을 걷어 iOS 톤에 맞춘다. */
@Composable
fun DronePassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            // iOS Toggle 기본 tint(시스템 초록). DronePass iOS 는 tint 를 바꾸지 않는다.
            checkedTrackColor = IosSystemGreen,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = IosSystemGray4,
            uncheckedBorderColor = Color.Transparent,
        ),
        thumbContent = {
            // 체크 여부와 관계없이 같은 크기의 흰 썸을 쓴다.
            Spacer(modifier = Modifier.size(SwitchDefaults.IconSize))
        },
    )
}

/**
 * 시트 안 화면의 iOS Large Title 헤더. Material LargeTopAppBar 는 시트 안에서 큰 빈 공간을 만들어서
 * 제목과 닫기 버튼만 가볍게 그린다.
 */
@Composable
fun SheetLargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    closeText: String? = null,
    onClose: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (onClose != null && closeText != null) {
            TextButton(onClick = onClose) {
                Text(closeText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * 시트 내비게이션 헤더 아래 hairline. iOS inline 내비게이션 바처럼 콘텐츠가 헤더 밑으로
 * 스크롤됐을 때만 나타난다(맨 위에서는 숨김). 높이는 항상 차지해 레이아웃이 흔들리지 않는다.
 */
@Composable
fun SheetHeaderDivider(
    visible: Boolean,
    modifier: Modifier = Modifier,
    thickness: Dp = 0.5.dp,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "sheet_header_divider",
    )
    HorizontalDivider(
        modifier = modifier.alpha(alpha),
        thickness = thickness,
        color = IosSeparator,
    )
}
