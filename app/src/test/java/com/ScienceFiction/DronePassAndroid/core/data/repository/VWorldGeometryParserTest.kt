package com.ScienceFiction.DronePassAndroid.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class VWorldGeometryParserTest {

    @Test
    fun `Polygon parser preserves interior rings for iOS overlay holes`() {
        val outerRing = listOf(
            listOf(126.0, 37.0),
            listOf(127.0, 37.0),
            listOf(127.0, 38.0),
            listOf(126.0, 37.0),
        )
        val holeRing = listOf(
            listOf(126.2, 37.2),
            listOf(126.4, 37.2),
            listOf(126.4, 37.4),
            listOf(126.2, 37.2),
        )

        val parsed = requireNotNull(parseFlightZoneGeometry("Polygon", listOf(outerRing, holeRing)))

        assertEquals(
            listOf(
                37.0 to 126.0,
                37.0 to 127.0,
                38.0 to 127.0,
                37.0 to 126.0,
            ),
            parsed.polygons.single()
        )
        assertEquals(2, parsed.polygonRings.single().size)
        assertEquals(
            listOf(
                37.2 to 126.2,
                37.2 to 126.4,
                37.4 to 126.4,
                37.2 to 126.2,
            ),
            parsed.polygonRings.single()[1]
        )
    }

    @Test
    fun `MultiPolygon parser preserves rings separately per polygon`() {
        val firstOuterRing = listOf(
            listOf(126.0, 37.0),
            listOf(127.0, 37.0),
            listOf(127.0, 38.0),
            listOf(126.0, 37.0),
        )
        val secondOuterRing = listOf(
            listOf(128.0, 35.0),
            listOf(129.0, 35.0),
            listOf(129.0, 36.0),
            listOf(128.0, 35.0),
        )
        val secondHoleRing = listOf(
            listOf(128.2, 35.2),
            listOf(128.4, 35.2),
            listOf(128.4, 35.4),
            listOf(128.2, 35.2),
        )

        val parsed = requireNotNull(
            parseFlightZoneGeometry(
                "MultiPolygon",
                listOf(
                    listOf(firstOuterRing),
                    listOf(secondOuterRing, secondHoleRing),
                )
            )
        )

        assertEquals(2, parsed.polygons.size)
        assertEquals(listOf(37.0 to 126.0, 37.0 to 127.0, 38.0 to 127.0, 37.0 to 126.0), parsed.polygons[0])
        assertEquals(listOf(35.0 to 128.0, 35.0 to 129.0, 36.0 to 129.0, 35.0 to 128.0), parsed.polygons[1])
        assertEquals(1, parsed.polygonRings[0].size)
        assertEquals(2, parsed.polygonRings[1].size)
        assertEquals(listOf(35.2 to 128.2, 35.2 to 128.4, 35.4 to 128.4, 35.2 to 128.2), parsed.polygonRings[1][1])
    }

    @Test
    fun `Polygon parser drops rings with fewer than three points like iOS overlays`() {
        val invalidRing = listOf(
            listOf(126.0, 37.0),
            listOf(127.0, 37.0),
        )

        assertEquals(null, parseFlightZoneGeometry("Polygon", listOf(invalidRing)))
    }

    @Test
    fun `MultiPolygon parser keeps valid polygons after dropping invalid rings`() {
        val invalidRing = listOf(
            listOf(126.0, 37.0),
            listOf(127.0, 37.0),
        )
        val validRing = listOf(
            listOf(128.0, 35.0),
            listOf(129.0, 35.0),
            listOf(129.0, 36.0),
        )

        val parsed = requireNotNull(
            parseFlightZoneGeometry(
                "MultiPolygon",
                listOf(
                    listOf(invalidRing),
                    listOf(validRing),
                ),
            ),
        )

        assertEquals(1, parsed.polygons.size)
        assertEquals(listOf(35.0 to 128.0, 35.0 to 129.0, 36.0 to 129.0), parsed.polygons.single())
    }
}
