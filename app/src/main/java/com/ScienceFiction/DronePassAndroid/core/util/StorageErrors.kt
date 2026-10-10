package com.ScienceFiction.DronePassAndroid.core.util

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 기기 저장소(Room·DataStore) 오류. 저장 공간이 가득 차거나 설정 파일이 깨지면 이 예외가 launch 밖으로 나가
 * 앱이 종료됐다. 지도 이동처럼 매번 저장하는 동작도 있어, 종료 대신 한 번 안내한다.
 */
internal fun isRecoverableStorageError(error: Throwable): Boolean =
    error is IOException || error is SQLiteException || error is CorruptionException

/** 저장 실패 안내. MainActivity 가 받아 알림창을 띄운다. */
internal object StorageErrorBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun publish() {
        _events.tryEmit(Unit)
    }
}

/**
 * [launch] 와 같되 저장소 오류는 앱을 종료하지 않고 [StorageErrorBus] 로 알린다. 다른 예외와 취소는 그대로 던진다.
 */
internal fun CoroutineScope.launchCatchingStorageErrors(
    onStorageError: (Exception) -> Unit = ::reportStorageError,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!isRecoverableStorageError(e)) throw e
        onStorageError(e)
    }
}

private fun reportStorageError(error: Exception) {
    Log.e("StorageError", "기기 저장소 쓰기 실패", error)
    StorageErrorBus.publish()
}
