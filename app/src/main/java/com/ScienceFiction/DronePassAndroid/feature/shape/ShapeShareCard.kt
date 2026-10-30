package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.parseIosOpaqueRgbHexColor

/** 세로 4:5. 3배로 렌더링하면 1080×1350px (iOS ShapeShareCardLayout 정합) */
internal val ShapeShareCardWidth = 360.dp
internal val ShapeShareCardHeight = 450.dp
internal val ShapeShareCardMapHeight = 270.dp
internal const val ShapeShareCardRenderScale = 3f

// 다크 모드와 상관없이 카드는 항상 밝은 색으로 그린다
private val CardPrimaryText = Color(0xFF1C1C1F)
private val CardSecondaryText = Color(0xFF6B6B73)
private val CardBorder = Color(0xFFE0E0E6)
private val CardCornerRadius = 20.dp
private val CardShape = RoundedCornerShape(CardCornerRadius)
private val CardIosSystemGray = Color(0xFF8E8E93)

/**
 * 공유 카드 레이아웃 (위: 지도, 아래: 도형 정보, 우하단: 앱 로고).
 * 미리보기에서는 실시간 지도를, 이미지 렌더링에서는 지도 스냅샷을 [map] 칸에 넣는다.
 * 글자 크기는 시스템 글꼴 배율과 상관없이 고정이어야 하므로 호출부에서 fontScale 1 인 Density 로 감싼다.
 */
@Composable
internal fun ShapeShareCard(
    content: ShapeShareCardContent,
    modifier: Modifier = Modifier,
    map: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .size(ShapeShareCardWidth, ShapeShareCardHeight)
            .clip(CardShape)
            .background(Color.White)
            .border(1.dp, CardBorder, CardShape),
    ) {
        Box(
            modifier = Modifier
                .size(ShapeShareCardWidth, ShapeShareCardMapHeight)
                .clipToBounds(),
        ) {
            map()
        }
        ShapeShareCardInfo(content)
    }
}

@Composable
private fun ShapeShareCardInfo(content: ShapeShareCardContent) {
    Column(
        modifier = Modifier
            .size(ShapeShareCardWidth, ShapeShareCardHeight - ShapeShareCardMapHeight)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = content.title,
            modifier = Modifier.padding(bottom = 2.dp),
            style = TextStyle(color = CardPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 18.sp * 0.7f, maxFontSize = 18.sp),
        )

        content.address?.let { address ->
            ShapeShareCardInfoRow(
                icon = rememberVectorPainter(Icons.Outlined.Place),
                text = address,
                maxLines = 2,
            )
        }

        content.period?.let { period ->
            ShapeShareCardInfoRow(
                icon = rememberVectorPainter(Icons.Outlined.CalendarToday),
                text = period,
                maxLines = 1,
            )
        }

        ShapeShareCardSpecRow(content)

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            content.coordinate?.let { coordinate ->
                Box(modifier = Modifier.width(16.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Outlined.NearMe),
                        contentDescription = null,
                        tint = CardSecondaryText,
                        modifier = Modifier.size(11.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // iOS monospacedDigit: 숫자만 고정폭(tnum)이고 기호·공백은 기본 글꼴 폭이다
                ShapeShareCardText(
                    text = coordinate,
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
            } ?: Spacer(modifier = Modifier.weight(1f))
            ShapeShareCardBrandMark()
        }
    }
}

/** 반경 · 고도 · 드론 · 태그를 한 줄에 둔다. 값이 없는 항목은 뺀다 */
@Composable
private fun ShapeShareCardSpecRow(content: ShapeShareCardContent) {
    if (content.radius == null && content.height == null && content.droneName == null && content.tagName == null) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content.radius?.let { radius ->
            ShapeShareCardSpecItem(
                icon = painterResource(R.drawable.ic_circle_dashed_ios_like),
                text = stringResource(R.string.shape_share_card_radius, radius),
            )
        }
        content.height?.let { height ->
            ShapeShareCardSpecItem(
                icon = rememberVectorPainter(Icons.Filled.VerticalAlignTop),
                text = stringResource(R.string.shape_share_card_height, height),
            )
        }
        content.droneName?.let { droneName ->
            val droneColor = content.droneColorHex
                ?.let(::parseIosOpaqueRgbHexColor)
                ?.let(::Color)
                ?: CardIosSystemGray
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(droneColor),
                )
                ShapeShareCardText(text = droneName, fontSize = 13.sp)
            }
        }
        content.tagName?.let { tagName ->
            ShapeShareCardSpecItem(
                icon = rememberVectorPainter(Icons.Outlined.Sell),
                text = tagName,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun ShapeShareCardSpecItem(icon: Painter, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = icon, contentDescription = null, tint = CardSecondaryText, modifier = Modifier.size(13.dp))
        ShapeShareCardText(text = text, fontSize = 13.sp)
    }
}

@Composable
private fun ShapeShareCardInfoRow(icon: Painter, text: String, maxLines: Int) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = icon, contentDescription = null, tint = CardSecondaryText, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        ShapeShareCardText(text = text, fontSize = 13.sp, maxLines = maxLines)
    }
}

/** iOS minimumScaleFactor(0.8) 처럼 넘치면 글자를 줄이고, 그래도 넘치면 말줄임한다 */
@Composable
private fun ShapeShareCardText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    fontFeatureSettings: String? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(color = CardSecondaryText, fontSize = fontSize, fontFeatureSettings = fontFeatureSettings),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = fontSize * 0.8f, maxFontSize = fontSize),
    )
}

@Composable
private fun ShapeShareCardBrandMark() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.login_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        BasicText(
            text = "DronePass",
            style = TextStyle(color = CardPrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}
