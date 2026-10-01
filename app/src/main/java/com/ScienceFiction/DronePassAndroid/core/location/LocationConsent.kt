package com.ScienceFiction.DronePassAndroid.core.location

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * 위치정보 이용 동의(위치정보법 제19조). 동의는 기기 단위로 저장하고 계정과 무관하다.
 * 동의 기록이 없으면 상태가 없는 것(`null`)으로 본다.
 */
enum class LocationConsentStatus(val raw: String) {
    AGREED("agreed"),
    DECLINED("declined"),
    WITHDRAWN("withdrawn"),
    ;

    companion object {
        fun fromRaw(raw: String?): LocationConsentStatus? = entries.firstOrNull { it.raw == raw }
    }
}

data class LocationConsentRecord(
    val status: LocationConsentStatus,
    /** 동의·거부·철회 당시의 위치약관 버전(`yyyy-MM-dd`). */
    val version: String,
    val atMillis: Long,
    val ageConfirmed: Boolean,
)

/** 동의 이력 한 줄. 동의 입증용이라 철회해도 지우지 않는다. 좌표는 넣지 않는다. */
data class LocationConsentHistoryEntry(
    val atMillis: Long,
    val version: String,
    val action: LocationConsentStatus,
)

/** 위치 동의·이용 기록·위치 캐시 키. 철회는 이 키들을 한 번의 편집으로 함께 정리한다. */
internal object LocationConsentKeys {
    val STATUS = stringPreferencesKey("location_consent_status")
    val VERSION = stringPreferencesKey("location_consent_version")
    val AT = longPreferencesKey("location_consent_at")
    val AGE_CONFIRMED = booleanPreferencesKey("location_consent_age_confirmed")
    val HISTORY = stringPreferencesKey("location_consent_history")
    val USAGE_RECORDS = stringSetPreferencesKey("location_usage_records")

    /** 동의하지 않은 상태에서 마지막으로 본 지도 중심(이용자가 정한 지점). */
    val LAST_MAP_CENTER_LATITUDE = doublePreferencesKey("last_map_center_latitude")
    val LAST_MAP_CENTER_LONGITUDE = doublePreferencesKey("last_map_center_longitude")
}

internal fun storedLocationConsent(preferences: Preferences): LocationConsentRecord? {
    val status = LocationConsentStatus.fromRaw(preferences[LocationConsentKeys.STATUS]) ?: return null
    val version = preferences[LocationConsentKeys.VERSION] ?: return null
    return LocationConsentRecord(
        status = status,
        version = version,
        atMillis = preferences[LocationConsentKeys.AT] ?: 0L,
        ageConfirmed = preferences[LocationConsentKeys.AGE_CONFIRMED] ?: false,
    )
}

/**
 * 기기 위치를 써도 되는지. 모든 위치 접근은 이 판단 하나를 거친다.
 * 지금 버전의 약관에 동의했고 만 14세 이상임을 확인한 경우만 허용한다.
 */
internal fun isLocationUseAllowed(record: LocationConsentRecord?, currentVersion: String): Boolean =
    record != null &&
        record.status == LocationConsentStatus.AGREED &&
        record.ageConfirmed &&
        record.version >= currentVersion

/** 동의 화면을 띄워야 하는지: 기록이 없거나 저장된 버전이 지금 약관보다 낮다. 거부·철회는 다시 묻지 않는다. */
internal fun locationConsentNeedsPrompt(record: LocationConsentRecord?, currentVersion: String): Boolean =
    record == null || record.version < currentVersion

internal fun encodeLocationConsentHistory(entries: List<LocationConsentHistoryEntry>): String =
    entries.joinToString("\n") { "${it.atMillis}|${it.version}|${it.action.raw}" }

internal fun decodeLocationConsentHistory(raw: String?): List<LocationConsentHistoryEntry> =
    raw.orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size != 3) return@mapNotNull null
        val at = parts[0].toLongOrNull() ?: return@mapNotNull null
        val action = LocationConsentStatus.fromRaw(parts[2]) ?: return@mapNotNull null
        LocationConsentHistoryEntry(atMillis = at, version = parts[1], action = action)
    }.toList()
