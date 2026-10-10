package com.ScienceFiction.DronePassAndroid.core.account

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.domain.model.validateFirebaseShapeBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 서버 배치 저장은 하나라도 무효면 전체를 거부한다. 공백 제목 도형 하나가 가져오기·로그아웃 업로드를 계속 실패시켰다.
 */
class AccountUploadPreparationTest {

    private fun circle(title: String, radius: Double = 100.0) = ShapeModel(
        title = title,
        shapeType = ShapeType.CIRCLE,
        baseCoordinate = Coordinate(37.5, 127.0),
        radius = radius,
        updatedAt = 1_000L,
    )

    @Test
    fun `blank titles and names get defaults and invalid items are left out`() {
        val blank = circle("   ")
        val ok = circle("비행 구역")
        val zeroRadius = circle("반경 0", radius = 0.0)
        val drone = DroneModel(name = " ")
        val sketch = SketchModel(points = listOf(Coordinate(37.5, 127.0), Coordinate(37.6, 127.1)))
        val badSketch = SketchModel(points = listOf(Coordinate(37.5, 127.0)), strokeWidth = 0.0)

        val batch = prepareAccountUpload(
            drones = listOf(drone),
            shapes = listOf(blank, ok, zeroRadius, ok),
            sketches = listOf(sketch, badSketch),
            defaultShapeTitle = "새 도형",
            defaultDroneName = "내 드론",
        )

        assertEquals(listOf("새 도형", "비행 구역"), batch.shapes.map { it.title })
        assertEquals(1_000L, batch.shapes.first().updatedAt)
        assertEquals("내 드론", batch.drones.single().name)
        assertEquals(listOf(sketch.id), batch.sketches.map { it.id })
        assertEquals(3, batch.skippedCount)
        assertTrue(validateFirebaseShapeBatch(batch.shapes).isValid)
    }

    @Test
    fun `account upload goes through the preparation`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val upload = File(root, "com/ScienceFiction/DronePassAndroid/core/account/AccountSessionFlows.kt").readText()
            .substringAfter("private suspend fun uploadAndMark(")
        val prepare = upload.indexOf("prepareAccountUpload(")
        assertTrue(prepare >= 0 && prepare < upload.indexOf("droneStore.saveDrones(ticket, batch.drones)"))
        assertTrue(upload.contains("shapeStore.saveShapes(ticket, batch.shapes)"))
        assertTrue(upload.contains("sketchStore.saveSketches(ticket, batch.sketches)"))
    }
}
