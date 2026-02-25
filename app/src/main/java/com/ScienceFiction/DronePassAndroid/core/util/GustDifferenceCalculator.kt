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
 * Floor 체크: 평균풍 < 최소평균풍(1.0) -> 국지 돌풍 정책 적용
 */
object GustDifferenceCalculator {

    // 카테고리별 임계값 데이터 클래스
    private data class Thresholds(
        val windCaution: Double,
        val windDanger: Double,
        val gustDiffCaution: Double,
        val gustDiffDanger: Double,
        val gfCaution: Double,
        val gfDanger: Double
    )

    private val thresholdsMap = mapOf(
        DroneCategory.TOY to Thresholds(
            windCaution = 7.0, windDanger = 9.0,
            gustDiffCaution = 5.0, gustDiffDanger = 7.0,
            gfCaution = 1.35, gfDanger = 1.55
        ),
        DroneCategory.CLASS4 to Thresholds(
            windCaution = 8.5, windDanger = 10.5,
            gustDiffCaution = 6.0, gustDiffDanger = 8.5,
            gfCaution = 1.40, gfDanger = 1.60
        ),
        DroneCategory.CLASS3 to Thresholds(
            windCaution = 10.0, windDanger = 12.0,
            gustDiffCaution = 7.0, gustDiffDanger = 9.5,
            gfCaution = 1.40, gfDanger = 1.60
        ),
        DroneCategory.CLASS2 to Thresholds(
            windCaution = 12.0, windDanger = 15.0,
            gustDiffCaution = 9.0, gustDiffDanger = 12.0,
            gfCaution = 1.45, gfDanger = 1.65
        )
    )

    /** 최소 평균풍 (Floor 체크용) */
    private const val MIN_SUSTAINED_WIND = 1.0

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

        // 돌풍 추정: 실제 돌풍 없으면 평균풍 x 1.3
        val effectiveGust = gustWind ?: (sustainedWind * 1.3)
        val gustDifference = effectiveGust - sustainedWind

        // 하드-스톱: 돌풍 >= (위험+0.8) 또는 평균풍 >= (위험+0.8) -> 즉시 DANGER
        if (effectiveGust >= thresholds.windDanger + 0.8 ||
            sustainedWind >= thresholds.windDanger + 0.8
        ) {
            return GustDifferenceLevel.DANGER
        }

        // Floor 체크: 평균풍 < 최소평균풍 -> 국지 돌풍 정책
        if (sustainedWind < MIN_SUSTAINED_WIND) {
            return if (effectiveGust >= thresholds.gustDiffCaution) {
                GustDifferenceLevel.LOCALIZED_GUST
            } else {
                GustDifferenceLevel.SAFE
            }
        }

        // 3축 평가
        // 축A: 순간 풍속 증가량
        val axisA = evaluateAxis(gustDifference, thresholds.gustDiffCaution, thresholds.gustDiffDanger)

        // 축B: Gust Factor (평균풍 >= 1.0 일 때만)
        val gustFactor = if (sustainedWind >= 1.0) effectiveGust / sustainedWind else 1.0
        val axisB = evaluateAxis(gustFactor, thresholds.gfCaution, thresholds.gfDanger)

        // 축C: 절대값 (평균풍 또는 돌풍 중 큰 값)
        val maxWind = maxOf(sustainedWind, effectiveGust)
        val axisC = evaluateAxis(maxWind, thresholds.windCaution, thresholds.windDanger)

        // 2-out-of-3 투표
        val axes = listOf(axisA, axisB, axisC)
        val dangerCount = axes.count { it == GustDifferenceLevel.DANGER }
        val cautionOrAboveCount = axes.count {
            it == GustDifferenceLevel.DANGER || it == GustDifferenceLevel.CAUTION
        }

        return when {
            dangerCount >= 2 -> GustDifferenceLevel.DANGER
            cautionOrAboveCount >= 2 -> GustDifferenceLevel.CAUTION
            else -> GustDifferenceLevel.SAFE
        }
    }

    /**
     * 풍속과 돌풍 차이 계산 (편의 메서드)
     */
    fun calculateGustDifference(sustainedWind: Double, gustWind: Double?): Double {
        val effectiveGust = gustWind ?: (sustainedWind * 1.3)
        return effectiveGust - sustainedWind
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
