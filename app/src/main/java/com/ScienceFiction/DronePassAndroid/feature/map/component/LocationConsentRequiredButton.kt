package com.ScienceFiction.DronePassAndroid.feature.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.map.MapBottomContentPadding

/**
 * 네이버 SDK 현재 위치 버튼(`navermap_map_controls_view`)과 같은 자리·모양: 왼쪽 10dp, 지도 콘텐츠 하단
 * 패딩 위 42dp, 52dp 아이콘. SDK 버튼은 위치 소스가 있어야 동작하므로, 위치 동의가 없을 때는 SDK 버튼을
 * 끄고 이 버튼을 둔다. 누를 때만 안내를 띄운다(반복 팝업 없음).
 */
internal val LocationConsentButtonStartPadding = 10.dp
internal val LocationConsentButtonBottomPadding = MapBottomContentPadding + 42.dp
internal val LocationConsentButtonSize = 52.dp

@Composable
internal fun LocationConsentRequiredButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Image(
        painter = painterResource(
            if (pressed) {
                com.naver.maps.map.R.drawable.navermap_location_none_pressed
            } else {
                com.naver.maps.map.R.drawable.navermap_location_none_normal
            },
        ),
        contentDescription = stringResource(R.string.location_current_location_button),
        modifier = modifier
            .padding(start = LocationConsentButtonStartPadding, bottom = LocationConsentButtonBottomPadding)
            .size(LocationConsentButtonSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    )
}
