package com.freelibrary.app.data.reader

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class EpubPageServerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val opened = mutableListOf<EpubBook>()
    private val style = ReaderStyle(textColor = 0x112233, backgroundColor = 0xFFFFFF, linkColor = 0xAA0000, textPercent = 120)

    private val container =
        """
        <?xml version="1.0"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
        </container>
        """.trimIndent()

    private val packageXml =
        """
        <?xml version="1.0"?>
        <package xmlns="http://www.idpf.org/2007/opf">
          <manifest><item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/></manifest>
          <spine><itemref idref="c1"/></spine>
        </package>
        """.trimIndent()

    private val pageWithHead = "<html><head><title>T</title></head><body>One</body></html>"
    private val link = """<link rel="stylesheet" type="text/css" href="${EpubPageServer.STYLE_URL}?v=${style.css().hashCode()}"/>"""

    @After
    fun tearDown() {
        opened.forEach { it.close() }
    }

    private fun server(
        vararg files: Pair<String, ByteArray>,
        maxPageBytes: Int = EpubPageServer.DEFAULT_MAX_PAGE_BYTES,
    ): EpubPageServer {
        val file = temporaryFolder.newFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            val entries =
                listOf(
                    "META-INF/container.xml" to container.toByteArray(),
                    "OEBPS/content.opf" to packageXml.toByteArray(),
                    "OEBPS/ch1.xhtml" to pageWithHead.toByteArray(),
                ) + files
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        val book = (EpubBook.open(file) as EpubOpenResult.Opened).book
        opened.add(book)
        return EpubPageServer(book, style, maxPageBytes)
    }

    private fun ServedFile.text(): String = stream.use { it.readBytes().decodeToString() }

    @Test
    fun `an address points back at the path it was made from`() {
        val server = server()
        val paths = listOf("OEBPS/ch1.xhtml", "OEBPS/text/ch 2.xhtml", "OEBPS/\u00fc file.xhtml", "a%b/c#d.xhtml")

        for (path in paths) {
            assertEquals(path, server.pathOf(server.addressOf(path)))
        }
    }

    @Test
    fun `a link to part of a page points at the page`() {
        val server = server()

        val address = server.addressOf("OEBPS/ch1.xhtml", "section-2")

        assertTrue(address.endsWith("#section-2"))
        assertEquals("OEBPS/ch1.xhtml", server.pathOf(address))
    }

    @Test
    fun `an address that is not the server's own has no path`() {
        val server = server()

        assertNull(server.pathOf("https://example.org/OEBPS/ch1.xhtml"))
        assertNull(server.pathOf("http://${EpubPageServer.HOST}/OEBPS/ch1.xhtml"))
        assertNull(server.pathOf("https://${EpubPageServer.HOST}/"))
        assertNull(server.pathOf("https://${EpubPageServer.HOST}"))
        assertNull(server.pathOf("mailto:someone@example.org"))
        assertNull(server.pathOf("not an address"))
    }

    @Test
    fun `an address can never point outside the book`() {
        val server = server()

        assertEquals("secret.xhtml", server.pathOf("https://${EpubPageServer.HOST}/../../secret.xhtml"))
    }

    @Test
    fun `a chapter is served with the style sheet linked in before its head ends`() {
        val served = server().serve("OEBPS/ch1.xhtml")!!

        assertEquals("application/xhtml+xml", served.mimeType)
        assertEquals("utf-8", served.encoding)
        assertEquals("<html><head><title>T</title>$link</head><body>One</body></html>", served.text())
    }

    @Test
    fun `the end of the head is found whatever its case`() {
        val server = server("OEBPS/ch2.html" to "<HTML><HEAD></HEAD ><BODY>Two</BODY></HTML>".toByteArray())

        val served = server.serve("OEBPS/ch2.html")!!

        assertEquals("text/html", served.mimeType)
        assertEquals("<HTML><HEAD>$link</HEAD ><BODY>Two</BODY></HTML>", served.text())
    }

    @Test
    fun `a page without a head is served as it is`() {
        val server = server("OEBPS/ch3.xhtml" to "<html><body>Three</body></html>".toByteArray())

        assertEquals("<html><body>Three</body></html>", server.serve("OEBPS/ch3.xhtml")!!.text())
    }

    @Test
    fun `text in a page is kept as it was`() {
        val text = "<html><head></head><body>caf\u00e9 \u0633\u0644\u0627\u0645</body></html>"
        val server = server("OEBPS/ch4.xhtml" to text.toByteArray())

        assertEquals(
            "<html><head>$link</head><body>caf\u00e9 \u0633\u0644\u0627\u0645</body></html>",
            server.serve("OEBPS/ch4.xhtml")!!.text(),
        )
    }

    @Test
    fun `other files are served as they are with their media type`() {
        val image = byteArrayOf(1, 2, 3, 4, 5)
        val server = server("OEBPS/images/cover.png" to image, "OEBPS/fonts/a.woff2" to byteArrayOf(9))

        val served = server.serve("OEBPS/images/cover.png")!!

        assertEquals("image/png", served.mimeType)
        assertNull(served.encoding)
        assertArrayEquals(image, served.stream.use { it.readBytes() })
        assertEquals("font/woff2", server.serve("OEBPS/fonts/a.woff2")!!.mimeType)
    }

    @Test
    fun `a file the book does not have is not served`() {
        val server = server()

        assertNull(server.serve("OEBPS/missing.png"))
        assertNull(server.serve("OEBPS/missing.xhtml"))
    }

    @Test
    fun `a page larger than the limit is not served`() {
        val server = server(maxPageBytes = 20)

        assertNull(server.serve("OEBPS/ch1.xhtml"))
    }

    @Test
    fun `the style sheet carries the theme colours and the text size`() {
        val served = server().serve(EpubPageServer.STYLE_PATH)!!
        val css = served.text()

        assertEquals("text/css", served.mimeType)
        assertTrue(css.contains("color: #112233 !important"))
        assertTrue(css.contains("background-color: #FFFFFF !important"))
        assertTrue(css.contains("color: #AA0000 !important"))
        assertTrue(css.contains("font-size: 120% !important"))
    }

    @Test
    fun `the text size is kept within sensible limits`() {
        assertTrue(style.copy(textPercent = 5).css().contains("font-size: ${ReaderStyle.MIN_TEXT_PERCENT}%"))
        assertTrue(style.copy(textPercent = 5000).css().contains("font-size: ${ReaderStyle.MAX_TEXT_PERCENT}%"))
    }

    @Test
    fun `the page view style lays the chapter out in screen wide columns`() {
        val paged = style.copy(paged = true).css()

        assertTrue(paged.contains("column-width: 100vw !important"))
        assertTrue(paged.contains("column-gap: 0 !important"))
        assertTrue(paged.contains("height: 100vh !important"))
        assertTrue(paged.contains("color: #112233 !important"))
        assertTrue(paged.contains("font-size: 120% !important"))
        assertFalse(style.css().contains("column-width"))
    }

    @Test
    fun `the page view can be given the exact height of the screen in pixels`() {
        val paged = style.copy(paged = true, pageHeight = 640).css()

        assertTrue(paged.contains("height: 640px !important"))
        assertFalse(paged.contains("100vh !important; padding"))
    }

    @Test
    fun `a changed style changes the stamp on the style link`() {
        val scrolling = server()
        val first = scrolling.serve("OEBPS/ch1.xhtml")!!.text()

        val paged = EpubPageServer(opened.first(), style.copy(paged = true))
        val second = paged.serve("OEBPS/ch1.xhtml")!!.text()

        assertTrue(first != second)
    }

    @Test
    fun `the style link and the style sheet use the same address`() {
        val server = server()

        assertEquals(EpubPageServer.STYLE_PATH, server.pathOf(EpubPageServer.STYLE_URL))
    }
}
