package com.ScienceFiction.DronePassAndroid.subscription

import com.ScienceFiction.DronePassAndroid.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 결제 연결 전에 구매 복원을 누르면 바로 "구독 없음"으로 답했고, 조회 중 다른 조회가 시작되면 답하지 않아
 * 복원 진행 표시가 멈췄다.
 */
class RestoreResultTest {

    @Test
    fun `restore failure is not reported as no subscription`() {
        assertEquals(R.string.subscription_restore_success, restoreResultMessageRes(RestoreResult.RESTORED))
        assertEquals(R.string.subscription_restore_not_found, restoreResultMessageRes(RestoreResult.NOT_FOUND))
        assertEquals(R.string.subscription_restore_failed, restoreResultMessageRes(RestoreResult.FAILED))
    }

    @Test
    fun `restore waits for the billing connection and always answers`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val manager = File(root, "com/ScienceFiction/DronePassAndroid/subscription/SubscriptionManager.kt").readText()
        val refresh = manager.substringAfter("fun refreshPurchases(").substringBefore("private fun applyPurchases(")
        val notReady = refresh.substringAfter("if (!billing.isReady) {").substringBefore("}")
        assertTrue(notReady.contains("pendingRestoreCallbacks::add"))
        assertFalse(notReady.contains("onRestored?.invoke("))
        // 세대가 바뀌어도 return 하지 않고 이 조회 결과로 답한다.
        assertFalse(refresh.contains("return@queryPurchasesAsync"))
        val connect = manager.substringAfter("private fun connectBilling()").substringBefore("private fun drainPendingRestoreCallbacks")
        assertEquals(2, Regex("""forEach \{ it\(RestoreResult\.FAILED\) \}""").findAll(connect).count())
    }
}
