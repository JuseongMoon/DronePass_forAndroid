package com.ScienceFiction.DronePassAndroid.core.data

import androidx.datastore.preferences.core.doublePreferencesKey

/**
 * 사용자의 마지막 알려진 위치를 [androidx.datastore.preferences.core.Preferences] 에
 * 캐시하기 위한 공유 키 집합.
 *
 * 재부팅 후 [com.ScienceFiction.DronePassAndroid.service.BootCompletedReceiver] 가
 * 위치 권한 없이도 가까운 좌표를 사용해 일출/일몰을 다시 계산하도록 한다.
 *
 * 캐시 갱신은 [com.ScienceFiction.DronePassAndroid.feature.settings.SettingsViewModel]
 * 등 위치를 조회하는 ViewModel 이 수행한다.
 */
object UserLocationKeys {
    val KEY_LAST_LATITUDE = doublePreferencesKey("last_known_latitude")
    val KEY_LAST_LONGITUDE = doublePreferencesKey("last_known_longitude")

    /** 사용자 위치를 알 수 없을 때 사용할 폴백 좌표 (서울 시청) */
    const val FALLBACK_LATITUDE = 37.5665
    const val FALLBACK_LONGITUDE = 126.9780
}
