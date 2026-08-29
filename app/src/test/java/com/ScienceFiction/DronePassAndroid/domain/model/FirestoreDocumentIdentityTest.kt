package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.Locale
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreDocumentIdentityTest {

    @Test
    fun `new shape ids are canonical uppercase UUIDs`() {
        repeat(100) {
            val id = newCanonicalFirestoreUuid()

            assertEquals(id.uppercase(Locale.ROOT), id)
            assertEquals(UUID.fromString(id).toString().uppercase(Locale.ROOT), id)
        }
    }
}
