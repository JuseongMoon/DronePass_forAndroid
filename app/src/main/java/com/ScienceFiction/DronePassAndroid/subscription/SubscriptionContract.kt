package com.ScienceFiction.DronePassAndroid.subscription

import com.ScienceFiction.DronePassAndroid.core.legal.formatLegalDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/** 지금 유효한 Google Play 구독의 주문 정보. */
data class SubscriptionPurchaseInfo(
    val orderId: String?,
    /** Google Play 가 알려 주는 구매 시각(연 구독의 시작 시각). */
    val purchaseTimeMillis: Long,
    val autoRenewing: Boolean,
)

/**
 * 구매를 시작할 수 있는지: 플랜 확인이 끝났다(사양 v2 C-2). 로그인은 선택이다(D-52, App Review 5.1.1(v)).
 * 플랜 확인 중에는 평생 무료 대상자의 실수 결제를 막는다.
 */
internal fun canStartPurchase(entitlement: EntitlementState): Boolean =
    entitlement != EntitlementState.UNKNOWN

/**
 * 결제 화면에 보여 줄 평생 무료 기준일(이 시각 전에 가입한 계정). 원격 설정값을 한국 날짜로 바꾼다.
 * 2026-11-05T15:00:00Z → "2026년 11월 6일"(한국어) / "November 6, 2026"(영어).
 */
internal fun legacyCutoffDisplayDate(accountCreatedBefore: Instant, locale: Locale): String =
    formatLegalDate(accountCreatedBefore.atZone(LegalTimeZone).toLocalDate().toString(), locale)

private val LegalTimeZone: ZoneId = ZoneId.of("Asia/Seoul")

/** 연 구독의 마지막 결제(최초 구매 또는 가장 최근 갱신) 시각. */
internal fun lastYearlyPaymentMillis(purchaseTimeMillis: Long, nowMillis: Long, zone: ZoneId): Long {
    val start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(purchaseTimeMillis), zone)
    val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
    var years = ChronoUnit.YEARS.between(start, now).coerceAtLeast(0)
    while (years > 0 && start.plusYears(years).isAfter(now)) years -= 1
    return start.plusYears(years).toInstant().toEpochMilli()
}

/** 다음 갱신 예정일(마지막 결제 + 1년). */
internal fun nextYearlyRenewalMillis(purchaseTimeMillis: Long, nowMillis: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(lastYearlyPaymentMillis(purchaseTimeMillis, nowMillis, zone))
        .atZone(zone).plusYears(1).toInstant().toEpochMilli()

enum class RefundRequestType {
    /** 결제·자동 갱신 후 7일 이내: 사용 여부와 관계없이 전액 환불. */
    FULL_WITHIN_7_DAYS,

    /** 그 뒤 해지: 남은 기간만큼 환급. */
    PRORATED,
}

internal const val FullRefundWindowDays = 7L

internal fun refundRequestType(lastPaymentMillis: Long, nowMillis: Long): RefundRequestType =
    if (nowMillis - lastPaymentMillis <= FullRefundWindowDays * 24 * 60 * 60 * 1000) {
        RefundRequestType.FULL_WITHIN_7_DAYS
    } else {
        RefundRequestType.PRORATED
    }

internal const val SupportEmail = "support@sciencefiction.co.kr"
