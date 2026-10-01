package com.ScienceFiction.DronePassAndroid.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.openUriSafely
import com.ScienceFiction.DronePassAndroid.feature.document.LocationTermsScreen
import com.ScienceFiction.DronePassAndroid.feature.document.PrivacyPolicyScreen
import com.ScienceFiction.DronePassAndroid.feature.document.TermsOfServiceScreen
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRow
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRowHorizontalPadding
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleContentGap
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleHeader
import com.ScienceFiction.DronePassAndroid.ui.theme.IosLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

/** 공정거래위원회 사업자정보 공개 페이지(사업자등록번호 839-86-03068). */
internal const val BusinessInfoKftcUrl = "https://www.ftc.go.kr/bizCommPop.do?wrkr_no=8398603068"
internal const val BusinessInfoEmail = "support@sciencefiction.co.kr"
internal const val BusinessInfoRegistrationNumber = "839-86-03068"

private enum class BusinessInfoDocument { TERMS, PRIVACY, LOCATION_TERMS }

/**
 * 사업자 정보(전자상거래법 제10조·제13조①). 설정 첫 단계와 결제 화면의 판매자 줄에서 연다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessInfoScreen(onClose: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var document by remember { mutableStateOf<BusinessInfoDocument?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(
            title = stringResource(R.string.business_info_title),
            closeText = stringResource(R.string.common_close),
            onClose = onClose,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = SheetLargeTitleContentGap, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(InsetGroupedSectionSpacing),
        ) {
            InsetGroupedSection {
                BusinessInfoRow(stringResource(R.string.business_info_company_label), stringResource(R.string.business_info_company))
                InsetGroupedDivider()
                BusinessInfoRow(stringResource(R.string.business_info_representative_label), stringResource(R.string.business_info_representative))
                InsetGroupedDivider()
                BusinessInfoRow(stringResource(R.string.business_info_address_label), stringResource(R.string.business_info_address))
                InsetGroupedDivider()
                BusinessInfoRow(stringResource(R.string.business_info_phone_label), stringResource(R.string.business_info_phone))
                InsetGroupedDivider()
                BusinessInfoRow(stringResource(R.string.business_info_email_label), BusinessInfoEmail)
                InsetGroupedDivider()
                BusinessInfoRow(stringResource(R.string.business_info_registration_label), BusinessInfoRegistrationNumber)
                InsetGroupedDivider()
                BusinessInfoRow(stringResource(R.string.business_info_ecommerce_label), stringResource(R.string.business_info_ecommerce))
                InsetGroupedDivider()
                BusinessInfoRow(stringResource(R.string.business_info_lbs_label), stringResource(R.string.business_info_lbs))
            }

            InsetGroupedSection {
                InsetGroupedRow(
                    title = stringResource(R.string.business_info_verify),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = { openUriSafely(uriHandler, BusinessInfoKftcUrl) },
                )
            }

            InsetGroupedSection(header = stringResource(R.string.profile_section_terms)) {
                InsetGroupedRow(
                    title = stringResource(R.string.profile_terms_service),
                    titleColor = MaterialTheme.colorScheme.primary,
                    showChevron = true,
                    onClick = { document = BusinessInfoDocument.TERMS },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.profile_terms_privacy),
                    titleColor = MaterialTheme.colorScheme.primary,
                    showChevron = true,
                    onClick = { document = BusinessInfoDocument.PRIVACY },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.profile_terms_location),
                    titleColor = MaterialTheme.colorScheme.primary,
                    showChevron = true,
                    onClick = { document = BusinessInfoDocument.LOCATION_TERMS },
                )
            }
        }
    }

    document?.let { target ->
        DronePassModalBottomSheet(
            onDismissRequest = { document = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            when (target) {
                BusinessInfoDocument.TERMS -> TermsOfServiceScreen(onDismiss = { document = null })
                BusinessInfoDocument.PRIVACY -> PrivacyPolicyScreen(onDismiss = { document = null })
                BusinessInfoDocument.LOCATION_TERMS -> LocationTermsScreen(onDismiss = { document = null })
            }
        }
    }
}

/** 항목 이름(작게) 아래 값. 주소처럼 긴 값도 줄바꿈해 모두 보이고, 길게 눌러 복사할 수 있다. */
@Composable
private fun BusinessInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InsetGroupedRowHorizontalPadding, vertical = 10.dp),
    ) {
        Text(text = label, fontSize = 13.sp, lineHeight = 18.sp, color = IosSecondaryLabel)
        SelectionContainer {
            Text(text = value, fontSize = 17.sp, lineHeight = 22.sp, color = IosLabel)
        }
    }
}
