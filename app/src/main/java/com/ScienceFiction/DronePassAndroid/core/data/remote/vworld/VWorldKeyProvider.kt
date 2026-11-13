package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** 요청마다 쓸 V-World 키를 고르고, 키 오류가 나면 다른 키를 내준다. */
interface VWorldKeySource {
    /** 내장 키(BuildConfig.VWORLD_API_KEY). */
    val builtInKey: String

    /** 지금 쓸 키. 형식이 맞고 이번 실행에서 거절되지 않은 원격 키, 없으면 내장 키. */
    suspend fun currentKey(): String

    /** [key] 로 키 오류가 났다. 원격 키면 이번 실행 동안 쓰지 않는다. */
    fun onKeyRejected(key: String)

    /** 키 오류 뒤 설정을 다시 받는다. 최소 간격 안이면 받지 않는다. */
    suspend fun refreshAfterKeyError()
}

/**
 * 원격 키 계약 v1 의 Android 구현.
 *
 * - 앱 시작 때 [refreshInBackground] 로 한 번 받는다. 지도 로딩은 기다리지 않는다.
 * - 키 오류 때 다시 받는다. 받는 간격은 [MinRefreshIntervalMs] 이상이다.
 * - 받은 키는 DataStore 에 남겨 다음 실행에도 쓴다. apiKey 가 없거나 틀린 파일을 받으면 지우고,
 *   받지 못했거나 다른 schemaVersion 이면 유지한다.
 * - 키 값은 로그에 남기지 않는다.
 */
class VWorldKeyProvider internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val configSource: VWorldKeyConfigSource,
    override val builtInKey: String,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val log: (String) -> Unit = {},
) : VWorldKeySource {

    private val loadMutex = Mutex()
    private val refreshMutex = Mutex()
    @Volatile private var loaded = false
    @Volatile private var remoteKey: String? = null
    private var lastRefreshStartedAtMs: Long? = null
    private val rejectedRemoteKeys: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun currentKey(): String {
        ensureLoaded()
        val remote = remoteKey
        return if (isWellFormedVWorldKey(remote) && remote !in rejectedRemoteKeys) remote!! else builtInKey
    }

    override fun onKeyRejected(key: String) {
        if (key != builtInKey) {
            rejectedRemoteKeys += key
            log("원격 키가 거절돼 이번 실행 동안 쓰지 않습니다")
        }
    }

    override suspend fun refreshAfterKeyError() {
        refresh()
    }

    fun refreshInBackground() {
        scope.launch { refresh() }
    }

    /**
     * 설정을 받아 저장한다. 같은 때 들어온 호출은 앞 호출이 끝나기를 기다린 뒤 간격 제한으로 돌아간다.
     * 그래서 앱 시작 조회가 진행 중일 때 키 오류가 나면 그 결과를 이어 쓴다.
     */
    internal suspend fun refresh() {
        refreshMutex.withLock {
            val now = nowMs()
            val last = lastRefreshStartedAtMs
            if (last != null && now - last < MinRefreshIntervalMs) return
            lastRefreshStartedAtMs = now
            ensureLoaded()

            val result = try {
                configSource.fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 오프라인, App Check 실패, 서버 오류는 모두 '받지 못함'이다. 저장된 키를 유지한다.
                log("원격 키 설정을 받지 못했습니다: ${e.javaClass.simpleName}")
                return
            }

            when (result) {
                is VWorldKeyConfigResult.Key -> {
                    if (result.apiKey != remoteKey) {
                        dataStore.edit { it[RemoteKeyPreference] = result.apiKey }
                        remoteKey = result.apiKey
                        log("원격 키를 새로 저장했습니다")
                    }
                }
                VWorldKeyConfigResult.MissingKey -> {
                    if (remoteKey != null) {
                        dataStore.edit { it.remove(RemoteKeyPreference) }
                        remoteKey = null
                        log("설정에 키가 없어 저장된 원격 키를 지웠습니다")
                    }
                }
                VWorldKeyConfigResult.Ignored -> log("알 수 없는 설정 형식이라 무시합니다")
            }
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            val stored = runCatching { dataStore.data.first()[RemoteKeyPreference] }.getOrNull()
            // 다른 요청이 이미 새 키를 받았으면 덮지 않는다.
            if (remoteKey == null) remoteKey = stored?.takeIf(::isWellFormedVWorldKey)
            loaded = true
        }
    }

    companion object {
        const val MinRefreshIntervalMs = 60_000L
        internal val RemoteKeyPreference = stringPreferencesKey("vworld_remote_api_key")
    }
}

/**
 * [request] 를 현재 키로 보내고, 키 오류면 설정을 다시 받아 다른 키로 재시도한다.
 * 한 요청에서 키마다 한 번만 시도한다. 원격 키가 거절되면 내장 키로 넘어간다.
 * 모든 후보가 키 오류면 마지막 응답을 그대로 돌려준다.
 */
internal suspend fun <T> requestWithVWorldKeyRetry(
    keys: VWorldKeySource,
    isKeyError: (T) -> Boolean,
    request: suspend (key: String) -> T,
): T {
    val tried = HashSet<String>()
    var key = keys.currentKey()
    while (true) {
        tried += key
        val response = request(key)
        if (!isKeyError(response)) return response
        keys.onKeyRejected(key)
        keys.refreshAfterKeyError()
        key = keys.currentKey().takeIf { it !in tried }
            ?: keys.builtInKey.takeIf { it !in tried }
            ?: return response
    }
}
