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
            blankDroneName = "드론",
        )

        assertEquals(listOf("새 도형", "비행 구역"), batch.shapes.map { it.title })
        assertEquals(1_000L, batch.shapes.first().updatedAt)
        assertEquals("드론", batch.drones.single().name)
        // 기본 드론 이름이면 다른 기기가 "손대지 않은 기본 드론"으로 판정해 병합·정리할 수 있다.
        org.junit.Assert.assertFalse(batch.drones.single().name in com.ScienceFiction.DronePassAndroid.core.data.repository.DefaultDroneNames)
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
        val prepare = upload.indexOf("prepareUploadBatch(drones, shapes, sketches)")
        assertTrue(prepare >= 0 && prepare < upload.indexOf("droneStore.saveDrones(ticket, batch.drones)"))
        // 가져오기는 계정에 없을 태그만 비운 batch.shapes 를 올린다
        assertTrue(upload.contains("clearingTagIds(batch.shapes, accountTagIds"))
        assertTrue(upload.contains("shapeStore.saveShapes(ticket, uploadShapes)"))
        assertTrue(upload.contains("sketchStore.saveSketches(ticket, batch.sketches)"))
    }

    @Test
    fun `blank drone names are not corrected to the default drone names`() {
        val res = listOf("src/main/res", "app/src/main/res")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        listOf("values", "values-en").forEach { dir ->
            val name = Regex("""<string name="drone_upload_blank_name">([^<]+)</string>""")
                .find(File(res, "$dir/strings.xml").readText())!!.groupValues[1]
            org.junit.Assert.assertFalse(dir, name in com.ScienceFiction.DronePassAndroid.core.data.repository.DefaultDroneNames)
        }
    }

    @Test
    fun `logout and import ask before unsavable items are deleted`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        val flows = File(root, "com/ScienceFiction/DronePassAndroid/core/account/AccountSessionFlows.kt").readText()
        val logout = flows.substringAfter("private suspend fun logoutLocked(").substringBefore("private suspend fun flushBeforeLogout(")
        val ask = logout.indexOf("return LogoutResult.NEEDS_UNSAVABLE_CONFIRMATION")
        assertTrue(ask > 0 && ask < logout.indexOf("auth.signOut()") && ask < logout.indexOf("finishLogoutWipe()"))
        assertTrue(logout.contains("unsavable > 0 && !discardUnsavable"))
        assertTrue(flows.contains("unsavableCount = prepareUploadBatch("))
        val dialogs = File(root, "com/ScienceFiction/DronePassAndroid/feature/account/AccountSessionDialogs.kt").readText()
        assertTrue(dialogs.contains("R.plurals.account_import_unsavable_note"))
        val profile = File(root, "com/ScienceFiction/DronePassAndroid/feature/profile/ProfileScreen.kt").readText()
        assertTrue(profile.contains("viewModel.signOut(discardUnsavable = true)"))
    }
}
