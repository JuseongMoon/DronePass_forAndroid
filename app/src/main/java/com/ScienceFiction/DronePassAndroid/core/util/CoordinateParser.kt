package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import java.util.Locale
import kotlin.math.abs

/**
 * 다양한 좌표 형식을 파싱하고 포맷팅하는 유틸리티
 *
 * 지원 형식:
 * - DMS (도/분/초): 37°33'58.0"N 126°58'41.0"E
 * - Decimal degrees (십진도): 37.5661, 126.9781
 * - Simple decimal (단순 십진수): 37.5661 126.9781
 * - Geo URI: geo:37.5661,126.9781
 */
object CoordinateParser {

    /**
     * DMS 패턴: 도°분'초"방향
     * 예: 37°33'58.0"N, 126°58'41.0"E
     */
    private val DMS_PATTERN = Regex(
        """(\d{1,3})\s*[°]\s*(\d{1,2})\s*['′]\s*([\d.]+)\s*["″]\s*([NSns])""" +
        """\s+(\d{1,3})\s*[°]\s*(\d{1,2})\s*['′]\s*([\d.]+)\s*["″]\s*([EWew])"""
    )

    /**
     * iOS CoordinateManager.parseDecimalDegrees 정합:
     * 쉼표가 있는 십진도는 firstMatch 로 부분 매칭되며, 각 값 뒤의 도 기호도 허용한다.
     */
    private const val IOS_DECIMAL = """-?\d{1,3}\.?\d*"""
    private const val SIGNED_SIMPLE_DECIMAL = """[+-]?\d{1,3}(?:\.\d+)?"""

    private val DECIMAL_DEGREES_PATTERN = Regex(
        """($IOS_DECIMAL)\s*°?\s*,\s*($IOS_DECIMAL)\s*°?"""
    )

    /**
     * iOS CoordinateManager.parseSimpleDecimal 정합:
     * 공백 구분 단순 십진수는 split 결과가 정확히 두 개여야 하므로 전체 입력만 허용한다.
     */
    private val SIMPLE_DECIMAL_PATTERN = Regex(
        """^\s*($SIGNED_SIMPLE_DECIMAL)\s+($SIGNED_SIMPLE_DECIMAL)\s*$"""
    )

    /**
     * Geo URI 패턴
     * 예: geo:37.5661,126.9781
     */
    private val GEO_URI_PATTERN = Regex(
        """^\s*geo:\s*($SIGNED_SIMPLE_DECIMAL)\s*,\s*($SIGNED_SIMPLE_DECIMAL)\s*$"""
    )

    /**
     * 입력 문자열을 파싱하여 Coordinate 객체를 반환한다.
     *
     * DMS -> comma decimal degrees -> simple decimal -> Geo URI 순서로 시도한다.
     *
     * @param input 좌표 문자열
     * @return 파싱된 Coordinate, 또는 파싱 실패 시 null
     */
    fun parse(input: String): Coordinate? {
        val trimmed = input.trim()

        // 1. DMS
        parseDms(trimmed)?.let { return it }

        // 2. Comma decimal degrees
        parseDecimalDegrees(trimmed)?.let { return it }

        // 3. Simple decimal
        parseSimpleDecimal(trimmed)?.let { return it }

        // 4. Geo URI
        parseGeoUri(trimmed)?.let { return it }

        return null
    }

    /**
     * Coordinate를 DMS(도/분/초) 형식 문자열로 포맷팅한다.
     *
     * 예: "37°33'58.0"N 126°58'41.0"E"
     */
    fun formatDMS(coordinate: Coordinate): String {
        val latDirection = if (coordinate.latitude >= 0) "N" else "S"
        val lonDirection = if (coordinate.longitude >= 0) "E" else "W"

        val latAbs = abs(coordinate.latitude)
        val latDeg = latAbs.toInt()
        val latMinDecimal = (latAbs - latDeg) * 60
        val latMin = latMinDecimal.toInt()
        val latSec = (latMinDecimal - latMin) * 60

        val lonAbs = abs(coordinate.longitude)
        val lonDeg = lonAbs.toInt()
        val lonMinDecimal = (lonAbs - lonDeg) * 60
        val lonMin = lonMinDecimal.toInt()
        val lonSec = (lonMinDecimal - lonMin) * 60

        return String.format(Locale.ROOT,
            "%d\u00B0%d'%.1f\"%s %d\u00B0%d'%.1f\"%s",
            latDeg, latMin, latSec, latDirection,
            lonDeg, lonMin, lonSec, lonDirection
        )
    }

    /**
     * Coordinate를 십진수 형식 문자열로 포맷팅한다.
     *
     * 예: "37.566100, 126.978100"
     */
    fun formatDecimal(coordinate: Coordinate): String {
        return String.format(Locale.ROOT, "%.6f, %.6f", coordinate.latitude, coordinate.longitude)
    }

    /**
     * Geo URI 형식 파싱
     */
    private fun parseGeoUri(input: String): Coordinate? {
        val match = GEO_URI_PATTERN.find(input) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return validateAndCreate(lat, lon)
    }

    /**
     * DMS 형식 파싱
     *
     * 분/초는 [0, 60) 범위만 유효. 60 이상의 입력은 사용자의 형식 오타로 간주하고 거부한다.
     * (예: "37°60'00\"N" 같은 입력은 38°N 으로 취급되지 않고 null 반환.)
     */
    private fun parseDms(input: String): Coordinate? {
        val match = DMS_PATTERN.find(input) ?: return null

        val latDeg = match.groupValues[1].toIntOrNull() ?: return null
        val latMin = match.groupValues[2].toIntOrNull() ?: return null
        val latSec = match.groupValues[3].toDoubleOrNull() ?: return null
        val latDir = match.groupValues[4].uppercase()

        val lonDeg = match.groupValues[5].toIntOrNull() ?: return null
        val lonMin = match.groupValues[6].toIntOrNull() ?: return null
        val lonSec = match.groupValues[7].toDoubleOrNull() ?: return null
        val lonDir = match.groupValues[8].uppercase()

        // 분/초 자체 범위 검증 (NaN/음수도 차단)
        if (latMin !in 0..59) return null
        if (lonMin !in 0..59) return null
        if (latSec.isNaN() || latSec < 0.0 || latSec >= 60.0) return null
        if (lonSec.isNaN() || lonSec < 0.0 || lonSec >= 60.0) return null
        // 도(degree) 자체 범위는 validateAndCreate 가 최종 검증

        var lat = latDeg + latMin / 60.0 + latSec / 3600.0
        var lon = lonDeg + lonMin / 60.0 + lonSec / 3600.0

        if (latDir == "S") lat = -lat
        if (lonDir == "W") lon = -lon

        return validateAndCreate(lat, lon)
    }

    /**
     * 쉼표 십진도 형식 파싱
     */
    private fun parseDecimalDegrees(input: String): Coordinate? {
        val match = DECIMAL_DEGREES_PATTERN.find(input) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return validateAndCreate(lat, lon)
    }

    /**
     * 공백 구분 단순 십진수 형식 파싱
     */
    private fun parseSimpleDecimal(input: String): Coordinate? {
        val match = SIMPLE_DECIMAL_PATTERN.find(input) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return validateAndCreate(lat, lon)
    }

    /**
     * 좌표 유효성 검사 후 Coordinate 객체 생성
     */
    private fun validateAndCreate(lat: Double, lon: Double): Coordinate? {
        if (lat < -90.0 || lat > 90.0) return null
        if (lon < -180.0 || lon > 180.0) return null
        return Coordinate(latitude = lat, longitude = lon)
    }
}
