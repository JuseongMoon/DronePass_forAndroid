package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.google.firebase.Timestamp

internal fun firestoreOptionalDouble(value: Any?): Double? {
    return (value as? Number)?.toDouble()
}

internal fun hasInvalidFirestoreTimestampField(
    data: Map<String, Any?>,
    field: String,
): Boolean {
    val value = data[field] ?: return false
    return value !is Timestamp
}
