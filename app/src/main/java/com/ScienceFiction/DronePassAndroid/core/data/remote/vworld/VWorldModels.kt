package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import com.ScienceFiction.DronePassAndroid.core.util.AltitudeFormatter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * VWorld WFS GeoJSON 응답 최상위 모델
 */
@JsonClass(generateAdapter = true)
data class VWorldWfsResponse(
    @Json(name = "response") val response: VWorldResponseBody?
)

@JsonClass(generateAdapter = true)
data class VWorldResponseBody(
    @Json(name = "status") val status: String?,
    @Json(name = "result") val result: VWorldResult?
)

@JsonClass(generateAdapter = true)
data class VWorldResult(
    @Json(name = "featureCollection") val featureCollection: GeoJSONFeatureCollection?
)

/**
 * GeoJSON FeatureCollection
 */
@JsonClass(generateAdapter = true)
data class GeoJSONFeatureCollection(
    @Json(name = "features") val features: List<GeoJSONFeature>? = null,
    @Json(name = "totalFeatures") val totalFeatures: Int? = null,
    @Json(name = "type") val type: String? = null
)

/**
 * GeoJSON Feature
 */
@JsonClass(generateAdapter = true)
data class GeoJSONFeature(
    @Json(name = "type") val type: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "geometry") val geometry: GeoJSONGeometry? = null,
    @Json(name = "properties") val properties: Map<String, Any?>? = null
)

/**
 * GeoJSON Geometry
 * type: Polygon, MultiPolygon
 * coordinates: 중첩 배열 (Any로 처리 후 수동 파싱)
 */
@JsonClass(generateAdapter = true)
data class GeoJSONGeometry(
    @Json(name = "type") val type: String? = null,
    @Json(name = "coordinates") val coordinates: Any? = null
)

/**
 * 파싱된 비행구역 모델 (UI에서 사용)
 */
data class DroneZoneFeature(
    val id: String,
    val layer: FlightZoneLayer,
    val polygons: List<List<Pair<Double, Double>>>,
    val zoneCode: String?,
    val upperAltitude: Double?,
    val lowerAltitude: Double?,
    val zoneName: String?,
    val properties: Map<String, Any?> = emptyMap()
)

// ============================================================
// NotamStatus enum
// ============================================================

/**
 * NOTAM 상태를 나타내는 열거형
 */
enum class NotamStatus(val displayName: String, val emoji: String) {
    SCHEDULED("예정됨", "\uD83D\uDD35"),   // 🔵
    ACTIVE("활성", "\uD83D\uDD34"),         // 🔴
    EXPIRED("만료됨", "\u26AB"),             // ⚫
    UNKNOWN("알 수 없음", "\u2753")          // ❓
}

// ============================================================
// DroneZoneFeature Extension Properties - NOTAM 관련 (임시비행금지구역 전용)
// ============================================================

/**
 * NOTAM 원본 문자열 (예: "A)RKRR B)2501311500 C)2504301459")
 * 임시비행금지구역(TEMPORARY_PROHIBITED)에서만 반환됩니다.
 */
val DroneZoneFeature.notam: String?
    get() {
        if (layer != FlightZoneLayer.TEMPORARY_PROHIBITED) return null
        return properties["notam"]?.toString()
    }

/**
 * NOTAM 시작 날짜 (B 필드에서 파싱, UTC)
 */
val DroneZoneFeature.notamStartDate: Date?
    get() = notam?.let { parseNotamDate(it, "B") }

/**
 * NOTAM 종료 날짜 (C 필드에서 파싱, UTC)
 */
val DroneZoneFeature.notamEndDate: Date?
    get() = notam?.let { parseNotamDate(it, "C") }

/**
 * NOTAM 상태 판정
 *
 * 시작/종료 날짜를 기준으로 현재 시각과 비교하여 상태를 결정합니다.
 */
val DroneZoneFeature.notamStatus: NotamStatus
    get() {
        val start = notamStartDate ?: return NotamStatus.UNKNOWN
        val end = notamEndDate ?: return NotamStatus.UNKNOWN
        val now = Date()
        return when {
            now.before(start) -> NotamStatus.SCHEDULED
            now.after(end) -> NotamStatus.EXPIRED
            else -> NotamStatus.ACTIVE
        }
    }

/**
 * NOTAM 종료까지 남은 일수 (활성 상태일 때만 반환)
 */
val DroneZoneFeature.daysRemaining: Int?
    get() {
        if (notamStatus != NotamStatus.ACTIVE) return null
        val end = notamEndDate ?: return null
        val now = Date()
        val diffMillis = end.time - now.time
        if (diffMillis <= 0) return 0
        return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    }

