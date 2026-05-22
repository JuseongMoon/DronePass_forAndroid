package com.ScienceFiction.DronePassAndroid.core.data.local

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ScienceFiction.DronePassAndroid.core.data.remote.VWorldContactsApi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 공공기관 연락처. iOS `PublicContactInfo` 매핑.
 */
@JsonClass(generateAdapter = true)
data class PublicContactInfo(
    val organizationName: String,
    val phoneNumber: String,
)

/**
 * VWorld 공공기관 연락처 매니저. iOS `VWorldContactManager` 매핑.
 *
 * - S3 의 `vworld-contacts.txt` 를 다운로드해 기관명별 전화번호로 파싱한다.
 * - DataStore 에 JSON 직렬화로 5일 캐싱한다. (오프라인 시 만료 캐시 사용)
 * - 한 줄 = 기관명, 다음 줄 = 전화번호. `#` 접두 라인은 주석.
 * - 전화번호 형식 검증: 숫자/하이픈/괄호만 허용.
 * - lookup 은 정확 일치 우선, 실패 시 양방향 contains 부분 일치.
 *
 * `ensureLoaded` 를 안전하게 멱등 호출 가능 (Mutex 보호). 호출자(MapViewModel 등)는
 * 진입 시 한 번 호출하면 충분하며, 캐시가 유효하면 즉시 반환한다.
 */
@Singleton
class VWorldContactManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val api: VWorldContactsApi,
    moshi: Moshi,
) {
    companion object {
        private const val TAG = "VWorldContactManager"
        private val KEY_CACHE = stringPreferencesKey("vworld_contacts_cache_json")
        private val KEY_LAST_FETCH_MS = longPreferencesKey("vworld_contacts_last_fetch_ms")
        private val CACHE_VALID_MS = TimeUnit.DAYS.toMillis(5)
        private val PHONE_PATTERN = Regex("^[0-9\\-()]+$")
    }

    private val listAdapter = moshi.adapter<List<PublicContactInfo>>(
        Types.newParameterizedType(List::class.java, PublicContactInfo::class.java)
    )

    private val _contacts = MutableStateFlow<Map<String, PublicContactInfo>>(emptyMap())
    val contacts: StateFlow<Map<String, PublicContactInfo>> = _contacts.asStateFlow()

    private val loadMutex = Mutex()

    /**
     * 캐시가 유효하면 캐시를, 아니면 S3 에서 다운로드한다.
     * 다운로드 실패 시 만료된 캐시라도 사용한다 (iOS 동작과 동일).
     */
    suspend fun ensureLoaded() = loadMutex.withLock {
        if (_contacts.value.isNotEmpty() && isCacheValid()) return@withLock

        val cached = loadFromCache()
        if (cached != null && isCacheValid()) {
            _contacts.value = cached
            return@withLock
        }

        val downloaded = runCatching {
            val body = api.fetchContacts()
            val text = body.string()
            parseContacts(text)
        }.getOrNull()

        when {
            downloaded != null -> {
                _contacts.value = downloaded
                saveToCache(downloaded)
            }
            cached != null -> {
                // 다운로드 실패 + 만료 캐시 존재 → 오프라인 폴백 (iOS 와 동일)
                _contacts.value = cached
                Log.w(TAG, "S3 다운로드 실패, 만료 캐시 사용 (${cached.size}개)")
            }
            else -> Log.e(TAG, "연락처 다운로드 실패 + 캐시 없음")
        }
    }

    /**
     * 구역명 또는 기관명으로 연락처 lookup. 정확 일치 → 부분 일치 순.
     */
    fun findContact(name: String?): PublicContactInfo? {
        if (name.isNullOrBlank()) return null
        val map = _contacts.value
        map[name]?.let { return it }
        // 양방향 contains (iOS 와 동일)
        return map.entries.firstOrNull { (org, _) ->
            name.contains(org) || org.contains(name)
        }?.value
    }

    private suspend fun isCacheValid(): Boolean {
        val last = dataStore.data.first()[KEY_LAST_FETCH_MS] ?: return false
        return System.currentTimeMillis() - last < CACHE_VALID_MS
    }

    private suspend fun loadFromCache(): Map<String, PublicContactInfo>? {
        val json = dataStore.data.first()[KEY_CACHE] ?: return null
        return runCatching {
            listAdapter.fromJson(json)
                ?.associateBy { it.organizationName }
        }.getOrNull()
    }

    private suspend fun saveToCache(contacts: Map<String, PublicContactInfo>) {
        val json = listAdapter.toJson(contacts.values.toList())
        dataStore.edit { prefs ->
            prefs[KEY_CACHE] = json
            prefs[KEY_LAST_FETCH_MS] = System.currentTimeMillis()
        }
    }

    private fun parseContacts(content: String): Map<String, PublicContactInfo> {
        val lines = content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()

        val result = LinkedHashMap<String, PublicContactInfo>(lines.size / 2 + 1)
        var i = 0
        while (i < lines.size) {
            val org = lines[i]
            val phone = lines.getOrNull(i + 1)
            if (phone != null && PHONE_PATTERN.matches(phone)) {
                result[org] = PublicContactInfo(org, phone)
                i += 2
            } else {
                i += 1
            }
        }
        return result
    }
}
