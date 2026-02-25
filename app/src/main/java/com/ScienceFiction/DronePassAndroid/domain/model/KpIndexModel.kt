package com.ScienceFiction.DronePassAndroid.domain.model

/**
 * Kp 지수 데이터
 *
 * @param timeTag 시간 태그 (예: "2026-02-24 12:00:00")
 * @param kp Kp 지수 값 (0~9)
 * @param observed 관측 데이터 구분 (observed, estimated, predicted)
 */
data class KpIndexData(
    val timeTag: String,
    val kp: Double,
    val observed: String? = null
)

/**
 * Kp 지수 수준 (지자기 폭풍 수준)
 *
 * @param label 표시 라벨
 * @param color ARGB 색상
 */
/**
 * 27일 장기 예보 데이터
 *
 * @param date 날짜 문자열 (예: "2026 Feb 25")
 * @param kp 예측 Kp 지수
 * @param ap 예측 Ap 지수
 */
data class Kp27DayForecast(
    val date: String,
    val kp: Double,
    val ap: Int
)

enum class KpLevel(val label: String, val color: Long) {
    NORMAL("Normal", 0xFF008B8B),
    G1("G1 Minor", 0xFF2E8B57),
    G2("G2 Moderate", 0xFFDAA520),
    G3("G3 Strong", 0xFFFF8C00),
    G4("G4 Severe", 0xFFFF4500),
    G5("G5 Extreme", 0xFFDC143C);

    companion object {
        /**
         * Kp 값에 따른 수준 판정
         */
        fun fromKp(kp: Double): KpLevel = when {
            kp < 5.0 -> NORMAL
            kp < 6.0 -> G1
            kp < 7.0 -> G2
            kp < 8.0 -> G3
            kp < 9.0 -> G4
            else -> G5
        }
    }
}
