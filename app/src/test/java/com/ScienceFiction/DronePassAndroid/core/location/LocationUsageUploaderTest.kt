package com.ScienceFiction.DronePassAndroid.core.location

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

/** 서버 계약 B-2026-10-01: 밀린 기록은 한 요청에 묶고(최대 500개), 값은 고정 코드로 보낸다. */
class LocationUsageUploaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val twoDaysAgo = Instant.parse("2026-11-10T05:00:00Z").toEpochMilli()
    private val yesterday = Instant.parse("2026-11-11T05:00:00Z").toEpochMilli()
    private val today = Instant.parse("2026-11-12T05:00:00Z").toEpochMilli()
    private val tomorrow = Instant.parse("2026-11-13T05:00:00Z").toEpochMilli()
    private val weather = LocationUsage(LocationUsagePurpose.WEATHER, LocationRecipient.WEATHERKIT_VIA_SERVER)
    private val map = LocationUsage(LocationUsagePurpose.CURRENT_LOCATION_ON_MAP, LocationRecipient.NONE)

    /** [failure] 가 있으면 그 예외로 실패한다(예: 서버의 resource-exhausted). */
    private class RecordingServer(var failure: Exception? = null) : LocationUsageServer {
        val records = mutableListOf<Map<String, Any>>()
        val deletes = mutableListOf<Map<String, Any>>()

        override suspend fun record(request: Map<String, Any>) {
            failure?.let { throw it }
            records += request
        }

        override suspend fun delete(request: Map<String, Any>) {
            failure?.let { throw it }
            deletes += request
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun entries(request: Map<String, Any>) = request["entries"] as List<Map<String, Any>>

    private suspend fun agreedRepositoryWithRecords(): LocationConsentRepository {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        repository.submitConsent(LocationPurpose.entries.toSet(), ageConfirmed = true)
        repository.recordUsage(weather, twoDaysAgo)
        repository.recordUsage(map, yesterday)
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
    fun `all past days go in one request with fixed codes and today waits`() = runBlocking {
        val repository = agreedRepositoryWithRecords()
        val server = RecordingServer()
        val uploader = LocationUsageUploader(repository, server)

        uploader.sync(uploadEnabled = true, nowMillis = today)
        uploader.sync(uploadEnabled = true, nowMillis = today)

        assertEquals(1, server.records.size)
        val request = server.records.single()
        assertEquals(repository.installId(), request["installId"])
        assertEquals(
            listOf(
                mapOf("occurredHour" to "2026-11-10T14", "purpose" to "weather", "acquisitionPath" to "googleFusedLocation", "recipient" to "appleWeatherKitViaCompanyServer"),
                mapOf("occurredHour" to "2026-11-11T14", "purpose" to "currentLocationOnMap", "acquisitionPath" to "googleFusedLocation", "recipient" to "none"),
                mapOf("occurredHour" to "2026-11-11T14", "purpose" to "weather", "acquisitionPath" to "googleFusedLocation", "recipient" to "appleWeatherKitViaCompanyServer"),
            ),
            entries(request),
        )

        // 다음 날: 어제(=12일) 기록만 새로 올린다.
        uploader.sync(uploadEnabled = true, nowMillis = tomorrow)
        assertEquals(2, server.records.size)
        assertEquals(listOf("2026-11-12T14"), entries(server.records.last()).map { it["occurredHour"] })
    }

    @Test
    fun `resource exhausted and other failures are retried on the next run`() = runBlocking {
        val repository = agreedRepositoryWithRecords()
        val server = RecordingServer(failure = IllegalStateException("resource-exhausted"))
        val uploader = LocationUsageUploader(repository, server)

        runCatching { uploader.sync(uploadEnabled = true, nowMillis = today) }
        assertTrue(repository.uploadState().uploaded.isEmpty())

        server.failure = null
        uploader.sync(uploadEnabled = true, nowMillis = today)
        assertEquals(3, entries(server.records.single()).size)
    }

    @Test
    fun `more than 500 pending records are sent oldest first over several runs`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        repository.submitConsent(setOf(LocationPurpose.WEATHER_AND_SUN), ageConfirmed = true)
        val start = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
        repeat(520) { hour -> repository.recordUsage(weather, start + hour * 3_600_000L) }
        val server = RecordingServer()
        val uploader = LocationUsageUploader(repository, server)

        uploader.sync(uploadEnabled = true, nowMillis = today)
        uploader.sync(uploadEnabled = true, nowMillis = today)

        assertEquals(listOf(500, 20), server.records.map { entries(it).size })
        assertEquals("2026-10-01T09", entries(server.records.first()).first()["occurredHour"])
    }

    @Test
    fun `withdrawal deletes on the server per purpose and retries after failure`() = runBlocking {
        val repository = agreedRepositoryWithRecords()
        val server = RecordingServer()
        val uploader = LocationUsageUploader(repository, server)
        uploader.sync(uploadEnabled = true, nowMillis = today)

        repository.withdraw(LocationPurpose.WEATHER_AND_SUN)
        assertTrue(repository.uploadState().uploaded.none { it.contains("|weather|") })
        server.failure = IllegalStateException("offline")
        runCatching { uploader.sync(uploadEnabled = false, nowMillis = today) }
        assertEquals(3, repository.uploadState().pendingServerDeletes.size)

        server.failure = null
        // 플래그가 꺼져도 이미 올린 기록의 삭제는 한다.
        uploader.sync(uploadEnabled = false, nowMillis = today)
        assertEquals(setOf("weather", "sunriseSunset", "sunriseAlert"), server.deletes.map { it["purpose"] }.toSet())
        assertTrue(repository.uploadState().pendingServerDeletes.isEmpty())
    }
}
