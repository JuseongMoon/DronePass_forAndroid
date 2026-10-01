package com.ScienceFiction.DronePassAndroid.core.location

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 위치정보 이용 기록(위치정보법 제16조② 확인자료). 기기에만 두고 서버로 보내지 않으며 좌표는 넣지 않는다.
 * 실제 기기 위치를 읽은 때만 기록하고, 같은 날(KST)·같은 목적은 한 건으로 묶는다.
 */
enum class LocationUsagePurpose(val raw: String, @StringRes val labelRes: Int) {
    CURRENT_LOCATION_ON_MAP("currentLocationOnMap", R.string.location_usage_purpose_map),
    WEATHER("weather", R.string.location_usage_purpose_weather),
    SUNRISE_SUNSET("sunriseSunset", R.string.location_usage_purpose_sun),
    SUNRISE_ALERT("sunriseAlert", R.string.location_usage_purpose_sun_alert),
    ;

    companion object {
        fun fromRaw(raw: String): LocationUsagePurpose? = entries.firstOrNull { it.raw == raw }
    }
}

data class LocationUsageRecord(
    val date: LocalDate,
    val purpose: LocationUsagePurpose,
)

internal val LocationUsageZone: ZoneId = ZoneId.of("Asia/Seoul")

/** 6개월 이상 보관하도록 190일이 지난 기록만 지운다. */
internal const val LocationUsageRetentionDays = 190L

internal fun locationUsageDate(nowMillis: Long): LocalDate =
    Instant.ofEpochMilli(nowMillis).atZone(LocationUsageZone).toLocalDate()

internal fun encodeLocationUsageRecord(record: LocationUsageRecord): String =
    "${record.date}|${record.purpose.raw}"

internal fun decodeLocationUsageRecord(raw: String): LocationUsageRecord? {
    val parts = raw.split('|')
    if (parts.size != 2) return null
    val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return null
    val purpose = LocationUsagePurpose.fromRaw(parts[1]) ?: return null
    return LocationUsageRecord(date, purpose)
}

/** 같은 날·같은 목적은 집합의 같은 원소가 되어 한 건으로 묶인다. */
internal fun addLocationUsageRecord(
    stored: Set<String>,
    purpose: LocationUsagePurpose,
    nowMillis: Long,
): Set<String> = stored + encodeLocationUsageRecord(LocationUsageRecord(locationUsageDate(nowMillis), purpose))

/** 보존 기간이 지난 기록과 읽을 수 없는 값을 뺀다. */
internal fun pruneLocationUsageRecords(stored: Set<String>, nowMillis: Long): Set<String> {
    val today = locationUsageDate(nowMillis)
    return stored.filterTo(mutableSetOf()) { raw ->
        val record = decodeLocationUsageRecord(raw) ?: return@filterTo false
        ChronoUnit.DAYS.between(record.date, today) <= LocationUsageRetentionDays
    }
}

/** 화면 표시 순서: 최근 날짜 먼저, 같은 날은 목적 순서대로. */
internal fun sortedLocationUsageRecords(stored: Set<String>): List<LocationUsageRecord> =
    stored.mapNotNull(::decodeLocationUsageRecord)
        .sortedWith(compareByDescending<LocationUsageRecord> { it.date }.thenBy { it.purpose.ordinal })
