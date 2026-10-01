package com.ScienceFiction.DronePassAndroid.core.location

import com.ScienceFiction.DronePassAndroid.app.termsNoticeNeeded
import com.ScienceFiction.DronePassAndroid.core.legal.LOCATION_TERMS_VERSION
import com.ScienceFiction.DronePassAndroid.core.legal.TERMS_NOTICE_VERSION
import com.ScienceFiction.DronePassAndroid.core.legal.formatLegalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class LocationConsentRulesTest {

    private val p1 = LocationPurpose.CURRENT_LOCATION
    private val p2 = LocationPurpose.WEATHER_AND_SUN

    private fun state(
        p1Status: LocationConsentStatus? = null,
        p2Status: LocationConsentStatus? = null,
        version: String = LOCATION_TERMS_VERSION,
        ageConfirmed: Boolean = true,
    ) = LocationConsentState(
        purposes = buildMap {
            p1Status?.let { put(p1, LocationPurposeConsent(it, version, 0L)) }
            p2Status?.let { put(p2, LocationPurposeConsent(it, version, 0L)) }
        },
        ageConfirmed = ageConfirmed,
    )

    @Test
    fun `each purpose is allowed only by its own current agreement with the age check`() {
        val onlyMap = state(p1Status = LocationConsentStatus.AGREED, p2Status = LocationConsentStatus.DECLINED)

        assertTrue(isLocationPurposeAllowed(onlyMap, p1, LOCATION_TERMS_VERSION))
        assertFalse(isLocationPurposeAllowed(onlyMap, p2, LOCATION_TERMS_VERSION))
        assertTrue(isAnyLocationPurposeAllowed(onlyMap, LOCATION_TERMS_VERSION))

        assertFalse(isLocationPurposeAllowed(LocationConsentState.Empty, p1, LOCATION_TERMS_VERSION))
        assertFalse(
            isLocationPurposeAllowed(state(p1Status = LocationConsentStatus.AGREED, ageConfirmed = false), p1, LOCATION_TERMS_VERSION),
        )
        assertFalse(
            isLocationPurposeAllowed(state(p1Status = LocationConsentStatus.AGREED, version = "2026-10-06"), p1, LOCATION_TERMS_VERSION),
        )
        assertFalse(isLocationPurposeAllowed(state(p2Status = LocationConsentStatus.WITHDRAWN), p2, LOCATION_TERMS_VERSION))
        assertFalse(isAnyLocationPurposeAllowed(state(LocationConsentStatus.DECLINED, LocationConsentStatus.WITHDRAWN), LOCATION_TERMS_VERSION))
    }

    @Test
    fun `prompt shows while a purpose is undecided or the terms version goes up`() {
        assertTrue(locationConsentNeedsPrompt(LocationConsentState.Empty, LOCATION_TERMS_VERSION))
        assertTrue(locationConsentNeedsPrompt(state(p1Status = LocationConsentStatus.AGREED), LOCATION_TERMS_VERSION))
        assertFalse(
            locationConsentNeedsPrompt(state(LocationConsentStatus.AGREED, LocationConsentStatus.DECLINED), LOCATION_TERMS_VERSION),
        )
        assertFalse(
            locationConsentNeedsPrompt(state(LocationConsentStatus.WITHDRAWN, LocationConsentStatus.WITHDRAWN), LOCATION_TERMS_VERSION),
        )
        assertTrue(locationConsentNeedsPrompt(state(LocationConsentStatus.AGREED, LocationConsentStatus.AGREED), "2099-01-01"))
    }

    @Test
    fun `consent submission agrees to chosen purposes and keeps existing agreements`() {
        // P1 만 고름
        assertEquals(
            mapOf(p1 to LocationConsentStatus.AGREED, p2 to LocationConsentStatus.DECLINED),
            resolveConsentSubmission(LocationConsentState.Empty, setOf(p1), LOCATION_TERMS_VERSION),
        )
        // 동의하지 않음
        assertEquals(
            mapOf(p1 to LocationConsentStatus.DECLINED, p2 to LocationConsentStatus.DECLINED),
            resolveConsentSubmission(LocationConsentState.Empty, emptySet(), LOCATION_TERMS_VERSION),
        )
        // 이미 P1 에 동의한 상태에서 설정으로 P2 만 새로 동의: P1 은 그대로
        assertEquals(
            mapOf(p1 to LocationConsentStatus.AGREED, p2 to LocationConsentStatus.AGREED),
            resolveConsentSubmission(
                state(LocationConsentStatus.AGREED, LocationConsentStatus.WITHDRAWN),
                setOf(p2),
                LOCATION_TERMS_VERSION,
            ),
        )
    }

    @Test
    fun `usage records group by KST hour, purpose and recipient`() {
        val at1400 = Instant.parse("2026-11-12T05:00:00Z").toEpochMilli()
        val at1459 = Instant.parse("2026-11-12T05:59:59Z").toEpochMilli()
        val at1500 = Instant.parse("2026-11-12T06:00:00Z").toEpochMilli()
        val weather = LocationUsage(LocationUsagePurpose.WEATHER, LocationRecipient.WEATHERKIT_VIA_SERVER)
        val map = LocationUsage(LocationUsagePurpose.CURRENT_LOCATION_ON_MAP, LocationRecipient.NONE)

        var stored = emptySet<String>()
        stored = addLocationUsageRecord(stored, weather, at1400)
        stored = addLocationUsageRecord(stored, weather, at1459)
        stored = addLocationUsageRecord(stored, weather, at1500)
        stored = addLocationUsageRecord(stored, map, at1500)

        assertEquals(
            setOf(
                "2026-11-12T14|weather|weatherKitViaServer",
                "2026-11-12T15|weather|weatherKitViaServer",
                "2026-11-12T15|currentLocationOnMap|none",
            ),
            stored,
        )
        val sorted = sortedLocationUsageRecords(stored)
        assertEquals(LocalDateTime.of(2026, 11, 12, 15, 0), sorted.first().hour)
        assertEquals(LocationUsagePurpose.CURRENT_LOCATION_ON_MAP, sorted.first().purpose)
        assertEquals("2026-11-12T15", sorted.first().occurredHour)
        assertEquals("2026-11-12 15:00", locationUsageHourText(sorted.first()))
    }

    @Test
    fun `usage records keep 190 days and drop unreadable or v1 values`() {
        val now = Instant.parse("2026-11-12T03:00:00Z").toEpochMilli()
        val pruned = pruneLocationUsageRecords(
            setOf(
                "2026-05-06T09|weather|weatherKitViaServer",
                "2026-05-05T09|weather|weatherKitViaServer",
                "2026-11-12|weather", // v1 형식
                "garbage",
            ),
            now,
        )

        assertEquals(setOf("2026-05-06T09|weather|weatherKitViaServer"), pruned)
    }

    @Test
    fun `withdrawing a purpose removes only that purpose's records`() {
        val stored = setOf(
            "2026-11-12T14|currentLocationOnMap|none",
            "2026-11-12T14|weather|weatherKitViaServer",
            "2026-11-12T14|sunriseAlert|none",
        )

        assertEquals(
            setOf("2026-11-12T14|currentLocationOnMap|none"),
            removeLocationUsageRecords(stored, LocationPurpose.WEATHER_AND_SUN),
        )
        assertEquals(
            setOf("2026-11-12T14|weather|weatherKitViaServer", "2026-11-12T14|sunriseAlert|none"),
            removeLocationUsageRecords(stored, LocationPurpose.CURRENT_LOCATION),
        )
    }

    @Test
    fun `one upload batch holds every past unsent record oldest first`() {
        val stored = setOf(
            "2026-11-10T09|weather|weatherKitViaServer",
            "2026-11-11T08|currentLocationOnMap|none",
            "2026-11-11T21|weather|weatherKitViaServer",
            "2026-11-12T14|weather|weatherKitViaServer", // 오늘: 아직 올리지 않는다
        )
        val now = Instant.parse("2026-11-12T05:00:00Z").toEpochMilli()

        val all = pendingUploadBatch(stored, uploaded = emptySet(), nowMillis = now)
        assertEquals(listOf("2026-11-10T09", "2026-11-11T08", "2026-11-11T21"), all.map { it.occurredHour })

        val rest = pendingUploadBatch(stored, uploaded = setOf("2026-11-10T09|weather|weatherKitViaServer"), nowMillis = now)
        assertEquals(listOf("2026-11-11T08", "2026-11-11T21"), rest.map { it.occurredHour })
    }

    @Test
    fun `upload batch is capped at 500 oldest records`() {
        val start = LocalDateTime.of(2026, 10, 1, 0, 0)
        val stored = (0 until 600).map { offset ->
            "${start.plusHours(offset.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH"))}|weather|weatherKitViaServer"
        }.toSet()
        val now = Instant.parse("2026-11-12T05:00:00Z").toEpochMilli()

        val batch = pendingUploadBatch(stored, uploaded = emptySet(), nowMillis = now)

        assertEquals(500, batch.size)
        assertEquals("2026-10-01T00", batch.first().occurredHour)
        assertEquals(LocationUsageUploadLimit, batch.size)
    }

    @Test
    fun `server request carries the B-3 fields and no coordinates`() {
        val record = decodeLocationUsageRecord("2026-11-12T14|weather|weatherKitViaServer")!!

        val request = recordLocationUsageRequest("install-1", listOf(record))

        assertEquals(setOf("installId", "entries"), request.keys)
        @Suppress("UNCHECKED_CAST")
        val entry = (request["entries"] as List<Map<String, Any>>).single()
        assertEquals(
            mapOf(
                "occurredHour" to "2026-11-12T14",
                "purpose" to "weather",
                "acquisitionPath" to "googleFusedLocation",
                "recipient" to "appleWeatherKitViaCompanyServer",
            ),
            entry,
        )
        assertEquals(mapOf("installId" to "install-1", "purpose" to "weather"), deleteLocationUsageRequest("install-1", "weather"))
    }

    @Test
    fun `stored records map to server codes and unmappable old entries stay out of uploads`() {
        val now = Instant.parse("2026-11-12T05:00:00Z").toEpochMilli()
        val stored = setOf(
            "2026-11-11T08|weather|weatherKitViaServer", // 기기 저장 코드 → 서버 코드로 바꿔 보낸다
            "2026-11-11T09|currentLocationOnMap|none",
            "2026-11-11|weather", // v1 형식
            "2026-11-11T10|weather|회사 서버 경유 Apple(WeatherKit)", // 표시용 문자열
            "2026-11-11T11|weather|WeatherKitViaServer", // 대소문자가 다른 값
            "2026-11-11T12|weather|", // 빈 값
            "2026-11-11T13|unknownPurpose|none",
        )

        val batch = pendingUploadBatch(stored, uploaded = emptySet(), nowMillis = now)
        @Suppress("UNCHECKED_CAST")
        val entries = recordLocationUsageRequest("install-1", batch)["entries"] as List<Map<String, Any>>

        assertEquals(listOf("2026-11-11T08", "2026-11-11T09"), entries.map { it["occurredHour"] })
        assertEquals(listOf("appleWeatherKitViaCompanyServer", "none"), entries.map { it["recipient"] })
        assertTrue(entries.all { it["acquisitionPath"] == "googleFusedLocation" })
        val allowedRecipients = setOf("none", "appleWeatherKit", "appleWeatherKitViaCompanyServer")
        assertTrue(entries.all { it["recipient"] in allowedRecipients })
        // 매핑할 수 없는 항목은 실행할 때 기기 기록에서도 정리된다.
        assertEquals(
            setOf("2026-11-11T08|weather|weatherKitViaServer", "2026-11-11T09|currentLocationOnMap|none"),
            pruneLocationUsageRecords(stored, now),
        )
    }

    @Test
    fun `server receives fixed codes while the screen shows localized labels`() {
        assertEquals(listOf("none", "appleWeatherKitViaCompanyServer"), LocationRecipient.entries.map { it.uploadValue })
        assertEquals("googleFusedLocation", LocationAcquisitionPath)
    }

    @Test
    fun `usage purposes keep the shared raw values and consent purposes`() {
        assertEquals(
            listOf("currentLocationOnMap", "weather", "sunriseSunset", "sunriseAlert"),
            LocationUsagePurpose.entries.map { it.raw },
        )
        assertEquals(
            listOf(p1, p2, p2, p2),
            LocationUsagePurpose.entries.map { it.consentPurpose },
        )
        assertEquals(listOf("currentLocation", "weatherAndSun"), LocationPurpose.entries.map { it.raw })
        assertNull(LocationUsagePurpose.fromRaw("unknown"))
    }

    @Test
    fun `terms notice shows once per terms version`() {
        assertTrue(termsNoticeNeeded(shownVersion = null, currentVersion = TERMS_NOTICE_VERSION))
        assertTrue(termsNoticeNeeded(shownVersion = "2026-01-01", currentVersion = TERMS_NOTICE_VERSION))
        assertFalse(termsNoticeNeeded(shownVersion = TERMS_NOTICE_VERSION, currentVersion = TERMS_NOTICE_VERSION))
    }

    @Test
    fun `legal dates are the confirmed N and T`() {
        assertEquals("2026-10-07", LOCATION_TERMS_VERSION)
        assertEquals("2026-11-06", TERMS_NOTICE_VERSION)
        assertEquals("2026년 11월 6일", formatLegalDate(TERMS_NOTICE_VERSION, Locale.KOREAN))
        assertEquals("November 6, 2026", formatLegalDate(TERMS_NOTICE_VERSION, Locale.ENGLISH))
    }

    @Test
    fun `legal dates format per language`() {
        assertEquals("2026년 11월 12일", formatLegalDate("2026-11-12", Locale.KOREAN))
        assertEquals("November 12, 2026", formatLegalDate("2026-11-12", Locale.ENGLISH))
    }
}
