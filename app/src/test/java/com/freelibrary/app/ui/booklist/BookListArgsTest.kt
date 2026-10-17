package com.freelibrary.app.ui.booklist

import com.freelibrary.app.domain.model.ViewMoreTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookListArgsTest {
    private fun roundTrip(target: ViewMoreTarget): ViewMoreTarget? {
        val args = target.toArgs()
        return viewMoreTargetFrom(args.kind, args.bookshelfName)
    }

    @Test
    fun `every kind of target survives the trip through arguments`() {
        val targets =
            listOf(
                ViewMoreTarget.ContinueReading,
                ViewMoreTarget.WantToRead,
                ViewMoreTarget.RecentlyAdded,
                ViewMoreTarget.Finished,
                ViewMoreTarget.CatalogShelf("Harvard Classics"),
            )

        targets.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `only a shelf carries a bookshelf name`() {
        assertNull(ViewMoreTarget.WantToRead.toArgs().bookshelfName)
        assertEquals("Poetry", ViewMoreTarget.CatalogShelf("Poetry").toArgs().bookshelfName)
    }

    @Test
    fun `an unknown or missing kind names nothing`() {
        assertNull(viewMoreTargetFrom("nonsense", null))
        assertNull(viewMoreTargetFrom(null, null))
    }

    @Test
    fun `a shelf without a name names nothing`() {
        assertNull(viewMoreTargetFrom(KIND_SHELF, null))
        assertNull(viewMoreTargetFrom(KIND_SHELF, "  "))
    }
}
