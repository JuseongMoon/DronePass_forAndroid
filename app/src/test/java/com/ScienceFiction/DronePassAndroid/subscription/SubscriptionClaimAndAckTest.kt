package com.ScienceFiction.DronePassAndroid.subscription

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 3.6.2: 서버 Pro 미러용 claim(D-23)과 백그라운드 acknowledge 재시도(D-24). 계약 §9-2. */
class SubscriptionClaimAndAckTest {

    private val base = """{"schemaVersion":1"""

    @Test
    fun `proClaim 플래그는 features 안에서 읽고 없거나 불리언이 아니면 꺼진다`() {
        assertFalse(RemoteSubscriptionConfig.parse("$base}")!!.proClaimEnabled)
        assertFalse(RemoteSubscriptionConfig.parse("""$base,"features":{"proClaim":{"enabled":"true"}}}""")!!.proClaimEnabled)
        assertTrue(RemoteSubscriptionConfig.parse("""$base,"features":{"proClaim":{"enabled":true}}}""")!!.proClaimEnabled)
    }

    @Test
    fun `features 에 proClaim 이 더해져도 locationAudit 업로드 플래그는 그대로 읽는다`() {
        val both = RemoteSubscriptionConfig.parse(
            """$base,"features":{"locationAudit":{"uploadEnabled":true},"proClaim":{"enabled":false}}}""",
        )!!
        assertTrue(both.locationAuditUploadEnabled)
        assertFalse(both.proClaimEnabled)
        val onlyClaim = RemoteSubscriptionConfig.parse("""$base,"features":{"proClaim":{"enabled":true}}}""")!!
        assertFalse(onlyClaim.locationAuditUploadEnabled)
        assertTrue(onlyClaim.proClaimEnabled)
    }

    @Test
    fun `claim 요청은 계약 형식 그대로 android 구매 토큰을 담는다`() {
        assertEquals(
            mapOf(
                "platform" to "android",
                "purchases" to listOf(mapOf("productId" to "dronepass.pro", "purchaseToken" to "token-1")),
            ),
            claimSubscriptionRequest("dronepass.pro", listOf("token-1")),
        )
        assertEquals("claimSubscription", CLAIM_SUBSCRIPTION_CALLABLE)
    }

    @Test
    fun `claim 은 플래그가 켜져 있고 로그인했고 활성 구독이 있을 때만 보낸다`() {
        val now = 10L * PRO_CLAIM_INTERVAL_MS
        assertFalse(shouldSendProClaim(false, true, true, force = true, lastSentAtMillis = null, nowMillis = now))
        assertFalse(shouldSendProClaim(true, false, true, force = true, lastSentAtMillis = null, nowMillis = now))
        assertFalse(shouldSendProClaim(true, true, false, force = true, lastSentAtMillis = null, nowMillis = now))
        assertTrue(shouldSendProClaim(true, true, true, force = false, lastSentAtMillis = null, nowMillis = now))
    }

    @Test
    fun `시작과 포그라운드는 계정마다 24시간에 한 번, 구매와 복원 직후는 바로 보낸다`() {
        val now = 10L * PRO_CLAIM_INTERVAL_MS
        val recent = now - PRO_CLAIM_INTERVAL_MS + 1
        assertFalse(shouldSendProClaim(true, true, true, force = false, lastSentAtMillis = recent, nowMillis = now))
        assertTrue(shouldSendProClaim(true, true, true, force = false, lastSentAtMillis = now - PRO_CLAIM_INTERVAL_MS, nowMillis = now))
        assertTrue(shouldSendProClaim(true, true, true, force = true, lastSentAtMillis = recent, nowMillis = now))
    }

    @Test
    fun `claim 플래그는 저장본에 남기지 않고 구매 직후와 복원 직후에 보낸다`() {
        val manager = source("subscription/SubscriptionManager.kt")
        val restore = manager.substringAfter("private fun restoreConfig()").substringBefore("private fun logUsageIfDue()")
        assertFalse(restore, restore.contains("proClaim"))
        val cached = manager.substringAfter("val cachedConfig =").substringBefore("\n")
        assertFalse(cached, cached.contains("proClaim"))
        assertTrue(manager.contains("sendProClaimIfDue(force = fromPurchaseFlow)"))
        assertTrue(manager.contains("if (result == RestoreResult.RESTORED) sendProClaimIfDue(force = true)"))
        assertTrue(manager.contains("proClaimEnabled = config.proClaimEnabled"))
    }

