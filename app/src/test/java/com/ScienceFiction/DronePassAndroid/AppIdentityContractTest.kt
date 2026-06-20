package com.ScienceFiction.DronePassAndroid

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIdentityContractTest {

    @Test
    fun applicationIdKeepsSharedFirebasePackageName() {
        assertEquals("com.ScienceFiction.DronePassAndroid", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun debugBuildDoesNotUseApplicationIdSuffix() {
        assertFalse(BuildConfig.APPLICATION_ID.endsWith(".debug"))
    }

    @Test
    fun googleServicesJsonKeepsAndroidPackageName() {
        assertTrue(googleServicesAndroidPackageNames().contains(BuildConfig.APPLICATION_ID))
    }

    private fun googleServicesAndroidPackageNames(): List<String> {
        val mapType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java,
        )
        val root = Moshi.Builder()
            .build()
            .adapter<Map<String, Any>>(mapType)
            .fromJson(resolveProjectFile("google-services.json", "app/google-services.json").readText())
            ?: return emptyList()
        val clients = root["client"] as? List<*> ?: return emptyList()
        return clients.mapNotNull { rawClient ->
            val client = rawClient as? Map<*, *> ?: return@mapNotNull null
            val clientInfo = client["client_info"] as? Map<*, *>
            val androidClientInfo = clientInfo?.get("android_client_info") as? Map<*, *>
            androidClientInfo?.get("package_name") as? String
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Could not resolve any project file candidate: ${candidates.joinToString()}")
    }
}
