package com.ScienceFiction.DronePassAndroid.core.util

import kotlin.math.roundToInt

/**
 * 고도 단위 열거형
 *
 * 항공 고도 표기에서 사용되는 다양한 단위를 정의합니다.
 */
enum class AltitudeUnit(val label: String, val description: String) {
    /** Above Mean Sea Level - 평균 해수면 */
    AMSL("AMSL", "평균 해수면"),
    /** Above Ground Level - 지상 기준 */
    AGL("AGL", "지상 기준"),
    /** Mean Sea Level - 평균 해수면 */
    MSL("MSL", "평균 해수면"),
    /** Flight Level - 비행고도층 */
    FL("FL", "비행고도층"),
    /** Feet Height - 높이 */
    FT_HEIGHT("FT HEI", "높이"),
    /** Feet Altitude - 고도 */
    FT_ALT("FT ALT", "고도"),
    /** Unlimited - 제한없음 */
    UNL("UNL", "제한없음"),
    /** Ground - 지상 */
    GND("GND", "지상"),
    /** Surface - 표면 */
    SFC("SFC", "표면"),
    /** 알 수 없음 */
    UNKNOWN("", "")
}

/**
 * 파싱된 고도 정보
 *
 * @param rawValue 원본 문자열
 * @param numericValue 숫자 값 (없으면 null)
 * @param unit 고도 단위
 * @param meters 미터 환산 값 (환산 불가능하면 null)
 */
data class ParsedAltitude(
    val rawValue: String,
    val numericValue: Double?,
    val unit: AltitudeUnit,
    val meters: Double?
)

/**
 * 항공 고도 문자열을 파싱하고 포맷팅하는 유틸리티
 *
 * iOS의 AltitudeFormatter를 Kotlin으로 포팅한 것입니다.
 *
 * 지원 형식 예시:
 * - "3 000 AGL" -> "3 000 AGL (914.4m, 지상 기준)"
 * - "FL100" -> "FL100 (3048m, 비행고도층)"
 * - "UNL" -> "UNL (제한없음)"
 * - "GND" / "SFC" -> "GND (지상)" / "SFC (표면)"
 * - "1500FT AMSL" -> "1500FT AMSL (457.2m, 평균 해수면)"
 */
object AltitudeFormatter {

    /** 1피트 = 0.3048미터 */
    private const val FEET_TO_METERS = 0.3048

    /** 공백 제거 패턴 — 매 호출 컴파일 회피용 캐시 */
    private val WHITESPACE_PATTERN = Regex("\\s")

    /**
     * 고도 문자열을 파싱하여 ParsedAltitude를 반환합니다.
     *
     * @param altitudeString 고도 원본 문자열 (예: "3 000 AGL", "FL100", "UNL")
     * @return ParsedAltitude, 입력이 null이거나 빈 문자열이면 null
     */
    fun parse(altitudeString: String?): ParsedAltitude? {
        val original = altitudeString?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val upperValue = original.uppercase()

        // 특수 단위 우선 처리
        if (upperValue == "UNL") {
            return ParsedAltitude(
                rawValue = original,
                numericValue = null,
                unit = AltitudeUnit.UNL,
                meters = null
            )
        }
        if (upperValue == "GND") {
            return ParsedAltitude(
                rawValue = original,
                numericValue = 0.0,
                unit = AltitudeUnit.GND,
                meters = 0.0
            )
        }
        if (upperValue == "SFC") {
            return ParsedAltitude(
                rawValue = original,
                numericValue = 0.0,
                unit = AltitudeUnit.SFC,
                meters = 0.0
            )
        }

        var unit = AltitudeUnit.UNKNOWN
        var numericPart = upperValue

        when {
            upperValue.contains("FT HEI") || upperValue.contains("FTHEI") -> {
                unit = AltitudeUnit.FT_HEIGHT
                numericPart = upperValue
                    .replace("FT HEI", "")
                    .replace("FTHEI", "")
            }
            upperValue.contains("FT ALT") || upperValue.contains("FTALT") -> {
                unit = AltitudeUnit.FT_ALT
                numericPart = upperValue
                    .replace("FT ALT", "")
                    .replace("FTALT", "")
            }
            upperValue.contains("AMSL") -> {
                unit = AltitudeUnit.AMSL
                numericPart = upperValue
                    .replace("FT", "")
                    .replace("AMSL", "")
            }
            upperValue.contains("AGL") -> {
                unit = AltitudeUnit.AGL
                numericPart = upperValue
                    .replace("FT", "")
                    .replace("AGL", "")
            }
            upperValue.contains("MSL") -> {
                unit = AltitudeUnit.MSL
                numericPart = upperValue
                    .replace("FT", "")
                    .replace("MSL", "")
            }
            upperValue.startsWith("FL") -> {
                unit = AltitudeUnit.FL
                numericPart = upperValue.replace("FL", "")
            }
        }

        val cleanedNumeric = numericPart
            .replace(WHITESPACE_PATTERN, "")
            .trim()
        cleanedNumeric.toDoubleOrNull()?.let { numericValue ->
            return ParsedAltitude(
                rawValue = original,
                numericValue = numericValue,
                unit = unit,
                meters = convertToMeters(numericValue, unit)
            )
        }

        // 파싱 실패 시 원본 그대로 반환
        return ParsedAltitude(
            rawValue = original,
            numericValue = null,
            unit = AltitudeUnit.UNKNOWN,
            meters = null
        )
    }

