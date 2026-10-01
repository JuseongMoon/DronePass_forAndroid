package com.ScienceFiction.DronePassAndroid.core.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** 동의하지 않은 상태의 첫 지도 위치이자 날씨 기준 위치의 마지막 대안(서울시청). */
internal val MapFallbackCenter = Coordinate(latitude = 37.5665, longitude = 126.9780)

internal fun storedMapCenter(preferences: Preferences): Coordinate? {
    val latitude = preferences[LocationConsentKeys.LAST_MAP_CENTER_LATITUDE] ?: return null
    val longitude = preferences[LocationConsentKeys.LAST_MAP_CENTER_LONGITUDE] ?: return null
    return Coordinate(latitude, longitude)
}

/**
 * 날씨·일출/일몰 동의(P2)가 없을 때 그 기준이 되는 지도 중심.
 * 지도 중심은 이용자가 정한 지점이라 개인위치정보가 아니다(위치정보법 해설서 20쪽).
 *
 * 현재 위치 표시(P1)에 동의하면 카메라가 기기 위치를 따라갈 수 있다. 그런 카메라 위치는 기기 위치와 같으므로
 * 기록하지 않는다. 그래서 여기 남는 값은 언제나 이용자가 직접 옮겨 본 지점이다.
 */
@Singleton
class MapCenterStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    @Volatile private var liveCenter: Coordinate? = null

    /** 지도 카메라가 멈출 때 호출한다. [followsDeviceLocation] 이면 기록하지 않는다. */
    suspend fun onMapCenterChanged(center: Coordinate, followsDeviceLocation: Boolean) {
        if (followsDeviceLocation) return
        liveCenter = center
        dataStore.edit { preferences ->
            preferences[LocationConsentKeys.LAST_MAP_CENTER_LATITUDE] = center.latitude
            preferences[LocationConsentKeys.LAST_MAP_CENTER_LONGITUDE] = center.longitude
        }
    }

    /** 현재 위치 표시 동의가 없을 때 앱을 열면 보이는 첫 지도 위치. 저장값이 없으면 null. */
    suspend fun lastViewedCenter(): Coordinate? = storedMapCenter(dataStore.data.first())

    /** 날씨·일출/일몰 기준 위치: 지금 보고 있는 지도 중심 → 마지막으로 본 지도 중심 → 서울시청. */
    suspend fun weatherBasisCenter(): Coordinate =
        liveCenter ?: lastViewedCenter() ?: MapFallbackCenter
}
