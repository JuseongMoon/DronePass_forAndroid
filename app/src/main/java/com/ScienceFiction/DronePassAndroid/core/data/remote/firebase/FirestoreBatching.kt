package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

internal const val FIRESTORE_WRITE_BATCH_LIMIT = 500

internal fun <T> firestoreWriteChunks(items: List<T>): List<List<T>> {
    return items.chunked(FIRESTORE_WRITE_BATCH_LIMIT)
}
