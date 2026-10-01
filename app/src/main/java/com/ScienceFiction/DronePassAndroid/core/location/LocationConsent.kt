package com.ScienceFiction.DronePassAndroid.core.location

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * 위치정보 이용 동의의 목적(위치정보법 제19조⑤: 목적별로 따로 동의받는다).
 * 동의는 기기 단위로 저장하고 계정과 무관하다.
 */
enum class LocationPurpose(val raw: String) {
    /** P1: 지도에 현재 위치 표시. */
    CURRENT_LOCATION("currentLocation"),

    /** P2: 현재 위치의 날씨·일출/일몰 정보와 알림. */
    WEATHER_AND_SUN("weatherAndSun"),
}

enum class LocationConsentStatus(val raw: String) {
    AGREED("agreed"),
    DECLINED("declined"),
    WITHDRAWN("withdrawn"),
    ;

    companion object {
        fun fromRaw(raw: String?): LocationConsentStatus? = entries.firstOrNull { it.raw == raw }
    }
}

/** 목적 하나의 현재 상태. 이력은 남기지 않는다(제24조④: 철회하면 지체 없이 파기). */
data class LocationPurposeConsent(
    val status: LocationConsentStatus,
    /** 동의·거부·철회 당시의 위치약관 버전(`yyyy-MM-dd`). */
    val version: String,
    val atMillis: Long,
)

data class LocationConsentState(
    val purposes: Map<LocationPurpose, LocationPurposeConsent>,
    val ageConfirmed: Boolean,
) {
    operator fun get(purpose: LocationPurpose): LocationPurposeConsent? = purposes[purpose]

    companion object {
        val Empty = LocationConsentState(emptyMap(), ageConfirmed = false)
    }
}

/** 위치 동의·이용 기록·위치 캐시 키. */
internal object LocationConsentKeys {
    fun status(purpose: LocationPurpose) = stringPreferencesKey("location_consent_${purpose.raw}_status")
    fun version(purpose: LocationPurpose) = stringPreferencesKey("location_consent_${purpose.raw}_version")
    fun at(purpose: LocationPurpose) = longPreferencesKey("location_consent_${purpose.raw}_at")

    val AGE_CONFIRMED = booleanPreferencesKey("location_consent_age_confirmed")
    val USAGE_RECORDS = stringSetPreferencesKey("location_usage_records_v2")

    /** 확인자료 서버 업로드: 설치 ID, 올린 마지막 날짜, 한 번이라도 올렸는지, 서버 삭제 대기열. */
    val INSTALL_ID = stringPreferencesKey("location_install_id")
    /** 서버에 올린 기록(기기 기록과 같은 값). 기록과 함께 보존 기간·철회로 정리한다. */
    val UPLOADED_RECORDS = stringSetPreferencesKey("location_usage_uploaded_records")
    val EVER_UPLOADED = booleanPreferencesKey("location_usage_ever_uploaded")
    val PENDING_SERVER_DELETES = stringSetPreferencesKey("location_usage_pending_deletes")

    /** 이용자가 정한 마지막 지도 중심(기기 위치를 따라간 카메라 위치는 넣지 않는다). */
    val LAST_MAP_CENTER_LATITUDE = doublePreferencesKey("last_map_center_latitude")
    val LAST_MAP_CENTER_LONGITUDE = doublePreferencesKey("last_map_center_longitude")

    /**
     * v1(단일 동의 + 동의 이력) 키. 내부 테스트 빌드에만 있었고, 이력은 남기면 안 되므로 실행할 때 지운다.
     * v1 동의는 v2 목적별 동의로 옮기지 않는다(동의 화면 문구가 바뀌어 다시 받는다).
     */
    val LEGACY_V1_KEYS = listOf(
        stringPreferencesKey("location_consent_status"),
        stringPreferencesKey("location_consent_version"),
        longPreferencesKey("location_consent_at"),
        stringPreferencesKey("location_consent_history"),
        stringSetPreferencesKey("location_usage_records"),
        // 날짜 단위 업로드 표시(서버 계약 확정 전). 기록 단위 표시로 바꿨다.
        stringPreferencesKey("location_usage_uploaded_through"),
    )
}

internal fun storedLocationConsent(preferences: Preferences): LocationConsentState {
    val purposes = LocationPurpose.entries.mapNotNull { purpose ->
        val status = LocationConsentStatus.fromRaw(preferences[LocationConsentKeys.status(purpose)])
            ?: return@mapNotNull null
        val version = preferences[LocationConsentKeys.version(purpose)] ?: return@mapNotNull null
        purpose to LocationPurposeConsent(status, version, preferences[LocationConsentKeys.at(purpose)] ?: 0L)
    }.toMap()
    return LocationConsentState(purposes, preferences[LocationConsentKeys.AGE_CONFIRMED] ?: false)
}

/**
 * 이 목적으로 기기 위치를 써도 되는지. 모든 위치 접근은 이 판단을 거친다.
 * 지금 버전의 약관에 그 목적으로 동의했고 만 14세 이상임을 확인한 경우만 허용한다.
 */
internal fun isLocationPurposeAllowed(
    state: LocationConsentState,
    purpose: LocationPurpose,
    currentVersion: String,
): Boolean {
    val consent = state[purpose] ?: return false
    return state.ageConfirmed && consent.status == LocationConsentStatus.AGREED && consent.version >= currentVersion
}

/** 하나라도 동의한 목적이 있는지(OS 위치 권한을 물을지 정할 때 쓴다). */
internal fun isAnyLocationPurposeAllowed(state: LocationConsentState, currentVersion: String): Boolean =
    LocationPurpose.entries.any { isLocationPurposeAllowed(state, it, currentVersion) }

/** 동의 화면을 띄워야 하는지: 아직 정하지 않은 목적이 있거나 저장된 버전이 지금 약관보다 낮다. */
internal fun locationConsentNeedsPrompt(state: LocationConsentState, currentVersion: String): Boolean =
    LocationPurpose.entries.any { purpose ->
        val consent = state[purpose]
        consent == null || consent.version < currentVersion
    }

/**
 * 동의 화면 결과를 목적별 상태로 바꾼다. 고른 목적은 동의, 고르지 않은 목적은 이미 지금 버전으로 동의해
 * 둔 경우 그대로 두고 나머지는 거부로 둔다(철회는 설정 토글로만 한다).
 */
internal fun resolveConsentSubmission(
    current: LocationConsentState,
    agreedPurposes: Set<LocationPurpose>,
    currentVersion: String,
): Map<LocationPurpose, LocationConsentStatus> =
    LocationPurpose.entries.associateWith { purpose ->
        when {
            purpose in agreedPurposes -> LocationConsentStatus.AGREED
            isLocationPurposeAllowed(current, purpose, currentVersion) -> LocationConsentStatus.AGREED
            else -> LocationConsentStatus.DECLINED
        }
    }
