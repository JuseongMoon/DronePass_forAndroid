package com.ScienceFiction.DronePassAndroid.core.util

import kotlin.math.roundToInt

/**
 * 고도 단위 열거형
 *
 * 항공 고도 표기에서 사용되는 다양한 단위를 정의합니다.
 */
enum class AltitudeUnit(val label: String, val description: String) {
    /** Above Mean Sea Level - 평균해수면 기준 */
    AMSL("AMSL", "평균해수면 기준"),
    /** Above Ground Level - 지상 기준 */
    AGL("AGL", "지상 기준"),
    /** Mean Sea Level - 해수면 기준 */
    MSL("MSL", "해수면 기준"),
    /** Flight Level - 비행고도 (기압고도 100ft 단위) */
    FL("FL", "비행고도"),
    /** Feet Height - 피트(높이) */
    FT_HEIGHT("FT", "피트(높이)"),
    /** Feet Altitude - 피트(고도) */
    FT_ALT("FT", "피트(고도)"),
    /** Unlimited - 무제한 */
    UNL("UNL", "무제한"),
    /** Ground - 지면 */
    GND("GND", "지면"),
    /** Surface - 지표면 */
    SFC("SFC", "지표면"),
    /** 알 수 없음 */
    UNKNOWN("", "알 수 없음")
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
 * - "3 000 AGL" -> 3,000ft AGL (914m, 지상 기준)
 * - "FL100" -> FL100 (3,048m, 비행고도)
 * - "UNL" -> 무제한
 * - "GND" / "SFC" -> 지면 / 지표면
 * - "1500 AMSL" -> 1,500ft AMSL (457m, 평균해수면 기준)
 */
object AltitudeFormatter {

    /** 1피트 = 0.3048미터 */
    private const val FEET_TO_METERS = 0.3048

    /** "FL100", "FL 100" 등의 Flight Level 패턴 — 매 호출 컴파일 회피용 캐시 */
    private val FL_PATTERN = Regex("^FL\\s*(\\d+)$")

    /** "3 000 AGL", "1500 AMSL" 등 숫자 + 단위 패턴 — 매 호출 컴파일 회피용 캐시 */
    private val NUMERIC_UNIT_PATTERN = Regex("^([\\d\\s]+)\\s+(AGL|AMSL|MSL|FT|ALT)$")

    /** 공백 제거 패턴 — 매 호출 컴파일 회피용 캐시 */
    private val WHITESPACE_PATTERN = Regex("\\s")

