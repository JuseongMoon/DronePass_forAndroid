package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.domain.model.isValidForFirebasePersistence
import com.ScienceFiction.DronePassAndroid.domain.model.isValidForFirebaseRead
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.squareup.moshi.JsonReader
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Date

class CrossPlatformFirestoreContractTest {

    @Test
    fun `all seven shared iOS shape fixture files parse on Android`() {
        val expectedTypes = mapOf(
            "shape-circle.json" to ShapeType.CIRCLE,
            "shape-rectangle.json" to ShapeType.RECTANGLE,
            "shape-polygon.json" to ShapeType.POLYGON,
            "shape-polyline.json" to ShapeType.POLYLINE,
            "shape-soft-deleted.json" to ShapeType.CIRCLE,
            "shape-android-active.json" to ShapeType.CIRCLE,
            "shape-legacy.json" to ShapeType.CIRCLE,
        )

        assertEquals(SHARED_SHAPE_FIXTURE_FILES.toSet(), expectedTypes.keys)
        expectedTypes.forEach { (fileName, expectedType) ->
            val fixture = loadSharedShapeFixture(fileName)
            val shape = shapeFromFirestoreDocument(
                documentId = fixture.documentId,
                data = fixture.fields,
            )

            requireNotNull(shape) { "Shared fixture did not parse: $fileName" }
            assertEquals(fixture.documentId, shape.id)
            assertEquals(expectedType, shape.shapeType)
        }
    }

    @Test
    fun `shared circle shape fixture preserves canonical fields on Android`() {
        val shape = parsedSharedShapeFixture("shape-circle.json")

        assertEquals("1A2B3C4D-5E6F-4A7B-8C9D-0E1F2A3B4C5D", shape.id)
        assertEquals("한강 드론 비행구역", shape.title)
        assertEquals(ShapeType.CIRCLE, shape.shapeType)
        assertEquals(Coordinate(37.541234, 126.986123), shape.baseCoordinate)
        assertEquals(150.5, shape.radius ?: 0.0, 0.0)
        assertEquals(120.5, shape.height ?: 0.0, 0.0)
        assertEquals("#007AFF", shape.color)
        assertEquals(1_782_900_000_000L, shape.flightStartDate)
        assertEquals(1_785_456_000_000L, shape.flightEndDate)
        assertEquals(1_782_907_200_123L, shape.updatedAt)
        assertEquals("DRONE-001", shape.droneId)
    }

    @Test
    fun `shared legacy iOS fixture uses case insensitive type and date fallbacks`() {
        val fixture = loadSharedShapeFixture("shape-legacy.json")

        assertFalse(fixture.fields.containsKey("flightStartDate"))
        assertFalse(fixture.fields.containsKey("flightEndDate"))
        assertFalse(fixture.fields.containsKey("createdAt"))
        assertFalse(fixture.fields.containsKey("updatedAt"))
        assertFalse(fixture.fields.containsKey("memo"))
        assertFalse(fixture.fields.containsKey("address"))

        val shape = parsedSharedShapeFixture("shape-legacy.json")
        assertEquals(ShapeType.CIRCLE, shape.shapeType)
        assertEquals(1_750_000_000_000L, shape.flightStartDate)
        assertEquals(1_755_000_000_000L, shape.flightEndDate)
        assertEquals(shape.flightStartDate, shape.createdAt)
        assertEquals(shape.createdAt, shape.updatedAt)
        assertNull(shape.memo)
        assertNull(shape.address)
    }

    @Test
    fun `future unknown shapeType fixture is skipped instead of coerced to circle`() {
        val shape = shapeFromFirestoreDocument(
            documentId = SHAPE_ID,
            data = iosCircleShapeDocument(shapeType = "ellipse"),
        )

        assertNull(shape)
    }

