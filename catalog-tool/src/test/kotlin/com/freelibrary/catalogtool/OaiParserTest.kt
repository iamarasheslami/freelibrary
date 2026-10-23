package com.freelibrary.catalogtool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** A record shaped like a real OAPEN xoai record (101963), shortened. */
private val BOOK_RECORD =
    """
    <record><header><identifier>oai:library.oapen.org:20.500.12657/101963</identifier><datestamp>2025-05-16T11:24:10Z</datestamp>
    <setSpec>com_20.500.12657_5</setSpec></header><metadata><metadata xmlns="http://www.lyncode.com/xoai">
    <element name="dc">
      <element name="contributor"><element name="editor"><element name="none">
        <field name="value">DZHW</field><field name="authority">3c6e</field>
        <field name="value">DAAD</field>
      </element></element></element>
      <element name="date"><element name="issued"><element name="none"><field name="value">2022</field></element></element></element>
      <element name="description"><element name="abstract"><element name="none">
        <field name="value">The study has been  analysing
        the international nature of studies.</field>
      </element></element></element>
      <element name="language"><element name="none"><field name="originalValue">English</field><field name="value">eng</field></element></element>
      <element name="subject">
        <element name="classification"><element name="none">
          <field name="value">thema EDItEUR::J Society and Social Sciences::JN Education::JNM Higher education</field>
        </element></element>
        <element name="other"><element name="none"><field name="value">OECD</field></element></element>
      </element>
      <element name="title">
        <element name="none"><field name="value">Wissenschaft weltoffen 2021</field></element>
        <element name="alternative"><element name="none"><field name="value">Facts and Figures</field></element></element>
      </element>
      <element name="type"><element name="none"><field name="value">book</field></element></element>
    </element>
    <element name="bundles">
      <element name="bundle"><field name="name">ORIGINAL</field><element name="bitstreams"><element name="bitstream">
        <field name="name">9783763967605.pdf</field><field name="format">application/pdf</field><field name="size">16226217</field>
        <field name="url">https://library.oapen.org/bitstream/20.500.12657/101963/1/9783763967605.pdf</field>
        <field name="rights">CC-BY-SA</field>
      </element></element></element>
      <element name="bundle"><field name="name">TEXT</field><element name="bitstreams"><element name="bitstream">
        <field name="name">9783763967605.pdf.txt</field><field name="format">text/plain</field>
        <field name="url">https://library.oapen.org/bitstream/20.500.12657/101963/2/9783763967605.pdf.txt</field>
      </element></element></element>
      <element name="bundle"><field name="name">THUMBNAIL</field><element name="bitstreams"><element name="bitstream">
        <field name="name">9783763967605.pdf.jpg</field><field name="format">image/jpeg</field>
        <field name="url">https://library.oapen.org/bitstream/20.500.12657/101963/3/9783763967605.pdf.jpg</field>
      </element></element></element>
    </element>
    </metadata></metadata></record>
    """.trimIndent()

private fun listRecords(
    records: String,
    token: String = "",
): ByteArray =
    (
        """<?xml version="1.0" encoding="UTF-8"?><OAI-PMH xmlns="http://www.openarchives.org/OAI/2.0/">""" +
            """<responseDate>2026-10-22T21:20:07Z</responseDate><request verb="ListRecords">x</request><ListRecords>""" +
            records +
            """<resumptionToken completeListSize="50823" cursor="0">$token</resumptionToken></ListRecords></OAI-PMH>"""
    ).toByteArray()

class OaiParserTest {
    @Test
    fun `a book record becomes a book with its file, cover and details`() {
        val page = parseOaiPage(listRecords(BOOK_RECORD, token = "abc"))

        val item = page.books.single()
        val book = item.book
        assertEquals("101963", book.externalId)
        assertEquals("2025-05-16", item.lastModified)
        assertEquals("Wissenschaft weltoffen 2021", book.title)
        assertEquals("2022", book.issuedDate)
        assertEquals("en", book.language)
        assertEquals(listOf("DZHW", "DAAD"), book.creators.map { it.name })
        assertEquals(listOf("Category: Society and Social Sciences"), book.bookshelves)
        assertEquals("The study has been analysing the international nature of studies.", book.summary)
        assertEquals("https://library.oapen.org/bitstream/20.500.12657/101963/3/9783763967605.pdf.jpg", book.coverUrl)
        assertEquals(
            listOf(
                ParsedFormat("https://library.oapen.org/bitstream/20.500.12657/101963/1/9783763967605.pdf", "application/pdf"),
                ParsedFormat("https://library.oapen.org/bitstream/20.500.12657/101963/2/9783763967605.pdf.txt", "text/plain"),
            ),
            book.formats,
        )
        assertNull(book.locc)
    }

    @Test
    fun `the resumption token and the total are read`() {
        val page = parseOaiPage(listRecords(BOOK_RECORD, token = "abc/123"))

        assertEquals("abc/123", page.resumptionToken)
        assertEquals(50823, page.completeListSize)
    }

    @Test
    fun `an empty resumption token means the last page`() {
        assertNull(parseOaiPage(listRecords(BOOK_RECORD)).resumptionToken)
    }

    @Test
    fun `records that are not usable books are skipped and counted`() {
        val chapter = BOOK_RECORD.replace("""<field name="value">book</field>""", """<field name="value">chapter</field>""")
        val noFile = BOOK_RECORD.replace("application/pdf", "text/html").replace("text/plain", "text/html")
        val insecure = BOOK_RECORD.replace("https://library.oapen.org/bitstream/", "http://example.org/")
        val deleted =
            """<record><header status="deleted"><identifier>oai:library.oapen.org:20.500.12657/5</identifier>""" +
                """<datestamp>2025-05-16T11:24:10Z</datestamp></header></record>"""

        val page = parseOaiPage(listRecords(BOOK_RECORD + chapter + noFile + insecure + deleted))

        assertEquals(1, page.books.size)
        assertEquals(4, page.skipped)
    }

    @Test
    fun `a book with only the extracted text is kept with a text file`() {
        val textOnly = BOOK_RECORD.replace("application/pdf", "text/html")

        val book = parseOaiPage(listRecords(textOnly)).books.single().book

        assertEquals(
            listOf(ParsedFormat("https://library.oapen.org/bitstream/20.500.12657/101963/2/9783763967605.pdf.txt", "text/plain")),
            book.formats,
        )
    }

    @Test
    fun `an unknown language is left empty and a known three-letter code becomes two letters`() {
        val unknown = BOOK_RECORD.replace("""<field name="value">eng</field>""", """<field name="value">zzz</field>""")
        val german = BOOK_RECORD.replace("""<field name="value">eng</field>""", """<field name="value">ger</field>""")

        assertNull(parseOaiPage(listRecords(unknown)).books.single().book.language)
        assertEquals("de", parseOaiPage(listRecords(german)).books.single().book.language)
    }

    @Test
    fun `noRecordsMatch is an empty last page`() {
        val xml = """<OAI-PMH><error code="noRecordsMatch">nothing</error></OAI-PMH>""".toByteArray()

        val page = parseOaiPage(xml)

        assertEquals(0, page.books.size)
        assertNull(page.resumptionToken)
    }

    @Test
    fun `another OAI error and broken XML are failures`() {
        assertThrows(
            OaiParseException::class.java,
        ) { parseOaiPage("""<OAI-PMH><error code="badArgument">x</error></OAI-PMH>""".toByteArray()) }
        assertThrows(OaiParseException::class.java) { parseOaiPage("not xml".toByteArray()) }
    }
}