    @Test
    fun `백그라운드 acknowledge 는 성공하거나 할 일이 없으면 끝내고 그 밖에는 다시 시도한다`() = runBlocking {
        var sent = 0
        val ok: suspend () -> Boolean = { sent++; true }
        val fail: suspend () -> Boolean = { sent++; false }

        assertEquals(PurchaseAckStep.DONE, purchaseAckStep(0, PurchaseAckTarget.NEEDS_ACK, ok))
        assertEquals(PurchaseAckStep.RETRY, purchaseAckStep(0, PurchaseAckTarget.NEEDS_ACK, fail))
        assertEquals(2, sent)
        // 이미 acknowledge 됐거나 구매가 사라졌으면 보내지 않고 끝낸다.
        assertEquals(PurchaseAckStep.DONE, purchaseAckStep(0, PurchaseAckTarget.ACKNOWLEDGED, fail))
        assertEquals(PurchaseAckStep.DONE, purchaseAckStep(0, PurchaseAckTarget.NOT_FOUND, fail))
        // 연결·조회 실패, 결제 대기는 다시 시도한다.
        assertEquals(PurchaseAckStep.RETRY, purchaseAckStep(0, null, fail))
        assertEquals(PurchaseAckStep.RETRY, purchaseAckStep(0, PurchaseAckTarget.PENDING, fail))
        assertEquals(2, sent)
        // 3일이 지나면 Play 가 이미 환불했으므로 끝낸다.
        assertEquals(PurchaseAckStep.DONE, purchaseAckStep(PURCHASE_ACK_RETRY_WINDOW_MS + 1, null, fail))
    }

    @Test
    fun `acknowledge 작업 이름은 구매마다 하나이고 토큰을 드러내지 않는다`() {
        val name = purchaseAckWorkName("secret-token-abc")
        assertEquals(name, purchaseAckWorkName("secret-token-abc"))
        assertFalse(name == purchaseAckWorkName("other-token"))
        assertFalse(name.contains("secret"))
        assertTrue(name.startsWith("purchase-ack-"))
    }

    @Test
    fun `앱 안 acknowledge 가 다시 시도까지 실패하면 백그라운드 작업을 맡긴다`() {
        val manager = source("subscription/SubscriptionManager.kt")
        val failure = manager.substringAfter("onFailure = { token ->").substringBefore("},")
        assertTrue(failure, failure.contains("enqueuePurchaseAcknowledgeWork(context, token)"))
        val worker = source("subscription/PurchaseAcknowledgeWorker.kt")
        assertTrue(worker.contains("setRequiredNetworkType(NetworkType.CONNECTED)"))
        assertTrue(worker.contains("BackoffPolicy.EXPONENTIAL"))
        assertTrue(worker.contains("ExistingWorkPolicy.KEEP"))
    }

    @Test
    fun `이미 가진 구독을 다시 사려 한 실패는 안내 대신 구매를 다시 조회한다`() {
        assertTrue(purchaseFailureRefreshesPurchases(com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED))
        assertFalse(purchaseFailureRefreshesPurchases(com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR))
        assertFalse(purchaseFailureRefreshesPurchases(com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE))
    }

    @Test
    fun `claim 은 원격 플래그가 켜졌거나 디버그 강제값이 켜졌을 때만 켠다`() {
        assertFalse(effectiveProClaimEnabled(remoteEnabled = false, debugForced = false))
        assertTrue(effectiveProClaimEnabled(remoteEnabled = true, debugForced = false))
        assertTrue(effectiveProClaimEnabled(remoteEnabled = false, debugForced = true))
    }

    @Test
    fun `릴리스 빌드는 빌드 속성과 관계없이 claim 강제값이 꺼져 있다`() {
        // testReleaseUnitTest 에서는 릴리스 BuildConfig 로 돈다(D-40: 운영은 원격 플래그로만 켠다).
        if (!com.ScienceFiction.DronePassAndroid.BuildConfig.DEBUG) {
            assertFalse(com.ScienceFiction.DronePassAndroid.BuildConfig.PRO_CLAIM_FORCE_ENABLED)
        }
        val gradle = listOf("build.gradle.kts", "app/build.gradle.kts")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isFile && it.readText().contains("buildTypes") }
            .readText()
        val release = gradle.substringAfter("        release {").substringBefore("        }")
        assertTrue(release, release.contains("""buildConfigField("boolean", "PRO_CLAIM_FORCE_ENABLED", "false")"""))
        val debug = gradle.substringAfter("        debug {").substringBefore("        release {")
        assertTrue(debug, debug.contains("""providers.gradleProperty("proClaimDebug").orNull == "true""""))
        val manager = source("subscription/SubscriptionManager.kt")
        assertTrue(manager.contains("effectiveProClaimEnabled(proClaimEnabled, BuildConfig.PRO_CLAIM_FORCE_ENABLED)"))
    }

    @Test
    fun `claim 응답에서 결과 종류만 꺼낸다`() {
        assertEquals("recorded", claimResultLabel(mapOf("ok" to true, "result" to "recorded")))
        assertEquals("unknown", claimResultLabel(mapOf("ok" to true)))
        assertEquals("unknown", claimResultLabel(null))
    }

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }
}
