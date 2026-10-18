package com.freelibrary.app.data.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class EpubBookTest {
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
        spine: String,
        title: String = "Moby-Dick",
    ) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>$title</dc:title></metadata>
          <manifest>$items</manifest>
          <spine>$spine</spine>
        </package>
        """.trimIndent()

    private fun page(
        id: String,
        href: String,
    ) = """<item id="$id" href="$href" media-type="application/xhtml+xml"/>"""

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

    private fun standardBook(): File =
        epub(
            "META-INF/container.xml" to container,
            "OEBPS/content.opf" to
                packageXml(
                    items =
                        page("c1", "ch1.xhtml") +
                            page("c2", "text/ch%202.xhtml") +
                            """<item id="css" href="style.css" media-type="text/css"/>""",
                    spine = """<itemref idref="c1"/><itemref idref="c2"/>""",
                ),
            "OEBPS/ch1.xhtml" to "<html><body>One</body></html>",
            "OEBPS/text/ch 2.xhtml" to "<html><body>Two</body></html>",
            "OEBPS/style.css" to "body {}",
        )

    @Test
    fun `a book opens with its title and chapters in reading order`() {
        opened(standardBook()).use { book ->
            assertEquals("Moby-Dick", book.title)
            assertEquals(listOf("OEBPS/ch1.xhtml", "OEBPS/text/ch 2.xhtml"), book.chapters)
        }
    }

    @Test
    fun `the reading order follows the spine, not the manifest`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to
                    packageXml(
                        items = page("a", "a.xhtml") + page("b", "b.xhtml"),
                        spine = """<itemref idref="b"/><itemref idref="a"/>""",
                    ),
                "OEBPS/a.xhtml" to "<html/>",
                "OEBPS/b.xhtml" to "<html/>",
            )

        opened(file).use { assertEquals(listOf("OEBPS/b.xhtml", "OEBPS/a.xhtml"), it.chapters) }
    }

    @Test
    fun `files inside the book can be read and have a media type`() {
        opened(standardBook()).use { book ->
            assertEquals("body {}", book.read("OEBPS/style.css")!!.use { it.readBytes().decodeToString() })
            assertNull(book.read("OEBPS/missing.png"))
            assertEquals("text/css", book.mimeType("OEBPS/style.css"))
            assertEquals("application/xhtml+xml", book.mimeType("OEBPS/ch1.xhtml"))
            assertEquals("image/png", book.mimeType("OEBPS/images/unlisted.png"))
            assertEquals("application/octet-stream", book.mimeType("OEBPS/data.bin"))
        }
    }

    @Test
    fun `material outside the main reading path is left out`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to
                    packageXml(
                        items = page("cover", "cover.xhtml") + page("c1", "ch1.xhtml"),
                        spine = """<itemref idref="cover" linear="no"/><itemref idref="c1"/>""",
                    ),
                "OEBPS/cover.xhtml" to "<html/>",
                "OEBPS/ch1.xhtml" to "<html/>",
            )

        opened(file).use { assertEquals(listOf("OEBPS/ch1.xhtml"), it.chapters) }
    }

    @Test
    fun `a book whose only pages are outside the main path still opens`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to
                    packageXml(items = page("c1", "ch1.xhtml"), spine = """<itemref idref="c1" linear="no"/>"""),
                "OEBPS/ch1.xhtml" to "<html/>",
            )

        opened(file).use { assertEquals(listOf("OEBPS/ch1.xhtml"), it.chapters) }
    }

    @Test
    fun `entries that point at nothing or at something that is not a page are left out`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to
                    packageXml(
                        items =
                            page("c1", "ch1.xhtml") + page("gone", "gone.xhtml") +
                                """<item id="img" href="cover.png" media-type="image/png"/>""" +
                                """<item id="web" href="http://example.org/x.xhtml" media-type="application/xhtml+xml"/>""",
                        spine =
                            """<itemref idref="c1"/><itemref idref="gone"/><itemref idref="img"/>""" +
                                """<itemref idref="web"/><itemref idref="nobody"/>""",
                    ),
                "OEBPS/ch1.xhtml" to "<html/>",
                "OEBPS/cover.png" to "png",
            )

        opened(file).use { assertEquals(listOf("OEBPS/ch1.xhtml"), it.chapters) }
    }

    @Test
    fun `tag prefixes in the package are ignored`() {
        val packageWithPrefixes =
            """
            <?xml version="1.0"?>
            <opf:package xmlns:opf="http://www.idpf.org/2007/opf" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <opf:metadata><dc:title>  Prefixed  </dc:title></opf:metadata>
              <opf:manifest><opf:item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/></opf:manifest>
              <opf:spine><opf:itemref idref="c1"/></opf:spine>
            </opf:package>
            """.trimIndent()
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageWithPrefixes,
                "OEBPS/ch1.xhtml" to "<html/>",
            )

        opened(file).use { book ->
            assertEquals("Prefixed", book.title)
            assertEquals(listOf("OEBPS/ch1.xhtml"), book.chapters)
        }
    }

    @Test
    fun `a book without a title opens with no title`() {
        val noTitle =
            """
            <?xml version="1.0"?>
            <package xmlns="http://www.idpf.org/2007/opf">
              <manifest>${page("c1", "ch1.xhtml")}</manifest>
              <spine><itemref idref="c1"/></spine>
            </package>
            """.trimIndent()
        val file = epub("META-INF/container.xml" to container, "OEBPS/content.opf" to noTitle, "OEBPS/ch1.xhtml" to "<html/>")

        opened(file).use { assertNull(it.title) }
    }

    @Test
    fun `a file that is not a zip is not an EPUB`() {
        val file = temporaryFolder.newFile()
        file.writeText("This is a plain text book, not an EPUB.")

        assertEquals(EpubOpenResult.Failed(EpubProblem.NOT_AN_EPUB), EpubBook.open(file))
    }

    @Test
    fun `a zip without a container is not an EPUB`() {
        val file = epub("readme.txt" to "hello")

        assertEquals(EpubOpenResult.Failed(EpubProblem.NOT_AN_EPUB), EpubBook.open(file))
    }

    @Test
    fun `a container that names a missing package is damaged`() {
        val file = epub("META-INF/container.xml" to container)

        assertEquals(EpubOpenResult.Failed(EpubProblem.DAMAGED), EpubBook.open(file))
    }

    @Test
    fun `a container that names no package is damaged`() {
        val file = epub("META-INF/container.xml" to """<container><rootfiles/></container>""")

        assertEquals(EpubOpenResult.Failed(EpubProblem.DAMAGED), EpubBook.open(file))
    }

    @Test
    fun `a package that is not well formed is damaged`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to """<package><manifest><item id="a"></manifest></package>""",
            )

        assertEquals(EpubOpenResult.Failed(EpubProblem.DAMAGED), EpubBook.open(file))
    }

    @Test
    fun `a book with no chapter is empty`() {
        val file =
            epub(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to packageXml(items = "", spine = ""),
            )

        assertEquals(EpubOpenResult.Failed(EpubProblem.EMPTY), EpubBook.open(file))
    }

    @Test
    fun `a failed open leaves the file free to delete`() {
        val file = epub("readme.txt" to "hello")

        EpubBook.open(file)

        assertTrue(file.delete())
    }

    @Test
    fun `paths are resolved against the file they are written in`() {
        assertEquals("OEBPS/text/ch 2.xhtml", EpubPackageParser.resolve("OEBPS/content.opf", "text/ch%202.xhtml"))
        assertEquals("Other/a.xhtml", EpubPackageParser.resolve("OEBPS/content.opf", "../Other/a.xhtml#section"))
        assertEquals("a.xhtml", EpubPackageParser.resolve("content.opf", "a.xhtml"))
        assertEquals("abs/a.xhtml", EpubPackageParser.resolve("OEBPS/content.opf", "/abs/a.xhtml"))
        assertEquals("x.xhtml", EpubPackageParser.resolve("OEBPS/content.opf", "../../../x.xhtml"))
        assertEquals("OEBPS/a.xhtml", EpubPackageParser.resolve("OEBPS/content.opf", "./a.xhtml?x=1"))
    }

    @Test
    fun `the package path is read from the container`() {
        val path = container.byteInputStream().use { EpubPackageParser.rootFilePath(it) }

        assertNotNull(path)
        assertEquals("OEBPS/content.opf", path)
    }
}
