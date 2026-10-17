package com.freelibrary.app.ui.bookpage

import com.freelibrary.app.data.local.entity.ShelfState
import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfControlsTest {
    @Test
    fun `a book on no shelf offers Save and Mark as finished`() {
        assertEquals(ShelfControls(ShelfAction.SAVE, ShelfAction.MARK_FINISHED), shelfControlsFor(null))
    }

    @Test
    fun `a wanted book can be finished or removed`() {
        assertEquals(
            ShelfControls(ShelfAction.MARK_FINISHED, ShelfAction.REMOVE),
            shelfControlsFor(ShelfState.WANT_TO_READ),
        )
    }

    @Test
    fun `a book being read can be finished or removed`() {
        assertEquals(
            ShelfControls(ShelfAction.MARK_FINISHED, ShelfAction.REMOVE),
            shelfControlsFor(ShelfState.READING),
        )
    }

    @Test
    fun `a finished book can only be removed`() {
        assertEquals(ShelfControls(null, ShelfAction.REMOVE), shelfControlsFor(ShelfState.FINISHED))
    }

    @Test
    fun `every shelf state has controls that make sense`() {
        (listOf<ShelfState?>(null) + ShelfState.entries).forEach { state ->
            val controls = shelfControlsFor(state)
            // Removing only makes sense for a book that is on a shelf.
            assertEquals(state != null, controls.secondary == ShelfAction.REMOVE)
        }
    }
}
