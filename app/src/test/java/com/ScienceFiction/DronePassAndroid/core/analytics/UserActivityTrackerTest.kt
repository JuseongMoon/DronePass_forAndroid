package com.ScienceFiction.DronePassAndroid.core.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserActivityTrackerTest {

    @Test
    fun `첫 인증 활동은 기록한다`() {
        assertTrue(shouldRecordUserActivity(lastRecordedAtMillis = null, nowMillis = 1_000L))
    }

    @Test
    fun `15분 이내 재활성화는 기록하지 않는다`() {
        assertFalse(
            shouldRecordUserActivity(
                lastRecordedAtMillis = 1_000L,
                nowMillis = 1_000L + USER_ACTIVITY_MIN_INTERVAL_MILLIS - 1,
            )
        )
    }

    @Test
    fun `15분 경계부터 다시 기록한다`() {
        assertTrue(
            shouldRecordUserActivity(
                lastRecordedAtMillis = 1_000L,
                nowMillis = 1_000L + USER_ACTIVITY_MIN_INTERVAL_MILLIS,
            )
        )
    }
}
