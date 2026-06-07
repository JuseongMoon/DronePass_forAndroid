package com.ScienceFiction.DronePassAndroid.core.data.repository

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.Job

class SketchRepositoryTest {

    @Test
    fun `전체 스케치 삭제는 활성 스케치만 같은 시각으로 소프트 삭제한다`() {
        val now = 10_000L
        val active = sketchModel(id = "active", deletedAt = null, updatedAt = 1L)
        val alreadyDeleted = sketchModel(id = "deleted", deletedAt = 5_000L, updatedAt = 5_000L)

        val deletedSketches = softDeleteActiveSketchModels(
            sketches = listOf(active, alreadyDeleted),
            now = now,
        )

        assertEquals(listOf("active"), deletedSketches.map { it.id })
        assertEquals(now, deletedSketches.single().deletedAt)
        assertEquals(now, deletedSketches.single().updatedAt)
        assertEquals(active.points, deletedSketches.single().points)
        assertEquals(active.color, deletedSketches.single().color)
    }

    @Test
    fun `전체 스케치 삭제 정책은 이미 삭제된 스케치를 변경하지 않는다`() {
        val deleted = sketchModel(id = "deleted", deletedAt = 5_000L, updatedAt = 5_000L)

        val deletedSketches = softDeleteActiveSketchModels(
            sketches = listOf(deleted),
            now = 10_000L,
        )

        assertEquals(emptyList<SketchModel>(), deletedSketches)
        assertEquals(5_000L, deleted.deletedAt)
        assertEquals(5_000L, deleted.updatedAt)
    }

    @Test
    fun `완료된 디바운스 작업은 자신이 최신 대기 작업일 때만 제거 대상이다`() {
        val completedJob = Job()
        val replacementJob = Job()

        assertTrue(isCompletedSketchSyncStillPending(completedJob, completedJob))
        assertFalse(isCompletedSketchSyncStillPending(replacementJob, completedJob))
        assertFalse(isCompletedSketchSyncStillPending(null, completedJob))

        completedJob.cancel()
        replacementJob.cancel()
    }

    private fun sketchModel(
        id: String,
        deletedAt: Long?,
        updatedAt: Long,
    ): SketchModel {
        return SketchModel(
            id = id,
            points = listOf(
                Coordinate(latitude = 37.0, longitude = 127.0),
                Coordinate(latitude = 37.1, longitude = 127.1),
            ),
            color = "#FF0000",
            strokeWidth = 4.0,
            opacity = 1.0,
            createdAt = 1L,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    }
}
