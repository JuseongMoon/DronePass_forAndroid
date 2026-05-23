package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import com.ScienceFiction.DronePassAndroid.core.util.AltitudeFormatter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * GeoJSON FeatureCollection.
 *
 * VWorld WFS `outputFormat=application/json` 응답은 wrapper 없는 표준 GeoJSON
 * FeatureCollection 으로 직접 도착한다 (iOS 와 동일). 이전 버전의 `VWorldWfsResponse`
 * (`response.result.featureCollection.features` 3단 wrapper) 는 잘못된 가정이라
 * 모든 layer 가 0개 features 로 파싱되는 버그가 있었음.
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
 *
 * [properties] 는 VWorld WFS 응답에서 레이어별로 다른 키 집합을 가지므로
 * [Map<String, Any?>] 로 유지한다. 안전한 접근은 [stringProp] / [doubleProp] /
 * [intProp] helper 를 사용한다 (모든 접근이 nullable-string-toString 패턴이라 누락된 키와
 * 잘못된 타입을 동일하게 null 로 정규화).
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

/** properties Map 에서 String 값을 안전하게 꺼낸다 (Any? → trimmed String? 또는 null). */
internal fun Map<String, Any?>.stringProp(key: String): String? =
    this[key]?.toString()?.takeIf { it.isNotBlank() }

/** properties Map 에서 Double 값을 안전하게 꺼낸다. */
internal fun Map<String, Any?>.doubleProp(key: String): Double? = when (val v = this[key]) {
    is Number -> v.toDouble()
    is String -> v.toDoubleOrNull()
    else -> null
}

/** properties Map 에서 Int 값을 안전하게 꺼낸다. */
internal fun Map<String, Any?>.intProp(key: String): Int? = when (val v = this[key]) {
    is Number -> v.toInt()
    is String -> v.toIntOrNull()
    else -> null
}

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
        return properties.stringProp("notam")
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

    val twoDigitYear = dateString.substring(0, 2).toIntOrNull() ?: return null
    val month = (dateString.substring(2, 4).toIntOrNull() ?: return null) - 1 // Calendar는 0-indexed
    val day = dateString.substring(4, 6).toIntOrNull() ?: return null
    val hour = dateString.substring(6, 8).toIntOrNull() ?: return null
    val minute = dateString.substring(8, 10).toIntOrNull() ?: return null

    // 2-digit year 를 4-digit 으로 변환 (sliding window: 현재 연도 ± 50 범위).
    // NOTAM 은 표준상 YY 형식을 사용하므로 단순 +2000 만 적용하면 2099 년 이후는
    // 잘못된 연도로 파싱됨. 항공 분야는 늘 현재/근미래 NOTAM 이므로 현재 시점 기준
    // 가까운 세기를 선택한다.
    val currentYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR)
    val century = (currentYear / 100) * 100
    val candidateThis = century + twoDigitYear
    val candidatePrev = century - 100 + twoDigitYear
    val year = if (kotlin.math.abs(candidateThis - currentYear) <=
        kotlin.math.abs(candidatePrev - currentYear)
    ) candidateThis else candidatePrev

    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.isLenient = false // 비정상 값(예: month=12, day=31, hour=24) 자동 보정 차단 → 명시적 실패
    cal.set(Calendar.MILLISECOND, 0)
    return try {
        cal.set(year, month, day, hour, minute, 0)
        cal.time
    } catch (e: IllegalArgumentException) {
        null
    }
}

// ============================================================
// DroneZoneFeature Extension Properties - 사전협의구역 전용
// ============================================================

/** 관리기관 한글명 */
val DroneZoneFeature.authorityNameKor: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties.stringProp("nm_kor")
    }

/** 관리기관 영문명 */
val DroneZoneFeature.authorityNameEng: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties.stringProp("nm_eng")
    }

/** 운영기관 */
val DroneZoneFeature.operatingInstitution: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties.stringProp("oper_inst")
    }

/** 연락처 */
val DroneZoneFeature.phoneNumber: String?
    get() {
        if (layer != FlightZoneLayer.PRIOR_CONSULTATION) return null
        return properties.stringProp("telno")
    }

// ============================================================
// DroneZoneFeature Extension Properties - 문화재보호구역 전용
// ============================================================

/** 문화재명 */
val DroneZoneFeature.heritageName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties.stringProp("alias") ?: properties.stringProp("remark")
    }

/** 시도명 */
val DroneZoneFeature.sidoName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties.stringProp("sido_name")
    }

/** 시군구명 */
val DroneZoneFeature.sigunguName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties.stringProp("sigg_name")
    }

/** 문화재구역명 */
val DroneZoneFeature.heritageZoneName: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties.stringProp("uname")
    }

/** 지정연도 */
val DroneZoneFeature.designationYear: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties.stringProp("dyear")
    }

/** 지정번호 */
val DroneZoneFeature.designationNumber: String?
    get() {
        if (layer != FlightZoneLayer.CULTURAL_HERITAGE) return null
        return properties.stringProp("dnum")
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
                properties.stringProp("prh_lbl_2")
            FlightZoneLayer.RESTRICTED -> properties.stringProp("res_lbl_2")
            FlightZoneLayer.DANGER -> properties.stringProp("dng_lbl_2")
            FlightZoneLayer.ALERT -> properties.stringProp("alt_lbl_2")
            FlightZoneLayer.ULTRALIGHT -> properties.stringProp("uac_lbl_2")
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
                properties.stringProp("prh_lbl_3")
            FlightZoneLayer.RESTRICTED -> properties.stringProp("res_lbl_3")
            FlightZoneLayer.DANGER -> properties.stringProp("dng_lbl_3")
            FlightZoneLayer.ALERT -> properties.stringProp("alt_lbl_3")
            FlightZoneLayer.ULTRALIGHT -> properties.stringProp("uac_lbl_3")
            else -> null
        }
        return AltitudeFormatter.format(altStr)
    }

// ============================================================
// DroneZoneFeature Extension Properties - 중심 좌표 (iOS 정합)
// ============================================================

/**
 * 비행구역의 중심 좌표를 (lat, lon) Pair 로 반환.
 *
 * iOS [GeoJSONGeometry.centerCoordinate] 와 동일하게 첫 polygon 의 첫 ring 의
 * 좌표 평균을 사용한다. polygons 가 비어있으면 (0.0, 0.0) fallback.
 */
val DroneZoneFeature.centerCoordinate: Pair<Double, Double>
    get() {
        val firstRing = polygons.firstOrNull()
        if (firstRing.isNullOrEmpty()) return 0.0 to 0.0
        val avgLat = firstRing.sumOf { it.first } / firstRing.size
        val avgLon = firstRing.sumOf { it.second } / firstRing.size
        return avgLat to avgLon
    }

/**
 * 좌표를 iOS 의 [CLLocationCoordinate2D.formattedCoordinate] 와 동일한 포맷으로 변환.
 *
 * 형식: `37.5000° N, 126.5000° E`
 */
fun formatCoordinate(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return String.format(
        java.util.Locale.US,
        "%.4f° %s, %.4f° %s",
        kotlin.math.abs(lat), latDir,
        kotlin.math.abs(lon), lonDir
    )
}
