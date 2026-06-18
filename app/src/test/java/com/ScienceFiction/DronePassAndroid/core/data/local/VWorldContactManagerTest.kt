package com.ScienceFiction.DronePassAndroid.core.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VWorldContactManagerTest {

    @Test
    fun `contact lookup uses exact match before partial match like iOS`() {
        val contacts = linkedMapOf(
            "설악산국립공원사무소" to PublicContactInfo(
                organizationName = "설악산국립공원사무소",
                phoneNumber = "033-801-0900",
            ),
            "설악산" to PublicContactInfo(
                organizationName = "설악산",
                phoneNumber = "033-000-0000",
            ),
        )

        assertEquals("033-000-0000", findVWorldContact(contacts, "설악산")?.phoneNumber)
    }

    @Test
    fun `contact lookup falls back to bidirectional partial match like iOS`() {
        val contacts = linkedMapOf(
            "설악산국립공원사무소" to PublicContactInfo(
                organizationName = "설악산국립공원사무소",
                phoneNumber = "033-801-0900",
            ),
        )

        assertEquals(
            "033-801-0900",
            findVWorldContact(contacts, "설악산국립공원사무소 일부구역")?.phoneNumber,
        )
        assertEquals(
            "033-801-0900",
            findVWorldContact(contacts, "설악산")?.phoneNumber,
        )
    }

    @Test
    fun `contact lookup ignores missing or blank names`() {
        val contacts = mapOf(
            "설악산국립공원사무소" to PublicContactInfo(
                organizationName = "설악산국립공원사무소",
                phoneNumber = "033-801-0900",
            ),
        )

        assertNull(findVWorldContact(contacts, null))
        assertNull(findVWorldContact(contacts, ""))
        assertNull(findVWorldContact(contacts, "   "))
    }
}
