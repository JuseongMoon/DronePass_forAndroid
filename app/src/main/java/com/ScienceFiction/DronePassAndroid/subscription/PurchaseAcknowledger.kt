package com.ScienceFiction.DronePassAndroid.subscription

/** 구매 토큰의 acknowledge 상태를 다시 조회한 결과. */
internal enum class AcknowledgeCheck {
    /** acknowledge 됐거나, 더 이상 유효한 구매가 아니라 할 일이 없다. */
    DONE,

    /** 아직 acknowledge 되지 않았다. */
    NOT_DONE,

    /** 조회에 실패해 알 수 없다. */
    UNKNOWN,
}

/**
 * Google Play 구매 acknowledge. 3일 안에 acknowledge 하지 않으면 Play 가 자동 환불하므로 실패를 놓치면 안 되지만,
 * 결제 직후에는 구매 리스너와 onResume 재조회가 같은 토큰으로 거의 동시에 들어와, 겹친 요청 하나가 실패해도
 * 실제 구매는 acknowledge 된 상태일 수 있다(105 점검에서 성공한 결제에 "구매 확인 실패"가 뜬 원인).
 *
 * - 같은 토큰은 한 번에 하나만 보낸다. 진행 중이면 나중 호출은 버린다.
 * - 실패하면 상태를 다시 조회해 이미 끝났으면 조용히 마치고, 아니면 한 번 더 보낸다. 그것도 실패해야 [onFailure].
 * - 끝나면 토큰을 놓아 준다. 실패로 끝나도 다음 onResume·앱 시작의 구매 재조회가 다시 acknowledge 한다.
 *
 * [send] 와 [check] 는 결과를 콜백으로 한 번 돌려줘야 한다.
 */
internal class PurchaseAcknowledger(
    private val send: (token: String, onResult: (success: Boolean) -> Unit) -> Unit,
    private val check: (token: String, onResult: (AcknowledgeCheck) -> Unit) -> Unit,
    private val onFailure: () -> Unit,
) {
    private val inFlight = mutableSetOf<String>()

    fun acknowledge(token: String) {
        if (!synchronized(inFlight) { inFlight.add(token) }) return
        attempt(token, isRetry = false)
    }

    private fun attempt(token: String, isRetry: Boolean) {
        send(token) { success ->
            when {
                success -> release(token)
                isRetry -> {
                    release(token)
                    onFailure()
                }
                else -> check(token) { state ->
                    if (state == AcknowledgeCheck.DONE) release(token) else attempt(token, isRetry = true)
                }
            }
        }
    }

    private fun release(token: String) {
        synchronized(inFlight) { inFlight.remove(token) }
    }
}
