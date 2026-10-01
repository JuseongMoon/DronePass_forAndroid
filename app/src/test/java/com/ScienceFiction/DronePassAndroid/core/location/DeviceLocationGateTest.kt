package com.ScienceFiction.DronePassAndroid.core.location

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 동의 상태가 `agreed` 가 아니면 위치 요청이 0건이어야 한다(사양 §1.2). */
class DeviceLocationGateTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val seoul = DeviceLocation(latitude = 37.566535, longitude = 126.977969, accuracyMeters = 12f)

    @Test
    fun `no consent record means no location request`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val source = CountingDeviceLocationSource { seoul }
        val reader = DeviceLocationReader(repository, source)

        val result = reader.read(LocationUsagePurpose.WEATHER)

        assertEquals(DeviceLocationResult.NotAllowed, result)
        assertEquals(0, source.calls)
        assertTrue(repository.usageRecords.first().isEmpty())
    }

    @Test
    fun `declined and withdrawn states make no location request`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val source = CountingDeviceLocationSource { seoul }
        val reader = DeviceLocationReader(repository, source)

        repository.decline()
        assertEquals(DeviceLocationResult.NotAllowed, reader.read(LocationUsagePurpose.WEATHER))

        repository.agree(ageConfirmed = true)
        repository.withdraw()
        assertEquals(DeviceLocationResult.NotAllowed, reader.read(LocationUsagePurpose.SUNRISE_ALERT))

        assertEquals(0, source.calls)
    }

    @Test
    fun `agreed state reads the device location and records each purpose`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val source = CountingDeviceLocationSource { seoul }
        val reader = DeviceLocationReader(repository, source)
        repository.agree(ageConfirmed = true)

        val result = reader.read(LocationUsagePurpose.WEATHER, LocationUsagePurpose.SUNRISE_SUNSET)

        assertEquals(DeviceLocationResult.Available(seoul), result)
        assertEquals(1, source.calls)
        assertEquals(
            setOf(LocationUsagePurpose.WEATHER, LocationUsagePurpose.SUNRISE_SUNSET),
            repository.usageRecords.first().map { it.purpose }.toSet(),
        )
    }

    @Test
    fun `agreed state without a location fails instead of falling back and records nothing`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val reader = DeviceLocationReader(repository, CountingDeviceLocationSource { null })
        repository.agree(ageConfirmed = true)

        assertEquals(DeviceLocationResult.Unavailable, reader.read(LocationUsagePurpose.WEATHER))
        assertTrue(repository.usageRecords.first().isEmpty())
    }

    @Test
    fun `missing OS permission after consent is reported as permission denied`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val reader = DeviceLocationReader(repository, CountingDeviceLocationSource { throw SecurityException() })
        repository.agree(ageConfirmed = true)

        assertEquals(DeviceLocationResult.PermissionDenied, reader.read(LocationUsagePurpose.WEATHER))
    }

    @Test
    fun `map center is remembered only without consent and falls back to Seoul City Hall`() = runBlocking {
        val dataStore = testPreferencesDataStore(folder.root)
        val repository = LocationConsentRepository(dataStore)
        val store = MapCenterStore(dataStore)

        assertEquals(MapFallbackCenter, store.weatherBasisCenter())

        val busan = Coordinate(35.1796, 129.0756)
        store.onMapCenterChanged(busan)
        assertEquals(busan, store.weatherBasisCenter())
        assertEquals(busan, store.lastViewedCenter())

        // 동의 상태에서는 지도가 기기 위치를 따라갈 수 있어 기록하지 않는다.
        repository.agree(ageConfirmed = true)
        store.onMapCenterChanged(Coordinate(37.5, 127.0))
        assertEquals(busan, store.lastViewedCenter())

        // 철회하면 저장값과 메모리 값이 모두 비워진다.
        repository.withdraw()
        store.clearLiveCenter()
        assertEquals(MapFallbackCenter, store.weatherBasisCenter())
    }
}
