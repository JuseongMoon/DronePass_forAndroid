package com.ScienceFiction.DronePassAndroid.domain.model

import com.naver.maps.geometry.LatLng
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

data class Coordinate(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * 네이버 Maps SDK의 LatLng 객체로 변환
     */
    fun toLatLng(): LatLng = LatLng(latitude, longitude)

    /**
     * 도/분/초(DMS) 형식 문자열
     * 예: "37° 38' 55" N 126° 41' 12" E"
     */
    val formattedCoordinate: String
        get() {
            val latDirection = if (latitude >= 0) "N" else "S"
            val lonDirection = if (longitude >= 0) "E" else "W"

            val latAbs = abs(latitude)
            val latDegrees = latAbs.toInt()
            val latMinutesDecimal = (latAbs - latDegrees) * 60
            val latMinutes = latMinutesDecimal.toInt()
            val latSeconds = ((latMinutesDecimal - latMinutes) * 60).toInt()

            val lonAbs = abs(longitude)
            val lonDegrees = lonAbs.toInt()
            val lonMinutesDecimal = (lonAbs - lonDegrees) * 60
            val lonMinutes = lonMinutesDecimal.toInt()
            val lonSeconds = ((lonMinutesDecimal - lonMinutes) * 60).toInt()

            return "${latDegrees}\u00B0 ${latMinutes}\u2032 ${latSeconds}\u2033 $latDirection " +
                "${lonDegrees}\u00B0 ${lonMinutes}\u2032 ${lonSeconds}\u2033 $lonDirection"
        }

    /**
     * 십진수 형식 문자열
     * 예: "37.648611, 126.686667"
     *
     * Locale.ROOT 명시로 일부 유럽 로케일에서 소수점이 쉼표(,)로 변환되어
     * 파싱/표시 양쪽이 깨지는 문제를 차단한다.
     */
    val decimalCoordinate: String
        get() = String.format(Locale.ROOT, "%.6f, %.6f", latitude, longitude)

    companion object {
        /**
         * JSON 문자열로부터 Coordinate 객체 생성
         */
        fun fromJson(json: String): Coordinate {
            val jsonObject = JSONObject(json)
            return Coordinate(
                latitude = jsonObject.getDouble("latitude"),
                longitude = jsonObject.getDouble("longitude")
            )
        }

        /**
         * Coordinate 객체를 JSON 문자열로 변환
         */
        fun toJson(coordinate: Coordinate): String {
            val jsonObject = JSONObject().apply {
                put("latitude", coordinate.latitude)
                put("longitude", coordinate.longitude)
            }
            return jsonObject.toString()
        }

        /**
         * JSON 배열 문자열로부터 Coordinate 리스트 생성
         */
        fun listFromJson(json: String): List<Coordinate> {
            val jsonArray = org.json.JSONArray(json)
            return (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                Coordinate(
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude")
                )
            }
        }

        /**
         * Coordinate 리스트를 JSON 배열 문자열로 변환
         */
        fun listToJson(coordinates: List<Coordinate>): String {
            val jsonArray = org.json.JSONArray()
            coordinates.forEach { coordinate ->
                val obj = JSONObject().apply {
                    put("latitude", coordinate.latitude)
                    put("longitude", coordinate.longitude)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        /**
         * 네이버 Maps SDK LatLng으로부터 Coordinate 생성
         */
        fun fromLatLng(latLng: LatLng): Coordinate =
            Coordinate(latitude = latLng.latitude, longitude = latLng.longitude)
    }
}
