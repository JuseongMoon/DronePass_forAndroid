package com.ScienceFiction.DronePassAndroid.subscription

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

enum class EntitlementState { UNKNOWN, FREE, PRO }
enum class QuotaLimitKind { SHAPES, SKETCHES, DRONES }
enum class QuotaAction(val limitKind: QuotaLimitKind) {
    CREATE_SHAPE(QuotaLimitKind.SHAPES),
    DUPLICATE_SHAPE(QuotaLimitKind.SHAPES),
    START_SKETCH_STROKE(QuotaLimitKind.SKETCHES),
    ADD_DRONE(QuotaLimitKind.DRONES),
}

data class QuotaLimits(val freeShapes: Int, val freeSketches: Int, val freeDrones: Int) {
    fun limit(forKind: QuotaLimitKind): Int = when (forKind) {
        QuotaLimitKind.SHAPES -> freeShapes
        QuotaLimitKind.SKETCHES -> freeSketches
        QuotaLimitKind.DRONES -> freeDrones
    }

    fun raised(by: QuotaLimits?): QuotaLimits = by?.let {
        QuotaLimits(maxOf(freeShapes, it.freeShapes), maxOf(freeSketches, it.freeSketches), maxOf(freeDrones, it.freeDrones))
    } ?: this

    companion object { val fallback = QuotaLimits(30, 100, 3) }
}

sealed interface QuotaDecision {
    data object Allowed : QuotaDecision
    data class Blocked(val kind: QuotaLimitKind) : QuotaDecision
}

object QuotaPolicy {
    fun countsTowardLimit(deletedAt: Long?): Boolean = deletedAt == null
    fun shapeCount(deletedAts: Iterable<Long?>): Int = deletedAts.count(::countsTowardLimit)
    fun evaluate(action: QuotaAction, currentCount: () -> Int, entitlement: EntitlementState, limits: QuotaLimits): QuotaDecision {
        if (entitlement != EntitlementState.FREE) return QuotaDecision.Allowed
        return if (currentCount() >= limits.limit(action.limitKind)) QuotaDecision.Blocked(action.limitKind) else QuotaDecision.Allowed
    }
}

data class LegacyCutoff(val iosOriginalBuildBefore: Int, val accountCreatedBefore: Instant) {
    fun extended(by: LegacyCutoff?): LegacyCutoff = by?.let {
        LegacyCutoff(maxOf(iosOriginalBuildBefore, it.iosOriginalBuildBefore), maxOf(accountCreatedBefore, it.accountCreatedBefore))
    } ?: this

    companion object { val fallback = LegacyCutoff(102, Instant.parse("2026-11-05T15:00:00Z")) }
}

enum class LegacyKind { EARLY_ACCESS, ORIGINAL_DOWNLOAD }

object LegacyPolicy {
    /**
     * [isKnownSandbox] 는 iOS 가 StoreKit 환경을 sandbox·xcode 로 **확인한** 경우만 true 다(심사·TestFlight 에서
     * 기준일 전 계정으로 구매 버튼이 사라지지 않게 계정 경로를 건너뛴다). 환경을 알 수 없으면 false 로 계정 경로를
     * 유지한다. Android 는 환경을 알 수 없으므로 항상 false 다.
     */
    fun legacyKind(
        originalAppVersion: String?,
        isProductionEnvironment: Boolean,
        accountCreatedAt: Instant?,
        cutoff: LegacyCutoff,
        isKnownSandbox: Boolean = false,
    ): LegacyKind? {
        if (!isKnownSandbox && accountCreatedAt != null && accountCreatedAt < cutoff.accountCreatedBefore) return LegacyKind.EARLY_ACCESS
        val build = originalAppVersion?.trim()?.toIntOrNull()
        return if (isProductionEnvironment && build != null && build < cutoff.iosOriginalBuildBefore) LegacyKind.ORIGINAL_DOWNLOAD else null
    }

    fun isLegacyUser(
        originalAppVersion: String?,
        isProductionEnvironment: Boolean,
        accountCreatedAt: Instant?,
        cutoff: LegacyCutoff,
        isKnownSandbox: Boolean = false,
    ): Boolean = legacyKind(originalAppVersion, isProductionEnvironment, accountCreatedAt, cutoff, isKnownSandbox) != null
}

fun isVersionOlder(current: String, minimum: String): Boolean {
    val lhs = current.split('.').map { it.toIntOrNull() ?: 0 }
    val rhs = minimum.split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(lhs.size, rhs.size)) {
        val left = lhs.getOrElse(index) { 0 }
        val right = rhs.getOrElse(index) { 0 }
        if (left != right) return left < right
    }
    return false
}

fun appAccountToken(uid: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest("dronepass:$uid".toByteArray(Charsets.UTF_8)).copyOfRange(0, 16)
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x50).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    val high = java.nio.ByteBuffer.wrap(bytes, 0, 8).long
    val low = java.nio.ByteBuffer.wrap(bytes, 8, 8).long
    return UUID(high, low).toString().uppercase(java.util.Locale.ROOT)
}

fun countBucket(count: Int): String = when {
    count < 10 -> "0-9"
    count < 50 -> "10-49"
    count < 90 -> "50-89"
    count < 100 -> "90-99"
    count < 200 -> "100-199"
    count < 300 -> "200-299"
    count < 500 -> "300-499"
    else -> "500+"
}
