package com.ScienceFiction.DronePassAndroid.subscription

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.util.openMailDraft
import com.ScienceFiction.DronePassAndroid.core.util.openUriSafely
import com.ScienceFiction.DronePassAndroid.feature.document.PrivacyPolicyScreen
import com.ScienceFiction.DronePassAndroid.feature.document.TermsOfServiceScreen
import com.ScienceFiction.DronePassAndroid.feature.legal.BusinessInfoKftcUrl
import com.ScienceFiction.DronePassAndroid.feature.legal.BusinessInfoScreen
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun formatContractDate(millis: Long, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))

/**
 * 판매자 정보 텍스트와 [사업자 정보] [사업자정보 확인(공정위)] 링크(사양 v2 C-4). 링크만으로는 부족해 본문을 함께 쓴다.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SellerInformation(
    onBusinessInfo: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.subscription_seller),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = IosSecondaryLabel,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = if (textAlign == TextAlign.Center) {
                Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterHorizontally)
            } else {
                Arrangement.spacedBy(16.dp)
            },
        ) {
            SellerLink(stringResource(R.string.business_info_title), onBusinessInfo)
            SellerLink(stringResource(R.string.subscription_seller_verify)) {
                openUriSafely(uriHandler, BusinessInfoKftcUrl)
            }
        }
    }
}

@Composable
private fun SellerLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

private enum class ContractDocument { TERMS, PRIVACY, BUSINESS }

/**
 * 계약 내용(사양 v2 C-5). 구매 직후 시트로 띄우고, 설정 > 구독 관리에서 다시 볼 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionContractScreen(
    manager: SubscriptionManager,
    onClose: () -> Unit,
) {
    val purchase by manager.activePurchase.collectAsStateWithLifecycle()
    val price by manager.productPrice.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    var document by remember { mutableStateOf<ContractDocument?>(null) }
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(
            title = stringResource(R.string.subscription_contract_title),
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
                ContractRow(stringResource(R.string.subscription_contract_product), stringResource(R.string.subscription_product_name))
                InsetGroupedDivider()
                ContractRow(
                    stringResource(R.string.subscription_contract_price),
                    price?.let { stringResource(R.string.subscription_paywall_price, it) } ?: "-",
                )
                InsetGroupedDivider()
                ContractRow(stringResource(R.string.subscription_contract_period), stringResource(R.string.subscription_contract_period_value))
                InsetGroupedDivider()
                ContractRow(
                    stringResource(R.string.subscription_contract_start),
                    purchase?.let { formatContractDate(it.purchaseTimeMillis, locale, zone) } ?: "-",
                )
                InsetGroupedDivider()
                ContractRow(
                    stringResource(R.string.subscription_contract_next_renewal),
                    when {
                        purchase == null -> "-"
                        purchase?.autoRenewing == false -> stringResource(R.string.subscription_contract_not_renewing)
                        else -> formatContractDate(nextYearlyRenewalMillis(purchase!!.purchaseTimeMillis, now, zone), locale, zone)
                    },
                )
            }
            InsetGroupedSection(header = stringResource(R.string.subscription_contract_renewal)) {
                ContractParagraph(stringResource(R.string.subscription_paywall_renewal))
            }
            InsetGroupedSection(header = stringResource(R.string.subscription_contract_refund)) {
                ContractParagraph(stringResource(R.string.subscription_refund_notice))
            }
            InsetGroupedSection(header = stringResource(R.string.subscription_contract_seller)) {
                SellerInformation(
                    onBusinessInfo = { document = ContractDocument.BUSINESS },
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = InsetGroupedRowHorizontalPadding, vertical = 10.dp),
                )
            }
            InsetGroupedSection {
                InsetGroupedRow(
                    title = stringResource(R.string.profile_terms_service),
                    titleColor = MaterialTheme.colorScheme.primary,
                    showChevron = true,
                    onClick = { document = ContractDocument.TERMS },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.profile_terms_privacy),
                    titleColor = MaterialTheme.colorScheme.primary,
                    showChevron = true,
                    onClick = { document = ContractDocument.PRIVACY },
                )
            }
        }
    }

    when (document) {
        ContractDocument.TERMS -> DronePassModalBottomSheet(onDismissRequest = { document = null }) {
            TermsOfServiceScreen(onDismiss = { document = null })
        }
        ContractDocument.PRIVACY -> DronePassModalBottomSheet(onDismissRequest = { document = null }) {
            PrivacyPolicyScreen(onDismiss = { document = null })
        }
        ContractDocument.BUSINESS -> DronePassModalBottomSheet(
            onDismissRequest = { document = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = IosSystemGroupedBackground,
            dragHandle = null,
        ) {
            BusinessInfoScreen(onClose = { document = null })
        }
        null -> Unit
    }
}

/**
 * 설정 > 구독 관리 > 해지·환불 요청(사양 v2 C-6). 환불 기준을 보여 주고, 회사에 메일로 요청하거나
 * Google Play 에서 자동 갱신을 해지한다.
 */
