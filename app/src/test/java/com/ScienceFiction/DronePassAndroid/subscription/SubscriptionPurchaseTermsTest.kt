package com.ScienceFiction.DronePassAndroid.subscription

import com.ScienceFiction.DronePassAndroid.core.util.mailtoUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** 사양 v2 C: 구매 조건(D-52 로그인 선택), 계약 내용의 날짜, 해지·환불 요청 유형. */
class SubscriptionPurchaseTermsTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun `purchase needs a finished plan check but not sign-in`() {
        // D-52(App Review 5.1.1(v)): 로그인은 선택이다. 플랜 확인 중 차단만 남긴다.
        assertFalse(canStartPurchase(EntitlementState.UNKNOWN))
        assertTrue(canStartPurchase(EntitlementState.FREE))
    }

    @Test
    fun `signed-out paywall keeps the purchase button first and adds the sign-in notice below it`() {
        val paywall = source("subscription/SubscriptionPaywall.kt")
        assertFalse(paywall.contains("subscription_sign_in_to_subscribe"))
        assertFalse(paywall.contains("!signedIn -> PaywallPrimaryButton"))
        val button = paywall.indexOf("text = stringResource(R.string.subscription_subscribe)")
        val notice = paywall.indexOf("GuestPurchaseNotice(\n")
        assertTrue(button >= 0 && notice > button)
        assertTrue(paywall.contains("if (!signedIn) {"))

        val manager = source("subscription/SubscriptionManager.kt")
        // 계정 연결 토큰은 로그인 상태의 구매에만 붙인다(계약 §8).
        assertTrue(manager.contains("auth.currentUser?.uid?.let { builder.setObfuscatedAccountId(appAccountToken(it)) }"))
        assertTrue(manager.contains("if (!canStartPurchase(entitlement = _status.value.entitlement)) return"))
        // "이 기기에서 로그인한 적 있음"은 로그아웃해도 남긴다.
        assertFalse(manager.contains("remove(KEY_SIGNED_IN_ON_DEVICE)"))
        assertFalse(manager.contains("preferences.edit().clear()"))
        assertFalse(source("core/account/AccountSessionFlows.kt").contains("getSharedPreferences(\"subscription\""))
    }

    @Test
    fun `lifetime cutoff on the paywall is the Korean date of the remote cutoff`() {
        val cutoff = LegacyCutoff.fallback.accountCreatedBefore // 2026-11-05T15:00:00Z = 11/6 00:00 KST

        assertEquals("2026년 11월 6일", legacyCutoffDisplayDate(cutoff, Locale.KOREAN))
        assertEquals("November 6, 2026", legacyCutoffDisplayDate(cutoff, Locale.ENGLISH))
        assertEquals("2026년 12월 1일", legacyCutoffDisplayDate(Instant.parse("2026-11-30T15:00:00Z"), Locale.KOREAN))
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

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }
}
