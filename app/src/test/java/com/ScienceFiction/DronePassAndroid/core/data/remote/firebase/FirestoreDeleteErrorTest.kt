package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreDeleteErrorTest {

    @Test
    fun `missing document Firestore code is treated as already deleted`() {
        assertTrue(isMissingFirestoreDocumentCode("NOT_FOUND"))
        assertFalse(isMissingFirestoreDocumentCode("PERMISSION_DENIED"))
        assertFalse(isMissingFirestoreDocumentCode(null))
        assertFalse(isMissingFirestoreDocument(IllegalStateException("other")))
    }
}
