package com.ScienceFiction.DronePassAndroid.core.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** 테스트마다 새 파일로 만드는 실제 Preferences DataStore. */
internal fun testPreferencesDataStore(directory: File): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { File(directory, "test_${System.nanoTime()}.preferences_pb") },
    )

/** 위치 API 호출 수를 세는 가짜 원천. */
internal class CountingDeviceLocationSource(
    private val result: () -> DeviceLocation?,
) : DeviceLocationSource {
    var calls = 0
        private set

    override suspend fun currentOrLastKnown(): DeviceLocation? {
        calls += 1
        return result()
    }
}
