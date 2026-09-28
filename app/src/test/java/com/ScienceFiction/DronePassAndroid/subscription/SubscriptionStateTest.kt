package com.ScienceFiction.DronePassAndroid.subscription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionStateTest {
    @Test fun `paid cache protects Pro only before its expiry`() {
        val cached = PlanStatus(EntitlementState.PRO, ProSource.SUBSCRIPTION, proUntilMillis = 1_000L)
        assertTrue(cached.isKnownAndValid(999L))
        assertFalse(cached.isKnownAndValid(1_000L))
        assertTrue(PlanStatus(EntitlementState.PRO, ProSource.LEGACY, LegacyKind.EARLY_ACCESS).isKnownAndValid(Long.MAX_VALUE))
        assertFalse(PlanStatus().isKnownAndValid(0L))
    }

    @Test fun `suspended paid subscription still needs cancellation notice`() {
        val onHold = PlanStatus(EntitlementState.FREE, paymentIssue = true, hasPaidSubscription = true)
        assertTrue(onHold.isPaidSubscriber)
        assertFalse(PlanStatus(EntitlementState.PRO, ProSource.LEGACY, LegacyKind.EARLY_ACCESS).isPaidSubscriber)
    }

    @Test fun `remote config rejects fractional and incomplete sections`() {
        assertNull(RemoteSubscriptionConfig.parse("""{"schemaVersion":1.5}"""))
        val config = RemoteSubscriptionConfig.parse("""{"schemaVersion":1,"free":{"shapes":100.5,"sketches":300,"drones":3},"legacy":{"iosOriginalBuildBefore":102,"accountCreatedBefore":"2026-09-22T00:00:00Z"}}""")!!
        assertNull(config.limits)
        assertTrue(config.legacyCutoff == LegacyCutoff.fallback)
    }
}
