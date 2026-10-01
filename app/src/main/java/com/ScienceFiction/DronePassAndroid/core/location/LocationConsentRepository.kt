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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치정보 이용 동의(목적별)와 이용사실 확인자료의 저장소이자, 위치 접근의 단일 게이트.
 *
 * 기기 위치를 읽는 모든 경로는 목적별로 [isAllowed]·[allowed] 를 거친다. 동의 상태는 현재 값만 저장하고
 * 이력은 남기지 않는다.
 */
@Singleton
class LocationConsentRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val currentVersion: String = LOCATION_TERMS_VERSION

    val state: Flow<LocationConsentState> = dataStore.data
        .map(::storedLocationConsent)
        .distinctUntilChanged()

    fun allowed(purpose: LocationPurpose): Flow<Boolean> = state
        .map { isLocationPurposeAllowed(it, purpose, currentVersion) }
        .distinctUntilChanged()

    val anyAllowed: Flow<Boolean> = state
        .map { isAnyLocationPurposeAllowed(it, currentVersion) }
        .distinctUntilChanged()

    val usageRecords: Flow<List<LocationUsageRecord>> = dataStore.data
        .map { sortedLocationUsageRecords(it[LocationConsentKeys.USAGE_RECORDS].orEmpty()) }
        .distinctUntilChanged()

    suspend fun isAllowed(purpose: LocationPurpose): Boolean =
        isLocationPurposeAllowed(storedLocationConsent(dataStore.data.first()), purpose, currentVersion)

    suspend fun needsPromptNow(): Boolean =
        locationConsentNeedsPrompt(storedLocationConsent(dataStore.data.first()), currentVersion)

    /**
     * 동의 화면의 결과. [agreedPurposes] 가 비어 있으면 "동의하지 않음"이다.
     * 동의가 하나라도 있으면 만 14세 이상 확인이 있어야 한다.
     */
    suspend fun submitConsent(
        agreedPurposes: Set<LocationPurpose>,
        ageConfirmed: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        require(agreedPurposes.isEmpty() || ageConfirmed) { "Location consent requires the age confirmation" }
        dataStore.edit { preferences ->
            val current = storedLocationConsent(preferences)
            val resolved = resolveConsentSubmission(current, agreedPurposes, currentVersion)
            resolved.forEach { (purpose, status) ->
                val unchanged = purpose !in agreedPurposes &&
                    current[purpose]?.status == status &&
                    current[purpose]?.version == currentVersion
                if (!unchanged) preferences.writePurpose(purpose, status, nowMillis)
            }
            if (agreedPurposes.isNotEmpty()) preferences[LocationConsentKeys.AGE_CONFIRMED] = true
        }
    }

    /**
     * 목적별 철회(제24조). 그 목적의 이용 기록과 그 목적으로 캐시한 기기 위치를 지운다.
     * 서버에 올린 적이 있으면 서버 삭제를 대기열에 넣는다([LocationUsageUploader] 가 처리).
     */
    suspend fun withdraw(purpose: LocationPurpose, nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences.writePurpose(purpose, LocationConsentStatus.WITHDRAWN, nowMillis)
            preferences.updateRecordSet(LocationConsentKeys.USAGE_RECORDS) { removeLocationUsageRecords(it, purpose) }
            preferences.updateRecordSet(LocationConsentKeys.UPLOADED_RECORDS) { removeLocationUsageRecords(it, purpose) }
            if (purpose == LocationPurpose.WEATHER_AND_SUN) {
                // 일출·일몰 알림용으로 캐시한 마지막 기기 위치
                preferences.remove(UserLocationKeys.KEY_LAST_LATITUDE)
                preferences.remove(UserLocationKeys.KEY_LAST_LONGITUDE)
            }
            if (!isAnyLocationPurposeAllowed(storedLocationConsent(preferences), currentVersion)) {
                preferences[LocationConsentKeys.AGE_CONFIRMED] = false
            }
            if (preferences[LocationConsentKeys.EVER_UPLOADED] == true) {
                val usagePurposes = LocationUsagePurpose.entries.filter { it.consentPurpose == purpose }.map { it.raw }
                preferences[LocationConsentKeys.PENDING_SERVER_DELETES] =
                    preferences[LocationConsentKeys.PENDING_SERVER_DELETES].orEmpty() + usagePurposes
            }
        }
    }

    /**
     * 실제 기기 위치를 읽은 뒤 호출한다. 같은 편집 안에서 그 목적의 동의를 다시 확인해, 읽는 도중 철회됐다면
     * 기록을 남기지 않는다(철회로 지운 기록이 되살아나지 않게).
     */
    suspend fun recordUsage(usage: LocationUsage, nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            val allowed = isLocationPurposeAllowed(
                storedLocationConsent(preferences),
                usage.purpose.consentPurpose,
                currentVersion,
            )
            if (!allowed) return@edit
            preferences[LocationConsentKeys.USAGE_RECORDS] = addLocationUsageRecord(
                stored = preferences[LocationConsentKeys.USAGE_RECORDS].orEmpty(),
                usage = usage,
                nowMillis = nowMillis,
            )
        }
    }

    /** 앱을 실행할 때: v1 키(동의 이력 포함)를 지우고 보존 기간이 지난 기록을 지운다. */
    suspend fun runLaunchMaintenance(nowMillis: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            LocationConsentKeys.LEGACY_V1_KEYS.forEach { preferences.remove(it) }
            preferences.updateRecordSet(LocationConsentKeys.USAGE_RECORDS) { pruneLocationUsageRecords(it, nowMillis) }
            preferences.updateRecordSet(LocationConsentKeys.UPLOADED_RECORDS) { pruneLocationUsageRecords(it, nowMillis) }
        }
    }

    /** 열람 요청에 쓰는 설치 ID. 앱이 처음 필요할 때 만든다. 철회해도 바뀌지 않는다. */
    suspend fun installId(): String {
        dataStore.data.first()[LocationConsentKeys.INSTALL_ID]?.let { return it }
        var id = ""
        dataStore.edit { preferences ->
            id = preferences[LocationConsentKeys.INSTALL_ID] ?: UUID.randomUUID().toString().also {
                preferences[LocationConsentKeys.INSTALL_ID] = it
            }
        }
        return id
    }

    internal suspend fun uploadState(): LocationUsageUploadState {
        val preferences = dataStore.data.first()
        return LocationUsageUploadState(
            records = preferences[LocationConsentKeys.USAGE_RECORDS].orEmpty(),
            uploaded = preferences[LocationConsentKeys.UPLOADED_RECORDS].orEmpty(),
            pendingServerDeletes = preferences[LocationConsentKeys.PENDING_SERVER_DELETES].orEmpty(),
        )
    }

    /** 서버가 받은 기록을 표시한다. 그사이 철회로 지운 기록은 다시 넣지 않는다. */
    internal suspend fun markUploaded(records: List<LocationUsageRecord>) {
        dataStore.edit { preferences ->
            val stillStored = preferences[LocationConsentKeys.USAGE_RECORDS].orEmpty()
            val uploaded = records.map(::encodeLocationUsageRecord).filter { it in stillStored }
            preferences[LocationConsentKeys.UPLOADED_RECORDS] =
                preferences[LocationConsentKeys.UPLOADED_RECORDS].orEmpty() + uploaded
            preferences[LocationConsentKeys.EVER_UPLOADED] = true
        }
    }

    internal suspend fun markServerDeleted(purposeRaw: String) {
        dataStore.edit { preferences ->
            val remaining = preferences[LocationConsentKeys.PENDING_SERVER_DELETES].orEmpty() - purposeRaw
            if (remaining.isEmpty()) {
                preferences.remove(LocationConsentKeys.PENDING_SERVER_DELETES)
            } else {
                preferences[LocationConsentKeys.PENDING_SERVER_DELETES] = remaining
            }
        }
    }

    private fun MutablePreferences.updateRecordSet(
        key: Preferences.Key<Set<String>>,
        transform: (Set<String>) -> Set<String>,
    ) {
        val stored = this[key] ?: return
        val updated = transform(stored)
        if (updated.isEmpty()) remove(key) else if (updated != stored) this[key] = updated
    }

    private fun MutablePreferences.writePurpose(
        purpose: LocationPurpose,
        status: LocationConsentStatus,
        nowMillis: Long,
    ) {
        this[LocationConsentKeys.status(purpose)] = status.raw
        this[LocationConsentKeys.version(purpose)] = currentVersion
        this[LocationConsentKeys.at(purpose)] = nowMillis
    }
}

internal data class LocationUsageUploadState(
    val records: Set<String>,
    val uploaded: Set<String>,
    val pendingServerDeletes: Set<String>,
)
