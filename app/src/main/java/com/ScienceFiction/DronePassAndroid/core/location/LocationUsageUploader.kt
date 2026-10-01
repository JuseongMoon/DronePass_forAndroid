package com.ScienceFiction.DronePassAndroid.core.location

import android.util.Log
import com.ScienceFiction.DronePassAndroid.subscription.RemoteFeatureFlags
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

internal const val RecordLocationUsageCallable = "recordLocationUsage"
internal const val DeleteLocationUsageCallable = "deleteLocationUsage"

/** `recordLocationUsage` 요청 본문(서버 계약 B-2026-10-01). 좌표는 없고 값은 고정 코드다. */
internal fun recordLocationUsageRequest(installId: String, records: List<LocationUsageRecord>): Map<String, Any> = mapOf(
    "installId" to installId,
    "entries" to records.map { record ->
        mapOf(
            "occurredHour" to record.occurredHour,
            "purpose" to record.purpose.raw,
            "acquisitionPath" to LocationAcquisitionPath,
            "recipient" to record.recipient.uploadValue,
        )
    },
)

/** `deleteLocationUsage` 요청 본문(사양 v2 B-4). [purpose] 는 확인자료 목적 값이다. */
internal fun deleteLocationUsageRequest(installId: String, purpose: String): Map<String, Any> =
    mapOf("installId" to installId, "purpose" to purpose)

/** 서버와 주고받을 일. 테스트에서는 가짜로 바꾼다. */
interface LocationUsageServer {
    suspend fun record(request: Map<String, Any>)
    suspend fun delete(request: Map<String, Any>)
}

/** 기본 FirebaseApp 의 callable. 좌표가 없는 요청이라 로그인 토큰이 함께 가도 된다(사양 v2 B-3). */
@Singleton
class FirebaseLocationUsageServer @Inject constructor(
    private val functions: FirebaseFunctions,
) : LocationUsageServer {
    override suspend fun record(request: Map<String, Any>) {
        functions.getHttpsCallable(RecordLocationUsageCallable).call(request).await()
    }

    override suspend fun delete(request: Map<String, Any>) {
        functions.getHttpsCallable(DeleteLocationUsageCallable).call(request).await()
    }
}

/**
 * 확인자료 서버 동기화. 앱을 실행할 때 서버 삭제 대기열을 먼저 처리하고, 지난 날짜의 밀린 기록을 한 번에 올린다.
 * 실패하면 다음 실행 때 다시 시도한다. 업로드는 원격 플래그가 켜졌을 때만 한다(서버 함수 배포 전에는 꺼 둔다).
 */
@Singleton
class LocationUsageUploader @Inject constructor(
    private val consentRepository: LocationConsentRepository,
    private val server: LocationUsageServer,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    @Volatile private var observingFlag = false

    fun syncAsync() {
        scope.launch { runCatching { sync() }.onFailure { Log.w(TAG, "확인자료 동기화 실패", it) } }
        if (!observingFlag) {
            observingFlag = true
            // 원격 설정을 늦게 받아 플래그가 켜지면 그때 한 번 더 올린다.
            scope.launch {
                RemoteFeatureFlags.locationAuditUploadEnabled.filter { it }.collect {
                    runCatching { sync() }.onFailure { Log.w(TAG, "확인자료 동기화 실패", it) }
                }
            }
        }
    }

    internal suspend fun sync(
        uploadEnabled: Boolean = RemoteFeatureFlags.locationAuditUploadEnabled.value,
        nowMillis: Long = System.currentTimeMillis(),
    ) = mutex.withLock {
        val state = consentRepository.uploadState()
        if (state.pendingServerDeletes.isNotEmpty()) {
            val installId = consentRepository.installId()
            state.pendingServerDeletes.forEach { purpose ->
                server.delete(deleteLocationUsageRequest(installId, purpose))
                consentRepository.markServerDeleted(purpose)
            }
        }
        if (!uploadEnabled) return@withLock
        // 서버가 installId 당 시간에 한 번만 받으므로 밀린 기록을 한 요청에 담는다(최대 500개).
        // 실패(resource-exhausted 포함)하면 표시하지 않고 다음 실행 때 다시 보낸다.
        val batch = pendingUploadBatch(state.records, state.uploaded, nowMillis)
        if (batch.isEmpty()) return@withLock
        server.record(recordLocationUsageRequest(consentRepository.installId(), batch))
        consentRepository.markUploaded(batch)
    }

    private companion object {
        const val TAG = "LocationUsageUploader"
    }
}
