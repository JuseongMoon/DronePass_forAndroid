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

    @Test
    fun googleServicesJsonKeepsSharedFirebaseProjectAndAndroidAppStream() {
        val root = googleServicesRoot()
        val projectInfo = root["project_info"] as? Map<*, *> ?: error("project_info missing")

        assertEquals("dronepass-91564", projectInfo["project_id"])
        assertEquals("307728758098", projectInfo["project_number"])

        val clientInfo = googleServicesAndroidClientInfo()
        assertEquals(
            "1:307728758098:android:248e79a064ad7aed99768b",
            clientInfo["mobilesdk_app_id"],
        )
    }

    private fun googleServicesAndroidPackageNames(): List<String> {
        val clients = googleServicesClients()
        return clients.mapNotNull { rawClient ->
            val client = rawClient as? Map<*, *> ?: return@mapNotNull null
            val clientInfo = client["client_info"] as? Map<*, *>
            val androidClientInfo = clientInfo?.get("android_client_info") as? Map<*, *>
            androidClientInfo?.get("package_name") as? String
        }
    }

    private fun googleServicesAndroidClientInfo(): Map<*, *> {
        val clients = googleServicesClients()
        return clients.firstNotNullOfOrNull { rawClient ->
            val client = rawClient as? Map<*, *> ?: return@firstNotNullOfOrNull null
            client["client_info"] as? Map<*, *>
        } ?: error("client_info missing")
    }

    private fun googleServicesClients(): List<*> {
        val root = googleServicesRoot()
        return root["client"] as? List<*> ?: return emptyList<Any>()
    }

    private fun googleServicesRoot(): Map<String, Any> {
        val mapType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java,
        )
        return Moshi.Builder()
            .build()
            .adapter<Map<String, Any>>(mapType)
            .fromJson(resolveProjectFile("google-services.json", "app/google-services.json").readText())
            ?: error("Could not parse google-services.json")
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("Could not resolve any project file candidate: ${candidates.joinToString()}")
    }
}
