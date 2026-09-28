package com.ScienceFiction.DronePassAndroid.subscription

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.document.PrivacyPolicyScreen
import com.ScienceFiction.DronePassAndroid.feature.document.TermsOfServiceScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPaywall(
    manager: SubscriptionManager,
    request: PaywallRequest,
    activity: Activity,
    onDismiss: () -> Unit,
) {
    val status by manager.status.collectAsStateWithLifecycle()
    val price by manager.productPrice.collectAsStateWithLifecycle()
    var document by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text(stringResource(R.string.subscription_paywall_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            if (request.updateRequired) {
                Text(stringResource(R.string.subscription_update_required))
            } else {
                if (status.legacyKind == LegacyKind.EARLY_ACCESS) {
                    Text(stringResource(R.string.subscription_early_badge), color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.subscription_early_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.subscription_early_message))
                    Text(stringResource(R.string.subscription_early_lifetime))
                }
                Text(stringResource(R.string.subscription_paywall_benefits))
                if (status.entitlement != EntitlementState.PRO && !status.paymentIssue) {
                    Spacer(Modifier.height(12.dp))
                    price?.let { Text(stringResource(R.string.subscription_paywall_price, it), style = MaterialTheme.typography.titleLarge) }
                    Text(stringResource(R.string.subscription_paywall_renewal))
                    Button(
                        onClick = { manager.purchase(activity, request.source) },
                        enabled = price != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.subscription_purchase)) }
                    TextButton(onClick = manager::restore) { Text(stringResource(R.string.subscription_restore)) }
                }
                if (status.paymentIssue) Text(stringResource(R.string.subscription_payment_issue))
                if (status.isPaidSubscriber) {
                    TextButton(onClick = { manager.openManagement(activity) }) { Text(stringResource(R.string.subscription_manage)) }
                }
                if (status.isPaidSubscriber && status.legacyKind != null) Text(stringResource(R.string.subscription_legacy_subscribed))
                Text(stringResource(R.string.subscription_cross_platform_notice), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { document = "terms" }) { Text(stringResource(R.string.subscription_terms)) }
            TextButton(onClick = { document = "privacy" }) { Text(stringResource(R.string.subscription_privacy)) }
        }
    }
    when (document) {
        "terms" -> ModalBottomSheet(onDismissRequest = { document = null }) { TermsOfServiceScreen(onDismiss = { document = null }) }
        "privacy" -> ModalBottomSheet(onDismissRequest = { document = null }) { PrivacyPolicyScreen(onDismiss = { document = null }) }
    }
}
