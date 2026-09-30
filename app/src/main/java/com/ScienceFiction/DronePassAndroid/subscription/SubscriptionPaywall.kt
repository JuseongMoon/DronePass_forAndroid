package com.ScienceFiction.DronePassAndroid.subscription

import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemOrange
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.ui.platform.LocalContext
import com.ScienceFiction.DronePassAndroid.ui.component.IosNavBarButtonFontSize
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
import androidx.compose.runtime.LaunchedEffect
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
/** iOS .callout */
private val PaywallCalloutSize = 16.sp

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
    val isLoadingProducts by manager.isLoadingProducts.collectAsStateWithLifecycle()
    val limits by manager.limits.collectAsStateWithLifecycle()
    var document by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreNotFound by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isPro = status.entitlement == EntitlementState.PRO

    // iOS PaywallView `.task { loadProducts() }`: 가격을 아직 못 받았으면 열릴 때 다시 불러온다.
    LaunchedEffect(Unit) {
        if (manager.productPrice.value == null) manager.reloadProducts()
    }

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
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), fontSize = IosNavBarButtonFontSize, fontWeight = FontWeight.Normal) }
            }

            PaywallHero()
            Spacer(Modifier.height(24.dp))

            if (request.updateRequired) {
                // iOS updateSection: 전체 안내 + 스토어 업데이트 버튼
                PaywallBanner(stringResource(R.string.subscription_update_required))
                Spacer(Modifier.height(24.dp))
                PaywallPrimaryButton(
                    text = stringResource(R.string.subscription_update_button),
                    onClick = { openPlayStoreListing(context) },
                )
                Spacer(Modifier.height(24.dp))
            } else {
                if (status.legacyKind == LegacyKind.EARLY_ACCESS) {
                    EarlyAccessCard()
                    Spacer(Modifier.height(24.dp))
                }
                request.limitKind?.let { kind ->
                    PaywallBanner(
                        text = when (kind) {
                            QuotaLimitKind.SHAPES -> stringResource(R.string.subscription_limit_shapes, limits.freeShapes)
                            QuotaLimitKind.SKETCHES -> stringResource(R.string.subscription_limit_sketches, limits.freeSketches)
                            QuotaLimitKind.DRONES -> stringResource(R.string.subscription_limit_drones, limits.freeDrones)
                        },
                    )
                    Spacer(Modifier.height(24.dp))
                }
                // iOS billingRetryBanner: 무료 플랜의 결제 재시도 상태 — 안내 + 구독 관리 버튼(빨강 0.1 배경)
                if (status.paymentIssue && !isPro) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PaywallCardCornerRadius))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.subscription_billing_retry_message),
                            fontSize = PaywallCalloutSize,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.subscription_manage),
                            fontSize = PaywallCalloutSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { manager.openManagement(activity) },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                BenefitsCard(limits)
                Spacer(Modifier.height(24.dp))

                // iOS planSection
                when {
                    status.legacyKind == LegacyKind.EARLY_ACCESS && !status.isPaidSubscriber -> Unit // 카드가 이미 안내
                    status.legacyKind == LegacyKind.ORIGINAL_DOWNLOAD && !status.isPaidSubscriber ->
                        ProStatusLabel(
                            icon = Icons.Default.CardGiftcard, // iOS gift.fill
                            text = stringResource(R.string.subscription_status_legacy),
                        )
                    isPro -> {
                        ProStatusLabel(
                            icon = Icons.Default.Verified, // iOS checkmark.seal.fill
                            text = stringResource(R.string.subscription_status_active),
                        )
                        if (status.legacyKind != null) {
                            Text(
                                text = stringResource(R.string.subscription_legacy_subscribed),
                                fontSize = 12.sp,
                                color = IosSystemOrange,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        if (status.isPaidSubscriber) {
                            TextButton(onClick = { manager.openManagement(activity) }) {
                                Text(stringResource(R.string.subscription_manage), fontSize = 17.sp, fontWeight = FontWeight.Normal)
                            }
                        }
                    }
                    else -> {
                        val currentPrice = price
                        if (currentPrice != null) {
                            Text(
                                text = stringResource(R.string.subscription_product_name),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.subscription_paywall_price, currentPrice),
                                fontSize = 22.sp, // iOS .title2 bold
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            PaywallPrimaryButton(
                                text = stringResource(R.string.subscription_subscribe),
                                onClick = { manager.purchase(activity, request.source) },
                                enabled = !isRestoring,
                            )
                        } else if (isLoadingProducts) {
                            // iOS: 상품을 불러오는 동안 ProgressView
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            // iOS: 상품을 못 불러오면 구독 버튼 대신 안내와 "다시 시도"
                            Text(
                                text = stringResource(R.string.subscription_product_unavailable),
                                fontSize = PaywallCalloutSize,
                                color = IosSecondaryLabel,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = manager::reloadProducts) {
                                Text(stringResource(R.string.subscription_product_retry), fontSize = 17.sp, fontWeight = FontWeight.Normal)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            enabled = !isRestoring,
                            onClick = {
                                isRestoring = true
                                manager.restore { restored ->
                                    isRestoring = false
                                    if (!restored) {
                                        restoreNotFound = true
                                    } else if (request.limitKind != null) {
                                        // iOS: 한도 때문에 뜬 페이월은 복원 성공 시 닫는다.
                                        onDismiss()
                                    }
                                }
                            },
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.subscription_restore), fontSize = 17.sp, fontWeight = FontWeight.Normal)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                // iOS legalSection: 자동 갱신 고지는 구매할 수 있거나 구독 중인 사람에게 보인다(평생 무료 제외).
                if (status.legacyKind == null || status.isPaidSubscriber) {
                    FinePrint(stringResource(R.string.subscription_paywall_renewal))
                    Spacer(Modifier.height(8.dp))
                }
                FinePrint(stringResource(R.string.subscription_cross_platform_notice))
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
            ) {
                // iOS legalSection 링크 .caption(12)
                TextButton(onClick = { document = "terms" }) {
                    Text(stringResource(R.string.subscription_terms), fontSize = 12.sp, fontWeight = FontWeight.Normal)
                }
                TextButton(onClick = { document = "privacy" }) {
                    Text(stringResource(R.string.subscription_privacy), fontSize = 12.sp, fontWeight = FontWeight.Normal)
                }
            }
        }
    }
    if (restoreNotFound) {
        AlertDialog(
            onDismissRequest = { restoreNotFound = false },
            title = { Text(stringResource(R.string.subscription_pro)) },
            text = { Text(stringResource(R.string.subscription_restore_not_found)) },
            confirmButton = {
                TextButton(onClick = { restoreNotFound = false }) { Text(stringResource(R.string.common_confirm)) }
            },
        )
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
    // iOS header VStack(spacing: 8)
    Spacer(Modifier.height(8.dp))
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
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun PaywallBanner(
    text: String,
    background: Color = PaywallLimitBannerColor,
) {
    Text(
        text = text,
        fontSize = PaywallCalloutSize, // iOS .callout
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
        Text(text = text, fontSize = PaywallCalloutSize) // iOS .callout
    }
}

/** iOS EarlyAccessBadge: 주황→분홍 그라데이션 캡슐. 설정의 플랜 행과 Pro 혜택 카드가 함께 쓴다. */
@Composable
internal fun EarlyAccessBadge() {
    Text(
        text = stringResource(R.string.subscription_early_badge),
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.ExtraBold, // iOS .heavy
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
    // iOS EarlyAccessCard: 가운데 정렬, sparkles, 주황→분홍 0.12 그라데이션 + 1.5 테두리
    val gradient = listOf(Color(0xFFFF9500), Color(0xFFFF2D55))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient.map { it.copy(alpha = 0.12f) }))
            .border(1.5.dp, Brush.linearGradient(gradient), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EarlyAccessBadge()
        Icon(
            imageVector = Icons.Default.AutoAwesome, // iOS sparkles
            contentDescription = null,
            tint = Color(0xFFFF9500),
            modifier = Modifier.size(36.dp),
        )
        Text(stringResource(R.string.subscription_early_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.subscription_early_message),
            fontSize = PaywallCalloutSize,
            textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AllInclusive, contentDescription = null, tint = IosSystemGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.subscription_early_lifetime),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = IosSystemGreen,
            )
        }
    }
}

@Composable
private fun ProStatusLabel(icon: ImageVector, text: String) {
    // iOS Label(.headline, .green)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = IosSystemGreen, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = IosSystemGreen,
            textAlign = TextAlign.Center,
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

@Composable
private fun PaywallPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(PaywallCtaHeight),
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun openPlayStoreListing(context: android.content.Context) {
    val packageName = context.packageName
    val market = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName"))
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(market) }.onFailure {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
