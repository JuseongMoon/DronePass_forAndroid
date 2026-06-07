package com.ScienceFiction.DronePassAndroid.core.data

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserLocationKeysTest {

    @Test
    fun `일출 일몰 알림 복구는 저장된 실제 위치가 있을 때만 좌표를 사용한다`() {
        val preferences = preferencesOf(
            UserLocationKeys.KEY_LAST_LATITUDE to 37.5665,
            UserLocationKeys.KEY_LAST_LONGITUDE to 126.9780,
        )

        assertEquals(37.5665 to 126.9780, storedSunAlarmLocation(preferences))
    }

    @Test
    fun `일출 일몰 알림 복구는 위치가 없으면 iOS처럼 예약 좌표를 만들지 않는다`() {
        assertNull(storedSunAlarmLocation(preferencesOf()))
    }

    @Test
    fun `일출 일몰 알림 복구는 위도와 경도 중 하나만 있어도 예약 좌표를 만들지 않는다`() {
        assertNull(
            storedSunAlarmLocation(
                preferencesOf(UserLocationKeys.KEY_LAST_LATITUDE to 37.5665),
            ),
        )
        assertNull(
            storedSunAlarmLocation(
                preferencesOf(UserLocationKeys.KEY_LAST_LONGITUDE to 126.9780),
            ),
        )
    }
}
