package com.freelibrary.app.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupCopyLimitTest {
    private fun copy(
        size: Int,
        limit: Long,
    ): Pair<Boolean, ByteArray> {
        val out = ByteArrayOutputStream()
        val complete = copyAtMost(ByteArrayInputStream(ByteArray(size) { 7 }), out, limit)
        return complete to out.toByteArray()
    }

    @Test
    fun `a file within the limit is copied whole`() {
        val (complete, bytes) = copy(size = 20_000, limit = 20_000)

        assertTrue(complete)
        assertArrayEquals(ByteArray(20_000) { 7 }, bytes)
    }

    @Test
    fun `an empty file is copied as empty`() {
        val (complete, bytes) = copy(size = 0, limit = 10)

        assertTrue(complete)
        assertEquals(0, bytes.size)
    }

    @Test
    fun `a file over the limit is refused and never written past it`() {
        val (complete, bytes) = copy(size = 20_001, limit = 20_000)

        assertFalse(complete)
        assertTrue(bytes.size <= 20_000)
    }
}
