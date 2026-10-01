package com.ScienceFiction.DronePassAndroid.subscription

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** 지금 유효한 Google Play 구독의 주문 정보. */
data class SubscriptionPurchaseInfo(
    val orderId: String?,
    /** Google Play 가 알려 주는 구매 시각(연 구독의 시작 시각). */
    val purchaseTimeMillis: Long,
    val autoRenewing: Boolean,
)

/** 구매를 시작할 수 있는지: 로그인했고 플랜 확인이 끝났다(사양 v2 C-1·C-2). */
internal fun canStartPurchase(signedIn: Boolean, entitlement: EntitlementState): Boolean =
    signedIn && entitlement != EntitlementState.UNKNOWN

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
