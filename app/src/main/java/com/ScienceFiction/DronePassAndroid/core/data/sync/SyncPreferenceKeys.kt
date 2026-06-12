package com.ScienceFiction.DronePassAndroid.core.data.sync

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import kotlin.math.abs

internal object SyncPreferenceKeys {
    val LAST_SYNC_TIME = longPreferencesKey("lastSyncTime")
    val LAST_LOCAL_MODIFICATION_TIME = longPreferencesKey("lastLocalModificationTime")
    val LAST_LOCAL_DRONE_MODIFICATION_TIME = longPreferencesKey("lastLocalDroneModificationTime")
    val LAST_SKETCH_SYNC_TIME = longPreferencesKey("lastSketchSyncTime")
    val LAST_LOCAL_SKETCH_MODIFICATION_TIME = longPreferencesKey("lastLocalSketchModificationTime")
    val SYNCED_SHAPE_BASELINE = stringPreferencesKey("syncedShapeBaseline")
}

internal val SHAPE_REALTIME_SYNC_SUCCESS_KEYS_TO_CLEAR: List<Preferences.Key<*>> = listOf(
    SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME,
    SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME,
)

internal val SKETCH_REALTIME_SYNC_SUCCESS_KEYS_TO_CLEAR: List<Preferences.Key<*>> = listOf(
    SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME,
)

internal fun MutablePreferences.recordShapeRealtimeSyncSuccess(
    syncTimeMillis: Long,
    syncedShapeBaseline: String? = null,
) {
    this[SyncPreferenceKeys.LAST_SYNC_TIME] = syncTimeMillis
    if (syncedShapeBaseline != null) {
        this[SyncPreferenceKeys.SYNCED_SHAPE_BASELINE] = syncedShapeBaseline
    }
    SHAPE_REALTIME_SYNC_SUCCESS_KEYS_TO_CLEAR.forEach { key ->
        remove(key)
    }
}

internal fun MutablePreferences.recordSketchRealtimeSyncSuccess(syncTimeMillis: Long) {
    this[SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME] = syncTimeMillis
    SKETCH_REALTIME_SYNC_SUCCESS_KEYS_TO_CLEAR.forEach { key ->
        remove(key)
    }
}

internal data class AccountSwitchLocalChangeState(
    val hasUnsyncedLocalChanges: Boolean,
    val atRiskCount: Int,
)

internal fun hasUnsyncedLocalChanges(
    lastLocalModificationTime: Long?,
    lastSyncTime: Long?,
    localItemCount: Int,
): Boolean {
    return localItemCount > 0 &&
        lastLocalModificationTime != null &&
        lastLocalModificationTime > (lastSyncTime ?: Long.MIN_VALUE)
}

internal fun buildAccountSwitchLocalChangeState(
    currentShapeUpdatedAtById: Map<String, Long>,
    syncedShapeBaseline: Map<String, Long>?,
    droneCount: Int = 0,
    lastLocalDroneModificationTime: Long? = null,
    lastSyncTime: Long? = null,
    sketchCount: Int,
    lastLocalSketchModificationTime: Long?,
    lastSketchSyncTime: Long?,
): AccountSwitchLocalChangeState {
    val unsyncedShapeCount = countAccountSwitchShapeBaselineChanges(
        currentShapeUpdatedAtById = currentShapeUpdatedAtById,
        syncedShapeBaseline = syncedShapeBaseline,
    )
    val hasUnsyncedDrones = hasUnsyncedLocalChanges(
        lastLocalModificationTime = lastLocalDroneModificationTime,
        lastSyncTime = lastSyncTime,
        localItemCount = droneCount,
    )
    val droneAtRiskCount = if (hasUnsyncedDrones) droneCount else 0
    val hasUnsyncedSketches = hasUnsyncedLocalChanges(
        lastLocalModificationTime = lastLocalSketchModificationTime,
        lastSyncTime = lastSketchSyncTime,
        localItemCount = sketchCount,
    )
    val sketchAtRiskCount = if (hasUnsyncedSketches) sketchCount else 0

    return AccountSwitchLocalChangeState(
        hasUnsyncedLocalChanges = unsyncedShapeCount > 0 || hasUnsyncedDrones || hasUnsyncedSketches,
        atRiskCount = unsyncedShapeCount + droneAtRiskCount + sketchAtRiskCount,
    )
}

internal fun countAccountSwitchShapeBaselineChanges(
    currentShapeUpdatedAtById: Map<String, Long>,
    syncedShapeBaseline: Map<String, Long>?,
    toleranceMillis: Long = 1_000L,
): Int {
    if (syncedShapeBaseline == null) return currentShapeUpdatedAtById.size

    val baselineIds = syncedShapeBaseline.keys
    val currentIds = currentShapeUpdatedAtById.keys
    val added = currentIds.subtract(baselineIds).size
    val removed = baselineIds.subtract(currentIds).size
    val modified = currentIds.intersect(baselineIds).count { id ->
        val baselineUpdatedAt = syncedShapeBaseline[id]
        val currentUpdatedAt = currentShapeUpdatedAtById[id]
        baselineUpdatedAt != null &&
            currentUpdatedAt != null &&
            abs(currentUpdatedAt - baselineUpdatedAt) > toleranceMillis
    }

    return added + removed + modified
}

internal fun encodeAccountSwitchShapeBaseline(shapeUpdatedAtById: Map<String, Long>): String {
    return shapeUpdatedAtById.entries
        .sortedBy { it.key }
        .joinToString(prefix = "{", postfix = "}") { (id, updatedAt) ->
            "\"${id.escapeBaselineJsonKey()}\":$updatedAt"
        }
}

internal fun buildAccountSwitchShapeBaseline(shapes: List<ShapeModel>): Map<String, Long> {
    return shapes
        .filter { !it.isDeleted }
        .associate { shape -> shape.id to shape.updatedAt }
}

internal fun decodeAccountSwitchShapeBaseline(encoded: String?): Map<String, Long>? {
    if (encoded.isNullOrBlank()) return null

    return runCatching {
        val trimmed = encoded.trim()
        if (trimmed == "{}") return@runCatching emptyMap()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) error("invalid baseline")

        val body = trimmed.drop(1).dropLast(1)
        if (body.isBlank()) return@runCatching emptyMap()

        body.split(",").associate { entry ->
            val separatorIndex = entry.indexOf(':')
            if (separatorIndex <= 0) error("invalid baseline entry")
            val key = entry
                .substring(0, separatorIndex)
                .trim()
                .removeSurrounding("\"")
                .unescapeBaselineJsonKey()
            val value = entry.substring(separatorIndex + 1).trim().toLong()
            key to value
        }
    }.getOrNull()
}

private fun String.escapeBaselineJsonKey(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

private fun String.unescapeBaselineJsonKey(): String =
    replace("\\\"", "\"").replace("\\\\", "\\")
