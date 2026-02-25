package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlin.math.abs

/**
 * 다양한 좌표 형식을 파싱하고 포맷팅하는 유틸리티
 *
 * 지원 형식:
 * - DMS (도/분/초): 37°33'58.0"N 126°58'41.0"E
 * - Decimal (십진수): 37.5661, 126.9781
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
     * 십진수 패턴: 위도, 경도
     * 예: 37.5661, 126.9781 또는 37.5661 126.9781
     */
    private val DECIMAL_PATTERN = Regex(
        """(-?\d{1,3}\.?\d*)\s*[,\s]\s*(-?\d{1,3}\.?\d*)"""
    )

    /**
     * Geo URI 패턴
     * 예: geo:37.5661,126.9781
     */
    private val GEO_URI_PATTERN = Regex(
        """geo:\s*(-?\d{1,3}\.?\d*)\s*,\s*(-?\d{1,3}\.?\d*)"""
    )

    /**
     * 입력 문자열을 파싱하여 Coordinate 객체를 반환한다.
     *
     * Geo URI -> DMS -> Decimal 순서로 시도한다.
     *
     * @param input 좌표 문자열
     * @return 파싱된 Coordinate, 또는 파싱 실패 시 null
     */
    fun parse(input: String): Coordinate? {
        val trimmed = input.trim()

        // 1. Geo URI
        parseGeoUri(trimmed)?.let { return it }

        // 2. DMS
        parseDms(trimmed)?.let { return it }

        // 3. Decimal
        parseDecimal(trimmed)?.let { return it }

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

        return String.format(
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
        return String.format("%.6f, %.6f", coordinate.latitude, coordinate.longitude)
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

        var lat = latDeg + latMin / 60.0 + latSec / 3600.0
        var lon = lonDeg + lonMin / 60.0 + lonSec / 3600.0

        if (latDir == "S") lat = -lat
        if (lonDir == "W") lon = -lon

        return validateAndCreate(lat, lon)
    }

    /**
     * 십진수 형식 파싱
     */
    private fun parseDecimal(input: String): Coordinate? {
        val match = DECIMAL_PATTERN.find(input) ?: return null
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
