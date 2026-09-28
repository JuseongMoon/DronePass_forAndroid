package com.ScienceFiction.DronePassAndroid.subscription

import com.squareup.moshi.Moshi
import java.time.Instant

data class RemoteSubscriptionConfig(
    val limits: QuotaLimits?,
    val legacyCutoff: LegacyCutoff?,
    val minSupportedVersion: String?,
) {
    companion object {
        const val URL = "https://sciencefiction.co.kr/dronepass/config/limits.json"
        private val adapter = Moshi.Builder().build().adapter(Map::class.java)
        private fun validInt(value: Any?): Int? {
            val number = value as? Number ?: return null
            val double = number.toDouble()
            return if (double.isFinite() && double >= Int.MIN_VALUE && double <= Int.MAX_VALUE && double == number.toInt().toDouble()) number.toInt() else null
        }

        fun parse(json: String, platform: String = "android"): RemoteSubscriptionConfig? {
            val root = try { adapter.fromJson(json) as? Map<*, *> } catch (_: Exception) { null } ?: return null
            if (validInt(root["schemaVersion"]) != 1) return null
            val free = root["free"] as? Map<*, *>
            val shapes = validInt(free?.get("shapes"))
            val sketches = validInt(free?.get("sketches"))
            val drones = validInt(free?.get("drones"))
            val limits = if (shapes != null && sketches != null && drones != null && shapes > 0 && sketches > 0 && drones > 0) {
                QuotaLimits(shapes, sketches, drones)
            } else null
            val legacy = root["legacy"] as? Map<*, *>
            val build = validInt(legacy?.get("iosOriginalBuildBefore"))
            val date = try { (legacy?.get("accountCreatedBefore") as? String)?.let(Instant::parse) } catch (_: Exception) { null }
            val cutoff = if (build != null && build > 0 && date != null) LegacyCutoff(build, date) else null
            val minimum = (root["minSupportedVersion"] as? Map<*, *>)?.get(platform) as? String
            return RemoteSubscriptionConfig(limits, cutoff, minimum?.takeIf { it.isNotEmpty() })
        }
    }
}
