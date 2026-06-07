package com.ScienceFiction.DronePassAndroid.core.util

/**
 * 돌풍 평가 알고리즘
 *
 * 3축 평가 + 2-out-of-3 투표 방식으로 돌풍 위험도를 판단합니다.
 * - 축A: 순간 풍속 증가량 (gustDifference = gust - sustained)
 * - 축B: Gust Factor (GF = gust / sustained, 평균풍 >= 1.0 일 때만)
 * - 축C: 절대값 (평균풍 또는 돌풍이 임계값 초과)
 *
 * 하드-스톱: 돌풍 >= (위험+0.8) 또는 평균풍 >= (위험+0.8) -> 즉시 DANGER
 * Floor 체크: 평균풍 < 등급별 최소평균풍 -> iOS 국지 돌풍 정책 적용
 */
object GustDifferenceCalculator {

    private data class Thresholds(
        val sustainedCaution: Double,
        val sustainedDanger: Double,
        val gustCaution: Double,
        val gustDanger: Double,
        val gustDiffCaution: Double,
        val gustDiffDanger: Double,
        val gfCaution: Double,
        val gfDanger: Double,
        val minimumSustainedWind: Double
    )

    private data class LocalizedGustPolicy(
        val diff: Double,
        val gf: Double,
        val absGust: Double,
        val votesRequired: Int,
        val minMeanForEval: Double = MIN_MEAN_FOR_GF
    )

    private val thresholdsMap = mapOf(
        DroneCategory.TOY to Thresholds(
            sustainedCaution = 7.0, sustainedDanger = 9.0,
            gustCaution = 9.0, gustDanger = 10.7,
            gustDiffCaution = 5.0, gustDiffDanger = 7.0,
            gfCaution = 1.35, gfDanger = 1.55,
            minimumSustainedWind = 4.0
        ),
        DroneCategory.CLASS4 to Thresholds(
            sustainedCaution = 8.5, sustainedDanger = 10.5,
            gustCaution = 10.5, gustDanger = 12.0,
            gustDiffCaution = 6.0, gustDiffDanger = 8.5,
            gfCaution = 1.40, gfDanger = 1.60,
            minimumSustainedWind = 5.0
        ),
        DroneCategory.CLASS3 to Thresholds(
            sustainedCaution = 10.0, sustainedDanger = 12.0,
            gustCaution = 12.0, gustDanger = 13.0,
            gustDiffCaution = 7.0, gustDiffDanger = 9.5,
            gfCaution = 1.40, gfDanger = 1.60,
            minimumSustainedWind = 6.0
        ),
        DroneCategory.CLASS2 to Thresholds(
            sustainedCaution = 12.0, sustainedDanger = 15.0,
            gustCaution = 15.0, gustDanger = 18.0,
            gustDiffCaution = 9.0, gustDiffDanger = 12.0,
            gfCaution = 1.45, gfDanger = 1.65,
            minimumSustainedWind = 7.0
        )
    )

    private val localizedPolicies = mapOf(
        DroneCategory.TOY to LocalizedGustPolicy(diff = 3.0, gf = 1.50, absGust = 8.0, votesRequired = 1),
        DroneCategory.CLASS4 to LocalizedGustPolicy(diff = 4.0, gf = 1.60, absGust = 9.5, votesRequired = 1),
        DroneCategory.CLASS3 to LocalizedGustPolicy(diff = 5.0, gf = 1.60, absGust = 10.5, votesRequired = 2),
        DroneCategory.CLASS2 to LocalizedGustPolicy(diff = 6.0, gf = 1.70, absGust = 12.0, votesRequired = 2)
    )

    private const val MIN_MEAN_FOR_GF = 1.0
    private const val GF_CAP = 4.0
    private const val HARD_STOP_MARGIN = 0.8
    private const val ESTIMATED_GF = 1.3
    private const val LOCALIZED_LOW_MEAN_MIN = 0.8
    private const val LOCALIZED_LOW_MEAN_MAX = 1.0
    private const val LOCALIZED_LOW_MEAN_DIFF_BOOST = 1.0