    @Test
    fun `shared non circle shape fixtures preserve canonical geometry on Android`() {
        val rectangleShape = parsedSharedShapeFixture("shape-rectangle.json")
        assertEquals(ShapeType.RECTANGLE, rectangleShape.shapeType)
        assertEquals(Coordinate(37.615321, 126.715654), rectangleShape.baseCoordinate)
        assertEquals(Coordinate(37.610111, 126.722222), rectangleShape.secondCoordinate)
        assertEquals("#FF3B30", rectangleShape.color)

        val polygonShape = parsedSharedShapeFixture("shape-polygon.json")
        assertEquals(ShapeType.POLYGON, polygonShape.shapeType)
        assertEquals(
            listOf(
                Coordinate(37.511111, 127.011111),
                Coordinate(37.512222, 127.013333),
                Coordinate(37.509999, 127.014444),
            ),
            polygonShape.polygonCoordinates,
        )
        assertEquals("#34C759", polygonShape.color)

        val polylineShape = parsedSharedShapeFixture("shape-polyline.json")
        assertEquals(ShapeType.POLYLINE, polylineShape.shapeType)
        assertEquals(
            listOf(
                Coordinate(36.351234, 127.384567),
                Coordinate(36.353456, 127.386789),
                Coordinate(36.355678, 127.389012),
            ),
            polylineShape.polylineCoordinates,
        )
        assertEquals("#FF9500", polylineShape.color)
    }

