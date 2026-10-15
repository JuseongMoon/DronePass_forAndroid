package com.ScienceFiction.DronePassAndroid.subscription

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Play 는 3일 안에 acknowledge 하지 않은 구매를 자동 환불한다. 그 뒤에는 다시 시도해도 소용이 없다. */
internal const val PURCHASE_ACK_RETRY_WINDOW_MS = 3L * 24L * 60L * 60L * 1000L
internal const val PURCHASE_ACK_BACKOFF_SECONDS = 30L
private const val KEY_TOKEN = "purchaseToken"
private const val KEY_ENQUEUED_AT = "enqueuedAt"

/** 백그라운드 acknowledge 한 번의 결과. */
internal enum class PurchaseAckStep { DONE, RETRY }

/** 조회한 구독 중 같은 토큰의 구매 상태. */
internal enum class PurchaseAckTarget { NOT_FOUND, ACKNOWLEDGED, PENDING, NEEDS_ACK }

/**
 * 백그라운드 acknowledge 판단(계약 §9-2 D-24).
 * 성공했거나 이미 acknowledge 됐거나, 더는 유효한 구매가 아니거나, 3일이 지났으면 끝낸다. 그 밖에는 다시 시도한다.
 *
 * @param target 조회한 구매 상태. 연결이나 조회에 실패하면 null.
 * @param acknowledge 아직 acknowledge 되지 않은 결제 완료 구매일 때만 부른다. 성공하면 true.
 */
internal suspend fun purchaseAckStep(
    elapsedMillis: Long,
    target: PurchaseAckTarget?,
    acknowledge: suspend () -> Boolean,
): PurchaseAckStep = when {
    elapsedMillis > PURCHASE_ACK_RETRY_WINDOW_MS -> PurchaseAckStep.DONE
    target == null || target == PurchaseAckTarget.PENDING -> PurchaseAckStep.RETRY
    target != PurchaseAckTarget.NEEDS_ACK -> PurchaseAckStep.DONE
    acknowledge() -> PurchaseAckStep.DONE
    else -> PurchaseAckStep.RETRY
}

internal fun purchaseAckTarget(purchase: Purchase?): PurchaseAckTarget = when {
    purchase == null -> PurchaseAckTarget.NOT_FOUND
    purchase.isAcknowledged -> PurchaseAckTarget.ACKNOWLEDGED
    purchase.purchaseState != Purchase.PurchaseState.PURCHASED -> PurchaseAckTarget.PENDING
    else -> PurchaseAckTarget.NEEDS_ACK
}

/** 같은 구매에는 작업을 하나만 둔다. 토큰은 작업 이름에 그대로 넣지 않는다. */
internal fun purchaseAckWorkName(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
    return "purchase-ack-" + digest.take(8).joinToString("") { "%02x".format(it) }
}

/**
 * 앱 안의 acknowledge 가 다시 시도까지 실패하면 부른다. 앱을 다시 열지 않아도 네트워크가 있을 때
 * 지수 백오프로 acknowledge 를 다시 시도한다. 이미 있는 작업은 그대로 둔다.
 */
internal fun enqueuePurchaseAcknowledgeWork(context: Context, token: String, now: Long = System.currentTimeMillis()) {
    val request = OneTimeWorkRequestBuilder<PurchaseAcknowledgeWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, PURCHASE_ACK_BACKOFF_SECONDS, TimeUnit.SECONDS)
        .setInputData(workDataOf(KEY_TOKEN to token, KEY_ENQUEUED_AT to now))
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(purchaseAckWorkName(token), ExistingWorkPolicy.KEEP, request)
}

/** 구독 구매를 백그라운드에서 acknowledge 한다. 앱 프로세스의 결제 연결과 별개로 자기 연결을 열고 닫는다. */
class PurchaseAcknowledgeWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN) ?: return Result.success()
        val elapsed = System.currentTimeMillis() - inputData.getLong(KEY_ENQUEUED_AT, System.currentTimeMillis())
        if (elapsed > PURCHASE_ACK_RETRY_WINDOW_MS) return Result.success()
        val client = BillingClient.newBuilder(applicationContext)
            .setListener { _, _ -> }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        return try {
            val purchases = if (client.connect()) client.querySubscriptions() else null
            val step = purchaseAckStep(
                elapsedMillis = elapsed,
                target = purchases?.let { list -> purchaseAckTarget(list.firstOrNull { it.purchaseToken == token }) },
                acknowledge = { client.acknowledge(token) },
            )
            if (step == PurchaseAckStep.DONE) Result.success() else Result.retry()
        } finally {
            client.endConnection()
        }
    }
}

private suspend fun BillingClient.connect(): Boolean = suspendCancellableCoroutine { continuation ->
    startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
            if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
        }

        override fun onBillingServiceDisconnected() {
            if (continuation.isActive) continuation.resume(false)
        }
    })
}

/** 조회에 실패하면 null. */
private suspend fun BillingClient.querySubscriptions(): List<Purchase>? = suspendCancellableCoroutine { continuation ->
    val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
    queryPurchasesAsync(params) { result, purchases ->
        if (continuation.isActive) {
            continuation.resume(purchases.takeIf { result.responseCode == BillingClient.BillingResponseCode.OK })
        }
    }
}

private suspend fun BillingClient.acknowledge(token: String): Boolean = suspendCancellableCoroutine { continuation ->
    val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
    acknowledgePurchase(params) { result ->
        if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
    }
}
