package com.ScienceFiction.DronePassAndroid.core.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ScienceFiction.DronePassAndroid.core.data.UserLocationKeys
import com.ScienceFiction.DronePassAndroid.core.legal.LOCATION_TERMS_VERSION
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

class LocationConsentRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val now = Instant.parse("2026-11-12T03:00:00Z").toEpochMilli()
    private val p1 = LocationPurpose.CURRENT_LOCATION
    private val p2 = LocationPurpose.WEATHER_AND_SUN
    private val mapUsage = LocationUsage(LocationUsagePurpose.CURRENT_LOCATION_ON_MAP, LocationRecipient.NONE)
    private val weatherUsage = LocationUsage(LocationUsagePurpose.WEATHER, LocationRecipient.WEATHERKIT_VIA_SERVER)

    private fun newDataStore(): DataStore<Preferences> = testPreferencesDataStore(folder.root)

    @Test
    fun `the four consent combinations allow exactly the chosen purposes`() = runBlocking {
        val combinations = listOf(setOf(p1), setOf(p2), setOf(p1, p2), emptySet())
        for (chosen in combinations) {
            val repository = LocationConsentRepository(newDataStore())
            assertTrue(repository.needsPromptNow())

            repository.submitConsent(chosen, ageConfirmed = chosen.isNotEmpty(), nowMillis = now)

            assertEquals("chosen=$chosen", p1 in chosen, repository.isAllowed(p1))
            assertEquals("chosen=$chosen", p2 in chosen, repository.isAllowed(p2))
            assertEquals("chosen=$chosen", chosen.isNotEmpty(), repository.anyAllowed.first())
            assertFalse("chosen=$chosen", repository.needsPromptNow())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `agreeing without the age confirmation is rejected`() = runBlocking {
        LocationConsentRepository(newDataStore()).submitConsent(setOf(p1), ageConfirmed = false, nowMillis = now)
    }

    @Test
    fun `withdrawing one purpose keeps the other and deletes only its records and cache`() = runBlocking {
        val dataStore = newDataStore()
        val repository = LocationConsentRepository(dataStore)
        repository.submitConsent(setOf(p1, p2), ageConfirmed = true, nowMillis = now)
        repository.recordUsage(mapUsage, now)
        repository.recordUsage(weatherUsage, now)
        dataStore.edit { preferences ->
            preferences[UserLocationKeys.KEY_LAST_LATITUDE] = 37.57
            preferences[UserLocationKeys.KEY_LAST_LONGITUDE] = 126.98
        }

        repository.withdraw(p2, nowMillis = now + 1)

        assertTrue(repository.isAllowed(p1))
        assertFalse(repository.isAllowed(p2))
        assertEquals(listOf(LocationUsagePurpose.CURRENT_LOCATION_ON_MAP), repository.usageRecords.first().map { it.purpose })
        val preferences = dataStore.data.first()
        assertNull(preferences[UserLocationKeys.KEY_LAST_LATITUDE])
        assertNull(preferences[UserLocationKeys.KEY_LAST_LONGITUDE])
        // 철회해도 다시 묻지 않는다(설정 토글로만 다시 동의).
        assertFalse(repository.needsPromptNow())

        repository.withdraw(p1, nowMillis = now + 2)
        assertTrue(repository.usageRecords.first().isEmpty())
        assertFalse(dataStore.data.first()[LocationConsentKeys.AGE_CONFIRMED] ?: true)
    }

    @Test
    fun `only the current state is stored and no consent history remains`() = runBlocking {
        val dataStore = newDataStore()
        val repository = LocationConsentRepository(dataStore)
        repository.submitConsent(setOf(p1, p2), ageConfirmed = true, nowMillis = now)
        repository.withdraw(p1, nowMillis = now + 1)
        repository.submitConsent(setOf(p1), ageConfirmed = true, nowMillis = now + 2)

        val consentKeys = dataStore.data.first().asMap().keys.map { it.name }.filter { it.startsWith("location_consent") }.toSet()

        assertEquals(
            setOf(
                "location_consent_currentLocation_status",
                "location_consent_currentLocation_version",
                "location_consent_currentLocation_at",
                "location_consent_weatherAndSun_status",
                "location_consent_weatherAndSun_version",
                "location_consent_weatherAndSun_at",
                "location_consent_age_confirmed",
            ),
            consentKeys,
        )
        assertEquals(now + 2, repository.state.first()[p1]?.atMillis)
        // 다시 동의할 때 고르지 않은 P2 는 그대로 둔다(동의 시각이 바뀌지 않는다).
        assertEquals(now, repository.state.first()[p2]?.atMillis)
        assertEquals(LOCATION_TERMS_VERSION, repository.state.first()[p2]?.version)
    }

    @Test
    fun `launch maintenance deletes v1 keys including the consent history`() = runBlocking {
        val dataStore = newDataStore()
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("location_consent_status")] = "agreed"
            preferences[stringPreferencesKey("location_consent_version")] = LOCATION_TERMS_VERSION
            preferences[longPreferencesKey("location_consent_at")] = 1L
            preferences[stringPreferencesKey("location_consent_history")] = "1|$LOCATION_TERMS_VERSION|agreed"
            preferences[stringSetPreferencesKey("location_usage_records")] = setOf("2026-11-12|weather")
            preferences[booleanPreferencesKey("location_consent_age_confirmed")] = true
        }
        val repository = LocationConsentRepository(dataStore)

        repository.runLaunchMaintenance(nowMillis = now)

        val names = dataStore.data.first().asMap().keys.map { it.name }.toSet()
        assertFalse(names.contains("location_consent_status"))
        assertFalse(names.contains("location_consent_history"))
        assertFalse(names.contains("location_usage_records"))
        // v1 동의는 v2 목적별 동의로 옮기지 않는다.
        assertFalse(repository.isAllowed(p1))
        assertTrue(repository.needsPromptNow())
    }

    @Test
    fun `usage is recorded only while that purpose is agreed`() = runBlocking {
        val repository = LocationConsentRepository(newDataStore())
        repository.submitConsent(setOf(p1), ageConfirmed = true, nowMillis = now)

        repository.recordUsage(weatherUsage, now)
        repository.recordUsage(mapUsage, now)

        assertEquals(listOf(LocationUsagePurpose.CURRENT_LOCATION_ON_MAP), repository.usageRecords.first().map { it.purpose })
    }

    @Test
    fun `server deletion is queued on withdrawal only after something was uploaded`() = runBlocking {
        val repository = LocationConsentRepository(newDataStore())
        repository.submitConsent(setOf(p1, p2), ageConfirmed = true, nowMillis = now)

        repository.withdraw(p1, nowMillis = now)
        assertTrue(repository.uploadState().pendingServerDeletes.isEmpty())

        repository.markUploaded(java.time.LocalDate.of(2026, 11, 11))
        repository.withdraw(p2, nowMillis = now)
        assertEquals(setOf("weather", "sunriseSunset", "sunriseAlert"), repository.uploadState().pendingServerDeletes)
    }

    @Test
    fun `install id is created once and kept`() = runBlocking {
        val repository = LocationConsentRepository(newDataStore())

        val first = repository.installId()
        repository.submitConsent(setOf(p1), ageConfirmed = true, nowMillis = now)
        repository.withdraw(p1, nowMillis = now)

        assertNotNull(first)
        assertEquals(first, repository.installId())
    }
}
