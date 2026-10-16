package com.freelibrary.app.data.repository

import com.freelibrary.app.domain.model.BookFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookFilterHolderTest {
    private val holder = BookFilterHolder()

    @Test
    fun `it starts without a filter`() {
        assertFalse(holder.filter.value.isActive)
    }

    @Test
    fun `an update becomes the current filter`() {
        val chosen = BookFilter(language = "fr", formats = setOf("epub"))

        holder.update(chosen)

        assertEquals(chosen, holder.filter.value)
        assertTrue(holder.filter.value.isActive)
    }

    @Test
    fun `reset goes back to no filter`() {
        holder.update(BookFilter(genres = setOf("Category: Fiction")))

        holder.reset()

        assertEquals(BookFilter(), holder.filter.value)
    }
}
