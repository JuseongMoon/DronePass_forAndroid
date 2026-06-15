package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

internal fun firestoreOptionalDouble(value: Any?): Double? {
    return (value as? Number)?.toDouble()
}
