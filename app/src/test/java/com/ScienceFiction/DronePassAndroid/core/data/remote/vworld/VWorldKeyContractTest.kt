package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import androidx.datastore.preferences.core.edit
import com.ScienceFiction.DronePassAndroid.core.di.FIREBASE_FUNCTIONS_REGION
import com.ScienceFiction.DronePassAndroid.core.location.testPreferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/** iOS 기준 구현과 같은 공유 fixture(team/fixtures/vworld-key-cases.json)로 원격 키 계약 v1 을 확인한다. */
class VWorldKeyContractTest {

    @get:Rule val folder = TemporaryFolder()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "vworld-key-cases.json")
        val missing = "Shared V-World key fixture is missing: $file"
        if (System.getProperty("dronepass.requireSharedFixtures") == "true") {
            check(file.isFile) { missing }
        } else {
            assumeTrue(missing, file.isFile)
        }
        return requireNotNull(moshi.adapter(Map::class.java).fromJson(file.readText()))
    }

    private fun Map<*, *>.required(key: String): Any = requireNotNull(this[key]) { "Missing $key" }
    private fun Map<*, *>.string(key: String): String = required(key) as? String ?: error("Invalid $key")
    private fun Map<*, *>.list(key: String): List<Map<*, *>> =
        (required(key) as? List<*> ?: error("Invalid $key")).map { it as Map<*, *> }
    private fun Map<*, *>.map(key: String): Map<*, *> = required(key) as? Map<*, *> ?: error("Invalid $key")

    @Test
    fun `fixture has the agreed case counts and source`() {
        val root = fixture()
        assertEquals(1.0, (root.required("schemaVersion") as Number).toDouble(), 0.0)
        assertEquals(8, root.list("keyFormatCases").size)
        assertEquals(15, root.list("remoteConfigCases").size)
        assertEquals(10, root.list("keyErrorCases").size)
        assertEquals(14, root.list("scenarios").size)

        val source = root.map("source")
        assertEquals("callable", source.string("kind"))
        assertEquals(VWorldKeyCallable, source.string("function"))
        assertEquals(FIREBASE_FUNCTIONS_REGION, source.string("region"))
        assertEquals(
            VWorldKeyProvider.MinRefreshIntervalMs,
            ((root.map("rules").required("minRefetchIntervalSeconds") as Number).toDouble() * 1000).toLong(),
        )
    }

    @Test
    fun `key format cases match`() {
        fixture().list("keyFormatCases").forEach { case ->
            assertEquals(case.string("name"), case.required("valid") as Boolean, isWellFormedVWorldKey(case.string("key")))
        }
    }

    @Test
    fun `remote config cases match`() {
        fixture().list("remoteConfigCases").forEach { case ->
            val expected = when (val expect = case.string("expect")) {
                "key" -> VWorldKeyConfigResult.Key(case.string("key"))
                "clearStoredKey" -> VWorldKeyConfigResult.MissingKey
                "ignore" -> VWorldKeyConfigResult.Ignored
                else -> error("Unknown expect $expect")
            }
            assertEquals(case.string("name"), expected, parseVWorldKeyConfig(case.string("body"), moshi))
        }
    }

    @Test
    fun `key error cases match`() {
        fixture().list("keyErrorCases").forEach { case ->
            assertEquals(case.string("name"), case.required("isKeyError") as Boolean, isVWorldKeyErrorBody(case.string("body")))
        }
    }

    @Test
    fun `wfs bodies classify as their names say`() {
        val bodies = fixture().map("wfsBodies")
        assertEquals("ok", outcome(bodies["ok"] as String))
        assertEquals("keyError", outcome(bodies["expired"] as String))
        assertEquals("keyError", outcome(bodies["invalid"] as String))
        assertEquals("otherError", outcome(bodies["maintenance"] as String))
    }

    @Test
    fun `scenarios match`() = runBlocking {
        val root = fixture()
        val bodies = root.map("wfsBodies")
        root.list("scenarios").forEach { scenario ->
            val name = scenario.string("name")
            val store = testPreferencesDataStore(folder.root)
            (scenario["storedBefore"] as? String)?.let { stored ->
                store.edit { it[VWorldKeyProvider.RemoteKeyPreference] = stored }
            }
            val remote = ArrayDeque(scenario.list("remote"))
            var fetches = 0
            var nowMs = 0L
            val provider = VWorldKeyProvider(
                dataStore = store,
                configSource = {
                    fetches += 1
                    val next = remote.removeFirstOrNull() ?: error("$name: unexpected remote fetch #$fetches")
                    if (next["notReceived"] == true) throw IOException("not received")
                    parseVWorldKeyConfig(next.string("received"), moshi)
                },
                builtInKey = scenario.string("embedded"),
                nowMs = { nowMs },
            )
            val wfs = scenario.map("wfs")

            scenario.list("steps").forEachIndexed { index, step ->
                nowMs = ((step.required("at") as Number).toDouble() * 1000).toLong()
                when (val action = step.string("action")) {
                    "startup" -> provider.refresh()
                    "request" -> {
                        val sent = mutableListOf<String>()
                        val body = requestWithVWorldKeyRetry(provider, ::isVWorldKeyErrorBody) { key ->
                            sent += key
                            val bodyName = wfs[key] as? String ?: error("$name: no WFS body for a key")
                            bodies[bodyName] as String
                        }
                        assertEquals("$name step $index keys", step.required("expectKeys"), sent)
                        assertEquals("$name step $index result", step.string("expectResult"), outcome(body))
                    }
                    else -> error("$name: unknown action $action")
                }
            }

            assertEquals("$name stored", scenario["expectStoredAfter"], store.data.first()[VWorldKeyProvider.RemoteKeyPreference])
            assertEquals("$name fetches", (scenario.required("expectRemoteFetches") as Number).toInt(), fetches)
        }
    }

    private fun outcome(body: String): String = when {
        isVWorldFeatureCollectionBody(body) -> "ok"
        isVWorldKeyErrorBody(body) -> "keyError"
        else -> "otherError"
    }
}
