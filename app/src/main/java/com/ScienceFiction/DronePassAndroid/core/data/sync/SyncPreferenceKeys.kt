package com.ScienceFiction.DronePassAndroid.core.data.sync

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel

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

private fun String.escapeBaselineJsonKey(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