@Composable
fun SubscriptionCancelRefundScreen(
    manager: SubscriptionManager,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val purchase by manager.activePurchase.collectAsStateWithLifecycle()
    val subject = stringResource(R.string.subscription_cancel_refund_mail_subject)
    val mailUnavailable = stringResource(R.string.subscription_mail_unavailable)
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val requestType = purchase?.let {
        refundRequestType(lastYearlyPaymentMillis(it.purchaseTimeMillis, now, zone), now)
    }
    val body = stringResource(
        R.string.subscription_cancel_refund_mail_body,
        purchase?.orderId ?: "-",
        purchase?.let { Instant.ofEpochMilli(it.purchaseTimeMillis).atZone(zone).toLocalDate().toString() } ?: "-",
        when (requestType) {
            RefundRequestType.FULL_WITHIN_7_DAYS -> stringResource(R.string.subscription_refund_type_full)
            RefundRequestType.PRORATED -> stringResource(R.string.subscription_refund_type_prorated)
            null -> "-"
        },
        manager.accountEmail ?: "-",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(
            title = stringResource(R.string.subscription_cancel_refund),
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
            InsetGroupedSection(header = stringResource(R.string.subscription_contract_refund)) {
                ContractParagraph(stringResource(R.string.subscription_refund_notice))
            }
            InsetGroupedSection {
                InsetGroupedRow(
                    title = stringResource(R.string.subscription_cancel_refund_company),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        if (!openMailDraft(context, SupportEmail, subject, body)) {
                            Toast.makeText(context, mailUnavailable, Toast.LENGTH_LONG).show()
                        }
                    },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.subscription_cancel_refund_play),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = { manager.openManagement(context) },
                )
            }
        }
    }
}

private enum class ManageDestination { CONTRACT, CANCEL_REFUND }

/** 설정 > 구독 관리: 계약 내용, 해지·환불 요청, Google Play 구독 관리. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionManageScreen(
    manager: SubscriptionManager,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var destination by remember { mutableStateOf<ManageDestination?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(
            title = stringResource(R.string.subscription_manage),
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
                InsetGroupedRow(
                    title = stringResource(R.string.subscription_contract_title),
                    showChevron = true,
                    onClick = { destination = ManageDestination.CONTRACT },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.subscription_cancel_refund),
                    showChevron = true,
                    onClick = { destination = ManageDestination.CANCEL_REFUND },
                )
                InsetGroupedDivider()
                InsetGroupedRow(
                    title = stringResource(R.string.subscription_manage_in_play),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = { manager.openManagement(context) },
                )
            }
        }
    }

    destination?.let { target ->
        DronePassModalBottomSheet(
            onDismissRequest = { destination = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = IosSystemGroupedBackground,
            dragHandle = null,
        ) {
            when (target) {
                ManageDestination.CONTRACT -> SubscriptionContractScreen(manager, onClose = { destination = null })
                ManageDestination.CANCEL_REFUND -> SubscriptionCancelRefundScreen(manager, onClose = { destination = null })
            }
        }
    }
}

@Composable
private fun ContractRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InsetGroupedRowHorizontalPadding, vertical = 10.dp),
    ) {
        Text(text = label, fontSize = 13.sp, lineHeight = 18.sp, color = IosSecondaryLabel)
        Text(text = value, fontSize = 17.sp, lineHeight = 22.sp, color = IosLabel)
    }
}

@Composable
private fun ContractParagraph(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = IosLabel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InsetGroupedRowHorizontalPadding, vertical = 12.dp),
    )
}
