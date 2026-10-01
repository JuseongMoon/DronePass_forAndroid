package com.ScienceFiction.DronePassAndroid.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.legal.TERMS_NOTICE_VERSION
import com.ScienceFiction.DronePassAndroid.core.legal.formatLegalDate
import com.ScienceFiction.DronePassAndroid.feature.document.PrivacyPolicyScreen
import com.ScienceFiction.DronePassAndroid.feature.document.TermsOfServiceScreen
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet

private enum class TermsNoticeDocument { TERMS, PRIVACY }

/**
 * 3.6.0 이용약관·개인정보 처리방침 개정 안내(사양 §6). 업데이트한 기존 설치에 한 번만 띄우고 "확인"으로만 닫는다.
 * "보기" 버튼은 앱 안의 기존 약관 화면을 연다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsNoticeDialog(onConfirm: () -> Unit) {
    var document by remember { mutableStateOf<TermsNoticeDocument?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val effectiveDate = formatLegalDate(TERMS_NOTICE_VERSION, locale)

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.terms_notice_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.terms_notice_effective, effectiveDate))
                Text(stringResource(R.string.terms_notice_item_pro))
                Text(stringResource(R.string.terms_notice_item_free))
                Text(stringResource(R.string.terms_notice_item_existing))
                Text(stringResource(R.string.terms_notice_item_privacy))
                Text(stringResource(R.string.terms_notice_disagree))
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                TextButton(onClick = { document = TermsNoticeDocument.TERMS }) {
                    Text(stringResource(R.string.terms_notice_view_terms))
                }
                TextButton(onClick = { document = TermsNoticeDocument.PRIVACY }) {
                    Text(stringResource(R.string.terms_notice_view_privacy))
                }
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        },
    )

    document?.let { target ->
        DronePassModalBottomSheet(
            onDismissRequest = { document = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            when (target) {
                TermsNoticeDocument.TERMS -> TermsOfServiceScreen(onDismiss = { document = null })
                TermsNoticeDocument.PRIVACY -> PrivacyPolicyScreen(onDismiss = { document = null })
            }
        }
    }
}