    /**
     * 고도 문자열을 파싱하여 ParsedAltitude를 반환합니다.
     *
     * @param altitudeString 고도 원본 문자열 (예: "3 000 AGL", "FL100", "UNL")
     * @return ParsedAltitude, 입력이 null이거나 빈 문자열이면 null
     */
    fun parse(altitudeString: String?): ParsedAltitude? {
        if (altitudeString.isNullOrBlank()) return null

        val trimmed = altitudeString.trim().uppercase()

        // 특수 단위 우선 처리
        if (trimmed == "UNL" || trimmed == "UNLIMITED") {
            return ParsedAltitude(
                rawValue = altitudeString,
                numericValue = null,
                unit = AltitudeUnit.UNL,
                meters = null
            )
        }
        if (trimmed == "GND" || trimmed == "GROUND") {
            return ParsedAltitude(
                rawValue = altitudeString,
                numericValue = 0.0,
                unit = AltitudeUnit.GND,
                meters = 0.0
            )
        }
        if (trimmed == "SFC" || trimmed == "SURFACE") {
            return ParsedAltitude(
                rawValue = altitudeString,
                numericValue = 0.0,
                unit = AltitudeUnit.SFC,
                meters = 0.0
            )
        }

        // FL (Flight Level) 처리: "FL100", "FL 100" 등
        FL_PATTERN.find(trimmed)?.let { match ->
            val flValue = match.groupValues[1].toDoubleOrNull() ?: return@let
            val feet = flValue * 100.0
            val meters = feet * FEET_TO_METERS
            return ParsedAltitude(
                rawValue = altitudeString,
                numericValue = flValue,
                unit = AltitudeUnit.FL,
                meters = meters
            )
        }

        // 숫자 + 단위 형식 처리: "3 000 AGL", "1500 AMSL", "500 MSL" 등
        // 숫자 부분에서 공백 제거 (항공 표기법에서 "3 000" = 3000)
        NUMERIC_UNIT_PATTERN.find(trimmed)?.let { match ->
            val numStr = match.groupValues[1].replace(WHITESPACE_PATTERN, "")
            val unitStr = match.groupValues[2]
            val numericValue = numStr.toDoubleOrNull() ?: return@let

            val unit = when (unitStr) {
                "AGL" -> AltitudeUnit.AGL
                "AMSL" -> AltitudeUnit.AMSL
                "MSL" -> AltitudeUnit.MSL
                "FT" -> AltitudeUnit.FT_HEIGHT
                "ALT" -> AltitudeUnit.FT_ALT
                else -> AltitudeUnit.UNKNOWN
            }

            val meters = convertToMeters(numericValue, unit)
            return ParsedAltitude(
                rawValue = altitudeString,
                numericValue = numericValue,
                unit = unit,
                meters = meters
            )
        }

        // 숫자만 있는 경우 (단위 없음) - 피트로 간주
        val numOnly = trimmed.replace(WHITESPACE_PATTERN, "")
        numOnly.toDoubleOrNull()?.let { numericValue ->
            val meters = numericValue * FEET_TO_METERS
            return ParsedAltitude(
                rawValue = altitudeString,
                numericValue = numericValue,
                unit = AltitudeUnit.UNKNOWN,
                meters = meters
            )
        }

        // 파싱 실패 시 원본 그대로 반환
        return ParsedAltitude(
            rawValue = altitudeString,
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
     * - "3 000 AGL" -> "3,000ft AGL (914m, 지상 기준)"
     * - "FL100" -> "FL100 (3,048m, 비행고도)"
     * - "UNL" -> "무제한"
     * - "GND" -> "지면 (0m)"
     * - "SFC" -> "지표면 (0m)"
     */
    fun format(altitudeString: String?): String? {
        val parsed = parse(altitudeString) ?: return null

        return when (parsed.unit) {
            AltitudeUnit.UNL -> "무제한"

            AltitudeUnit.GND -> "지면 (0m)"

            AltitudeUnit.SFC -> "지표면 (0m)"

            AltitudeUnit.FL -> {
                val metersStr = parsed.meters?.let { formatMeters(it) } ?: "?"
                "FL${parsed.numericValue?.toInt() ?: "?"} ($metersStr, ${parsed.unit.description})"
            }

            AltitudeUnit.AGL, AltitudeUnit.AMSL, AltitudeUnit.MSL,
            AltitudeUnit.FT_HEIGHT, AltitudeUnit.FT_ALT -> {
                val feetStr = parsed.numericValue?.let { formatNumber(it) } ?: "?"
                val metersStr = parsed.meters?.let { formatMeters(it) } ?: "?"
                "${feetStr}ft ${parsed.unit.label} ($metersStr, ${parsed.unit.description})"
            }

            AltitudeUnit.UNKNOWN -> {
                if (parsed.numericValue != null && parsed.meters != null) {
                    val feetStr = formatNumber(parsed.numericValue)
                    val metersStr = formatMeters(parsed.meters)
                    "${feetStr}ft ($metersStr)"
                } else {
                    parsed.rawValue
                }
            }
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
            AltitudeUnit.UNKNOWN -> value * FEET_TO_METERS
        }
    }

    /**
     * 미터 값을 포맷팅합니다 (반올림하여 정수로 표시).
     */
    private fun formatMeters(meters: Double): String {
        val rounded = Math.round(meters)
        return "${formatNumber(rounded.toDouble())}m"
    }

    /**
     * 숫자를 천 단위 구분자(,)가 포함된 문자열로 포맷팅합니다.
     * 소수점 이하가 0이면 정수로 표시합니다.
     */
    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            String.format("%,d", value.toLong())
        } else {
            String.format("%,.1f", value)
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
