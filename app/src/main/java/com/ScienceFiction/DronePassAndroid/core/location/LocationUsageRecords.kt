package com.ScienceFiction.DronePassAndroid.core.location

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 위치정보 이용사실 확인자료(위치정보법 제16조②, 고시 제6조). 좌표는 넣지 않는다.
 * 실제 기기 위치를 읽은 때만 기록하고, 같은 시간(KST 정시)·같은 목적·같은 제공받는 자는 한 건으로 묶는다.
 */
enum class LocationUsagePurpose(
    val raw: String,
    @StringRes val labelRes: Int,
    val consentPurpose: LocationPurpose,
) {
    CURRENT_LOCATION_ON_MAP("currentLocationOnMap", R.string.location_usage_purpose_map, LocationPurpose.CURRENT_LOCATION),
    WEATHER("weather", R.string.location_usage_purpose_weather, LocationPurpose.WEATHER_AND_SUN),
    SUNRISE_SUNSET("sunriseSunset", R.string.location_usage_purpose_sun, LocationPurpose.WEATHER_AND_SUN),
    SUNRISE_ALERT("sunriseAlert", R.string.location_usage_purpose_sun_alert, LocationPurpose.WEATHER_AND_SUN),
    ;

    companion object {
        fun fromRaw(raw: String): LocationUsagePurpose? = entries.firstOrNull { it.raw == raw }
    }
}

/**
 * 위치를 제공받는 자. 서버에는 고정 코드값을 보내고 화면 표시만 현지화한다(서버 계약 B-2026-10-01).
 * Android 날씨는 회사 서버(getAndroidWeather)를 거쳐 Apple WeatherKit 으로 간다.
 */
enum class LocationRecipient(val raw: String, val uploadValue: String, @StringRes val labelRes: Int) {
    NONE("none", "none", R.string.location_usage_recipient_none),
    WEATHERKIT_VIA_SERVER(
        "weatherKitViaServer",
        "appleWeatherKitViaCompanyServer",
        R.string.location_usage_recipient_weatherkit,
    ),
    ;

    companion object {
        fun fromRaw(raw: String): LocationRecipient? = entries.firstOrNull { it.raw == raw }
    }
}

/** 취득 경로 코드값(서버 계약 B-2026-10-01). 화면에는 표시하지 않는다. */
internal const val LocationAcquisitionPath = "googleFusedLocation"

/** 서버가 한 요청에 받는 최대 항목 수. */
internal const val LocationUsageUploadLimit = 500

data class LocationUsage(
    val purpose: LocationUsagePurpose,
    val recipient: LocationRecipient,
)

data class LocationUsageRecord(
    /** KST 정시. */
    val hour: LocalDateTime,
    val purpose: LocationUsagePurpose,
    val recipient: LocationRecipient,
) {
    val date: LocalDate get() = hour.toLocalDate()

    /** 서버 계약의 `occurredHour`(예: 2026-11-12T14). */
    val occurredHour: String get() = hour.format(OccurredHourFormat)
}

internal val LocationUsageZone: ZoneId = ZoneId.of("Asia/Seoul")

/** 6개월 이상 보관하도록 190일이 지난 기록만 지운다. */
internal const val LocationUsageRetentionDays = 190L

private val OccurredHourFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH")

internal fun locationUsageHour(nowMillis: Long): LocalDateTime =
    Instant.ofEpochMilli(nowMillis).atZone(LocationUsageZone).toLocalDateTime().truncatedTo(ChronoUnit.HOURS)

internal fun locationUsageDate(nowMillis: Long): LocalDate = locationUsageHour(nowMillis).toLocalDate()

internal fun encodeLocationUsageRecord(record: LocationUsageRecord): String =
    "${record.occurredHour}|${record.purpose.raw}|${record.recipient.raw}"

internal fun decodeLocationUsageRecord(raw: String): LocationUsageRecord? {
    val parts = raw.split('|')
    if (parts.size != 3) return null
    val hour = runCatching { LocalDateTime.parse(parts[0], OccurredHourFormat) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(parts[0] + ":00") }.getOrNull()
        ?: return null
    val purpose = LocationUsagePurpose.fromRaw(parts[1]) ?: return null
    val recipient = LocationRecipient.fromRaw(parts[2]) ?: return null
    return LocationUsageRecord(hour, purpose, recipient)
}

/** 같은 시간·목적·제공받는 자는 집합의 같은 원소가 되어 한 건으로 묶인다. */
internal fun addLocationUsageRecord(
    stored: Set<String>,
    usage: LocationUsage,
    nowMillis: Long,
): Set<String> = stored + encodeLocationUsageRecord(
    LocationUsageRecord(locationUsageHour(nowMillis), usage.purpose, usage.recipient),
)

/** 보존 기간이 지난 기록과 읽을 수 없는 값을 뺀다. */
internal fun pruneLocationUsageRecords(stored: Set<String>, nowMillis: Long): Set<String> {
    val today = locationUsageDate(nowMillis)
    return stored.filterTo(mutableSetOf()) { raw ->
        val record = decodeLocationUsageRecord(raw) ?: return@filterTo false
        ChronoUnit.DAYS.between(record.date, today) <= LocationUsageRetentionDays
    }
}

/** 철회한 목적의 기록을 뺀다. */
internal fun removeLocationUsageRecords(stored: Set<String>, purpose: LocationPurpose): Set<String> =
    stored.filterTo(mutableSetOf()) { raw -> decodeLocationUsageRecord(raw)?.purpose?.consentPurpose != purpose }

/** 화면 표시 순서: 최근 시간 먼저, 같은 시간은 목적 순서대로. */
internal fun sortedLocationUsageRecords(stored: Set<String>): List<LocationUsageRecord> =
    stored.mapNotNull(::decodeLocationUsageRecord)
        .sortedWith(
            compareByDescending<LocationUsageRecord> { it.hour }
                .thenBy { it.purpose.ordinal }
                .thenBy { it.recipient.ordinal },
        )

/**
 * 서버에 올릴 묶음: 어제(KST)까지의 기록 중 아직 올리지 않은 것을 오래된 순서로 최대 [limit] 개.
 * 서버가 installId 당 시간에 한 번만 받으므로 밀린 날짜를 모두 한 요청에 담는다. 넘치는 것은 다음 실행 때 보낸다.
 * 오늘 기록은 하루가 끝난 뒤 올린다.
 */
internal fun pendingUploadBatch(
    stored: Set<String>,
    uploaded: Set<String>,
    nowMillis: Long,
    limit: Int = LocationUsageUploadLimit,
): List<LocationUsageRecord> {
    val today = locationUsageDate(nowMillis)
    return (stored - uploaded)
        .mapNotNull(::decodeLocationUsageRecord)
        .filter { it.date < today }
        .sortedWith(compareBy<LocationUsageRecord> { it.hour }.thenBy { it.purpose.ordinal }.thenBy { it.recipient.ordinal })
        .take(limit)
}

/** 표시 형식의 시간 부분: `2026-11-12 14:00`. */
internal fun locationUsageHourText(record: LocationUsageRecord): String =
    record.hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
