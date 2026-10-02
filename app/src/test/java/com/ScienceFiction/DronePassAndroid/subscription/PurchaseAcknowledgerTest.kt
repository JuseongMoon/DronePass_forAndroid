package com.ScienceFiction.DronePassAndroid.subscription

import org.junit.Assert.assertEquals
import org.junit.Test

/** 105 점검: 성공한 결제 직후 "구매 확인에 실패" 가 뜨던 회귀. */
class PurchaseAcknowledgerTest {

    /** 응답을 바로 주지 않고 쌓아 두었다가 테스트가 원하는 순서로 돌려준다. */
    private class FakeBilling {
        val sends = mutableListOf<(Boolean) -> Unit>()
        val checks = mutableListOf<(AcknowledgeCheck) -> Unit>()
        var failures = 0

        val acknowledger = PurchaseAcknowledger(
            send = { _, onResult -> sends += onResult },
            check = { _, onResult -> checks += onResult },
            onFailure = { failures++ },
        )
    }

    @Test
    fun `구매 리스너와 onResume 이 같은 토큰으로 동시에 들어와도 acknowledge 는 한 번만 보낸다`() {
        val billing = FakeBilling()

        billing.acknowledger.acknowledge("token")
        billing.acknowledger.acknowledge("token")

        assertEquals(1, billing.sends.size)
        billing.sends.single()(true)
        assertEquals(0, billing.failures)
    }

    @Test
    fun `첫 응답이 실패여도 다시 조회해 이미 acknowledge 됐으면 메시지를 띄우지 않는다`() {
        val billing = FakeBilling()

        billing.acknowledger.acknowledge("token")
        billing.sends.single()(false)
        billing.checks.single()(AcknowledgeCheck.DONE)

        assertEquals(1, billing.sends.size)
        assertEquals(0, billing.failures)
    }

    @Test
    fun `아직 acknowledge 되지 않았으면 한 번 더 보내고 그것이 성공하면 메시지가 없다`() {
        val billing = FakeBilling()

        billing.acknowledger.acknowledge("token")
        billing.sends[0](false)
        billing.checks.single()(AcknowledgeCheck.NOT_DONE)
        billing.sends[1](true)

        assertEquals(2, billing.sends.size)
        assertEquals(0, billing.failures)
    }

    @Test
    fun `조회를 못 해도 한 번 더 보내고 재시도까지 실패해야 메시지를 한 번 띄운다`() {
        val billing = FakeBilling()

        billing.acknowledger.acknowledge("token")
        billing.sends[0](false)
        billing.checks.single()(AcknowledgeCheck.UNKNOWN)
        billing.sends[1](false)

        assertEquals(2, billing.sends.size)
        assertEquals(1, billing.checks.size)
        assertEquals(1, billing.failures)
    }

    @Test
    fun `실패로 끝나도 다음 onResume 재조회가 같은 토큰을 다시 acknowledge 할 수 있다`() {
        val billing = FakeBilling()
        billing.acknowledger.acknowledge("token")
        billing.sends[0](false)
        billing.checks.single()(AcknowledgeCheck.NOT_DONE)
        billing.sends[1](false)

        billing.acknowledger.acknowledge("token")

        assertEquals(3, billing.sends.size)
    }

    @Test
    fun `구독 매니저는 모든 acknowledge 를 이 경로로 보내고 구매 재조회마다 다시 시도한다`() {
        val relative = "src/main/java/com/ScienceFiction/DronePassAndroid/subscription/SubscriptionManager.kt"
        val userDir = java.io.File(requireNotNull(System.getProperty("user.dir")))
        val source = listOf(relative, "app/$relative").map { java.io.File(userDir, it) }.first { it.exists() }.readText()

        // onResume·앱 시작의 refreshPurchases → applyPurchases 가 acknowledge 되지 않은 구매를 다시 넘긴다.
        val apply = source.substringAfter("private fun applyPurchases(")
        org.junit.Assert.assertTrue(apply.substringBefore("private fun acknowledge(").contains("acknowledge(active)"))
        val acknowledge = source.substringAfter("private fun acknowledge(").substringBefore("private fun sendAcknowledge(")
        org.junit.Assert.assertTrue(acknowledge.contains("if (purchase.isAcknowledged) return"))
        org.junit.Assert.assertTrue(acknowledge.contains("acknowledger.acknowledge(purchase.purchaseToken)"))
        assertEquals(1, Regex("""billing\.acknowledgePurchase\(""").findAll(source).count())
    }

    @Test
    fun `다른 토큰은 서로 막지 않는다`() {
        val billing = FakeBilling()

        billing.acknowledger.acknowledge("a")
        billing.acknowledger.acknowledge("b")

        assertEquals(2, billing.sends.size)
    }
}
