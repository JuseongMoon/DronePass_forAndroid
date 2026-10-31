package com.ScienceFiction.DronePassAndroid.subscription

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.TagRepository
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
private const val KEY_SIGNED_IN_ON_DEVICE = "signedInOnDevice"

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
data class QuotaUsage(val shapes: Int = 0, val sketches: Int = 0, val drones: Int = 0, val tags: Int = 0)

@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val shapeRepository: ShapeRepository,
    private val sketchRepository: SketchRepository,
    private val droneRepository: DroneRepository,
    private val tagRepository: TagRepository,
    private val analytics: AnalyticsLogger,
    private val claimSender: SubscriptionClaimSender,
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
    /** `features.proClaim.enabled`. 이번 실행에서 받은 설정에서만 켠다(계약 §9-2, §6 저장본 규칙). */
    @Volatile private var proClaimEnabled = false
    /** 마지막 조회에서 찾은 활성 구독 구매 토큰. 복원 직후·설정을 받은 뒤 claim 에 쓴다. */
    @Volatile private var activePurchaseTokens: List<String> = emptyList()
    private val claimInFlight = java.util.concurrent.atomic.AtomicBoolean(false)
    /** 결제 연결 전에 누른 구매 복원. 연결이 끝나면 조회해 답하고, 실패하면 오류로 답한다. */
    private val pendingRestoreCallbacks = mutableListOf<(RestoreResult) -> Unit>()

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
        onFailure = { token ->
            message(R.string.subscription_message_acknowledge_failed)
            enqueuePurchaseAcknowledgeWork(context, token)
        },
    )
    private val _signedIn = MutableStateFlow(auth.currentUser != null)
    /** 로그인 여부. 구매에는 필요 없다(D-52). 로그아웃 결제 화면이 로그인 안내를 보여 줄 때 쓴다. */
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()
    private val _hasSignedInOnDevice = MutableStateFlow(preferences.getBoolean(KEY_SIGNED_IN_ON_DEVICE, false))
    /**
     * 이 기기에서 로그인한 적이 있다. 로그아웃해도 지우지 않는다. 로그아웃 결제 화면이 평생 무료 안내를
     * 강조할 때만 쓴다(D-52). 평생 무료 계정이 로그아웃한 채 결제하지 않게 한다.
     */
    val hasSignedInOnDevice: StateFlow<Boolean> = _hasSignedInOnDevice.asStateFlow()
    private val _activePurchase = MutableStateFlow<SubscriptionPurchaseInfo?>(null)
    /** 지금 유효한 Google Play 구독의 주문 정보. 계약 내용·해지·환불 요청 화면이 쓴다. */
    val activePurchase: StateFlow<SubscriptionPurchaseInfo?> = _activePurchase.asStateFlow()
    private val _purchaseCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** 구매 직후 계약 내용 시트를 띄우기 위한 이벤트(사양 v2 C-5). */
    val purchaseCompleted = _purchaseCompleted.asSharedFlow()

    /** 해지·환불 요청 메일에 미리 채울 계정 이메일. */
    val accountEmail: String? get() = auth.currentUser?.email

    private val _legacyAccountCreatedBefore = MutableStateFlow(LegacyCutoff.fallback.accountCreatedBefore)
    /** 평생 무료 기준(이 시각 전에 가입한 계정). 원격 설정으로만 늦춰진다. 로그아웃 결제 화면 안내가 쓴다. */
    val legacyAccountCreatedBefore: StateFlow<Instant> = _legacyAccountCreatedBefore.asStateFlow()
    private var cutoff = LegacyCutoff.fallback
        set(value) {
            field = value
            _legacyAccountCreatedBefore.value = value.accountCreatedBefore
        }
    private val billing = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (purchases != null) applyPurchases(purchases, queryGeneration.incrementAndGet(), fromPurchaseFlow = true)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> analytics.logPurchaseFail(latestPurchaseSource, "cancelled")
                else -> {
                    analytics.logPurchaseFail(latestPurchaseSource, "error")
                    handlePurchaseFailure(result.responseCode)
                }
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val authListener = FirebaseAuth.AuthStateListener { onAccountChanged() }

    fun start() {
        if (started) return
        started = true
        // 3.6.3 이하에서 로그인했던 기기: 그때 Pro 판정과 함께 남긴 계정 uid 로 한 번 채운다.
        if (preferences.getString("uid", null) != null) rememberSignedInOnDevice()
        restoreConfig()
        auth.addAuthStateListener(authListener)
        connectBilling()
        refreshConfig(force = true)
        scope.launch {
            combine(
                shapeRepository.getAllShapes(),
                sketchRepository.getActiveSketches(),
                droneRepository.getActiveDrones(),
                tagRepository.getActiveTags(),
            ) { shapes, sketches, drones, tags ->
                QuotaUsage(QuotaPolicy.shapeCount(shapes.map { it.deletedAt }), sketches.size, drones.size, tags.size)
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
        if (auth.currentUser != null) rememberSignedInOnDevice()
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

    private fun rememberSignedInOnDevice() {
        if (_hasSignedInOnDevice.value) return
        _hasSignedInOnDevice.value = true
        preferences.edit().putBoolean(KEY_SIGNED_IN_ON_DEVICE, true).apply()
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
                    val waiting = drainPendingRestoreCallbacks()
                    refreshPurchases(waiting.takeIf { it.isNotEmpty() }?.let { callbacks -> { result -> callbacks.forEach { it(result) } } })
                    queryProduct()
                } else {
                    _isLoadingProducts.value = false
                    drainPendingRestoreCallbacks().forEach { it(RestoreResult.FAILED) }
                }
            }
            override fun onBillingServiceDisconnected() {
                connecting = false
                _isLoadingProducts.value = false
                drainPendingRestoreCallbacks().forEach { it(RestoreResult.FAILED) }
            }
        })
    }

    private fun drainPendingRestoreCallbacks(): List<(RestoreResult) -> Unit> =
        pendingRestoreCallbacks.toList().also { pendingRestoreCallbacks.clear() }

    fun refreshPurchases(onRestored: ((RestoreResult) -> Unit)? = null) {
        if (!billing.isReady) {
            // 연결 전에 "구독 없음"으로 답하면 구독이 있어도 없다고 안내했다. 연결을 기다린다.
            onRestored?.let(pendingRestoreCallbacks::add)
            connectBilling()
            return
        }
        val generation = queryGeneration.incrementAndGet()
        val uid = currentUid
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .includeSuspendedSubscriptions(true)
            .build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            // 다른 조회가 뒤에 시작됐으면 상태 반영은 그쪽에 맡기되, 복원 결과는 이 조회로 답한다.
            // 답하지 않으면 복원 버튼의 진행 표시가 멈춘 채 남았다.
            val isLatest = generation == queryGeneration.get() && uid == currentUid
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                if (isLatest) applyPurchases(purchases, generation, fromPurchaseFlow = false)
                val restored = purchases.any { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isSuspended }
                onRestored?.invoke(if (restored) RestoreResult.RESTORED else RestoreResult.NOT_FOUND)
            } else {
                onRestored?.invoke(RestoreResult.FAILED)
            }
        }
    }

    private fun applyPurchases(purchases: List<Purchase>, generation: Long, fromPurchaseFlow: Boolean) {
        if (generation != queryGeneration.get()) return
        val relevant = purchases.filter { PRODUCT_ID in it.products }
        val active = relevant.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isSuspended }
        activePurchaseTokens = relevant
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isSuspended }
            .map { it.purchaseToken }
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
            // 서버 Pro 미러용 증빙: 구매 직후는 바로, 시작·포그라운드 조회는 계정마다 24시간에 한 번.
            sendProClaimIfDue(force = fromPurchaseFlow)
            if (fromPurchaseFlow) {
                analytics.logPurchaseSuccess(latestPurchaseSource)
                _purchaseCompleted.tryEmit(Unit)
            }
        } else if (fromPurchaseFlow && relevant.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            analytics.logPurchaseFail(latestPurchaseSource, "pending")
            message(R.string.subscription_message_purchase_pending)
        }
    }

    /**
     * 활성 구독의 구매 토큰을 `claimSubscription` 으로 보낸다(계약 §9-2). 원격 플래그가 꺼져 있거나 로그아웃 상태면 보내지 않는다.
     * 결과는 화면·판정에 쓰지 않는다. 호출이 끝나면 이 계정의 보낸 시각을 남기고, 실패하면 다음 기회에 다시 보낸다.
     */
    private fun sendProClaimIfDue(force: Boolean) {
        val uid = auth.currentUser?.uid
        val tokens = activePurchaseTokens
        val lastSentAt = uid?.let { preferences.getLong(proClaimSentAtKey(it), -1L).takeIf { at -> at >= 0L } }
        val now = System.currentTimeMillis()
        val enabled = effectiveProClaimEnabled(proClaimEnabled, BuildConfig.PRO_CLAIM_FORCE_ENABLED)
        if (!shouldSendProClaim(enabled, uid != null, tokens.isNotEmpty(), force, lastSentAt, now)) return
        if (!claimInFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                if (claimSender.send(PRODUCT_ID, tokens) && auth.currentUser?.uid == uid) {
                    preferences.edit().putLong(proClaimSentAtKey(requireNotNull(uid)), System.currentTimeMillis()).apply()
                }
            } finally {
                claimInFlight.set(false)
            }
        }
    }

    private fun proClaimSentAtKey(uid: String) = "proClaimSentAt.$uid"

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
        // 플랜 확인 중에는 평생 무료 대상자의 실수 결제를 막는다(사양 v2 C-2). 로그인은 선택이다(D-52).
        if (!canStartPurchase(entitlement = _status.value.entitlement)) return
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
        // 계정 연결 토큰은 로그인 상태의 구매에만 붙인다(계약 §8). 비로그인 구매는 서버·확장에 연결하지 않는다.
        auth.currentUser?.uid?.let { builder.setObfuscatedAccountId(appAccountToken(it)) }
        val result = billing.launchBillingFlow(activity, builder.build())
        analytics.logBillingFlowLaunch(source, result.responseCode)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            analytics.logPurchaseFail(source, "error")
            handlePurchaseFailure(result.responseCode)
        }
    }

    /**
     * 취소가 아닌 결제 실패. 이미 구독 중이면(ITEM_ALREADY_OWNED) 안내 대신 구매를 다시 조회해 바로 반영한다.
     * 그 밖에는 결제 결과를 알 수 없으니 구매 복원으로 확인하라고 안내한다.
     */
    private fun handlePurchaseFailure(responseCode: Int) {
        if (purchaseFailureRefreshesPurchases(responseCode)) refreshPurchases()
        else message(R.string.subscription_message_purchase_unconfirmed)
    }

    /** [onResult] 는 복원 결과(iOS SettingView 의 복원 결과 알림용). 결제 연결·조회 실패는 [RestoreResult.FAILED]. */
    fun restore(onResult: ((RestoreResult) -> Unit)? = null) {
        analytics.logRestoreTap()
        refreshPurchases { result ->
            analytics.logRestoreResult(result == RestoreResult.RESTORED)
            if (result == RestoreResult.RESTORED) sendProClaimIfDue(force = true)
            onResult?.invoke(result)
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
            QuotaLimitKind.TAGS -> tagRepository.activeTagCount()
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
        analytics.logQuotaUsage(current.shapes, current.sketches, current.drones, current.tags, plan)
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
            proClaimEnabled = config.proClaimEnabled
            // 설정이 구매 조회보다 늦게 오면 시작 때 보낼 기회를 놓친다. 받은 뒤 한 번 더 확인한다(24시간 간격은 그대로).
            sendProClaimIfDue(force = false)
            val effective = _limits.value
            val cachedConfig = """{"schemaVersion":1,"free":{"shapes":${effective.freeShapes},"sketches":${effective.freeSketches},"drones":${effective.freeDrones},"tags":${effective.freeTags}},"legacy":{"iosOriginalBuildBefore":${cutoff.iosOriginalBuildBefore},"accountCreatedBefore":"${cutoff.accountCreatedBefore}"},"features":{"locationAudit":{"uploadEnabled":${config.locationAuditUploadEnabled}}}}"""
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

enum class RestoreResult { RESTORED, NOT_FOUND, FAILED }

/** 이미 가진 구독을 다시 사려 한 실패는 안내 대신 구매 재조회로 처리한다. */
internal fun purchaseFailureRefreshesPurchases(responseCode: Int): Boolean =
    responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED

/** 복원 결과 안내 문구. 연결·조회 실패를 "구독 없음"으로 안내하지 않는다. */
fun restoreResultMessageRes(result: RestoreResult): Int = when (result) {
    RestoreResult.RESTORED -> com.ScienceFiction.DronePassAndroid.R.string.subscription_restore_success
    RestoreResult.NOT_FOUND -> com.ScienceFiction.DronePassAndroid.R.string.subscription_restore_not_found
    RestoreResult.FAILED -> com.ScienceFiction.DronePassAndroid.R.string.subscription_restore_failed
}
