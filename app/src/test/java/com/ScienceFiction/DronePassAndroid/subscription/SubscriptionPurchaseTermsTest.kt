package com.ScienceFiction.DronePassAndroid.subscription

import com.ScienceFiction.DronePassAndroid.core.util.mailtoUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** 사양 v2 C: 구매 조건, 계약 내용의 날짜, 해지·환불 요청 유형. */
class SubscriptionPurchaseTermsTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun `purchase needs sign-in and a finished plan check`() {
        assertFalse(canStartPurchase(signedIn = false, entitlement = EntitlementState.FREE))
        assertFalse(canStartPurchase(signedIn = true, entitlement = EntitlementState.UNKNOWN))
        assertTrue(canStartPurchase(signedIn = true, entitlement = EntitlementState.FREE))
    }

    @Test
    fun `yearly payment dates follow the purchase anniversary`() {
        val purchase = millis("2026-11-20T03:00:00Z")

        // 첫해: 마지막 결제는 구매일, 다음 갱신은 1년 뒤
        assertEquals(purchase, lastYearlyPaymentMillis(purchase, millis("2027-03-01T00:00:00Z"), seoul))
        assertEquals(millis("2027-11-20T03:00:00Z"), nextYearlyRenewalMillis(purchase, millis("2027-03-01T00:00:00Z"), seoul))
        // 갱신 직후: 마지막 결제는 갱신일
        assertEquals(millis("2027-11-20T03:00:00Z"), lastYearlyPaymentMillis(purchase, millis("2027-11-21T00:00:00Z"), seoul))
        assertEquals(millis("2028-11-20T03:00:00Z"), nextYearlyRenewalMillis(purchase, millis("2027-11-21T00:00:00Z"), seoul))
        // 갱신 하루 전: 아직 첫 결제
        assertEquals(purchase, lastYearlyPaymentMillis(purchase, millis("2027-11-19T03:00:00Z"), seoul))
    }

    @Test
    fun `refund type is full within 7 days of a payment and prorated after`() {
        val payment = millis("2026-11-20T03:00:00Z")

        assertEquals(RefundRequestType.FULL_WITHIN_7_DAYS, refundRequestType(payment, millis("2026-11-27T03:00:00Z")))
        assertEquals(RefundRequestType.PRORATED, refundRequestType(payment, millis("2026-11-27T03:00:01Z")))
    }

    @Test
    fun `remote flag for the location audit upload defaults to off`() {
        val base = """{"schemaVersion":1,"free":{"shapes":100,"sketches":300,"drones":3}"""
        assertFalse(RemoteSubscriptionConfig.parse("$base}")!!.locationAuditUploadEnabled)
        assertFalse(
            RemoteSubscriptionConfig.parse("""$base,"features":{"locationAudit":{"uploadEnabled":"yes"}}}""")!!.locationAuditUploadEnabled,
        )
        assertTrue(
            RemoteSubscriptionConfig.parse("""$base,"features":{"locationAudit":{"uploadEnabled":true}}}""")!!.locationAuditUploadEnabled,
        )
    }

    @Test
    fun `mail drafts go to customer support with an encoded subject`() {
        val uri = mailtoUri(SupportEmail, "[DronePass] 구독 해지·환불 요청", "플랫폼: Android")

        assertTrue(uri.startsWith("mailto:support@sciencefiction.co.kr?subject="))
        assertFalse(uri.contains(" "))
        assertTrue(uri.contains("&body="))
    }
}
