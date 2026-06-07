package com.ScienceFiction.DronePassAndroid.core.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey

/**
 * 사용자의 마지막 알려진 위치를 [androidx.datastore.preferences.core.Preferences] 에
 * 캐시하기 위한 공유 키 집합.
 *
 * 재부팅 후 [com.ScienceFiction.DronePassAndroid.service.BootCompletedReceiver] 가
 * 마지막으로 확인된 실제 좌표를 사용해 일출/일몰을 다시 계산하도록 한다.
 *
 * 캐시 갱신은 [com.ScienceFiction.DronePassAndroid.feature.settings.SettingsViewModel]
 * 등 위치를 조회하는 ViewModel 이 수행한다.
 */
object UserLocationKeys {
    val KEY_LAST_LATITUDE = doublePreferencesKey("last_known_latitude")
    val KEY_LAST_LONGITUDE = doublePreferencesKey("last_known_longitude")
}

internal fun storedSunAlarmLocation(preferences: Preferences): Pair<Double, Double>? {
    val latitude = preferences[UserLocationKeys.KEY_LAST_LATITUDE]
    val longitude = preferences[UserLocationKeys.KEY_LAST_LONGITUDE]
    return if (latitude != null && longitude != null) {
        latitude to longitude
    } else {
        null
    }
}
