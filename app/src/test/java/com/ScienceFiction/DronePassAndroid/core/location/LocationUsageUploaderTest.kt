package com.ScienceFiction.DronePassAndroid.core.location

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.LocalDate

class LocationUsageUploaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val yesterday = Instant.parse("2026-11-11T05:00:00Z").toEpochMilli()
    private val today = Instant.parse("2026-11-12T05:00:00Z").toEpochMilli()
    private val weather = LocationUsage(LocationUsagePurpose.WEATHER, LocationRecipient.WEATHERKIT_VIA_SERVER)

    private class RecordingServer(var fail: Boolean = false) : LocationUsageServer {
        val records = mutableListOf<Map<String, Any>>()
        val deletes = mutableListOf<Map<String, Any>>()

        override suspend fun record(request: Map<String, Any>) {
            if (fail) error("offline")
            records += request
        }

        override suspend fun delete(request: Map<String, Any>) {
            if (fail) error("offline")
            deletes += request
        }
    }

    private suspend fun agreedRepositoryWithRecords(): LocationConsentRepository {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        repository.submitConsent(setOf(LocationPurpose.WEATHER_AND_SUN), ageConfirmed = true)
        repository.recordUsage(weather, yesterday)
        repository.recordUsage(weather, today)
        return repository
    }

    @Test
    fun `nothing is uploaded while the remote flag is off`() = runBlocking {
        val server = RecordingServer()
        val uploader = LocationUsageUploader(agreedRepositoryWithRecords(), server)

        uploader.sync(uploadEnabled = false, nowMillis = today)

        assertTrue(server.records.isEmpty())
    }

    @Test
    fun `past days are uploaded once per day and today waits`() = runBlocking {
        val repository = agreedRepositoryWithRecords()
        val server = RecordingServer()
        val uploader = LocationUsageUploader(repository, server)

        uploader.sync(uploadEnabled = true, nowMillis = today)
        uploader.sync(uploadEnabled = true, nowMillis = today)

        assertEquals(1, server.records.size)
        @Suppress("UNCHECKED_CAST")
        val entries = server.records.single()["entries"] as List<Map<String, Any>>
        assertEquals(listOf("2026-11-11T14"), entries.map { it["occurredHour"] })
        assertEquals(repository.installId(), server.records.single()["installId"])
        assertEquals(LocalDate.of(2026, 11, 11), repository.uploadState().uploadedThrough)
    }

    @Test
    fun `failed uploads are retried on the next sync`() = runBlocking {
        val repository = agreedRepositoryWithRecords()
        val server = RecordingServer(fail = true)
        val uploader = LocationUsageUploader(repository, server)

        runCatching { uploader.sync(uploadEnabled = true, nowMillis = today) }
        assertEquals(null, repository.uploadState().uploadedThrough)

        server.fail = false
        uploader.sync(uploadEnabled = true, nowMillis = today)
        assertEquals(1, server.records.size)
    }

    @Test
    fun `withdrawal deletes on the server per purpose and retries after failure`() = runBlocking {
        val repository = agreedRepositoryWithRecords()
        val server = RecordingServer()
        val uploader = LocationUsageUploader(repository, server)
        uploader.sync(uploadEnabled = true, nowMillis = today)

        repository.withdraw(LocationPurpose.WEATHER_AND_SUN)
        server.fail = true
        runCatching { uploader.sync(uploadEnabled = false, nowMillis = today) }
        assertEquals(3, repository.uploadState().pendingServerDeletes.size)

        server.fail = false
        // 플래그가 꺼져도 이미 올린 기록의 삭제는 한다.
        uploader.sync(uploadEnabled = false, nowMillis = today)
        assertEquals(setOf("weather", "sunriseSunset", "sunriseAlert"), server.deletes.map { it["purpose"] }.toSet())
        assertTrue(repository.uploadState().pendingServerDeletes.isEmpty())
    }
}
