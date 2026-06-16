package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CrossPlatformFirestoreContractTest {

    @Test
    fun `iOS circle shape fixture parses on Android`() {
        val shape = shapeFromFirestoreDocument(
            documentId = SHAPE_ID,
            data = iosCircleShapeDocument(),
        )

        requireNotNull(shape)
        assertEquals(SHAPE_ID, shape.id)
        assertEquals("iOS Circle", shape.title)
        assertEquals(ShapeType.CIRCLE, shape.shapeType)
        assertEquals(Coordinate(37.5665, 126.978), shape.baseCoordinate)
        assertEquals(120.0, shape.radius ?: 0.0, 0.0)
        assertEquals("#007AFF", shape.color)
        assertEquals(1_700_000_000_000L, shape.flightStartDate)
        assertEquals(1_700_000_600_000L, shape.flightEndDate)
        assertEquals(DRONE_ID, shape.droneId)
    }

    @Test
    fun `legacy uppercase shapeType fixture still parses on Android`() {
        val shape = shapeFromFirestoreDocument(
            documentId = SHAPE_ID,
            data = iosCircleShapeDocument(shapeType = "CIRCLE"),
        )

        requireNotNull(shape)
        assertEquals(ShapeType.CIRCLE, shape.shapeType)
    }

    @Test
    fun `iOS sketch fixture parses on Android`() {
        val sketch = sketchFromFirestoreDocument(
            documentId = SKETCH_ID,
            data = iosSketchDocument(),
        )

        requireNotNull(sketch)
        assertEquals(SKETCH_ID, sketch.id)
        assertEquals(
            listOf(
                Coordinate(37.5665, 126.978),
                Coordinate(37.567, 126.979),
            ),
            sketch.points,
        )
        assertEquals("#FF0000", sketch.color)
        assertEquals(4.0, sketch.strokeWidth, 0.0)
        assertEquals(0.75, sketch.opacity, 0.0)
        assertEquals(1_700_000_000_000L, sketch.createdAt)
        assertEquals(1_700_000_060_000L, sketch.updatedAt)
    }

    @Test
    fun `iOS drone fixture parses on Android`() {
        val drone = droneFromFirestoreDocument(
            documentId = DRONE_ID,
            data = iosDroneDocument(),
        )

        requireNotNull(drone)
        assertEquals(DRONE_ID, drone.id)
        assertEquals("iOS Drone", drone.name)
        assertEquals("#34C759", drone.color)
        assertEquals("SN-IOS-1", drone.serialNumber)
        assertEquals("249g", drone.takeoffWeight)
        assertEquals("180x180x80mm", drone.size)
        assertEquals("shared account fixture", drone.memo)
        assertEquals(1_700_000_000_000L, drone.createdAt)
        assertEquals(1_700_000_060_000L, drone.updatedAt)
    }

    @Test
    fun `Android shape write remains canonical for iOS`() {
        val data = shapeToFirestoreDocumentData(
            ShapeModel(
                id = SHAPE_ID,
                title = "Android Circle",
                shapeType = ShapeType.CIRCLE,
                baseCoordinate = Coordinate(37.5665123, 126.9780432),
                radius = 120.0,
                height = 45.0,
                memo = "memo",
                address = "Seoul",
                color = "#007aff",
                droneId = DRONE_ID,
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_060_000L,
                flightStartDate = 1_700_000_000_000L,
                flightEndDate = 1_700_000_600_000L,
            ),
        )
        val baseCoordinate = data["baseCoordinate"] as Map<*, *>

        assertEquals("circle", data["shapeType"])
        assertFalse(data["shapeType"] == ShapeType.CIRCLE.name)
        assertEquals("#007AFF", data["color"])
        assertTrue(baseCoordinate["latitude"] is Double)
        assertTrue(baseCoordinate["longitude"] is Double)
        assertEquals(37.566512, baseCoordinate["latitude"])
        assertEquals(126.978043, baseCoordinate["longitude"])
        assertTrue(data["createdAt"] is Timestamp)
        assertTrue(data["updatedAt"] is Timestamp)
        assertTrue(data["flightStartDate"] is Timestamp)
        assertTrue(data["flightEndDate"] is Timestamp)
        assertFalse(data.containsKey("startedAt"))
        assertFalse(data.containsKey("expireDate"))
    }

    @Test
    fun `Android sketch and drone writes remain canonical for iOS`() {
        val sketchData = sketchToFirestoreDocumentData(
            SketchModel(
                id = SKETCH_ID,
                points = listOf(Coordinate(37.5665123, 126.9780432)),
                color = "#ff0000",
                strokeWidth = 4.0,
                opacity = 0.755,
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_060_000L,
            ),
        )
        val sketchPoint = (sketchData["points"] as List<*>).first() as Map<*, *>

        assertTrue(sketchPoint["latitude"] is Double)
        assertTrue(sketchPoint["longitude"] is Double)
        assertEquals(37.566512, sketchPoint["latitude"])
        assertEquals(126.978043, sketchPoint["longitude"])
        assertEquals("#FF0000", sketchData["color"])
        assertEquals(0.76, sketchData["opacity"])
        assertTrue(sketchData["createdAt"] is Timestamp)
        assertTrue(sketchData["updatedAt"] is Timestamp)

        val droneData = droneToFirestoreDocumentData(
            DroneModel(
                id = DRONE_ID,
                name = "Android Drone",
                color = "#34c759",
                serialNumber = "SN-ANDROID-1",
                takeoffWeight = "249g",
                size = "180x180x80mm",
                memo = "memo",
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_060_000L,
            ),
        )

        assertEquals("#34C759", droneData["color"])
        assertTrue(droneData["createdAt"] is Timestamp)
        assertTrue(droneData["updatedAt"] is Timestamp)
        assertFalse(droneData.containsKey("deletedAt"))
    }

    private fun iosCircleShapeDocument(shapeType: String = "circle"): Map<String, Any?> {
        return mapOf(
            "id" to SHAPE_ID,
            "title" to "iOS Circle",
            "shapeType" to shapeType,
            "baseCoordinate" to mapOf(
                "latitude" to 37.5665,
                "longitude" to 126.978,
            ),
            "radius" to 120.0,
            "height" to 45.0,
            "memo" to "iOS memo",
            "address" to "Seoul",
            "color" to "#007AFF",
            "droneId" to DRONE_ID,
            "createdAt" to timestamp(1_700_000_000_000L),
            "updatedAt" to timestamp(1_700_000_060_000L),
            "flightStartDate" to timestamp(1_700_000_000_000L),
            "flightEndDate" to timestamp(1_700_000_600_000L),
        )
    }

    private fun iosSketchDocument(): Map<String, Any?> {
        return mapOf(
            "id" to SKETCH_ID,
            "points" to listOf(
                mapOf(
                    "latitude" to 37.5665,
                    "longitude" to 126.978,
                ),
                mapOf(
                    "latitude" to 37.567,
                    "longitude" to 126.979,
                ),
            ),
            "color" to "#FF0000",
            "strokeWidth" to 4.0,
            "opacity" to 0.75,
            "createdAt" to timestamp(1_700_000_000_000L),
            "updatedAt" to timestamp(1_700_000_060_000L),
        )
    }

    private fun iosDroneDocument(): Map<String, Any?> {
        return mapOf(
            "id" to DRONE_ID,
            "name" to "iOS Drone",
            "color" to "#34C759",
            "serialNumber" to "SN-IOS-1",
            "takeoffWeight" to "249g",
            "size" to "180x180x80mm",
            "memo" to "shared account fixture",
            "createdAt" to timestamp(1_700_000_000_000L),
            "updatedAt" to timestamp(1_700_000_060_000L),
        )
    }

    private fun timestamp(millis: Long): Timestamp {
        return Timestamp(Date(millis))
    }

    private companion object {
        const val SHAPE_ID = "00000000-0000-0000-0000-000000000101"
        const val SKETCH_ID = "00000000-0000-0000-0000-000000000102"
        const val DRONE_ID = "00000000-0000-0000-0000-000000000103"
    }
}
