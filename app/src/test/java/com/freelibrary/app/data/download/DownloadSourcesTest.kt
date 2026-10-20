package com.freelibrary.app.data.download

import com.freelibrary.app.domain.model.BookFileFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadSourcesTest {
    @Test
    fun `a text file is tried at the catalog address, then at each mirror in order`() {
        val url = "https://www.gutenberg.org/ebooks/2701.txt.utf-8"

        assertEquals(
            listOf(
                url,
                "https://gutenberg.pglaf.org/2/7/0/2701/2701-0.txt",
                "https://gutenberg.pglaf.org/2/7/0/2701/2701.txt",
                "https://aleph.gutenberg.org/2/7/0/2701/2701-0.txt",
                "https://aleph.gutenberg.org/2/7/0/2701/2701.txt",
                "https://www.mirrorservice.org/sites/ftp.ibiblio.org/pub/docs/books/gutenberg/2/7/0/2701/2701-0.txt",
                "https://www.mirrorservice.org/sites/ftp.ibiblio.org/pub/docs/books/gutenberg/2/7/0/2701/2701.txt",
                "https://mirrors.xmission.com/gutenberg/2/7/0/2701/2701-0.txt",
                "https://mirrors.xmission.com/gutenberg/2/7/0/2701/2701.txt",
            ),
            DownloadSources.candidates(url, BookFileFormat.TEXT),
        )
    }

    @Test
    fun `an epub is tried at the catalog address, then at the mirror that carries epubs`() {
        val url = "https://www.gutenberg.org/ebooks/2701.epub3.images"

        assertEquals(
            listOf(url, "https://gutenberg.pglaf.org/cache/epub/2701/pg2701.epub"),
            DownloadSources.candidates(url, BookFileFormat.EPUB),
        )
    }

    @Test
    fun `the book number is found in every kind of catalog address`() {
        val files = DownloadSources.candidates("https://www.gutenberg.org/files/1342/1342-0.txt", BookFileFormat.TEXT)
        val cache = DownloadSources.candidates("https://www.gutenberg.org/cache/epub/76/pg76.epub", BookFileFormat.EPUB)
        val plain = DownloadSources.candidates("http://gutenberg.org/ebooks/12345.epub.noimages", BookFileFormat.EPUB)

        assertEquals("https://gutenberg.pglaf.org/1/3/4/1342/1342-0.txt", files[1])
        assertEquals("https://gutenberg.pglaf.org/cache/epub/76/pg76.epub", cache[1])
        assertEquals("https://gutenberg.pglaf.org/cache/epub/12345/pg12345.epub", plain[1])
    }

    @Test
    fun `a number with two digits has a one digit folder`() {
        val sources = DownloadSources.candidates("https://www.gutenberg.org/ebooks/10.txt.utf-8", BookFileFormat.TEXT)

        assertEquals("https://gutenberg.pglaf.org/1/10/10-0.txt", sources[1])
    }

    @Test
    fun `a pdf, a small number or another site has only its own address`() {
        val pdf = "https://www.gutenberg.org/ebooks/2701.pdf"
        val small = "https://www.gutenberg.org/ebooks/5.txt.utf-8"
        val other = "https://example.org/books/2701.txt"

        assertEquals(listOf(pdf), DownloadSources.candidates(pdf, BookFileFormat.PDF))
        assertEquals(listOf(small), DownloadSources.candidates(small, BookFileFormat.TEXT))
        assertEquals(listOf(other), DownloadSources.candidates(other, BookFileFormat.TEXT))
        assertEquals(listOf("not an address"), DownloadSources.candidates("not an address", BookFileFormat.TEXT))
    }
}
