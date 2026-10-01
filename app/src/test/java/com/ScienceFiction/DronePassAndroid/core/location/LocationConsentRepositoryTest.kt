package com.ScienceFiction.DronePassAndroid.core.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import com.ScienceFiction.DronePassAndroid.core.legal.LOCATION_TERMS_VERSION
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.LocalDate

class LocationConsentRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val now = Instant.parse("2026-11-12T03:00:00Z").toEpochMilli()

    private fun newDataStore(): DataStore<Preferences> = testPreferencesDataStore(folder.root)

    @Test
    fun `consent goes none to agreed to withdrawn to agreed again`() = runBlocking {
        val repository = LocationConsentRepository(newDataStore())

        assertNull(repository.record.first())
        assertTrue(repository.needsPromptNow())
        assertFalse(repository.isAllowed())

        repository.agree(ageConfirmed = true, nowMillis = now)
        assertTrue(repository.isAllowed())
        assertFalse(repository.needsPromptNow())
        assertEquals(LOCATION_TERMS_VERSION, repository.record.first()?.version)

        repository.withdraw(nowMillis = now + 1)
        assertFalse(repository.isAllowed())
        assertEquals(LocationConsentStatus.WITHDRAWN, repository.record.first()?.status)
        // 철회는 다시 묻지 않는다(설정 토글로만 다시 동의).
        assertFalse(repository.needsPromptNow())

        repository.agree(ageConfirmed = true, nowMillis = now + 2)
        assertTrue(repository.isAllowed())

        // 동의 이력은 지우지 않고 모두 남는다.
        assertEquals(
            listOf(LocationConsentStatus.AGREED, LocationConsentStatus.WITHDRAWN, LocationConsentStatus.AGREED),
            repository.history.first().map { it.action },
        )
    }

    @Test
    fun `declining is stored and not prompted again for the same terms version`() = runBlocking {
        val repository = LocationConsentRepository(newDataStore())

        repository.decline(nowMillis = now)

        assertFalse(repository.isAllowed())
        assertFalse(repository.needsPromptNow())
        val record = repository.record.first()
        assertEquals(LocationConsentStatus.DECLINED, record?.status)
        assertEquals(now, record?.atMillis)
        assertEquals(false, record?.ageConfirmed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `agreeing without the age confirmation is rejected`() = runBlocking {
        LocationConsentRepository(newDataStore()).agree(ageConfirmed = false, nowMillis = now)
    }

    @Test
    fun `an agreement to older terms is not allowed and asks again`() = runBlocking {
        val dataStore = newDataStore()
        dataStore.edit { preferences ->
            preferences[LocationConsentKeys.STATUS] = LocationConsentStatus.AGREED.raw
            preferences[LocationConsentKeys.VERSION] = "2026-01-01"
            preferences[LocationConsentKeys.AGE_CONFIRMED] = true
        }
        val repository = LocationConsentRepository(dataStore)

        assertFalse(repository.isAllowed())
        assertTrue(repository.needsPromptNow())
    }

    @Test
    fun `withdrawal deletes usage records and every cached location but keeps the history`() = runBlocking {
        val dataStore = newDataStore()
        val repository = LocationConsentRepository(dataStore)
        val mapCenterStore = MapCenterStore(dataStore)
        // 동의 전에 본 지도 중심(이용자가 정한 지점)
        mapCenterStore.onMapCenterChanged(Coordinate(35.1, 129.0))
        repository.agree(ageConfirmed = true, nowMillis = now)
        repository.recordUsage(LocationUsagePurpose.WEATHER, nowMillis = now)
        dataStore.edit { preferences ->
            preferences[UserLocationKeys.KEY_LAST_LATITUDE] = 37.5
            preferences[UserLocationKeys.KEY_LAST_LONGITUDE] = 127.0
        }

        repository.withdraw(nowMillis = now + 1)

        val preferences = dataStore.data.first()
        assertTrue(repository.usageRecords.first().isEmpty())
        assertNull(preferences[UserLocationKeys.KEY_LAST_LATITUDE])
        assertNull(preferences[UserLocationKeys.KEY_LAST_LONGITUDE])
        assertNull(storedMapCenter(preferences))
        assertEquals(2, repository.history.first().size)
    }

    @Test
    fun `usage is recorded only while consent is agreed`() = runBlocking {
        val repository = LocationConsentRepository(newDataStore())

        repository.recordUsage(LocationUsagePurpose.WEATHER, nowMillis = now)
        assertTrue(repository.usageRecords.first().isEmpty())

        repository.agree(ageConfirmed = true, nowMillis = now)
        repository.recordUsage(LocationUsagePurpose.WEATHER, nowMillis = now)
        repository.recordUsage(LocationUsagePurpose.WEATHER, nowMillis = now + 60_000)
        repository.recordUsage(LocationUsagePurpose.SUNRISE_ALERT, nowMillis = now)

        assertEquals(
            listOf(
                LocationUsageRecord(LocalDate.of(2026, 11, 12), LocationUsagePurpose.WEATHER),
                LocationUsageRecord(LocalDate.of(2026, 11, 12), LocationUsagePurpose.SUNRISE_ALERT),
            ),
            repository.usageRecords.first(),
        )
    }

    @Test
    fun `pruning at launch drops records older than the retention period`() = runBlocking {
        val dataStore = newDataStore()
        val repository = LocationConsentRepository(dataStore)
        dataStore.edit { preferences ->
            preferences[LocationConsentKeys.USAGE_RECORDS] = setOf(
                "2026-11-12|weather",
                "2026-05-06|weather", // 190일 전: 남긴다
                "2026-05-05|sunriseAlert", // 191일 전: 지운다
            )
        }

        repository.pruneUsageRecords(nowMillis = now)

        assertEquals(
            setOf("2026-11-12|weather", "2026-05-06|weather"),
            dataStore.data.first()[LocationConsentKeys.USAGE_RECORDS],
        )
    }
}
