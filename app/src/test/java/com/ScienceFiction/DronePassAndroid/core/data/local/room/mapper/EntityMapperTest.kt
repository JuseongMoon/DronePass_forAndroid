package com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper

import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMapperTest {

    @Test
    fun `ShapeEntity 는 iOS raw value shapeType 을 읽는다`() {
        val shape = shapeEntity(shapeType = "circle").toDomain()

        assertEquals(ShapeType.CIRCLE, shape.shapeType)
    }

    @Test
    fun `ShapeEntity 는 Android 레거시 enum name shapeType 을 읽는다`() {
        val shape = shapeEntity(shapeType = "CIRCLE").toDomain()

        assertEquals(ShapeType.CIRCLE, shape.shapeType)
    }

    @Test
    fun `ShapeEntity 는 손상된 shapeType 도 Flow 를 깨지 않고 circle 로 복구한다`() {
        val shape = shapeEntity(shapeType = "unknown-shape-type").toDomain()

        assertEquals(ShapeType.CIRCLE, shape.shapeType)
    }

    @Test
    fun `ShapeModel 저장 시 shapeType 은 iOS raw value 로 저장한다`() {
        val entity = ShapeModel(shapeType = ShapeType.CIRCLE).toEntity()

        assertEquals("circle", entity.shapeType)
    }

    @Test
    fun `iOS geometry 필드는 Room 엔티티에 저장된다`() {
        val polygon = listOf(
            Coordinate(37.0, 127.0),
            Coordinate(37.1, 127.1),
            Coordinate(37.2, 127.2),
        )
        val polyline = listOf(
            Coordinate(36.0, 126.0),
            Coordinate(36.1, 126.1),
        )
        val shape = ShapeModel(
            secondCoordinate = Coordinate(37.2, 127.2),
            polygonCoordinates = polygon,
            polylineCoordinates = polyline,
        )

        val entity = shape.toEntity()

        assertEquals(37.2, entity.secondLatitude)
        assertEquals(127.2, entity.secondLongitude)
        assertEquals(Coordinate.listToJson(polygon), entity.polygonCoordinates)
        assertEquals(Coordinate.listToJson(polyline), entity.polylineCoordinates)
    }

    @Test
    fun `ShapeModel Room 왕복은 iOS ShapeModel 단일값 선택 필드를 보존한다`() {
        val shape = ShapeModel(
            id = "shape-all-fields",
            title = "전체 필드 도형",
            shapeType = ShapeType.RECTANGLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            address = "서울특별시",
            radius = 100.0,
            secondCoordinate = Coordinate(37.1, 127.1),
            height = 120.0,
            memo = "memo",
            color = "#FF3B30",
            droneId = "drone-1",
            createdAt = 1_000L,
            deletedAt = 2_000L,
            flightStartDate = 3_000L,
            flightEndDate = 4_000L,
            updatedAt = 5_000L,
        )

        val restored = shape.toEntity().toDomain()

        assertEquals(shape, restored)
    }

    @Test
    fun `iOS secondCoordinate 필드는 Room 엔티티에서 복원된다`() {
        val restored = shapeEntity(shapeType = "circle")
            .copy(secondLatitude = 37.2, secondLongitude = 127.2)
            .toDomain()

        assertEquals(Coordinate(37.2, 127.2), restored.secondCoordinate)
    }

    private fun shapeEntity(shapeType: String): ShapeEntity {
        val now = 1_700_000_000_000L
        return ShapeEntity(
            id = "shape-1",
            title = "Shape",
            shapeType = shapeType,
            baseLatitude = 37.0,
            baseLongitude = 127.0,
            address = null,
            radius = 100.0,
            secondLatitude = null,
            secondLongitude = null,
            polygonCoordinates = null,
            polylineCoordinates = null,
            height = null,
            memo = null,
            color = "#007AFF",
            droneId = null,
            createdAt = now,
            deletedAt = null,
            flightStartDate = now,
            flightEndDate = null,
            updatedAt = now,
        )
    }
}
