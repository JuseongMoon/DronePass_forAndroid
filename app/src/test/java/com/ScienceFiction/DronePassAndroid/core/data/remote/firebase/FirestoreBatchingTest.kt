package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreBatchingTest {

    @Test
    fun `Firestore batch limit matches platform write cap`() {
        assertEquals(500, FIRESTORE_WRITE_BATCH_LIMIT)
    }

    @Test
    fun `empty write list creates no batches`() {
        assertTrue(firestoreWriteChunks(emptyList<Int>()).isEmpty())
    }

    @Test
    fun `exactly five hundred writes stay in one batch`() {
        val chunks = firestoreWriteChunks((0 until 500).toList())

        assertEquals(listOf(500), chunks.map { it.size })
    }

    @Test
    fun `more than five hundred writes split without exceeding Firestore limit`() {
        val chunks = firestoreWriteChunks((0 until 501).toList())

        assertEquals(listOf(500, 1), chunks.map { it.size })
        assertTrue(chunks.all { it.size <= FIRESTORE_WRITE_BATCH_LIMIT })
    }

    @Test
    fun `multiple overflow batches preserve write order`() {
        val source = (0 until 1001).toList()
        val chunks = firestoreWriteChunks(source)

        assertEquals(listOf(500, 500, 1), chunks.map { it.size })
        assertEquals(source, chunks.flatten())
    }
}
