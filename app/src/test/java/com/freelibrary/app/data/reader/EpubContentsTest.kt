package com.freelibrary.app.data.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class EpubContentsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val container =
        """
        <?xml version="1.0"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
        </container>
        """.trimIndent()

    private fun packageXml(
        items: String,
        spineAttributes: String = "",
    ) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Book</dc:title></metadata>
          <manifest>
            <item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
            <item id="c2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
            <item id="c3" href="ch3.xhtml" media-type="application/xhtml+xml"/>
            $items
          </manifest>
          <spine $spineAttributes><itemref idref="c1"/><itemref idref="c2"/><itemref idref="c3"/></spine>
        </package>
        """.trimIndent()

    private val navItem = """<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>"""
    private val ncxItem = """<item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>"""

    private val chapterFiles =
        arrayOf(
            "OEBPS/ch1.xhtml" to "<html><body>One</body></html>",
            "OEBPS/ch2.xhtml" to "<html><body>Two</body></html>",
            "OEBPS/ch3.xhtml" to "<html><body>Three</body></html>",
        )

    private fun navDocument(body: String) =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body>${body.replace("\n", " ")}</body></html>
        """.trimIndent()

    private val ncxDocument =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
          <docTitle><text>Book</text></docTitle>
          <navMap>
            <navPoint id="a" playOrder="1"><navLabel><text>Loomings</text></navLabel><content src="ch1.xhtml"/>
              <navPoint id="b" playOrder="2"><navLabel><text>The Carpet-Bag</text></navLabel><content src="ch2.xhtml#top"/></navPoint>
            </navPoint>
            <navPoint id="c" playOrder="3"><navLabel><text>Elsewhere</text></navLabel><content src="other.xhtml"/></navPoint>
          </navMap>
        </ncx>
        """.trimIndent()

    private fun epub(vararg entries: Pair<String, String>): File {
        val file = temporaryFolder.newFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun opened(file: File): EpubBook = (EpubBook.open(file) as EpubOpenResult.Opened).book

    @Test
    fun `the contents is read from the navigation document with its nesting`() {
        val nav =
            navDocument(
                """
                <nav epub:type="toc"><ol>
                  <li><a href="ch1.xhtml">  Part
                    One </a><ol><li><a href="ch2.xhtml#s1"><span>Chapter</span> <em>Two</em></a></li></ol></li>
                  <li><a href="gone.xhtml">Missing</a></li>
                  <li><a href="ch3.xhtml">Three</a></li>
                </ol></nav>
                <nav epub:type="landmarks"><ol><li><a href="ch1.xhtml">Start</a></li></ol></nav>
                """.trimIndent(),
            )
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageXml(navItem),
                "OEBPS/nav.xhtml" to nav,
                *chapterFiles,
            )

        opened(file).use { book ->
            assertEquals(
                listOf(
                    ContentsEntry("Part One", 0, 0),
                    ContentsEntry("Chapter Two", 1, 1),
                    ContentsEntry("Three", 2, 0),
                ),
                book.contents,
            )
        }
    }

    @Test
    fun `an older book gets its contents from the ncx file`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageXml(ncxItem, spineAttributes = """toc="ncx""""),
                "OEBPS/toc.ncx" to ncxDocument,
                *chapterFiles,
            )

        opened(file).use { book ->
            assertEquals(listOf(ContentsEntry("Loomings", 0, 0), ContentsEntry("The Carpet-Bag", 1, 1)), book.contents)
        }
    }

    @Test
    fun `the navigation document is preferred over the ncx file`() {
        val nav = navDocument("""<nav epub:type="toc"><ol><li><a href="ch3.xhtml">From the navigation</a></li></ol></nav>""")
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageXml(navItem + ncxItem, spineAttributes = """toc="ncx""""),
                "OEBPS/nav.xhtml" to nav,
                "OEBPS/toc.ncx" to ncxDocument,
                *chapterFiles,
            )

        opened(file).use { book -> assertEquals(listOf(ContentsEntry("From the navigation", 2, 0)), book.contents) }
    }

    @Test
    fun `a book without a contents file has an empty contents`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageXml(""),
                *chapterFiles,
            )

        opened(file).use { book ->
            assertTrue(book.contents.isEmpty())
            assertEquals(3, book.chapters.size)
        }
    }

    @Test
    fun `a damaged contents file does not stop the book from opening, and the ncx file is tried next`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageXml(navItem + ncxItem, spineAttributes = """toc="ncx""""),
                "OEBPS/nav.xhtml" to """<html><body><nav epub:type="toc"><ol><li><a href="ch1.xhtml">One</li>""",
                "OEBPS/toc.ncx" to ncxDocument,
                *chapterFiles,
            )

        opened(file).use { book ->
            assertEquals(3, book.chapters.size)
            assertEquals(listOf(ContentsEntry("Loomings", 0, 0), ContentsEntry("The Carpet-Bag", 1, 1)), book.contents)
        }
    }

    @Test
    fun `a contents file that only points outside the reading order gives an empty contents`() {
        val nav =
            navDocument("""<nav epub:type="toc"><ol><li><a href="cover.xhtml">Cover</a></li><li><a href="#top">Top</a></li></ol></nav>""")
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageXml(navItem),
                "OEBPS/nav.xhtml" to nav,
                *chapterFiles,
            )

        opened(file).use { book -> assertTrue(book.contents.isEmpty()) }
    }
}
