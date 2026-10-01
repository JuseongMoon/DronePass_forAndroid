package com.ScienceFiction.DronePassAndroid.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.document.LocationTermsScreen
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

private val ConsentHorizontalPadding = 20.dp
private val ConsentButtonHeight = 50.dp

/**
 * 위치정보 이용 동의(선택) 화면. 전체 화면이고 바깥 탭·뒤로 가기로 닫히지 않는다(두 버튼 중 하나로만 끝난다).
 * 만 14세 확인 체크박스는 미리 선택하지 않고, 두 버튼은 같은 크기·같은 강조로 둔다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationConsentScreen(
    onAgree: (ageConfirmed: Boolean) -> Unit,
    onDecline: () -> Unit,
) {
    var ageConfirmed by rememberSaveable { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ConsentHorizontalPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.location_consent_title),
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosSystemGroupedBackground)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ConsentBodyText(stringResource(R.string.location_consent_body_intro))
                    ConsentBodyText(stringResource(R.string.location_consent_item_map))
                    ConsentBodyText(stringResource(R.string.location_consent_item_weather))
                }
                ConsentBodyText(stringResource(R.string.location_consent_body_storage))
                ConsentBodyText(stringResource(R.string.location_consent_body_optional), color = IosSecondaryLabel)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .toggleable(
                            value = ageConfirmed,
                            role = Role.Checkbox,
                            onValueChange = { ageConfirmed = it },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = ageConfirmed, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.location_consent_age),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                TextButton(onClick = { showTerms = true }) {
                    Text(stringResource(R.string.location_consent_view_terms), fontSize = 15.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ConsentHorizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = { onAgree(ageConfirmed) },
                    enabled = ageConfirmed,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ConsentButtonHeight),
                ) {
                    Text(stringResource(R.string.location_consent_agree), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                FilledTonalButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ConsentButtonHeight),
                ) {
                    Text(stringResource(R.string.location_consent_decline), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showTerms) {
            DronePassModalBottomSheet(
                onDismissRequest = { showTerms = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                LocationTermsScreen(onDismiss = { showTerms = false })
            }
        }
    }
}

@Composable
private fun ConsentBodyText(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Text(text = text, fontSize = 16.sp, lineHeight = 23.sp, color = color)
}
