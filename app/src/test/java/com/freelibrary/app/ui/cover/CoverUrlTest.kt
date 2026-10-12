package com.freelibrary.app.ui.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverUrlTest {
    @Test
    fun `a medium cover address becomes the small one`() {
        assertEquals(
            "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.small.jpg",
            smallCoverUrl("https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"),
        )
    }

    @Test
    fun `a missing address gives nothing`() {
        assertNull(smallCoverUrl(null))
    }

    @Test
    fun `an address of another shape gives nothing instead of a guess`() {
        assertNull(smallCoverUrl(""))
        assertNull(smallCoverUrl("https://example.org/cover.jpg"))
        assertNull(smallCoverUrl("https://www.gutenberg.org/cache/epub/1/pg1.cover.small.jpg"))
    }

    @Test
    fun `an address that is not secure gives nothing`() {
        assertNull(smallCoverUrl("http://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"))
    }
}
