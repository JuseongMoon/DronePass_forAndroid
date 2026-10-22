package com.ScienceFiction.DronePassAndroid.subscription

import android.util.Log
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** 서버 Pro 미러용 구매 증빙 callable(계약 §9-2). 리전은 Functions 기본값 asia-northeast3. */
internal const val CLAIM_SUBSCRIPTION_CALLABLE = "claimSubscription"

private const val TAG = "SubscriptionClaim"

/**
 * claim 을 켤지. 운영은 원격 플래그만 따른다. 디버그 빌드는 `-PproClaimDebug=true` 로 빌드했을 때만
 * [debugForced] 가 true 가 되어 끝단 시험을 할 수 있다(D-40). 릴리스 빌드의 [debugForced] 는 항상 false 다.
 */
internal fun effectiveProClaimEnabled(remoteEnabled: Boolean, debugForced: Boolean): Boolean = remoteEnabled || debugForced

/** 시작·포그라운드 때는 계정마다 24시간에 한 번만 보낸다. 구매·복원 직후에는 바로 보낸다. */
internal const val PRO_CLAIM_INTERVAL_MS = 24L * 60L * 60L * 1000L

/** `{platform:"android", purchases:[{productId, purchaseToken}]}`. 앱 버전은 SDK 가 붙이므로 넣지 않는다. */
internal fun claimSubscriptionRequest(productId: String, purchaseTokens: List<String>): Map<String, Any> = mapOf(
    "platform" to "android",
    "purchases" to purchaseTokens.map { mapOf("productId" to productId, "purchaseToken" to it) },
)

/**
 * 구매 증빙을 보낼지(계약 §9-2). 원격 플래그가 켜져 있고 로그인했으며 활성 구독이 있을 때만 보낸다.
 * 구매·복원 직후([force])가 아니면 이 계정으로 마지막에 보낸 지 24시간이 지났을 때만 보낸다.
 */
internal fun shouldSendProClaim(
    enabled: Boolean,
    signedIn: Boolean,
    hasActivePurchase: Boolean,
    force: Boolean,
    lastSentAtMillis: Long?,
    nowMillis: Long,
): Boolean = enabled && signedIn && hasActivePurchase &&
    (force || lastSentAtMillis == null || nowMillis - lastSentAtMillis >= PRO_CLAIM_INTERVAL_MS)

/**
 * `claimSubscription` 을 Functions SDK 로 부른다. 로그인 토큰과 App Check 토큰은 SDK 가 붙인다(둘 다 서버가 요구한다).
 * 응답(`recorded`·`ignored`·`disabled`)과 오류는 화면에도 판정에도 쓰지 않는다. 호출이 끝났는지만 돌려준다.
 */
@Singleton
class SubscriptionClaimSender @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun send(productId: String, purchaseTokens: List<String>): Boolean = try {
        val result = functions.getHttpsCallable(CLAIM_SUBSCRIPTION_CALLABLE)
            .call(claimSubscriptionRequest(productId, purchaseTokens)).await()
        // 끝단 시험 확인용. 디버그 빌드에서만 결과 종류(recorded·ignored·disabled)를 남긴다. 토큰은 남기지 않는다.
        if (BuildConfig.DEBUG) Log.d(TAG, "claimSubscription 결과: ${claimResultLabel(result.data)}")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.d(TAG, "claimSubscription 실패: ${e.javaClass.simpleName} ${e.message}")
        false
    }
}

/** 응답 `{ok, result}` 에서 result 만 꺼낸다. 형식이 다르면 "unknown". */
internal fun claimResultLabel(data: Any?): String = ((data as? Map<*, *>)?.get("result") as? String) ?: "unknown"
