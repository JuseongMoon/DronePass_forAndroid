package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestoreException

private const val MissingFirestoreDocumentCodeName = "NOT_FOUND"

internal fun isMissingFirestoreDocumentCode(codeName: String?): Boolean {
    return codeName == MissingFirestoreDocumentCodeName
}

internal fun isMissingFirestoreDocument(error: Exception): Boolean {
    return error is FirebaseFirestoreException &&
        isMissingFirestoreDocumentCode(error.code.name)
}