    /**
     * 돌풍 위험도 평가
     *
     * @param sustainedWind 평균풍(m/s)
     * @param gustWind 돌풍(m/s), null이면 추정값 사용
     * @param category 드론 카테고리
     * @return 평가된 GustDifferenceLevel
     */
    fun evaluate(
        sustainedWind: Double,
        gustWind: Double?,
        category: DroneCategory
    ): GustDifferenceLevel {
        val thresholds = thresholdsMap[category] ?: return GustDifferenceLevel.SAFE

        // 입력 유효성 가드: NaN/Infinity 입력은 평가 불가 → SAFE 로 보수적 분류하지 않고
        // 호출자가 명시적으로 "측정 불가" UI 분기를 처리하도록 SAFE 반환.
        // 음수 풍속은 API 데이터 오류로 간주하고 0으로 클램프 (실제 풍속은 음수일 수 없음).
        if (!sustainedWind.isFinite()) return GustDifferenceLevel.SAFE
        if (gustWind != null && !gustWind.isFinite()) return GustDifferenceLevel.SAFE
        val safeSustained = sustainedWind.coerceAtLeast(0.0)
        val safeGust = gustWind?.coerceAtLeast(0.0)

        // 돌풍 추정: 실제 돌풍 없으면 평균풍 x 1.3. 실측 돌풍은 iOS처럼 평균풍보다 낮지 않게 보정.
        val effectiveGust = safeGust?.coerceAtLeast(safeSustained) ?: (safeSustained * ESTIMATED_GF)
        // gustDifference 가 음수가 되는 경우(돌풍 < 평균풍, API 잡음) 0으로 클램프하여
        // evaluateAxis 가 비현실적 음수 입력으로 잘못 SAFE 판정하는 것을 방지.
        val gustDifference = (effectiveGust - safeSustained).coerceAtLeast(0.0)

        // 하드-스톱: 돌풍 >= (위험+0.8) 또는 평균풍 >= (위험+0.8) -> 즉시 DANGER
        if (effectiveGust >= thresholds.gustDanger + HARD_STOP_MARGIN ||
            safeSustained >= thresholds.sustainedDanger + HARD_STOP_MARGIN
        ) {
            return GustDifferenceLevel.DANGER
        }

        // Floor 체크: 평균풍 < 등급별 최소평균풍 -> iOS 국지 돌풍 전용 정책
        if (safeSustained < thresholds.minimumSustainedWind) {
            val policy = localizedPolicies[category] ?: return GustDifferenceLevel.SAFE
            val localizedDiffThreshold =
                if (safeSustained >= LOCALIZED_LOW_MEAN_MIN && safeSustained < LOCALIZED_LOW_MEAN_MAX) {
                    policy.diff + LOCALIZED_LOW_MEAN_DIFF_BOOST
                } else {
                    policy.diff
                }
            val gustFactor = calculateGustFactor(safeSustained, effectiveGust)
            val votes = listOf(
                gustDifference >= localizedDiffThreshold,
                safeSustained >= policy.minMeanForEval && gustFactor >= policy.gf,
                effectiveGust >= policy.absGust
            ).count { it }
            return if (votes >= policy.votesRequired) {
                GustDifferenceLevel.LOCALIZED_GUST
            } else {
                GustDifferenceLevel.SAFE
            }
        }

        // 3축 평가
        // 축A: 순간 풍속 증가량
        val axisA = evaluateAxis(gustDifference, thresholds.gustDiffCaution, thresholds.gustDiffDanger)

        // 축B: Gust Factor (평균풍 >= 1.0 일 때만)
        val gustFactor = calculateGustFactor(safeSustained, effectiveGust)
        val axisB = evaluateAxis(gustFactor, thresholds.gfCaution, thresholds.gfDanger)

        // 축C: 절대값. iOS는 평균풍과 돌풍 임계값을 분리해 평가한다.
        val axisC = when {
            safeSustained >= thresholds.sustainedDanger || effectiveGust >= thresholds.gustDanger ->
                GustDifferenceLevel.DANGER
            safeSustained >= thresholds.sustainedCaution || effectiveGust >= thresholds.gustCaution ->
                GustDifferenceLevel.CAUTION
            else -> GustDifferenceLevel.SAFE
        }

        // 2-out-of-3 투표
        val axes = listOf(axisA, axisB, axisC)
        val dangerCount = axes.count { it == GustDifferenceLevel.DANGER }
        val cautionOrAboveCount = axes.count {
            it == GustDifferenceLevel.DANGER || it == GustDifferenceLevel.CAUTION
        }

        return when {
            dangerCount >= 2 -> GustDifferenceLevel.DANGER
            dangerCount == 1 -> GustDifferenceLevel.CAUTION
            cautionOrAboveCount >= 2 -> GustDifferenceLevel.CAUTION
            else -> GustDifferenceLevel.SAFE
        }
    }

    /**
     * 풍속과 돌풍 차이 계산 (편의 메서드)
     */
    fun calculateGustDifference(sustainedWind: Double, gustWind: Double?): Double {
        val safeSustained = sustainedWind.coerceAtLeast(0.0)
        val effectiveGust = gustWind?.coerceAtLeast(safeSustained) ?: (safeSustained * ESTIMATED_GF)
        return (effectiveGust - safeSustained).coerceAtLeast(0.0)
    }

    private fun calculateGustFactor(sustainedWind: Double, gustWind: Double): Double =
        if (sustainedWind >= MIN_MEAN_FOR_GF) {
            (gustWind / sustainedWind).coerceAtMost(GF_CAP)
        } else {
            1.0
        }

    /**
     * 단일 축 평가
     */
    private fun evaluateAxis(
        value: Double,
        cautionThreshold: Double,
        dangerThreshold: Double
    ): GustDifferenceLevel {
        return when {
            value >= dangerThreshold -> GustDifferenceLevel.DANGER
            value >= cautionThreshold -> GustDifferenceLevel.CAUTION
            else -> GustDifferenceLevel.SAFE
        }
    }
}
