package com.freelibrary.app.ui.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverUrlTest {
    @Test
    fun `a medium cover address is loaded as it is`() {
        val url = "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"

        assertEquals(url, mediumCoverUrl(url))
    }

    @Test
    fun `a missing address gives nothing`() {
        assertNull(mediumCoverUrl(null))
    }

    @Test
    fun `an address of another shape gives nothing instead of a guess`() {
        assertNull(mediumCoverUrl(""))
        assertNull(mediumCoverUrl("https://example.org/cover.jpg"))
        assertNull(mediumCoverUrl("https://www.gutenberg.org/cache/epub/1/pg1.cover.small.jpg"))
    }

    @Test
    fun `an address that is not secure gives nothing`() {
        assertNull(mediumCoverUrl("http://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"))
    }
}
