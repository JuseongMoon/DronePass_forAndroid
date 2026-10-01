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
 * 위치를 제공받는 자. 서버 업로드 값은 사양 B-1 의 문구 그대로다.
 * Android 날씨는 회사 서버(getAndroidWeather)를 거쳐 Apple WeatherKit 으로 간다.
 */
enum class LocationRecipient(val raw: String, val uploadValue: String, @StringRes val labelRes: Int) {
    NONE("none", "없음", R.string.location_usage_recipient_none),
    WEATHERKIT_VIA_SERVER("weatherKitViaServer", "회사 서버 경유 Apple(WeatherKit)", R.string.location_usage_recipient_weatherkit),
    ;

    companion object {
        fun fromRaw(raw: String): LocationRecipient? = entries.firstOrNull { it.raw == raw }
    }
}

/** 취득 경로(사양 B-1). */
internal const val LocationAcquisitionPath = "Google Fused Location Provider"

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
 * 서버에 올릴 날짜별 묶음: 마지막으로 올린 날짜 다음부터 어제(KST)까지. 오늘 기록은 하루가 끝난 뒤 올린다.
 */
internal fun pendingUploadBatches(
    stored: Set<String>,
    uploadedThrough: LocalDate?,
    nowMillis: Long,
): List<Pair<LocalDate, List<LocationUsageRecord>>> {
    val today = locationUsageDate(nowMillis)
    return sortedLocationUsageRecords(stored)
        .filter { it.date < today && (uploadedThrough == null || it.date > uploadedThrough) }
        .groupBy { it.date }
        .toSortedMap()
        .map { (date, records) -> date to records.sortedBy { it.hour } }
}

/** 표시 형식의 시간 부분: `2026-11-12 14:00`. */
internal fun locationUsageHourText(record: LocationUsageRecord): String =
    record.hour.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
