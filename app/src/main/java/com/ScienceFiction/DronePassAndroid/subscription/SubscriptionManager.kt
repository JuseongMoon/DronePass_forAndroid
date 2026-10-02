package com.ScienceFiction.DronePassAndroid.subscription

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.util.AnalyticsLogger
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URL
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private const val PRODUCT_ID = "dronepass.pro"
private const val BASE_PLAN_ID = "yearly"
private const val CONFIG_REFRESH_INTERVAL = 60L * 60L * 1000L
private const val PRO_CACHE_TTL = 24L * 60L * 60L * 1000L
private const val USAGE_EVENT_INTERVAL = 24L * 60L * 60L * 1000L

enum class ProSource { SUBSCRIPTION, LEGACY }

data class PlanStatus(
    val entitlement: EntitlementState = EntitlementState.UNKNOWN,
    val source: ProSource? = null,
    val legacyKind: LegacyKind? = null,
    val paymentIssue: Boolean = false,
    val hasPaidSubscription: Boolean = false,
    val proUntilMillis: Long? = null,
) {
    val isPaidSubscriber: Boolean get() = hasPaidSubscription
    fun isKnownAndValid(now: Long): Boolean = when (entitlement) {
        EntitlementState.UNKNOWN -> false
        EntitlementState.FREE -> true
        EntitlementState.PRO -> proUntilMillis?.let { it > now } ?: true
    }
}

