package com.ScienceFiction.DronePassAndroid.core.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import com.ScienceFiction.DronePassAndroid.core.legal.LOCATION_TERMS_VERSION
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치정보 이용 동의와 이용 기록의 저장소이자, 위치 접근의 단일 게이트.
 *
 * 기기 위치를 읽는 모든 경로(날씨, 일출·일몰 알림, 지도 현재 위치)는 [isAllowed]·[allowed] 를 거친다.
 * 동의 상태가 `agreed` 가 아니면 위치 API 를 부르지 않는다.
 */
@Singleton
class LocationConsentRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val currentVersion: String = LOCATION_TERMS_VERSION

    val record: Flow<LocationConsentRecord?> = dataStore.data
        .map(::storedLocationConsent)
        .distinctUntilChanged()

    val allowed: Flow<Boolean> = record
        .map { isLocationUseAllowed(it, currentVersion) }
        .distinctUntilChanged()

    val needsPrompt: Flow<Boolean> = record
        .map { locationConsentNeedsPrompt(it, currentVersion) }
        .distinctUntilChanged()

    val history: Flow<List<LocationConsentHistoryEntry>> = dataStore.data
        .map { decodeLocationConsentHistory(it[LocationConsentKeys.HISTORY]) }
        .distinctUntilChanged()

    val usageRecords: Flow<List<LocationUsageRecord>> = dataStore.data
        .map { sortedLocationUsageRecords(it[LocationConsentKeys.USAGE_RECORDS].orEmpty()) }
        .distinctUntilChanged()

    suspend fun isAllowed(): Boolean = isLocationUseAllowed(storedLocationConsent(dataStore.data.first()), currentVersion)

    suspend fun needsPromptNow(): Boolean =
        locationConsentNeedsPrompt(storedLocationConsent(dataStore.data.first()), currentVersion)

    /** 동의. 만 14세 이상 확인 없이는 동의로 저장하지 않는다. */
    suspend fun agree(ageConfirmed: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        require(ageConfirmed) { "Location consent requires the age confirmation" }
        dataStore.edit { preferences ->
            preferences.writeConsent(LocationConsentStatus.AGREED, nowMillis, ageConfirmed = true)
        }
    }

    suspend fun decline(nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences.writeConsent(LocationConsentStatus.DECLINED, nowMillis, ageConfirmed = false)
        }
    }

    /**
     * 철회(제24조). 상태를 `withdrawn` 으로 두고, 이용 기록과 이 기기에 캐시된 위치를 모두 지운다.
     * 동의 이력은 입증용이라 남긴다.
     */
    suspend fun withdraw(nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences.writeConsent(LocationConsentStatus.WITHDRAWN, nowMillis, ageConfirmed = false)
            preferences.clearCachedLocations()
        }
    }

    /**
     * 실제 기기 위치를 읽은 뒤 호출한다. 같은 편집 안에서 동의 상태를 다시 확인해, 읽는 도중 철회됐다면
     * 기록을 남기지 않는다(철회로 지운 기록이 되살아나지 않게).
     */
    suspend fun recordUsage(purpose: LocationUsagePurpose, nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            if (!isLocationUseAllowed(storedLocationConsent(preferences), currentVersion)) return@edit
            preferences[LocationConsentKeys.USAGE_RECORDS] = addLocationUsageRecord(
                stored = preferences[LocationConsentKeys.USAGE_RECORDS].orEmpty(),
                purpose = purpose,
                nowMillis = nowMillis,
            )
        }
    }

    /** 앱을 실행할 때 보존 기간이 지난 기록을 지운다. */
    suspend fun pruneUsageRecords(nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            val stored = preferences[LocationConsentKeys.USAGE_RECORDS] ?: return@edit
            val pruned = pruneLocationUsageRecords(stored, nowMillis)
            if (pruned.isEmpty()) {
                preferences.remove(LocationConsentKeys.USAGE_RECORDS)
            } else if (pruned != stored) {
                preferences[LocationConsentKeys.USAGE_RECORDS] = pruned
            }
        }
    }

    private fun MutablePreferences.writeConsent(
        status: LocationConsentStatus,
        nowMillis: Long,
        ageConfirmed: Boolean,
    ) {
        this[LocationConsentKeys.STATUS] = status.raw
        this[LocationConsentKeys.VERSION] = currentVersion
        this[LocationConsentKeys.AT] = nowMillis
        this[LocationConsentKeys.AGE_CONFIRMED] = ageConfirmed
        val history = decodeLocationConsentHistory(this[LocationConsentKeys.HISTORY]) +
            LocationConsentHistoryEntry(atMillis = nowMillis, version = currentVersion, action = status)
        this[LocationConsentKeys.HISTORY] = encodeLocationConsentHistory(history)
    }

    private fun MutablePreferences.clearCachedLocations() {
        remove(LocationConsentKeys.USAGE_RECORDS)
        remove(UserLocationKeys.KEY_LAST_LATITUDE)
        remove(UserLocationKeys.KEY_LAST_LONGITUDE)
        remove(LocationConsentKeys.LAST_MAP_CENTER_LATITUDE)
        remove(LocationConsentKeys.LAST_MAP_CENTER_LONGITUDE)
    }
}
