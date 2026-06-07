package com.ScienceFiction.DronePassAndroid.core.data.sync

import androidx.datastore.preferences.core.longPreferencesKey

internal object SyncPreferenceKeys {
    val LAST_SYNC_TIME = longPreferencesKey("lastSyncTime")
    val LAST_LOCAL_MODIFICATION_TIME = longPreferencesKey("lastLocalModificationTime")
    val LAST_SKETCH_SYNC_TIME = longPreferencesKey("lastSketchSyncTime")
    val LAST_LOCAL_SKETCH_MODIFICATION_TIME = longPreferencesKey("lastLocalSketchModificationTime")
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
    shapeCount: Int,
    sketchCount: Int,
    lastLocalModificationTime: Long?,
    lastSyncTime: Long?,
    lastLocalSketchModificationTime: Long?,
    lastSketchSyncTime: Long?,
): AccountSwitchLocalChangeState {
    val hasUnsyncedShapes = hasUnsyncedLocalChanges(
        lastLocalModificationTime = lastLocalModificationTime,
        lastSyncTime = lastSyncTime,
        localItemCount = shapeCount,
    )
    val hasUnsyncedSketches = hasUnsyncedLocalChanges(
        lastLocalModificationTime = lastLocalSketchModificationTime,
        lastSyncTime = lastSketchSyncTime,
        localItemCount = sketchCount,
    )

    return AccountSwitchLocalChangeState(
        hasUnsyncedLocalChanges = hasUnsyncedShapes || hasUnsyncedSketches,
        atRiskCount = shapeCount + sketchCount,
    )
}
