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
import java.util.Locale

class LocationConsentRulesTest {

    private fun record(
        status: LocationConsentStatus,
        version: String = LOCATION_TERMS_VERSION,
        ageConfirmed: Boolean = status == LocationConsentStatus.AGREED,
    ) = LocationConsentRecord(status, version, atMillis = 0L, ageConfirmed = ageConfirmed)

    @Test
    fun `only a current agreement with the age check allows location use`() {
        assertFalse(isLocationUseAllowed(null, LOCATION_TERMS_VERSION))
        assertTrue(isLocationUseAllowed(record(LocationConsentStatus.AGREED), LOCATION_TERMS_VERSION))
        assertFalse(isLocationUseAllowed(record(LocationConsentStatus.DECLINED), LOCATION_TERMS_VERSION))
        assertFalse(isLocationUseAllowed(record(LocationConsentStatus.WITHDRAWN), LOCATION_TERMS_VERSION))
        assertFalse(
            isLocationUseAllowed(record(LocationConsentStatus.AGREED, ageConfirmed = false), LOCATION_TERMS_VERSION),
        )
        assertFalse(
            isLocationUseAllowed(record(LocationConsentStatus.AGREED, version = "2026-10-12"), LOCATION_TERMS_VERSION),
        )
    }

    @Test
    fun `prompt shows with no record or when the terms version goes up`() {
        assertTrue(locationConsentNeedsPrompt(null, LOCATION_TERMS_VERSION))
        assertFalse(locationConsentNeedsPrompt(record(LocationConsentStatus.AGREED), LOCATION_TERMS_VERSION))
        assertFalse(locationConsentNeedsPrompt(record(LocationConsentStatus.DECLINED), LOCATION_TERMS_VERSION))
        assertFalse(locationConsentNeedsPrompt(record(LocationConsentStatus.WITHDRAWN), LOCATION_TERMS_VERSION))
        assertTrue(locationConsentNeedsPrompt(record(LocationConsentStatus.AGREED), "2099-01-01"))
        assertTrue(locationConsentNeedsPrompt(record(LocationConsentStatus.DECLINED), "2099-01-01"))
    }

    @Test
    fun `consent history round-trips without coordinates`() {
        val entries = listOf(
            LocationConsentHistoryEntry(1L, LOCATION_TERMS_VERSION, LocationConsentStatus.AGREED),
            LocationConsentHistoryEntry(2L, LOCATION_TERMS_VERSION, LocationConsentStatus.WITHDRAWN),
        )
        val encoded = encodeLocationConsentHistory(entries)

        assertEquals("1|$LOCATION_TERMS_VERSION|agreed\n2|$LOCATION_TERMS_VERSION|withdrawn", encoded)
        assertEquals(entries, decodeLocationConsentHistory(encoded))
        assertEquals(emptyList<LocationConsentHistoryEntry>(), decodeLocationConsentHistory(null))
    }

    @Test
    fun `usage records group by KST day and purpose`() {
        // 2026-11-12 23:30 KST 와 2026-11-13 00:30 KST 는 다른 날이다.
        val lateNight = Instant.parse("2026-11-12T14:30:00Z").toEpochMilli()
        val afterMidnight = Instant.parse("2026-11-12T15:30:00Z").toEpochMilli()

        var stored = emptySet<String>()
        stored = addLocationUsageRecord(stored, LocationUsagePurpose.WEATHER, lateNight)
        stored = addLocationUsageRecord(stored, LocationUsagePurpose.WEATHER, lateNight + 1_000)
        stored = addLocationUsageRecord(stored, LocationUsagePurpose.WEATHER, afterMidnight)
        stored = addLocationUsageRecord(stored, LocationUsagePurpose.CURRENT_LOCATION_ON_MAP, afterMidnight)

        assertEquals(
            listOf(
                LocationUsageRecord(LocalDate.of(2026, 11, 13), LocationUsagePurpose.CURRENT_LOCATION_ON_MAP),
                LocationUsageRecord(LocalDate.of(2026, 11, 13), LocationUsagePurpose.WEATHER),
                LocationUsageRecord(LocalDate.of(2026, 11, 12), LocationUsagePurpose.WEATHER),
            ),
            sortedLocationUsageRecords(stored),
        )
    }

    @Test
    fun `usage records keep 190 days and drop unreadable values`() {
        val now = Instant.parse("2026-11-12T03:00:00Z").toEpochMilli()
        val pruned = pruneLocationUsageRecords(
            setOf("2026-05-06|weather", "2026-05-05|weather", "garbage", "2026-11-12|unknownPurpose"),
            now,
        )

        assertEquals(setOf("2026-05-06|weather"), pruned)
    }

    @Test
    fun `usage purposes keep the shared raw values`() {
        assertEquals(
            listOf("currentLocationOnMap", "weather", "sunriseSunset", "sunriseAlert"),
            LocationUsagePurpose.entries.map { it.raw },
        )
        assertNull(LocationUsagePurpose.fromRaw("unknown"))
    }

    @Test
    fun `terms notice shows once per terms version`() {
        assertTrue(termsNoticeNeeded(shownVersion = null, currentVersion = TERMS_NOTICE_VERSION))
        assertTrue(termsNoticeNeeded(shownVersion = "2026-01-01", currentVersion = TERMS_NOTICE_VERSION))
        assertFalse(termsNoticeNeeded(shownVersion = TERMS_NOTICE_VERSION, currentVersion = TERMS_NOTICE_VERSION))
    }

    @Test
    fun `legal dates format per language`() {
        assertEquals("2026년 11월 12일", formatLegalDate("2026-11-12", Locale.KOREAN))
        assertEquals("November 12, 2026", formatLegalDate("2026-11-12", Locale.ENGLISH))
    }
}
