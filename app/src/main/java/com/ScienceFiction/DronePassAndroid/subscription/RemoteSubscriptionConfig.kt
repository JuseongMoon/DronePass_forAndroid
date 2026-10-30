package com.ScienceFiction.DronePassAndroid.subscription

import com.squareup.moshi.Moshi
import java.time.Instant

data class RemoteSubscriptionConfig(
    val limits: QuotaLimits?,
    val legacyCutoff: LegacyCutoff?,
    val minSupportedVersion: String?,
    /** `features.locationAudit.uploadEnabled`. 값이 없으면 false(확인자료 서버 업로드를 끈다). */
    val locationAuditUploadEnabled: Boolean = false,
    /**
     * `features.proClaim.enabled`. 값이 없으면 false(서버 Pro 미러용 구매 증빙을 보내지 않는다, 계약 §9-2).
     * `minSupportedVersion` 처럼 이번 실행에서 받은 설정에만 적용한다(저장본에는 남기지 않는다).
     */
    val proClaimEnabled: Boolean = false,
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
            // tags 는 3.7 에서 추가한 선택 값이다. 없거나 0 이하면 0(태그 한도만 올리지 않음), 나머지 세 값은 그대로 적용한다.
            val tags = validInt(free?.get("tags"))?.takeIf { it > 0 } ?: 0
            val limits = if (shapes != null && sketches != null && drones != null && shapes > 0 && sketches > 0 && drones > 0) {
                QuotaLimits(shapes, sketches, drones, tags)
            } else null
            val legacy = root["legacy"] as? Map<*, *>
            val build = validInt(legacy?.get("iosOriginalBuildBefore"))
            val date = try { (legacy?.get("accountCreatedBefore") as? String)?.let(Instant::parse) } catch (_: Exception) { null }
            val cutoff = if (build != null && build > 0 && date != null) LegacyCutoff(build, date) else null
            val minimum = (root["minSupportedVersion"] as? Map<*, *>)?.get(platform) as? String
            val locationAudit = (root["features"] as? Map<*, *>)?.get("locationAudit") as? Map<*, *>
            val uploadEnabled = locationAudit?.get("uploadEnabled") as? Boolean ?: false
            val proClaim = (root["features"] as? Map<*, *>)?.get("proClaim") as? Map<*, *>
            val proClaimEnabled = proClaim?.get("enabled") as? Boolean ?: false
            return RemoteSubscriptionConfig(limits, cutoff, minimum?.takeIf { it.isNotEmpty() }, uploadEnabled, proClaimEnabled)
        }
    }
}
