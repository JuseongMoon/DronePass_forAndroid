package com.ScienceFiction.DronePassAndroid.core.location

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 그 목적의 동의 상태가 `agreed` 가 아니면 위치 요청이 0건이어야 한다(사양 §1.2, v2 A-1). */
class DeviceLocationGateTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val seoul = DeviceLocation(latitude = 37.566535, longitude = 126.977969, accuracyMeters = 12f)
    private val weather = LocationUsage(LocationUsagePurpose.WEATHER, LocationRecipient.WEATHERKIT_VIA_SERVER)
    private val sun = LocationUsage(LocationUsagePurpose.SUNRISE_SUNSET, LocationRecipient.WEATHERKIT_VIA_SERVER)
    private val map = LocationUsage(LocationUsagePurpose.CURRENT_LOCATION_ON_MAP, LocationRecipient.NONE)

    @Test
    fun `no consent record means no location request`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val source = CountingDeviceLocationSource { seoul }
        val reader = DeviceLocationReader(repository, source)

        assertEquals(DeviceLocationResult.NotAllowed, reader.read(weather))
        assertEquals(DeviceLocationResult.NotAllowed, reader.read(map))
        assertEquals(0, source.calls)
        assertTrue(repository.usageRecords.first().isEmpty())
    }

    @Test
    fun `a purpose that is not agreed makes no location request even when the other is agreed`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val source = CountingDeviceLocationSource { seoul }
        val reader = DeviceLocationReader(repository, source)
        repository.submitConsent(setOf(LocationPurpose.CURRENT_LOCATION), ageConfirmed = true)

        assertEquals(DeviceLocationResult.NotAllowed, reader.read(weather, sun))
        assertEquals(0, source.calls)

        assertEquals(DeviceLocationResult.Available(seoul), reader.read(map))
        assertEquals(1, source.calls)
    }

    @Test
    fun `declined and withdrawn states make no location request`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val source = CountingDeviceLocationSource { seoul }
        val reader = DeviceLocationReader(repository, source)

        repository.submitConsent(emptySet(), ageConfirmed = false)
        assertEquals(DeviceLocationResult.NotAllowed, reader.read(weather))

        repository.submitConsent(setOf(LocationPurpose.WEATHER_AND_SUN), ageConfirmed = true)
        repository.withdraw(LocationPurpose.WEATHER_AND_SUN)
        assertEquals(DeviceLocationResult.NotAllowed, reader.read(weather))

        assertEquals(0, source.calls)
    }

    @Test
    fun `agreed purpose reads the device location and records each usage`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val source = CountingDeviceLocationSource { seoul }
        val reader = DeviceLocationReader(repository, source)
        repository.submitConsent(setOf(LocationPurpose.WEATHER_AND_SUN), ageConfirmed = true)

        val result = reader.read(weather, sun)

        assertEquals(DeviceLocationResult.Available(seoul), result)
        assertEquals(1, source.calls)
        assertEquals(
            setOf(weather, sun),
            repository.usageRecords.first().map { LocationUsage(it.purpose, it.recipient) }.toSet(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `usages of different consent purposes cannot share one read`() = runBlocking<Unit> {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        DeviceLocationReader(repository, CountingDeviceLocationSource { seoul }).read(weather, map)
    }

    @Test
    fun `agreed state without a location fails instead of falling back and records nothing`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val reader = DeviceLocationReader(repository, CountingDeviceLocationSource { null })
        repository.submitConsent(setOf(LocationPurpose.WEATHER_AND_SUN), ageConfirmed = true)

        assertEquals(DeviceLocationResult.Unavailable, reader.read(weather))
        assertTrue(repository.usageRecords.first().isEmpty())
    }

    @Test
    fun `missing OS permission after consent is reported as permission denied`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        val reader = DeviceLocationReader(repository, CountingDeviceLocationSource { throw SecurityException() })
        repository.submitConsent(setOf(LocationPurpose.WEATHER_AND_SUN), ageConfirmed = true)

        assertEquals(DeviceLocationResult.PermissionDenied, reader.read(weather))
    }

    @Test
    fun `map center ignores camera positions that follow the device and falls back to Seoul City Hall`() = runBlocking {
        val store = MapCenterStore(testPreferencesDataStore(folder.root))

        assertEquals(MapFallbackCenter, store.weatherBasisCenter())

        val busan = Coordinate(35.1796, 129.0756)
        store.onMapCenterChanged(busan, followsDeviceLocation = false)
        assertEquals(busan, store.weatherBasisCenter())
        assertEquals(busan, store.lastViewedCenter())

        // 카메라가 기기 위치를 따라간 중심은 기기 위치와 같아 기록하지 않는다.
        store.onMapCenterChanged(Coordinate(37.5, 127.0), followsDeviceLocation = true)
        assertEquals(busan, store.weatherBasisCenter())
        assertEquals(busan, store.lastViewedCenter())
    }

    @Test
    fun `weather falls back to the map center without consent or with OS permission paused`() {
        assertEquals(
            com.ScienceFiction.DronePassAndroid.feature.weather.WeatherLocationSource.MAP_CENTER,
            com.ScienceFiction.DronePassAndroid.feature.weather.resolveWeatherLocationSource(DeviceLocationResult.NotAllowed),
        )
        // OS 권한을 끈 것은 일시 중지: 동의·기록은 그대로 두고 지도 중심으로 대체한다.
        assertEquals(
            com.ScienceFiction.DronePassAndroid.feature.weather.WeatherLocationSource.MAP_CENTER,
            com.ScienceFiction.DronePassAndroid.feature.weather.resolveWeatherLocationSource(DeviceLocationResult.PermissionDenied),
        )
        // 동의·권한이 있는데 위치를 못 읽으면 지도 중심으로 바꾸지 않는다.
        assertEquals(
            com.ScienceFiction.DronePassAndroid.feature.weather.WeatherLocationSource.UNAVAILABLE,
            com.ScienceFiction.DronePassAndroid.feature.weather.resolveWeatherLocationSource(DeviceLocationResult.Unavailable),
        )
        assertEquals(
            com.ScienceFiction.DronePassAndroid.feature.weather.WeatherLocationSource.DEVICE,
            com.ScienceFiction.DronePassAndroid.feature.weather.resolveWeatherLocationSource(DeviceLocationResult.Available(seoul)),
        )
    }

    @Test
    fun `pausing by OS permission keeps the consent and records`() = runBlocking {
        val repository = LocationConsentRepository(testPreferencesDataStore(folder.root))
        repository.submitConsent(setOf(LocationPurpose.WEATHER_AND_SUN), ageConfirmed = true)
        repository.recordUsage(weather)
        val reader = DeviceLocationReader(repository, CountingDeviceLocationSource { throw SecurityException() })

        assertEquals(DeviceLocationResult.PermissionDenied, reader.read(weather))
        assertTrue(repository.isAllowed(LocationPurpose.WEATHER_AND_SUN))
        assertEquals(1, repository.usageRecords.first().size)
    }
}