/** [limitKind] 는 무료 한도에 걸려 열린 페이월일 때만 채운다. 페이월 상단 안내 문구를 고른다. */
data class PaywallRequest(
    val source: String,
    val updateRequired: Boolean = false,
    val limitKind: QuotaLimitKind? = null,
)
data class QuotaUsage(val shapes: Int = 0, val sketches: Int = 0, val drones: Int = 0)

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val shapeRepository: ShapeRepository,
    private val sketchRepository: SketchRepository,
    private val droneRepository: DroneRepository,
    private val analytics: AnalyticsLogger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val preferences = context.getSharedPreferences("subscription", Context.MODE_PRIVATE)
    private val queryGeneration = AtomicLong()
    private val creationMutex = Mutex()
    private var currentUid: String? = null
    private var started = false
    private var lastConfigAttempt = 0L
    private var productDetails: ProductDetails? = null
    private var latestPurchaseSource = "settings"
    private var currentRemoteMinimum: String? = null
    private var usageLoaded = false
    private var connecting = false

    private val _status = MutableStateFlow(PlanStatus())
    val status: StateFlow<PlanStatus> = _status.asStateFlow()
    private val _limits = MutableStateFlow(QuotaLimits.fallback)
    val limits: StateFlow<QuotaLimits> = _limits.asStateFlow()
    private val _usage = MutableStateFlow(QuotaUsage())
    val usage: StateFlow<QuotaUsage> = _usage.asStateFlow()
    private val _productPrice = MutableStateFlow<String?>(null)
    val productPrice: StateFlow<String?> = _productPrice.asStateFlow()
    private val _isLoadingProducts = MutableStateFlow(false)
    /** 상품 정보를 불러오는 중. iOS `isLoadingProducts` 처럼 페이월이 ProgressView 를 보여 준다. */
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()
    private val _paywallRequests = MutableSharedFlow<PaywallRequest>(extraBufferCapacity = 4)
    val paywallRequests = _paywallRequests.asSharedFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()
    private val acknowledger = PurchaseAcknowledger(
        send = ::sendAcknowledge,
        check = ::checkAcknowledged,
        onFailure = { message(R.string.subscription_message_acknowledge_failed) },
    )
    private val _signedIn = MutableStateFlow(auth.currentUser != null)
    /** 구매는 로그인한 상태에서만 한다(사양 v2 C-1). */
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()
    private val _activePurchase = MutableStateFlow<SubscriptionPurchaseInfo?>(null)
    /** 지금 유효한 Google Play 구독의 주문 정보. 계약 내용·해지·환불 요청 화면이 쓴다. */
    val activePurchase: StateFlow<SubscriptionPurchaseInfo?> = _activePurchase.asStateFlow()
    private val _purchaseCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** 구매 직후 계약 내용 시트를 띄우기 위한 이벤트(사양 v2 C-5). */
    val purchaseCompleted = _purchaseCompleted.asSharedFlow()

    /** 해지·환불 요청 메일에 미리 채울 계정 이메일. */
    val accountEmail: String? get() = auth.currentUser?.email

    private var cutoff = LegacyCutoff.fallback
    private val billing = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (purchases != null) applyPurchases(purchases, queryGeneration.incrementAndGet(), fromPurchaseFlow = true)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> analytics.logPurchaseFail(latestPurchaseSource, "cancelled")
                else -> analytics.logPurchaseFail(latestPurchaseSource, "error")
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val authListener = FirebaseAuth.AuthStateListener { onAccountChanged() }

    fun start() {
        if (started) return
        started = true
        restoreConfig()
        auth.addAuthStateListener(authListener)
        connectBilling()
        refreshConfig(force = true)
        scope.launch {
            combine(shapeRepository.getAllShapes(), sketchRepository.getActiveSketches(), droneRepository.getActiveDrones()) { shapes, sketches, drones ->
                QuotaUsage(QuotaPolicy.shapeCount(shapes.map { it.deletedAt }), sketches.size, drones.size)
            }.collect { current ->
                _usage.value = current
                usageLoaded = true
                logUsageIfDue()
            }
        }
    }

    fun onForeground() {
        start()
        if (!_status.value.isKnownAndValid(System.currentTimeMillis())) {
            val legacy = legacyKind() ?: LegacyKind.EARLY_ACCESS.takeIf {
                currentUid != null && preferences.getString("uid", null) == currentUid && preferences.getBoolean("legacy", false)
            }
            _status.value = legacy?.let { PlanStatus(EntitlementState.PRO, ProSource.LEGACY, it) } ?: PlanStatus()
        }
        refreshConfig(force = false)
        if (billing.isReady) refreshPurchases() else connectBilling()
    }

    private fun onAccountChanged() {
        _signedIn.value = auth.currentUser != null
        val uid = auth.currentUser?.uid
        if (uid == currentUid && _status.value.entitlement != EntitlementState.UNKNOWN) return
        currentUid = uid
        queryGeneration.incrementAndGet()
        _activePurchase.value = null
        // 로그인 계정에서 받은 평생 무료 권한을 다른 계정에 잠시라도 보여주지 않는다.
        _status.value = PlanStatus()
        val legacy = legacyKind()
        val cachedUid = preferences.getString("uid", null)
        val cachedSource = preferences.getString("source", null)
        val cachedUntil = preferences.getLong("proUntil", 0L)
        val cachedLegacy = uid != null && cachedUid == uid && preferences.getBoolean("legacy", false)
        if (cachedUid == uid && cachedSource == "subscription" && cachedUntil > System.currentTimeMillis()) {
            _status.value = PlanStatus(EntitlementState.PRO, ProSource.SUBSCRIPTION,
                legacyKind = legacy ?: LegacyKind.EARLY_ACCESS.takeIf { cachedLegacy },
                hasPaidSubscription = true, proUntilMillis = cachedUntil)
        } else if (legacy != null || cachedLegacy) {
            _status.value = PlanStatus(EntitlementState.PRO, ProSource.LEGACY, LegacyKind.EARLY_ACCESS)
        }
        logUsageIfDue()
        if (billing.isReady) refreshPurchases()
    }

    private fun legacyKind(): LegacyKind? {
        val createdAt = auth.currentUser?.metadata?.creationTimestamp?.takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        // Android 는 스토어 환경(샌드박스 여부)을 알 수 없으므로 계정 경로를 그대로 쓴다.
        return LegacyPolicy.legacyKind(null, false, createdAt, cutoff, isKnownSandbox = false)
    }

    private fun connectBilling() {
        if (billing.isReady || connecting) return
        connecting = true
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshPurchases()
                    queryProduct()
                } else {
                    _isLoadingProducts.value = false
                }
            }
            override fun onBillingServiceDisconnected() {
                connecting = false
                _isLoadingProducts.value = false
            }
        })
    }

    fun refreshPurchases(onRestored: ((Boolean) -> Unit)? = null) {
        if (!billing.isReady) {
            connectBilling()
            onRestored?.invoke(false)
            return
        }
        val generation = queryGeneration.incrementAndGet()
        val uid = currentUid
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .includeSuspendedSubscriptions(true)
            .build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            if (generation != queryGeneration.get() || uid != currentUid) return@queryPurchasesAsync
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                applyPurchases(purchases, generation, fromPurchaseFlow = false)
                onRestored?.invoke(purchases.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isSuspended })
            } else {
                onRestored?.invoke(false)
            }
        }
    }

    private fun applyPurchases(purchases: List<Purchase>, generation: Long, fromPurchaseFlow: Boolean) {
        if (generation != queryGeneration.get()) return
        val relevant = purchases.filter { PRODUCT_ID in it.products }
        val active = relevant.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isSuspended }
        val suspended = relevant.any { it.isSuspended }
        val legacy = legacyKind()
        val legacyUnknown = auth.currentUser?.metadata?.creationTimestamp?.let { it <= 0L } == true
        val next = when {
            active != null -> PlanStatus(EntitlementState.PRO, ProSource.SUBSCRIPTION, legacy, hasPaidSubscription = true, proUntilMillis = System.currentTimeMillis() + PRO_CACHE_TTL)
            legacy != null -> PlanStatus(EntitlementState.PRO, ProSource.LEGACY, legacy, paymentIssue = suspended, hasPaidSubscription = suspended)
            relevant.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> _status.value
            legacyUnknown -> _status.value.takeIf { it.isKnownAndValid(System.currentTimeMillis()) }
                ?: PlanStatus(paymentIssue = suspended, hasPaidSubscription = suspended)
            else -> PlanStatus(EntitlementState.FREE, paymentIssue = suspended, hasPaidSubscription = suspended)
        }
        _status.value = next
        _activePurchase.value = active?.let {
            SubscriptionPurchaseInfo(orderId = it.orderId, purchaseTimeMillis = it.purchaseTime, autoRenewing = it.isAutoRenewing)
        }
        logUsageIfDue()
        if (next.entitlement == EntitlementState.PRO) {
            preferences.edit().putString("uid", currentUid).putString("source", next.source?.name?.lowercase())
                .putBoolean("legacy", next.legacyKind != null)
                .putLong("proUntil", if (next.source == ProSource.LEGACY) Long.MAX_VALUE else System.currentTimeMillis() + PRO_CACHE_TTL).apply()
        } else {
            preferences.edit().remove("source").remove("proUntil").remove("legacy").apply()
        }
        if (active != null) {
            acknowledge(active)
            if (fromPurchaseFlow) {
                analytics.logPurchaseSuccess(latestPurchaseSource)
                _purchaseCompleted.tryEmit(Unit)
            }
        } else if (fromPurchaseFlow && relevant.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            analytics.logPurchaseFail(latestPurchaseSource, "pending")
            message(R.string.subscription_message_purchase_pending)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        acknowledger.acknowledge(purchase.purchaseToken)
    }

    private fun sendAcknowledge(token: String, onResult: (Boolean) -> Unit) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
        billing.acknowledgePurchase(params) { result -> onResult(result.responseCode == BillingClient.BillingResponseCode.OK) }
    }

    private fun checkAcknowledged(token: String, onResult: (AcknowledgeCheck) -> Unit) {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(AcknowledgeCheck.UNKNOWN)
                return@queryPurchasesAsync
            }
            val purchase = purchases.firstOrNull { it.purchaseToken == token }
            onResult(if (purchase == null || purchase.isAcknowledged) AcknowledgeCheck.DONE else AcknowledgeCheck.NOT_DONE)
        }
    }

    private fun message(resId: Int) {
        _messages.tryEmit(context.getString(resId))
    }

    /** 페이월 "다시 시도": 상품 정보를 다시 불러온다. */
    fun reloadProducts() {
        _isLoadingProducts.value = true
        if (billing.isReady) queryProduct() else connectBilling()
    }

    private fun queryProduct(onLoaded: ((ProductDetails?) -> Unit)? = null) {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS).build()),
        ).build()
        _isLoadingProducts.value = true
        billing.queryProductDetailsAsync(params) { result, products ->
            _isLoadingProducts.value = false
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onLoaded?.invoke(null)
                return@queryProductDetailsAsync
            }
            productDetails = products.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
            val offer = productDetails?.subscriptionOfferDetails?.firstOrNull { it.basePlanId == BASE_PLAN_ID && it.offerId == null }
            _productPrice.value = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            onLoaded?.invoke(productDetails)
        }
    }

    fun purchase(activity: Activity, source: String) {
        // 로그인 필수(계정 토큰을 붙여 구매한다), 플랜 확인 중에는 평생 무료 대상자의 실수 결제를 막는다(사양 v2 C-1·C-2).
        if (!canStartPurchase(signedIn = auth.currentUser != null, entitlement = _status.value.entitlement)) return
        latestPurchaseSource = source
        analytics.logPurchaseStart(source)
        if (!billing.isReady) {
            connectBilling()
            analytics.logPurchaseFail(source, "error")
            message(R.string.subscription_message_billing_connecting)
            return
        }
        queryProduct { product ->
            if (product == null) {
                analytics.logPurchaseFail(source, "error")
                message(R.string.subscription_message_product_unavailable)
                return@queryProduct
            }
            launchBillingFlow(activity, source, product)
        }
    }

    private fun launchBillingFlow(activity: Activity, source: String, product: ProductDetails) {
        val offer = product.subscriptionOfferDetails?.firstOrNull { it.basePlanId == BASE_PLAN_ID && it.offerId == null }
            ?: run { message(R.string.subscription_message_yearly_plan_missing); return }
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build()
        val builder = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item))
        auth.currentUser?.uid?.let { builder.setObfuscatedAccountId(appAccountToken(it)) }
        val result = billing.launchBillingFlow(activity, builder.build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) analytics.logPurchaseFail(source, "error")
    }

    /** [onResult] 는 복원된 구독이 있으면 true (iOS SettingView 의 복원 결과 알림용). */
    fun restore(onResult: ((Boolean) -> Unit)? = null) {
        analytics.logRestoreTap()
        refreshPurchases { restored ->
            analytics.logRestoreResult(restored)
            onResult?.invoke(restored)
        }
    }

    fun openManagement(activity: Context) {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=${context.packageName}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (activity !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    fun showPaywall(source: String, limitKind: QuotaLimitKind? = null) {
        analytics.logPaywallView(source)
        _paywallRequests.tryEmit(PaywallRequest(source, limitKind = limitKind))
    }

    suspend fun allow(action: QuotaAction, source: String): Boolean {
        if (currentRemoteMinimum?.let { isVersionOlder(BuildConfig.VERSION_NAME.removeSuffix("-debug"), it) } == true) {
            _paywallRequests.emit(PaywallRequest(source, updateRequired = true))
            return false
        }
        val count = when (action.limitKind) {
            QuotaLimitKind.SHAPES -> QuotaPolicy.shapeCount(shapeRepository.getAllShapes().first().map { it.deletedAt })
            QuotaLimitKind.SKETCHES -> sketchRepository.getActiveSketches().first().size
            QuotaLimitKind.DRONES -> droneRepository.getActiveDrones().first().size
        }
        if (QuotaPolicy.evaluate(action, { count }, _status.value.entitlement, _limits.value) == QuotaDecision.Allowed) return true
        analytics.logQuotaBlock(action.limitKind.name.lowercase())
        showPaywall(source, action.limitKind)
        return false
    }

    suspend fun withCreationPermit(action: QuotaAction, source: String, save: suspend () -> Unit): Boolean =
        creationMutex.withLock {
            if (!allow(action, source)) return@withLock false
            save()
            true
        }

    fun allowSketchStroke(currentCount: Int): Boolean {
        if (currentRemoteMinimum?.let { isVersionOlder(BuildConfig.VERSION_NAME.removeSuffix("-debug"), it) } == true) {
            _paywallRequests.tryEmit(PaywallRequest("sketch", updateRequired = true))
            return false
        }
        if (QuotaPolicy.evaluate(QuotaAction.START_SKETCH_STROKE, { currentCount }, _status.value.entitlement, _limits.value) == QuotaDecision.Allowed) return true
        analytics.logQuotaBlock("sketches")
        return false
    }

    private fun restoreConfig() {
        val cached = preferences.getString("config", null)?.let { RemoteSubscriptionConfig.parse(it) } ?: return
        _limits.value = QuotaLimits.fallback.raised(cached.limits)
        cutoff = LegacyCutoff.fallback.extended(cached.legacyCutoff)
        RemoteFeatureFlags.update(cached)
    }

    private fun logUsageIfDue() {
        if (!usageLoaded || _status.value.entitlement == EntitlementState.UNKNOWN) return
        val now = System.currentTimeMillis()
        if (now - preferences.getLong("usageLoggedAt", 0L) < USAGE_EVENT_INTERVAL) return
        val plan = when {
            _status.value.source == ProSource.SUBSCRIPTION -> "pro"
            _status.value.legacyKind == LegacyKind.EARLY_ACCESS -> "early_access"
            _status.value.legacyKind == LegacyKind.ORIGINAL_DOWNLOAD -> "legacy"
            else -> "free"
        }
        val current = _usage.value
        analytics.logQuotaUsage(current.shapes, current.sketches, current.drones, plan)
        preferences.edit().putLong("usageLoggedAt", now).apply()
    }

    private fun refreshConfig(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastConfigAttempt < CONFIG_REFRESH_INTERVAL) return
        lastConfigAttempt = now
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                try {
                    (URL(RemoteSubscriptionConfig.URL).openConnection().apply {
                        connectTimeout = 10_000
                        readTimeout = 10_000
                    }).getInputStream().bufferedReader().use { it.readText() }
                } catch (_: Exception) { null }
            } ?: return@launch
            val config = RemoteSubscriptionConfig.parse(json) ?: return@launch
            _limits.value = _limits.value.raised(config.limits)
            cutoff = cutoff.extended(config.legacyCutoff)
            currentRemoteMinimum = config.minSupportedVersion
            RemoteFeatureFlags.update(config)
            val effective = _limits.value
            val cachedConfig = """{"schemaVersion":1,"free":{"shapes":${effective.freeShapes},"sketches":${effective.freeSketches},"drones":${effective.freeDrones}},"legacy":{"iosOriginalBuildBefore":${cutoff.iosOriginalBuildBefore},"accountCreatedBefore":"${cutoff.accountCreatedBefore}"},"features":{"locationAudit":{"uploadEnabled":${config.locationAuditUploadEnabled}}}}"""
            preferences.edit().putString("config", cachedConfig).apply()
            val newLegacy = legacyKind()
            val old = _status.value
            if (newLegacy != null) {
                _status.value = if (old.source == ProSource.SUBSCRIPTION) old.copy(legacyKind = newLegacy)
                else PlanStatus(EntitlementState.PRO, ProSource.LEGACY, newLegacy, old.paymentIssue, old.hasPaidSubscription)
            }
        }
    }
}
