package com.freelibrary.catalogtool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val SAMPLE_RSS =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://purl.org/rss/1.0/">
    <channel>
        <title>Project Gutenberg Recent RSS Feed</title>
        <link>https://www.gutenberg.org</link>
        <description>eBooks posted or updated in the last 24 hours.</description>
    </channel>
    <item>
        <title>Some Book Title</title>
        <link>https://www.gutenberg.org/ebooks/79609</link>
        <description>Language: English</description>
    </item>
    <item>
        <title>Another Book Title</title>
        <link>https://www.gutenberg.org/ebooks/79610</link>
        <description>Language: French</description>
    </item>
    </rdf:RDF>
    """.trimIndent().toByteArray()

private val EMPTY_RSS =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://purl.org/rss/1.0/">
    <channel>
        <title>Project Gutenberg Recent RSS Feed</title>
        <link>https://www.gutenberg.org</link>
    </channel>
    </rdf:RDF>
    """.trimIndent().toByteArray()

class RssParserTest {
    @Test
    fun `parseRssBookIds extracts ids from item links, excluding the channel-level link`() {
        val ids = parseRssBookIds(SAMPLE_RSS)

        assertEquals(listOf("79609", "79610"), ids)
    }

    @Test
    fun `parseRssBookIds returns an empty list when there are no items`() {
        val ids = parseRssBookIds(EMPTY_RSS)

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `parseRssBookIds deduplicates repeated ids while preserving order`() {
        val rssWithDuplicate =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://purl.org/rss/1.0/">
            <channel>
                <link>https://www.gutenberg.org</link>
            </channel>
            <item>
                <link>https://www.gutenberg.org/ebooks/79609</link>
            </item>
            <item>
                <link>https://www.gutenberg.org/ebooks/79609</link>
            </item>
            <item>
                <link>https://www.gutenberg.org/ebooks/79610</link>
            </item>
            </rdf:RDF>
            """.trimIndent().toByteArray()

        val ids = parseRssBookIds(rssWithDuplicate)

        assertEquals(listOf("79609", "79610"), ids)
    }
}
