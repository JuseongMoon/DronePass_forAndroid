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

    @Test
    fun `스케치 세션은 새로 만든 뒤 삭제한 스케치를 iOS처럼 원격 삭제 대상으로 만들지 않는다`() {
        val created = sketchModel(id = "created", deletedAt = null, updatedAt = 1L)

        val afterCreate = trackSketchUpsertForEditSession(
            plan = SketchEditSyncPlan(existingSketchIdsOnEnter = emptySet()),
            sketch = created,
        )
        val afterDelete = trackSketchDeleteForEditSession(
            plan = afterCreate,
            sketchId = created.id,
        )

        assertTrue(afterDelete.pendingUpserts.isEmpty())
        assertTrue(afterDelete.pendingDeleteIds.isEmpty())
    }

    @Test
    fun `스케치 세션은 기존 스케치 삭제를 iOS처럼 원격 삭제 대상으로 기록한다`() {
        val existing = sketchModel(id = "existing", deletedAt = null, updatedAt = 1L)

        val afterEdit = trackSketchUpsertForEditSession(
            plan = SketchEditSyncPlan(existingSketchIdsOnEnter = setOf(existing.id)),
            sketch = existing.copy(updatedAt = 2L),
        )
        val afterDelete = trackSketchDeleteForEditSession(
            plan = afterEdit,
            sketchId = existing.id,
        )

        assertTrue(afterDelete.pendingUpserts.isEmpty())
        assertEquals(setOf(existing.id), afterDelete.pendingDeleteIds)
    }

    @Test
    fun `스케치 세션은 기존 스케치 삭제 취소를 iOS처럼 업로드 대상으로 되돌린다`() {
        val existing = sketchModel(id = "existing", deletedAt = null, updatedAt = 1L)

        val afterDelete = trackSketchDeleteForEditSession(
            plan = SketchEditSyncPlan(existingSketchIdsOnEnter = setOf(existing.id)),
            sketchId = existing.id,
        )
        val afterRestore = trackSketchUpsertForEditSession(
            plan = afterDelete,
            sketch = existing.copy(updatedAt = 3L),
        )

        assertEquals(existing.id, afterRestore.pendingUpserts.keys.single())
        assertTrue(afterRestore.pendingDeleteIds.isEmpty())
    }

    @Test
    fun `첫 동기화의 서버 누락 로컬 스케치는 업로드 대상으로 유지한다`() {
        val local = sketchModel(id = "local", deletedAt = null, updatedAt = 1L)

        val result = mergeSketchesForFullSync(
            localSketches = listOf(local),
            serverSketches = emptyList(),
            lastSyncTime = null,
        )

        assertEquals(listOf("local"), result.merged.map { it.id })
        assertEquals(listOf("local"), result.toUpload.map { it.id })
    }

    @Test
    fun `이전 동기화 이후 서버에서 사라진 로컬 스케치는 되살리지 않는다`() {
        val local = sketchModel(id = "stale", deletedAt = null, updatedAt = 100L)

        val result = mergeSketchesForFullSync(
            localSketches = listOf(local),
            serverSketches = emptyList(),
            lastSyncTime = 200L,
        )

        assertTrue(result.merged.isEmpty())
        assertTrue(result.toUpload.isEmpty())
    }

    @Test
    fun `마지막 동기화 이후 수정된 로컬 스케치는 서버에 없어도 업로드한다`() {
        val local = sketchModel(id = "newer", deletedAt = null, updatedAt = 300L)

        val result = mergeSketchesForFullSync(
            localSketches = listOf(local),
            serverSketches = emptyList(),
            lastSyncTime = 200L,
        )

        assertEquals(listOf("newer"), result.merged.map { it.id })
        assertEquals(listOf("newer"), result.toUpload.map { it.id })
    }

    @Test
    fun `서버 소프트 삭제가 더 최신이면 로컬 활성 스케치보다 우선한다`() {
        val local = sketchModel(id = "same", deletedAt = null, updatedAt = 100L)
        val serverDeleted = sketchModel(id = "same", deletedAt = 200L, updatedAt = 200L)

        val result = mergeSketchesForFullSync(
            localSketches = listOf(local),
            serverSketches = listOf(serverDeleted),
            lastSyncTime = 150L,
        )

        assertEquals(serverDeleted, result.merged.single())
        assertTrue(result.toUpload.isEmpty())
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