    /**
     * 고도 문자열을 사용자 친화적인 형식으로 포맷팅합니다.
     *
     * @param altitudeString 고도 원본 문자열
     * @return 포맷팅된 문자열, 입력이 null이면 null
     *
     * 예시:
     * - "3 000 AGL" -> "3 000 AGL (914.4m, 지상 기준)"
     * - "FL100" -> "FL100 (3048m, 비행고도층)"
     * - "UNL" -> "UNL (제한없음)"
     * - "GND" -> "GND (지상)"
     * - "SFC" -> "SFC (표면)"
     */
    fun format(altitudeString: String?): String? {
        val parsed = parse(altitudeString) ?: return null

        return when (parsed.unit) {
            AltitudeUnit.UNL, AltitudeUnit.GND, AltitudeUnit.SFC ->
                "${parsed.rawValue} (${parsed.unit.description})"
            else -> parsed.meters?.let { meters ->
                "${parsed.rawValue} (${formatMetersIos(meters)}m, ${parsed.unit.description})"
            } ?: parsed.rawValue
        }
    }

    /**
     * ft 값을 포맷된 문자열로 변환 (기존 호환성 유지)
     *
     * @param ft 고도(피트)
     * @param isAgl 지상 기준 여부 (true=AGL, false=MSL)
     */
    fun format(ft: Double, isAgl: Boolean = true): String {
        val ftInt = ft.roundToInt()
        val meters = (ft * FEET_TO_METERS).roundToInt()
        val formattedFt = formatNumberWithSpace(ftInt)
        val reference = if (isAgl) "AGL" else "MSL"
        val referenceKor = if (isAgl) "지상 기준" else "해수면 기준"
        return "${formattedFt}ft $reference (${meters}m, $referenceKor)"
    }

    /**
     * 피트 또는 Flight Level 값을 미터로 변환합니다.
     *
     * @param value 숫자 값 (피트 또는 FL 숫자)
     * @param unit 고도 단위
     * @return 미터 값, 변환 불가 시 null
     */
    fun convertToMeters(value: Double, unit: AltitudeUnit): Double? {
        return when (unit) {
            AltitudeUnit.FL -> value * 100.0 * FEET_TO_METERS
            AltitudeUnit.AGL, AltitudeUnit.AMSL, AltitudeUnit.MSL,
            AltitudeUnit.FT_HEIGHT, AltitudeUnit.FT_ALT -> value * FEET_TO_METERS
            AltitudeUnit.GND, AltitudeUnit.SFC -> 0.0
            AltitudeUnit.UNL -> null
            AltitudeUnit.UNKNOWN -> 0.0
        }
    }

    /**
     * iOS AltitudeFormatter.ParsedAltitude.formattedString 과 같은 미터 표기.
     * 정수면 소수점 없이, 아니면 한 자리까지 표시한다.
     */
    private fun formatMetersIos(meters: Double): String {
        return if (meters % 1.0 == 0.0) {
            String.format("%.0f", meters)
        } else {
            String.format("%.1f", meters)
        }
    }

    /**
     * 천 단위 구분자(공백)를 사용하여 숫자 포맷 (기존 호환성 유지)
     */
    private fun formatNumberWithSpace(value: Int): String {
        val abs = kotlin.math.abs(value)
        val formatted = abs.toString().reversed().chunked(3).joinToString(" ").reversed()
        return if (value < 0) "-$formatted" else formatted
    }
}
