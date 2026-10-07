package com.freelibrary.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeShelvesTest {
    @Test
    fun `there are ten shelf sliders`() {
        assertEquals(10, HomeShelves.all.size)
    }

    @Test
    fun `shelf ids and bookshelf names are unique`() {
        assertEquals(HomeShelves.all.size, HomeShelves.all.map { it.id }.toSet().size)
        assertEquals(HomeShelves.all.size, HomeShelves.all.map { it.bookshelfName }.toSet().size)
    }

    @Test
    fun `every shelf has a heading and a bookshelf name`() {
        HomeShelves.all.forEach { shelf ->
            assertTrue("${shelf.id} has no heading", shelf.titleRes != 0)
            assertTrue("${shelf.id} has no bookshelf name", shelf.bookshelfName.isNotBlank())
        }
    }

    @Test
    fun `a slider shows ten books`() {
        assertEquals(10, CatalogDefaults.BOOKS_PER_SLIDER)
    }
}
