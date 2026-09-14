package com.freelibrary.catalogtool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val SAMPLE_RDF = """
    <?xml version="1.0" encoding="utf-8"?>
    <rdf:RDF xml:base="http://www.gutenberg.org/"
             xmlns:dcam="http://purl.org/dc/dcam/"
             xmlns:dcterms="http://purl.org/dc/terms/"
             xmlns:pgterms="http://www.gutenberg.org/2009/pgterms/"
             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <pgterms:ebook rdf:about="ebooks/1400">
            <dcterms:title>Great Expectations</dcterms:title>
            <dcterms:creator>
                <pgterms:agent rdf:about="2009/agents/37">
                    <pgterms:name>Dickens, Charles</pgterms:name>
                    <pgterms:birthdate rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">1812</pgterms:birthdate>
                    <pgterms:deathdate rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">1870</pgterms:deathdate>
                </pgterms:agent>
            </dcterms:creator>
            <dcterms:issued rdf:datatype="http://www.w3.org/2001/XMLSchema#date">1998-07-01</dcterms:issued>
            <dcterms:language>
                <rdf:Description>
                    <rdf:value rdf:datatype="http://purl.org/dc/terms/RFC4646">en</rdf:value>
                </rdf:Description>
            </dcterms:language>
            <dcterms:subject>
                <rdf:Description>
                    <dcam:memberOf rdf:resource="http://purl.org/dc/terms/LCSH"/>
                    <rdf:value>Bildungsromans</rdf:value>
                </rdf:Description>
            </dcterms:subject>
            <dcterms:subject>
                <rdf:Description>
                    <rdf:value>PR</rdf:value>
                    <dcam:memberOf rdf:resource="http://purl.org/dc/terms/LCC"/>
                </rdf:Description>
            </dcterms:subject>
            <pgterms:bookshelf>
                <rdf:Description>
                    <rdf:value>Best Books Ever Listings</rdf:value>
                </rdf:Description>
            </pgterms:bookshelf>
            <dcterms:hasFormat>
                <pgterms:file rdf:about="http://www.gutenberg.org/files/1400/1400-0.txt">
                    <dcterms:format>
                        <rdf:Description>
                            <rdf:value rdf:datatype="http://purl.org/dc/terms/IMT">text/plain; charset=utf-8</rdf:value>
                        </rdf:Description>
                    </dcterms:format>
                </pgterms:file>
            </dcterms:hasFormat>
        </pgterms:ebook>
    </rdf:RDF>
""".trimIndent().toByteArray()

private val NON_TEXT_SAMPLE_RDF = """
    <?xml version="1.0" encoding="utf-8"?>
    <rdf:RDF xml:base="http://www.gutenberg.org/"
             xmlns:dcam="http://purl.org/dc/dcam/"
             xmlns:dcterms="http://purl.org/dc/terms/"
             xmlns:pgterms="http://www.gutenberg.org/2009/pgterms/"
             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <pgterms:ebook rdf:about="ebooks/50">
            <dcterms:title>Pi</dcterms:title>
            <dcterms:type>
                <rdf:Description>
                    <rdf:value>Dataset</rdf:value>
                </rdf:Description>
            </dcterms:type>
        </pgterms:ebook>
    </rdf:RDF>
""".trimIndent().toByteArray()

private val NO_TITLE_SAMPLE_RDF = """
    <?xml version="1.0" encoding="utf-8"?>
    <rdf:RDF xml:base="http://www.gutenberg.org/"
             xmlns:dcterms="http://purl.org/dc/terms/"
             xmlns:pgterms="http://www.gutenberg.org/2009/pgterms/"
             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <pgterms:ebook rdf:about="ebooks/90907">
        </pgterms:ebook>
    </rdf:RDF>
""".trimIndent().toByteArray()

private val NO_FORMATS_SAMPLE_RDF = """
    <?xml version="1.0" encoding="utf-8"?>
    <rdf:RDF xml:base="http://www.gutenberg.org/"
             xmlns:dcterms="http://purl.org/dc/terms/"
             xmlns:pgterms="http://www.gutenberg.org/2009/pgterms/"
             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <pgterms:ebook rdf:about="ebooks/69279">
            <dcterms:title>Come home from Earth</dcterms:title>
        </pgterms:ebook>
    </rdf:RDF>
""".trimIndent().toByteArray()

class RdfParserTest {

    @Test
    fun `parseRdf extracts title, id, and issued date`() {
        val book = parseRdf(SAMPLE_RDF)

        assertEquals("1400", book?.externalId)
        assertEquals("Great Expectations", book?.title)
        assertEquals("1998-07-01", book?.issuedDate)
    }

    @Test
    fun `parseRdf extracts creator with birth and death years`() {
        val book = parseRdf(SAMPLE_RDF)

        val creator = book?.creators?.first()
        assertEquals("Dickens, Charles", creator?.name)
        assertEquals(1812, creator?.birthYear)
        assertEquals(1870, creator?.deathYear)
    }

    @Test
    fun `parseRdf extracts language`() {
        val book = parseRdf(SAMPLE_RDF)

        assertEquals("en", book?.language)
    }

    @Test
    fun `parseRdf separates LCSH subjects from the LCC classification code`() {
        val book = parseRdf(SAMPLE_RDF)

        assertEquals(listOf("Bildungsromans"), book?.subjects)
        assertEquals("PR", book?.locc)
    }

    @Test
    fun `parseRdf extracts bookshelves`() {
        val book = parseRdf(SAMPLE_RDF)

        assertEquals(listOf("Best Books Ever Listings"), book?.bookshelves)
    }

    @Test
    fun `parseRdf extracts format URL and mime type`() {
        val book = parseRdf(SAMPLE_RDF)

        val format = book?.formats?.first()
        assertEquals("http://www.gutenberg.org/files/1400/1400-0.txt", format?.url)
        assertTrue(format?.mimeType?.startsWith("text/plain") == true)
    }

    @Test
    fun `parseRdf returns null for a non-Text entry, treated as an intentional skip`() {
        val result = parseRdf(NON_TEXT_SAMPLE_RDF)

        assertNull(result)
    }

    @Test
    fun `parseRdf returns null for an entry with no title, treated as an intentional skip`() {
        val result = parseRdf(NO_TITLE_SAMPLE_RDF)

        assertNull(result)
    }

    @Test
    fun `parseRdf returns null for an entry with zero downloadable formats, treated as an intentional skip`() {
        val result = parseRdf(NO_FORMATS_SAMPLE_RDF)

        assertNull(result)
    }
}