    @Test
    fun `one hundred iOS shape fixtures parse without losing ids on Android`() {
        val ids = (0 until 100).map { index -> fixtureUuid(200 + index) }

        val parsedShapes = ids.mapIndexed { index, id ->
            shapeFromFirestoreDocument(
                documentId = id,
                data = iosCircleShapeDocument(
                    id = id,
                    title = "iOS Circle $index",
                ),
            )
        }.map { shape -> requireNotNull(shape) }

        assertEquals(100, parsedShapes.size)
        assertEquals(ids.toSet(), parsedShapes.map { it.id }.toSet())
        assertEquals("iOS Circle 42", parsedShapes.single { it.id == ids[42] }.title)
        assertTrue(parsedShapes.all { it.shapeType == ShapeType.CIRCLE })
        assertTrue(parsedShapes.all { it.baseCoordinate == Coordinate(37.5665, 126.978) })
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
    fun `shared Firestore fixtures with mismatched document ids are skipped on Android`() {
        val mismatchedId = "00000000-0000-0000-0000-000000000999"

        assertNull(
            shapeFromFirestoreDocument(
                documentId = mismatchedId,
                data = iosCircleShapeDocument(),
            ),
        )
        assertNull(
            sketchFromFirestoreDocument(
                documentId = mismatchedId,
                data = iosSketchDocument(),
            ),
        )
        assertNull(
            droneFromFirestoreDocument(
                documentId = mismatchedId,
                data = iosDroneDocument(),
            ),
        )
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
    fun `Android circle shape write requires radius so iOS can render the overlay`() {
        val shapeWithoutRadius = ShapeModel(
            id = SHAPE_ID,
            title = "Android Circle Without Radius",
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.5665, 126.978),
            radius = null,
            color = "#007AFF",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_060_000L,
            flightStartDate = 1_700_000_000_000L,
        )

        assertTrue(shapeWithoutRadius.isValidForFirebaseRead())
        assertFalse(shapeWithoutRadius.isValidForFirebasePersistence())
    }

    @Test
    fun `Android soft delete writes iOS readable tombstone timestamps`() {
        val tombstoneMillis = 1_700_000_999_000L
        val payloads = listOf(
            shapeSoftDeleteFirestoreUpdateData(deletedAtMillis = tombstoneMillis),
            sketchSoftDeleteFirestoreUpdateData(deletedAtMillis = tombstoneMillis),
            droneSoftDeleteFirestoreUpdateData(deletedAtMillis = tombstoneMillis),
        )

        payloads.forEach { data ->
            val deletedAt = data["deletedAt"] as Timestamp
            val updatedAt = data["updatedAt"] as Timestamp
            assertEquals(tombstoneMillis, deletedAt.toDate().time)
            assertEquals(tombstoneMillis, updatedAt.toDate().time)
            assertFalse(data.containsKey("deleted"))
        }
    }

    @Test
    fun `shared iOS tombstone fixture parses as deleted on Android`() {
        val deletedAtMillis = 1_782_950_400_000L
        val shape = parsedSharedShapeFixture("shape-soft-deleted.json")
        val sketch = sketchFromFirestoreDocument(
            documentId = SKETCH_ID,
            data = iosSketchDocument() + mapOf(
                "deletedAt" to timestamp(deletedAtMillis),
                "updatedAt" to timestamp(deletedAtMillis),
            ),
        )
        val drone = droneFromFirestoreDocument(
            documentId = DRONE_ID,
            data = iosDroneDocument() + mapOf(
                "deletedAt" to timestamp(deletedAtMillis),
                "updatedAt" to timestamp(deletedAtMillis),
            ),
        )

        assertEquals(deletedAtMillis, shape.deletedAt)
        assertEquals(deletedAtMillis, shape.updatedAt)
        assertTrue(shape.isDeleted)
        assertEquals(emptyList<ShapeModel>(), listOf(shape).filter { it.deletedAt == null })

        requireNotNull(sketch)
        assertEquals(deletedAtMillis, sketch.deletedAt)
        assertEquals(deletedAtMillis, sketch.updatedAt)
        assertTrue(sketch.isDeleted)
        assertEquals(emptyList<SketchModel>(), listOf(sketch).filter { it.deletedAt == null })

        requireNotNull(drone)
        assertEquals(deletedAtMillis, drone.deletedAt)
        assertEquals(deletedAtMillis, drone.updatedAt)
        assertTrue(drone.isDeleted)
        assertEquals(emptyList<DroneModel>(), listOf(drone).filter { it.deletedAt == null })
    }

    @Test
    fun `shared Android active fixture accepts integer radius and height without deletedAt`() {
        val fixture = loadSharedShapeFixture("shape-android-active.json")

        assertFalse(fixture.fields.containsKey("deletedAt"))
        assertTrue(fixture.fields["radius"] is Long)
        assertTrue(fixture.fields["height"] is Long)

        val shape = parsedSharedShapeFixture("shape-android-active.json")
        assertEquals(200.0, shape.radius ?: 0.0, 0.0)
        assertEquals(100.0, shape.height ?: 0.0, 0.0)
        assertNull(shape.deletedAt)
        assertFalse(shape.isDeleted)
    }

    @Test
    fun `shared Firestore GeoPoint coordinates are not treated as valid Android coordinates`() {
        val geoPoint = GeoPoint(37.5665, 126.978)
        val shapeWithGeoPointBase = shapeFromFirestoreDocument(
            documentId = SHAPE_ID,
            data = iosCircleShapeDocument() + ("baseCoordinate" to geoPoint),
        )
        val rectangleWithGeoPointSecond = shapeFromFirestoreDocument(
            documentId = RECTANGLE_SHAPE_ID,
            data = iosRectangleShapeDocument() + ("secondCoordinate" to geoPoint),
        )
        val polygonWithGeoPointCoordinates = shapeFromFirestoreDocument(
            documentId = POLYGON_SHAPE_ID,
            data = iosPolygonShapeDocument() + (
                "polygonCoordinates" to listOf(
                    GeoPoint(37.5665, 126.978),
                    GeoPoint(37.567, 126.979),
                    GeoPoint(37.566, 126.98),
                )
            ),
        )
        val sketchWithGeoPoint = sketchFromFirestoreDocument(
            documentId = SKETCH_ID,
            data = iosSketchDocument() + ("points" to listOf(geoPoint)),
        )

        assertNull(shapeWithGeoPointBase)
        assertNull(rectangleWithGeoPointSecond)
        assertNull(polygonWithGeoPointCoordinates)
        requireNotNull(sketchWithGeoPoint)
        assertEquals(emptyList<Coordinate>(), sketchWithGeoPoint.points)
    }

    @Test
    fun `shared Firestore short coordinate keys are not treated as canonical Android coordinates`() {
        val shortKeyCoordinate = mapOf("lat" to 37.5665, "lng" to 126.978)
        val shapeWithShortKeyBase = shapeFromFirestoreDocument(
            documentId = SHAPE_ID,
            data = iosCircleShapeDocument() + ("baseCoordinate" to shortKeyCoordinate),
        )
        val rectangleWithShortKeySecond = shapeFromFirestoreDocument(
            documentId = RECTANGLE_SHAPE_ID,
            data = iosRectangleShapeDocument() + ("secondCoordinate" to shortKeyCoordinate),
        )
        val polylineWithShortKeyCoordinates = shapeFromFirestoreDocument(
            documentId = POLYLINE_SHAPE_ID,
            data = iosPolylineShapeDocument() + (
                "polylineCoordinates" to listOf(
                    mapOf("lat" to 37.565, "lng" to 126.977),
                    mapOf("lat" to 37.566, "lng" to 126.978),
                )
            ),
        )
        val sketchWithShortKeyPoint = sketchFromFirestoreDocument(
            documentId = SKETCH_ID,
            data = iosSketchDocument() + ("points" to listOf(shortKeyCoordinate)),
        )

        assertNull(shapeWithShortKeyBase)
        assertNull(rectangleWithShortKeySecond)
        assertNull(polylineWithShortKeyCoordinates)
        requireNotNull(sketchWithShortKeyPoint)
        assertEquals(emptyList<Coordinate>(), sketchWithShortKeyPoint.points)
    }

    @Test
    fun `Android non circle shape writes canonical geometry for iOS`() {
        val rectangleData = shapeToFirestoreDocumentData(
            androidShape(
                id = RECTANGLE_SHAPE_ID,
                title = "Android Rectangle",
                shapeType = ShapeType.RECTANGLE,
                secondCoordinate = Coordinate(37.1234567, 127.1234567),
            ),
        )
        val polygonData = shapeToFirestoreDocumentData(
            androidShape(
                id = POLYGON_SHAPE_ID,
                title = "Android Polygon",
                shapeType = ShapeType.POLYGON,
                polygonCoordinates = listOf(
                    Coordinate(37.1234567, 127.1234567),
                    Coordinate(37.2234567, 127.2234567),
                    Coordinate(37.3234567, 127.3234567),
                ),
            ),
        )
        val polylineData = shapeToFirestoreDocumentData(
            androidShape(
                id = POLYLINE_SHAPE_ID,
                title = "Android Polyline",
                shapeType = ShapeType.POLYLINE,
                polylineCoordinates = listOf(
                    Coordinate(36.1234567, 126.1234567),
                    Coordinate(36.2234567, 126.2234567),
                ),
            ),
        )

        val secondCoordinate = rectangleData["secondCoordinate"] as Map<*, *>
        assertEquals("rectangle", rectangleData["shapeType"])
        assertEquals(37.123457, secondCoordinate["latitude"])
        assertEquals(127.123457, secondCoordinate["longitude"])
        assertTrue(secondCoordinate["latitude"] is Double)
        assertTrue(secondCoordinate["longitude"] is Double)
        assertFalse(rectangleData.containsKey("radius"))
        assertFalse(rectangleData.containsKey("polygonCoordinates"))
        assertFalse(rectangleData.containsKey("polylineCoordinates"))

        val polygonCoordinates = polygonData["polygonCoordinates"] as List<*>
        val firstPolygonCoordinate = polygonCoordinates.first() as Map<*, *>
        assertEquals("polygon", polygonData["shapeType"])
        assertEquals(37.123457, firstPolygonCoordinate["latitude"])
        assertEquals(127.123457, firstPolygonCoordinate["longitude"])
        assertTrue(firstPolygonCoordinate["latitude"] is Double)
        assertTrue(firstPolygonCoordinate["longitude"] is Double)
        assertFalse(polygonData.containsKey("radius"))
        assertFalse(polygonData.containsKey("secondCoordinate"))
        assertFalse(polygonData.containsKey("polylineCoordinates"))

        val polylineCoordinates = polylineData["polylineCoordinates"] as List<*>
        val firstPolylineCoordinate = polylineCoordinates.first() as Map<*, *>
        assertEquals("polyline", polylineData["shapeType"])
        assertEquals(36.123457, firstPolylineCoordinate["latitude"])
        assertEquals(126.123457, firstPolylineCoordinate["longitude"])
        assertTrue(firstPolylineCoordinate["latitude"] is Double)
        assertTrue(firstPolylineCoordinate["longitude"] is Double)
        assertFalse(polylineData.containsKey("radius"))
        assertFalse(polylineData.containsKey("secondCoordinate"))
        assertFalse(polylineData.containsKey("polygonCoordinates"))
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

    private fun parsedSharedShapeFixture(fileName: String): ShapeModel {
        val fixture = loadSharedShapeFixture(fileName)
        return requireNotNull(
            shapeFromFirestoreDocument(
                documentId = fixture.documentId,
                data = fixture.fields,
            ),
        ) { "Shared fixture did not parse: $fileName" }
    }

    private fun loadSharedShapeFixture(fileName: String): SharedShapeFixture {
        val fixtureDirectory = File(
            System.getProperty(SHARED_FIXTURE_DIRECTORY_PROPERTY)
                ?: DEFAULT_SHARED_FIXTURE_DIRECTORY,
        )
        val fixtureFile = fixtureDirectory.resolve(fileName)
        require(fixtureFile.isFile) {
            "Missing shared iOS fixture: ${fixtureFile.absolutePath}. " +
                "Override with -D$SHARED_FIXTURE_DIRECTORY_PROPERTY=<fixture-directory>."
        }

        val root = JsonReader.of(Buffer().writeUtf8(fixtureFile.readText())).use { reader ->
            val value = readFixtureJsonValue(reader)
            require(reader.peek() == JsonReader.Token.END_DOCUMENT) {
                "Unexpected trailing JSON in ${fixtureFile.absolutePath}"
            }
            value
        } as? Map<*, *> ?: error("Fixture root must be an object: ${fixtureFile.absolutePath}")

        val documentId = root["documentId"] as? String
            ?: error("Fixture documentId must be a string: ${fixtureFile.absolutePath}")
        val rawFields = root["fields"] as? Map<*, *>
            ?: error("Fixture fields must be an object: ${fixtureFile.absolutePath}")
        val fields = rawFields.entries.associate { (key, value) ->
            val fieldName = key as? String
                ?: error("Fixture field name must be a string: ${fixtureFile.absolutePath}")
            fieldName to decodeFirestoreFixtureValue(value)
        }

        return SharedShapeFixture(documentId = documentId, fields = fields)
    }

    private fun readFixtureJsonValue(reader: JsonReader): Any? {
        return when (reader.peek()) {
            JsonReader.Token.BEGIN_OBJECT -> {
                val result = linkedMapOf<String, Any?>()
                reader.beginObject()
                while (reader.hasNext()) {
                    result[reader.nextName()] = readFixtureJsonValue(reader)
                }
                reader.endObject()
                result
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                val result = mutableListOf<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    result += readFixtureJsonValue(reader)
                }
                reader.endArray()
                result
            }
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                val rawNumber = reader.nextString()
                rawNumber.toLongOrNull() ?: rawNumber.toDouble()
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            else -> error("Unexpected JSON token ${reader.peek()} at ${reader.path}")
        }
    }

    private fun decodeFirestoreFixtureValue(value: Any?): Any? {
        return when (value) {
            is Map<*, *> -> {
                if (value.keys == TIMESTAMP_FIXTURE_FIELDS) {
                    val seconds = value["_seconds"] as? Number
                        ?: error("Timestamp _seconds must be an integer")
                    val nanoseconds = value["_nanoseconds"] as? Number
                        ?: error("Timestamp _nanoseconds must be an integer")
                    Timestamp(seconds.toLong(), nanoseconds.toInt())
                } else {
                    value.entries.associate { (key, nestedValue) ->
                        val fieldName = key as? String
                            ?: error("Fixture object key must be a string")
                        fieldName to decodeFirestoreFixtureValue(nestedValue)
                    }
                }
            }
            is List<*> -> value.map(::decodeFirestoreFixtureValue)
            else -> value
        }
    }

    private data class SharedShapeFixture(
        val documentId: String,
        val fields: Map<String, Any?>,
    )

    private fun iosCircleShapeDocument(
        id: String = SHAPE_ID,
        title: String = "iOS Circle",
        shapeType: String = "circle",
    ): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
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

    private fun iosRectangleShapeDocument(): Map<String, Any?> {
        return iosShapeDocument(
            id = RECTANGLE_SHAPE_ID,
            title = "iOS Rectangle",
            shapeType = "rectangle",
            color = "#FF9500",
            flightStartDate = 1_700_001_000_000L,
            extra = mapOf(
                "secondCoordinate" to mapOf(
                    "latitude" to 37.5675,
                    "longitude" to 126.979,
                ),
            ),
        )
    }

    private fun iosPolygonShapeDocument(): Map<String, Any?> {
        return iosShapeDocument(
            id = POLYGON_SHAPE_ID,
            title = "iOS Polygon",
            shapeType = "polygon",
            color = "#AF52DE",
            flightStartDate = 1_700_002_000_000L,
            extra = mapOf(
                "polygonCoordinates" to listOf(
                    mapOf("latitude" to 37.5665, "longitude" to 126.978),
                    mapOf("latitude" to 37.567, "longitude" to 126.979),
                    mapOf("latitude" to 37.566, "longitude" to 126.98),
                ),
            ),
        )
    }

    private fun iosPolylineShapeDocument(): Map<String, Any?> {
        return iosShapeDocument(
            id = POLYLINE_SHAPE_ID,
            title = "iOS Polyline",
            shapeType = "polyline",
            color = "#34C759",
            flightStartDate = 1_700_003_000_000L,
            extra = mapOf(
                "polylineCoordinates" to listOf(
                    mapOf("latitude" to 37.565, "longitude" to 126.977),
                    mapOf("latitude" to 37.566, "longitude" to 126.978),
                ),
            ),
        )
    }

    private fun iosShapeDocument(
        id: String,
        title: String,
        shapeType: String,
        color: String,
        flightStartDate: Long,
        extra: Map<String, Any?>,
    ): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "shapeType" to shapeType,
            "baseCoordinate" to mapOf(
                "latitude" to 37.5665,
                "longitude" to 126.978,
            ),
            "height" to 45.0,
            "memo" to "iOS memo",
            "address" to "Seoul",
            "color" to color,
            "droneId" to DRONE_ID,
            "createdAt" to timestamp(flightStartDate),
            "updatedAt" to timestamp(flightStartDate + 60_000L),
            "flightStartDate" to timestamp(flightStartDate),
        ) + extra
    }

    private fun androidShape(
        id: String,
        title: String,
        shapeType: ShapeType,
        secondCoordinate: Coordinate? = null,
        polygonCoordinates: List<Coordinate>? = null,
        polylineCoordinates: List<Coordinate>? = null,
    ): ShapeModel {
        return ShapeModel(
            id = id,
            title = title,
            shapeType = shapeType,
            baseCoordinate = Coordinate(37.5665123, 126.9780432),
            secondCoordinate = secondCoordinate,
            polygonCoordinates = polygonCoordinates,
            polylineCoordinates = polylineCoordinates,
            height = 45.0,
            memo = "memo",
            address = "Seoul",
            color = "#007aff",
            droneId = DRONE_ID,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_060_000L,
            flightStartDate = 1_700_000_000_000L,
            flightEndDate = 1_700_000_600_000L,
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

    private fun fixtureUuid(index: Int): String {
        return "00000000-0000-0000-0000-${index.toString().padStart(12, '0')}"
    }

    private companion object {
        const val SHARED_FIXTURE_DIRECTORY_PROPERTY = "dronepass.iosFixtureDirectory"
        const val DEFAULT_SHARED_FIXTURE_DIRECTORY =
            "/Users/david/Development/Swift/myProjects/DronePass/team/fixtures"
        const val SHAPE_ID = "00000000-0000-0000-0000-000000000101"
        const val SKETCH_ID = "00000000-0000-0000-0000-000000000102"
        const val DRONE_ID = "00000000-0000-0000-0000-000000000103"
        const val RECTANGLE_SHAPE_ID = "00000000-0000-0000-0000-000000000104"
        const val POLYGON_SHAPE_ID = "00000000-0000-0000-0000-000000000105"
        const val POLYLINE_SHAPE_ID = "00000000-0000-0000-0000-000000000106"
        val TIMESTAMP_FIXTURE_FIELDS = setOf("_seconds", "_nanoseconds")
        val SHARED_SHAPE_FIXTURE_FILES = listOf(
            "shape-circle.json",
            "shape-rectangle.json",
            "shape-polygon.json",
            "shape-polyline.json",
            "shape-soft-deleted.json",
            "shape-android-active.json",
            "shape-legacy.json",
        )
    }
}
