package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.location.testPreferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class VWorldKeyProviderTest {

    @get:Rule val folder = TemporaryFolder()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val builtIn = "11111111-1111-1111-1111-111111111111"
    private val remoteA = "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"
    private val remoteB = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

    // ---- 설정 파일 해석 ----

    @Test
    fun `json config with schema 1 and a well formed key yields the key and ignores unknown fields`() {
        val result = parseVWorldKeyConfig(
            """{"schemaVersion":1,"apiKey":"$remoteA","note":"x","extra":{"a":1}}""",
            moshi,
        )
        assertEquals(VWorldKeyConfigResult.Key(remoteA), result)
    }

    @Test
    fun `config without a usable key means the stored key is cleared`() {
        listOf(
            """{"schemaVersion":1}""",
            """{"schemaVersion":1,"apiKey":""}""",
            """{"schemaVersion":1,"apiKey":"not-a-key"}""",
            """{"schemaVersion":1,"apiKey":" $remoteA"}""",
            """{"schemaVersion":1,"apiKey":12}""",
            """{"schemaVersion":1,"apiKey":null}""",
        ).forEach { body ->
            assertEquals(body, VWorldKeyConfigResult.MissingKey, parseVWorldKeyConfig(body, moshi))
        }
    }

    @Test
    fun `other schema versions and non objects are ignored as a whole`() {
        listOf(
            """{"schemaVersion":2,"apiKey":"$remoteA"}""",
            """{"schemaVersion":"1","apiKey":"$remoteA"}""",
            """{"apiKey":"$remoteA"}""",
            """[1,2]""",
            """not json""",
            "",
        ).forEach { body ->
            assertEquals(body, VWorldKeyConfigResult.Ignored, parseVWorldKeyConfig(body, moshi))
        }
    }

    @Test
    fun `callable response maps follow the same contract whatever number type schemaVersion has`() {
        listOf<Number>(1, 1L, 1.0).forEach { version ->
            assertEquals(
                VWorldKeyConfigResult.Key(remoteA),
                interpretVWorldKeyConfig(mapOf("schemaVersion" to version, "apiKey" to remoteA, "x" to true)),
            )
        }
        assertEquals(
            VWorldKeyConfigResult.MissingKey,
            interpretVWorldKeyConfig(mapOf("schemaVersion" to 1, "apiKey" to "nope")),
        )
        assertEquals(
            VWorldKeyConfigResult.Ignored,
            interpretVWorldKeyConfig(mapOf("schemaVersion" to 2, "apiKey" to remoteA)),
        )
        assertEquals(VWorldKeyConfigResult.Ignored, interpretVWorldKeyConfig(null))
    }

    // ---- 키 오류 판정 ----

    @Test
    fun `expired and invalid key service exceptions are key errors`() {
        val expired = """<?xml version="1.0" encoding="UTF-8"?><ServiceExceptionReport><ServiceException code="EXPIRE_KEY">인증키 사용기간이 만료되었습니다.</ServiceException></ServiceExceptionReport>"""
        val invalid = """<ServiceExceptionReport><ServiceException code="INVALID_KEY">x</ServiceException></ServiceExceptionReport>"""
        val wordingOnly = """<ServiceExceptionReport><ServiceException code="UNKNOWN">등록되지 않은 인증키입니다.</ServiceException></ServiceExceptionReport>"""
        assertTrue(isVWorldKeyErrorBody(expired))
        assertTrue(isVWorldKeyErrorBody(invalid))
        assertTrue(isVWorldKeyErrorBody(wordingOnly))
    }

    @Test
    fun `other service exceptions and json are not key errors`() {
        val domain = """<ServiceExceptionReport><ServiceException code="INCORRECT_DOMAIN">도메인 오류</ServiceException></ServiceExceptionReport>"""
        assertFalse(isVWorldKeyErrorBody(domain))
        assertTrue(isVWorldServiceExceptionBody(domain))
        assertFalse(isVWorldKeyErrorBody("""{"type":"FeatureCollection","features":[]}"""))
    }

    // ---- 키 선택·저장 ----

    @Test
    fun `uses built in key until a remote key is stored`() = runBlocking {
        val provider = provider(source = { VWorldKeyConfigResult.Key(remoteA) })
        assertEquals(builtIn, provider.currentKey())
        provider.refresh()
        assertEquals(remoteA, provider.currentKey())
    }

    @Test
    fun `stored remote key is used on the next launch`() = runBlocking {
        val store = testPreferencesDataStore(folder.root)
        provider(store, source = { VWorldKeyConfigResult.Key(remoteA) }).refresh()
        val nextLaunch = provider(store, source = { throw IOException("offline") })
        assertEquals(remoteA, nextLaunch.currentKey())
    }

    @Test
    fun `malformed stored key falls back to built in key`() = runBlocking {
        val store = testPreferencesDataStore(folder.root)
        store.edit { it[VWorldKeyProvider.RemoteKeyPreference] = "broken" }
        assertEquals(builtIn, provider(store, source = { error("unused") }).currentKey())
    }

    @Test
    fun `received file without a key clears the stored key`() = runBlocking {
        val store = testPreferencesDataStore(folder.root)
        store.edit { it[VWorldKeyProvider.RemoteKeyPreference] = remoteA }
        val provider = provider(store, source = { VWorldKeyConfigResult.MissingKey })
        provider.refresh()
        assertEquals(builtIn, provider.currentKey())
        assertNull(store.data.first()[VWorldKeyProvider.RemoteKeyPreference])
    }

    @Test
    fun `failed or ignored fetch keeps the stored key`() = runBlocking {
        listOf<VWorldKeyConfigSource>(
            VWorldKeyConfigSource { throw IOException("offline") },
            VWorldKeyConfigSource { throw IllegalStateException("app check") },
            VWorldKeyConfigSource { VWorldKeyConfigResult.Ignored },
        ).forEach { source ->
            val store = testPreferencesDataStore(folder.root)
            store.edit { it[VWorldKeyProvider.RemoteKeyPreference] = remoteA }
            val provider = provider(store, source = source)
            provider.refresh()
            assertEquals(remoteA, provider.currentKey())
            assertEquals(remoteA, store.data.first()[VWorldKeyProvider.RemoteKeyPreference])
        }
    }

    @Test
    fun `refresh runs at most once per minute`() = runBlocking {
        var now = 0L
        var calls = 0
        val provider = provider(now = { now }, source = {
            calls += 1
            VWorldKeyConfigResult.Key(remoteA)
        })
        provider.refresh()
        now = 59_999L
        provider.refresh()
        assertEquals(1, calls)
        now = 60_000L
        provider.refresh()
        assertEquals(2, calls)
    }

    @Test
    fun `rejected remote key is not used again in this run even if fetched again`() = runBlocking {
        var now = 0L
        val provider = provider(now = { now }, source = { VWorldKeyConfigResult.Key(remoteA) })
        provider.refresh()
        provider.onKeyRejected(remoteA)
        assertEquals(builtIn, provider.currentKey())
        now = 120_000L
        provider.refresh()
        assertEquals(builtIn, provider.currentKey())
    }

    @Test
    fun `rejecting the built in key does not hide it`() = runBlocking {
        val provider = provider(source = { error("unused") })
        provider.onKeyRejected(builtIn)
        assertEquals(builtIn, provider.currentKey())
    }

    // ---- 재시도 ----

    @Test
    fun `expired built in key retries once with the fetched remote key`() = runBlocking {
        val provider = provider(source = { VWorldKeyConfigResult.Key(remoteA) })
        val sent = mutableListOf<String>()
        val result = requestWithVWorldKeyRetry(provider, { it == "key-error" }) { key ->
            sent += key
            if (key == builtIn) "key-error" else "ok"
        }
        assertEquals("ok", result)
        assertEquals(listOf(builtIn, remoteA), sent)
    }

    @Test
    fun `rejected remote key falls back to the built in key once`() = runBlocking {
        val store = testPreferencesDataStore(folder.root)
        store.edit { it[VWorldKeyProvider.RemoteKeyPreference] = remoteA }
        val provider = provider(store, source = { VWorldKeyConfigResult.Key(remoteA) })
        val sent = mutableListOf<String>()
        val result = requestWithVWorldKeyRetry(provider, { it == "key-error" }) { key ->
            sent += key
            if (key == remoteA) "key-error" else "ok"
        }
        assertEquals("ok", result)
        assertEquals(listOf(remoteA, builtIn), sent)
        assertEquals(builtIn, provider.currentKey())
    }

    @Test
    fun `rejected remote key moves to a newer remote key before the built in key`() = runBlocking {
        val store = testPreferencesDataStore(folder.root)
        store.edit { it[VWorldKeyProvider.RemoteKeyPreference] = remoteA }
        val provider = provider(store, source = { VWorldKeyConfigResult.Key(remoteB) })
        val sent = mutableListOf<String>()
        requestWithVWorldKeyRetry(provider, { it == "key-error" }) { key ->
            sent += key
            if (key == remoteB) "ok" else "key-error"
        }
        assertEquals(listOf(remoteA, remoteB), sent)
    }

    @Test
    fun `each key is tried once and the last key error is returned`() = runBlocking {
        val provider = provider(source = { VWorldKeyConfigResult.Key(remoteA) })
        val sent = mutableListOf<String>()
        val result = requestWithVWorldKeyRetry(provider, { it == "key-error" }) { key ->
            sent += key
            "key-error"
        }
        assertEquals("key-error", result)
        assertEquals(listOf(builtIn, remoteA), sent)
    }

    @Test
    fun `no retry when the refresh gives no other key`() = runBlocking {
        val provider = provider(source = { throw IOException("offline") })
        val sent = mutableListOf<String>()
        requestWithVWorldKeyRetry(provider, { it == "key-error" }) { key ->
            sent += key
            "key-error"
        }
        assertEquals(listOf(builtIn), sent)
    }

    @Test
    fun `successful response is not retried`() = runBlocking {
        var calls = 0
        val provider = provider(source = {
            calls += 1
            VWorldKeyConfigResult.Key(remoteA)
        })
        val result = requestWithVWorldKeyRetry(provider, { it == "key-error" }) { "ok" }
        assertEquals("ok", result)
        assertEquals(0, calls)
    }

    private fun provider(
        store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> =
            testPreferencesDataStore(folder.root),
        now: () -> Long = { 0L },
        source: VWorldKeyConfigSource,
    ) = VWorldKeyProvider(
        dataStore = store,
        configSource = source,
        builtInKey = builtIn,
        nowMs = now,
    )
}