/**
 * NOTAM 날짜 파싱 함수
 *
 * NOTAM 문자열에서 B) 또는 C) 뒤의 10자리 YYMMDDHHMM 형식을 Date로 변환합니다.
 *
 * @param notam NOTAM 원본 문자열 (예: "A)RKRR B)2501311500 C)2504301459")
 * @param field 추출할 필드 ("B" 또는 "C")
 * @return 파싱된 Date (UTC), 실패 시 null
 */
private fun parseNotamDate(notam: String, field: String): Date? {
    val pattern = Regex("$field\\)(\\d{10})")
    val match = pattern.find(notam) ?: return null
    val dateString = match.groupValues[1]

    val year = 2000 + (dateString.substring(0, 2).toIntOrNull() ?: return null)
    val month = (dateString.substring(2, 4).toIntOrNull() ?: return null) - 1 // Calendar는 0-indexed
    val day = dateString.substring(4, 6).toIntOrNull() ?: return null
    val hour = dateString.substring(6, 8).toIntOrNull() ?: return null
    val minute = dateString.substring(8, 10).toIntOrNull() ?: return null

    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(year, month, day, hour, minute, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

// ============================================================
// DroneZoneFeature Extension Properties - 사전협의구역 전용
// ============================================================

/** 관리기관 한글명 */
val DroneZoneFeature.authorityNameKor: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties["nm_kor"]?.toString()
    }

/** 관리기관 영문명 */
val DroneZoneFeature.authorityNameEng: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties["nm_eng"]?.toString()
    }

/** 운영기관 */
val DroneZoneFeature.operatingInstitution: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties["oper_inst"]?.toString()
    }

/** 연락처 */
val DroneZoneFeature.phoneNumber: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties["telno"]?.toString()
    }

// ============================================================
// DroneZoneFeature Extension Properties - 문화재보호구역 전용
// ============================================================

/** 문화재명 */
val DroneZoneFeature.heritageName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties["alias"]?.toString() ?: properties["remark"]?.toString()
    }

/** 시도명 */
val DroneZoneFeature.sidoName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties["sido_name"]?.toString()
    }

/** 시군구명 */
val DroneZoneFeature.sigunguName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties["sigg_name"]?.toString()
    }

/** 문화재구역명 */
val DroneZoneFeature.heritageZoneName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties["uname"]?.toString()
    }

/** 지정연도 */
val DroneZoneFeature.designationYear: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties["dyear"]?.toString()
    }

/** 지정번호 */
val DroneZoneFeature.designationNumber: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties["dnum"]?.toString()
    }

/** 전체 주소 (시도 + 시군구) */
val DroneZoneFeature.fullAddress: String?
    get() {
        val sido = sidoName ?: return null
        val sigungu = sigunguName ?: return null
        return "$sido $sigungu"
    }

// ============================================================
// DroneZoneFeature Extension Properties - 포맷팅된 고도
// ============================================================

/**
 * 레이어별 상한 고도를 포맷팅된 문자열로 반환합니다.
 *
 * 레이어에 따라 properties에서 적절한 키(prh_lbl_2, res_lbl_2 등)를 읽어
 * AltitudeFormatter로 포맷팅합니다.
 */
val DroneZoneFeature.formattedUpperAltitude: String?
    get() {
        val altStr = when (layer) {
            FlightZoneLayer.PROHIBITED, FlightZoneLayer.TEMPORARY_PROHIBITED ->
                properties["prh_lbl_2"]?.toString()
            FlightZoneLayer.RESTRICTED -> properties["res_lbl_2"]?.toString()
            FlightZoneLayer.DANGER -> properties["dng_lbl_2"]?.toString()
            FlightZoneLayer.ALERT -> properties["alt_lbl_2"]?.toString()
            FlightZoneLayer.ULTRALIGHT -> properties["uac_lbl_2"]?.toString()
            else -> null
        }
        return AltitudeFormatter.format(altStr)
    }

/**
 * 레이어별 하한 고도를 포맷팅된 문자열로 반환합니다.
 *
 * 레이어에 따라 properties에서 적절한 키(prh_lbl_3, res_lbl_3 등)를 읽어
 * AltitudeFormatter로 포맷팅합니다.
 */
val DroneZoneFeature.formattedLowerAltitude: String?
    get() {
        val altStr = when (layer) {
            FlightZoneLayer.PROHIBITED, FlightZoneLayer.TEMPORARY_PROHIBITED ->
                properties["prh_lbl_3"]?.toString()
            FlightZoneLayer.RESTRICTED -> properties["res_lbl_3"]?.toString()
            FlightZoneLayer.DANGER -> properties["dng_lbl_3"]?.toString()
            FlightZoneLayer.ALERT -> properties["alt_lbl_3"]?.toString()
            FlightZoneLayer.ULTRALIGHT -> properties["uac_lbl_3"]?.toString()
            else -> null
        }
        return AltitudeFormatter.format(altStr)
    }
