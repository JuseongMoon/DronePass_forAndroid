package com.ScienceFiction.DronePassAndroid.subscription

import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.draw.rotate
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.document.PrivacyPolicyScreen
import com.ScienceFiction.DronePassAndroid.feature.document.TermsOfServiceScreen
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGreen
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

private val PaywallHorizontalPadding = 20.dp
private val PaywallCardCornerRadius = 12.dp
private val PaywallCtaHeight = 54.dp
private val PaywallHeroSize = 64.dp
private val PaywallStarYellow = Color(0xFFFFCC00)
private val PaywallLimitBannerColor = Color(0xFFFF9500).copy(alpha = 0.12f)

/**
 * iOS PaywallView 구성: 닫기 → 히어로 → (한도 안내) → 혜택 카드 → 가격 → 구독 버튼 → 구매 복원 → 법적 고지.
 */
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
    val limits by manager.limits.collectAsStateWithLifecycle()
    var document by remember { mutableStateOf<String?>(null) }
    val isPro = status.entitlement == EntitlementState.PRO

    DronePassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = PaywallHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
            }

            PaywallHero()
            Spacer(Modifier.height(24.dp))

            if (request.updateRequired) {
                PaywallBanner(stringResource(R.string.subscription_update_required))
            } else {
                if (status.legacyKind == LegacyKind.EARLY_ACCESS) {
                    EarlyAccessCard()
                    Spacer(Modifier.height(16.dp))
                }
                request.limitKind?.let { kind ->
                    PaywallBanner(
                        text = when (kind) {
                            QuotaLimitKind.SHAPES -> stringResource(R.string.subscription_limit_shapes, limits.freeShapes)
                            QuotaLimitKind.SKETCHES -> stringResource(R.string.subscription_limit_sketches, limits.freeSketches)
                            QuotaLimitKind.DRONES -> stringResource(R.string.subscription_limit_drones, limits.freeDrones)
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (status.paymentIssue) {
                    PaywallBanner(
                        text = stringResource(R.string.subscription_payment_issue),
                        background = MaterialTheme.colorScheme.errorContainer,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                BenefitsCard(limits)
                Spacer(Modifier.height(24.dp))

                if (isPro) {
                    ProStatusLabel()
                    if (status.isPaidSubscriber) {
                        TextButton(onClick = { manager.openManagement(activity) }) {
                            Text(stringResource(R.string.subscription_manage))
                        }
                    }
                    if (status.isPaidSubscriber && status.legacyKind != null) {
                        FinePrint(stringResource(R.string.subscription_legacy_subscribed))
                    }
                } else if (!status.paymentIssue) {
                    Text(
                        text = stringResource(R.string.subscription_product_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val currentPrice = price
                    if (currentPrice != null) {
                        Text(
                            text = stringResource(R.string.subscription_paywall_price, currentPrice),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    } else {
                        FinePrint(stringResource(R.string.subscription_product_unavailable))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { manager.purchase(activity, request.source) },
                        enabled = currentPrice != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PaywallCtaHeight),
                    ) {
                        Text(
                            text = stringResource(R.string.subscription_subscribe),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    TextButton(onClick = manager::restore) {
                        Text(stringResource(R.string.subscription_restore), fontSize = 16.sp)
                    }
                    FinePrint(stringResource(R.string.subscription_paywall_renewal))
                }
                Spacer(Modifier.height(8.dp))
                FinePrint(stringResource(R.string.subscription_cross_platform_notice))
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
            ) {
                TextButton(onClick = { document = "terms" }) {
                    Text(stringResource(R.string.subscription_terms), fontSize = 13.sp)
                }
                TextButton(onClick = { document = "privacy" }) {
                    Text(stringResource(R.string.subscription_privacy), fontSize = 13.sp)
                }
            }
        }
    }
    when (document) {
        "terms" -> DronePassModalBottomSheet(onDismissRequest = { document = null }) { TermsOfServiceScreen(onDismiss = { document = null }) }
        "privacy" -> DronePassModalBottomSheet(onDismissRequest = { document = null }) { PrivacyPolicyScreen(onDismiss = { document = null }) }
    }
}

@Composable
private fun PaywallHero() {
    Box(
        modifier = Modifier
            .size(PaywallHeroSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = PaywallStarYellow,
            modifier = Modifier.size(40.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.subscription_pro),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(R.string.subscription_paywall_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = IosSecondaryLabel,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun PaywallBanner(
    text: String,
    background: Color = PaywallLimitBannerColor,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaywallCardCornerRadius))
            .background(background)
            .padding(16.dp),
    )
}

@Composable
private fun BenefitsCard(limits: QuotaLimits) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaywallCardCornerRadius))
            .background(IosSystemGroupedBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BenefitRow(ImageVector.vectorResource(R.drawable.ic_circle_dashed_ios_like), stringResource(R.string.subscription_benefit_shapes, limits.freeShapes))
        BenefitRow(Icons.Default.Gesture, stringResource(R.string.subscription_benefit_sketches, limits.freeSketches))
        BenefitRow(Icons.Default.Flight, stringResource(R.string.subscription_benefit_drones, limits.freeDrones), iconRotation = 90f)
        BenefitRow(Icons.Outlined.VerifiedUser, stringResource(R.string.subscription_benefit_keep_data))
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, text: String, iconRotation: Float = 0f) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .rotate(iconRotation),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** iOS EarlyAccessBadge: 주황→분홍 그라데이션 캡슐. 설정의 플랜 행과 Pro 혜택 카드가 함께 쓴다. */
@Composable
internal fun EarlyAccessBadge() {
    Text(
        text = stringResource(R.string.subscription_early_badge),
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.8.sp,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFF2D55))))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun EarlyAccessCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF9500).copy(alpha = 0.10f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EarlyAccessBadge()
        Text(stringResource(R.string.subscription_early_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.subscription_early_message), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.subscription_early_lifetime), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProStatusLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IosSystemGreen, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.subscription_pro),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = IosSystemGreen,
        )
    }
}

@Composable
private fun FinePrint(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = IosSecondaryLabel,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp)
            .padding(top = 4.dp),
    )
}
